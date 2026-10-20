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
