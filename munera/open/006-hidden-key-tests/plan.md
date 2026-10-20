# Plan

## Approach

Add one test namespace, `closed-record.hidden-key-test`
(`test/closed_record/hidden_key_test.clj`). It is the one place for V-14.

Key decisions:

- Fixture values: `(defclosedrecord E [a b])` in the test namespace. `lit` comes from
  `read-string` of `#closed_record.hidden_key_test.E{:a 1 :b 2 :z 3}` with `*ns*` bound
  (a literal in test source is a code constant and loses `__extmap`). `plain` is `(->E 1 2)`.
  Copy the private helpers `in-test-ns` and `read-literal` from
  `test/closed_record/literal_test.clj`, as each test namespace has its own copy.
- Guard: each `deftest` first asserts that `check` of `lit` throws `extra-keys` with
  `#{:z}`. Without the guard, a test can pass when `lit` has no hidden keys. A shared
  private helper asserts the error data (`:type :closed-record/extra-keys`, `:keys #{:z}`).
- Hidden-key assertions (Scope 3): for each operation `f` in the Context table, assert
  `(= (f plain) (f lit))`. Use `is (= ...)` so a failure shows both results. For
  `.iterator`, compare through `iterator-seq`. Group the assertions by the table groups
  (size and entries, lookup of `:z`, `java.util.Map`, equality and hash, print).
- Retention assertions (Scope 4): for each of `assoc`, `with-meta`, `dissoc`, `conj` of a
  vector pair, `merge` of a map, assert that `check` of the result throws with the error
  data above. For `dissoc`, also assert `(identical? lit (dissoc lit :z))`.
- Test source describes each case in words with durable ids (V-14, I-01, I-02, I-07,
  I-09, D-09). No reference to design sections.
- Mutation check (acceptance 3): for each operation, break the emitter path in
  `src/closed_record/impl/jvm.clj` in a `/tmp` copy of the repository, run the namespace,
  record in `implementation.md` the change and the assertion that failed, revert. One
  change can cover several operations if each fails with it. Follow the mutation rules in
  `mementum/knowledge/testing-generated-types.md` ("Test design"): a change that breaks
  all values or fails at load gives no signal. If no `src` change breaks an operation,
  record the reason.
- Emitter fixes only if a test finds a defect; record the cause in `implementation.md`.

## Risks

- Mutation of some operations may be impossible without breaking namespace load (for
  example `count` used by `check` itself). Acceptance 3 permits a recorded reason.
- The scry CLI differs from the REPL (`*ns*` is `user`). The `in-test-ns` helper and a
  CLI run on all three Clojure versions cover this.
- clj-kondo may warn on the record literal string or the generated factory names; the
  existing test namespaces show the working patterns.

## Slice order

1. Test namespace with fixtures, helpers, guard and hidden-key tests (Scope 2 and 3).
2. Retention tests (Scope 4).
3. Mutation checks and `implementation.md` records (acceptance 3), emitter fixes if any.
4. Lint, format, full runs on 1.11/1.12/1.13, update `mementum/state.md` (V-14 done).
