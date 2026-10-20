# Steps

## Slice 1: V-04 tests

- [ ] Create `test/closed_record/literal_test.clj` with ns `closed-record.literal-test`, requiring
      `closed-record.core` and `clojure.test`. Define `(defclosedrecord T [a ^long n])` and a helper to read a
      literal string with `*ns*` bound to the test namespace (copy, do not refer, from `compiler_rules_test.clj`
      patterns).
- [ ] Add round-trip test (I-06): `(read-string (pr-str r))` is `=` to `r` and has the class of `r`, for
      `r` = `(->T ...)` and for an `r` whose field value is a closed record. Each `r` has no metadata. No assertion
      on the metadata of the read-back value.
- [ ] Add `print-dup` round-trip test: `r` has no metadata (with metadata, the output starts with `^#=(...)`). With
      `*print-dup*` true, `pr-str` output starts with `#`, the class name of `T`, and `[`; the read-back value is
      `=` to `r` and has the class of `r`. No assertion on the metadata of the read-back value.
- [ ] Add positional-literal test: reading `#closed_record.literal_test.T[...]` gives a value `=` to the `->T`
      result.
- [ ] Add extra-key literal print test (I-07): for a literal with extra keys, `(pr-str lit)` equals `pr-str` of the
      `->T` result for the declared values; the re-read value is `=` to the literal and `check` returns it.
- [ ] Run `clojure -M:test -m scry.cli`; confirm exit 0, zero failures, zero errors.

## Slice 2: V-05 tests

- [ ] Add the constant macro: argument is a literal string; at expansion time it calls `read-string` on the string
      and returns the record object. No `eval`.
- [ ] Add value-constant test: a `deftest` body using the macro with
      `"^{:m 1} #closed_record.literal_test.T{:a 1 :n 2}"`: result is `=` to the value, has class `T`, and keeps
      the metadata.
- [ ] Add extra-key constant test (I-07, I-09): macro with an extra-key literal string. First assert `check` of the
      `macroexpand-1` result throws `extra-keys`, with `*ns*` bound to the test namespace. Then assert the compiled
      result is `=` to the declared-keys value and `check` returns it.
- [ ] Run `clojure -M:test -m scry.cli`; if a test fails because of the emitter, fix
      `src/closed_record/impl/jvm.clj` and record the cause in `implementation.md`.

## Slice 3: I-07, mutation check, acceptance

- [ ] Update I-07 in `ramora/defclosedrecord/invariants.md`: add the condition to `scope:` (a code constant is a
      constant that the compiler emits into a class via `Compiler.emitValue`; a constant evaluated without a class
      keeps the object); extend `basis:` with the 2026-10-19 task 003 observation and the `Compiler.eval` path.
      Keep the other fields.
- [ ] Mutation check: for each new test, make a temporary change that breaks the path or property that the test
      asserts (edit `impl/jvm.clj`, or a test-namespace edit such as a `print-method` for `T` or
      `*verbose-defrecords*` true), confirm the test fails, revert, and re-run the suite. A change that makes the
      tests fail through `=` or `check` for all values (for example `.equals` returns false, or `check` always
      throws) does not count. One change may cover several tests; each test needs at least one.
- [ ] Record in `implementation.md`, for each test, the change used and the assertion that failed (Acceptance 3).
- [ ] Run `clj-kondo --lint src test` (no errors or warnings) and `cljfmt check src test`.
- [ ] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and
      `clojure -M:test:clj-1.13 -m scry.cli`; confirm all exit 0 with zero failures and errors.
- [ ] Update `mementum/state.md`: V-04 and V-05 done.
