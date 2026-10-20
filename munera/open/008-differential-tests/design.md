# 008 differential tests

## Intent

Show with a test that a closed record and an equivalent `defrecord` give the same results for each permitted
operation (V-02, S-02). The intended differences are the closed key set (D-02, D-05, D-08, D-09), the type
(D-07), and the identity of a `dissoc` result (D-04). Out of scope lists each of them. The test finds any other
difference.

## Problem

The V-01 tests compare each operation with a fixed expected value. The V-08, V-09 and V-12 tests compare some
operations with `defrecord`, but only on values that a factory call, one `assoc` or one `with-meta` makes. No test
compares the two types after a sequence of operations. Thus a difference that shows only after a sequence of
`assoc`, `conj`, `merge`, `into`, `update`, `dissoc` or `with-meta` steps (entry order, not-found values, metadata,
`.hashCode`) is not found.
I-05 and the quality target "It behaves like `defrecord`, except for closed keys" list differential tests as
evidence, but V-02 has no test.

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [qualities](../../../ramora/defclosedrecord/qualities.md#verification-plan): V-02.
  - [purpose](../../../ramora/defclosedrecord/purpose.md#behaviour): behaviour tables, S-02.
  - [invariants](../../../ramora/defclosedrecord/invariants.md): I-05.
- Task 007 (closed) made `test/closed_record/property_test.clj`, a test.check property with a model map. This task
  uses the same structure: generators, named checks, stop at the first failed check, one `is` in the `deftest`.
- `org.clojure/test.check` 1.1.3 is in the `:test` alias.
- Test rules: `mementum/knowledge/testing-generated-types.md`.

### Observed facts (2026-10-20, Clojure 1.11.4, branch `master`, commit `820e889`)

`(defclosedrecord P [a b c])`, `(defrecord R [a b c])`, `p` is `(->P 0 nil 0)`, `r` is `(->R 0 nil 0)`:

| Expression | `p` | `r` |
|---|---|---|
| `(dissoc x :x)` | `identical?` to `p` | not `identical?`, `=` to `r` |
| `(seq x)` | `([:a 0] [:b nil] [:c 0])` | the same |
| `(into {} (conj x [:a 1]))`, `update`, `merge` | same entries | same entries |
| `(get x :x 9)`, `(:x x 7)`, `(find x :a)` | `9`, `7`, `[:a 0]` | the same |
| `.hashCode` | equal to `r` | equal to `p` |
| `(.equals p r)` | true | |
| `hash` | not equal to `r` (type hash, D-07) | |

## Scope

In scope:

1. A test namespace `closed-record.differential-test` in `test/closed_record/differential_test.clj`, with
   `(defclosedrecord P [a b c])` and `(defrecord R [a b c])`.
2. Generators, as in task 007: a value (nil or a small integer), a start `[a b c]`, and a sequence of 0 to 100
   permitted steps. A step is one of these, with the form that it applies to a value `x`:
   - `[:assoc k v]`: `(assoc x k v)`. `[:conj k v]`: `(conj x [k v])`. `[:merge k v]`: `(merge x {k v})`.
     `[:into k v]`: `(into x {k v})`. `[:update k v]`: `(update x k (constantly v))`. `k` is a declared key.
     `conj`, `merge` and `into` reach the three `cons` branches: vector pair, map, and map entry (`into` reduces
     over the map entries).
   - `[:dissoc k]`: `(dissoc x k)`, with `k` an undeclared key (`:x` or `"a"`).
   - `[:with-meta m]`: `(with-meta x m)`, with `m` nil or `{:n v}`.
3. One property. It starts with `x` = `(->P a b c)` and `y` = `(->R a b c)`. These two factory calls are setup:
   they are not caught. The property applies each step to `x` and to `y`. After the start and after each step, it
   compares `x` and `y` with the checks below, in this order. It stops at the first failed step or check and gives a
   failure whose `result-data` has the name in `:check`. The keys are the candidate keys `[:a :b :c :x "a"]`.
   - `:type`: `x` is a `P`.
   - `:entries`: `(into {} x)` = `(into {} y)`.
   - `:seq`: `(seq x)` = `(seq y)` (entry order).
   - `:count`: `(count x)` = `(count y)`.
   - `:get`: for each key `k`, `(get x k ::nf)` = `(get y k ::nf)`.
   - `:keyword`: for each keyword `k` in `[:a :b :c :x]`, `(k x ::nf)` = `(k y ::nf)`.
   - `:contains`: for each key `k`, `(contains? x k)` = `(contains? y k)`.
   - `:find`: for each key `k`, `(find x k)` = `(find y k)`.
   - `:size`: `(.size x)` = `(.size y)`.
   - `:is-empty`: `(.isEmpty x)` = `(.isEmpty y)`.
   - `:map-get`: for each key `k`, `(.get x k)` = `(.get y k)`.
   - `:contains-key`: for each key `k`, `(.containsKey x k)` = `(.containsKey y k)`.
   - `:key-set`: `(.keySet x)` = `(.keySet y)`.
   - `:values`: `(.values x)` = `(.values y)`.
   - `:entry-set`: `(.entrySet x)` = `(.entrySet y)`.
   - `:contains-value`: for each value `v` in `(vals y)` and `::nf`, `(.containsValue x v)` = `(.containsValue y v)`.
   - `:meta`: `(meta x)` = `(meta y)`.
   - `:hash-code`: `(.hashCode x)` = `(.hashCode y)`.
   - `:equals`: `(.equals x y)` is true.
   - `:start-equal`: `(= x x0)` = `(= y y0)`, with `x0` and `y0` the start values.
   - `:start-hash`: `(= (hash x) (hash x0))` = `(= (hash y) (hash y0))`.

   Throws. The outcome model is the model of task 007 (`attempt`, `judge`):
   - Each step runs once on `x` and once on `y`, each in `attempt`. If it throws on either side, the property fails
     with the name `:step`.
   - Each check runs each of its operations once on `x` and once on `y`, each in `attempt`. If an operation throws
     on either side, the check fails.
   - A throw on both sides also fails, also with the same exception class. Each step and each check is a permitted
     operation on valid input, so `defrecord` does not throw.

   The failure `result-data` has `:check`, and these keys:
   - `:step`: the step, for `:step` only.
   - `:side` and `:ex`, if an operation threw: `:closed` (on `x`) or `:record` (on `y`), and the `Throwable`. If both
     sides threw, the data gives the `x` side.
   - `:closed` and `:record`, if no operation threw: the two results that differ. `:type` and `:equals` have one
     operation, so they give only `:closed`, the result on `x` (`(class x)` or `(.equals x y)`).
   - `:key`: the first key that fails, for `:get`, `:keyword`, `:contains`, `:find`, `:map-get` and
     `:contains-key`. `:value`: the first value that fails, for `:contains-value`. These checks stop at that key or
     value.
4. One `deftest` runs `(quick-check 200 property)` and has one `is`. The message shows `:seed`, the shrunk input and
   the shrunk `result-data`.
5. If a check fails because of the emitter, fix `src/closed_record/impl/jvm.clj` and record the cause in
   `implementation.md`. If a difference can be intended, stop and ask the user. A human decides if a difference is
   intended. The agent does not decide it. A new intended difference changes public behaviour (commitment high,
   D-20, principle 15). For an intended difference, after the human decision:
   - (a) It refers to an existing D-id, or it gets a new decision in `ramora/defclosedrecord/decisions/` and an
     entry in the index in `ramora/defclosedrecord/decisions.md`.
   - (b) The Behaviour tables in `ramora/defclosedrecord/purpose.md` show it, with that D-id.
   - (c) Out of scope in this design gets a bullet for it, with that D-id.
   - (d) The property does not compare it.
6. Update `ramora/defclosedrecord/embodiment.md` (Artifacts table) and `mementum/state.md` (Status section): add
   `test/closed_record/differential_test.clj` (`closed-record.differential-test`, V-02) to the "Tests:" list of test
   files, and move V-02 from "Not done" to the "Verification plan done" list.

## Out of scope

- Operations that are not permitted (`assoc` of an undeclared key, `dissoc` of a declared key, `empty`, `map->Name`
  with an undeclared key). They differ from `defrecord` by design (D-02, D-05, D-08). V-01 and V-03 test them.
- `hash` and `=` between a `P` and an `R`. They differ by design (D-07). V-12 (`cross-type-test`) tests `=`. V-01
  (`hash-test`) tests the `hash` formula (type hash XOR `mapHasheq`).
- Identity: `dissoc` of an undeclared key returns the same `P`, and a new `R` (I-04, D-04).
- Print and read (V-04), primitive fields (V-09), hidden keys (V-14, D-09), malli (V-07), performance (V-11).
- A mutation check for each named check. Earlier mutation runs changed each emitter method that the checks and
  steps call: V-01 (task 002), V-05 (task 003), V-08 and V-09 (task 004), V-12 (task 005), V-14 (task 006) and V-03
  (task 007). The records are in `munera/closed/*/implementation.md`. No run changed the body of `meta` (it returns
  `__meta`), `isEmpty` (it calls `count`), `valAt k` (it calls `valAt k nil`) or `get` (it calls `valAt k`).
  Acceptance 3 shows that this property finds a fault.

## Decisions

- Compare with a real `defrecord`, not a model map. The subject of V-02 is parity with `defrecord`.
- Generate the operation sequences with test.check. It is in the `:test` alias, and task 007 gives the pattern.
- `:start-equal` compares `=` in the same type, not across types. It tests I-05 (equality follows the declared
  entries) without the type difference. `:start-hash` does the same for `hash` (`hasheq`), because `hash` across
  types differs by design (D-07).
- Metadata and hash: `withMeta` copies the `__hash` and `__hasheq` caches. The property reads a hash after the start
  and after each step, thus before each `[:with-meta m]` step. Thus a fault that puts `__meta` into a hash method
  does not always change a result. V-12 (`equality_test.clj`, `metadata-test`: both values exist before a hash call)
  verifies that metadata has no effect on hash. The property does not.
- No keyword call site in the property. A site keeps the types that it saw, so its state depends on earlier trials.
  V-06, V-08 and V-09 (`field_value_test.clj`) give each site to one test.
- Coverage of V-02. Each permitted operation in the purpose Behaviour tables and in C-02 to C-08 has a step or a
  check in Scope 2 and 3, or a V-id that covers it:

  | Operation | Method | Covered by |
  |---|---|---|
  | `->Name` (C-06) | constructor | the start: each check runs on `(->P a b c)` and `(->R a b c)` |
  | `map->Name` (C-06) | `map->Name`, `create` | V-01 (`map-factory-test`), V-09 (`map-factory-hint-test`, against `defrecord`) |
  | `get` (C-02) | `valAt k nf` | `:get` |
  | keyword call `(k x nf)` (C-02) | `valAt k nf` (through `Keyword.invoke`) | `:keyword` |
  | keyword call site `(:k x)` (C-02) | `getLookupThunk` | V-06, V-08 (`nil-lookup-test`), V-09 (`keyword-call-hint-test`) |
  | `.field` (C-02) | none (compiled field read) | V-08 (`nil-lookup-test`) |
  | `contains?` | `containsKey` | `:contains` |
  | `find` | `entryAt` | `:find` |
  | `count`, `seq`, `(into {} x)` | `count`, `seq`, `iterator` | `:count`, `:seq`, `:entries` |
  | `assoc`, `conj`, `merge`, `into`, `update` of a declared key (C-03) | `assoc`, `cons` | the steps, then each check |
  | `dissoc` of an undeclared key | `without` | the `:dissoc` step, then each check. Identity: Out of scope (D-04) |
  | print, reader literals (C-07) | none in `src` | V-04 (Out of scope) |
  | `meta`, `with-meta` (C-08) | `meta`, `withMeta` | `:meta`, the `:with-meta` step |
  | `=` in the same type (C-08) | `equiv` | `:start-equal` |
  | `=` with another type (an `R`) | `equiv` | V-12 (`cross-type-test`). Out of scope (D-07) |
  | `=` with a plain map | `equiv` | V-12 (`plain-map-test`, against `defrecord`) |
  | `.equals` with a `java.util.Map` (C-08) | `equals` | `:equals` |
  | `hash` (C-08) | `hasheq` | `:start-hash`. Metadata: V-12 |
  | `.hashCode` (C-08) | `hashCode` | `:hash-code`. Metadata: V-12 |
  | `record?` (C-08) | none (`instance?` of `IRecord`) | V-01 (`record-basics-test`) |
  | `java.util.Map` views (I-09) | `size`, `isEmpty`, `get`, `containsKey`, `keySet`, `values`, `entrySet`, `containsValue` | `:size`, `:is-empty`, `:map-get`, `:contains-key`, `:key-set`, `:values`, `:entry-set`, `:contains-value` |

  `.get` calls `valAt k`, and `valAt k` calls `valAt k nil`.

## Acceptance criteria

1. `test/closed_record/differential_test.clj` has the property in Scope 3, run as in Scope 4.
2. `clojure -M:test -m scry.cli`, with no alias, with `:clj-1.12` and with `:clj-1.13`, exits 0 with zero failures
   and zero errors.
3. Two mutations in `src/closed_record/impl/jvm.clj` each make the test fail, with the given check name in
   `[:shrunk :result-data :check]`. `implementation.md` records them with the failure report.
   - `seq` gives the entries in a different order (for example, reversed). Expected: `:seq`.
   - `valAt k nf` ignores the not-found value: the default branch gives nil in place of `nf`. Only `valAt k nf`
     changes. `valAt k` gives nil as `nf`, so its result does not change. Expected: `:get`, with `:key :x`.

   Each fault shows at the start value, so each input fails, and no earlier check reads the changed result
   (`:entries` reads `iterator`, which calls `valAt k`). Each mutation follows the mutation rules in
   `mementum/knowledge/testing-generated-types.md`. `git diff src` is empty after.
4. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
5. `embodiment.md` has an Artifacts row for `differential_test.clj` (V-02). `mementum/state.md` has the changes in
   Scope 6.
