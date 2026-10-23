# Plan

## Approach

Build bottom-up: pure layer first, with tests, then the process harness, then the structural test, then the
acceptance run, then the documents.

1. Pure layer (`closed-record.bench.report`). Add:
   - `encode-result` / `decode-result`: EDN encode and decode of the child result, a vector of six `[op p r]`
     entries in the Scope 3 operation order, with `p` and `r` as `{:mean :low :high}` (ns). Decode validates the
     format and the six operations; an invalid content gives a distinguishable error value (not an exception
     swallowed in the parent).
   - `fork-stats`: for 5 fork results, per operation and type, the arithmetic mean of the 5 fork `:mean` values
     and the Student t interval mean ± 2.776 · s / √5, `s` the sample standard deviation (divisor 4). `t` is the
     constant 2.776 (no stats library).
   - `Forks` column: extend `header` and `row` with a final `Forks` cell (value 5).
   - `pass?`, `exit-code` keep the 0 / 1 contract over timed rows. Exit 2 is parent-only, not in the pure layer.
2. Child mode (`closed-record.bench`).
   - Rework `run` for six operations, Scope 3 order, with `(get x :a)` added. Each `bench-op` call site runs once
     per JVM (divide-by-zero fault). Alternate order per operation by fork parity: even forks (0, 2, 4) `P` then
     `R`; odd forks `R` then `P`. Because call sites are fixed, emit both orders as distinct call sites selected
     by parity, each still executed at most once per JVM.
   - `-main` with two args `fork result-file`: run with 5 s limit, write `(report/encode-result ...)` to the file,
     exit 0 (not `report/exit-code` — the expected `assoc` fail must not fail the fork).
3. Parent mode (`closed-record.bench`).
   - `-main` with no args: for fork 0..4, create a temp result file, start
     `java <jvm-opts> -cp <classpath> clojure.main -m closed-record.bench <fork> <file>`, inherit child stdout/
     stderr, wait for exit. JVM opts from `RuntimeMXBean/getInputArguments` (drop `-Dclojure.basis=...` if it is
     not re-usable; keep blackhole opts), classpath from `java.class.path`, java binary from `java.home`/bin.
   - Fork failure (non-zero exit, missing file, invalid decode): print an error naming the fork and cause, start
     no more forks, print no table, exit 2.
   - On 5 good forks: `report/fork-stats` → table → `report/exit-code` (0 / 1).
4. Structural test. `test/closed_record/field_value_test.clj`: closed type and `defrecord` with one untyped and
   one `^long` field; non-static declared fields equal in name, order, type and modifiers. Compare by
   `getDeclaredFields` filtered of static, order as given (HotSpot gives declaration order; this matches the
   existing design-review note). Ns docstring names V-11.
5. Acceptance run: `clojure -M:bench`, complete output + exit code + JVM and machine into `implementation.md`.
   Expected: `assoc` fails, exit 1.
6. Documents (Scope 7): `purpose.md` S-03, `decisions/verification.md` D-16, `mementum/state.md`,
   `mementum/knowledge/benchmarks.md` (new "Forks" section, Known-limits edit, Code-structure edit),
   `embodiment.md` `dev/bench/` row.

## Key decisions

- Keep exit 2 entirely in `closed-record.bench` (parent); `report/exit-code` stays 0 / 1.
- Child exits 0 after writing its result; a bench failure (assoc) is a row result, not a fork failure.
- Per-parity call sites in `run` so each `bench` call site runs at most once per JVM.
- Decode returns data or an error value; the parent maps the error to the exit-2 path with a message.
- t = 2.776 hard-coded with a comment (0.975 quantile, 4 df).

## Risks

- JVM option replay: `-Dclojure.basis` points to a temp basis file that may be valid for a child; if not, drop it
  and pass only the classpath + blackhole opts. Verify the blackhole notice appears in each child.
- Macro-expansion of per-parity call sites must not double-run any call site in one JVM.
- `getDeclaredFields` order is not specified by Javadoc; HotSpot gives declaration order. Fall back to comparing
  by constructor-parameter order if the test is flaky (noted in implementation.md of the design review).
- Run time ≈ 300 s plus JVM start; acceptance run is long but one-off.
- Open SCOPE_QUESTION: the `qualities.md` V-11 row and `embodiment.md` `bench_report_test.clj` row are not in
  Scope 7. If the user decides to include them, add them in the documents slice and to Acceptance 6.

## Slice order

1. Pure layer: encode/decode, fork-stats, Forks column + tests.
2. Child mode: six operations, parity order, result-file write, exit 0.
3. Parent mode: fork orchestration, failure handling, table, exit codes.
4. Structural field declaration test (three aliases).
5. Acceptance run recorded in implementation.md.
6. Documents update (S-03, D-16, state.md, benchmarks.md, embodiment.md).
7. Final verification: scry on three aliases, clj-kondo, cljfmt.
