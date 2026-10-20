# 004 Field value tests

## Intent

Fix in tests that field values behave as with `defrecord`: keyword call sites (V-06), nil values (V-08) and type
hints (V-09). A closed record must be a replacement for a `defrecord` (O-02). These paths are emitter code: the lookup
thunks, the constructors, `assoc`, `create` and the primitive casts.

## Problem

Task 002 tested the key-set behaviour with untyped fields and non-nil values. Only these field-value cases have
tests:

- One keyword call site with `R`, `R2`, a `defrecord` and a plain map (`keyword-call-test`). This covers the V-06
  case "two closed types at the same site".
- A missing `^long` key in `map->Name` and in a map literal throws `NullPointerException` (D-19).

Nothing tests that primitive-hinted fields cast, box and fail as with `defrecord`, or that nil values give the
`defrecord` results. An emitter change (for example, an `assoc` that does not cast to the field type) can break
parity and no test fails.

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [qualities](../../../ramora/defclosedrecord/qualities.md#verification-plan): V-06, V-08, V-09.
  - [D-10](../../../ramora/defclosedrecord/decisions/type-structure.md#d-10-keyword-lookup) (lookup thunks),
    [D-19](../../../ramora/defclosedrecord/decisions/closed-key-set.md#d-19-missing-primitive-field) (missing
    primitive field), [D-03](../../../ramora/defclosedrecord/decisions/failure-and-validation.md#d-03-failure-signal)
    scope (casts keep the host exception).
- Existing tests: `test/closed_record/defclosedrecord_test.clj` (`keyword-call-test`, `map-factory-test`,
  `reader-literal-declared-key-test`).
- Test rules: `mementum/knowledge/testing-generated-types.md`.

### Observed facts (2026-10-19, Clojure 1.11.4, branch `master`, commit `462f852`)

Types `(defclosedrecord C [a ^long n ^double d ^int i ^boolean b ^String s])` and a `defrecord` `D` with the same
fields. Each case below gives the same result or the same exception class for `C` and `D`. In each input, only the
field under test has a bad or missing value. A row with two inputs gives two cases, one for each input. In
`map->Name` inputs, `:i` is an `Integer` (`(int ...)`):

| Path | Input | Result |
|---|---|---|
| `->Name` | `^long` ← `2.7`, `^double` ← `3` | cast: `2`, `3.0` |
| `->Name` | `^long` ← `"x"` | `ClassCastException` |
| `->Name` | `^long` ← nil | `NullPointerException` |
| `assoc` | `^long` ← `9.9` | cast: `9` |
| `assoc` | `^long` ← `"x"` | `ClassCastException` |
| `assoc` | `^long` ← nil, `^boolean` ← nil | `NullPointerException` |
| `assoc` | `^int` ← `5000000000` | `ArithmeticException` |
| `assoc` | `^String` ← `5` | accepted: `5` (an object hint does not cast) |
| `map->Name` | `^long` ← `2.5`, `^long` ← `"x"` | `ClassCastException` (`create` casts to the box class) |
| `map->Name` | `^boolean` missing | `NullPointerException` (D-19) |
| `map->Name` | `^int` ← `1` (a `Long`) | `ClassCastException` (`create` casts to the box class `Integer`) |
| `vals` | all fields | `Long Long Double Integer Boolean String` |
| keyword call | `(:n x)` in a function | `2`, class `Long` |

Types `(defclosedrecord N [a b])` and a `defrecord` with the same fields, all values nil: `get` with a not-found
value gives nil, `contains?` is true, `count` is 2, two equal values are `=` and have the same `hash`, `.hashCode`
and `.equals` agree with `{:a nil :b nil}`, and the record prints and reads back `=`.
`(= (->N nil nil) (->N nil false))` is false. Each of these results is the same for `N` and the `defrecord`. The
`hash` of an `N` value and the `hash` of the equal `defrecord` value are different: `hash` includes the type hash
(D-07).

## Scope

In scope:

1. A test namespace `closed-record.field-value-test` in `test/closed_record/field_value_test.clj`.
2. A differential helper in that namespace. It calls one function with the closed type factories and with the
   `defrecord` factories. It compares the two outcomes:
   - Both return: a record result is compared as `(into {} x)` and the class of each value. Each other result is
     compared with `=` and by its class. Thus a value of the wrong box class (`Long` for `Integer`) fails.
   - Both throw: the exception classes are compared.
   - Only one throws: the test fails.
3. V-09 tests: each row of the type hint table in Context, as a differential test.
4. V-08 tests: nil values for lookup (`get`, keyword call, `.field`), `contains?`, `=`, `hash`, `.hashCode`,
   `.equals`, print and read. A nil value is different from `false`. Each test uses the helper. The function returns
   only results that do not depend on the class: the lookup results, `contains?`, and booleans for the other
   operations. Two equal values are `=` and have the same `hash`. `.hashCode` and `.equals` agree with
   `{:a nil :b nil}`. The value that print gives reads back `=`. A value with nil is not `=` to a value with `false`.
   The helper does not compare the `hash` of a closed value with the `hash` of a `defrecord` value (D-07).
5. V-06 test: one keyword call site with a primitive-hinted field gives the boxed value. A site that sees a closed
   type, then a second closed type, then the first again gives the correct values. The test uses fixed expected
   values: the field value and its class (for example `2` and `Long`).
6. If a test fails because of the emitter, fix the emitter in `src/closed_record/impl/jvm.clj`. Record the cause in
   `implementation.md`.

## Out of scope

- Tests that 002 and 003 already have (missing `^long` key, the existing call site test).
- `^float`, `^short`, `^byte` and `^char` hints. `^long`, `^double`, `^int` and `^boolean` cover the cast and unbox
  paths.
- Exception messages. Only the exception class is compared.
- A general differential test of all operations (V-02) and property tests (V-03).
- Other verification items: V-02, V-03, V-07, V-11, V-12, V-14.

## Decisions

- Differential tests, not fixed expected values, for V-09 and V-08. The design commitment is parity with
  `defrecord`. A change in Clojure casts then changes both results and no test fails for a parity that still holds.
- Fixed expected values for V-06. The call site sees two closed types. No `defrecord` result is the counterpart.
- The helper is local to this namespace. A shared test utility namespace is not necessary for one user.
- Exceptions are compared by class only. Messages contain the class names, which differ.

## Acceptance criteria

1. `test/closed_record/field_value_test.clj` has a test for each item in Scope 3, Scope 4 and Scope 5.
2. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors.
3. Each row of the type hint table, each operation in Scope 4 and each case in Scope 5 has an assertion that fails
   when the behaviour that it checks is broken. This applies also when one `deftest` holds more than one row.
   `implementation.md` lists, for each row, operation and case, the change in `src` and the assertion that failed
   (for example: `assoc` does not cast to the field type). One change can cover more than one row if each of these
   rows fails with it. A change counts only if it follows the mutation rules in
   `mementum/knowledge/testing-generated-types.md` (section "Test design"). If no change in `src` breaks a row,
   `implementation.md` gives the reason.
4. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
5. `mementum/state.md` lists V-06, V-08 and V-09 as done.
