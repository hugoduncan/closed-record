# Implementation

## Design review

- 2026-10-20 architecture review added 1 new design step (README fix vs D-08 / D-19 `map->Name` semantics).
  - Sources: `ramora/META.md` and `doc/architecture.md` do not exist. The architecture is in `ramora/INDEX.md` →
    `ramora/defclosedrecord.md` → `ramora/defclosedrecord/*`.
  - `map->Name` with an extra key throws `:closed-record/extra-keys` (D-08), not `undeclared-key`.
  - Other points checked and fit: malli only in `:test` alias (D-11, K-04, purpose "Costs imposed"); no `src`
    integration; full `ex-data` checks match D-03 public API; embodiment already lists "malli" in the generic
    `*_test.clj` row, so a dedicated V-07 row follows the V-02/V-03/V-13/V-14 pattern.
- 2026-10-20 ambiguity review added 1 new design step (`me/humanize` check form).
  - Not filed: `ex-data` of the `key-transformer` row. The source and the V-01 test fix it as
    `{:type :closed-record/empty :record P}` (no `:key`).
- 2026-10-20 inconsistency review added 1 new design step (trade-off quote omits `:db/id`).
  - `README.md` does not name `:db/id` now, so the trade-off stays partly open after this task.

- 2026-10-20 architecture review (pass 2): no architectural review feedback.
  - Checked the revised design against D-08, D-11, D-16, K-02, K-04, the embodiment boundaries, and V-07 in `qualities.md`.
  - The extra `strip-extra-keys` and `key-transformer` rows go past the "assoc" wording of V-07. They test the same
    trade-off, so this is not a misfit.
- 2026-10-20 ambiguity review (pass 2): no ambiguity review feedback.
- 2026-10-20 inconsistency review (pass 2): no inconsistency review feedback.

## For the design-steps pass

- Edit `design.md` only. Scope (in/out) is frozen. All three steps change wording or test form, not scope.
- Prose rule: ASD-STE100 (`AGENTS.md`). Keep the README section short.
- README fix (step 1): state `map->Name` behaviour from `ramora/defclosedrecord/purpose.md` (Behaviour table) and
  `ramora/defclosedrecord/decisions/closed-key-set.md` (D-08, D-19). Do not invent a helper or a new API (D-11, K-04).
- humanize (step 2): if `=` on the full map is chosen, the expected maps are in the design table. They are malli
  0.20.3 strings; a malli version change must update them.
- Trade-off (step 3): source text is `ramora/defclosedrecord/qualities.md` (Trade-offs). Do not edit `qualities.md`
  or add `:db/id` text to the README in this task.
- `ex-data` helper pattern: `data-of` in `test/closed_record/defclosedrecord_test.clj` (V-01).

## Design-steps pass (2026-10-20)

- All 3 steps done in `design.md`. No `SCOPE_QUESTION:` items.
- nil for a missing key is D-08. D-19 covers only primitive-hinted fields (host exception). `P` has none.
- `map->Name` extra-key `ex-data`: `{:type :closed-record/extra-keys :record … :keys …}` (`src/closed_record/core.cljc`,
  `impl/jvm.clj`). The README names `:keys`; it does not need the exact map.
- The `:db/id` exclusion is now an Out of scope bullet. This is not a scope change: Scope 3 was already malli only.

## Design review pass 2 (2026-10-20)

- No open design steps. `design.md` is stable; the next step is planning (`create-task-plan`).
- Scope 4 `deps.edn` row: `ramora/defclosedrecord/embodiment.md` says "Test and bench aliases", but `deps.edn` has
  no bench alias (`:test`, `:nrepl`, `:clj-1.12`, `:clj-1.13`). Add malli only. Do not fix the bench claim in this task.
