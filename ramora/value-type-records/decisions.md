# Value-type records decisions

Each decision has the shape of the defclosedrecord
[decisions](../defclosedrecord/decisions.md):
`⟨subject, choice, rejected, rationale, assumptions, qualities, scope, commitment⟩`.
The commitment levels are the same.

## VT-D1 Selection by an explicit option

- Subject: how a user selects value-type for a type.
- Choice: the option `:value-type`, with the value `true` or `false`, in the
  `defrecord` option position.
- Rejected:
  - Select value-type when it is on the classpath. A dependency of a
    dependency could then change the representation with no signal.
  - A separate macro, for example `defclosedvalue`. It adds a public var and
    duplicates the syntax.
  - A dynamic var or a system property. It is global, and it can change the
    type between test and production.
- Rationale: O-VT2. The option is in the form, so a reader of the code sees
  the selection. The `:load-ns` option has the same shape.
- Qualities: + explicitness, + minimality. − Principle 9 says "no options".
  That principle is about switches for the checks (D-12). This option selects
  the representation and does not disable a check, so it adds no test and
  production difference.
- Scope: public API, all hosts.
- Commitment: high.

## VT-D2 Missing library is an error

- Subject: `:value-type true` when value-type cannot be resolved.
- Choice: fail at macroexpansion with
  `:type :closed-record/value-type-unavailable` (VT-D4 gives the check).
- Rejected: emit an ordinary closed record. value-type already has a
  fallback policy for JVM capability. A second, silent fallback in
  closed-record would hide what the user selected, and the application could
  not control it.
- Rationale: explicit selection must give the selected mechanism or an error.
- Qualities: + explicitness, + diagnosability.
- Scope: all hosts.
- Commitment: high.

## VT-D3 Delegate representation to `defvaluetype*`

- Subject: how the emitter makes the class.
- Choice: emit the same expansion as for an ordinary closed record, with the
  `deftype*` form replaced by `(value-type.core/defvaluetype* …)` with the
  same arguments. The emitter adds no capability check and no policy.
- Rejected:
  - Call the value-type loader or transform directly. Internal API, and it
    skips value-type policy.
  - Emit `defvaluetype`. `deftype` rejects the hidden names (E-07) and does
    not emit `create` (K-06).
  - Drop or rename the hidden fields in the form. `create`, both literal
    forms and code constants then fail (D-01). value-type output removes
    them instead (VT-D13).
- Rationale: value-type owns representation and policy. closed-record owns the
  record methods. One form changes, so both modes share the method emitter.
- Assumptions: `defvaluetype*` follows its design (VT-R1).
- Qualities: + `defrecord` parity, + local change. − closed-record depends on
  an unreleased library.
- Scope: JVM emitter.
- Commitment: medium.

## VT-D4 Resolve value-type only when selected

- Subject: when closed-record loads value-type.
- Choice: the JVM emitter calls `requiring-resolve` on
  `value-type.core/defvaluetype*` only when the option is `true`. Any failure
  of resolution gives the VT-D2 error. closed-record does not declare
  value-type as a dependency.
- Rejected:
  - A dependency in `deps.edn`. All users then get it (O-VT3).
  - A check at `closed-record.core` load time. It loads value-type for all
    users.
- Rationale: O-VT3. A version of value-type without `defvaluetype*` also
  gives a clear error.
- Qualities: + minimality, + no cost when not used.
- Scope: JVM emitter.
- Commitment: medium.

## VT-D5 Hidden field layout

- Subject: the hidden fields in value mode.
- Choice: the form has `__meta __extmap ^int __hash ^int __hasheq`, with
  no mutable metadata, and the option `:value-type/closed true` (VT-D13).
  Value output removes all four fields. Identity output keeps them: the hash
  fields hold 0, and `__meta` and `__extmap` hold nil.
- Rejected: the ordinary layout with `^:unsynchronized-mutable` hash fields.
  value-type rejects mutable fields (VT-K1).
- Rationale: K-06 and VT-K2 need all four fields with `int` hints in the
  form. VT-D13 removes them from value output.
- Qualities: + parity.
- Scope: JVM emitter.
- Commitment: medium.

## VT-D6 No hash cache

- Subject: `hasheq` and `hashCode` in value mode.
- Choice: compute `type-hash XOR mapHasheq` and `mapHash` on each call, in
  both representations. The values are as D-07.
- Rejected:
  - Compute the hash in the factories and store it. Each `assoc` then pays
    for a hash, and the constructor arguments change.
  - Cache only on identity fallback. The two representations then have
    different code, and the value output cannot be tested on CI.
- Rationale: VT-K1. One method set for both representations.
- Qualities: + simplicity. − K-05 does not apply to `hash` in value mode.
  A hash costs O(n) in the field count on each call.
- Scope: JVM emitter.
- Commitment: medium.

## VT-D7 No generated access to the hidden fields

- Subject: generated methods that use `__meta`, `__extmap`, `__hash` and
  `__hasheq`.
- Choice: in value mode, no generated method reads or writes them. `assoc`
  calls the constructor with the declared fields only. There is no
  `withMeta` or `meta` (VT-D14). `-extra-keys` returns nil.
- Rejected: pass nil and `0 0` to the main constructor. It has the same
  effect, but the emitter then names fields that value output removes.
- Rationale: value-type rejects access to a removed field. The generated code
  must not depend on the fields.
- Qualities: + robustness.
- Scope: JVM emitter.
- Commitment: low.

## VT-D8 Keep the other methods

- Subject: the methods that use `identical?`: `equiv`, `containsKey`,
  `entryAt`, `assoc`, `without`, `getLookupThunk`.
- Choice: no change (D-21).
- Rationale: on a value object, `identical?` compares fields. `equiv` stays
  correct. `containsKey` and `entryAt` use `this` as the not-found value. A
  field cannot hold a value equal to `this`, because a value object cannot
  contain itself. The other uses compare keywords or classes, which are
  identity objects.
- Qualities: + one method set, + D-21 method size.
- Scope: JVM emitter.
- Commitment: low.

## VT-D9 Hosts without value-type

- Subject: the option on hosts other than the JVM.
- Choice: an emitter that cannot use value-type rejects `:value-type true`
  with the VT-D2 error. It does not ignore the option.
- Rationale: VT-D2. K-02 keeps the API the same on all hosts.
- Scope: future ClojureScript and babashka emitters.
- Commitment: high.

## VT-D10 Value output on CI

See [verification](invariants-and-verification.md#vt-d10-value-output-on-ci).

## VT-D11 Non-nil fields through value-type

- Subject: `:non-nil` field metadata with `:value-type true`.
- Choice: pass `:non-nil` field metadata to `defvaluetype*` unchanged.
  value-type owns the contract, the enforcement choice and the errors.
  closed-record emits no value check.
- Rejected: closed-record checks in `->Name`, `map->Name` and `assoc` with
  an `ex-info`. They duplicate value-type, miss record literals and Java
  callers, and are value validation (D-11). Same rationale as D-19.
- Rationale: D-11, D-19, VT-D3. value-type owns the field contract;
  closed-record owns the closed key set.
- Assumptions: `defvaluetype*` follows its field contract (VT-R1).
- Qualities: + minimality, + one error contract for all paths. − Nil and
  type errors have no `ex-data`.
- Scope: public API, JVM.
- Commitment: high.

## VT-D12 `:non-nil` in ordinary mode

- Subject: `:non-nil` field metadata without `:value-type true`.
- Choice: ignore it, as `defrecord` does. The field accepts nil. This is
  public behaviour under D-20.
- Rejected:
  - An `IllegalArgumentException` at macroexpansion. The released version
    accepts the metadata, so rejection is a change of behaviour, not an
    accretion (D-20).
  - Ordinary enforcement of `:non-nil`. The released version accepts nil in
    such a field, so enforcement is also a change of behaviour (D-20).
    Ordinary enforcement can come only under a new name or a new option.
- Rationale: D-20, O-VT3, `defrecord` syntax parity. The released version
  accepts the metadata and accepts nil in such a field.
- Qualities: + compatibility, + parity. − A user can expect enforcement
  where there is none. The docs state the limit.
- Scope: public API, all hosts.
- Commitment: high (D-20).

## VT-D13 Closed record shape: no hidden fields on value output

- Subject: `__meta` and `__extmap` on value output. They hold 64 bits with
  compressed references, so a value record does not flatten (VT-K8, O-VT1).
- Choice: emit the record shape with the value-type option
  `:value-type/closed true` (value-type deftype* D9), in the `deftype*`
  option position: after `:implements` and the user options, before the
  methods.
  value-type output then:
  - removes `__meta`, `__extmap`, `__hash` and `__hasheq` on value output;
  - keeps all four on identity output;
  - in both outputs, rejects a non-nil `__meta` or `__extmap` argument of
    the main constructor with `IllegalArgumentException`, before the
    non-nil field checks (value-type deftype* D10).
  closed-record emits no check for these arguments. value-type owns the
  option and the exception. A closed form uses the value-type receiving
  expansion on both representations.
- Rejected:
  - closed-record emits a plain `deftype*` with no hidden fields. There is
    then no `create`, so map literals fail, printed records cannot be read,
    and code constants need `IType` (K-08). It needs a second emitter and
    breaks D-01.
  - Keep the fields and make the class loosely consistent
    (`@LooselyConsistentValue`) with non-atomic arrays. The contract does not
    change, but data races can tear values, the annotation is internal to
    the JDK, and each element keeps two reference slots.
  - Remove only `__extmap`. The object gets 4 bytes smaller, but a record
    with a `long` field still does not flatten.
  - Keep metadata outside the object. A value object has no identity, so it
    cannot be the key of a weak map.
  - closed-record changes the bytecode. value-type owns the representation
    (VT-D3).
- Rationale: O-VT1. value-type already removes the hash fields with this
  mechanism. VT-K7 makes one constructor check close all `create` paths.
- Assumptions: value-type provides the closed shape (VT-R6; true from
  value-type `e8e0c407`). JEP 401 still permits the record constructors
  (VT-R2).
- Qualities: + layout: records with at most 64 bits of declared fields can
  flatten (VT-K8); objects are 8 bytes smaller. + A map literal with an
  undeclared key fails at read, not later at `check`. − No metadata
  (VT-D14). − The literal error is an `IllegalArgumentException`, not
  `:closed-record/extra-keys`. − A new value-type contract (VT-R1, VT-R6).
- Scope: JVM emitter, value mode.
- Commitment: medium.

## VT-D14 No metadata in value mode

- Subject: metadata on value-type records.
- Choice: in value mode, the type does not implement `IMeta` or `IObj`, in
  both representations. `meta` returns nil. `with-meta` gives the host
  `ClassCastException`.
- Rejected:
  - `withMeta` throws `ex-info`. Generic code tests `IObj` before it adds
    metadata. With a throwing `withMeta`, that code fails; without `IObj`, it
    skips the record.
  - `withMeta` returns the record without the metadata. The loss is then
    silent.
  - Metadata on identity fallback only. The two representations then have
    different contracts (VT-D6 has the same rationale).
- Rationale: VT-D13 removes `__meta`. A type with no metadata does not claim
  the metadata interfaces.
- Qualities: + explicitness, + generic code works. − C-08 does not apply in
  value mode. − No `ex-data` on `with-meta`.
- Scope: public API, JVM emitter, value mode.
- Commitment: medium.

## VT-D15 Undeclared literal keys fail at read in value mode

- Subject: a map literal with an undeclared key, in value mode.
- Choice: the value-type constructor check of VT-D13 rejects it when the
  reader calls `create` (VT-K7). closed-record does not wrap the exception.
  `check` keeps its contract; it never finds extra keys in a value-type
  record. `map->Name` checks the key set first, as before.
- Rejected:
  - Wrap the exception in `:closed-record/extra-keys`. `create` is
    generated by the compiler and calls no closed-record code (K-07).
  - Drop the extra keys with no error. A key typo is then lost with no
    signal, and `check` cannot find it.
- Rationale: VT-D13 removes `__extmap`. In value mode, K-07 no longer stops a
  read-time check, so D-09 and I-08 do not apply.
- Qualities: + the closed key set holds on all paths, at the location of the
  error. − The error has no closed-record `ex-data`.
- Scope: public API, value mode.
- Commitment: medium.
