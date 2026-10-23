# 011 stable benchmarks

## Intent

Make the V-11 result repeatable, so that one bench run can show that S-03 holds or does not hold. Measure in
several JVMs. Add a structural check of field access, which does not depend on time, beside the timed row. Record
in D-16 that CI is deferred.

## Problem

Task 010 made `clojure -M:bench`, but one run does not give a reliable result:

- Ratios near 1.05 change from run to run on one machine. `=`: 1.086 (fail) in the acceptance run, then 0.957
  (pass). One JVM gives one JIT state and one run order. The bootstrap interval of one run does not include the
  variation between JVMs, so it is too narrow.
- Task 010 used criterium 0.5.243-ALPHA. Its measured loop let the JIT lift a pure expression out of the loop
  (criterium #155). `(.a x)` gave about 0.06 ns, and the reads of the fields of `x` in `assoc` were folded. Thus the
  task 010 results for cheap operations are not valid. criterium 0.5.251-ALPHA fixes this (commit `e0cb996`).
- D-16 gives the verification scope "CI matrix Clojure 1.11, 1.12, 1.13". The repository has no CI. Task 010 left
  this as an open question (SCOPE_QUESTION in `munera/closed/010-benchmarks/design-steps.md`). The user decision
  (2026-10-20): delay CI.
- V-11 is not done (`mementum/state.md`).

## Context

- Task 010: `munera/closed/010-benchmarks/` (`implementation.md`: the runs and the variance).
- Knowledge: `mementum/knowledge/benchmarks.md` (bench rules, criterium 0.5.251 section, known limits),
  `mementum/knowledge/dev-tooling.md`.
- Bench: `dev/bench/closed_record/bench.clj` (`closed-record.bench`: `bench-op`, `run`, `-main`) and
  `dev/bench/closed_record/bench/report.clj` (`closed-record.bench.report`: per-call stats, pass rule, table, exit
  code). Tests of the pure layer: `test/closed_record/bench_report_test.clj`.
- Architecture: S-03 in `ramora/defclosedrecord/purpose.md`, V-11 in `qualities.md`, D-16 in
  `decisions/verification.md`, D-01 (the `defrecord` field layout).
- criterium 0.5.251-ALPHA (`org.hugoduncan/criterium`, in the `:bench` alias):
  - It reads each argument of the measured expression through an opaque barrier on each iteration. Literal
    arguments (`:a`, `2`) also go through the barrier. The reported time includes the barrier cost, which criterium
    does not subtract.
  - It has no fork support.
  - When one `bench` call site runs a second time in one JVM, it can throw `ArithmeticException: Divide by zero`
    (`criterium.collect/batch-size`, the single-call estimate is 0 ns). 0.5.243 has the same fault.

### Observed facts

2026-10-20, Clojure 1.11.4, OpenJDK 25.0.2, arm64, commit `80492fb`:

- `(defclosedrecord P [a b c ^long n])` and `(defrecord R [a b c ^long n])` declare the same instance fields, in
  the same order, with the same modifiers and types: `a b c` (`public final Object`), `n` (`public final long`),
  `__meta __extmap` (`public final Object`), `__hash __hasheq` (`int`, no modifiers). Only the static constant
  fields differ. So `(.a ^P x)` and `(.a ^R x)` compile to the same `getfield` instruction.

2026-10-23, Clojure 1.11.4, criterium 0.5.251-ALPHA, OpenJDK 25.0.2, Apple M5 Max (arm64), commit `e0cb996`.
One JVM, `clojure -M:bench` (10 s for each run), exit code 1:

| Operation | P mean ns | R mean ns | P / R | Result |
|---|---|---|---|---|
| `(:a x)` | 0.95 | 0.94 | 1.016 | pass |
| `(.a x)` | 0.30 | 0.36 | 0.830 | pass |
| `(assoc x :a 2)` | 11.02 | 6.71 | 1.644 | fail |
| `(hash x)` | 4.23 | 4.41 | 0.959 | pass |
| `(= x y)` | 3.65 | 3.90 | 0.936 | pass |

- `assoc` stays slow when a macro keeps `:a` literal (3 s runs: `P` 8.62 ns, `R` 5.97 ns, `P / R` 1.44). Thus the
  gap is a cost of the emitter, not of the barrier on the key.
- A `(do x)` body with one reference argument gives about 0.11 ns: the barrier cost on this machine.

## Scope

In scope:

1. Forks. `clojure -M:bench` starts 5 child JVMs, one after the other. Each child has the classpath and JVM
   options of the parent, runs each operation once on `P` and once on `R`, and gives its per-call stats to the
   parent. In even forks, `P` runs first; in odd forks, `R` runs first. The run limit is 5 s.
2. Statistics. For each operation and type, the mean is the mean of the 5 per-call fork means. The 95% interval is
   the Student t interval of the 5 fork means (4 degrees of freedom). The pass rule of S-03 does not change, but it
   uses these values.
3. Operations: the five operations of task 010, `(:a x)`, `(.a x)`, `(assoc x :a 2)`, `(hash x)` and `(= x y)`,
   and `(get x :a)`. Order: `(:a x)`, `(get x :a)`, `(.a x)`, `(assoc x :a 2)`, `(hash x)`, `(= x y)`.
4. Field access, structural check. A test in `test/closed_record/compiler_rules_test.clj`: for a closed type and a
   `defrecord` with the same fields (one untyped field and one `^long` field), the non-static declared fields are
   equal in name, order, type and modifiers.
5. Output. The table of task 010, with one more column: the number of forks. The exit code does not change: 0 if
   all operations pass, 1 if not.
6. Acceptance run. The first full run of the finished harness is the acceptance run. Its complete output and
   its exit code go to `implementation.md`. No later run replaces it.
7. Documents:
   - `purpose.md` S-03: add `get` to the operations. The means and intervals are over 5 JVM forks. Field access
     has the timed row and the field declaration test.
   - D-16 Scope: the test suite runs on Clojure 1.11, 1.12 and 1.13 (aliases). The bench runs on Clojure 1.11.
     CI is deferred: all runs are manual.
   - `mementum/state.md`: the V-11 status from the acceptance run. V-11 is done only if all six operations pass.
     Replace the task 010 ratios, which are not valid.
   - `mementum/knowledge/benchmarks.md`: the fork rule.
   - `embodiment.md`: the `dev/bench/` row.

## Out of scope

- CI. The user decided to delay it.
- Changes to the emitter. The `assoc` gap (observed `P / R` 1.44–1.64) is expected to fail the acceptance run.
  Record it in `state.md` (V-11 not done, with the ratio) and complete the documents. The fix is a new task.
- Subtraction of the barrier cost. criterium does not measure a baseline (criterium #159). The cost is the same
  for `P` and `R`.
- Bench runs on Clojure 1.12 and 1.13.
- Other operations (`seq`, `conj`, `merge`, constructors), a different fork count from the command line, and a
  change of criterium version.
- The `qualities.md` rows that name a CI matrix as evidence. They state the target, not the current state.

## Decisions

- Forks in the bench, not in criterium. criterium 0.5 has no fork option. The parent JVM only starts the children
  and computes the result, so it does no measurement. Each child runs each `bench` call site once, so the divide by
  zero fault does not occur.
- 5 forks, 12 runs each, 5 s each: about 300 s of measurement. The interval uses the variation between JVMs, which
  is the variation that changed the result in task 010.
- Alternate the run order, so that an order effect does not favour one type.
- The Student t interval over fork means, not the bootstrap interval of one run. The fork means are independent;
  the samples inside one run are not independent of the JIT state of that run.
- Add `get`. Keyword lookup uses a keyword call site; `get` uses `RT.get` → `valAt`. In criterium 0.5.251 the key
  `:a` goes through the barrier, so `valAt` dispatches with a `case` on a key that the JIT does not know. This
  is the same dispatch as in `assoc`, so the row shows whether lookup has the cost of the `assoc` gap.
- Field access by time and by structure. With criterium 0.5.251, the timed row measures a real field read. The
  structural test shows the reason for parity (the same field declaration, thus the same `getfield`), and it does
  not change from run to run.
- The barrier cost stays in the result. It adds the same time to `P` and `R`, so it moves `P / R` a little toward
  1. For `(.a x)` this effect is largest, because the barrier is a large part of the time.
- D-16 records the deferral. Without it, D-16 states a scope that the project does not meet.

## Acceptance criteria

1. `clojure -M:bench` runs 5 forks with alternating order and prints the table of Scope 5 for the six operations
   of Scope 3, with the statistics of Scope 2. Its exit code follows the pass rule.
2. The pure layer (statistics over forks, pass rule, table) has tests in the suite, with no criterium and no
   child JVM.
3. The field declaration test of Scope 4 passes on the default alias, `:clj-1.12` and `:clj-1.13`.
4. `implementation.md` has the complete output and the exit code of the acceptance run, with the JVM and machine.
5. `clojure -M:test -m scry.cli` on the three aliases: zero failures and errors. `clj-kondo --lint src test dev`:
   no errors, no warnings. `cljfmt check src test dev` passes.
6. S-03, D-16, `state.md`, `benchmarks.md` and `embodiment.md` are updated as in Scope 7.
