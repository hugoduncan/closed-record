# 008 differential tests

## Intent

Show with a test that a closed record and an equivalent `defrecord` give the same results for each permitted
operation (V-02, S-02). The only intended differences are the closed key set and the type. The test finds any other
difference.

## Problem

The V-01 tests compare each operation with a fixed expected value. They do not compare with `defrecord`. Thus a
difference in a detail that no test names (entry order, not-found values, metadata, `.hashCode`) is not found.
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
   permitted steps. A step is one of:
   - `[:assoc k v]`, `[:conj k v]`, `[:merge k v]`, `[:into k v]`, `[:update k v]`, with `k` a declared key.
     `:update` uses `(constantly v)`.
   - `[:dissoc k]`, with `k` an undeclared key (`:x` or `"a"`).
   - `[:with-meta m]`, with `m` nil or `{:n v}`.
3. One property. It starts with `x` = `(->P a b c)` and `y` = `(->R a b c)`. It applies each step to `x` and to `y`.
   After the start and after each step, it compares `x` and `y` with the checks below, in this order. It stops at
   the first failed check and gives a failure with the check name. Each check catches any `Throwable` from its
   operation, as in task 007.
   - `:type`: `x` is a `P`.
   - `:entries`: `(into {} x)` = `(into {} y)`.
   - `:seq`: `(seq x)` = `(seq y)` (entry order).
   - `:count`: `(count x)` = `(count y)`.
   - `:get`: for each key in `[:a :b :c :x "a"]`, `(get x k ::nf)` = `(get y k ::nf)`.
   - `:keyword`: for each keyword in `[:a :b :c :x]`, `(k x ::nf)` = `(k y ::nf)`.
   - `:contains`: for each key in `[:a :b :c :x "a"]`, `(contains? x k)` = `(contains? y k)`.
   - `:find`: for each key in `[:a :b :c :x "a"]`, `(find x k)` = `(find y k)`.
   - `:meta`: `(meta x)` = `(meta y)`.
   - `:hash-code`: `(.hashCode x)` = `(.hashCode y)`.
   - `:equals`: `(.equals x y)` is true.
   - `:start-equal`: `(= x x0)` = `(= y y0)`, with `x0` and `y0` the start values.
4. One `deftest` runs `(quick-check 200 property)` and has one `is`. The message shows `:seed`, the shrunk input and
   the shrunk `result-data`.
5. If a check fails because of the emitter, fix `src/closed_record/impl/jvm.clj` and record the cause in
   `implementation.md`. If a difference is intended, record it in `ramora/defclosedrecord/purpose.md` and remove it
   from the property.
6. Update `ramora/defclosedrecord/embodiment.md` (Artifacts table) and `mementum/state.md` (V-02 done).

## Out of scope

- Operations that are not permitted (`assoc` of an undeclared key, `dissoc` of a declared key, `empty`). They differ
  from `defrecord` by design (D-04). V-01 and V-03 test them.
- `hash` and `=` between a `P` and an `R`. They differ by design (D-07). V-12 tests them.
- Identity: `dissoc` of an undeclared key returns the same `P`, and a new `R` (I-04).
- Print and read (V-04), primitive fields (V-09), hidden keys (V-14), malli (V-07), performance (V-11).
- A mutation check for each named check. Task 007 did this for V-03; the operations here call the same methods.

## Decisions

- Compare with a real `defrecord`, not a model map. The subject of V-02 is parity with `defrecord`.
- Generate the operation sequences with test.check. It is in the `:test` alias, and task 007 gives the pattern.
- `:start-equal` compares `=` in the same type, not across types. It tests I-05 (equality follows the declared
  entries) without the type difference.

## Acceptance criteria

1. `test/closed_record/differential_test.clj` has the property in Scope 3, run as in Scope 4.
2. `clojure -M:test -m scry.cli`, with no alias, with `:clj-1.12` and with `:clj-1.13`, exits 0 with zero failures
   and zero errors.
3. Two mutations in `src` each make the test fail, and `implementation.md` records them with the failure report:
   a change in `seq` that changes the entry order, and a change in `valAt` that ignores the not-found value.
   Each follows the mutation rules in `mementum/knowledge/testing-generated-types.md`. `git diff src` is empty after.
4. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
5. `embodiment.md` has an Artifacts row for `differential_test.clj` (V-02). `mementum/state.md` lists V-02 as done.
