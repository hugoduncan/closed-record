# 010 benchmarks

## Intent

Measure that a closed record is as fast as `defrecord` (V-11, S-03, K-05). A benchmark command compares the two
types for the five operations of S-03 and reports pass or fail for each operation.

## Problem

V-11 has no benchmark. The design claims `defrecord` performance (D-01, priority 3), but no measurement supports
the claim. `embodiment.md` names `dev/bench/` and a bench alias, but neither exists.

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [purpose](../../../ramora/defclosedrecord/purpose.md#success-criteria): S-03, priority 3.
  - [qualities](../../../ramora/defclosedrecord/qualities.md): V-11, target "Can it meet runtime demands?".
  - [embodiment](../../../ramora/defclosedrecord/embodiment.md): Artifacts rows `dev/bench/` and `deps.edn`.
- `criterium/criterium` 0.5.243-ALPHA is the latest release on Clojars (2026-10-20). 0.4.6 is the last 0.4.x
  release. The 0.5.x API is different from 0.4.x.
- criterium 0.5.243-ALPHA API used by this task:
  - `criterium.bench/bench` is a macro: `(bench expr & options)`. The expression can use local bindings. Options
    include `:limit-time-s` and `:viewer`. `:viewer :none` prints nothing.
  - `criterium.bench/last-bench` gives the data of the last run. The mean is at
    `[:data :bootstrap-stats :bootstrap :elapsed-time :mean]`: `:point-estimate`, and `:estimate-quantiles`, a
    vector of `{:value v :alpha 0.025}` and `{:value v :alpha 0.975}` (95% bootstrap interval).
  - These values are for one sample, a batch of calls, in nanoseconds. The batch size is at
    `[:data :bootstrap-stats :batch-size]`, and it is different from run to run. The time for one call is the
    value divided by the batch size.
- The criterium pom has a dependency on Clojure 1.12.0. The `deps.edn` top-level dependency, Clojure 1.11.4, has
  precedence. criterium loads and runs on Clojure 1.11.4 (observed).
- On JVM 17+, criterium warns when the compiler blackhole flag is not set, and uses a slower runtime blackhole
  (about 3 ns for each call). The flags are
  `-XX:+UnlockExperimentalVMOptions -XX:CompileCommand=blackhole,criterium.blackhole.Blackhole::consume`.
- `hash` and `hasheq` cache the result in a field (`__hash`, `__hasheq`), as `defrecord` does.

### Observed facts (2026-10-20, Clojure 1.11.4, criterium 0.5.243-ALPHA, OpenJDK 25.0.2, arm64, commit `bd52b3a`)

`(defclosedrecord P [a b c])`, `(defrecord R [a b c])`, one JVM with the blackhole flags, `bench` with
`:limit-time-s 5`. Time for one call (sample mean divided by batch size):

| Operation | `P` | `R` |
|---|---|---|
| `(:a x)` | 0.64 ns (batch 10000) | 0.69 ns (batch 10000) |
| `(assoc x :a 2)` | 7.29 ns (batch 2500) | 6.37 ns (batch 3333) |

In this probe, `assoc` on the closed record is about 14% slower than on `defrecord`. The default viewer shows the
median with a confidence interval, not the mean.

## Scope

In scope:

1. A `:bench` alias in `deps.edn`: extra path `dev/bench`, extra dependency `criterium/criterium`
   0.5.243-ALPHA, the blackhole flags in `:jvm-opts`, and main namespace `closed-record.bench`.
2. `dev/bench/closed_record/bench.clj` (ns `closed-record.bench`), with `(defclosedrecord P [a b c])` and
   `(defrecord R [a b c])`. For each operation, it runs `criterium.bench/bench` with `:viewer :none` and
   `:limit-time-s 10` on `P`, then on `R`, in one JVM:
   - keyword lookup: `(:a x)`
   - field access: `(.a x)` with a type hint
   - `assoc` of a declared key: `(assoc x :a 2)`
   - `hash`: `(hash x)` of one instance (the cached path)
   - `=`: `(= x y)` with `x` and `y` two equal instances of the same type
   The expressions use local bindings, not vars, so that a var deref is not part of the measurement.
3. For each run, the mean and its 95% interval for one call: the `:point-estimate` and the two
   `:estimate-quantiles` values of the mean (Context, criterium API), each divided by the `:batch-size` of that run.
   The interval low is the `:alpha 0.025` value, and the interval high is the `:alpha 0.975` value.
4. The pass rule for each operation: the closed record mean is not more than 5% above the `defrecord` mean
   (`P mean / R mean ≤ 1.05`), or the two 95% intervals overlap (`P low ≤ R high` and `R low ≤ P high`). A closed
   record that is faster passes. The rule uses the values before rounding.
5. Output: one table with one row for each operation, in the order of Scope 2. The columns, in this order:
   - Operation.
   - `P` mean, and `P` 95% interval as `[low, high]`, in ns for one call, with 2 decimals.
   - `R` mean, and `R` 95% interval as `[low, high]`, in ns for one call, with 2 decimals.
   - `P / R`: the ratio `P mean / R mean`, a factor (not a percent) with 3 decimals. A value above 1 means that the
     closed record is slower.
   - Result: `pass` or `fail`.
   `clojure -M:bench` exits 0 if all operations pass, and 1 if not.
6. Change the S-03 text in `purpose.md` to the one-sided rule of Scope 4.
7. Update `README.md`, `embodiment.md` and `mementum/state.md` with this text:
   - `README.md`, section "Development": at the end of the section, add the line "Run the benchmarks against
     `defrecord`:" and, after it, the command `clojure -M:bench` as an indented code block. The
     README gives the bench command only, and no results.
   - `embodiment.md`, Artifacts table, row `dev/bench/`: "`closed_record/bench.clj` (`closed-record.bench`):
     criterium benchmarks of the five S-03 operations against `defrecord` (V-11)."
   - `embodiment.md`, Artifacts table, row `deps.edn`: "Clojure 1.11 dependency. Aliases `:test`, `:bench`,
     `:nrepl`, `:clj-1.12` and `:clj-1.13`. The `:test` alias has scry, `test.check` and malli (`metosin/malli`).
     The `:bench` alias has criterium (`criterium/criterium`)."
   - `mementum/state.md`, Status, line "Project skeleton": the alias list is `:test`, `:bench`, `:nrepl`,
     `:clj-1.12`, `:clj-1.13`.
   - `mementum/state.md`, Status: after the line "Tests: `clojure -M:test -m scry.cli`. ...", add the line
     "Bench: `clojure -M:bench` (V-11). It exits 1 if an operation fails."
   - `mementum/state.md`, Status, line "Verification plan done: ... Not done: ...", the V-11 status:
     - All operations pass: move V-11 to the done list.
     - One or more operations fail: keep V-11 in the not-done list, and add the failed operations and their
       `P / R` ratios from the run that `implementation.md` records (Acceptance 3), for example "Not done: V-11
       (the bench exists; `assoc` fails, `P / R` 1.140)."

## Out of scope

- The bench in the test suite or in CI. It is slow and its result depends on the machine.
- Other operations (`get`, `seq`, `conj`, `merge`, constructors) and other field counts.
- Runs on Clojure 1.12 and 1.13. `clojure -M:bench:clj-1.12` can work, but it is not an acceptance criterion.
- Changes to the emitter. If an operation fails (the probe suggests `assoc` can), do not change the emitter.
  Record the result in `implementation.md`, and complete the other scope items (Scope 6 and 7, with the V-11 status
  of Scope 7 for a failed operation). A fix is a new task.
- criterium 0.4.x, the criterium domain API (`criterium.domain`), other viewers, and allocation traces.

## Decisions

- criterium 0.5.x, not 0.4.x. It is the current line on Clojars, it has bootstrap intervals with an
  effective sample size, and it supports a compiler blackhole.
- `criterium.bench/bench` for each operation and type, not the domain API. The domain API needs an axis and an
  analysis plan. Ten `bench` calls and one comparison function are simpler.
- `:limit-time-s 10`: about 100 s for the ten runs.
- The mean, as S-03 says, not the median that the default viewer shows.
- Divide by the batch size of each run. The batch sizes of `P` and `R` can differ (observed: 2500 and 3333).
- A one-sided rule. S-03 says "within 5%", but the probe shows that a closed record can be faster. A faster
  closed record meets K-05, so it must pass.
- A `:bench` alias apart from `:test`. criterium is not a test dependency, and `dev/bench` is not a test path.
- A failed operation keeps V-11 not done. The bench exists, but S-03 is not met.
- `P / R` as a factor, so that the 5% rule reads directly from the table (`≤ 1.05`).
- `hash` measures the cached path, because both types cache the hash. The first-call cost is out of scope.

## Acceptance criteria

1. `deps.edn` has the `:bench` alias of Scope 1, and no other new dependency.
2. `clojure -M:bench` runs the five operations of Scope 2, prints the table of Scope 5 with the values of
   Scope 3, and its exit code follows the pass rule of Scope 4. It prints no blackhole warning.
3. `implementation.md` records the table of one run on Clojure 1.11.4, with the JVM and machine.
4. `clj-kondo --lint src test dev` reports no errors and no warnings. `cljfmt check src test dev` passes.
5. `purpose.md` S-03, `README.md`, `embodiment.md` and `mementum/state.md` are updated as in Scope 6 and 7.
