# Design steps

- [x] Architecture review: move the Scope 4 field declaration test out of `compiler_rules_test.clj`.
  - `compiler_rules_test.clj` is V-13 (embodiment.md). Its ns docstring says that its tests use only `defrecord`,
    `deftype`, `deftype*`, the reader and the compiler. It does not require `closed-record.core`. It fixes the
    Clojure rules K-06 to K-08, which the library depends on.
  - The Scope 4 test compares a `defclosedrecord` type with a `defrecord`. It checks the emitter output (D-01),
    not a compiler rule. In that file, it breaks the V-13 boundary.
  - Put the test in a test namespace of the closed type, for example `field_value_test.clj` (differential closed
    type / `defrecord` cases, V-09 type hints). Name the verification item that it supports (S-03 field access,
    V-11).
- [x] Architecture review: define the parent / child boundary of the fork harness.
  - Scope 1 says that each child "gives its per-call stats to the parent". The design does not say how: the child
    entry point, or the transport (stdout, file).
  - The bench rule (benchmarks.md, Code structure) keeps the pure layer in `closed-record.bench.report`, with no
    criterium and no side effects. Acceptance 2 tests "statistics over forks" with no child JVM.
  - Say: the child entry point (in `closed-record.bench`), the transport and its data format, and that the encode
    and decode of the child result are in the pure layer, so the suite can test them. Process start and I/O stay
    in `closed-record.bench`.
  - A stdout transport must ignore other output: the JVM prints the `CompileCommand: blackhole ...` notice to
    stdout at start.
- [ ] Architecture review: SCOPE_QUESTION: Scope 7 does not update the verification plan and test artifact rows
  that this task changes.
  - `qualities.md` V-11 says "criterium: lookup, `.field`, `assoc`, `hash`, `=`". After this task, S-03 adds
    `get`, 5 JVM forks, and a structural field declaration test that is not criterium. D-16 points to V-11 as the
    verification plan.
  - `embodiment.md` `bench_report_test.clj` row lists "per-call stats, pass rule, table and exit code". After this
    task, the pure layer also has the statistics over forks (and the child result format).
  - Out of scope excludes only the `qualities.md` rows that name a CI matrix. A human decides: add the V-11 row and
    the `bench_report_test.clj` row to Scope 7, or accept the gap.
- [x] Ambiguity review: say how forks are numbered and what "runs first" covers (Scope 1).
  - "In even forks, `P` runs first; in odd forks, `R` runs first." With forks 0–4, 3 forks start with `P`. With
    forks 1–5, 3 forks start with `R`. The design does not say which.
  - "Runs first" can mean: for each operation, `P` then `R`; or all six `P` runs, then all six `R` runs.
  - Give the fork numbering, and the run sequence in one fork (for example: for each operation in the Scope 3
    order, `P` then `R` in even forks).
- [x] Ambiguity review: say what happens when a child fork fails, and what the forks column shows (Scope 1, 5).
  - A child can exit non-zero or throw (for example the criterium divide by zero fault), or give no result.
  - The design does not say if the parent then stops with an error, or computes the result from fewer forks.
    Scope 2 fixes 5 fork means and 4 degrees of freedom.
  - Scope 5 adds "the number of forks" column. Say if it shows the configured count (always 5) or the number of
    forks that gave a result. Give its header text and its position in the table.
- [x] Ambiguity review: say what the field declaration test adds to the S-03 pass and the V-11 status.
  - Scope 7 S-03: "Field access has the timed row and the field declaration test." Scope 7 `state.md`: "V-11 is
    done only if all six operations pass." The exit code uses the timed rows only (Scope 5).
  - Say if field access passes S-03 when the timed `(.a x)` row fails and the declaration test passes, and if V-11
    can be done when the declaration test fails.
- [x] Ambiguity review: give the new text of the Scope 7 `benchmarks.md` and `embodiment.md` changes.
  - "`benchmarks.md`: the fork rule" does not say the rule. It can be the fork count, the alternate order, the
    t interval, one run of each call site in a JVM, or all of these. The Known limits item "A stable bench needs
    more (for example several JVM forks)" can stay or change.
  - "`embodiment.md`: the `dev/bench/` row" does not say the new text. The row now says "the five S-03
    operations" and does not name forks.
  - Acceptance 6 cannot check these changes without the text or its content items.
