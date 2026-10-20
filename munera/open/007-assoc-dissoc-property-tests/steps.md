# Steps: 007 assoc and dissoc property tests

## Slice 1: dependency

- [ ] Add `org.clojure/test.check {:mvn/version "1.1.3"}` to the `:extra-deps` of the `:test` alias in `deps.edn`.
- [ ] Verify the dependency resolves: `clojure -M:test -e "(require 'clojure.test.check)"` succeeds.

## Slice 2: property test namespace

- [ ] Create `test/closed_record/property_test.clj` with ns `closed-record.property-test`, requiring
      `clojure.test`, `clojure.test.check`, `clojure.test.check.generators`,
      `clojure.test.check.properties`, `clojure.test.check.results`, `closed-record.core`.
- [ ] Define the type `(defclosedrecord P [a b c])` and `D` as `#{:a :b :c}`.
- [ ] Write the value generator: `(gen/one-of [(gen/return nil) gen/small-integer])`.
- [ ] Write the start generator: a vector of three values.
- [ ] Write the key generator: one of `:a :b :c :x "a"`.
- [ ] Write the step generator: `[:assoc k v]` or `[:dissoc k]`.
- [ ] Write the step-sequence generator: `(gen/vector step 0 100)`.
- [ ] Write a check helper: runs an operation, catches `Throwable`, returns pass or a failure Result with
      `result-data {:check <name>}` and `:ex` when the operation threw.
- [ ] Implement the 8 I-01 checks in order: `:keys`, `:key-set`, `:entry-set`, `:contains`, `:count`,
      `:size`, `:values`, `:check` (per the operations in design Scope 4).
- [ ] Implement the 5 step checks: `:assoc-type`, `:assoc-entries`, `:assoc-undeclared-throws`
      (full `ex-data` `{:type :closed-record/undeclared-key :record P :key k}`),
      `:dissoc-declared-throws` (full `ex-data` `{:type :closed-record/declared-key-dissoc :record P :key k}`),
      `:dissoc-undeclared-identical`.
- [ ] Implement the property: build start value `(->P a b c)` and model `{:a a :b b :c c}`; run I-01 checks
      on the start; for each step run its step check then the I-01 checks on the new `x`; after an expected
      throw, `x` and the model do not change; stop at the first failure; return `true` if all pass. No `is`
      in the body.
- [ ] Write the `deftest`: `(quick-check 200 property)`; one `(is (:pass? result) msg)` with `msg` showing
      `:seed`, `[:shrunk :smallest]` and `[:shrunk :result-data]`.
- [ ] Run `clojure -M:test -m scry.cli`: exit 0, zero failures/errors, property test counted as passed.
- [ ] Run `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`: exit 0, zero
      failures/errors.
- [ ] If a run on unchanged `src` fails because of the emitter, fix `src/closed_record/impl/jvm.clj`, record
      the cause in `implementation.md`, and run the three aliases again.

## Slice 3: mutation verification (acceptance 4)

For each check: apply one change in `src`, run the property test, confirm `[:shrunk :result-data]` names
exactly that check, revert the change, and record the change and failure report in `implementation.md`.
Follow the mutation rules in `mementum/knowledge/testing-generated-types.md` (section "Test design").

- [ ] Mutate for `:assoc-type` (e.g. `assoc` returns a plain map instead of a `P`).
- [ ] Mutate for `:assoc-entries` (e.g. `assoc` drops or alters the value of the key).
- [ ] Mutate for `:assoc-undeclared-throws` (e.g. `assoc` of an undeclared key does not throw, or wrong `ex-data`).
- [ ] Mutate for `:dissoc-declared-throws` (e.g. `without` returns `this` for a declared key).
- [ ] Mutate for `:dissoc-undeclared-identical` (e.g. `without` returns a copy for an undeclared key).
- [ ] Mutate for `:keys` (change in `seq`).
- [ ] Mutate for `:key-set` (change in the `keySet` method body only).
- [ ] Mutate for `:entry-set` (change in the `entrySet` method body only).
- [ ] Mutate for `:contains` (change in `containsKey`).
- [ ] Mutate for `:count` (change in `count`).
- [ ] Mutate for `:size` (change in the `size` method body only).
- [ ] Mutate for `:values` (change in the `values` method body only).
- [ ] Mutate for `:check`: in the `assoc` clause of `jvm.clj`, the declared-key branch passes
      `(assoc __extmap k v)` in place of `__extmap` to `new`. `check` then throws
      (`:closed-record/extra-keys`) only for a result of `assoc` of a declared key, not for all values.
- [ ] For any check with no valid breaking change in `src`, record the reason in `implementation.md`.
- [ ] Verify all mutations are reverted: `git diff src` is empty; rerun `clojure -M:test -m scry.cli`, exit 0.

## Slice 4: lint, format, documents

- [ ] Run `clj-kondo --lint src test`: no errors, no warnings.
- [ ] Run `cljfmt check src test`: passes.
- [ ] Update `ramora/defclosedrecord/embodiment.md`: Artifacts row for
      `test/closed_record/property_test.clj` (V-03); `deps.edn` row names `test.check` in the `:test` alias.
- [ ] Update `mementum/state.md`: V-03 done; name `closed-record.property-test` and the `test.check`
      dependency.
- [ ] Final run of all three test aliases: exit 0, zero failures and errors.
