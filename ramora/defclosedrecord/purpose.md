# Purpose

## Mission

A Clojure record is an open map:

- `assoc` of any key succeeds.
- `dissoc` of a declared key changes the record into a plain map.

These operations do not fail. Thus key typos and incorrect operations stay hidden until a later failure.

`defclosedrecord` defines a record type with a fixed key set. An operation that changes the key set fails immediately.

The type is for use as an implementation detail. It is a complement to the Clojure 1.13 destructuring
directives `:keys!`, `:select` and `:all`. It does not compete with them.

## Desired outcomes

- O-01 A key error fails at the location of the error.
  Key errors include a typo in a key, a wrong key, or an accidental `dissoc`.
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
- C-08 Support metadata, equality, hashing and `record?`.
- C-09 Find extra keys that come from a reader literal, with `check`.

## Scope

Included:

- The capabilities above.
- Clojure on the JVM.

Excluded:

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
| `map->Name` | missing key → nil | extra key → throw |
| reader literal `#ns.Name{...}` | missing key → nil | extra key → read, hidden, `check` throws |

Failure behaviour: throw `ex-info`. See [D-03](decisions.md#d-03-failure-signal) for the data.

## Success criteria

- S-01 Each row of the behaviour table has a test.
- S-02 Differential tests: a closed record and an equivalent `defrecord` give the same result for each permitted operation.
- S-03 Benchmarks show no significant difference from `defrecord` for keyword lookup, field access,
  `assoc` of a declared key, `hash` and `=`.
- S-04 Tests show compatibility with malli map schemas and nil field values.

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
