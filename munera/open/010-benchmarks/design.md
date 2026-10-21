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
- `criterium/criterium` 0.4.6 is the latest stable release (Clojars, 2026-10-20). The 0.5.x releases are alpha.
- `criterium.core/quick-benchmark*` takes a function of no arguments and an options map. The result has
  `:mean` as `[estimate [lower upper]]` in seconds, with a 95% bootstrap confidence interval.
- `hash` and `hasheq` cache the result in a field (`__hash`, `__hasheq`), as `defrecord` does.

### Observed facts (2026-10-20, Clojure 1.11.4, criterium 0.4.6, OpenJDK 25.0.2, arm64, commit `9ed5d8e`)

`(defclosedrecord P [a b c])`, `(defrecord R [a b c])`, one JVM, `quick-benchmark*` of a keyword lookup `(:a x)`:

| Type | Mean | 95% interval |
|---|---|---|
| `P` | 1.03 ns | 1.03–1.04 ns |
| `R` | 2.48 ns | 2.47–2.48 ns |

The closed record was faster. At this scale, the result can depend on JIT state and on the run order.

## Scope

In scope:

1. A `:bench` alias in `deps.edn`: extra path `dev/bench`, extra dependency `criterium/criterium` 0.4.6, and main
   namespace `closed-record.bench`.
2. `dev/bench/closed_record/bench.clj` (ns `closed-record.bench`), with `(defclosedrecord P [a b c])` and
   `(defrecord R [a b c])`. For each operation, it runs `quick-benchmark*` on `P`, then on `R`, in one JVM:
   - keyword lookup: `(:a x)`
   - field access: `(.a x)` with a type hint
   - `assoc` of a declared key: `(assoc x :a 2)`
   - `hash`: `(hash x)` of one instance (the cached path)
   - `=`: `(= x y)` with `x` and `y` two equal instances of the same type
   Each function closes over local values, not vars, so that a var deref is not part of the measurement.
3. The pass rule for each operation: the closed record mean is not more than 5% above the `defrecord` mean, or the
   two 95% intervals overlap. A closed record that is faster passes.
4. Output: one table row for each operation, with both means, both intervals, the ratio and pass or fail.
   `clojure -M:bench` exits 0 if all operations pass, and 1 if not.
5. Change the S-03 text in `purpose.md` to the one-sided rule of Scope 3.
6. Update `README.md` (Development: the bench command), `embodiment.md` (`dev/bench/` row, `deps.edn` row names
   the `:bench` alias) and `mementum/state.md` (V-11 done, the bench command).

## Out of scope

- The bench in the test suite or in CI. It is slow and its result depends on the machine.
- Other operations (`get`, `seq`, `conj`, `merge`, constructors) and other field counts.
- Runs on Clojure 1.12 and 1.13. `clojure -M:bench:clj-1.12` can work, but it is not an acceptance criterion.
- Changes to the emitter. If an operation fails, record the result in `implementation.md` and stop. A fix is a
  new task.
- criterium 0.5.x.

## Decisions

- A one-sided rule. S-03 says "within 5%", but the probe shows that a closed record can be faster. A faster
  closed record meets K-05, so it must pass.
- `quick-benchmark*`, not `benchmark*`. The full run takes about one minute for each measurement, thus ten minutes
  for the command. The confidence interval of the quick run is sufficient for the rule.
- A `:bench` alias apart from `:test`. criterium is not a test dependency, and `dev/bench` is not a test path.
- `hash` measures the cached path, because both types cache the hash. The first-call cost is out of scope.

## Acceptance criteria

1. `deps.edn` has the `:bench` alias of Scope 1, and no other new dependency.
2. `clojure -M:bench` runs the five operations of Scope 2, prints the table of Scope 4, and its exit code follows
   the pass rule of Scope 3.
3. `implementation.md` records the table of one run on Clojure 1.11.4, with the JVM and machine.
4. `clj-kondo --lint src test dev` reports no errors and no warnings. `cljfmt check src test dev` passes.
5. `purpose.md` S-03, `README.md`, `embodiment.md` and `mementum/state.md` are updated as in Scope 5 and 6.
