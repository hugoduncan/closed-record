# Plan

## Approach

One new test namespace `closed-record.literal-test` in `test/closed_record/literal_test.clj`. Build it in three
slices: V-04 tests (print, read, round trips), V-05 tests (code constants through a macro), then the I-07 wording
change and the mutation check.

Key decisions (from design.md):

- Define one closed type `T` in the test namespace with an unhinted field and a `^long` field, plus a nested-record
  case for the round trip. Copy private helpers from `compiler_rules_test.clj` as needed; do not refer to them.
- Read literals with `*ns*` bound to the test namespace. At file load, macro expansion already runs in the test
  namespace; only run-time `macroexpand-1` calls need the binding.
- The constant macro takes a literal string, calls `read-string` at expansion time, and returns the record object.
  No `eval` in the macro: `eval` of a form that contains the object drops `__extmap` before the macro returns.
- Printed-output assertions do not compare with a literal expected string. The `print-dup` test asserts a prefix
  (`#` + class name of `T` + `[`) and the read-back value. The extra-key print test compares `(pr-str lit)` with
  `pr-str` of the `->T` result.
- Constants are tested in a compiled function body only (a `deftest` body is one). No top-level `def` test.
- I-07 change: `scope:` gets the "emits into a class" condition; `basis:` keeps K-07 and E-03 and adds the task 003
  observation and the `Compiler.eval` path. No other architecture file changes.
- Mutation check: temporary edits to `src/closed_record/impl/jvm.clj` or to the test namespace (for example a
  `print-method` for `T`, or `*verbose-defrecords*` true). One change can cover more than one test; each test has at
  least one change; revert each change and re-run.
- Fix the emitter in `src/closed_record/impl/jvm.clj` only if a test fails because of it. Record the cause in
  `implementation.md`.

## Risks

- `emitValue` with `IType` emits all fields including `__extmap` (mutation hint, not checked). If a V-05 test fails
  on the real emitter, an emitter fix in `impl/jvm.clj` is in scope; this could grow the task.
- The value-constant mutation may be hard to break with a single edit (the design notes the `IType` edit probably
  does not fail the value test). Finding a breaking change per test may need iteration.
- Printer output differs between Clojure 1.11, 1.12 and 1.13 only in separators; prefix and value assertions avoid
  this, but the three-version run (Acceptance 2) is the check.
- `*print-dup*` behaviour depends on `*verbose-defrecords*` default staying false; the test is a change signal by
  design.

## Slice order

1. V-04 tests: test namespace, type `T`, round trip (plain, nested, metadata-free), `print-dup` round trip,
   positional literal, extra-key literal print.
2. V-05 tests: constant macro, value-constant test (equality, class, metadata), extra-key constant test
   (`macroexpand-1` fails `check`, compiled result passes).
3. I-07 wording in `ramora/defclosedrecord/invariants.md`; mutation check per test; lint, format,
   three-version test runs; `mementum/state.md` update.
