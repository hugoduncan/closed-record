# Purpose

## Mission

A Clojure record is an open map:

- `assoc` of any key succeeds.
- `dissoc` of a declared key changes the record into a plain map.

These operations do not fail. Thus key typos and incorrect operations stay hidden until a later failure.

`defclosedrecord` defines a record type with a fixed key set. An operation that changes the key set fails immediately.

Reads are not checked. `(:nmae r)` returns nil, as with `defrecord`. The Clojure 1.13 destructuring directives
`:keys!`, `:select` and `:all` find read errors. The type is a complement to these directives. It does not compete with them.

The type is for use as an implementation detail.

## Desired outcomes

- O-01 A change to the key set fails at the location of the change.
  Examples: `assoc` of a key with a typo, `assoc` of a wrong key, an accidental `dissoc` of a declared key.
- O-02 A closed record can replace a `defrecord`. Call sites that use the record correctly do not change.
- O-03 Reads and `assoc` of declared keys are as fast as with `defrecord`.

## Capabilities

- C-01 Define a type with declared fields, inline protocol and interface implementations, and field type hints.
  The syntax is the same as `defrecord`.
- C-02 Read a field by keyword, `get` or `.field`. Keyword call sites use lookup thunks.
- C-03 `assoc` a declared key. The result has the same type.
- C-04 Reject `assoc` of an undeclared key.
- C-05 Reject `dissoc` of a declared key.
- C-06 Construct with `->Name` and `map->Name`. `map->Name` rejects extra keys.
- C-07 Print as `#ns.Name{...}`. Read map literals and positional literals.
- C-08 Support metadata, equality, hashing and `record?`, the same as `defrecord`.
- C-09 Find extra keys that come from a reader literal, with `check`.

## Scope

Included:

- The capabilities above.
- Clojure on the JVM.

Excluded:

- Key checks on reads (`get`, keyword lookup, `contains?`). Use `:keys!`.
- Validation of values (types, nil checks, schemas). Use malli or spec.
- Optional keys or an open extension map.
- A switch that disables the checks.

Later:

- ClojureScript, babashka and other hosts. The design must not prevent them.

## Behaviour

| Operation | Declared key | Undeclared key |
|---|---|---|
| `get`, keyword call, `.field` | value | not-found value (nil) |
| `contains?` | true | false |
| `assoc`, `conj`, `merge`, `into`, `update` | new record of same type | throw |
| `dissoc` | throw | return the record |
| `empty` | throw | throw |
| `map->Name` | missing key → nil (D-19) | extra key → throw |
| reader literal `#ns.Name{...}` | missing key → nil (D-19) | extra key → read, hidden, `check` throws |

D-19: a missing primitive-hinted field throws a host exception, as with `defrecord`.

Equality and hash are the same as `defrecord` (D-07):

| Operation | Result |
|---|---|
| `=` with a record of the same type | true if the declared fields are equal |
| `=` with another type or a plain map | false |
| `.equals` with a `java.util.Map` | true if the entries are equal |
| `hash` | type hash XOR map `hasheq` |
| `.hashCode` | map `hashCode`, the same as an equal plain map |

Metadata and hidden literal keys do not have an effect on equality or hash.

Failure behaviour: key-set failures throw `ex-info`.
See [D-03](decisions/failure-and-validation.md#d-03-failure-signal) for the data.

## Value model

- Beneficiaries: developers who use records as internal types, and the people and AI agents that change that code.
- Benefits: key-set errors fail at the source. Less time to debug nil values far from the cause.
- Risk reduced: the shape of data does not change silently.
- Costs imposed:
  - Generic code that adds or removes keys throws (for example malli transformers, or code that adds `:db/id`).
  - `dissoc` does not give a plain map. Use `into {}` or `select-keys`.
  - One more dependency.

## Success criteria

- S-01 (O-01) Each row of the behaviour tables has a test.
- S-02 (O-02) Differential tests: a closed record and an equivalent `defrecord` give the same result for each permitted operation.
- S-03 (O-03) criterium benchmarks for keyword lookup, field access, `assoc` of a declared key, `hash` and `=`.
  For each operation, the mean time of the closed record is within 5% of `defrecord`, or the 95% confidence intervals overlap.
- S-04 (O-02) Tests show compatibility with malli map schemas and nil field values.

## Priority

1. Correct closed semantics.
2. Same behaviour as `defrecord` for permitted operations.
3. Same performance as `defrecord`.
4. Portability to other hosts.

## Lifecycle

- Public library on Clojars.
- Long-lived, with a small and stable API.

## Evolution expectations

- Support for ClojureScript and babashka.
- Use with the Clojure 1.13 destructuring directives.
- No growth of the core API is expected.
- After the first release, the public API changes only by accretion (D-20).
