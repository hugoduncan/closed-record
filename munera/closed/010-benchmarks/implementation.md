# Implementation

## Design architecture review (2026-10-20)

- Architecture sources: `ramora/defclosedrecord/` only. `ramora/META.md` and `doc/architecture.md` do not exist.
- Architecture review added 1 new design step (SCOPE_QUESTION: D-16 CI scope against the manual 1.11-only bench).
- Checked with no finding: `:bench` alias apart from `:test` (embodiment `deps.edn` row), K-01 (top-level Clojure
  1.11.4 has precedence over the criterium pom), bench uses public names only (embodiment boundaries), one-sided
  S-03 still meets O-03 and K-05 ("Metric: S-03").

## Design ambiguity review (2026-10-20)

- Ambiguity review added 2 new design steps.

## Design inconsistency review (2026-10-20)

- No inconsistency review feedback.

## Notes for the design-steps fix (2026-10-20)

- The SCOPE_QUESTION step needs a human decision. Do not add D-16 to Scope 7 without it.
- Fix the other steps by clarification in `design.md` only. Do not move the Scope/Out of scope boundary. The
  "stop" fix must not add emitter work.
- Write the ratio so that the 5% test of Scope 4 reads from it directly (`P / R ≤ 1.05`).
- The V-11 status text goes to `mementum/state.md` line "Verification plan done: ... Not done: V-11."
- K-05 (`ramora/defclosedrecord/context.md`) says "equal". This was left on purpose: its metric is S-03. Do not
  file it again.

## Design-steps fix (2026-10-20)

- SCOPE_QUESTION (D-16 CI scope) deferred to the user. Left unchecked. Scope 7 does not name D-16.
- "Stop" fixed: no emitter change, but Scope 6 and 7 complete. A failed operation keeps V-11 in the `state.md`
  not-done list, with the failed operations and `P / R` ratios. The task can close with a failed operation:
  no acceptance criterion needs all operations to pass.
- Table fixed in Scope 5. Interval low/high map to `:alpha 0.025`/`0.975` (Scope 3). Overlap test is inclusive.
  Pass rule uses unrounded values, so a printed `P / R` of `1.050` can still fail; the overlap can pass it.

## Design architecture review, pass 2 (2026-10-20)

- No new architectural review feedback. The D-16 SCOPE_QUESTION stays open for the user. Not filed again.
- Checked the design-steps fix with no finding: a failed operation is recorded in `state.md` only, as for other
  V-items. The ramora trade-off text ("parity and performance", D-01) states intent, not a measured result.

## Design ambiguity review, pass 2 (2026-10-20)

- Ambiguity review added 1 new design step (Scope 7 target text).
- Not filed: field values of the `P` and `R` instances, table text format, other output lines. Low effect; any
  reasonable choice meets the acceptance criteria.

## Design inconsistency review, pass 2 (2026-10-20)

- No inconsistency review feedback.

## Notes for the design-steps fix, pass 2 (2026-10-20)

- Open steps: the D-16 SCOPE_QUESTION (still the user's decision) and the pass 2 Scope 7 text step.
- Fix the Scope 7 step in `design.md` Scope 7 only. Give the exact target text, so that Acceptance 5 can check it.
  Do not add new target files.
- Targets:
  - `ramora/defclosedrecord/embodiment.md`, Artifacts table: rows `dev/bench/` and `deps.edn`.
  - `mementum/state.md`, Status: the "Project skeleton" line (alias list), the "Tests:" and "REPL:" lines (command
    style), and the "Verification plan done" line.
  - `README.md` "## Development": commands are indented code blocks, one for each command.
- The bench command is `clojure -M:bench`. `:main-opts` is in the alias, so no `-m`.

## Design-steps fix, pass 2 (2026-10-20)

- SCOPE_QUESTION (D-16 CI scope) still deferred to the user. Left unchecked.
- Scope 7 now gives the exact target text. Current text to replace: `embodiment.md` `deps.edn` row says
  "Test and bench aliases."; `state.md` has two "Tests:" lines (file list, command). The "Bench:" line goes
  after the command line. README "## Development" ends with the REPL `(scry/run)` block.
- Scope 7 does not add `dev/bench/...` to the `state.md` "Project skeleton" file list. Only the alias list changes.

## Design architecture review, pass 3 (2026-10-20)

- No new architectural review feedback. The D-16 SCOPE_QUESTION stays open for the user. Not filed again.
- Checked the pass 2 Scope 7 text with no finding: the new `embodiment.md` rows keep the artifact and boundary
  structure; `dev/bench` uses public names only.
- For the inconsistency turn: the new `deps.edn` row text "Aliases `:test` and `:bench`." does not name
  `:nrepl`, `:clj-1.12`, `:clj-1.13`. Not an architecture finding.

## Design ambiguity review, pass 3 (2026-10-20)

- No ambiguity review feedback.
- Not filed (low effect, one reasonable reading): Scope 6 S-03 wording (the rule is Scope 4); the `state.md`
  "Not done:" clause when V-11 is the last item (drop it); `=` uses two distinct equal instances.

## Design inconsistency review, pass 3 (2026-10-20)

- Inconsistency review added 1 new design step (Scope 7 `deps.edn` row alias list).
- The old row ("Test and bench aliases.") did not read as a full list; the new Scope 7 text does. Fix in Scope 7
  text only.

## Notes for the design-steps fix, pass 3 (2026-10-20)

- Open steps: the D-16 SCOPE_QUESTION (still the user's decision) and the pass 3 `deps.edn` row step.
- Fix the row step in `design.md` Scope 7 only: the `embodiment.md` `deps.edn` row sentence on aliases. Keep the
  other sentences of that row. Use the alias order of the Scope 7 `state.md` "Project skeleton" text (`:test`,
  `:bench`, `:nrepl`, `:clj-1.12`, `:clj-1.13`), so that the two documents agree.
- Source of truth for the alias list: `deps.edn` (current aliases `:test`, `:nrepl`, `:clj-1.12`, `:clj-1.13`)
  plus `:bench` from Scope 1. Target file: `ramora/defclosedrecord/embodiment.md`, Artifacts table.

## Design-steps fix, pass 3 (2026-10-20)

- SCOPE_QUESTION (D-16 CI scope) still deferred to the user. Left unchecked.
- Scope 7 `deps.edn` row now lists all five aliases, in the `state.md` "Project skeleton" order. The other
  sentences of the row are unchanged. The row does not describe `:nrepl`, `:clj-1.12`, `:clj-1.13`; only `:test`
  and `:bench` get a content sentence.

## Design architecture review, pass 4 (2026-10-20)

- No new architectural review feedback. The D-16 SCOPE_QUESTION stays open for the user. Not filed again.
- Checked the pass 3 fix (five-alias `deps.edn` row) against `deps.edn` and the embodiment Artifacts table: no finding.

## Design ambiguity review, pass 4 (2026-10-20)

- No ambiguity review feedback.

## Design inconsistency review, pass 4 (2026-10-20)

- No inconsistency review feedback.

## Notes for the design-steps fix, pass 4 (2026-10-20)

- Pass 4 added no design steps. The only open step is the D-16 SCOPE_QUESTION. It needs a user decision.
- If the user accepts the gap: tick the step and record the decision here. Do not change `design.md`.
- If the user wants a D-16 update: add it to Scope 7 as exact target text (as for the other Scope 7 rows), and
  add `ramora/defclosedrecord/decisions/verification.md` to Acceptance 5. Target: D-16 line 17
  "Scope: CI matrix Clojure 1.11, 1.12, 1.13." (applies to the line 13 criterium item). Change only the V-11 part.

## Plan ambiguity review (2026-10-20)

- Plan ambiguity review added 1 new design step (bench helper: macro, not a thunk function).
- Not filed (low effect, one reasonable reading): REPL alias for the Slice 1 smoke test (`-M:bench:nrepl`; the
  last `:main-opts` wins); field values of `P` and `R`; table text format; S-03 wording (filed as not-filed in
  design pass 3).

## Plan inconsistency review (2026-10-20)

- No inconsistency review feedback.
- Checked plan.md and steps.md against design.md Scope 1–7, Acceptance 1–5, Out of scope 4 and the
  implementation.md fix notes: alias content, five operations and order, pass rule, Scope 5 columns, Scope 7
  target text, V-11 status rule. They agree.
- For the Slice 2 check: HotSpot can print a `CompileCommand: blackhole ...` line at start. That is a JVM
  notice, not the criterium blackhole warning of Acceptance 2.

## Notes for the design-steps fix, plan pass (2026-10-20)

- Open steps: the D-16 SCOPE_QUESTION (still the user's decision; see pass 4 notes) and the plan step on the bench
  helper (macro, not a thunk function).
- Fix the helper step in the task `plan.md` Slice 1 and `steps.md` Slice 1 (the "Write a bench macro/fn" item)
  only. `design.md` needs no change: Scope 2 already requires local bindings and no extra cost in the measurement.
- Principle: the measured expression is the literal operation, at its own `bench` call site, with `x` (and `y`)
  as locals in a `let` around it. No `IFn` invoke, var deref or shared call site in the measured body.
- Accepted shape: a macro that expands to `(criterium.bench/bench ~expr :viewer :none :limit-time-s ~t)`, then
  reads `(criterium.bench/last-bench)` into the per-call stats. Ten direct `bench` calls are also fine. Keep
  `:limit-time-s` a parameter, so the Slice 1 REPL smoke test can use 1 s.
- Lint: `.clj-kondo/config.edn` has `:lint-as` for `defclosedrecord` only. A local macro in `dev/bench` needs no
  kondo config if its arguments are plain forms. Acceptance 4 lints `dev`.

## Design-steps fix, plan pass (2026-10-20)

- SCOPE_QUESTION (D-16 CI scope) still deferred to the user. Left unchecked.
- Bench helper step fixed in `plan.md` Slice 1 (and Key decisions) and `steps.md` Slice 1: macro
  `(bench-op expr limit-s)`, not a thunk function. `design.md` unchanged.
- criterium 0.5.243-ALPHA source (`~/.m2/repository/criterium/criterium/0.5.243-ALPHA/`, `criterium/bench.clj`,
  `criterium/measured/impl.clj`):
  - `bench` puts its options in a runtime map (`options->bench-plan`), so `:limit-time-s ~limit-s` can be a symbol.
  - `measured/expr` uses `&env`. It keeps the head of the call literal and hoists every argument (locals and
    constants, e.g. `:a` and `2` in `(assoc x :a 2)`) into a state vector. This is the same for `P` and `R`.
  - The arg type hint comes from the symbol meta (`^P x`) or the local's class. Put the hint on the symbol in
    the `(.a ^P x)` expression to be safe, so that no reflection occurs.

## Plan ambiguity review, pass 2 (2026-10-20)

- No ambiguity review feedback.
- Checked the plan pass fix (`bench-op` macro) with no finding. `:viewer :none` is a defined view method in
  criterium 0.5.243-ALPHA (the `bench` docstring does not list it). `last-bench` is a function.
- Not filed (low effect, one reasonable reading): the text in the Operation column; which run is "the acceptance
  run" if a harness bug forces a re-run (the first run of the fixed harness).

## Plan inconsistency review, pass 2 (2026-10-20)

- No inconsistency review feedback.

## Notes for the design-steps fix, plan pass 2 (2026-10-20)

- Plan pass 2 added no design steps. The only open step is the D-16 SCOPE_QUESTION (user decision; see pass 4 notes).
- If the user wants a D-16 update, the plan also changes, not only `design.md`: add a D-16 item to `plan.md`
  Slice 3 (Docs) and to `steps.md` Slice 3, with the same exact text as the new Scope 7 row. The D-16 lines
  (`ramora/defclosedrecord/decisions/verification.md` lines 13 and 17) are as the pass 4 notes give.
- If the user accepts the gap: no change to `plan.md` or `steps.md`. Remove the D-16 bullet from the `plan.md` Risks.

## Implementation (2026-10-20, base `122ce29`)

- Harness: `deps.edn` `:bench` alias; `dev/bench/closed_record/bench.clj`. Pure fns `per-call-stats`, `ratio`,
  `pass?`, `row`, `table`; macro `bench-op`; `run` takes `limit-s`; `-main` uses 10 s and `System/exit`.
- `:main-opts` in `:bench` runs `-main` even with `-e` after it. For a REPL/smoke use, add an alias with
  `:main-opts []` (e.g. `-Sdeps '{:aliases {:e {:main-opts []}}}' -M:bench:e`).
- Pass rule checked at the REPL: (1.06 vs 1.0, no overlap) fail; (overlap) pass; faster pass.
- `(.a ^P x)` measures about 0.03–0.06 ns: the JIT hoists the field read. Same for `P` and `R`.
- The JVM prints `CompileCommand: blackhole ...` at start. That is the HotSpot notice, not the criterium warning.

### Acceptance run

`clojure -M:bench`, Clojure 1.11.4, criterium 0.5.243-ALPHA, OpenJDK 25.0.2+10-69, Apple M5 Max (arm64),
macOS, base `122ce29`. Exit code 0.

| Operation      | P mean [low, high] ns | R mean [low, high] ns | P / R | Result |
|----------------|-----------------------|-----------------------|-------|--------|
| (:a x)         | 0.61 [0.61, 0.61]     | 0.69 [0.69, 0.69]     | 0.890 | pass   |
| (.a x)         | 0.06 [0.06, 0.06]     | 0.06 [0.06, 0.06]     | 0.993 | pass   |
| (assoc x :a 2) | 7.07 [7.00, 7.14]     | 6.99 [6.91, 7.09]     | 1.011 | pass   |
| (hash x)       | 4.14 [4.12, 4.17]     | 4.17 [4.14, 4.20]     | 0.995 | pass   |
| (= x y)        | 2.88 [2.86, 2.90]     | 3.01 [2.99, 3.03]     | 0.957 | pass   |

- Variance, for the record (not cherry-picked away): two earlier runs of the same harness gave failures. A 1 s
  smoke run: `assoc` 1.111 fail. One full 10 s run (output only partly captured, by mistake): `hash` 0.991 pass,
  `=` 1.086 fail. Results near 1.05 change from run to run on this machine. The run above is the first full run
  with the complete table captured; `state.md` V-11 is done from it. A future task can make the bench more
  stable (for example, several JVM forks) if V-11 must be robust.
- Acceptance 1–5 checked: alias only adds criterium; table and exit code per Scope 3–5; kondo 0/0, cljfmt ok;
  tests 83 pass; docs per Scope 6–7. D-16 SCOPE_QUESTION in `design-steps.md` stays open for the user (not in
  Scope 7).

## Implementation review (2026-10-20)

- Added 2 steps to be addressed.

## Review follow-up (2026-10-20)

- Acceptance run: the plan rule applies (no cherry-picked re-runs). The acceptance run is the first full 10 s run
  of the fixed harness: `=` `P / R` 1.086, fail. `state.md` keeps V-11 not done with that ratio. The table above
  is a later run, kept for the record only.
- Field access: `(.a x)` is not measured (the JIT hoists the read). `state.md` V-11 status says so. A change of
  the measurement is for a future task.
- addressed 2 review steps

## Implementation review, pass 2 (2026-10-20)

- Added 1 step to be addressed.

## Review follow-up, pass 2 (2026-10-20)

### Acceptance run (first full 10 s run of the fixed harness)

This run is the acceptance run (plan.md Risks: no cherry-picked re-runs). It replaces the "### Acceptance run"
table above as the Acceptance 3 record. That table is a later run, for the record only.

`clojure -M:bench`, Clojure 1.11.4, criterium 0.5.243-ALPHA, OpenJDK 25.0.2+10-69, Apple M5 Max (arm64),
macOS, base `122ce29`, 2026-10-20.

Captured rows (means and intervals not captured):

| Operation | P / R | Result |
|-----------|-------|--------|
| (hash x)  | 0.991 | pass   |
| (= x y)   | 1.086 | fail   |

- Rows `(:a x)`, `(.a x)`, `(assoc x :a 2)`: not captured.
- Exit code: not captured. The pass rule gives 1 (`=` fails).
- Acceptance 3 not fully met: the full table and the observed exit code of this run are not available.
- `state.md` V-11 (not done, `=` `P / R` 1.086) agrees with this run.
- addressed 1 review step

## Implementation review, pass 3 (2026-10-20)

- No new review feedback.

## Test review (2026-10-20)

- Added 2 steps to be addressed.

## Test review follow-up (2026-10-20)

- Moved the pure layer to `closed-record.bench.report` (`dev/bench/closed_record/bench/report.clj`), with no
  criterium dependency. Added `exit-code`; `-main` uses it.
- `:test` alias: extra path `dev/bench`, no new dependency. The suite loads only `test/` namespaces, so
  `closed-record.bench` (criterium) is not loaded.
- `test/closed_record/bench_report_test.clj`: `per-call-stats`, `pass?`, `row`, `table`, `exit-code`.
  No benchmark run. Suite: 88 tests, 331 assertions pass. clj-kondo and cljfmt clean. Bench smoke run
  (`run 0.2`) gives 5 rows.
- addressed 2 review steps

## Test review, pass 2 (2026-10-20)

- Added 1 step to be addressed.

## Test review, pass 2 follow-up (2026-10-20)

- `table-test`: keeps the Scope 5 checks only. Each data line's cells (split on `|`, trimmed) equal the `row`
  vector of its operation, in input order. Removed the separator, width, line-count and header-text assertions.
  Suite: 88 tests, 325 assertions pass. clj-kondo and cljfmt clean.
- addressed 1 review step

## Test review, pass 3 (2026-10-20)

- Added 1 step to be addressed.

## Test review, pass 3 follow-up (2026-10-20)

- `report`: private `fmt` formats with `java.util.Locale/ROOT` (`String/format`); `row` uses it for the ns
  cells and `P / R`. `pass?-test` premise check uses the `P / R` cell of `report/row`.
- Suite passes with the default locale and with `-Duser.language=de -Duser.country=DE` (88 tests, 325
  assertions). clj-kondo and cljfmt clean.
- addressed 1 review step

## Test review, pass 4 (2026-10-20)

- No new test review feedback.

## Test review, pass 5 (2026-10-20)

- No new test review feedback.

## Docs review (2026-10-20)

- Added 1 step to be addressed.

## Docs review follow-up (2026-10-20)

- `embodiment.md` Artifacts: added the row for `test/closed_record/bench_report_test.clj` (V-11).
- addressed 1 review step

## Docs review, pass 2 (2026-10-20)

- No new docs review feedback.

## Code shape review (2026-10-20)

- Added 1 step to be addressed.

## Code shape review follow-up (2026-10-20)

- `report/table`: cell padding uses `fmt` (`Locale/ROOT`). All formatting in `report` has one idiom.
- Suite passes (88 tests, 325 assertions). clj-kondo and cljfmt clean.
- addressed 1 review step

## Code shape review, pass 2 (2026-10-20)

- No new code shape review feedback.
