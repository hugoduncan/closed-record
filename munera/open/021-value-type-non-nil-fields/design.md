# 021 Non-nil fields in value-type records

## Intent

Let a user declare a field of a `:value-type true` closed record as non-nil,
with value-type's field contract:

```clojure
(defclosedrecord Point [^long x ^long y] :value-type true)
(defclosedrecord Segment [^Point ^:non-nil start
                          ^Point ^:non-nil end
                          label]
  :value-type true)
```

On value output, with a value-class tag, on a JVM that supports native
fields, the JVM restricts the field to non-null values (JEP 401 null
restriction). In all other cases, value-type adds constructor checks: for
example, a field with an identity-class tag such as `String`, or any field on
identity fallback. In both cases, nil gives `NullPointerException` and a
value of the wrong type gives `ClassCastException`.

closed-record adds no validation of its own. value-type owns the field
contract, the choice of enforcement and the errors. closed-record owns the
closed key set, as before. This task changes the Ramora design, the
dependency pin, the verification and the documentation to agree with this
split. It is expected to need no emitter change.

## Problem

The value-type records design (task 020) says that `:non-nil` field metadata
is rejected: `defvaluetype*` did not support it. VT-V6 uses that rejection as
its example of a value-type error that passes through.

value-type task 020 (commit `2b9dd72`) adds `:non-nil` support to
`defvaluetype*` for the plain and the record field shapes. closed-record
already passes field metadata into the `defvaluetype*` form with no change.
Thus the option can work with no new emitter code, but:

- the pinned value-type commit (`8bb7c4f`) rejects `:non-nil`, and `c366f15`
  cannot define a closed record with a non-nil field on value output
  (Prerequisite);
- the Ramora design and the VT-V6 test say that `:non-nil` is rejected:
  `ramora/value-type-records.md` (the context fact "`:non-nil` field
  metadata is rejected", and the value-type error example in the public
  contract), the VT-V6 row in
  `ramora/value-type-records/invariants-and-verification.md`, and
  `non-nil-field-test` in
  `test/value-type/closed_record/value_type/errors_test.clj` (reason
  `:non-nil-unsupported`). The README, `doc/`, the docstring and the
  CHANGELOG do not mention `:non-nil`;
- no test shows that the closed-record contract holds for a record with
  non-nil fields, or that the key-set errors stay unchanged.

## Context

### value-type contract (commit `edc3f79`)

From `doc/usage.md` ("Portable non-nil fields" and "`deftype*` forms"):

- Field metadata `:non-nil true` enables the constraint. Absent, `false` and
  `nil` keep ordinary field behaviour. Other values give `:invalid-field`
  with `:reason :invalid-non-nil`.
- The tag must name an already defined concrete, non-primitive class. Identity
  classes, value classes and fallen-back value-type classes are eligible.
  Interfaces, abstract classes, arrays, primitives, forward references and the
  enclosing type are not eligible. Each gives `:invalid-field` with its
  reason.
- A hidden-name field (`__meta`, `__extmap`, `__hash`, `__hasheq`) cannot be
  non-nil (`:reason :hidden-field`).
- Enforcement: native null restriction on value output, when the tag is a
  value class and the JVM supports native fields. Otherwise generated
  constructor checks. The fallback policy controls the representation, not
  the enforcement.
- Enabled fields have storage of the tag class. Constructor parameters stay
  `Object`. The constructor order and arity do not change.
- All constructors, `create`, record literals, compiled record constants and
  reflection reject nil with `NullPointerException` and wrong types with
  `ClassCastException`. The compiler, class initialization and reflection
  can wrap them; the exception is in the cause chain.
- Caller obligation for record shapes: supply each enabled field. `create`
  from a map with no entry for an enabled field fails. An `assoc` that calls
  a constructor rejects nil for an enabled field.
- Real-JVM evidence: Clojure 1.12.6, Java 8 and Valhalla
  `27-jep401ea3+1-1`.

### closed-record decisions that apply

- D-11 Value validation: closed-record does no value validation.
- D-19 Missing primitive field: `map->Name` without a key for a `^long`
  field lets the host `NullPointerException` through. An `ex-info` check was
  rejected: a map literal calls `create` directly, so the two paths would fail
  differently, and the check is value validation (D-11).
- D-20 API compatibility: after the first release (`0.1.190`), the public
  API changes only by accretion.
- VT-D3 Delegate representation to `defvaluetype*`. The value-type records
  design: value-type errors pass through with no change.
- `defclosedrecord` has the same syntax as `defrecord`. `defrecord` ignores
  `:non-nil` metadata.

### Probe observations

Probe: the emitter at `f034647`, value-type at `edc3f79`, Clojure 1.12.6.
Types: `(defclosedrecord Inner [v] :value-type true)` and
`(defclosedrecord Outer [^Inner ^:non-nil a ^String ^:non-nil s c] :value-type true)`.

Valhalla `27-jep401ea3+1-1` with `--enable-preview` (value output;
`Outer` is a value class) and Java 25 (identity fallback, generated checks)
give the same results. All of these behave as the contract says, with no
emitter change.

- Field storage types: `a` is `Inner`, `s` is `String`, `c` is `Object`.
- Valid values construct through `->Outer`, the map literal and the
  positional literal. Keyword lookup, `get` and `check` work.
- `->Outer` with nil gives `NullPointerException`; with a wrong type,
  `ClassCastException`.
- `assoc` of nil to `:a` gives `NullPointerException`; `assoc` of `1` to `:s`
  gives `ClassCastException`; `assoc` of nil to `:c` works.
- `map->Outer` without `:a` gives `NullPointerException`.
- `map->Outer {:zz 1}` gives `:closed-record/extra-keys`: the key-set check
  runs before `create`.
- `assoc` of an undeclared key gives `:closed-record/undeclared-key`;
  `dissoc` of `:a` gives `:closed-record/declared-key-dissoc`.
- A map literal or a positional literal with nil for `a` gives
  `CompilerException` caused by `NullPointerException`.
- `with-meta` works.
- An ordinary closed record with `^String ^:non-nil s` accepts nil.
- `^:non-nil` with no tag, with `:value-type true`, gives
  `CompilerException` caused by value-type's `ExceptionInfo` with
  `:value-type/error :invalid-field` and `:reason :missing-tag`.

With value-type `c366f15`, the definition of `Outer` failed on Valhalla. See
Prerequisite.

## Prerequisite

Status: satisfied by value-type `edc3f79` (fix commit `db102d7`, "resolve
unloaded classes in non-nil field stack maps"), on GitHub `master`. The
reproduction below passes on Valhalla with that commit. The pin is part of
this task (Dependencies).

value-type at `c366f15` cannot define a value class with an enabled field
when a method body has a `reify` that merges a read of that field with
another reference type. The closed-record `getLookupThunk` has this shape,
as `defrecord` has. Minimal reproduction with no closed-record, on Valhalla
`27-jep401ea3+1-1`, `--enable-preview`, Clojure 1.12.6:

```clojure
(defvaluetype Leaf [v])
(defvaluetype A [^Leaf ^:non-nil a]
  clojure.lang.ILookup
  (valAt [this k]
    (reify clojure.lang.ILookupThunk
      (get [thunk target]
        (if (identical? (class target) A) (.-a ^A target) thunk))))
  (valAt [this k nf] nf))
;; => CompilerException compiling reify*
;;    IllegalArgumentException: Could not resolve class A$reify__769
```

The trace goes through `value_type.impl.field_access/rewrite-class`
(`value_backend/prepare`, `loader/capture-loader`) into the class-file API
stack map generator (`ClassHierarchyImpl.resolve`). The rewrite of the
`reify` companion changes the field read to type `Leaf`. The stack map merge
of `Leaf` with the companion type then must resolve the companion class,
which is not yet defined. The same form without the `if` works. On Java 25
the form works.

closed-record does not work around such a defect: the field access rewrite
is value-type's concern (VT-D3), and every record-shaped type with a lookup
thunk has the same shape. VT-V9 reads non-nil fields through the lookup
thunk, so it detects a return of the defect.

## Scope

### Public contract

- With `:value-type true`, field metadata `:non-nil` has value-type's
  meaning (Context). closed-record passes it to `defvaluetype*` with no
  change.
- closed-record adds no check of field values. Nil and wrong-type values give
  the host exceptions from the constructor (`NullPointerException`,
  `ClassCastException`), on all closed-record paths: `->Name`, `map->Name`,
  `assoc` and record literals (map and positional). closed-record does not
  wrap them. They have no `ex-data`.
- A compiled record constant comes from a record value that exists, so it
  cannot hold nil or a wrong type in a non-nil field. value-type owns the
  load of compiled constants with enabled fields (Invariants and ownership).
- `map->Name` without a key for a non-nil field gives
  `NullPointerException`, as D-19 gives for a primitive field.
- Key-set errors come first. On paths that check the key set, a key-set
  failure gives the closed-record `ex-info` with no change, also when a
  non-nil field has no value: `assoc` of an undeclared key, `dissoc` of a
  declared key, and `map->Name` with extra keys.
- `:non-nil` declaration errors are value-type errors. They pass through with
  no change, at macroexpansion.
- Without `:value-type true` (no option or `:value-type false`), closed-record
  ignores `:non-nil` metadata, as `defrecord` does. The field accepts nil.
  The expansion does not change (VT-I2).

### Decisions to record in Ramora

Add these to `ramora/value-type-records/decisions.md`. Numbers are proposals.

- VT-D11 Non-nil fields through value-type.
  - Choice: pass `:non-nil` field metadata to `defvaluetype*` unchanged.
    value-type owns the contract, the enforcement choice and the errors.
    closed-record emits no value check.
  - Rejected: closed-record checks in `->Name`, `map->Name` and `assoc` with
    an `ex-info`. They duplicate value-type, miss record literals and Java
    callers, and are value validation (D-11). Same rationale as D-19.
  - Basis: D-11, D-19, VT-D3.
  - Qualities: + minimality, + one error contract for all paths.
    − Nil and type errors have no `ex-data`.
- VT-D12 `:non-nil` in ordinary mode.
  - Choice: ignore it, as `defrecord` does. The field accepts nil. This is
    public behaviour under D-20.
  - Rejected:
    - An `IllegalArgumentException` at macroexpansion. The released version
      accepts the metadata, so rejection is a change of behaviour, not an
      accretion (D-20).
    - Ordinary enforcement of `:non-nil`. The released version accepts nil
      in such a field, so enforcement is also a change of behaviour (D-20).
      Ordinary enforcement can come only under a new name or a new option.
  - Basis: D-20, O-VT3, `defrecord` syntax parity.
  - Qualities: + compatibility, + parity. − A user can expect enforcement
    where there is none. The docs state the limit.
  - Commitment: high (D-20).

### Emitter

No change is expected. The probe shows that field metadata already reaches
`defvaluetype*`, and that the factories, `assoc` and `with-meta` call the
compiler-generated constructors. If the implementation finds that an emitter
change is necessary, record the reason in `implementation.md` and in the
Ramora embodiment. The change must not add a value check (VT-D11) and must
not change the ordinary expansion (VT-I2).

### Dependencies

- Update the `:value-type-test` pin in `deps.edn` to value-type
  `edc3f79de77bcf044d6234df361e090edc46c813` (or a later commit on `master`
  that keeps the Prerequisite reproduction passing): an explicit HTTPS
  `:git/url` and the full SHA.
- Update the README dependency example to the same SHA.
- Do not change the evidence link in `doc/performance.md`. It points to a
  measurement at `8bb7c4f`.
- The new pin must not break the value-type suite, the Clojure 1.11 case or
  the ordinary suite.

### Verification

Add the new rows to `ramora/value-type-records/invariants-and-verification.md`,
and change the VT-V2 and VT-V6 rows. Numbers are proposals.

- VT-V9 Non-nil fields. In the value-type suite, so it runs on Temurin 21
  (identity fallback) and on Valhalla (value output). Fixture types: a value
  record `Inner`, and a record with a non-nil field tagged `Inner`, a non-nil
  field tagged `String`, and a field with no metadata. Checks:
  - Field storage types are the tag classes; the field with no metadata is
    `Object`.
  - On value output, the record class is a value class.
  - Valid values construct through `->Name`, `map->Name`, `assoc` of each
    declared key, the map literal and the positional literal. The fields read
    back by keyword at a call site (the lookup thunk), by `get` and by field
    access. `with-meta` keeps the fields.
  - For each non-nil field: nil and a wrong type give `NullPointerException`
    and `ClassCastException`, in the cause chain, through `->Name`,
    `map->Name` (nil value and missing key), `assoc`, the map literal and the
    positional literal.
  - The field with no metadata accepts nil on each path.
  - Key-set errors come first: `assoc` of an undeclared key,
    `dissoc` of a non-nil field, and `map->Name` with an extra key and a
    missing non-nil key give the closed-record `ex-info` types.
  - `check` returns a valid record.
- VT-V6. Use another value-type error as the pass-through example: a
  `:non-nil` field with no tag gives `:value-type/error :invalid-field` with
  `:reason :missing-tag`, unchanged, as the cause of the macroexpansion
  failure.
- VT-V10 Ordinary mode. In the ordinary runtime suite (Clojure 1.11, 1.12,
  1.13): a closed record with a `^String ^:non-nil` field and no option
  accepts nil on `->Name`, `map->Name` and `assoc`. Add one form to the
  expansion fixture (VT-I2): an ordinary closed record with a
  `^String ^:non-nil` field and no option. This form is required: it is the
  check that the ordinary expansion with `:non-nil` does not change. Record
  its expansion from the emitter at `f034647`, before any emitter change. The
  recorded expansions of the existing fixture forms do not change.
- VT-I6 (proposed invariant): `:non-nil` field metadata reaches
  `defvaluetype*` unchanged, and the emitter adds nothing else for it
  (VT-D11: no value check). Basis: VT-D11. Enforced by: the emitter puts
  field metadata only in the `defvaluetype*` field vector. Verified by:
  VT-V2.
- VT-V2 Expansion. Add the VT-I6 check. In the value-type suite, expand a
  value-type record form with `:non-nil` fields, and the same form without
  the `:non-nil` metadata, as the expansion fixture does (`macroexpand-1` in
  the fixture namespace, position metadata removed). Checks:
  - (a) In the expansion of the form with `:non-nil`, each field symbol with
    `:non-nil` in the `defvaluetype*` field vector carries `:non-nil true`,
    unchanged. No other place in the expansion carries `:non-nil`.
  - (b) Remove `:non-nil` from the metadata of those field symbols. Then the
    printed expansion (`*print-meta*` true, normalised with the fixture
    `normalise`) equals the printed expansion of the form without
    `:non-nil`.
- Enforcement mode (native or generated) is not verified here. value-type
  owns it and verifies it.
- All existing suites pass with the new pin, in all CI jobs.

### CI

No new job. The Temurin 21 and Valhalla value-type jobs run VT-V9 and the
VT-I6 check of VT-V2. The ordinary jobs run VT-V10.

### Documentation

- README "Value-type records": a "Non-nil fields" part with an example; the
  tag rule in short with a link to value-type's field contract; nil and wrong
  types give host exceptions with no `ex-data`; missing keys in `map->Name`
  give `NullPointerException`; the metadata has no effect without
  `:value-type true`. Add rows to the differences table: non-nil enforcement
  and field storage type. The non-nil enforcement row uses the Intent
  wording: native null restriction only on value output, with a value-class
  tag, on a JVM that supports native fields; otherwise constructor checks.
  Update the dependency SHA.
- `defclosedrecord` docstring: one sentence on `:non-nil` with
  `:value-type true`. Regenerate `doc/API.md`.
- `CHANGELOG.md` `## [Unreleased]`: add non-nil fields to the value-type
  entry.
- Ramora `value-type-records.md`: replace the context fact "`:non-nil` field
  metadata is rejected" with the current contract; update the public contract
  and the differences table (the non-nil enforcement row as in the README);
  add VT-D11 and VT-D12, VT-I6 (`verified-by: VT-V2`), VT-V9, VT-V10;
  change VT-V2 and VT-V6; update the embodiment. Update VT-R1 in place to
  the current risk: the `defvaluetype*` contract can change before the
  value-type release; the signals are VT-V2, VT-V6 and VT-V9. The Ramora
  document rules do not permit status updates or history on design pages.
  Record the `c366f15` defect only as technical evidence (for example, the
  reason that VT-V9 reads non-nil fields through the lookup thunk), not as
  an event.
- `mementum/state.md`: current state.

## Invariants and ownership

- value-type owns: the `:non-nil` declaration rules, enforcement selection,
  field storage types, the nil and type errors, the load of compiled record
  constants, AOT and fallback behaviour of enabled fields.
- closed-record owns: the closed key set and its errors (D-03), which take
  precedence where the path checks keys; the ordinary expansion (VT-I2); the
  pass-through of field metadata (VT-D11, VT-I6).
- D-11 stays true for closed-record: it emits no value validation.

## Out of scope

- A closed-record workaround for the Prerequisite defect.
- Non-nil enforcement for ordinary closed records.
- Wrapping host exceptions in `ex-info`, or `ex-data` for nil and type
  errors.
- Verification of native null restriction, flattening or object size.
- AOT checks beyond the existing VT-V7.
- Clojure 1.11 support for value-type records.
- Serialization of value output.
- A value-type release or a closed-record release.

## Acceptance criteria

1. `deps.edn` and the README pin value-type `edc3f79` (or a later `master`
   commit that keeps the Prerequisite reproduction passing on Valhalla
   `27-jep401ea3+1-1`).
2. VT-V9 passes in the Temurin 21 and the Valhalla value-type CI jobs.
3. VT-V10 passes on Clojure 1.11, 1.12 and 1.13. The expansion fixture has
   the `:non-nil` form, recorded as VT-V10 states, and the existing fixture
   entries are unchanged.
4. VT-V6 uses the new example and passes. VT-V2 has the VT-I6 check and
   passes.
5. The emitter has no value check. Any emitter change is recorded with its
   reason.
6. Ramora, README, docstring, `doc/API.md` and CHANGELOG agree with the
   public contract above. The quality checks (format, lint, prose, API
   freshness, links) pass.
7. All CI jobs pass on the last commit that changes files outside the task
   directory.
