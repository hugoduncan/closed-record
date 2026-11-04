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

- [Decisions](value-type-records/decisions.md): VT-D1 to VT-D10.
- [Invariants and verification](value-type-records/invariants-and-verification.md):
  VT-I1 to VT-I5, VT-V1 to VT-V8, CI decision VT-D10, risks VT-R1 to VT-R5.
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
  metadata, literals, code constants and `check`.
- Differences: hashes are not cached. On value output, the instance has no
  identity.

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

Success: each behaviour test of the defclosedrecord design passes for a
value-type record, on value output and on identity fallback.

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
  - the application property `value-type.fallback` selects silent fallback, a
    warning, or an `Error`;
  - `:non-nil` field metadata is rejected.
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
  (D-20). The option is an accretion.

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
  `:value-type/error :invalid-field` for `:non-nil` metadata, and the fallback
  policy `Error`.
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
| Serialization | as `defrecord` | not promised | as `defrecord` |

The hidden fields stay internal. User code must not read them.
