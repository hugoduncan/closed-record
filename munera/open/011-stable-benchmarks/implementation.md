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

## Plan review (2026-10-23, sha `0da1f23`)

- Ambiguity review added 5 new design steps.
  - Classified as actionable: the Slice 3 notice check is a full 300 s run, so it can be "the first full run of
    the finished harness" (Scope 6) before Slice 5. The order of these runs decides which output goes to
    `implementation.md`.
  - Not filed (an option for the plan, not an ambiguity): the plan doubles the call sites to alternate the order
    (24 `bench-op` sites, 12 run in each JVM). One thunk for each of the 12 call sites, called in the fork order,
    also runs each call site once, with no macro risk.
- Inconsistency review added 2 new design steps.
  - Correction: the ambiguity item on the Slice 2 command said `:e` is a user alias. It is defined inline with
    `-Sdeps` (`benchmarks.md`, Code structure). The design-steps item is corrected.
  - The thunk option above wraps a whole `bench-op` form (its literal expression stays at its own call site). It
    does not conflict with the `benchmarks.md` rule "the bench helper is a macro, not a function that takes a
    thunk", which is about the measured expression.
  - For Slice 5: `benchmarks.md` Known limits says to capture each full run with `clojure -M:bench | tee <file>`.
    `tee` gets only stdout: use `2>&1` so that it also captures the child stderr and an error on stderr. `tee`
    hides the exit code, so record it with `${PIPESTATUS[0]}`.
- For the design-steps work (after the plan review):
  - Principles: the 7 plan review items change `plan.md` and `steps.md` (not yet started, so edit in place).
    Change `design.md` only if an answer changes the design (Scope 6 acceptance run, Scope 7 document list). Keep
    the SCOPE_QUESTION open; do not decide it.
  - Do these items together, because they change the same text:
    - "`Forks` value" and "`decode-result` contract" (plan 1, Slice 1, `report.clj`).
    - "`decode-result` contract" and "decode each result file when its child exits" (plan 3, Slice 3: the parent
      prints the cause from the error value after each fork).
    - "which run is the acceptance run" and "manual verification commands" (Slice 3 verification, Slice 5).
  - Options found during the review:
    - `Forks`: `exit-code` destructures `[_ p r]`, so a fourth element in each `fork-stats` entry does not break it.
    - Acceptance run: move the "blackhole notice for each child" check from Slice 3 to Slice 5, on the recorded
      acceptance output. Then Slice 3 needs no full run. The failure check (forced fork 0 failure) is short.
  - Paths for the stale-text item: `dev/bench/closed_record/bench.clj` (ns docstring, `run` docstring "the five
    operations"), `dev/bench/closed_record/bench/report.clj` (ns docstring),
    `test/closed_record/bench_report_test.clj` (ns docstring "inject `last-bench`-shaped data"; the new tests
    also inject child results), `mementum/knowledge/benchmarks.md` (Code structure, "a 100 s run").

## Plan design steps (2026-10-23)

- Done: the 7 plan review items, in `plan.md` and `steps.md` only. `design.md` does not change. The SCOPE_QUESTION
  stays open (Slice 6 has a conditional step for it).
- Choices made: `Forks` = fourth element `n` of each `fork-stats` entry (`row [op p r n]`); `decode-result` returns
  `{:result v}` / `{:error msg}`, no throw; the child gets all parent input arguments; `parent` takes a
  `child-cmd` function, so the failure path is verified with `sh` / `rm` / `true` / `cp` commands and no JVM; the
  blackhole notice check and the exit-2 rule are in Slice 5; the harness is finished at the end of Slice 3.
- Observed (`clojure -M:bench:e`, 2026-10-23):
  - `-Dclojure.basis=.cpcache/<hash>.basis` is a persistent file with a relative path. A child started with the
    same working directory (the `ProcessBuilder` default) can use it, so no filter is needed.
  - The parent JVM also prints `CompileCommand: blackhole criterium/blackhole/Blackhole.consume bool blackhole =
    true` (the alias `:jvm-opts`), so a full run has 6 such lines.
  - `clojure.edn/read-string ""` gives `nil`; `"[1"` throws `RuntimeException` (EOF).
- Tests that call `report/row` with 3 args (`pass?-test` uses `(nth (report/row "op" p r) 3)`, `row-test`,
  `table-test`) must get the `n` argument in Slice 1.

## Plan review 2 (2026-10-23, after the plan design steps, sha `df35caa`)

- Ambiguity review: no ambiguity review feedback.
  - Not filed (the implementer can choose; no design effect):
    - `-main` with 1 or 3+ args: the plan gives only 0 (parent) and 2 (child). A usage error with a non-0 exit
      is the safe choice; a parent then sees a fork failure.
    - The `run` signature: it needs the fork number for the parity. The operation strings in `run` can come from
      `report/ops`, so that `decode-result` cannot reject a child for a text mismatch.
- Inconsistency review: no inconsistency review feedback.
  - Not filed (minor wording; the explicit rule governs):
    - plan Risks says a child option fault is "a fork failure (exit 2) in the acceptance run". Plan 5 and steps
      Slice 5 say an exit-2 run is not the acceptance run. Follow Slice 5.
    - The plan puts the `bench.clj` ns docstring in item 2 (child mode). The steps put it in Slice 3. Do it in
      Slice 3, when exit 2 exists.
  - Checked, still true after the task (no change needed): `mementum/state.md` "It exits 1 if an operation
    fails", `README.md` `clojure -M:bench`, `dev-tooling.md` `:bench` line.
- For the design-steps work (after plan review 2):
  - No new design steps. The only open item is the SCOPE_QUESTION. Do not tick it and do not edit `design.md`
    for it without a human decision (the "Design review 2" notes say what to change if it is decided).
  - With no decision, the plan and steps are ready for `implement-task`. Steps Slice 6 has the conditional step.

## Implementation pass 1 (2026-10-23)

### Acceptance run

Clojure 1.11.4, criterium 0.5.251-ALPHA, OpenJDK 25.0.2 (build 25.0.2+10-69), Apple M5 Max (arm64), commit
`61b630a`. Command: `clojure -M:bench 2>&1 | tee <file>; echo "exit ${PIPESTATUS[0]}"`. Complete output:

```text
CompileCommand: blackhole criterium/blackhole/Blackhole.consume bool blackhole = true
CompileCommand: blackhole criterium/blackhole/Blackhole.consume bool blackhole = true
CompileCommand: blackhole criterium/blackhole/Blackhole.consume bool blackhole = true
CompileCommand: blackhole criterium/blackhole/Blackhole.consume bool blackhole = true
CompileCommand: blackhole criterium/blackhole/Blackhole.consume bool blackhole = true
CompileCommand: blackhole criterium/blackhole/Blackhole.consume bool blackhole = true
| Operation      | P mean [low, high] ns | R mean [low, high] ns | P / R | Result | Forks |
|----------------|-----------------------|-----------------------|-------|--------|-------|
| (:a x)         | 0.98 [0.95, 1.01]     | 1.00 [0.94, 1.05]     | 0.982 | pass   | 5     |
| (get x :a)     | 4.30 [3.47, 5.13]     | 4.10 [3.83, 4.37]     | 1.049 | pass   | 5     |
| (.a x)         | 0.33 [0.31, 0.35]     | 0.33 [0.29, 0.37]     | 0.995 | pass   | 5     |
| (assoc x :a 2) | 7.93 [7.82, 8.05]     | 6.65 [6.32, 6.97]     | 1.193 | fail   | 5     |
| (hash x)       | 4.41 [4.24, 4.59]     | 4.38 [4.19, 4.57]     | 1.007 | pass   | 5     |
| (= x y)        | 5.06 [3.70, 6.42]     | 5.20 [3.89, 6.50]     | 0.974 | pass   | 5     |
exit 1
```

- 6 blackhole lines (parent + 5 children), `Forks` 5. Exit 1: `assoc` fails (1.193). V-11 not done.

### Notes for review

- `run` takes `[fork limit-s]`; it builds 12 thunks (one per `bench-op` call site) and calls each pair in parity
  order (`in-order`). Operation texts come from `report/ops`.
- `-main`: 0 args → `parent`; 2 args → child (exit 0 after `spit`); other → usage on stderr, exit 64.
- The parent keeps the temp result file (does not delete it before the child), so `true` gives an empty-file
  decode error ("not a vector of 6 entries"), as in the steps.
- Slice 3 `parent` checks (exit 3, rm, true, truncated EDN, valid fixture) gave the expected causes and 2 / 0.
  Manual child run with fork 1: exit 0, decode `{:result ...}`.
- Field test: `FL [a ^long n]` vs `DFL`; compares `[name type modifiers]` of non-static `getDeclaredFields`.
  Passes on 1.11, 1.12, 1.13.
- SCOPE_QUESTION still open: `qualities.md` V-11 row and the `embodiment.md` `bench_report_test.clj` row not changed.
- Final: scry 91 tests / 345 assertions, 0 failures, on all three aliases; clj-kondo 0/0; cljfmt ok.
