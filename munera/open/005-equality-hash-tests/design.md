# 005 Equality and hash tests

## Intent

Fix in tests that equality and hash behave as with `defrecord` (V-12, I-05, D-07). A closed record must be a
replacement for a `defrecord` (O-02). Code that puts records into Clojure or Java collections must see no change.
These paths are emitter code: `equiv`, `equals`, `hasheq` and `hashCode`.

## Problem

The V-01 tests in `test/closed_record/defclosedrecord_test.clj` fix equality and hash with expected values for
one pair of values (`->R 1 2`). They do not compare with `defrecord`. They do not use field values for which `=` and
`.equals` give different results.

`=` compares fields with `=` (`Util.equiv`). `.equals` compares entries with `Util.equals`. These give different
results for `1` and `1N`, for `(int 1)` and `1`, and for `0.0` and `-0.0`. An emitter change can use the wrong
comparison in `equiv` or the wrong map hash in `hasheq` or `hashCode`. For most values, the result does not change
and no test fails.

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [qualities](../../../ramora/defclosedrecord/qualities.md#verification-plan): V-12.
  - [invariants](../../../ramora/defclosedrecord/invariants.md): I-05 (verified by V-02 and V-12).
  - [D-07](../../../ramora/defclosedrecord/decisions/type-structure.md#d-07-equality-and-hash): equality and hash.
  - [purpose](../../../ramora/defclosedrecord/purpose.md#behaviour): the equality and hash table.
- Emitter: `src/closed_record/impl/jvm.clj` (methods `equiv`, `equals`, `hasheq`, `hashCode`, `withMeta`).
- Existing tests: `test/closed_record/defclosedrecord_test.clj` (`equal-same-type-test`, `equal-other-type-test`,
  `equals-java-map-test`, `hash-test`, `hash-code-test`, `equality-ignores-metadata-and-hidden-keys-test`,
  `metadata-test`).
- Differential test pattern and rules: `test/closed_record/field_value_test.clj`,
  `mementum/knowledge/testing-generated-types.md` (sections "Field hints and differential tests" and "Test design").

### Observed facts (2026-10-20, Clojure 1.11.4, branch `master`, commit `196793d`)

Types `(defclosedrecord E [a b])` and `(defrecord DE [a b])`. `x` and `y` are two values of the same type, with
field `:a` from the pair and `:b` = `2`. Each result is the same for `E` and for `DE`:

| `:a` values | `(= x y)` | `hash` equal | `.equals` | `.hashCode` equal |
|---|---|---|---|---|
| `1`, `1N` | true | true | false | true |
| `(int 1)`, `1` | true | true | false | true |
| `0.0`, `-0.0` | true | true | false | false |
| `1`, `1.0` | false | false | false | false |

`r` is `(->E 1 2)` or `(->DE 1 2)`, and `m` is `{:a 1 :b 2}`. Each result is the same for `E` and for `DE`:

| Case | Result |
|---|---|
| `(= r m)`, `(= m r)` | false |
| `(.equals r m)`, `(.equals m r)` | true |
| `(.equals r (java.util.HashMap. m))`, the reverse | true |
| `.hashCode` of `r` and `m` | equal |
| `hash` of `r` and `m` | not equal (type hash, D-07) |
| `(contains? #{m} r)` | false |
| `(.contains (java.util.HashSet. [m]) r)` | true |
| `r` and `(with-meta r {:m 1})` | `=`, `.equals`, `hash` equal, `.hashCode` equal |

`(->E 1 2)` and `(->DE 1 2)`: `=` is false, `.equals` is true, `.hashCode` is equal.

## Scope

In scope:

1. A test namespace `closed-record.equality-test` in `test/closed_record/equality_test.clj`.
2. A differential helper in that namespace. It calls one function with the closed type factory and with the
   `defrecord` factory. The function returns a vector of booleans. The helper asserts that the two vectors are `=`.
   A failure report shows both vectors.
3. Field value pairs: each row of the first table in Context, as a differential test. The function returns the
   four results of the row.
4. Plain maps: each case of the second table in Context, as a differential test. The `hash` case compares `hash`
   of `r` with `hash` of `m`, not across the two record types.
5. Metadata: `r` and `(with-meta r {:m 1})`, and two equal values with different metadata, as differential tests.
   The function returns the four results of the first table.
6. Cross type: `(->E 1 2)` and `(->DE 1 2)`. Fixed expected values: `=` false, `.equals` true, `.hashCode` equal.
7. If a test fails because of the emitter, fix the emitter in `src/closed_record/impl/jvm.clj`. Record the cause in
   `implementation.md`.

## Out of scope

- Hidden keys from a literal (V-14). `equality-ignores-metadata-and-hidden-keys-test` covers equality with hidden
  keys.
- `##NaN` field values. `=` of a value with itself is true by identity, so the field comparison does not run.
- Nil values (V-08, task 004).
- A general differential test of all operations (V-02). A shared differential helper belongs to V-02.
- Changes to the existing V-01 tests.
- Performance of `hash` and `=` (V-11).

## Decisions

- Differential tests for Scope 3, 4 and 5. The design commitment is parity with `defrecord` (D-07). A change in
  Clojure equality then changes both results and no test fails for a parity that still holds.
- Fixed expected values for Scope 6. A cross-type comparison has no `defrecord` counterpart.
- The helper is local to this namespace. Its results are booleans, so it does not pair values with classes as the
  task 004 helper does. A shared helper waits for V-02.
- Do not compare `hash` of a closed value with `hash` of a `defrecord` value. `hasheq` includes the type hash (D-07).

## Acceptance criteria

1. `test/closed_record/equality_test.clj` has a test for each item in Scope 3, 4, 5 and 6.
2. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors.
3. Each row of the first table, each case of the second table, each metadata case and the cross-type case has an
   assertion that fails when the behaviour that it checks is broken. `implementation.md` lists, for each, the change
   in `src` and the assertion that failed (for example: `equiv` compares fields with `.equals`). One change can cover
   more than one item if each of these items fails with it. A change counts only if it follows the mutation rules in
   `mementum/knowledge/testing-generated-types.md` (section "Test design"). If no change in `src` breaks an item,
   `implementation.md` gives the reason.
4. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
5. `mementum/state.md` lists V-12 as done.
