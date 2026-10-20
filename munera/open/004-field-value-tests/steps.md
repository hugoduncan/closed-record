# Steps

## Slice 1: scaffold

- [ ] Create `test/closed_record/field_value_test.clj` with ns `closed-record.field-value-test`, requiring `clojure.test` and `closed-record.core`.
- [ ] Define `(defclosedrecord C [a ^long n ^double d ^int i ^boolean b ^String s])` and `(defrecord DC [a ^long n ^double d ^int i ^boolean b ^String s])`.
- [ ] Define `(defclosedrecord N [a b])` and `(defrecord DN [a b])`.
- [ ] Add a `thrown` capture helper: `(try (f) nil (catch Throwable t t))` returning the throwable.
- [ ] Add the differential helper: takes `f` of a factory map, runs it with `C` factories and `DC` factories (and an `N`/`DN` variant), compares per design Scope 2 (record → `(into {} x)` plus per-value class; other → `=` plus class; both throw → exception class; mixed → fail).
- [ ] Add one smoke differential test (e.g. `->Name` with all valid values; `vals` classes).
- [ ] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 2: V-09 type-hint differential tests

- [ ] Test `->Name` casts: `^long` ← `2.7` gives `2`; `^double` ← `3` gives `3.0`.
- [ ] Test `->Name` `^long` ← `"x"` throws `ClassCastException` (same class both sides).
- [ ] Test `->Name` `^long` ← nil throws `NullPointerException`.
- [ ] Test `assoc` `^long` ← `9.9` casts to `9`.
- [ ] Test `assoc` `^long` ← `"x"` throws `ClassCastException`.
- [ ] Test `assoc` `^long` ← nil and `^boolean` ← nil each throw `NullPointerException`.
- [ ] Test `assoc` `^int` ← `5000000000` throws `ArithmeticException`.
- [ ] Test `assoc` `^String` ← `5` is accepted and the value is `5` (object hint does not cast).
- [ ] Test `map->Name` `^long` ← `2.5` and `^long` ← `"x"` each throw `ClassCastException` (`:i` given as `(int ...)` in inputs).
- [ ] Test `map->Name` with `^boolean` missing throws `NullPointerException` (D-19).
- [ ] Test `map->Name` `^int` ← `1` (a `Long`) throws `ClassCastException` (box-class cast in `create`).
- [ ] Test `vals` on a full value gives classes `Long Long Double Integer Boolean String`.
- [ ] Test keyword call `(:n x)` inside a function gives `2` and class `Long` (differential).
- [ ] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 3: V-08 nil-value differential tests

- [ ] Test lookup of nil fields via `get` with not-found, keyword call, and `.a`/`.b` direct field access, all returning nil (differential, class-independent results only).
- [ ] Test `contains?` is true for nil-valued keys and `count` is 2.
- [ ] Test two equal nil-valued values are `=` and have the same `hash` (closed vs closed; defrecord vs defrecord; never cross-type hash, D-07).
- [ ] Test `.hashCode` and `.equals` agree with `{:a nil :b nil}` (booleans compared differentially).
- [ ] Test print/read round-trip: the printed value reads back `=` to the original.
- [ ] Test `(= (->N nil nil) (->N nil false))` is false (differential boolean).
- [ ] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 4: V-06 call-site tests (fixed expected values)

- [ ] Test one keyword call site on a primitive-hinted field returns the boxed value: `2` and class `Long`.
- [ ] Test a single call site that sees closed type A, then closed type B, then A again, returning the correct fixed value each time.
- [ ] Run `clojure -M:test -m scry.cli`; confirm exit 0.

## Slice 5: verification, mutation checks, bookkeeping

- [ ] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`; all exit 0 with zero failures and errors.
- [ ] Run `clj-kondo --lint src test`; fix until no errors or warnings.
- [ ] Run `cljfmt check src test`; fix until it passes.
- [ ] Mutation check — constructor casts: break the constructor cast path in `src/closed_record/impl/jvm.clj`, confirm the `->Name` cast/exception assertions fail, revert, record in `implementation.md`.
- [ ] Mutation check — `assoc` cast: break the `assoc` field cast, confirm the `assoc` rows fail, revert, record.
- [ ] Mutation check — `create` box-class cast: break the box-class cast in `create`, confirm the `map->Name` rows fail, revert, record.
- [ ] Mutation check — D-19 missing-primitive path: break the missing-field NPE path, confirm the missing `^boolean` assertion fails, revert, record.
- [ ] Mutation check — lookup thunks (D-10): break the keyword-lookup thunk, confirm the V-06 and keyword-call assertions fail, revert, record.
- [ ] Mutation check — nil-value paths: break equality/hash or lookup handling of nil, confirm the V-08 assertions fail, revert, record; for any row no `src` change can break, record the reason.
- [ ] If any mutation check or test exposes a real emitter defect, fix it in `src/closed_record/impl/jvm.clj` and record the cause in `implementation.md`.
- [ ] Complete `implementation.md`: for each table row, Scope 4 operation and Scope 5 case, list the `src` change and the assertion that failed (or the reason none exists).
- [ ] Update `mementum/state.md`: mark V-06, V-08 and V-09 as done.
- [ ] Final full run of all three test aliases, clj-kondo and cljfmt; confirm all pass.
