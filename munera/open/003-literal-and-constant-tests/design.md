# 003 Literal and constant tests

## Intent

Fix in tests that closed records print, read and embed as code constants as the design says (V-04, V-05). These
paths do not go through closed-record code. They go through the Clojure printer, the reader (`create`, the
positional constructor) and `Compiler.emitValue`. A change in Clojure or in the emitter must then cause a failure in
a known test.

## Problem

Task 002 tested the key-set behaviour. For literals it tested only part of the behaviour table: a missing key gives
nil, extra keys are hidden, `check` finds them, and `assoc` and `withMeta` keep them. Nothing tests that:

- a printed record reads back as an equal record (I-06),
- a positional literal reads,
- printing a record from a literal with extra keys drops the extra keys (I-07),
- a record value in code compiles and gives an equal record with no extra keys (V-05, I-07, I-09).

Code constants depend on two compiler facts that only V-13 checks indirectly: `emitValue` uses the `IRecord` path
(K-08), and it builds the constant from the `java.util.Map` view (E-03, I-09). No test uses a closed record as a
constant.

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [invariants](../../../ramora/defclosedrecord/invariants.md): I-06, I-07, I-08, I-09.
  - [qualities](../../../ramora/defclosedrecord/qualities.md#verification-plan): V-04, V-05.
  - [D-09](../../../ramora/defclosedrecord/decisions/closed-key-set.md#d-09-reader-literals-and-printing),
    [context](../../../ramora/defclosedrecord/context.md): K-07, K-08, E-03, E-04.
- Existing tests: `test/closed_record/defclosedrecord_test.clj`: `reader-literal-declared-key-test`,
  `reader-literal-undeclared-key-test`, `extra-key-retention-test`, `check-test`.
- Test rules: `mementum/knowledge/testing-generated-types.md`. In the scry CLI, `*ns*` is `user`. Read literals with
  `*ns*` bound to the test namespace.

### Observed facts (2026-10-19, Clojure 1.11.4, current `main`)

Type `(defclosedrecord P [a ^long n])`, `r` = `(->P 1 2)`:

- `(pr-str r)` gives `#probe.P{:a 1, :n 2}`. `read-string` of it gives a value `=` to `r`.
- `read-string` of `#probe.P[1 2]` gives a value `=` to `r`.
- With `*print-dup*` true and `*verbose-defrecords*` false (the default), `pr-str` gives `#probe.P[1, 2]`.
- A literal `#probe.P{:a 1 :n 2 :x 3}` prints as `#probe.P{:a 1, :n 2}`. `check` of the re-read value returns it.
- A macro that returns a record value, used in a function body: the result is `=` to `r` and has class `P`. Metadata
  is kept. For a literal with extra keys, the result is `=` to `r`, and `check` returns it.
- ❗ The same macro in a top-level `def` that is evaluated (not compiled into a class): the value is the same object.
  `check` throws `extra-keys`. Only a constant that the compiler emits into a class goes through `emitValue`.
  I-07 says "code constants drop `__extmap`", but it does not name this condition.

## Scope

In scope:

1. A test namespace `closed-record.literal-test` in `test/closed_record/literal_test.clj`.
2. V-04 tests:
   - Round trip (I-06): `(read-string (pr-str r))` is `=` to `r` and has the same class. Use a type with an unhinted
     field and a `^long` field. Include a field value that is a closed record.
   - `print-dup` round trip: with `*print-dup*` true, the output is the positional form, and it reads back `=` to `r`.
   - Positional literal: `#ns.T[...]` reads as `=` to the `->T` result.
   - Print of a literal with extra keys: the output has the declared keys only. The re-read value is `=` to the
     literal, and `check` returns it (I-07).
3. V-05 tests, with the constant compiled in a function body (a `deftest` body is one):
   - A macro that returns a record value: the result is `=` to the value, has the same class and keeps metadata.
   - A macro that returns a record from a literal with extra keys: the result is `=` to the literal, and `check`
     returns it (I-07, I-09).
4. Make I-07 exact in `ramora/defclosedrecord/invariants.md`: "code constants" means constants that the compiler
   emits into a class. An evaluated top-level form keeps the object.
5. If a test fails because of the emitter, fix the emitter in `src/closed_record/impl/jvm.clj`. Record the cause in
   `implementation.md`.

## Out of scope

- Tests that 002 already has (missing keys, extra keys hidden, `check`, retention after `assoc` and `withMeta`).
- `*read-eval*` false, `clojure.edn` and data readers. Clojure owns that behaviour.
- AOT compilation tests.
- Other verification items: V-02, V-03, V-06 to V-09, V-11, V-12, V-14.
- The `.field` row for an undeclared name in the purpose behaviour table.

## Decisions

- One new test file. Do not add these tests to `defclosedrecord_test.clj`. That file covers V-01, V-10 and V-15.
- Constants are tested only in a compiled function body. The top-level evaluated path keeps the object: it is a
  Clojure evaluation detail, not a design commitment, so no test fixes it.
- The constant macro calls `eval` at expansion time on a form that builds the record. It is defined in the test
  namespace.

## Acceptance criteria

1. `test/closed_record/literal_test.clj` has a test for each item in Scope 2 and Scope 3.
2. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors.
3. Each new test fails when the behaviour that it checks is broken. `implementation.md` lists the change used for
   each test (for example: `seq` returns `__extmap` entries, or the type implements `IType`).
4. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
5. I-07 in `ramora/defclosedrecord/invariants.md` names the condition of Scope 4.
6. `mementum/state.md` lists V-04 and V-05 as done.
