# Steps

## Slice 1: pin and VT-V6

- [ ] Update the `:value-type-test` pin in `deps.edn` to value-type
  `edc3f79de77bcf044d6234df361e090edc46c813`, with an explicit HTTPS
  `:git/url` and the full SHA.
- [ ] Update the README dependency example to the same SHA. Do not change
  the evidence link in `doc/performance.md`.
- [ ] Change `non-nil-field-test` in
  `test/value-type/closed_record/value_type/errors_test.clj` to the new
  VT-V6 example: a `:non-nil` field with no tag gives `:value-type/error
  :invalid-field` with `:reason :missing-tag`, as the cause of the
  macroexpansion failure.
- [ ] Run the value-type suite and the ordinary suite locally; confirm the
  pin breaks nothing.
- [ ] Commit slice 1.

## Slice 2: VT-V9 non-nil field tests

- [ ] Add VT-V9 fixture types to the value-type suite: a value record
  `Inner`, and a record with `^Inner ^:non-nil`, `^String ^:non-nil` and
  an untagged field.
- [ ] Test field storage types: the non-nil fields store the tag classes;
  the untagged field stores `Object`.
- [ ] Test (value output only) that the record class is a value class.
- [ ] Test valid-value construction: `->Name`, `map->Name`, `assoc` of
  each declared key, the map literal and the positional literal; read back
  by keyword at a call site (lookup thunk), by `get` and by field access;
  `with-meta` keeps the fields.
- [ ] Test, for each non-nil field: nil gives `NullPointerException` and a
  wrong type gives `ClassCastException`, in the cause chain, through
  `->Name`, `map->Name` (nil value and missing key), `assoc`, the map
  literal and the positional literal.
- [ ] Test that the untagged field accepts nil on each path.
- [ ] Test key-set precedence: `assoc` of an undeclared key, `dissoc` of a
  non-nil field, and `map->Name` with an extra key and a missing non-nil
  key give the closed-record `ex-info` types.
- [ ] Test that `check` returns a valid record.
- [ ] Run the value-type suite; commit slice 2.

## Slice 3: VT-V2 extension (VT-I6)

- [ ] In the value-type suite, expand a value-type record form with
  `:non-nil` fields and the same form without, as the expansion fixture
  does (`macroexpand-1`, position metadata removed; reuse `normalise` and
  `strip-positions` from
  `test/runtime/closed_record/expansion_fixture.clj`, or expose a helper).
- [ ] Check part (a): each `:non-nil` field symbol in the `defvaluetype*`
  field vector carries `:non-nil true` unchanged, and no other place in
  the expansion carries `:non-nil`.
- [ ] Check part (b): after removal of `:non-nil` from those field
  symbols, the printed expansion (`*print-meta*` true, normalised) equals
  the printed expansion of the form without `:non-nil`.
- [ ] Run the value-type suite; commit slice 3.

## Slice 4: VT-V10 ordinary mode

- [ ] Add an ordinary-suite test: a closed record with a
  `^String ^:non-nil` field and no option accepts nil on `->Name`,
  `map->Name` and `assoc`.
- [ ] Add the ordinary `^String ^:non-nil` form (no option) to the
  expansion fixture `forms`, run `record!`, and verify by diff that the
  six existing `expansion_fixture.edn` entries are unchanged.
- [ ] Run the ordinary suite on Clojure 1.11, 1.12 and 1.13 locally where
  possible; commit slice 4.

## Slice 5: Ramora

- [ ] Add VT-D11 and VT-D12 to `ramora/value-type-records/decisions.md`,
  in the 8-tuple shape, with the bases and commitments from design.md.
- [ ] Add VT-I6 to `invariants-and-verification.md` (`verified-by:
  VT-V2`); add the VT-V9 and VT-V10 rows; extend the VT-V2 row with the
  VT-I6 checks (keep the id); change the VT-V6 row to the `:missing-tag`
  example; fix the incomplete "VT-V2 ... run in Temurin 21 jobs" sentence
  (both value-type jobs run it).
- [ ] In `ramora/value-type-records.md`: replace the context fact
  "`:non-nil` field metadata is rejected" (line ~69) with the current
  contract; update the public contract pass-through example (line ~113);
  add the non-nil enforcement and field storage rows to the differences
  table (one cell each for Ordinary, Value output, Identity fallback;
  native null restriction only on value output with a value-class tag on
  a native-field JVM, otherwise constructor checks); update the
  embodiment.
- [ ] Update VT-R1 in place: the `defvaluetype*` contract can change
  before the value-type release; signals VT-V2, VT-V6, VT-V9. Record the
  `c366f15` defect only as technical evidence (why VT-V9 reads through the
  lookup thunk), not as an event.
- [ ] Commit slice 5.

## Slice 6: documentation

- [ ] README "Value-type records": add a "Non-nil fields" part with an
  example; the tag rule in short with a link to value-type's field
  contract; nil and wrong types give host exceptions with no `ex-data`;
  missing keys in `map->Name` give `NullPointerException`; the metadata
  has no effect without `:value-type true`. Add the non-nil enforcement
  and field storage rows to the differences table, with the Intent
  enforcement wording.
- [ ] Add one sentence on `:non-nil` with `:value-type true` to the
  `defclosedrecord` docstring; regenerate `doc/API.md`.
- [ ] Add non-nil fields to the value-type entry in `CHANGELOG.md`
  `## [Unreleased]`.
- [ ] Commit slice 6.

## Slice 7: finish

- [ ] Update `mementum/state.md` to the current state.
- [ ] Run the quality checks: format, lint, prose, API freshness, links.
- [ ] Confirm all CI jobs pass on the last commit that changes files
  outside the task directory.
- [ ] Commit slice 7.
