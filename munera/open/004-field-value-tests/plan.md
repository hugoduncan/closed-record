# Plan

## Approach

Build one new test namespace `closed-record.field-value-test` in
`test/closed_record/field_value_test.clj`. The namespace defines at the top level:

- `(defclosedrecord C [a ^long n ^double d ^int i ^boolean b ^String s])` and a `defrecord` `DC`
  with the same fields, for V-09.
- `(defclosedrecord N [a b])` and a `defrecord` `DN` with the same fields, for V-08.
- A second closed type for the V-06 call-site test (reuse `C` and `N` where the site needs two
  closed types).

Key decisions (from design.md):

1. **Differential helper, local to the namespace.** The helper takes one function `f` of the
   factory functions (`->Name`, `map->Name`) and calls it twice: once with the closed-type
   factories and once with the `defrecord` factories. Outcomes are compared:
   - Both return: a record result compares as `(into {} x)` plus the class of each value; any
     other result compares with `=` plus its class (box-class parity).
   - Both throw: compare exception classes only.
   - Mixed: fail the test.
   Exceptions are captured with a `thrown`-style helper (`try`/`catch Throwable`), never
   `is (thrown? ...)`, because the comparison is between two runs.
2. **V-09 tests are differential**, one case per row of the type-hint table in the design
   Context: `->Name` casts (`2.7`→`2`, `3`→`3.0`), `->Name` `ClassCastException` and
   `NullPointerException`; `assoc` cast, `ClassCastException`, `NullPointerException` (nil for
   `^long` and `^boolean`), `ArithmeticException` (`^int` ← `5000000000`), object-hint
   acceptance (`^String` ← `5`); `map->Name` `ClassCastException` (`2.5` and `"x"` for `^long`;
   `Long` for `^int`), `NullPointerException` for missing `^boolean`; `vals` box classes;
   keyword call on `^long` field.
3. **V-08 tests are differential** on class-independent results only: `get` with not-found,
   keyword call, `.field` access, `contains?`, `count`, `=` of equal values, equal `hash` of
   equal values, `.hashCode`/`.equals` agreement with `{:a nil :b nil}`, print/read round-trip
   (`=`), and nil ≠ `false` (`(->N nil nil)` not `=` `(->N nil false)`). The helper never
   compares the `hash` of a closed value with a `defrecord` value (D-07).
4. **V-06 uses fixed expected values** (no `defrecord` counterpart): one keyword call site on a
   primitive-hinted field returns the boxed value (`2`, class `Long`); a site that sees closed
   type A, then closed type B, then A again returns correct values each time.
5. **Mutation check per acceptance criterion 3**: after the suite passes, break the emitter in
   `src/closed_record/impl/jvm.clj` one path at a time (lookup thunks, constructor casts,
   `assoc` cast, `create` box-class cast, D-19 missing-primitive path), confirm the matching
   assertions fail, revert, and record each pairing in `implementation.md`.
6. **Emitter defects found by tests are fixed in this task** and recorded in
   `implementation.md`.

Test rules from `mementum/knowledge/testing-generated-types.md` apply: run via the scry CLI on
all three Clojure versions, not only the REPL; use `apply` for wrong-arity factory calls if
clj-kondo `:lint-as defrecord` complains; capture throwables with a helper.

## Risks

- **CLI vs REPL divergence**: tests may pass in the REPL and fail in `scry.cli` (ns/loader
  binding issues). Mitigation: the tests use only top-level defined types and the public API —
  no `eval` of quoted forms — so the known pitfalls mostly do not apply; still verify on all
  three version aliases.
- **Clojure-version differences in cast behaviour** (1.11/1.12/1.13): the differential helper
  absorbs these by construction, but the V-06 fixed-value test could diverge; expected values
  (`2`, `Long`) are stable across versions.
- **Emitter defects discovered**: any parity failure requires a `src` fix, expanding scope
  beyond tests. Budgeted for by design Scope 6.
- **Mutation-check cost**: five-plus emitter mutations across many assertions; keep mutations
  minimal and sequential, revert after each.
- **clj-kondo warnings on generated factories**: unused-var or arity warnings for
  `->C`/`map->C` etc.; handle with existing config patterns, not suppressions in `src`.

## Slice order

1. **Scaffold**: namespace, type definitions (`C`/`DC`, `N`/`DN`), differential helper,
   `thrown` capture helper; one smoke differential test; suite green on 1.11.
2. **V-09**: differential tests for every row of the type-hint table (constructor, `assoc`,
   `map->Name`, `vals`, keyword call).
3. **V-08**: differential nil-value tests (lookup, `contains?`, equality, hashing, print/read,
   nil vs `false`).
4. **V-06**: fixed-expected-value call-site tests (boxed value; polymorphic site A→B→A).
5. **Verification and hardening**: run all three CLI aliases, clj-kondo, cljfmt; mutation
   checks per acceptance criterion 3; fix any emitter defects; write `implementation.md`
   mutation table; update `mementum/state.md` (V-06, V-08, V-09 done).
