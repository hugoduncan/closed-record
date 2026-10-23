# 011 stable benchmarks

## Intent

Make the V-11 result repeatable, so that one bench run can show that S-03 holds or does not hold. Measure in
several JVMs, and replace the field access timing, which measures nothing, with a check that does not depend on
time. Record in D-16 that CI is deferred.

## Problem

Task 010 made `clojure -M:bench`, but one run does not give a reliable result:

- Ratios near 1.05 change from run to run on one machine. `=`: 1.086 (fail) in the acceptance run, then 0.957
  (pass). `assoc`: 1.111 in a 1 s run, 1.011 in a 10 s run. One JVM gives one JIT state and one run order. The
  bootstrap interval of one run does not include the variation between JVMs, so it is too narrow.
- `(.a ^P x)` gives about 0.06 ns for `P` and `R`. The JIT hoists the field read, so the row passes with no signal.
- D-16 gives the verification scope "CI matrix Clojure 1.11, 1.12, 1.13". The repository has no CI. Task 010 left
  this as an open question (SCOPE_QUESTION in `munera/closed/010-benchmarks/design-steps.md`). The user decision
  (2026-10-20): delay CI.
- V-11 is not done (`mementum/state.md`).

## Context

- Task 010: `munera/closed/010-benchmarks/` (`implementation.md`: the runs and the variance).
- Knowledge: `mementum/knowledge/benchmarks.md` (bench rules, known limits), `mementum/knowledge/dev-tooling.md`.
- Bench: `dev/bench/closed_record/bench.clj` (`closed-record.bench`: `bench-op`, `run`, `-main`) and
  `dev/bench/closed_record/bench/report.clj` (`closed-record.bench.report`: per-call stats, pass rule, table, exit
  code). Tests of the pure layer: `test/closed_record/bench_report_test.clj`.
- Architecture: S-03 in `ramora/defclosedrecord/purpose.md`, V-11 in `qualities.md`, D-16 in
  `decisions/verification.md`, D-01 (the `defrecord` field layout).
- criterium 0.5.251-ALPHA (`org.hugoduncan/criterium`) has no fork support.

### Observed facts (2026-10-20, Clojure 1.11.4, OpenJDK 25.0.2, arm64, commit `80492fb`)

- `(defclosedrecord P [a b c ^long n])` and `(defrecord R [a b c ^long n])` declare the same instance fields, in
  the same order, with the same modifiers and types: `a b c` (`public final Object`), `n` (`public final long`),
  `__meta __extmap` (`public final Object`), `__hash __hasheq` (`int`, no modifiers). Only the static constant
  fields differ.
- `(.a ^P x)` compiles to a `getfield` of the field `a`. With the same field declaration, `P` and `R` give the same
  instruction.
- A field read through a `volatile!` in the bench body gave unstable values (3 s runs: `P` 0.12 ns, `R` 0.03 ns,
  the deref alone 2.23 ns). criterium hoists the arguments of the expression, so the result is not a field read.

## Scope

In scope:

1. Forks. `clojure -M:bench` starts 5 child JVMs, one after the other. Each child has the classpath and JVM
   options of the parent, runs each operation once on `P` and once on `R`, and gives its per-call stats to the
   parent. In even forks, `P` runs first; in odd forks, `R` runs first. The run limit is 5 s.
2. Statistics. For each operation and type, the mean is the mean of the 5 per-call fork means. The 95% interval is
   the Student t interval of the 5 fork means (4 degrees of freedom). The pass rule of S-03 does not change, but it
   uses these values.
3. Operations: `(:a x)`, `(assoc x :a 2)`, `(hash x)`, `(= x y)`. Remove the `(.a x)` row.
4. Field access. A test in `test/closed_record/compiler_rules_test.clj`: for a closed type and a `defrecord` with
   the same fields (one untyped field and one `^long` field), the non-static declared fields are equal in name,
   order, type and modifiers.
5. Output. The table of task 010, with one more column: the number of forks. The exit code does not change: 0 if
   all operations pass, 1 if not.
6. Acceptance run. The first full run of the finished harness is the acceptance run. Its complete output and
   its exit code go to `implementation.md`. No later run replaces it.
7. Documents:
   - `purpose.md` S-03: the means and intervals are over 5 JVM forks. Field access is verified by the field
     declaration test, not by time.
   - D-16 Scope: the test suite runs on Clojure 1.11, 1.12 and 1.13 (aliases). The bench runs on Clojure 1.11.
     CI is deferred: all runs are manual.
   - `mementum/state.md`: the V-11 status from the acceptance run. V-11 is done only if all four operations pass.
   - `mementum/knowledge/benchmarks.md`: the fork rule, and the resolved known limits.
   - `embodiment.md`: the `dev/bench/` row.

## Out of scope

- CI. The user decided to delay it.
- Changes to the emitter. If an operation fails in the acceptance run, record it in `state.md` (V-11 not done,
  with the ratio) and complete the documents. A fix is a new task.
- Bench runs on Clojure 1.12 and 1.13.
- Other operations, a different fork count from the command line, and a change of criterium version.
- The `qualities.md` rows that name a CI matrix as evidence. They state the target, not the current state.

## Decisions

- Forks in the bench, not in criterium. criterium 0.5 has no fork option. The parent JVM only starts the children
  and computes the result, so it does no measurement.
- 5 forks, 8 runs each, 5 s each: about 200 s of measurement, two times the 100 s of task 010. The interval uses the
  variation between JVMs, which is the variation that changed the result in task 010.
- Alternate the run order, so that an order effect does not favour one type.
- The Student t interval over fork means, not the bootstrap interval of one run. The fork means are independent;
  the samples inside one run are not independent of the JIT state of that run.
- Field access by structure, not by time. A timed field read that the JIT cannot hoist adds other work that is
  larger than the read (observed: the `volatile!` probe). The same field declaration gives the same `getfield`, so
  the structural test shows parity and does not change from run to run.
- D-16 records the deferral. Without it, D-16 states a scope that the project does not meet.

## Acceptance criteria

1. `clojure -M:bench` runs 5 forks with alternating order and prints the table of Scope 5 for the four operations
   of Scope 3, with the statistics of Scope 2. Its exit code follows the pass rule.
2. The pure layer (statistics over forks, pass rule, table) has tests in the suite, with no criterium and no
   child JVM.
3. The field declaration test of Scope 4 passes on the default alias, `:clj-1.12` and `:clj-1.13`.
4. `implementation.md` has the complete output and the exit code of the acceptance run, with the JVM and machine.
5. `clojure -M:test -m scry.cli` on the three aliases: zero failures and errors. `clj-kondo --lint src test dev`:
   no errors, no warnings. `cljfmt check src test dev` passes.
6. S-03, D-16, `state.md`, `benchmarks.md` and `embodiment.md` are updated as in Scope 7.
