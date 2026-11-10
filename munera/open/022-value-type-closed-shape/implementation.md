# Implementation

## Design check against value-type `4ed8d0a3` (2026-11-10)

- value-type task 022 (`e8e0c407`) is closed. Its contract
  (`ramora/deftype-star-design/public-contract.md`, "Closed record shape";
  the `defvaluetype*` docstring) agrees with VT-D13. D10 is firm, D9 is
  provisional. No Ramora decision changed; Ramora now records the pin, the
  receiving expansion, `:removed-field-access`, `value-type.value-classes`
  and the closed rows of VT-K8.
- New since `edc3f79`: a closed form is a receiving form also on identity
  fallback; the value-type fallback policy is read at first use;
  `value-type.value-classes` (`auto`, `off`); array adapters load lazily
  (the JVM flags do not change).
- Probe (EA3, `--enable-preview`, `jdk.internal.value` export,
  `defvaluetype*` forms written by hand): closed `[^long x]` and
  `[^int x ^int y]` have only the declared fields and `:flat? true`; closed
  `[^long x ^long y]` gives `:flat? false`; record shape `[^long x]` keeps
  `__meta __extmap` and gives `:flat? false`. A non-nil `__meta` and `create`
  with an undeclared key both throw `IllegalArgumentException`, with no
  wrapper.
- Current suites with the pin overridden to `4ed8d0a3` (`-Sdeps`
  `:override-deps`), no emitter change:
  - `test/value-type`, Temurin 21, identity: 89 tests, 435 assertions pass;
  - `test/value-type-clj-1.11`: 1 test, 7 assertions pass;
  - `test/value-type`, EA3, value: 87 tests, 436 assertions pass.

## Architecture review (2026-11-10)

- architectural review added 2 new design steps
- Sources: `ramora/META.md` and `doc/architecture.md` do not exist. The
  architecture is in Ramora (`ramora/INDEX.md`, `defclosedrecord*`,
  `value-type-records*`).
- No misfit with VT-D13 to VT-D15, D-11 or the ownership split. The two
  steps are about Ramora pages that the design does not name (parent
  design value-mode notes, VT-D10) and the place of the new mode
  differences in `impl.jvm` (`representation`, today:
  `:hidden-fields :hash-form :with-meta-form`; `base-interfaces`, the
  `IObj` methods, `assoc` and `-extra-keys` are shared).

## Ambiguity review (2026-11-10)

- ambiguity review added 3 new design steps
- Checked: value-type `4ed8d0a3` `doc/usage.md` has the heading
  "`deftype*` forms", so the anchor `#deftype-forms` exists. `edc3f79`
  occurs outside Munera in `deps.edn`, README (deps snippet),
  `doc/value-type-records.md` (lines 55 and 75) and the Ramora embodiment.
- Left for the inconsistency turn: acceptance criterion 1 permits "a later
  commit", but "Dependencies" pins one SHA; "If VT-V12 gives other results,
  change VT-K8" against acceptance criterion 2 (VT-V12 passes).

## Inconsistency review (2026-11-10)

- inconsistency review added 3 new design steps
- The value-type suite reads metadata outside the shared checks in
  `aot_test.clj` (VT-V7) and `non_nil_test.clj` (VT-V9, "with-meta keeps
  the fields"; the design already removes it). `representation_test.clj`
  (VT-V2) today checks only `__hash`/`__hasheq` in `specs`.

## Notes for the design-steps work (2026-11-10)

- Principles: Ramora first, then `design.md` (design rule; VT-R6). Ramora
  design pages state the current design only; differences and limits go
  only in `embodiment.md` (`ramora/INDEX.md`). Prose in ASD-STE100 style.
  Do not change the scope sections.
- Several steps edit one file:
  `ramora/value-type-records/invariants-and-verification.md` (VT-D10
  export; VT-V3 V-01 part; VT-V12 gate and pin rule). Do them in one
  change, so that VT-V3, VT-V12 and VT-D10 agree.
- The pin-rule step decides the VT-V12 gating step: if a later pin is
  allowed, VT-V12 and acceptance criterion 4 follow the pin.
- Paths: emitter `src/closed_record/impl/jvm.clj` (`representation`,
  `base-interfaces`, `generated-methods`); shared checks
  `test/runtime/closed_record/checks/` (`support.clj` has
  `define-checks` and `support/representation`); value-type suite
  `test/value-type/closed_record/value_type/` (`expectation.clj`,
  `representation_test.clj`, `aot_test.clj`, `non_nil_test.clj`); parent
  design `ramora/defclosedrecord/decisions/{type-structure,closed-key-set}.md`,
  `ramora/defclosedrecord/embodiment.md`,
  `ramora/defclosedrecord/embodiment/artifacts.md`.

## Design steps done (2026-11-10)

- All 8 design steps are done. No `SCOPE_QUESTION:` items.
- Ramora design pages changed in this pass (Ramora first): D-01, D-06
  (`type-structure.md`) and D-09 (`closed-key-set.md`) value-mode notes;
  VT-I9 verified-by, VT-V2, VT-V3 (V-01 and V-05 extra-key parts), VT-V7,
  VT-V12 gate, VT-D10 export (`invariants-and-verification.md`). The
  value-type embodiment now lists the missing export and the VT-V7 metadata
  read as differences.
- Found beyond the review: `literal.clj` `extra-key-constant-test` (V-05)
  and the reader metadata of `value-constant-test` also fail in value mode.
- `support/representation` is `:identity` in the ordinary suite and in the
  Temurin 21 value-type job. It cannot select the value-mode exclusions;
  use the record options (`:value-type true`) or a new `define-checks` key.
- value-type `4ed8d0a3`: `array-info` without the export throws
  `:array-inspection-unavailable`; `into-array` with
  `value-type.value-classes=off` throws `:array-unavailable`.
- The parent embodiment "Generated type" pointer already covers "Method
  behaviour" and "State"; no new pointer is needed. The README has no
  value-type doc link, only the deps snippet.

## Architecture review, pass 2 (2026-11-10)

- no architectural review feedback
- Checked: the "Emitter" list of mode differences is complete. In
  `impl/jvm.clj` the generated hidden-field references are only `withMeta`
  (`:with-meta-form`), `hash-form`, `meta`, `assoc` and `-extra-keys`.
  `base-interfaces` lists `IObj` (not `IMeta`).
- Not a finding: parent C-08, C-09 (`purpose.md`) and I-02, I-05, I-07,
  I-08 (`invariants.md`) have no value-mode notes. By convention, VT-I1 and
  VT-V3 state the exceptions; only parent decisions get value-mode notes.

## Ambiguity review, pass 2 (2026-11-10)

- no ambiguity review feedback
- Left to the plan, not design ambiguities: how `define-checks` selects the
  value-mode exclusions (see "Design steps done"), and the test files for
  VT-V11 and VT-V12. Acceptance criterion 4 (`Cell`) is covered by the
  VT-V12 `[^long x]` fixture with the VT-I7 field check (VT-V11 (e)).

## Inconsistency review, pass 2 (2026-11-10)

- no inconsistency review feedback
- README map for "Documentation" (the README has no single "Value-type
  records" section with these parts): Approach → `## Approach` (value-type
  paragraph); Requirements → `### Value-type records (optional)` under
  Installation (deps snippet, `value-type.fallback`); Differences → the list
  in `### Value-type records` under Usage; "Arrays and flattening" → its
  `####` subsection. The README already links `doc/value-type-records.md`.
- `artifacts.md` `test/value-type/` row also omits VT-V9 today; add it with
  VT-V11 and VT-V12.

## After design review pass 2 (2026-11-10)

- Pass 2 (architecture, ambiguity, inconsistency) added no design steps.
  `design-steps.md` has no unchecked items. The design is stable. Next
  step: plan (`create-task-plan`), not design-steps work.
- If a later pass adds steps, apply the rules in "Notes for the
  design-steps work" above. Do not raise again the items recorded as "not
  a finding" or "left to the plan" in the pass 2 notes.
