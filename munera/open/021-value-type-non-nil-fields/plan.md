# Plan

## Approach

Pass `:non-nil` field metadata through to `defvaluetype*` with no emitter
change. value-type owns the field contract; closed-record owns the key set.
The work is: a dependency pin, test changes, Ramora changes and
documentation changes.

Key decisions:

- Change the pin first. With the `8bb7c4f` pin, VT-V9 cannot pass; with the
  new pin (`edc3f79`), the current VT-V6 test fails until its expected
  reason changes to `:missing-tag`. So the pin update and the VT-V6 test
  change go in the same slice, to keep every commit green.
- No emitter change expected. The probe shows that field metadata already
  reaches `defvaluetype*` (`src/closed_record/impl/jvm.clj` puts field
  metadata only in the field vector; `plain-fields` strips it elsewhere).
  If a change proves necessary, record the reason in `implementation.md`
  and the Ramora embodiment; the change must not add a value check (VT-D11)
  and must not change the ordinary expansion (VT-I2).
- VT-I6 part (a) needs the unprinted expanded form. The fixture
  `expansion` returns a printed string; reuse `normalise` and
  `strip-positions` (or expose a helper) in the VT-V2 test.
- The VT-V10 fixture form: `record!` rewrites all of
  `expansion_fixture.edn` from `forms`. Add the `:non-nil` form, then
  verify by diff that the six existing entries are unchanged. Record the
  expansion before any emitter change (the current emitter is the
  `f034647` emitter).
- Ramora edits follow the record shapes in
  `ramora/value-type-records/decisions.md` (8-tuple) and
  `invariants-and-verification.md` (assertion record). No status updates
  or history on design pages; record the `c366f15` defect only as
  technical evidence. Update VT-R1 in place.

## Risks

- A value-type contract change before its release can invalidate the pin.
  Signals: VT-V2, VT-V6, VT-V9 (VT-R1).
- The Prerequisite defect (stack map resolution on Valhalla) can regress in
  a later value-type commit. The pin names `edc3f79`; VT-V9 reads non-nil
  fields through the lookup thunk, so it detects a return of the defect.
- Valhalla behaviour is verified only in CI (no local Valhalla JVM
  assumed); the Valhalla job is the gate for VT-V9 value-output checks.
- The new pin can break the existing value-type suite, the Clojure 1.11
  case or the ordinary suite. Acceptance requires all CI jobs green.

## Slice order

1. Pin and VT-V6: update `deps.edn` and the README SHA; change the VT-V6
   test to the `:missing-tag` example; run the suites.
2. VT-V9: non-nil field tests in the value-type suite.
3. VT-V2 extension: the VT-I6 expansion checks (parts (a) and (b)).
4. VT-V10: ordinary-mode nil acceptance test and the expansion fixture
   form.
5. Ramora: decisions VT-D11 and VT-D12, invariant VT-I6, verification rows
   VT-V9 and VT-V10, changed VT-V2 and VT-V6 rows, context fact, public
   contract, differences table, embodiment, VT-R1.
6. Documentation: README, docstring, `doc/API.md`, `CHANGELOG.md`.
7. Finish: `mementum/state.md`, quality checks, CI green.
