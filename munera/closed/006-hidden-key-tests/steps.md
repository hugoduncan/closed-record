# Steps

## Rule for emitter fixes (all slices)

A run on code that is not mutated is a run in the work tree: Slice 1, Slice 2 and Slice 4. If a test fails in such
a run because of the emitter (design Scope 5), fix `src/closed_record/impl/jvm.clj` in the work tree (not in the
`/tmp` copy), record the cause in `implementation.md`, and do the runs of that slice again. Commit the fix with that
slice. In Slice 4, the fix must keep all three Clojure versions passing (acceptance 2). If a fix after Slice 3
changes the path of an operation, do the Slice 3 mutation check for that operation again on the fixed code. A test
failure in a Slice 3 mutation run is the expected signal, not a defect.

## Slice 1: namespace, guard, hidden-key tests

- [x] Create `test/closed_record/hidden_key_test.clj` with ns form, `(defclosedrecord E [a b])`,
  private helpers `in-test-ns` and `read-literal` (copied from `literal_test.clj`), and
  defs/fns for `lit` (read at run time from `#closed_record.hidden_key_test.E{:a 1 :b 2 :z 3}`)
  and `plain` (`(->E 1 2)`).
- [x] Add a private helper that asserts `check` throws with `ex-data`
  `:type :closed-record/extra-keys` and `:keys #{:z}`; call it on `lit` at the start of each deftest (guard, Scope 2).
- [x] Add hidden-key test for size and entries: `count`, `seq`, `keys`, `vals`, `(into {} x)`,
  `iterator-seq` of `.iterator`, `reduce-kv`; each asserts `(= (f plain) (f lit))`.
- [x] Add hidden-key test for lookup of `:z`: `(get x :z :nf)`, `(:z x)`, `contains?`, `find`.
- [x] Add hidden-key test for `java.util.Map`: `.size`, `.keySet`, `.values`, `.entrySet`,
  `.containsKey :z`, `.get :z`, `.containsValue 3`.
- [x] Add hidden-key test for equality and hash: `(= lit plain)`, `hash`, `.hashCode`,
  `(.equals x {:a 1 :b 2})`, `(.equals {:a 1 :b 2 :z 3} x)`.
- [x] Add hidden-key test for print: `(= (pr-str plain) (pr-str lit))`.
- [x] Run `clojure -M:test -m scry.cli -n closed-record.hidden-key-test`; verify zero failures and errors. On a
  failure, apply the rule for emitter fixes.
- [x] Commit slice 1.

## Slice 2: retention tests

- [x] Add retention test: `check` of `(assoc lit :a 5)` throws `extra-keys` `#{:z}`.
- [x] Add retention test: `check` of `(with-meta lit {:m 1})` throws `extra-keys` `#{:z}`.
- [x] Add retention test: `check` of `(dissoc lit :z)` throws `extra-keys` `#{:z}`, and
  `(identical? lit (dissoc lit :z))`.
- [x] Add retention test: `check` of `(conj lit [:a 5])` throws `extra-keys` `#{:z}`.
- [x] Add retention test: `check` of `(merge lit {:b 9})` throws `extra-keys` `#{:z}`.
- [x] Run the namespace in the scry CLI; verify zero failures and errors. On a failure, apply the rule for emitter
  fixes.
- [x] Commit slice 2.

## Slice 3: mutation checks

- [x] Copy the repository to `/tmp` for mutation runs.
- [x] For each Scope 3 operation, apply an emitter change in the copy's
  `src/closed_record/impl/jvm.clj` that exposes `__extmap` on the path of that operation
  (for example: give `__extmap` to `RecordIterator` for `iterator`; include `__extmap` in
  `count`, `seq`, `entrySet`, `keySet`, `values`, `containsKey`, `valAt`, `equiv`,
  `hasheq`); run the namespace; record in `implementation.md` the operation, the change
  and the assertion that failed; revert the copy. One change can cover several operations
  only if each of them fails with it; record each of these operations.
- [x] For each Scope 4 operation, apply an emitter change that drops `__extmap` on its
  path (`assoc`, `withMeta`, `without`, `cons` vector branch, `cons` `:else` branch);
  run; record; revert.
- [x] For any operation with no valid single-path mutation, record the reason in
  `implementation.md`.
- [x] Commit slice 3 (implementation.md records).

## Slice 4: quality gates and state

- [x] Run `clj-kondo --lint src test`; fix until no errors and no warnings.
- [x] Run `cljfmt check src test`; fix until it passes.
- [x] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and
  `clojure -M:test:clj-1.13 -m scry.cli`; verify each exits 0 with zero failures and errors. On a failure, apply
  the rule for emitter fixes.
- [x] Update `mementum/state.md`: list V-14 as done; mention the new test namespace.
- [x] Commit slice 4.

## Review follow-up

- [x] Use the existing pattern for `check` error data. `defclosedrecord_test.clj` has `thrown` and `data-of`, and
  asserts the full `ex-data` map in one `is` (for example `{:type :closed-record/extra-keys :record R :keys #{:z}}`).
  `hidden_key_test.clj` adds a new helper `check-data` and splits the assertion in `assert-extra-z` into two `is`
  forms without `:record`. Copy `thrown` and `data-of` (implementation.md, plan review pass 2, says to copy
  `data-of`), and assert `{:type :closed-record/extra-keys :record E :keys #{:z}}` in one `is`.
- [x] Remove `guard-test` or its comment. Each other deftest calls `assert-extra-z` on `lit` first, so the comment
  "Without this, every other test could pass with no hidden key" is not true, and the test repeats the guard.

## Docs review follow-up

- [x] Make the ramora text for V-14 agree with what the tests verify. `hidden_key_test.clj` verifies retention for
  `assoc`, `with-meta`, `dissoc`, `conj` and `merge`, and hidden keys also for lookup (`get`, keyword call,
  `contains?`, `find`), the iterator and `reduce-kv`. Three places name only `assoc` and `withMeta`:
  `ramora/defclosedrecord/qualities.md` (V-14 row), `ramora/defclosedrecord/invariants.md` I-02 `scope` and I-07
  `object`. Update the V-14 row to list the operations that it verifies. In I-02 and I-07, state that `conj` and
  `merge` copy `__extmap` through `assoc` (`cons`), and that `dissoc` of a key outside `D(T)` returns the same
  instance (`without`). Do not change the invariant ids or the `verified-by` lines.
- [x] Make the `__extmap` row in `ramora/defclosedrecord/embodiment.md` ("State" table) agree with I-02 and I-07. It
  names only `assoc` and `withMeta` as the operations that keep `__extmap`. Add, as in I-02 and I-07, that `cons`
  (`conj`, `merge`) copies `__extmap` through `assoc`, and that `without` (`dissoc`) of a key outside `D(T)` returns
  the same instance.
- [x] Add the hidden-key tests to the "Artifacts" table in `ramora/defclosedrecord/embodiment.md`. The
  `test/closed_record/*_test.clj` row lists the test kinds ("Behaviour, differential, equality, malli, nil, literal
  and constant tests"), but not the new hidden-key tests. Add them, for example as a row
  `test/closed_record/hidden_key_test.clj` | "Tests that fix hidden keys of a literal and their retention (V-14)",
  as the `compiler_rules_test.clj` row does for V-13.

## Code-shaper review follow-up

- [x] Simplify the `same` calls in `test/closed_record/hidden_key_test.clj`. Each label repeats the code of its fn
  (for example `"(count x)" (fn [x] (count x))`), so a label and its fn can drift apart. Line 60 is 121 characters,
  the only line over 100 in `src` and `test`. Use a short operation name as the label, as in the design Context
  table and the mutation records in implementation.md (for example `"count"`). Where the fn only calls one
  function on `x`, pass the function (for example `count`, `seq`, `keys`, `vals`, `hash`, `pr-str`).
- [x] Make `same` fail on an odd number of `label f` arguments. `(partition 2 label-fs)` drops a last label with no
  fn, so an operation can be lost with no signal. Use `partition-all` and assert each pair has a fn, or take a
  vector of `[label f]` pairs.

## Code-shaper review pass 2 follow-up

- [x] Remove the `(is (= 2 (count pair)) ...)` assertion from `same` in `test/closed_record/hidden_key_test.clj`.
  The `[label f]` vector already shows the shape. A pair with no fn gives `f` nil, and `(f plain)` inside `is`
  reports an error, so the row is not lost. The extra assertion adds one passing assertion for each row and mixes a
  check of test data with the V-14 check.
- [x] Use one name for the value with hidden keys in `test/closed_record/hidden_key_test.clj`. The labels (from the
  design Context table) and `assert-extra-z` use `x`, but each `let` and `same` bind `v`, and the `same` docstring
  says `(f v)`. Because `v` is taken, the `reduce-kv` fn names its value `w`. Bind `x` in each `let` and in `same`,
  and name the `reduce-kv` value `v`.

## Code-shaper review pass 3 follow-up

- [x] Make the guard (design Scope 2) part of `same` in `test/closed_record/hidden_key_test.clj`. The five
  hidden-key deftests each repeat `(let [x (lit)] (assert-extra-z x) (same x ...))`, so the guard holds only by
  convention: a new deftest that calls `same` without `assert-extra-z` can pass with no hidden key. Let `same` take
  only the `[label f]` pairs, read `(lit)`, call `assert-extra-z` on it, then assert each row. Each hidden-key
  deftest is then one `same` call. Keep the explicit guard in `retention-test`.
