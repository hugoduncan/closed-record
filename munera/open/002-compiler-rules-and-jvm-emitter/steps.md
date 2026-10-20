# Steps

## Slice 1: Compiler rule tests (V-13)

- [ ] Create `test/closed_record/compiler_rules_test.clj` with no `closed-record` requires; `deftype*` forms as quoted data passed to `eval`.
- [ ] K-06 four-field test: `deftype*` with declared field `a`, the four hidden fields at the end (`__hash`/`__hasheq` with `^int ^:unsynchronized-mutable`), `:implements [clojure.lang.IRecord]`; assert static `create` exists and the alternative constructors `[Object Object Object]` and `[Object]` exist via `.getConstructors`/`.getParameterTypes`.
- [ ] K-06 control test: `(deftype Q [a])` has `getBasis` and no `create`.
- [ ] K-06 three-field tests: all four three-field layouts (each with D-01 metadata on any `__hash`/`__hasheq` present, its own class name); `eval` returns, then `(Class/forName name true (RT/baseLoader))` throws `java.lang.VerifyError` itself (not wrapper, not cause).
- [ ] K-06 reserved-name tests: for each of `__meta __extmap __hash __hasheq`, `(macroexpand-1 '(deftype T [a <name>]))` throws with `ex-cause` class `AssertionError`.
- [ ] K-07 tests: `create` puts extra map-literal keys into `__extmap`; missing key gives nil; `create` does not call type methods (e.g. `assoc`).
- [ ] K-08 tests: `defrecord` does not implement `IType`; a record constant without `IType` is built through `create` from the `java.util.Map` view (`Compiler.emitValue` IType-before-IRecord path).
- [ ] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli`, `clojure -M:test:clj-1.13 -m scry.cli`; all exit 0. If any rule fails: stop, record in `implementation.md`, ask the user.

## Slice 2: API skeleton and lint config

- [ ] Add `ClosedRecord` protocol with `-extra-keys` to `src/closed_record/core.cljc` (D-17).
- [ ] Create `.clj-kondo/config.edn` with `:lint-as {closed-record.core/defclosedrecord clojure.core/defrecord}`.

## Slice 3: JVM emitter

- [ ] Create `src/closed_record/impl/jvm.clj` with one public emitter function (name/type/fields/options/specs → expansion form); no require of `closed-record.core`.
- [ ] Emitter argument checks: reject `:volatile-mutable`/`:unsynchronized-mutable` declared-field metadata (`IllegalArgumentException`); accept only options `:no-print` and `:load-ns` (pass to `deftype*`), throw `IllegalArgumentException` on others.
- [ ] Symbol-to-interface resolution: var-resolving symbol → `(:on @var)`; `Object`/`java.lang.Object` methods stay; reimplement `maybe-destructured` for non-symbol method args.
- [ ] Emit `deftype*` with D-01 field layout (declared fields keep their metadata, then `__meta __extmap` and `^int ^:unsynchronized-mutable __hash __hasheq`), D-06 interfaces plus the `ClosedRecord` protocol interface (as a symbol), not `IType`.
- [ ] Emit each method of the embodiment "Method behaviour" table, with closed checks in `case` default branches (D-02) and inline `ex-info` with the exact "Error data" `ex-data` maps.
- [ ] Emit `valAt k` (`(.valAt this k nil)`), `isEmpty` (`(= 0 (.count this))`), `containsValue` (`(boolean (some #{v} (vals this)))`).
- [ ] Emit `cons` with the three `imap-cons` branches inline (map entry → assoc; any vector → `(nth v 0)`/`(nth v 1)` no count check; else seq of map entries, loop, nil → unchanged).
- [ ] Emit `assoc` of a declared key via the declared+`__meta`+`__extmap` constructor, hash caches 0; `withMeta` copies `__extmap` and both hash caches; `getLookupThunk` of undeclared key returns nil.
- [ ] Emit the type hash as an expansion-time constant `(hash classname)` with `classname` = `(symbol (str (namespace-munge *ns*) "." Name))`; `hasheq` = `(int (bit-xor type-hash (APersistentMap/mapHasheq this)))`.
- [ ] Emit `->Name` (positional-factory reimplementation; works for >20 fields; `ArityException` on wrong count) and `map->Name` (`MapEquivalence` → use argument, else `(into {} m)`; extra-key check → `extra-keys` ex-info with all extra keys as a set; then `Name/create`).
- [ ] Emit the full expansion form: `(let [] (declare ->Name) (declare map->Name) (deftype* ...) (import classname) (defn ->Name ...) (defn map->Name ...) classname)`.
- [ ] Verify generated code refers only to `closed-record.core/ClosedRecord`, `clojure.lang`, public `clojure.core` vars and `java.lang`/`java.util`/`java.io`; no `closed-record.impl.*` calls, no private `clojure.core` vars.

## Slice 4: Macro and check

- [ ] Add `defclosedrecord` macro to `core.cljc` with `defrecord` syntax: reject reserved field names (`AssertionError`) and duplicate field names (`AssertionError`), split options and specs (no key/symbol validation), call the JVM emitter (required via `:clj` reader conditional only), return the emitter form unchanged.
- [ ] Add `check` to `core.cljc` (D-09, D-18): closed record with no extras → the record; extra literal keys → `extra-keys` ex-info; other values (including nil) → `not-closed-record` ex-info with `:value-class` (nil for nil).
- [ ] Write docstrings for `defclosedrecord` and `check`.

## Slice 5: Tests (V-01, V-10, V-15, errors)

- [ ] Create emitter/behaviour test namespace(s) using the public macro only (no form comparison).
- [ ] V-01: one test per operation per column of the purpose behaviour tables (declared key / undeclared key / etc.); equality table one test per row; `empty` one test; `.field` only in the declared column.
- [ ] V-01 reader-literal undeclared-key test: `read-string` a literal with an extra key; instance of type; `get`/keyword call → nil; `contains?` → false; `check` throws `extra-keys` with full `ex-data`.
- [ ] V-01 missing declared key (D-19): type with an unhinted field and a `^long` field; `map->Name` and `read-string` without the unhinted key → nil; without the `^long` key → `NullPointerException` itself (no wrapper, no cause); both rows, both cases.
- [ ] V-10: 22-field type; `->Name` with 22 args and `map->Name` with all keys give each field in order; `->Name` with 21 and 23 args throws `ArityException`.
- [ ] V-15: `check` of a clean closed record, a record from a literal with extra keys, and other values including nil.
- [ ] Expansion-time error tests via the macro: reserved names, duplicate fields, mutable field metadata, unsupported option; assert `ex-cause` class per the Decisions (acceptance 5).
- [ ] `ex-data` tests: each of the 7 "Error data" paths (assoc and cons undeclared-key as separate paths) checks `ex-data` equal to the table map; reuse V-01/V-15 tests where they reach a path.
- [ ] Assert acceptance 4 basics: `record?` true, `(instance? IType r)` false, prints as `#ns.Name{...}`.
- [ ] Record the V-01 test-unit-to-test mapping in `implementation.md` (acceptance 3).

## Slice 6: Cleanup and docs

- [ ] Remove the smoke test from `test/closed_record/core_test.clj`.
- [ ] Update `mementum/state.md` to name `defclosedrecord`, `check` and the test files.
- [ ] Update `mementum/knowledge/dev-tooling.md`: remove smoke-test sentences from the "Scry" bullet (keep the zero-tests exit rule); remove the "Smoke-test lint" section.

## Slice 7: Final verification

- [ ] `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli`, `clojure -M:test:clj-1.13 -m scry.cli` all exit 0, zero failures/errors.
- [ ] In the psi project REPL, `(:pass? (scry/run))` is true.
- [ ] `clj-kondo --lint src test` reports no errors and no warnings.
- [ ] `cljfmt check src test` passes.
- [ ] Confirm the only source namespaces are `closed-record.core` and `closed-record.impl.jvm`, and `impl/jvm.clj` does not require `core` (acceptance 6).
