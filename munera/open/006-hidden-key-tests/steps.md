# Steps

## Slice 1: namespace, guard, hidden-key tests

- [ ] Create `test/closed_record/hidden_key_test.clj` with ns form, `(defclosedrecord E [a b])`,
  private helpers `in-test-ns` and `read-literal` (copied from `literal_test.clj`), and
  defs/fns for `lit` (read at run time from `#closed_record.hidden_key_test.E{:a 1 :b 2 :z 3}`)
  and `plain` (`(->E 1 2)`).
- [ ] Add a private helper that asserts `check` throws with `ex-data`
  `:type :closed-record/extra-keys` and `:keys #{:z}`; call it on `lit` at the start of each deftest (guard, Scope 2).
- [ ] Add hidden-key test for size and entries: `count`, `seq`, `keys`, `vals`, `(into {} x)`,
  `iterator-seq` of `.iterator`, `reduce-kv`; each asserts `(= (f plain) (f lit))`.
- [ ] Add hidden-key test for lookup of `:z`: `(get x :z :nf)`, `(:z x)`, `contains?`, `find`.
- [ ] Add hidden-key test for `java.util.Map`: `.size`, `.keySet`, `.values`, `.entrySet`,
  `.containsKey :z`, `.get :z`, `.containsValue 3`.
- [ ] Add hidden-key test for equality and hash: `(= lit plain)`, `hash`, `.hashCode`,
  `(.equals x {:a 1 :b 2})`, `(.equals {:a 1 :b 2 :z 3} x)`.
- [ ] Add hidden-key test for print: `(= (pr-str plain) (pr-str lit))`.
- [ ] Run `clojure -M:test -m scry.cli -n closed-record.hidden-key-test`; verify zero failures and errors.
- [ ] Commit slice 1.

## Slice 2: retention tests

- [ ] Add retention test: `check` of `(assoc lit :a 5)` throws `extra-keys` `#{:z}`.
- [ ] Add retention test: `check` of `(with-meta lit {:m 1})` throws `extra-keys` `#{:z}`.
- [ ] Add retention test: `check` of `(dissoc lit :z)` throws `extra-keys` `#{:z}`, and
  `(identical? lit (dissoc lit :z))`.
- [ ] Add retention test: `check` of `(conj lit [:a 5])` throws `extra-keys` `#{:z}`.
- [ ] Add retention test: `check` of `(merge lit {:b 9})` throws `extra-keys` `#{:z}`.
- [ ] Run the namespace in the scry CLI; verify zero failures and errors.
- [ ] Commit slice 2.

## Slice 3: mutation checks

- [ ] Copy the repository to `/tmp` for mutation runs.
- [ ] For each Scope 3 operation group, apply an emitter change in the copy's
  `src/closed_record/impl/jvm.clj` that exposes `__extmap` on that path (for example:
  give `__extmap` to `RecordIterator` for `iterator`; include `__extmap` in `count`,
  `seq`, `entrySet`, `keySet`, `values`, `containsKey`, `valAt`, `equiv`, `hasheq`);
  run the namespace; record in `implementation.md` the change and the assertion that
  failed; revert the copy.
- [ ] For each Scope 4 operation, apply an emitter change that drops `__extmap` on its
  path (`assoc`, `withMeta`, `without`, `cons` vector branch, `cons` `:else` branch);
  run; record; revert.
- [ ] For any operation with no valid single-path mutation, record the reason in
  `implementation.md`.
- [ ] If a mutation run found a real emitter defect (a test fails on unmutated code),
  fix `src/closed_record/impl/jvm.clj` and record the cause in `implementation.md`.
- [ ] Commit slice 3 (implementation.md records, any emitter fix).

## Slice 4: quality gates and state

- [ ] Run `clj-kondo --lint src test`; fix until no errors and no warnings.
- [ ] Run `cljfmt check src test`; fix until it passes.
- [ ] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and
  `clojure -M:test:clj-1.13 -m scry.cli`; verify each exits 0 with zero failures and errors.
- [ ] Update `mementum/state.md`: list V-14 as done; mention the new test namespace.
- [ ] Commit slice 4.
