# Steps

## Slice 1: VT-V10 fixture form, pin and VT-V6

- [x] Do this step first, before any other change of this task. Confirm
  that `src/` has no change since `f034647`
  (`git diff --quiet f034647 -- src`). Then the current emitter is the
  `f034647` emitter. Add the ordinary form
  `:non-nil (closed-record.core/defclosedrecord NonNil [^String ^:non-nil s])`
  (no option) to `forms` in
  `test/runtime/closed_record/expansion_fixture.clj`. Run `record!`
  (`closed-record.expansion-fixture/record!`, from the repository root,
  with the `:test` alias). Verify with `git diff` on
  `test/runtime/closed_record/expansion_fixture.edn` that the diff adds
  only the `:non-nil` entry and that the six existing entries are
  unchanged. The `forms` change and the `.edn` change go in the same
  commit (`ordinary-expansion-test` compares the two key sets).
- [x] Update the `:value-type-test` pin in `deps.edn` to value-type
  `edc3f79de77bcf044d6234df361e090edc46c813`, with an explicit HTTPS
  `:git/url` and the full SHA.
- [x] Update the README dependency example to the same SHA. Do not change
  the evidence link in `doc/performance.md`.
- [x] Change `non-nil-field-test` in
  `test/value-type/closed_record/value_type/errors_test.clj` to the new
  VT-V6 example: a `:non-nil` field with no tag gives `:value-type/error
  :invalid-field` with `:reason :missing-tag`, as the cause of the
  macroexpansion failure.
- [x] Run T-ID, T-VAL, T-11 and T-RT (plan.md, Test commands); confirm
  the pin breaks nothing.
- [x] Commit slice 1.

## Slice 2: VT-V9 non-nil field tests

- [x] Put all VT-V9 tests of this slice in a new file
  `test/value-type/closed_record/value_type/non_nil_test.clj`
  (ns `closed-record.value-type.non-nil-test`).
- [x] Add VT-V9 fixture types to that file: a value record
  `Inner`, and a record with `^Inner ^:non-nil`, `^String ^:non-nil` and
  an untagged field.
- [x] Test field storage types: the non-nil fields store the tag classes;
  the untagged field stores `Object`.
- [x] Test (value output only) that the record class is a value class.
- [x] Test valid-value construction: `->Name`, `map->Name`, `assoc` of
  each declared key, the map literal and the positional literal; read back
  by keyword at a call site (lookup thunk), by `get` and by field access;
  `with-meta` keeps the fields.
- [x] Test, for each non-nil field: nil gives `NullPointerException` and a
  wrong type gives `ClassCastException`, in the cause chain, through
  `->Name`, `map->Name` (nil value and missing key), `assoc`, the map
  literal and the positional literal.
- [x] Test that the untagged field accepts nil on each path.
- [x] Test key-set precedence: `assoc` of an undeclared key, `dissoc` of a
  non-nil field, and `map->Name` with an extra key and a missing non-nil
  key give the closed-record `ex-info` types.
- [x] Test that `check` returns a valid record.
- [x] Run T-ID and T-VAL (plan.md, Test commands); commit slice 2.

## Slice 3: VT-V2 extension (VT-I6)

- [x] In `test/value-type/closed_record/value_type/representation_test.clj`,
  beside the VT-V2 test `value-expansion-test`, expand a value-type record
  form with
  `:non-nil` fields and the same form without, as the expansion fixture
  does (`macroexpand-1`, position metadata removed; reuse `normalise` and
  `strip-positions` from
  `test/runtime/closed_record/expansion_fixture.clj`, or expose a helper).
- [x] Check part (a): each `:non-nil` field symbol in the `defvaluetype*`
  field vector carries `:non-nil true` unchanged, and no other place in
  the expansion carries `:non-nil`.
- [x] Check part (b): after removal of `:non-nil` from those field
  symbols, the printed expansion (`*print-meta*` true, normalised) equals
  the printed expansion of the form without `:non-nil`.
- [x] Run T-ID and T-VAL (plan.md, Test commands). If the slice changes
  `test/runtime/closed_record/expansion_fixture.clj`, also run T-RT.
  Commit slice 3.

## Slice 4: VT-V10 ordinary mode

- [x] Add an ordinary-suite test to
  `test/runtime/closed_record/value_type_option_test.clj` (the ordinary
  VT-V1 placement): a closed record with a `^String ^:non-nil` field and
  no option accepts nil on `->Name`, `map->Name` and `assoc`. The VT-V10
  expansion fixture form is already in place (slice 1); do not run
  `record!` again.
- [x] Run T-RT (plan.md, Test commands: Clojure 1.11, 1.12 and 1.13);
  commit slice 4.

## Slice 5: Ramora

- [x] Add VT-D11 and VT-D12 to `ramora/value-type-records/decisions.md`,
  in the 8-tuple shape (subject, choice, rejected, rationale, assumptions,
  qualities, scope, commitment). Choice, Rejected and Qualities come from
  design.md (Decisions to record in Ramora). The design.md "Basis" goes in
  the rationale field. Use these values for the other fields:
  - VT-D11 Non-nil fields through value-type.
    - Subject: `:non-nil` field metadata with `:value-type true`.
    - Rationale: "D-11, D-19, VT-D3. value-type owns the field contract;
      closed-record owns the closed key set."
    - Assumptions: `defvaluetype*` follows its field contract (VT-R1).
    - Scope: public API, JVM.
    - Commitment: high. VT-D11 fixes public behaviour: the error
      contract (host exceptions, no `ex-data`) on all paths
      (`ramora/defclosedrecord/decisions.md`, commitment levels).
  - VT-D12 `:non-nil` in ordinary mode.
    - Subject: `:non-nil` field metadata without `:value-type true`.
    - Rationale: "D-20, O-VT3, `defrecord` syntax parity. The released
      version accepts the metadata and accepts nil in such a field."
    - Assumptions: omit (no assumption applies).
    - Scope: public API, all hosts.
    - Commitment: high (D-20).
- [x] Add VT-I6 to `invariants-and-verification.md`, after VT-I5, in the
  assertion record shape:

  ```text
  id: VT-I6
  subject: :non-nil field metadata in the expansion with :value-type true
  predicate: occurs unchanged only in
  object: the defvaluetype* field vector; the rest of the expansion equals the expansion without :non-nil
  scope: all types with :value-type true
  basis: VT-D11
  enforced-by: the JVM emitter puts field metadata only in the defvaluetype* field vector
  verified-by: VT-V2
  confidence: high
  ```

  Add the VT-V9 and VT-V10 rows; extend the VT-V2 row with the VT-I6
  checks (parts (a) and (b); keep the id); change the VT-V6 row to the
  `:missing-tag` example.
- [x] Rewrite the job placement sentence after VT-D10 in
  `invariants-and-verification.md` (line ~110) so that it is complete.
  Both value-type jobs run all of `test/value-type` (`ci.yml`):
  - VT-V2, VT-V6 and VT-V9 run in both value-type jobs
    (`Value-type tests (Temurin 21)` and `Value-type tests (Valhalla)`).
  - VT-V3 runs in the Temurin 21 value-type job; VT-V4 (VT-V3 on value
    output) runs in the Valhalla job.
  - VT-V5 runs only in the Temurin 21 value-type job
    (`subprocess_test.clj` defines it for identity output only).
  - VT-V1: the ordinary part runs in the runtime jobs, the value-type
    suite part in both value-type jobs, the Clojure 1.11 part in the
    Temurin 21 value-type job.
  - VT-V10 runs in the runtime jobs (Clojure 1.11, 1.12 and 1.13).
  - VT-V7 and VT-V8 keep their current placement.
- [x] In `ramora/value-type-records.md`, update the index id lists
  (lines ~15-17): decisions "VT-D1 to VT-D12"; invariants and
  verification "VT-I1 to VT-I6, VT-V1 to VT-V10" (CI decision VT-D10 and
  risks VT-R1 to VT-R5 do not change).
- [x] In `ramora/value-type-records.md`: replace the context fact
  "`:non-nil` field metadata is rejected" (line ~69) with the current
  contract; add the non-nil enforcement and field storage type rows to the
  differences table, with the cells in plan.md (Key decisions,
  differences tables).
- [x] Update the "Elements that change" table in
  `ramora/value-type-records/embodiment.md` (keep the table shape):
  - Tests row: the id list becomes "VT-V1 to VT-V7, VT-V9 and VT-V10"
    (VT-V8 stays in the `dev/bench` row). Add the placements: VT-V9 in
    `test/value-type/closed_record/value_type/non_nil_test.clj`; the VT-I6
    check of VT-V2 in
    `test/value-type/closed_record/value_type/representation_test.clj`;
    VT-V10 in `test/runtime/closed_record/value_type_option_test.clj` and
    the `:non-nil` form of the expansion fixture
    (`test/runtime/closed_record/expansion_fixture.clj`).
  - `closed-record.core` row: the docstring also has one sentence on
    `:non-nil` with `:value-type true`.
  - `closed-record.impl.jvm` row: add that field metadata, `:non-nil`
    included, goes into the `defvaluetype*` field vector with no change,
    and nowhere else (VT-I6); non-nil fields need no emitter change. If
    slices 1 to 4 made an emitter change, state it and its reason here.
  - `doc/` and `README.md` row: add non-nil fields.
- [x] In the public contract of `ramora/value-type-records.md`
  (lines ~91-118): change the value-type error pass-through example
  (line ~113) to a `:non-nil` field with no tag
  (`:value-type/error :invalid-field`, `:reason :missing-tag`). Add the
  design.md public contract statements on non-nil fields:
  - with `:value-type true`, `:non-nil` field metadata has value-type's
    meaning, and closed-record passes it to `defvaluetype*` unchanged;
  - nil and wrong-type values give the host `NullPointerException` and
    `ClassCastException`, not wrapped and with no `ex-data`, on all
    closed-record paths (`->Name`, `map->Name`, `assoc`, map and
    positional record literals);
  - `map->Name` without a key for a non-nil field gives
    `NullPointerException`, as D-19;
  - key-set errors come first, unchanged, on the paths that check the key
    set (`assoc` of an undeclared key, `dissoc` of a declared key,
    `map->Name` with extra keys);
  - `:non-nil` declaration errors are value-type errors and pass through
    unchanged at macroexpansion;
  - without `:value-type true`, closed-record ignores `:non-nil`, as
    `defrecord` does; the field accepts nil and the expansion does not
    change (VT-I2);
  - a compiled record constant cannot hold nil or a wrong type in a
    non-nil field; value-type owns the load of compiled constants with
    enabled fields.
- [x] Update VT-R1 in place: the `defvaluetype*` contract can change
  before the value-type release. Likelihood: medium (no change).
  Effect: VT-D3, VT-D5, VT-D11 and VT-I6 change. Signals: VT-V2, VT-V6,
  VT-V9. Record the
  `c366f15` defect only as technical evidence (why VT-V9 reads through the
  lookup thunk), not as an event.
- [x] Commit slice 5.

## Slice 6: documentation

- [x] README "Value-type records": add a "Non-nil fields" part with an
  example; the tag rule in short with a link to value-type's field
  contract at the pinned SHA,
  `https://github.com/hugoduncan/value-type/blob/edc3f79de77bcf044d6234df361e090edc46c813/doc/usage.md#portable-non-nil-fields`
  (heading "Portable non-nil fields"; not a `master` link; the SHA is the
  `deps.edn` pin, so a later pin change also changes this link); nil and
  wrong types give host exceptions with no `ex-data`;
  missing keys in `map->Name` give `NullPointerException`; the metadata
  has no effect without `:value-type true`. Add the non-nil enforcement
  and field storage type rows to the differences table, with the cells in
  plan.md (Key decisions, differences tables), the same as the Ramora
  table.
- [x] Add one sentence on `:non-nil` with `:value-type true` to the
  `defclosedrecord` docstring; regenerate `doc/API.md`.
- [x] Add non-nil fields to the value-type entry in `CHANGELOG.md`
  `## [Unreleased]`.
- [x] Commit slice 6.

## Slice 7: finish

- [x] Update `mementum/state.md` to the current state.
- [x] Run T-ID, T-VAL, T-11 and T-RT (plan.md, Test commands).
- [x] Run the quality checks: `bb clj-fmt:check`, `bb clj-kondo:lint`,
  `bb vale`, `bb api-docs --check`, `bb dev/link_check_test.clj`,
  `bb lychee:check` (`doc/development.md`).
- [x] Commit slice 7. This is the last commit that changes files outside
  the task directory.
- [ ] **Stop and ask the user before the first push** (plan.md, CI and
  push). The task 020 approval for the branch `020-value-type-records`
  is not assumed for this task. Never push `master`.
- [ ] Push: `git push origin 020-value-type-records`. Pull request #1
  (https://github.com/hugoduncan/closed-record/pull/1) is open for the
  branch; do not open a new pull request.
- [ ] Read the run of the pushed commit:
  `gh run list --workflow ci.yml --branch 020-value-type-records --commit <sha>`,
  `gh run watch <id> --exit-status`, `gh run view <id> --json jobs`.
  Every job has conclusion `success`, including
  `Value-type tests (Temurin 21)` and `Value-type tests (Valhalla)`.
  After a failure: fix, commit, push to the same branch, read the new run.
- [ ] Record in implementation.md: the date, the branch, the commit SHA,
  the pull request URL, the run URL and each job conclusion (a failed run
  also gets its cause and fix commit).
- [ ] Run `bb lychee:check` (the CI `Markdown links` job checks every
  tracked `*.md` file, task files included, and checks bare URLs online).
  Fix any failure. Then commit the record (task files only; no push).

## Implementation review follow-up

- [x] In `test/value-type/closed_record/value_type/non_nil_test.clj`,
  reuse `closed-record.probe-support` (`thrown`, `data-of`) instead of
  the local `cause-classes` and `error-type` try/catch helpers:
  `error-type` is `(:type (data-of f))`, and `cause-classes` can walk
  the `ex-cause` chain of `(thrown f)`. The probe-support docstring names
  the value-type suites as users.
- [x] In `non-nil-expansion-test`
  (`test/value-type/closed_record/value_type/representation_test.clj`),
  derive the expected `:non-nil` count in the printed expansion from the
  non-nil field symbols, not the literal `2`, so that part (a) stays
  correct if the fixture form changes.
- [x] Update the ns docstring of
  `test/value-type/closed_record/value_type/representation_test.clj`
  so that it names the VT-I6 check of VT-V2 (`non-nil-expansion-test`).
  The plan review (pass 3) asked for this update in slice 3; slice 4 did
  the same for `value_type_option_test.clj` (VT-V10).

## Test review follow-up

- [x] In `invalid-value-test`
  (`test/value-type/closed_record/value_type/non_nil_test.clj`), check the
  public contract "closed-record does not wrap them; they have no
  `ex-data`". Today the test checks only that the host exception is in the
  cause chain, so an `ex-info` wrapper from closed-record passes. For the
  direct paths (`->NN`, `map->NN` with a nil value and with a missing key,
  `assoc`), check that `(data-of f)` is nil. Do not check the literal
  paths: the reader wraps them in a `ReaderException` with `ex-data`.
  Checked on T-ID: these paths throw `NullPointerException` or
  `ClassCastException` directly, with no `ex-data`.

## Test review, pass 3 follow-up

- [x] In `test/value-type/closed_record/value_type/non_nil_test.clj`,
  remove the overlap of `untagged-nil-test` with
  `valid-construction-test`. Today `valid` has `:u nil` and `r` is
  `(->NN inner "s" nil)`. Thus `valid-construction-test` already builds
  a record with `u` nil on every path, and `untagged-nil-test` checks
  nothing new. Give `u` a value that is not nil in `valid`, in `r` and in
  the positional literal (for example `1`), so that only
  `untagged-nil-test` checks the nil acceptance of the untagged field.
  `paths` then sets `:u nil` on each path.

## Test review, pass 4 follow-up

- [x] In `key-set-precedence-test`
  (`test/value-type/closed_record/value_type/non_nil_test.clj`), add
  `testing` labels for each case (`assoc :z`, and for each `k`: `dissoc`
  and `map->NN` with an extra key). Today the `doseq` over `[:i :s]` has
  no label, so a failure does not tell which field or path broke the
  key-set precedence. The other tests in the file label each field and
  path.

## Docs review follow-up

- [x] Correct the "no `ex-data`" claim for record literals in
  `README.md` ("Non-nil fields", first bullet) and in the public contract
  of `ramora/value-type-records.md` ("Non-nil fields", second bullet).
  Both say that nil and wrong types give host exceptions with no
  `ex-data` on all paths, record literals included. But the reader wraps
  the literal paths in a `ReaderException` with `ex-data`; the host
  exception is in the cause chain (`non_nil_test.clj`, `direct-paths`
  docstring). State that the host exception has no `ex-data` on `->Name`,
  `map->Name` and `assoc`, and that on record literals the reader can
  wrap it (host exception in the cause chain). Make design.md (Public
  contract) agree. Run `bb vale` and `bb lychee:check`.
  - README and Ramora corrected; `bb vale` and `bb lychee:check` clean.
    Open: design.md (Public contract, line ~185) not changed; blocker in
    implementation.md.
  - design.md corrected at close (Public contract, VT-D11 qualities,
    Documentation).

## Code-shaper review follow-up

- [x] In `test/value-type/closed_record/value_type/non_nil_test.clj`, do
  not select the `ex-data` check by the path label string.
  `check-host-exception` checks `(data-of f)` only when `label` is in
  `direct-paths`, and the missing-key case passes the labels as literals.
  A label rename in `paths` or a typo turns the check off with no failure.
  Make each path in `paths` carry a direct flag (for example
  `{:label "->NN" :f ... :direct? true}`) and pass that flag to
  `check-host-exception`; remove `direct-paths`. Also call `f` once
  (`thrown`), and derive the cause classes and the `ex-data` from that
  one exception.
- [x] In `test/value-type/closed_record/value_type/representation_test.clj`,
  derive `plain-form` from `non-nil-form` with `remove-non-nil`, not as a
  second literal form. Today the two forms repeat the type name, the
  fully qualified `VP` tag and the field list; a change to one form only
  makes part (b) compare forms that differ in more than `:non-nil`.
  Part (b) then checks that `remove-non-nil` commutes with the expansion.

## Code-shaper review, pass 2 follow-up

- [x] In `test/value-type/closed_record/value_type/non_nil_test.clj`,
  build the valid base record from one source. Today `valid`
  (`{:i inner :s "s" :u 1}`) and three copies of `(->NN inner "s" 1)`
  (`valid-construction-test`, `paths`, `key-set-precedence-test`) and the
  positional literal values `[inner "s" 1]` state the same field values
  separately; a change to one copy only makes `(= r v)` and the path
  checks compare different records. Add one private `base`
  (for example `(def ^:private base (map->NN valid))`, or `->NN` from
  `(map valid [:i :s :u])`) and use it in the three places; derive the
  positional literal values from `valid`.
  - `base` is `(apply ->NN (positional-values valid))`; `paths`, the
    `->NN` path and the positional literals use `positional-values`.

## Code-shaper review, pass 3 follow-up

- [x] In `test/value-type/closed_record/value_type/non_nil_test.clj`,
  name the non-nil field keys once. Today `[:i :s]` occurs in
  `invalid-value-test` and `key-set-precedence-test`, and `bad` has the
  same keys. A new non-nil field in `NN` needs three edits, and a missed
  edit removes checks with no failure. Add one private `non-nil-keys`
  (or use `(keys bad)`) and use it in both `doseq` forms.
- [x] In the same file, make `check-fields` read the expected values from
  `valid` (`(:i valid)`, `(:s valid)`), not the literals `inner` and
  `"s"`; make the `assoc` cases of `valid-construction-test` use the
  `valid` values too (`(assoc base k (valid k))` for each of
  `field-keys`). Remove the `(let [r base] ...)` alias in
  `valid-construction-test` and `key-set-precedence-test`; use `base`.
  Then `valid` is the only source of the field values.
  - `non-nil-keys` is `(keys bad)`; the `assoc` cases come from
    `field-keys`; no `r` alias.

## Code-shaper review, pass 4 follow-up

- [x] In `non-nil-expansion-test`
  (`test/value-type/closed_record/value_type/representation_test.clj`),
  derive the expected non-nil fields from `non-nil-form`, not the literal
  `'[p s]`. Today the literal repeats the field vector of `non-nil-form`,
  and the count check compares against `non-nil-fields` from the
  expansion; if the emitter drops `:non-nil` from a field, only the
  literal catches it. Add one private value (for example the
  `:non-nil` symbols of `(nth non-nil-form 2)`), compare
  `non-nil-fields` to it, and use its count in the "no other place"
  check. Then `non-nil-form` is the only source of the non-nil fields.
  - `declared-non-nil-fields` is the `:non-nil` symbols of
    `(nth non-nil-form 2)`; both checks use it.

## Code-shaper review, pass 5 follow-up

- [x] In `invalid-value-test`
  (`test/value-type/closed_record/value_type/non_nil_test.clj`), give the
  missing-key case the same data shape as `paths`. Today it builds two
  path maps inline, with no `:label`, and repeats the labels `"map->NN"`
  and `"map literal"` as `testing` strings. Add a private
  `missing-key-paths` (field `k` → vector of `{:label :f :direct?}`, built
  from `(dissoc valid k)`), and loop over it as the nil and wrong-type
  cases loop over `paths`. Also correct the `paths` docstring: it returns
  a vector of maps, not "a map of each path".
- [x] Name the cause-chain walk once. `(take-while some? (iterate ex-cause
  e))` occurs in `cause-classes` (`non_nil_test.clj`) and `first-cause`
  (`errors_test.clj`). Add `causes` (exception → seq of the exception and
  its causes) to `closed-record.probe-support`, and use it in both
  places. The copy in the `subprocess_test.clj` script text stays (it is
  code in a string that a child JVM evaluates).
  - `missing-key-paths` added; `paths` docstring corrected; `causes` in
    `probe-support`, used by `cause-classes` and `first-cause`.

## Code-shaper review, pass 6 follow-up

- [x] Rewrap the two docstrings that the pass-5 follow-up made longer
  than the 80-column wrap that the rest of each file uses:
  `paths` in `test/value-type/closed_record/value_type/non_nil_test.clj`
  (line ~124, 97 columns) and the ns docstring of
  `test/runtime/closed_record/probe_support.clj` (line 3, 91 columns).
  `bb clj-fmt:check` does not rewrap docstrings, so no check finds this.
  - Both docstrings rewrapped to 80 columns; no line over 80 columns in
    either file.

## Code-shaper review, pass 7 follow-up

- [x] Name the fixture expansion steps once. `fixture-expansion` and
  `printed` in
  `test/value-type/closed_record/value_type/representation_test.clj`
  copy the body of `expansion` in
  `test/runtime/closed_record/expansion_fixture.clj` (bind `*ns*` to the
  fixture namespace, `macroexpand-1` of `strip-positions`, then
  `normalise` of `pr-str` with `*print-meta*` true). A change to one copy
  only makes VT-I6 compare expansions that the fixture does not record.
  Split `expansion` into two public functions (for example `expand`:
  form → unprinted expansion; `printed`: form → normalised printed
  text), define `expansion` as their composition, and use both in
  `non-nil-expansion-test`. Remove the local copies. Run T-RT (the
  fixture namespace changes) and T-ID.
- [x] Name the metadata-key removal walk once. `remove-non-nil`
  (`representation_test.clj`) and `strip-positions`
  (`expansion_fixture.clj`) are the same `form/walk-form` over `IObj`
  values with `(not-empty (dissoc (meta x) ...))`; only the keys differ.
  Add one function (for example `strip-meta`: form, keys → form) beside
  `strip-positions`; define `strip-positions` and `remove-non-nil` with
  it.
  - `expand`, `printed`, `expansion` (`printed` of `expand`) and
    `strip-meta` in `expansion_fixture.clj`; local copies in
    `representation_test.clj` removed.
