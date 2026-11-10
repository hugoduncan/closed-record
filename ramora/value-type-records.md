# Value-type records design

This is the architecture description of the `:value-type` option of
`defclosedrecord`. It uses the architecture model in the
`hugoduncan/architecture` repository (file `ramora/INDEX.md`):

```text
Purpose + Context → Architecture → Embodiment → Observed qualities
```

It extends the [defclosedrecord design](defclosedrecord.md). Read that design
first. Identifiers in this design have the prefix `VT-`. Other identifiers
(K-06, D-01, I-05 and so on) are in the defclosedrecord design.

- [Decisions](value-type-records/decisions.md): VT-D1 to VT-D15.
- [Invariants and verification](value-type-records/invariants-and-verification.md):
  VT-I1 to VT-I9, VT-V1 to VT-V12, CI decision VT-D10, risks VT-R1 to VT-R7.
- [Embodiment](value-type-records/embodiment.md): current implementation and
  verification limits.

## Summary

- `(defclosedrecord Name [fields*] :value-type true specs*)` defines a closed
  record through value-type. On a supported JVM the type is a JEP 401 value
  class. Otherwise, value-type's application policy selects an identity class
  or an error.
- Without the option, closed-record does not load value-type, and the
  expansion does not change.
- With the option and without value-type on the classpath, macroexpansion
  fails.
- The record contract does not change: closed keys, equality and hash values,
  literals, code constants and `check`.
- Differences: hashes are not cached. On value output, the instance has no
  identity. The type has no metadata. A map literal with an undeclared key
  throws when it is read. On value output, the instance has no hidden fields
  (VT-D13).

## Purpose

Let users of closed records get value objects with no new syntax and no
second record library.

Outcomes:

- O-VT1: a closed record can be a value object: `identical?` compares fields,
  and the JVM can flatten it or remove its allocation.
- O-VT2: the user selects this per type, explicitly.
- O-VT3: users who do not select it have no new dependency, cost or behaviour.

Priorities: the closed-record contract first, then explicit selection, then
minimality. Performance is not a target. value-type measured value objects as
larger than identity objects, and value records compute hashes on each call.

O-VT1 needs a small layout (VT-K8). For this outcome, value mode gives up two
parts of the ordinary contract: metadata (C-08) and hidden extra keys from
map literals (C-09, D-09). The closed key set does not change (VT-D13).

Success: each behaviour test of the defclosedrecord design passes for a
value-type record, on value output and on identity fallback, except the
tests of metadata and of hidden literal keys (VT-V3). On the pinned Valhalla
build, an array of a value record with at most 64 bits of declared fields is
flat (VT-V12).

## Context

### Interacting systems

- value-type (`org.hugoduncan/value-type`). It has no release yet. Its
  `defvaluetype*` design (`ramora/deftype-star-design.md` in value-type)
  gives the contract that this design uses:
  - the same arguments as `deftype*`, and the expansion evaluates to the class;
  - the record shape (K-06) gets its compiler-generated constructors and
    `create`;
  - value output removes `__hash` and `__hasheq` and ignores their
    constructor arguments; identity output keeps them;
  - the closed record shape (VT-D13; value-type deftype* D9 and D10,
    implemented in value-type commit `e8e0c407`, task 022, and present at
    `4ed8d0a3`). D10 is firm; D9 is provisional. The `defvaluetype*` form
    has the record shape and the option `:value-type/closed true`. Value
    output removes all four hidden fields. In both outputs, the main
    constructor throws `IllegalArgumentException` for a non-nil `__meta` or
    `__extmap` argument, before the non-nil field checks. On value output, a
    captured class that reads or writes a removed field gives `:invalid-field`
    with `:reason :removed-field-access` at definition. A closed form uses the
    receiving expansion on both representations, also on identity fallback;
  - the application property `value-type.fallback` selects silent fallback, a
    warning, or an `Error`. The property `value-type.value-classes` (`auto`
    or `off`) can disable value classes: `off` selects identity fallback and
    disables array allocation;
  - field metadata `:non-nil true` makes a field non-nil, with a tag that
    names a concrete, non-primitive class. Enforcement is native null
    restriction on value output with a value-class tag on a JVM that
    supports native fields; otherwise constructor checks. The field has
    storage of the tag class. Nil and wrong types give
    `NullPointerException` and `ClassCastException`. Declaration errors are
    `:value-type/error :invalid-field`.
- The JVM. Value output needs a Valhalla early-access JDK with
  `--enable-preview`. Other JVMs give identity fallback.
- The Clojure compiler. K-06 to K-08 apply with no change.

### Constraints

- VT-K1 Value-class fields are final (JEP 401). Thus `__hash` and `__hasheq`
  cannot be caches.
- VT-K2 The compiler calls the main constructor with `int` 0 for both hash
  arguments (K-06). The record shape keeps both fields in the `deftype*` form.
- VT-K3 The type must not implement `IType` (K-08). `defvaluetype*` passes
  the `deftype*` arguments with no change and adds no interface.
- VT-K4 value-type is tested only on Clojure 1.12.6. closed-record supports
  Clojure 1.11 (K-01). This design does not change K-01.
- VT-K5 Only Valhalla early-access builds give value output. They are
  published on `download.java.net`, not through `actions/setup-java`, and
  a later build can replace an old build. value-type tests
  `27-jep401ea3+1-1`.
- VT-K6 After the first release, the public API changes only by accretion
  (D-20). The option is an accretion. The option is not released, so its
  contract can change before its release (K-03).
- VT-K7 The compiler-generated `create` calls the main constructor with
  `null` for `__meta` and `RT.seqOrElse(rest)` for `__extmap`. `rest` is the
  input map without the declared keys. Thus the `__extmap` argument is nil
  only when the map has no undeclared key. The constructor with the declared
  fields only passes nil for both. (Observed: `Compiler.java` 8803–8855,
  `emitStatics`, Clojure 1.12.1.) A main-constructor check of `__extmap`
  therefore rejects undeclared keys on all `create` paths: map literals and
  any other `create` call.
- VT-K8 Flattening limit (observed, Valhalla `27-jep401ea3+1-1`,
  `--enable-preview`, macOS arm64, compressed references). A null-restricted
  atomic array is flat only when the instance fields fit in 64 bits:

  | Instance fields | Flat atomic array |
  |---|---|
  | record shape `[^long x]`: `long` + `__meta` + `__extmap` | no |
  | record shape `[^int x ^int y]`: 2 `int` + 2 references | no |
  | closed shape `[^long x]`: `long` (value-type `4ed8d0a3`) | yes |
  | closed shape `[^int x ^int y]`: 2 `int` (value-type `4ed8d0a3`) | yes |
  | closed shape `[^long x ^long y]`: 2 `long` (value-type `4ed8d0a3`) | no |
  | `defvaluetype [^long x]` | yes |
  | `defvaluetype [^int x ^int y]` | yes |
  | `defvaluetype [x y]`: 2 references | yes |
  | `defvaluetype [^long x ^long y]` | no |

  Non-atomic arrays flatten larger layouts only for a class with the internal
  annotation `@LooselyConsistentValue`. Flattening is a JVM choice. A later
  build can change the limit.

## Public contract

```clojure
(defclosedrecord Point [x y] :value-type true)
```

- `:value-type true` selects value-type. `:value-type false` and no option
  give an ordinary closed record. Other values give an
  `IllegalArgumentException` at macroexpansion, as for unsupported options.
- The option can be used with `:no-print` and `:load-ns`.
- Macroexpansion with `:value-type true` fails when `value-type.core/defvaluetype*`
  cannot be resolved. The error is `ex-info` with
  `:type :closed-record/value-type-unavailable` and `:class-name`. The
  compiler wraps it in a `CompilerException`.
  - `:class-name` is the class-name symbol that `deftype*` receives: the
    munged namespace name, a dot and the type name. `Point` in `my-app.geo`
    gives `my_app.geo.Point`. It is a symbol, not a class.
  - A missing library or a load failure (for example on Clojure 1.11):
    the exception of `requiring-resolve` is the cause.
  - `value-type.core` loads but has no `defvaluetype*`: the error has no
    cause.
- value-type errors pass through with no change. Examples:
  `:value-type/error :invalid-field` with `:reason :missing-tag` for a
  `:non-nil` field with no tag, and the fallback policy `Error`.
- Non-nil fields (VT-D11, VT-D12):
  - With `:value-type true`, `:non-nil` field metadata has value-type's
    meaning. closed-record passes it to `defvaluetype*` with no change.
  - Nil and wrong-type values give the host `NullPointerException` and
    `ClassCastException` on all closed-record paths: `->Name`, `map->Name`,
    `assoc`, and map and positional record literals. closed-record does not
    wrap them. On `->Name`, `map->Name` and `assoc`, they have no
    `ex-data`. On record literals, the reader can wrap them in a
    `ReaderException` with `ex-data`; the host exception is in the cause
    chain.
  - `map->Name` without a key for a non-nil field gives
    `NullPointerException`, as D-19.
  - Key-set errors come first, with no change, on the paths that check the
    key set: `assoc` of an undeclared key, `dissoc` of a declared key, and
    `map->Name` with extra keys.
  - `:non-nil` declaration errors are value-type errors. They pass through
    with no change at macroexpansion.
  - Without `:value-type true`, closed-record ignores `:non-nil`, as
    `defrecord` does. The field accepts nil, and the expansion does not
    change (VT-I2).
  - A compiled record constant cannot hold nil or a wrong type in a non-nil
    field. value-type owns the load of compiled constants with enabled
    fields.
- Metadata (VT-D14): the type does not implement `IMeta` or `IObj`, on
  value output and on identity fallback. `meta` returns nil. `with-meta`
  and `vary-meta` give the host `ClassCastException`. Reader metadata on a
  record literal gives the reader error for a value that is not an `IMeta`.
- Undeclared keys in map literals (VT-D15): a map literal
  `#ns.Name{...}` with an undeclared key throws when it is read. The cause
  is the `IllegalArgumentException` of VT-D13; the reader can wrap it.
  closed-record does not wrap it. `map->Name` checks the key set first and
  throws `:closed-record/extra-keys`, as before.
- `check` returns a value-type record, because it cannot hold an
  undeclared key. For other values, `check` does not change.
- Code that uses the option needs value-type at run time, also for AOT
  classes.

All behaviour of the defclosedrecord design applies, with these differences:

| Behaviour | Ordinary | Value output | Identity fallback |
|---|---|---|---|
| Hash values | D-07 | same as D-07 | same as D-07 |
| Hash caching | yes | no | no |
| `identical?` | reference | field values | reference |
| `locking`, weak references | work | throw `IdentityException` | work |
| `__hash`, `__hasheq` fields | caches | absent | always 0 |
| `__meta`, `__extmap` fields | metadata, hidden literal keys | absent | always nil |
| Metadata | as `defrecord` | none: not `IMeta` | none: not `IMeta` |
| Map literal with an undeclared key | read; key hidden; `check` throws | throws at read | throws at read |
| Serialization | as `defrecord` | not promised | as `defrecord` |
| Non-nil enforcement | none: `:non-nil` has no effect | native null restriction with a value-class tag on a JVM that supports native fields; otherwise constructor checks | constructor checks |
| Field storage type | primitive tag: the primitive; other fields: `Object` | `:non-nil` field: the tag class; other fields: as Ordinary | `:non-nil` field: the tag class; other fields: as Ordinary |

The hidden fields stay internal. User code must not read them. On value
output, a user method that reads or writes a hidden field fails at definition
with value-type's `:invalid-field` error (`:reason :removed-field-access`),
which passes through with no change. On identity fallback, `__meta` and
`__extmap` hold nil.
