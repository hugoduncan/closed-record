# Steps

## Slice 1: scaffold

- [x] Create `test/closed_record/field_value_test.clj` with ns `closed-record.field-value-test`, requiring `clojure.test` and `closed-record.core`.
- [x] Define `(defclosedrecord C [a ^long n ^double d ^int i ^boolean b ^String s])` and `(defrecord DC [a ^long n ^double d ^int i ^boolean b ^String s])`.
- [x] Define `(defclosedrecord N [a b])`, `(defrecord DN [a b])` and `(defclosedrecord N2 [a b])` (V-06 A → B → A).
- [x] Add a `thrown` capture helper: `(try (f) nil (catch Throwable t t))` returning the throwable.
- [x] Add the differential helper: takes `f` of a factory map, runs it with `C` factories and `DC` factories (and an `N`/`DN` variant), compares per design Scope 2 (record → `(into {} x)` plus per-value class; other → `=` plus class; both throw → exception class; mixed → fail).
- [x] Add one smoke differential test (e.g. `->Name` with all valid values; `vals` classes).
- [x] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 2: V-09 type-hint differential tests

- [x] Test `->Name` casts: `^long` ← `2.7` gives `2`; `^double` ← `3` gives `3.0`.
- [x] Test `->Name` `^long` ← `"x"` throws `ClassCastException` (same class both sides).
- [x] Test `->Name` `^long` ← nil throws `NullPointerException`.
- [x] Test `assoc` `^long` ← `9.9` casts to `9`.
- [x] Test `assoc` `^long` ← `"x"` throws `ClassCastException`.
- [x] Test `assoc` `^long` ← nil and `^boolean` ← nil each throw `NullPointerException`.
- [x] Test `assoc` `^int` ← `5000000000` throws `ArithmeticException`.
- [x] Test `assoc` `^String` ← `5` is accepted and the value is `5` (object hint does not cast).
- [x] Test `map->Name` `^long` ← `2.5` and `^long` ← `"x"` each throw `ClassCastException` (`:i` given as `(int ...)` in inputs).
- [x] Test `map->Name` with `^boolean` missing throws `NullPointerException` (D-19).
- [x] Test `map->Name` `^int` ← `1` (a `Long`) throws `ClassCastException` (box-class cast in `create`).
- [x] Test `vals` of `(->Name 1 2 3 4 true "s")`, returned as `(mapv class (vals x))`, gives classes `Long Long Double Integer Boolean String` (the Long inputs for `^double` and `^int` are necessary for M1).
- [x] Test keyword call `(:n x)` inside a function, `x` built with `->Name` and `^long` ← `2.7`, gives `2` and class `Long` (differential; the `2.7` input is necessary for M1).
- [x] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 3: V-08 nil-value differential tests

- [x] Test lookup of nil fields via `get` with not-found, keyword call, and `.a`/`.b` direct field access, all returning nil (differential, class-independent results only). Keyword call: one function `(fn [x] (:a x))`, called two times.
- [x] Test `contains?` is true for nil-valued keys and `count` is 2.
- [x] Test two equal nil-valued values are `=` and have the same `hash` (closed vs closed; defrecord vs defrecord; never cross-type hash, D-07).
- [x] Test `.hashCode` and `.equals` agree with `{:a nil :b nil}` (booleans compared differentially). `.equals` in both directions: `(.equals x {:a nil :b nil})` and `(.equals {:a nil :b nil} x)`.
- [x] Test print/read round-trip: the printed value reads back `=` to the original. Use `pr-str`, then `read-string` inside `(binding [*ns* (the-ns test-ns)] ...)` (the `in-test-ns`/`read-literal` pattern in `test/closed_record/literal_test.clj`; `*ns*` is `user` in the CLI). The function returns `(= x read-back)`.
- [x] Test `(= (->N nil nil) (->N nil false))` is false (differential boolean).
- [x] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 4: V-06 call-site tests (fixed expected values)

- [x] Test one keyword call site on a primitive-hinted field returns the boxed value: `(:n x)` in a function, `x` built with `->C` and `^long` ← `(int 2)`, gives `2` and class `Long`.
- [x] Test a single call site `(fn [x] (:a x))` that sees an `N`, then an `N2`, then the `N` again, returning the correct fixed value each time.
- [x] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 5: verification, mutation checks, bookkeeping

- [x] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`; all exit 0 with zero failures and errors.
- [x] Run `clj-kondo --lint src test`; fix until no errors or warnings.
- [x] Run `cljfmt check src test`; fix until it passes.
- [x] Mutation checks: apply each mutation M1 to M15 in plan.md "Mutation map", one at a time, to `src/closed_record/impl/jvm.clj`. Run `clojure -M:test -m scry.cli`, confirm that the tests listed for it fail, revert. Record the change and the failed assertion for each.
  - [x] M1 (no hints in `deftype*`): all V-09 rows except `assoc` `^String` ← `5`; V-06 boxed value.
  - [x] M2 (`->Name` through `create`): `->Name` `^long` ← `2.7` and `^double` ← `3`.
  - [x] M3 (`assoc` through `create`): `assoc` `^long` ← `9.9`, `^int` ← `5000000000`.
  - [x] M4 (`assoc` through `Reflector/invokeConstructor`): `assoc` `^long` ← `"x"`, `^int` ← `5000000000`.
  - [x] M5 (`map->Name` through `new`): `map->Name` `^long` ← `2.5`, `^int` ← `1`.
  - [x] M6 (thunk reads the first field): V-09 keyword call; V-06 boxed value.
  - [x] M7 (thunk without class check): V-06 A → B → A.
  - [x] M8 (thunk: nil is a miss): V-08 keyword call.
  - [x] M9 (`valAt`: nil gives not-found): V-08 `get` with not-found.
  - [x] M10 (`containsKey` through `valAt`): V-08 `contains?`.
  - [x] M11 (`count` of non-nil fields): V-08 `count`.
  - [x] M12 (`equals` without nil entries): V-08 `.equals`.
  - [x] M13 (`seq` without nil entries): V-08 `.hashCode`.
  - [x] M14 (`equiv`: nil ≠ nil): V-08 `=` of equal values; V-08 print/read (no change on the print or read path itself fails it).
  - [x] M15 (`equiv` skips nil fields): V-08 nil ≠ `false`.
- [x] Record in `implementation.md` the tests that no mutation breaks, with the reason from plan.md "Mutation map": `assoc` `^String` ← `5`, V-08 `.field`, V-08 equal `hash`.
- [x] If any mutation check or test exposes a real emitter defect, fix it in `src/closed_record/impl/jvm.clj` and record the cause in `implementation.md`.
- [x] Complete `implementation.md`: for each table row, Scope 4 operation and Scope 5 case, list the `src` change and the assertion that failed (or the reason none exists).
- [x] Update `mementum/state.md`: mark V-06, V-08 and V-09 as done.
- [x] Final full run of all three test aliases, clj-kondo and cljfmt; confirm all pass.

## Review follow-up

- [x] Isolate the V-06 keyword call sites. `call-site-type-change-test` and `call-site-boxed-value-test` use `get-a`
  and `get-n`, which `nil-lookup-test` and `keyword-call-hint-test` also call (with `N`, `DN`, `C`, `DC`). Thus the
  site state at the start of each V-06 test depends on the test order, and "A, then B, then A" is not the first
  sequence that the site sees (design Scope 5). Give each V-06 test its own site (a local `(fn [x] (:a x))` in the
  `deftest`). Run M6 and M7 again and update `implementation.md` if the failed tests change.
- [x] Add `test/closed_record/field_value_test.clj` (V-06, V-08, V-09: field values) to the "Tests:" list in
  `mementum/state.md`.

## Implementation review 2

- [x] Make a differential failure show both outcomes in the test report. `same-outcome?` prints the two outcomes
  with `println` and returns `false`, so `(is (c-diff ...))` and `(is (n-diff ...))` report only `actual: false`, and
  the outcomes are not in `.scry-results/`. `mementum/knowledge/testing-generated-types.md` ("Test design") gives the
  existing rule: assert with `=`, so a failure shows the values. Change the helper to give the two outcomes (for
  example `[closed-outcome record-outcome]`) and assert `(is (= a b))` in a small wrapper or macro, then remove the
  `println`. Run the three aliases, clj-kondo and cljfmt.

## Test review

- [x] Remove `smoke-test` from `test/closed_record/field_value_test.clj`. Its assertion
  `(is (= [:ok [5 Long]] (outcome (constantly 5))))` tests the test helper `outcome`, not `src` (no tests that cover
  tests). Its other assertion `(is-c-diff #(c-new %))` (all values valid) checks no row of the type hint table and no
  Scope 4 or Scope 5 item; the V-09 rows already call `c-new` and the helper. Run the three aliases, clj-kondo and
  cljfmt.

## Test review 2

- [x] Remove the second assertion of `nil-print-read-test` in `test/closed_record/field_value_test.clj`:
  `(is (true? (let [x (->N nil nil)] (= x (in-test-ns ...)))))`. It repeats the closed half of the `is-n-diff`
  assertion above it. The `defrecord` side gives `true`, so the differential assertion already fails when the closed
  value does not read back `=` (M14 fails both). Design Scope 4 states that each V-08 test uses the helper, and no
  other V-08 test has a fixed assertion. Run the three aliases, clj-kondo and cljfmt.

## Code-shaper review

- [x] Remove the duplicate default input in `test/closed_record/field_value_test.clj`. `c-new` (:58) and `c-map`
  (:64) each hold the literal `{:a 1 :n 2 :d 3.0 :i (int 4) :b true :s "s"}`. A change to one default (for example
  `:i`, which must stay an `Integer`, design Context) can miss the other. Keep one source: for example, `c-new`
  calls `(apply (:new fs) (map (apply c-map (mapcat identity kvs)) [:a :n :d :i :b :s]))`, or both use one private
  `c-defaults`. Run the three aliases, clj-kondo and cljfmt.
- [x] Remove the side test from the V-08 `.field` case (`nil-lookup-test`, :112). The function branches on
  `(= % n-factories)` (equality of two maps of functions) to pick the `^N` or `^DN` hint. Thus the test body knows
  which side of the differential it runs on, and it mixes flow control with the computation under test. Put the
  hinted reader in the factory maps (for example `:fields (fn [^N x] [(.-a x) (.-b x)])` in `n-factories` and the
  `^DN` form in `dn-factories`) and call `((:fields %) (nil-value %))`, as the other cases call `(:new %)`. Check with
  clj-kondo that no reflection warning is added. Run the three aliases, clj-kondo and cljfmt.

## Code-shaper review 2

- [x] Remove the unused `:map` entries from `n-factories` and `dn-factories` in
  `test/closed_record/field_value_test.clj` (:24, :26). No V-08 case calls `(:map %)`; only `:new` (`nil-value`) and
  `:fields` are used. The unused entry suggests a `map->Name` path that the V-08 tests do not check. Run the three
  aliases, clj-kondo and cljfmt.
- [x] Remove the two remaining copies of the default `C` input. `vals-hint-test` (:95) gives
  `((:new %) 1 2 3 4 true "s")` and `call-site-boxed-value-test` (:147) gives `(->C 1 (int 2) 3.0 (int 4) true "s")`.
  Each repeats the `c-map` defaults positionally, so the first code-shaper step left three sources. Use `c-new`:
  `(c-new % :d 3 :i 4)` for `vals` (the `Long` inputs that M1 needs stay) and `(c-new c-factories :n (int 2))` for
  the V-06 boxed value. Run M1 again for `vals-hint-test` and `call-site-boxed-value-test`, then the three aliases,
  clj-kondo and cljfmt.

## Code-shaper review 3

- [x] Use one idiom for the keyword call sites in `test/closed_record/field_value_test.clj`. The V-06 tests use a
  site local to the `deftest` (`(fn [x] (:n x))`, :146, :154). The V-09 and V-08 keyword tests use top-level
  `get-n` (:97) and `get-a` (:103), each with one user, defined away from the assertion. Put each site in its
  `deftest` as a local `(fn [x] ...)` and remove `get-n` and `get-a`. Then each test holds its own site, and no later
  test can share it. Run M6 and M8 again, then the three aliases, clj-kondo and cljfmt.
