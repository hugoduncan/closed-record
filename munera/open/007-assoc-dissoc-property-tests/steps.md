# Steps: 007 assoc and dissoc property tests

## Slice 1: dependency

- [x] Add `org.clojure/test.check {:mvn/version "1.1.3"}` to the `:extra-deps` of the `:test` alias in `deps.edn`.
- [x] Verify the dependency resolves: `clojure -M:test -e "(require 'clojure.test.check)"` succeeds.

## Slice 2: property test namespace

- [x] Create `test/closed_record/property_test.clj` with ns `closed-record.property-test`, requiring
      `clojure.test`, `clojure.test.check`, `clojure.test.check.generators`,
      `clojure.test.check.properties`, `clojure.test.check.results`, `closed-record.core`.
- [x] Define the type `(defclosedrecord P [a b c])` and `D` as `#{:a :b :c}`.
- [x] Write the value generator: `(gen/one-of [(gen/return nil) gen/small-integer])`.
- [x] Write the start generator: a vector of three values.
- [x] Write the key generator: one of `:a :b :c :x "a"`.
- [x] Write the step generator: `[:assoc k v]` or `[:dissoc k]`.
- [x] Write the step-sequence generator: `(gen/vector step 0 100)`.
- [x] Write a check helper: runs an operation, catches `Throwable`, returns pass or a failure Result with
      `result-data {:check <name>}` and `:ex` when the operation threw.
- [x] Implement the 8 I-01 checks in order: `:keys`, `:key-set`, `:entry-set`, `:contains`, `:count`,
      `:size`, `:values`, `:check` (per the operations in design Scope 4).
- [x] Implement the 5 step checks: `:assoc-type`, `:assoc-entries`, `:assoc-undeclared-throws`
      (full `ex-data` `{:type :closed-record/undeclared-key :record P :key k}`),
      `:dissoc-declared-throws` (full `ex-data` `{:type :closed-record/declared-key-dissoc :record P :key k}`),
      `:dissoc-undeclared-identical`.
- [x] Implement the property: build start value `(->P a b c)` and model `{:a a :b b :c c}`; run I-01 checks
      on the start; for each step run its step check then the I-01 checks on the new `x`; after an expected
      throw, `x` and the model do not change; stop at the first failure; return `true` if all pass. No `is`
      in the body.
- [x] Write the `deftest`: `(quick-check 200 property)`; one `(is (:pass? result) msg)` with `msg` showing
      `:seed`, `[:shrunk :smallest]` and `[:shrunk :result-data]`.
- [x] Run `clojure -M:test -m scry.cli`: exit 0, zero failures/errors, property test counted as passed.
- [x] Run `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`: exit 0, zero
      failures/errors.
- [x] If a run on unchanged `src` fails because of the emitter, fix `src/closed_record/impl/jvm.clj`, record
      the cause in `implementation.md`, and run the three aliases again.
      No emitter fault: all three aliases passed on unchanged `src` (76 tests, 0 failed).

## Slice 3: mutation verification (acceptance 4)

For each check: apply one change in `src`, run the property test, confirm `[:shrunk :result-data]` names
exactly that check, revert the change, and record the change and failure report in `implementation.md`.
Follow the mutation rules in `mementum/knowledge/testing-generated-types.md` (section "Test design").

- [x] Mutate for `:assoc-type` (e.g. `assoc` returns a plain map instead of a `P`).
- [x] Mutate for `:assoc-entries` (e.g. `assoc` drops or alters the value of the key).
- [x] Mutate for `:assoc-undeclared-throws` (e.g. `assoc` of an undeclared key does not throw, or wrong `ex-data`).
- [x] Mutate for `:dissoc-declared-throws` (e.g. `without` returns `this` for a declared key).
- [x] Mutate for `:dissoc-undeclared-identical` (e.g. `without` returns a copy for an undeclared key).
- [x] Mutate for `:keys` (change in `seq`).
- [x] Mutate for `:key-set` (change in the `keySet` method body only).
- [x] Mutate for `:entry-set` (change in the `entrySet` method body only).
- [x] Mutate for `:contains` (change in `containsKey`).
- [x] Mutate for `:count` (change in `count`).
- [x] Mutate for `:size` (change in the `size` method body only).
- [x] Mutate for `:values` (change in the `values` method body only).
- [x] Mutate for `:check`: in the `assoc` clause of `jvm.clj`, the declared-key branch passes
      `(assoc __extmap k v)` in place of `__extmap` to `new`. `check` then throws
      (`:closed-record/extra-keys`) only for a result of `assoc` of a declared key, not for all values.
- [x] For any check with no valid breaking change in `src`, record the reason in `implementation.md`.
      None: each of the 13 checks has a valid change.
- [x] Verify all mutations are reverted: `git diff src` is empty; rerun `clojure -M:test -m scry.cli`, exit 0.

## Slice 4: lint, format, documents

- [x] Run `clj-kondo --lint src test`: no errors, no warnings.
- [x] Run `cljfmt check src test`: passes.
- [x] Update `ramora/defclosedrecord/embodiment.md`: Artifacts row for
      `test/closed_record/property_test.clj` (V-03); `deps.edn` row names `test.check` in the `:test` alias.
- [x] Update `mementum/state.md`: V-03 done; name `closed-record.property-test` and the `test.check`
      dependency.
- [x] Final run of all three test aliases: exit 0, zero failures and errors.

## Code-shaper review follow-up

- [x] Remove the hidden state between `:assoc-type` and `:assoc-entries` in `run-step`
      (`test/closed_record/property_test.clj` 80–86). The `:assoc-type` operation writes the `assoc` result to a
      `volatile!`, and `:assoc-entries` reads it. Thus one check operation has a side effect, and the second check
      is correct only if the first check ran first and passed. Split the call from the judgement: for example an
      `attempt` helper that runs `op` once and gives the value or the `Throwable`, and checks that take that
      outcome. Keep the check names, the check order and the `:ex` in `result-data`.
      Done: `attempt` gives `{:value v}` or `{:ex e}`; `judge` checks an outcome; both `assoc` checks judge one outcome.
- [x] Make the result shape of `run-step` and `run-steps` explicit. Checks give nil or a failure `Result`, but
      `run-step` and `run-steps` give `[x model]` or a failure, and three places tell them apart with `vector?`
      (lines 106, 107 via `r`, 116). Use one predicate (for example `failure?` on `results/Result`) in all three
      places, or make `run-step` give only failures through `reduced`, so that the shape rule is in one place.
      Run the three aliases and one mutation (for example `:check`) again after the change.
      Done: `failure?` (`instance?` of the `Result` interface) in all three places. Three aliases pass; mutations
      `:check`, `:assoc-entries`, `:assoc-type` each name their own check.

## Code-shaper review follow-up, pass 2

- [x] Use one idiom to run an operation and classify the outcome. `run-check` uses `attempt` and `judge`, but
      `throws-check` (`test/closed_record/property_test.clj` 67–75) has its own `try` with two `catch` clauses.
      Make `throws-check` use `attempt`: no `:ex` → `(failure id nil)`; `(= data (ex-data ex))` → pass; else
      `(failure id ex)`. `ex-data` of a throw that is not `ex-info` is nil, and `data` is not nil, so the
      `ExceptionInfo` catch is not necessary. Keep the check names, the `:ex` in `result-data` and the behaviour.
      Run the three aliases and the `:assoc-undeclared-throws` and `:dissoc-declared-throws` mutations again.
      Done: `throws-check` uses `attempt` and a `cond`. Three aliases pass; both mutations name their own check.

## Code-shaper review follow-up, pass 3

- [x] Make the docstring of `judge` agree with its arguments (`test/closed_record/property_test.clj` 54–56). It
      says "A throw from `op` or `ok?` fails `id`", but `judge` has no `op`: it takes `id`, an outcome and `ok?`.
      State the rule in terms of the outcome, for example: "Pass if `outcome` has no `:ex` and `(ok? value)` is
      true. An `:ex` in `outcome`, or a throw from `ok?`, fails `id`." Change only the docstring.
      Done: docstring states the rule in terms of `outcome`. Only the docstring changed.
