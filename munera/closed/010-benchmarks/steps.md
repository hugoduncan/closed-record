# Steps

## Slice 1: harness

- [x] Add the `:bench` alias to `deps.edn`: `:extra-paths ["dev/bench"]`, `:extra-deps
      {criterium/criterium {:mvn/version "0.5.243-ALPHA"}}`, `:jvm-opts
      ["-XX:+UnlockExperimentalVMOptions" "-XX:CompileCommand=blackhole,criterium.blackhole.Blackhole::consume"]`,
      `:main-opts ["-m" "closed-record.bench"]`.
- [x] Create `dev/bench/closed_record/bench.clj` (ns `closed-record.bench`) with
      `(defclosedrecord P [a b c])` and `(defrecord R [a b c])`.
- [x] Write pure helpers: extract from `criterium.bench/last-bench` data the mean `:point-estimate` and the
      two `:estimate-quantiles` values at `[:data :bootstrap-stats :bootstrap :elapsed-time :mean]`, and the
      `:batch-size` at `[:data :bootstrap-stats :batch-size]`; divide each by batch size to get per-call ns
      (mean, low = `:alpha 0.025`, high = `:alpha 0.975`).
- [x] Write the pass rule as a pure function on unrounded values: pass when `P-mean / R-mean ≤ 1.05`, or
      intervals overlap inclusively (`P-low ≤ R-high` and `R-low ≤ P-high`).
- [x] Write table formatting: one row per operation, columns Operation; P mean and `[low, high]` (ns,
      2 decimals); R mean and `[low, high]` (ns, 2 decimals); `P / R` ratio (3 decimals); Result
      `pass`/`fail`.
- [x] Write a bench macro (not a function that takes a thunk) `(bench-op expr limit-s)`: it expands to
      `(criterium.bench/bench ~expr :viewer :none :limit-time-s ~limit-s)` around the literal operation
      expression, then reads `(criterium.bench/last-bench)` and returns the per-call stats. The measured
      body has no `IFn` invoke, var deref or shared call site; `x` (and `y`) are locals in a `let` around
      each use.
- [x] Write `-main`: for each operation in order — `(:a x)`, `(.a x)` with a type hint,
      `(assoc x :a 2)`, `(hash x)` on one instance, `(= x y)` on two distinct equal instances — bench `P`
      then `R` with `bench-op` and `:limit-time-s` 10 (ten `bench-op` uses, each with its own literal
      expression); print the table; `(System/exit (if all-pass 0 1))`.
- [x] REPL smoke test with a short `:limit-time-s` (e.g. 1 s): verify the `last-bench` data paths, the
      table shape, and the pass rule on sample values.
- [x] Commit the harness.

## Slice 2: acceptance run

- [x] Run `clojure -M:bench` on Clojure 1.11.4 on a quiet machine. Verify: five operations run, table
      printed, no blackhole warning, exit code matches the pass rule (`echo $?`).
- [x] Record in `implementation.md`: the full table, the exit code, the JVM version, the machine, the
      Clojure version, and the date.
- [x] Commit the run record.

## Slice 3: docs

- [x] `ramora/defclosedrecord/purpose.md`, S-03: change to the one-sided rule — the closed record mean is
      not more than 5% above the `defrecord` mean, or the 95% intervals overlap.
- [x] `README.md`, end of "## Development": add "Run the benchmarks against `defrecord`:" and an indented
      code block `clojure -M:bench`. No results in the README.
- [x] `ramora/defclosedrecord/embodiment.md`, Artifacts row `dev/bench/`: replace with
      "`closed_record/bench.clj` (`closed-record.bench`): criterium benchmarks of the five S-03 operations
      against `defrecord` (V-11)."
- [x] `ramora/defclosedrecord/embodiment.md`, Artifacts row `deps.edn`: replace with "Clojure 1.11
      dependency. Aliases `:test`, `:bench`, `:nrepl`, `:clj-1.12` and `:clj-1.13`. The `:test` alias has
      scry, `test.check` and malli (`metosin/malli`). The `:bench` alias has criterium
      (`criterium/criterium`)."
- [x] `mementum/state.md`, Status "Project skeleton" line: alias list becomes `:test`, `:bench`, `:nrepl`,
      `:clj-1.12`, `:clj-1.13`.
- [x] `mementum/state.md`, Status: after the "Tests: `clojure -M:test -m scry.cli`. ..." line, add
      "Bench: `clojure -M:bench` (V-11). It exits 1 if an operation fails."
- [x] `mementum/state.md`, "Verification plan" line, from the Slice 2 recorded run: all pass → move V-11 to
      the done list (drop the "Not done:" clause if empty); any fail → keep V-11 not done and add the failed
      operations with their `P / R` ratios, e.g. "Not done: V-11 (the bench exists; `assoc` fails, `P / R`
      1.140)."
- [x] Commit the docs.

## Slice 4: verify

- [x] `clj-kondo --lint src test dev`: no errors, no warnings. Fix findings in `dev/bench` if any.
- [x] `cljfmt check src test dev`: passes. Fix with `cljfmt fix` if needed.
- [x] Run the test suite (`clojure -M:test -m scry.cli`): still passes.
- [x] Check all acceptance criteria 1–5 against the work; note the check in `implementation.md`.
- [x] Commit any fixes.

## Implementation review

- [x] Acceptance run choice does not follow the plan. `mementum/state.md` marks V-11 done from a later run.
  - plan.md Risks: "record the one acceptance run ... Do not cherry-pick re-runs". The plan ambiguity review,
    pass 2, says the acceptance run is "the first run of the fixed harness".
  - implementation.md: the first full 10 s run of the same harness gave `=` 1.086 fail. Its output was only
    partly captured. The run in the table is a later run.
  - Fix: apply the plan rule (keep V-11 not done in `state.md`, with `=` `P / R` 1.086, as Scope 7 says), or
    get a user decision to accept the later run, and record the decision in implementation.md.
- [x] The field access row does not measure a field read. `(.a ^P x)` and `(.a ^R x)` give about 0.06 ns
  (less than one CPU cycle). implementation.md says that the JIT hoists the read. The `pass` result is vacuous.
  - Fix: change the `(.a x)` measurement so that the JIT cannot hoist the read (verify a time near the keyword
    lookup), or state in the V-11 status of `mementum/state.md` that field access is not measured.
- [x] The V-11 status in `mementum/state.md` does not come from the run that implementation.md records.
  - design.md Scope 7: the failed operations and `P / R` ratios come "from the run that `implementation.md`
    records (Acceptance 3)". Acceptance 3: implementation.md records the table of one run.
  - The review follow-up makes the first full 10 s run the acceptance run (`=` 1.086). Its table is not recorded:
    the output was only partly captured. The table under "### Acceptance run" is a later run (exit code 0, all pass).
  - Fix: in implementation.md, record the captured rows and the exit code of the first full run as the acceptance
    run, and note that its full table is not available (Acceptance 3 not fully met). Or get a user decision on
    which run is the acceptance run, and make `state.md` and implementation.md agree with it.

## Test review

- [x] The pure layer of `closed-record.bench` has no tests. The Scope 3 and Scope 4 behaviour is checked only at
  the REPL (implementation.md, "Pass rule checked at the REPL"). No test keeps it.
  - Add tests that inject `last-bench`-shaped data maps (state, not a criterium run):
    - `per-call-stats`: divides the mean and both quantiles by `:batch-size`; low is `:alpha 0.025`, high is
      `:alpha 0.975`, independent of the vector order.
    - `pass?`: ratio `≤ 1.05` passes; ratio above 1.05 with no overlap fails; overlap passes; inclusive
      overlap edge (`P low = R high`) passes; faster `P` passes; a ratio that prints `1.050` but is above 1.05
      with no overlap fails (unrounded values).
  - The tests run no benchmark (Out of scope 1 is about the slow, machine-dependent run). Do not add criterium to
    the `:test` alias (Acceptance 1, Decisions). Give the run command in the test file docstring or README.
- [x] The Scope 5 output and the exit code rule have no tests.
  - `row` and `table`: column order, 2 decimals for ns, 3 decimals for `P / R`, `pass`/`fail` text, one row for
    each operation in input order.
  - Exit code: `-main` mixes the all-pass decision with `System/exit`. Extract the decision (for example
    `(exit-code results)`) as a pure function used by `-main`, and test it: 0 when all pass, 1 when one fails.
    No test-only option in `-main`.

## Test review, pass 2

- [x] `table-test` fixes table format choices that the design does not specify. Design Scope 5 gives the columns
  and their order only. The plan ambiguity review lists "table text format" as an open choice.
  - These assertions prevent a change of format: the separator line regex (`\|[-|]+\|`), "all lines have the same
    width" (padding), the line count 4 (header and separator), and the exact header label text.
  - Fix: keep the Scope 5 checks only. For each data line, the cells (split on `|`, trimmed) are equal to the
    `row` vector of that operation, and the data lines are in input order. Remove the separator, width and
    header-text assertions.

## Test review, pass 3

- [x] The `row` and `pass?` tests depend on the JVM default locale. With `-Duser.language=de -Duser.country=DE`,
  3 assertions fail: `report/row` gives `"1,23 [1,20, 1,30]"` and `"1,234"`, and the test `format` gives `"1,050"`.
  - The cause is in `closed-record.bench.report`: `format` uses the default locale. The bench table then also
    has a decimal comma next to the interval comma (`[1,20, 1,30]`), which is ambiguous.
  - Fix: format with a fixed locale in `report` (for example `(String/format java.util.Locale/ROOT ...)`), and
    use the same in the `pass?-test` premise check (or use `report/row` for it). No test-only option.

## Docs review

- [x] `ramora/defclosedrecord/embodiment.md`, Artifacts table, has no row for `test/closed_record/bench_report_test.clj`.
  - The test file matches the `test/closed_record/*_test.clj` glob row, but that row says "Behaviour, differential,
    equality, malli, nil, literal and constant tests". It does not name bench report tests.
  - The other specific test files have a row each, with the V-item that they verify.
  - Fix: add a row, for example "`test/closed_record/bench_report_test.clj` | Tests of the bench pure layer
    (`closed-record.bench.report`): per-call stats, pass rule, table and exit code (V-11). No benchmark run."

## Code shape review

- [x] `closed-record.bench.report/table` pads cells with `clojure.core/format`. The other formatting in the
  namespace uses the private `fmt` (`Locale/ROOT`). Two format idioms in one namespace: a reader must know that
  `%-Ns` on a string does not depend on the locale.
  - Fix: use `fmt` in `table` for the cell padding, so that all formatting in `report` has one idiom.
