# Steps

## Slice 1: V-04 tests

- [x] Create `test/closed_record/literal_test.clj` with ns `closed-record.literal-test`, requiring
      `closed-record.core` and `clojure.test`. Define `(defclosedrecord T [a ^long n])` and a helper to read a
      literal string with `*ns*` bound to the test namespace (copy, do not refer, from `compiler_rules_test.clj`
      patterns).
- [x] Add round-trip test (I-06): `(read-string (pr-str r))` is `=` to `r` and has the class of `r`, for
      `r` = `(->T ...)` and for an `r` whose field value is a closed record. Each `r` has no metadata. No assertion
      on the metadata of the read-back value.
- [x] Add `print-dup` round-trip test: `r` has no metadata (with metadata, the output starts with `^#=(...)`). With
      `*print-dup*` true, `pr-str` output starts with `#`, the class name of `T`, and `[`; the read-back value is
      `=` to `r` and has the class of `r`. No assertion on the metadata of the read-back value.
- [x] Add positional-literal test: reading `#closed_record.literal_test.T[...]` gives a value `=` to the `->T`
      result.
- [x] Add extra-key literal print test (I-07): for a literal with extra keys, `(pr-str lit)` equals `pr-str` of the
      `->T` result for the declared values; the re-read value is `=` to the literal and `check` returns it.
- [x] Run `clojure -M:test -m scry.cli`; confirm exit 0, zero failures, zero errors.

## Slice 2: V-05 tests

- [x] Add the constant macro: argument is a literal string; at expansion time it calls `read-string` on the string
      and returns the record object. No `eval`.
- [x] Add value-constant test: a `deftest` body using the macro with
      `"^{:m 1} #closed_record.literal_test.T{:a 1 :n 2}"`: result is `=` to the value, has class `T`, and keeps
      the metadata.
- [x] Add extra-key constant test (I-07, I-09): macro with an extra-key literal string. First assert `check` of the
      `macroexpand-1` result throws `extra-keys`, with `*ns*` bound to the test namespace. Then assert the compiled
      result is `=` to the declared-keys value and `check` returns it.
- [x] Run `clojure -M:test -m scry.cli`; if a test fails because of the emitter, fix
      `src/closed_record/impl/jvm.clj` and record the cause in `implementation.md`.

## Slice 3: I-07, mutation check, acceptance

- [x] Update I-07 in `ramora/defclosedrecord/invariants.md`: add the condition to `scope:` (a code constant is a
      constant that the compiler emits into a class via `Compiler.emitValue`; a constant evaluated without a class
      keeps the object); extend `basis:` with the 2026-10-19 task 003 observation and the `Compiler.eval` path.
      Keep the other fields.
- [x] Mutation check: for each new test, make a temporary change that breaks the path or property that the test
      asserts (edit `impl/jvm.clj`, or a test-namespace edit such as a `print-method` for `T` or
      `*verbose-defrecords*` true), confirm the test fails, revert, and re-run the suite. A change that makes the
      tests fail through `=` or `check` for all values (for example `.equals` returns false, or `check` always
      throws) does not count. One change may cover several tests; each test needs at least one.
- [x] Record in `implementation.md`, for each test, the change used and the assertion that failed (Acceptance 3).
- [x] Run `clj-kondo --lint src test` (no errors or warnings) and `cljfmt check src test`.
- [x] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and
      `clojure -M:test:clj-1.13 -m scry.cli`; confirm all exit 0 with zero failures and errors.
- [x] Update `mementum/state.md`: V-04 and V-05 done.

## Implementation review (2026-10-19)

- [x] `extra-key-literal-print-test`: assert that `lit` has extra keys before the print assertions (`check` of
      `lit` throws `extra-keys`, as in `extra-key-constant-test`). Now, if `lit` has no `__extmap` (for example a
      literal in the `deftest` source, which is a code constant), the test passes and does not show that print drops
      `__extmap` (I-07). The recorded mutation (`seq` reverse) breaks all printing, not the drop of `__extmap`. Run
      the mutation "`seq` adds `__extmap` entries" against this test and record it in `implementation.md`.

## Implementation review, pass 2 (2026-10-19)

- [x] `mementum/state.md`: the `Tests:` line lists `compiler_rules_test.clj` and `defclosedrecord_test.clj` only. Add
      `test/closed_record/literal_test.clj` (V-04, V-05: print, read and code constants).

## Test review, pass 2 (2026-10-19)

- [x] `print-dup-round-trip-test`: the prefix assertion `(is (.startsWith ^String s ...))` reports only
      `actual: false` when it fails (`.startsWith` is not a var, so `clojure.test` does not show its arguments). This
      assertion is the change signal for the Clojure printer fact, so a failure must show the new output `s`. Assert
      with `=` on the prefix (for example `(is (= prefix (subs s 0 (min (count s) (count prefix)))))`), or put `s` in
      the `testing` label or the `is` message.

## Code-shaper review (2026-10-19)

- [x] `literal_test.clj`: one idiom for the extra-keys assertion. `extra-key-literal-print-test` inlines
      `(:type (ex-data (thrown #(check lit))))`; `extra-key-constant-test` binds `e` and then reads
      `(:type (ex-data e))`. Add one private helper (for example `check-error-type`: the `:type` of the `check`
      exception of `x`, or nil) and use it in both tests.
- [x] `literal_test.clj`: `round-trip-test` and `print-dup-round-trip-test` each have the literal vector
      `[(->T 1 2) (->T (->T 3 4) 5)]`. Define it once (for example `(def ^:private round-trip-values ...)`, no
      metadata) and use it in both tests, so that the plain and nested cases cannot drift apart.
