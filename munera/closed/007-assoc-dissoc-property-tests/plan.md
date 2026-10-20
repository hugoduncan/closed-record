# Plan: 007 assoc and dissoc property tests

## Approach

Implement in four slices, each independently verifiable.

1. **Dependency**: add `org.clojure/test.check` 1.1.3 to the `:extra-deps` of the `:test` alias in `deps.edn`.
   No other dependency change.
2. **Property test namespace**: create `test/closed_record/property_test.clj` with:
   - One type `(defclosedrecord P [a b c])`.
   - Generators as in Scope 3: value (nil or small integer), start (vector of three values), key
     (`:a :b :c :x "a"`), step (`[:assoc k v]` or `[:dissoc k]`), step sequence (`gen/vector` 0–100).
   - One property (Scope 4): keeps `x` and a model map; runs I-01 checks on the start value, then for each
     step runs the step check and the I-01 checks in the listed order; stops at the first failure and returns
     a `clojure.test.check.results/Result` with `pass?` false and `result-data {:check <name>}` (plus `:ex`
     when the operation threw). Each check wraps its operation in a `Throwable` catch. Expected throws only
     for `:assoc-undeclared-throws` and `:dissoc-declared-throws`, matched by full `ex-data`
     (`:type`, `:record`, `:key`).
   - One `deftest` (Scope 5): `(quick-check 200 property)`, single `(is (:pass? result) msg)` where `msg`
     shows `:seed`, `[:shrunk :smallest]` and `[:shrunk :result-data]`. No `is` in the property body.
   - Run the three test aliases on unchanged `src`. If a test fails because of the emitter (Scope 6), fix
     `src/closed_record/impl/jvm.clj`, record the cause in `implementation.md`, and run the three aliases again.
3. **Mutation verification** (acceptance 4): for each of the 13 named checks, apply a single change in
   `src/closed_record/impl/jvm.clj`, confirm the property fails naming exactly that check, revert the change,
   and record the change and failure report in `implementation.md`. Follow the mutation rules in
   `mementum/knowledge/testing-generated-types.md`. View checks (`:key-set`, `:entry-set`, `:size`, `:values`)
   mutate the body of their view method, not `seq` or `count`. The `:check` change is in the `assoc` clause:
   the declared-key branch passes `(assoc __extmap k v)` in place of `__extmap` to `new`. Thus `check` throws
   (`:closed-record/extra-keys`) only for a result of `assoc` of a declared key, not for a `->P` value, so the
   change does not break `check` for all values. No mutation is in `core.cljc`. If no `src` change can break a
   check, record the reason.
4. **Documents**: update `ramora/defclosedrecord/embodiment.md` (Artifacts row for the new test namespace,
   `deps.edn` row names `test.check`) and `mementum/state.md` (V-03 done, new namespace and dependency).

Key decisions (from design): model is a plain map; values are nil/small-integer to avoid `##NaN`; catch
`Throwable` not `Exception`; property returns a result value instead of using `is`, so the failed check name
survives shrinking.

## Risks

- A latent emitter fault may surface under long sequences (Scope 6). It shows in the Slice 2 runs on unchanged
  `src`, not in a mutation run. Mitigation: in Slice 2, fix in `src/closed_record/impl/jvm.clj`, record the
  cause in `implementation.md`, and run the three aliases again.
- scry may count the property test as "unknown" rather than passed. Mitigation: keep the test a plain
  `deftest` with one `is`; verify acceptance 3 wording explicitly.
- Mutation checks are sensitive to check order: the property stops at the first failure, so a mutation that
  affects multiple checks reports only the first. Mitigation: choose mutations local to each view method
  body, per the design's I-01 decision note.
- `defclosedrecord` in a `.clj` test file: existing tests set the pattern; follow
  `mementum/knowledge/testing-generated-types.md`.

## Slice order

1. Add `test.check` dependency to `deps.edn`.
2. Create `test/closed_record/property_test.clj` (generators, property, deftest); run all three test aliases;
   fix the emitter if a test fails because of the emitter.
3. Mutation verification of all 13 checks; record results in `implementation.md`.
4. Lint/format checks; update `embodiment.md` and `mementum/state.md`.
