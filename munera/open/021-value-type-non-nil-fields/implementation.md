# Implementation

## Design review

- 2026-11-04 architecture review: `ramora/META.md` and `doc/architecture.md`
  do not exist; used `AGENTS.md`, `ramora/INDEX.md` (document rules),
  `ramora/defclosedrecord` (principles, D-03, D-11, D-19, D-20) and
  `ramora/value-type-records`. VT-D12 item is actionable because its
  rationale contradicts D-20, which also decides its commitment. VT-D11
  fits D-03 scope (host exceptions for value casts) and D-19.
- architectural review added 2 new design steps
- 2026-11-04 ambiguity review: the emitter puts field metadata only in the
  `deftype*`/`defvaluetype*` field vector; factories and methods use
  `plain-fields` (metadata removed). Thus VT-I6 part (a) is checkable. The
  expansion fixture compares printed strings with `*print-meta*` true.
- ambiguity review added 2 new design steps
- 2026-11-04 inconsistency review: the current VT-V6 test
  (`test/value-type/closed_record/value_type/errors_test.clj`,
  `non-nil-field-test`) expects `:non-nil-unsupported`; it fails with the new
  pin until VT-V6 changes. Only Ramora (not README or `doc/`) states that
  `:non-nil` is rejected.
- inconsistency review added 1 new design step
- For the design-steps slice:
  - Edit design.md in place; no amendment notes (Munera evolution rule).
    Keep the scope sections unchanged.
  - Write in ASD-STE100 prose. Proposed Ramora records follow the shapes in
    `ramora/value-type-records/decisions.md` (8-tuple) and
    `ramora/value-type-records/invariants-and-verification.md` (assertion
    record). Commitment levels: `ramora/defclosedrecord/decisions.md`.
  - D-20 text: `ramora/defclosedrecord/decisions/platform-and-evolution.md`;
    D-03, D-11: `.../failure-and-validation.md`; D-19:
    `.../closed-key-set.md`. Ramora page rules: `ramora/INDEX.md`.
  - VT-I6 and VT-V10 depend on
    `test/runtime/closed_record/expansion_fixture.clj` (`expansion`,
    `normalise`) and the field-metadata path in
    `src/closed_record/impl/jvm.clj` (`plain-fields`, field vector).

## Design-steps slice (2026-11-04)

- All 5 design steps done; no `SCOPE_QUESTION:` items.
- VT-V10 fixture form: `record!` rewrites the whole
  `expansion_fixture.edn` from `forms`; after adding the `:non-nil` form,
  check by diff that the six existing entries are unchanged. No `src/` or
  `test/` commit since `f034647`, so the current emitter is the `f034647`
  emitter until this task changes it.
- VT-I6 needs the unprinted expanded form for part (a); the fixture
  `expansion` returns only the printed string. Reuse `normalise` and the
  position stripping (private `strip-positions`), or expose a helper.
- Compiled constants are not in the failing-path list; value-type owns
  their load (ownership list). VT-V9 has no compiled-constant check.

## Design review, pass 2 (2026-11-04)

- architecture review (pass 2): no architectural review feedback. Checked
  the revised design against D-03 scope (value casts keep host exceptions),
  D-11, D-19, D-20, principles 9 and 15, and I-03 (an `assoc` that throws
  on nil does not break it, as for `^long` fields today).
- ambiguity review (pass 2) added 1 new design step
- inconsistency review (pass 2) added 1 new design step

## For the pass-2 design steps (2026-11-04)

- Two open steps only; prior notes on edit rules and Ramora shapes apply.
- "Eligible tag" step: take the wording from design.md Context,
  "Enforcement" bullet (value-class tag ∧ native-field JVM → native;
  else generated checks). Check that Intent, the README part and the
  "non-nil enforcement" row (README differences table,
  `README.md` "Value-type records"; Ramora table in
  `ramora/value-type-records.md` "Public contract") then say the same.
  On identity fallback, enforcement is always generated checks.
- VT-I6 step: VT-V2 is the "Expansion" row of the table in
  `ramora/value-type-records/invariants-and-verification.md`; it runs in
  the Temurin 21 job (value-type suite). If VT-I6 goes there, also make
  acceptance criterion 4 and the VT-R1 signal list (Documentation) agree;
  VT-V2 is already a VT-R1 signal.

## Pass-2 design-steps slice (2026-11-04)

- Both steps done; no `SCOPE_QUESTION:` items.
- VT-I6 check now lives in VT-V2 (design.md Verification). Both value-type
  CI jobs run all of `test/value-type` (`ci.yml`), so VT-V2 runs on
  Temurin 21 and Valhalla. The Ramora sentence "VT-V2 ... run in Temurin 21
  jobs" (`invariants-and-verification.md`, after VT-D10) is incomplete; fix
  it when VT-V2 changes.
- Current VT-V2 table text has no VT-I6 part; extend the row, keep its id.

## Design review, pass 3 (2026-11-04)

- architecture review (pass 3): no architectural review feedback. The
  pass-2 edits (Intent enforcement wording, VT-I6 `verified-by: VT-V2`)
  fit the Ramora record shapes. I-06 to I-08 stay true: a literal that
  omits a non-nil key fails before an instance exists.
- ambiguity review (pass 3): no ambiguity review feedback. `conj`,
  `merge` and `into` are not in the public contract path list, but the
  emitted `cons` calls `assoc` (`src/closed_record/impl/jvm.clj`), so the
  `assoc` path covers them.
- inconsistency review (pass 3) added 1 new design step. Checked: no
  `8bb7c4f` reference outside `deps.edn`, README and `doc/performance.md`;
  the README and Ramora differences tables have the columns Ordinary,
  Value output and Identity fallback, so the non-nil enforcement row
  needs one cell for each.

## For the pass-3 design step (2026-11-04)

- One open step: a wording fix in design.md Problem, second bullet. Change
  only that bullet. Do not add a Documentation item: no user doc has a
  `:non-nil` claim to remove (`git grep -i non-nil -- . ':!munera'`).
- Rejection claims to name: `ramora/value-type-records.md` (Context fact
  "`:non-nil` field metadata is rejected"; Public contract, value-type
  errors pass-through sentence),
  `ramora/value-type-records/invariants-and-verification.md` (VT-V6 row),
  `test/value-type/closed_record/value_type/errors_test.clj`
  (`non-nil-field-test`). The Documentation section already changes all
  of them.

## Pass-3 design-steps slice (2026-11-04)

- Step done; no `SCOPE_QUESTION:` items. Only the Problem second bullet
  changed. Documentation already covers each named artifact: Ramora context
  fact and public contract ("update the public contract"), VT-V6 row and
  test (Verification, VT-V6).
- Ramora rejection claims: `value-type-records.md` line ~69 (Context,
  "Interacting systems") and ~113 (Public contract example); VT-V6 row in
  `invariants-and-verification.md` line ~78.

## Design review, pass 4 (2026-11-04)

- architecture review (pass 4): no architectural review feedback. Only the
  Problem bullet changed since pass 3; it names artifacts and does not
  change the contract, decisions or ownership split.
- ambiguity review (pass 4): no ambiguity review feedback. The new Problem
  bullet names each artifact by path and section.
- inconsistency review (pass 4): no inconsistency review feedback.
  `git grep -i non-nil` outside `munera/` and `mementum/` finds exactly the
  four places that the Problem bullet names. The current VT-V6 test form
  (`^:non-nil a`, no tag) is the new VT-V6 example; with the new pin only
  the expected reason changes (`:missing-tag`).

## After design review, pass 4 (2026-11-04)

- No open design steps: pass 4 added none in any of the three reviews.
  design.md is stable; the next step is plan creation, not a design-steps
  slice.
- For the plan: the earlier "For the ..." notes in this file keep the
  file paths (emitter `src/closed_record/impl/jvm.clj`, expansion fixture
  `test/runtime/closed_record/expansion_fixture.clj`, Ramora pages, CI
  `ci.yml`). Change the pin first: with the `8bb7c4f` pin VT-V9 cannot pass,
  and with the new pin the current VT-V6 test fails until its reason changes.
