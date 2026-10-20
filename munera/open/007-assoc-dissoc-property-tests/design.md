# 007 assoc and dissoc property tests

## Intent

Fix with property tests that random `assoc` and `dissoc` sequences keep the key set closed (V-03). After each step,
the observable keys are `D(T)` (I-01), `assoc` of a declared key gives a `T` (I-03), and `dissoc` throws for a
declared key and returns `r` for another key (I-04). The example tests check one step from one start value. Property
tests check long sequences from many values.

## Problem

The V-01 tests check each `assoc` and `dissoc` case once, on a value made by `->Name`. They do not check a value that
is the result of many earlier steps, a nil field value made by `assoc`, or a step after a failed step. I-01, I-03 and
I-04 list V-03 in `verified-by`, but V-03 has no test.

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [qualities](../../../ramora/defclosedrecord/qualities.md#verification-plan): V-03.
  - [invariants](../../../ramora/defclosedrecord/invariants.md): I-01, I-03, I-04.
  - [purpose](../../../ramora/defclosedrecord/purpose.md#behaviour): the behaviour tables.
- Emitter: `src/closed_record/impl/jvm.clj`. The checks use the methods `assoc`, `without`, `seq`, `iterator`,
  `count`, `containsKey`, the `java.util.Map` methods `size`, `keySet`, `values` and `entrySet`, and `-extra-keys`
  (protocol `ClosedRecord`, returns `__extmap`).
- `check`: `src/closed_record/core.cljc`. It returns `x` if `(-extra-keys x)` is empty, and throws if not.
- Existing tests: `test/closed_record/defclosedrecord_test.clj` (`assoc-test`, `dissoc-test`, `count-seq-test`,
  `contains-test`).
- Test rules: `mementum/knowledge/testing-generated-types.md`, `mementum/knowledge/dev-tooling.md`.
- `deps.edn`: the `:test` alias has scry only. No property test library is a dependency.
  `org.clojure/test.check` 1.1.3 is the latest release (Maven Central, 2026-10-20).

### Observed facts (2026-10-20, Clojure 1.11.4, branch `master`, commit `65b8a14`)

Type `(defclosedrecord P [a b c])`, `p` is `(->P 0 nil 0)`:

| Step | Result |
|---|---|
| `(assoc p :a 1)` | a `P`, `{:a 1 :b nil :c 0}` |
| `(assoc p :b nil)` | a `P`, `{:a 0 :b nil :c 0}` |
| `(assoc p :x 1)` | throws, `:type` `:closed-record/undeclared-key`, `:key` `:x` |
| `(dissoc p :a)` | throws, `:type` `:closed-record/declared-key-dissoc`, `:key` `:a` |
| `(dissoc p :x)` | `identical?` to `p` |

## Scope

In scope:

1. Add `org.clojure/test.check` 1.1.3 to the `:extra-deps` of the `:test` alias.
2. A test namespace `closed-record.property-test` in `test/closed_record/property_test.clj`. It uses one type,
   `(defclosedrecord P [a b c])`, with three untyped fields. In this design, `D` is `D(P)`, the declared keys
   `#{:a :b :c}`.
3. Generators (`gen` is `clojure.test.check.generators`):
   - a value: `(gen/one-of [(gen/return nil) gen/small-integer])`. Field values and `assoc` values use it.
   - a start: a vector of three values, `[a b c]`. The property makes the start value `(->P a b c)` and the start
     model `{:a a :b b :c c}`.
   - a key: one of `:a`, `:b`, `:c` (declared) and `:x`, `"a"` (undeclared).
   - a step: `[:assoc k v]` or `[:dissoc k]`, with a key `k` and a value `v`.
   - a step sequence: `(gen/vector step 0 100)`, 0 to 100 steps.
4. One property. Its arguments are a start and a step sequence. It keeps the current value `x` (first, the start
   value) and a model: a plain map of the declared entries (first, the start model). First it runs the I-01 checks on
   the start value. Then, for each step, it runs the step checks of that step, then the I-01 checks on the new `x`. The
   checks run in the order of the lists below. At the first check that fails, the property stops and returns a failure
   that has the name of the check (Scope 5). If all checks pass, it returns `true`.
   - The operation of a check is the call in its list item below: for example `(assoc x k v)` for `:assoc-type`,
     `(into {} result)` for `:assoc-entries`, `(.size x)` for `:size`, `(check x)` for `:check`. Each check catches
     any `Throwable` from its operation. A throw that the check does not expect is a failure of that check. Only
     `:assoc-undeclared-throws` and `:dissoc-declared-throws` expect a throw, and only an `ex-info` with the given
     `ex-data`. Another throw, or no throw, is a failure of that check.
   - A throw outside the operation of a check (for example in `->P`) is not caught. test.check records it as
     `:clojure.test.check.properties/error` in the `result-data`, and the trial fails.
   - Step checks, for the step on `x`:
     - `:assoc-type`: `assoc` of a declared key `k` with `v` gives an instance of `P`.
     - `:assoc-entries`: for the same step, `(into {} result)` is `=` to `(assoc model k v)`. Then `x` is the result
       and the model is `(assoc model k v)`.
     - `:assoc-undeclared-throws`: `assoc` of an undeclared key `k` throws, and the `ex-data` is `=` to
       `{:type :closed-record/undeclared-key :record P :key k}`.
     - `:dissoc-declared-throws`: `dissoc` of a declared key `k` throws, and the `ex-data` is `=` to
       `{:type :closed-record/declared-key-dissoc :record P :key k}`.
     - `:dissoc-undeclared-identical`: `dissoc` of an undeclared key gives a result that is `identical?` to `x`.
   - After a step that throws as expected (`:assoc-undeclared-throws`, `:dissoc-declared-throws`), `x` and the model
     do not change, and the next step uses `x`. This is part of the run, not a check.
   - I-01 checks, for the start value and for `x` after each step:
     - `:keys`: `(set (keys x))` is `=` to `D`.
     - `:key-set`: `(set (.keySet x))` is `=` to `D`.
     - `:entry-set`: `(set (map key (.entrySet x)))` is `=` to `D`.
     - `:contains`: `(set (filter #(contains? x %) [:a :b :c :x "a"]))` is `=` to `D`.
     - `:count`: `(count x)` is 3.
     - `:size`: `(.size x)` is 3.
     - `:values`: `(count (.values x))` is 3.
     - `:check`: `(check x)` is `identical?` to `x`.
5. The property runs inside one `deftest`, with `(clojure.test.check/quick-check 200 property)`: 200 trials, with a
   random seed. The property body has no `is`. A failure is a `clojure.test.check.results/Result` with `pass?` false
   and `result-data` `{:check <name of the check>}`. If the operation of the check threw, the `result-data` also has
   `:ex`, the `Throwable`. The `deftest` has one assertion, `(is (:pass? result) msg)`. `msg` shows `:seed`,
   `[:shrunk :smallest]` (the shrunk start and step sequence) and `[:shrunk :result-data]` (the name of the failed
   check and `:ex` if present, or the error of a throw outside a check).
6. If a test fails because of the emitter, fix the emitter in `src/closed_record/impl/jvm.clj`. Record the cause in
   `implementation.md`.
7. Update the documents: `ramora/defclosedrecord/embodiment.md` (Artifacts table and `deps.edn` row) and
   `mementum/state.md`.

## Out of scope

- Other operations: `conj`, `merge`, `update`, `into`, `with-meta`. V-03 is about `assoc` and `dissoc` sequences
  only. V-01 has example tests for these operations (`defclosedrecord_test.clj`). V-02 (not done) is the planned
  differential test.
- Primitive-hinted fields (V-09, task 004) and hidden keys (V-14, task 006).
- Differential tests against `defrecord`. `dissoc` of a declared key differs from `defrecord` by design (D-04).
- Generators for other types or for more than one field shape.
- Changes to existing tests.

## Decisions

- A model map, not a `defrecord`. The property checks the closed behaviour (I-01, I-04), which `defrecord` does not
  have.
- Use `org.clojure/test.check`. It is the standard Clojure property test library and it shrinks failures. The
  dependency is in the `:test` alias only, so the library has no new run-time dependency.
- One type with three untyped fields. The key set is the subject of V-03, not the field types.
- `check` is part of the I-01 check. A value made only by `->Name`, `assoc` and `dissoc` has no extra keys (I-07).
  `check` returns its argument (public interface), so the check uses `identical?`.
- Values are nil or small integers, not `gen/any`. `gen/any` can give `##NaN`, and `(= ##NaN ##NaN)` is false, so
  `:assoc-entries` can fail with no emitter fault. The field values do not change the key set. Small values keep the
  failure report short.
- The undeclared key `"a"` has the name of a declared field, but it is not a keyword.
- The I-01 checks cover all of the subject of I-01: `seq` (through `keys`), `count`, `contains?` and the
  `java.util.Map` views `size`, `keySet`, `entrySet` and `values`. V-14 checks the same views. Each view has its own
  method body in `jvm.clj`, but the bodies call other methods: `size` calls `count`, and `keySet`, `values` and
  `entrySet` read the entries through `seq`. A fault in the body of a view method shows only in its view. A fault in
  `seq` or `count` shows in more than one check, and first in `:keys` or `:count` (the check order in Scope 4). Thus,
  for acceptance 4, the change for a view check is in the body of its view method, not in `seq` or `count`.
- The error checks compare all of the `ex-data` of D-03, with `:record`, as the V-01 tests do.
- Each check catches any `Throwable` from its operation and fails with its own name. Thus a change that makes an
  operation throw is reported as a failure of the check that uses that operation (acceptance 4). `check` only returns
  its argument or throws, so a valid change for `:check` makes `check` throw. `Throwable`, not `Exception`, because
  a fault in an emitted method can give an `Error` (for example `AbstractMethodError`).
- The property body returns a value and has no `is`. An `is` in the body adds one assertion, and one failure, for
  each trial and each shrink step. The name of the failed check in `result-data` shows which check fails, also in a
  mutation run.
- 200 trials of 0 to 100 steps give 10 000 steps on average for each run. The seed is random, so each run tests new
  sequences. The report gives the seed, so a failure can be run again.

## Acceptance criteria

1. `deps.edn` has `org.clojure/test.check` 1.1.3 in the `:test` alias, and no other new dependency.
2. `test/closed_record/property_test.clj` has the property in Scope 4, run as in Scope 5.
3. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors. scry counts the property test as a passed test, not as "unknown".
4. Each of the 13 named checks in Scope 4 (5 step checks and 8 I-01 checks) fails when the behaviour that it checks
   is broken. Each check needs its own change in `src`: a change covers only the check that `[:shrunk :result-data]`
   names, because the property stops at the first failed check. `implementation.md` lists, for each check, the change in
   `src` and the failure report (for example: `without` returns `this` for a declared key). A change counts only if it
   follows the mutation rules in `mementum/knowledge/testing-generated-types.md` (section "Test design"). If no change
   in `src` breaks a check, `implementation.md` gives the reason.
5. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
6. `mementum/state.md` lists V-03 as done and names the new test namespace and dependency.
7. In `ramora/defclosedrecord/embodiment.md`, the Artifacts table has a row for `test/closed_record/property_test.clj`
   (V-03), and the `deps.edn` row names `test.check` in the `:test` alias.
