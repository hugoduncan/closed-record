# Steps

## Slice 1: harness

- [ ] Add the `:bench` alias to `deps.edn`: `:extra-paths ["dev/bench"]`, `:extra-deps
      {criterium/criterium {:mvn/version "0.5.243-ALPHA"}}`, `:jvm-opts
      ["-XX:+UnlockExperimentalVMOptions" "-XX:CompileCommand=blackhole,criterium.blackhole.Blackhole::consume"]`,
      `:main-opts ["-m" "closed-record.bench"]`.
- [ ] Create `dev/bench/closed_record/bench.clj` (ns `closed-record.bench`) with
      `(defclosedrecord P [a b c])` and `(defrecord R [a b c])`.
- [ ] Write pure helpers: extract from `criterium.bench/last-bench` data the mean `:point-estimate` and the
      two `:estimate-quantiles` values at `[:data :bootstrap-stats :bootstrap :elapsed-time :mean]`, and the
      `:batch-size` at `[:data :bootstrap-stats :batch-size]`; divide each by batch size to get per-call ns
      (mean, low = `:alpha 0.025`, high = `:alpha 0.975`).
- [ ] Write the pass rule as a pure function on unrounded values: pass when `P-mean / R-mean ≤ 1.05`, or
      intervals overlap inclusively (`P-low ≤ R-high` and `R-low ≤ P-high`).
- [ ] Write table formatting: one row per operation, columns Operation; P mean and `[low, high]` (ns,
      2 decimals); R mean and `[low, high]` (ns, 2 decimals); `P / R` ratio (3 decimals); Result
      `pass`/`fail`.
- [ ] Write a bench macro (not a function that takes a thunk) `(bench-op expr limit-s)`: it expands to
      `(criterium.bench/bench ~expr :viewer :none :limit-time-s ~limit-s)` around the literal operation
      expression, then reads `(criterium.bench/last-bench)` and returns the per-call stats. The measured
      body has no `IFn` invoke, var deref or shared call site; `x` (and `y`) are locals in a `let` around
      each use.
- [ ] Write `-main`: for each operation in order — `(:a x)`, `(.a x)` with a type hint,
      `(assoc x :a 2)`, `(hash x)` on one instance, `(= x y)` on two distinct equal instances — bench `P`
      then `R` with `bench-op` and `:limit-time-s` 10 (ten `bench-op` uses, each with its own literal
      expression); print the table; `(System/exit (if all-pass 0 1))`.
- [ ] REPL smoke test with a short `:limit-time-s` (e.g. 1 s): verify the `last-bench` data paths, the
      table shape, and the pass rule on sample values.
- [ ] Commit the harness.

## Slice 2: acceptance run

- [ ] Run `clojure -M:bench` on Clojure 1.11.4 on a quiet machine. Verify: five operations run, table
      printed, no blackhole warning, exit code matches the pass rule (`echo $?`).
- [ ] Record in `implementation.md`: the full table, the exit code, the JVM version, the machine, the
      Clojure version, and the date.
- [ ] Commit the run record.

## Slice 3: docs

- [ ] `ramora/defclosedrecord/purpose.md`, S-03: change to the one-sided rule — the closed record mean is
      not more than 5% above the `defrecord` mean, or the 95% intervals overlap.
- [ ] `README.md`, end of "## Development": add "Run the benchmarks against `defrecord`:" and an indented
      code block `clojure -M:bench`. No results in the README.
- [ ] `ramora/defclosedrecord/embodiment.md`, Artifacts row `dev/bench/`: replace with
      "`closed_record/bench.clj` (`closed-record.bench`): criterium benchmarks of the five S-03 operations
      against `defrecord` (V-11)."
- [ ] `ramora/defclosedrecord/embodiment.md`, Artifacts row `deps.edn`: replace with "Clojure 1.11
      dependency. Aliases `:test`, `:bench`, `:nrepl`, `:clj-1.12` and `:clj-1.13`. The `:test` alias has
      scry, `test.check` and malli (`metosin/malli`). The `:bench` alias has criterium
      (`criterium/criterium`)."
- [ ] `mementum/state.md`, Status "Project skeleton" line: alias list becomes `:test`, `:bench`, `:nrepl`,
      `:clj-1.12`, `:clj-1.13`.
- [ ] `mementum/state.md`, Status: after the "Tests: `clojure -M:test -m scry.cli`. ..." line, add
      "Bench: `clojure -M:bench` (V-11). It exits 1 if an operation fails."
- [ ] `mementum/state.md`, "Verification plan" line, from the Slice 2 recorded run: all pass → move V-11 to
      the done list (drop the "Not done:" clause if empty); any fail → keep V-11 not done and add the failed
      operations with their `P / R` ratios, e.g. "Not done: V-11 (the bench exists; `assoc` fails, `P / R`
      1.140)."
- [ ] Commit the docs.

## Slice 4: verify

- [ ] `clj-kondo --lint src test dev`: no errors, no warnings. Fix findings in `dev/bench` if any.
- [ ] `cljfmt check src test dev`: passes. Fix with `cljfmt fix` if needed.
- [ ] Run the test suite (`clojure -M:test -m scry.cli`): still passes.
- [ ] Check all acceptance criteria 1–5 against the work; note the check in `implementation.md`.
- [ ] Commit any fixes.
