# 006 Hidden key tests

## Intent

Fix in tests that the extra keys of a map literal stay hidden (V-14, I-01, I-02, I-09). A value read from a literal
with extra keys must give the same result as the value without them for each operation except `check`. Operations
that make a new value from it must keep the extra keys, so that `check` still finds them (I-07, D-09).

## Problem

A map literal `#ns.T{...}` with keys outside `D(T)` puts them in `__extmap` (K-07). Only `-extra-keys` reads
`__extmap` (I-02). Many emitter methods could expose it: `count`, `seq`, `iterator`, `entrySet`, `keySet`, `values`,
`containsKey`, `valAt`, `equiv`, `hasheq`. Existing tests check a literal with extra keys only for `get`, keyword
call, `contains?`, `=`, `hash`, `.hashCode` and print. Nothing tests `count`, `seq`, the iterator or the
`java.util.Map` views on such a value. An emitter change that exposes `__extmap` in one of these methods makes no
test fail.

For retention, tests cover only `assoc` and `with-meta` (`extra-key-retention-test`).

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [qualities](../../../ramora/defclosedrecord/qualities.md#verification-plan): V-14.
  - [invariants](../../../ramora/defclosedrecord/invariants.md): I-01, I-02, I-07, I-09.
  - [D-09](../../../ramora/defclosedrecord/decisions/closed-key-set.md#d-09-reader-literals-and-printing): reader
    literals and printing (hidden `__extmap`).
- Emitter: `src/closed_record/impl/jvm.clj`.
- Existing tests: `test/closed_record/defclosedrecord_test.clj` (`reader-literal-undeclared-key-test`,
  `equality-ignores-metadata-and-hidden-keys-test`, `extra-key-retention-test`),
  `test/closed_record/literal_test.clj` (`extra-key-literal-print-test`).
- Test rules: `mementum/knowledge/testing-generated-types.md` (sections "Record literals and code constants" and
  "Test design"). A literal in test source is a code constant and loses `__extmap`. Read the literal at run time with
  `read-string` and `*ns*` bound, and assert first that `check` throws `extra-keys`.

### Observed facts (2026-10-20, Clojure 1.11.4, branch `master`, commit `1e277e5`)

Type `(defclosedrecord E [a b])`. `lit` is `#ns.E{:a 1 :b 2 :z 3}` from `read-string`. `plain` is `(->E 1 2)`.
For each operation `f` below, `(f lit)` is `=` to `(f plain)`:

| Group | Operations |
|---|---|
| Size and entries | `count`, `seq`, `keys`, `vals`, `(into {} x)`, `iterator-seq` of `.iterator`, `reduce-kv` |
| Lookup of `:z` | `(get x :z :nf)`, `(:z x)`, `contains?`, `find` |
| `java.util.Map` | `.size`, `.keySet`, `.values`, `.entrySet`, `.containsKey :z`, `.get :z`, `.containsValue 3` |
| Equality and hash | `(= x plain)`, `hash`, `.hashCode`, `(.equals x {:a 1 :b 2})`, `(.equals {:a 1 :b 2 :z 3} x)` |
| Print | `pr-str` |

`check` throws `extra-keys` with `#{:z}` for each of these values made from `lit`: `(assoc lit :a 5)`,
`(with-meta lit {:m 1})`, `(dissoc lit :z)`, `(conj lit [:a 5])`, `(merge lit {:b 9})`. `(dissoc lit :z)` is
`identical?` to `lit`.

## Scope

In scope:

1. A test namespace `closed-record.hidden-key-test` in `test/closed_record/hidden_key_test.clj`.
2. A guard: each test first asserts that `check` of `lit` throws `extra-keys` with `#{:z}`.
3. Hidden keys: each operation in the table in Context. The test asserts `(= (f plain) (f lit))`. A failure report
   shows both results.
4. Retention: each operation in the last paragraph of Context. The test asserts the `check` error data
   (`:type` and `:keys`). For `dissoc`, the test also asserts `identical?`.
5. If a test fails because of the emitter, fix the emitter in `src/closed_record/impl/jvm.clj`. Record the cause in
   `implementation.md`.

## Out of scope

- Code constants and print with `*print-dup*` (V-04, V-05, task 003).
- `map->Name` of a map with extra keys. It throws `extra-keys` (V-01).
- Retention through serialization (`java.io.Serializable`).
- Changes to existing tests, also where a row repeats an existing assertion.
- Differential tests against `defrecord`. A `defrecord` literal shows its extra keys, so it is not a counterpart.

## Decisions

- Expected values come from `plain`, not fixed values. The design commitment is that `__extmap` has no effect
  (I-02). A comparison with the same value without extra keys states this directly.
- Rows that repeat an existing assertion stay in the table. The new namespace is the one place for V-14.
- Each retention case in Scope 4 reaches a different path in the emitter (`src/closed_record/impl/jvm.clj`):
  - `assoc`: method `assoc`.
  - `with-meta`: method `withMeta`.
  - `dissoc`: method `without`, which returns `this` for a key outside `D(T)`.
  - `conj` of a vector pair: method `cons`, vector branch.
  - `merge` of a map: `merge` calls `conj` with the map. Method `cons`, `:else` branch (a loop of `assoc`).
- `update` and `into` are not retention cases:
  - `update` calls `assoc`. This is the path of the `assoc` case.
  - `into` calls `conj` once for each element. For vector pairs, this is the path of the `conj` case. For a map,
    each element is a map entry and goes to the map-entry branch of `cons`. No case in Scope 4 reaches this branch.
    This task does not add a case for it.

## Acceptance criteria

1. `test/closed_record/hidden_key_test.clj` has an assertion for each operation in Scope 3 and Scope 4, and the
   guard in Scope 2.
2. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors.
3. Each operation in Scope 3 and Scope 4 has an assertion that fails when the behaviour that it checks is broken.
   `implementation.md` lists, for each operation, the change in `src` and the assertion that failed (for example:
   `iterator` gives `__extmap` to `RecordIterator`). One change can cover more than one operation if each of these
   operations fails with it. A change counts only if it follows the mutation rules in
   `mementum/knowledge/testing-generated-types.md` (section "Test design"). If no change in `src` breaks an
   operation, `implementation.md` gives the reason.
4. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
5. `mementum/state.md` lists V-14 as done.
