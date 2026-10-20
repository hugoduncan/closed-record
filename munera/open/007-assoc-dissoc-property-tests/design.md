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
- Emitter: `src/closed_record/impl/jvm.clj` (methods `assoc`, `without`, `seq`, `count`, `containsKey`, `keySet`).
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
2. A test namespace `closed-record.property-test` in `test/closed_record/property_test.clj`. It uses one type with
   three untyped fields.
3. Generators:
   - a start value from `->Name` with random field values, nil included;
   - a step: `assoc` or `dissoc`, with a key from the declared keys and two undeclared keys, and for `assoc` a random
     value, nil included.
4. One property. It runs a random step sequence from a random start value. It keeps a model: a plain map of the
   declared entries. For each step:
   - `assoc` of a declared key: the result is a `T`, and `(into {} result)` is `=` to the model after the same
     `assoc`.
   - `assoc` of an undeclared key: throws `ex-info` with `:type` `:closed-record/undeclared-key` and the key.
   - `dissoc` of a declared key: throws `ex-info` with `:type` `:closed-record/declared-key-dissoc` and the key.
   - `dissoc` of an undeclared key: the result is `identical?` to the value before the step.
   - After a step that throws, the sequence continues with the value before the step.
   - After each step, I-01 holds for the current value: `(set (keys x))`, `(.keySet x)` and the keys for which
     `contains?` is true are each `=` to the declared keys, `count` is 3, and `check` returns the value.
5. The property runs inside a `deftest` with a fixed number of trials. A failure report shows the shrunk step
   sequence and the seed.
6. If a test fails because of the emitter, fix the emitter in `src/closed_record/impl/jvm.clj`. Record the cause in
   `implementation.md`.

## Out of scope

- Other operations: `conj`, `merge`, `update`, `into`, `with-meta`. V-02 covers them.
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

## Acceptance criteria

1. `deps.edn` has `org.clojure/test.check` 1.1.3 in the `:test` alias, and no other new dependency.
2. `test/closed_record/property_test.clj` has the property in Scope 4, run as in Scope 5.
3. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors. scry counts the property test as a passed test, not as "unknown".
4. Each check in Scope 4 fails when the behaviour that it checks is broken. `implementation.md` lists, for each
   check, the change in `src` and the failure report (for example: `without` returns `this` for a declared key). One
   change can cover more than one check if each of these checks fails with it. A change counts only if it follows the
   mutation rules in `mementum/knowledge/testing-generated-types.md` (section "Test design"). If no change in `src`
   breaks a check, `implementation.md` gives the reason.
5. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
6. `mementum/state.md` lists V-03 as done and names the new test namespace and dependency.
