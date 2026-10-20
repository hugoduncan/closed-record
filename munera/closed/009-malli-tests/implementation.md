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

## Plan review (2026-10-20)

- Ambiguity review added 2 new design steps (test `ns` requires; `mementum/state.md` edit scope).
  - Not filed: the check form for "a `P` with entries". Closed-record `=` follows `defrecord` (V-12): a record is not
    `=` to a plain map. Thus `(= (->P 1 nil 3) result)` checks the type and the entries; a map-only `=` fails, so
    a wrong form cannot pass.
  - Not filed: README section position. `README.md` has only "Rational" and "Development"; any `##` position fits.
- Inconsistency review: no inconsistency review feedback.

## For the plan-review design-steps pass

- Edit `steps.md` only (Slice 2 ns step, Slice 4 `state.md` step). Both items change wording, not scope or
  `design.md`. Keep `plan.md` as it is.
- ns pattern to copy: `test/closed_record/hidden_key_test.clj` (docstring with V-ids, then `clojure.test` and
  `closed-record.core` requires). The docstring can name V-07, S-04, D-11 (the design Intent).
- `mementum/state.md` "Status" section: the "Tests:" list and the "Verification plan done" line. The
  `property_test.clj` entry is the pattern for a test that names its library in the `:test` alias.

## Plan-review design-steps pass (2026-10-20)

- Both plan-review steps done in `steps.md` (Slice 2 ns step, Slice 4 `state.md` step). `plan.md` Slice 4 line
  now also names the "Tests" list entry, so plan and steps agree.
- No implementation step is done yet. `deps.edn` has no malli; Slice 1 is the next step.
- `mementum/state.md` "Tests:" list does not name `equality_test.clj` (V-12). Not part of this task; do not fix it
  in Slice 4 unless asked.

## Plan review pass 2 (2026-10-20)

- Ambiguity review (pass 2): no ambiguity review feedback.
  - Not filed: where the `ex-data` helper comes from. No shared test helper ns exists; each test ns has its own
    private `thrown` and `data-of` (`defclosedrecord_test.clj`, `hidden_key_test.clj`). Copy them into
    `malli_test.clj`.
  - Not filed: the schema of the nil-field test. Any `[:map ... [:b :int]]` vs `[:map ... [:b [:maybe :int]]]`
    pair on `p` gives the required false/true.
- Inconsistency review (pass 2): no inconsistency review feedback.

## For the next step (after plan review pass 2)

- `design-steps.md` has no open items. No design-steps pass is needed. `design.md`, `plan.md` and `steps.md`
  are stable; go to implementation (Slice 1).
- If a later review adds design steps: edit `steps.md` (and `plan.md` only to keep it in agreement); do not change
  scope or `design.md`.

## Implementation pass (2026-10-20)

- All slices done in one commit. No `src` change; no emitter fault found.
- All observed-facts rows held under Clojure 1.11.4, 1.12.6, 1.13.0-alpha9 (82 tests, 308 assertions, 0 fail).
- Equality checks use `(= (->P ...) result)`: this checks the type and the entries (V-12).
- `key-transformer` `ex-data` is `{:type :closed-record/empty :record P}` (no `:key`).
- README section is `## malli`, placed before `## Development`.

## Implementation review (2026-10-20)

- No implementation review feedback.

## Test review (2026-10-20)

- Added 1 step to be addressed.

## Test review follow-up (2026-10-20)

- Addressed 1 review step: `plain-map-fix-test` in `malli_test.clj`. REPL values matched the step. `=` against a plain map also checks that the result is not a record.

## Test review pass 2 (2026-10-20)

- No test review feedback.

## Test review pass 3 (2026-10-20)

- No test review feedback.

## Docs review (2026-10-20)

- No docs review feedback.

## Code-shaper review (2026-10-20)

- Added 2 steps to be addressed.

## Code-shaper review follow-up (2026-10-20)

- Addressed 2 review steps in `malli_test.clj`: `sc` is `(into [:map {:closed true}] (rest s))`; `throwing-cases` table is shared by `throwing-transformer-test` and `plain-map-fix-test`.
- 1.11, 1.12, 1.13: 83 tests, 311 assertions, 0 fail. clj-kondo and cljfmt clean.

## Code-shaper review pass 2 (2026-10-20)

- No code-shaper review feedback.
