# Plan

## Approach

Build bottom-up: pure layer first, with tests, then the process harness, then the structural test, then the
acceptance run, then the documents.

1. Pure layer (`closed-record.bench.report`). Add:
   - `ops`: the six operation strings in the Scope 3 order: `"(:a x)"`, `"(get x :a)"`, `"(.a x)"`,
     `"(assoc x :a 2)"`, `"(hash x)"`, `"(= x y)"`.
   - `fork-count`: 5. The parent loop and `fork-stats` use it.
   - `encode-result`: return the EDN string (`pr-str`) of a child result, a vector of six `[op p r]` entries,
     `p` and `r` as `{:mean :low :high}` (ns).
   - `decode-result`: take the file content (a string). Return `{:result <vector>}` if the content is valid, and
     `{:error <message>}` if not. It does not throw. The message is a short text that gives the cause, for
     example `"invalid EDN: <exception message>"` or `"entry 2: operation is \"(.a x)\", expected \"(get x :a)\""`.
     - It catches the reader exception (`clojure.edn/read-string` throws `RuntimeException` on truncated EDN) and
       returns an error value with the exception message.
     - Empty content reads as `nil` (no exception). Validation rejects it ("not a vector of 6 entries").
     - Validation: a vector of exactly 6 entries; each entry a vector of 3 elements; the operation strings equal
       to `ops`, in that order; `p` and `r` maps with exactly the keys `:mean :low :high`, each value a number.
   - `fork-stats`: take the decoded results of the forks (`:result` values). Precondition: `fork-count` results
     (`:pre`), because `t` = 2.776 is for 4 degrees of freedom. For each operation and type: the arithmetic mean of
     the fork `:mean` values and the Student t interval mean ± 2.776 · s / √5, `s` the sample standard deviation
     (divisor 4). `t` is the constant 2.776 with a comment (0.975 quantile, 4 df; no stats library). Return a
     vector of `[op p r n]` in the Scope 3 order, with `p` and `r` as `{:mean :low :high}` and `n` the number of
     fork results (the `Forks` value).
   - `Forks` column: `header` gets a final `"Forks"` cell. `row` becomes `[op p r n]` and gets a final cell
     `(str n)`. So the `Forks` value is data from `fork-stats`, not a constant in `row`.
   - `pass?`, `exit-code` keep the 0 / 1 contract over timed rows. `exit-code` destructures `[_ p r]`, so it
     accepts the 4-element `fork-stats` entries. Exit 2 is parent-only, not in the pure layer.
   - Ns docstring: add the encode and decode of the child result and the statistics over forks to the list.
2. Child mode (`closed-record.bench`).
   - Rework `run` for six operations, Scope 3 order, with `(get x :a)` added. Each `bench-op` call site runs once
     per JVM (divide-by-zero fault). Alternate order per operation by fork parity: even forks (0, 2, 4) `P` then
     `R`; odd forks `R` then `P`. Because call sites are fixed, emit both orders as distinct call sites selected
     by parity, each still executed at most once per JVM. `run` docstring: "the six operations" (not five).
   - `-main` with two args `fork result-file`: run with 5 s limit, `spit` `(report/encode-result ...)` to the
     file, exit 0 (not `report/exit-code` — the expected `assoc` fail must not fail the fork).
   - Ns docstring: the parent and child modes of `-main` and the exit codes 0, 1 and 2.
3. Parent mode (`closed-record.bench`).
   - `parent`: a function of `child-cmd`, a function of `[fork file]` that returns the command vector. It prints the
     error or the table and returns the exit code (0, 1 or 2). It does not call `System/exit`.
   - `-main` with no args: `(System/exit (parent default-child-cmd))`.
   - `default-child-cmd` gives `[<java> <jvm-opts>... "-cp" <classpath> "clojure.main" "-m" "closed-record.bench"
     <fork> <file>]`: the java binary from `java.home`/`bin/java`, the classpath from `java.class.path`, and all
     the input arguments from `RuntimeMXBean/getInputArguments`, with no filter. This includes the blackhole options
     and `-Dclojure.basis=.cpcache/<hash>.basis` (a persistent file, relative to the working directory, which the
     child inherits).
   - Fork loop, for fork 0 to `fork-count` - 1: make a temporary result file (`File/createTempFile`), start the
     child (`ProcessBuilder`, `inheritIO`), wait for its exit, then check in this order:
     1. exit value not 0 → cause `"exit <value>"`.
     2. result file missing → cause `"missing result file <path>"`.
     3. `(report/decode-result (slurp file))` gives `{:error msg}` → cause `msg`.
     On the first failure: print `fork <n> failed: <cause>` to stderr, start no more forks, print no table,
     return 2. The parent does not delete the result files, so a bad file stays for inspection.
   - On 5 good forks: `report/fork-stats` of the 5 decoded results (decoded after each fork) → table → return
     `report/exit-code` (0 / 1).
   - Verification in Slice 3 is short, with no full run: call `parent` with test commands that do not start a
     JVM (see steps). The full parent run is the acceptance run (Slice 5).
4. Structural test. `test/closed_record/field_value_test.clj`: closed type and `defrecord` with one untyped and
   one `^long` field; non-static declared fields equal in name, order, type and modifiers. Compare by
   `getDeclaredFields` filtered of static, order as given (HotSpot gives declaration order; this matches the
   existing design-review note). Ns docstring names V-11.
5. Acceptance run: the first full run of `clojure -M:bench`.
   - The harness is finished at the end of Slice 3 (Slice 4 adds a test only). No full parent run occurs before
     Slice 5.
   - Command: `clojure -M:bench 2>&1 | tee <file>; echo "exit ${PIPESTATUS[0]}"` (`2>&1` captures the child stderr
     and the fork error; `tee` hides the exit code).
   - Complete output + exit code + JVM and machine into `implementation.md`. Expected: `assoc` fails, exit 1.
   - Check on this output: a `CompileCommand: blackhole ...` line for the parent and for each child (6 lines).
   - A run that exits 2, or that has no blackhole line for a child, is a harness fault, not the acceptance run.
     Record its output and the cause in `implementation.md`, fix the fault, and the next full run is the
     acceptance run.
6. Documents (Scope 7): `purpose.md` S-03, `decisions/verification.md` D-16, `mementum/state.md`,
   `mementum/knowledge/benchmarks.md` (new "Forks" section, Known-limits edit, Code-structure edits),
   `embodiment.md` `dev/bench/` row.
   - Also in `benchmarks.md`, texts that the task makes false: "Code structure" "`:main-opts` in `:bench` runs
     `-main` (a 100 s run)" → a run of 5 forks, about 300 s plus JVM start; "Known limits" capture command
     `clojure -M:bench | tee <file>` → the Slice 5 command.

## Key decisions

- Keep exit 2 entirely in `closed-record.bench` (parent); `report/exit-code` stays 0 / 1.
- Child exits 0 after writing its result; a bench failure (assoc) is a row result, not a fork failure.
- Per-parity call sites in `run` so each `bench` call site runs at most once per JVM.
- `decode-result` returns `{:result v}` or `{:error msg}` and does not throw; the parent prints `msg` as the cause
  and takes the exit-2 path.
- The parent decodes each result file when its child exits, so a bad result stops the next forks.
- The `Forks` value is the fourth element `n` of each `fork-stats` entry. `row` prints it.
- The child gets all the input arguments of the parent, unchanged.
- `parent` takes the child command as an argument, so the failure path is verified with short test commands, not
  with an edit of the code.
- t = 2.776 hard-coded with a comment (0.975 quantile, 4 df).

## Risks

- JVM option replay: the child gets all the parent input arguments. If a child fails because of one option, that
  is a fork failure (exit 2) in the acceptance run; fix the option list and record the change in
  `implementation.md`.
- Macro-expansion of per-parity call sites must not double-run any call site in one JVM.
- `getDeclaredFields` order is not specified by Javadoc; HotSpot gives declaration order. Fall back to comparing
  by constructor-parameter order if the test is flaky (noted in implementation.md of the design review).
- Run time ≈ 300 s plus JVM start; acceptance run is long but one-off.
- Open SCOPE_QUESTION: the `qualities.md` V-11 row and `embodiment.md` `bench_report_test.clj` row are not in
  Scope 7. If the user decides to include them, add them in the documents slice and to Acceptance 6.

## Slice order

1. Pure layer: `ops`, `fork-count`, encode/decode, fork-stats, Forks column, ns docstring + tests.
2. Child mode: six operations, parity order, result-file write, exit 0.
3. Parent mode: `parent`, fork orchestration, decode after each fork, failure handling, table, exit codes.
4. Structural field declaration test (three aliases).
5. Acceptance run recorded in implementation.md.
6. Documents update (S-03, D-16, state.md, benchmarks.md, embodiment.md).
7. Final verification: scry on three aliases, clj-kondo, cljfmt.
