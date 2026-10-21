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
