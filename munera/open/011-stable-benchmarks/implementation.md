# Implementation

## Design review

- Architecture sources: `ramora/META.md` and `doc/architecture.md` do not exist. The architecture is
  `ramora/INDEX.md` → `ramora/defclosedrecord.md` → `ramora/defclosedrecord/*.md` (purpose, principles,
  decisions/, embodiment, qualities).
- Architecture review (2026-10-20, sha `da8f624`) added 3 new design steps (1 SCOPE_QUESTION).
  - The field test location finding is actionable, because the V-13 file is Clojure-only by its docstring and
    embodiment row. D-01 output checks belong with the closed-type tests.
- Ambiguity review (2026-10-20) added 4 new design steps.
  - Not filed: `Class/getDeclaredFields` order is not specified by the Javadoc. HotSpot gives declaration
    order. The Scope 4 "order" check depends on that; a plan can compare by the order of the constructor
    parameters instead, if this is a concern.
- Inconsistency review: no inconsistency review feedback.
  - Correction: the architecture and ambiguity review notes above say 2026-10-20. The commits are 2026-10-23.
  - Checked: the design dates and commits (`80492fb` 2026-10-20, `e0cb996` 2026-10-23), the observed numbers
    against `benchmarks.md` and `state.md`, the D-01 layout against the observed fields, the bench and report
    names. All agree.
  - Emitter fact (`impl/jvm.clj` `key-case`) for the Scope 3 `get` rationale and the later `assoc` task: the
    closed type uses `case` on the key in `valAt` and in `assoc`. Clojure 1.11 `defrecord` uses `case` in `valAt`
    and `condp identical?` in `assoc`. So the `get` row compares `case` with `case`. The `assoc` row compares
    `case` with `condp identical?`.
- For the design-steps work (after the design review):
  - Principles: edit `design.md` in place, with no revision markers. Write in ASD-STE100 style. Do not
    resolve the SCOPE_QUESTION in `design.md`; a human decides it. Task 010 left its SCOPE_QUESTION
    unchecked (`munera/closed/010-benchmarks/design-steps.md`).
  - Do these items together, because they change the same text:
    - The architecture "field test location" item and the ambiguity "S-03 pass and V-11 status" item (Scope 4
      and the Scope 7 S-03 text).
    - The architecture "parent / child boundary" item and the ambiguity "child fork fails" item (Scope 1, 2
      and 5).
    - The ambiguity "Scope 7 text" item and the SCOPE_QUESTION. If the human adds the
      `bench_report_test.clj` row, the new `embodiment.md` text covers that row too.
  - Paths:
    - Test files: `test/closed_record/compiler_rules_test.clj` (ns docstring: Clojure only) and
      `test/closed_record/field_value_test.clj` (requires `closed-record.core`; differential V-06, V-08, V-09).
    - Architecture rows: `ramora/defclosedrecord/qualities.md` (V-11 row) and
      `ramora/defclosedrecord/embodiment.md` (Artifacts table).
    - Bench rules: `mementum/knowledge/benchmarks.md` ("Known limits", "Code structure").
    - Bench code: `dev/bench/closed_record/bench/report.clj` (`per-call-stats` stays as the result of one
      child). The child JVMs need the blackhole `:jvm-opts` of the `deps.edn` `:bench` alias.

## Design steps (2026-10-23)

- Done: the 6 actionable design steps, in `design.md` Scope 1, 2, 4, 5, 7, Decisions and Acceptance 1, 2.
- Deferred to the user: the SCOPE_QUESTION (V-11 row in `qualities.md`, `bench_report_test.clj` row in
  `embodiment.md`). It stays unchecked. Scope 7 does not change these rows.
- Choices made for the design steps: forks 0–4; order alternates for each operation; EDN result file (not stdout);
  a failed fork stops the run with exit 2 (new exit code); `Forks` column last; field access passes only if
  the timed row and the declaration test both pass; t = 2.776 (no stats library in the pure layer).
- Child JVM facts (observed with `clojure -M:bench:e`, `:e` clears `:main-opts`):
  - `RuntimeMXBean/getInputArguments` gives `-XX:-OmitStackTraceInFastThrow`, the two blackhole options and
    `-Dclojure.basis=...`. The classpath is in `java.class.path`; the `java` binary is under `java.home`/`bin`.
  - The `:bench` `:main-opts` is `-m closed-record.bench`, so the parent `-main` gets no arguments. A child can
    start as `java <opts> -cp <cp> clojure.main -m closed-record.bench <fork> <file>`.
- Code facts: `run` has 10 fixed `bench-op` call sites in `[op p r]` order. To alternate the order at run time,
  each call site must still run once in a JVM. `report/header` and `report/row` have 5 cells; the `Forks` column
  changes both and the `bench_report_test.clj` table tests.

## Design review 2 (2026-10-23, after the design steps, sha `37834a7`)

- Architecture review: no architectural review feedback.
  - Checked against the new Scope 1, 4, 5 and 7: the bench layers (`benchmarks.md` Code structure), the V-13
    boundary, the D-13 dependency rules, K-05, D-16 and the README bench text. The V-11 row and the
    `bench_report_test.clj` row stay the open SCOPE_QUESTION. No new item for them.
- Ambiguity review: no ambiguity review feedback.
  - Not filed (the design implies it): the child exits 0 when it has written its result. The current `-main` exits
    with `report/exit-code`. If the child keeps that, the expected `assoc` fail gives exit 1, and Scope 1 then
    fails each fork with exit 2.
  - Not filed: exit 2 comes from the parent (Scope 1). `report/exit-code` stays 0 / 1 over the timed rows.
- Inconsistency review: no inconsistency review feedback.
  - Checked: the Scope 7 quoted text against `benchmarks.md` ("A stable bench needs more ...", "Only `-main` has
    side effects ..."), the run count (6 × 2 × 5 s × 5 forks = 300 s), the fork order count (3 / 2), the
    Acceptance 2 and Scope 7 `embodiment.md` pure-layer lists, and the observed numbers. All agree.
  - Not filed: Scope 2 "the pass rule of S-03 does not change" means the per-row rule (`report/pass?`). The new
    field access condition (Scope 7) is on S-03 and V-11 status, not on the row rule.
- For the design-steps work (after design review 2):
  - Open design steps: only the SCOPE_QUESTION. It needs a human decision. Do not tick it and do not edit
    `design.md` for it without that decision. If no decision comes, the design is ready for `create-task-plan`.
  - If the human adds the rows to Scope 7:
    - The V-11 row (`ramora/defclosedrecord/qualities.md`) must agree with the S-03 text of Scope 7: six
      operations with `get`, 5 forks, and the field declaration test in `field_value_test.clj`.
    - The `bench_report_test.clj` row (`ramora/defclosedrecord/embodiment.md`) must use the same pure-layer list
      as Acceptance 2.
    - Add both rows to Acceptance 6.
