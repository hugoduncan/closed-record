# Steps

## Slice 1: Compiler rule tests (V-13)

- [ ] Create `test/closed_record/compiler_rules_test.clj` with no `closed-record` requires; `deftype*` forms as quoted data passed to `eval`.
- [ ] Namespace of quoted test forms (here and in Slice 5): each `macroexpand-1` or `eval` of a quoted form runs inside `(binding [*ns* (the-ns '<test-ns>)] ...)`, where `<test-ns>` is the namespace of the test.
  In the scry CLI `*ns*` is `user` when a test runs. Without the binding, a referred macro does not expand (no throw) and an unqualified test-namespace var fails at `eval` ("Unable to resolve symbol"). Such a failure is a test-form error, not a rule failure.
- [ ] K-06 class lookup (four-field and three-field tests, same in the scry CLI and the REPL): `eval` of a `deftype*` form returns nil, so get the class with
  `(with-bindings {clojure.lang.Compiler/LOADER (clojure.lang.DynamicClassLoader.)} (Class/forName class-name true (clojure.lang.RT/baseLoader)))`.
  The binding makes `RT/baseLoader` a `DynamicClassLoader`, which finds the `eval`ed class in its static class cache. Without it, the CLI throws `ClassNotFoundException` (`Compiler/LOADER` is not bound in a CLI test).
- [ ] K-06 four-field test: `deftype*` with declared field `a`, the four hidden fields at the end (`__hash`/`__hasheq` with `^int ^:unsynchronized-mutable`), `:implements [clojure.lang.IRecord]`; get the class with the K-06 class lookup (it loads); assert static `create` exists and the alternative constructors `[Object Object Object]` and `[Object]` exist via `.getConstructors`/`.getParameterTypes`.
- [ ] K-06 control test: top-level `(deftype Q [a])` in the test namespace; the class `Q` has `getBasis` and no `create`.
- [ ] K-06 three-field tests: all four three-field layouts (each with D-01 metadata on any `__hash`/`__hasheq` present, its own class name); `eval` returns, then the K-06 class lookup throws `java.lang.VerifyError` itself (not wrapper, not cause).
- [ ] K-06 reserved-name tests: for each of `__meta __extmap __hash __hasheq`, `(macroexpand-1 '(deftype T [a <name>]))` (inside the `*ns*` binding) throws with `ex-cause` class `AssertionError`.
- [ ] K-07 test: `deftype*` with field `a`, the K-06 hidden fields (D-01 metadata), `:implements [clojure.lang.IRecord clojure.lang.IPersistentMap]`, and methods `assoc`, `without` and `cons` that `swap!` their name into an atom and return `this`. The atom is held by a var `calls` of the test namespace; the quoted method bodies name it by the unqualified symbol `calls`, and the `eval` runs inside the `*ns*` binding. `read-string` of `#<class>{:a 1 :x 2}` gives `(.-a o)` 1 and `(.-__extmap o)` `{:x 2}`; `read-string` of `#<class>{:x 2}` gives `(.-a o)` nil; the atom stays `[]` (no calls).
- [ ] K-08 test, `defrecord`: top-level `(defrecord R [a])`; `(instance? clojure.lang.IType (->R 1))` is false.
- [ ] K-08 test, `IRecord` path: `deftype*` with field `a`, the K-06 hidden fields (D-01 metadata), `:implements [clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]`, and `entrySet` that returns `#{(clojure.lang.MapEntry/create :a 99)}`. `v` is `(eval (list 'new <class> 1 nil nil))`; the constant `c` is `((eval (list 'fn [] v)))`. Assert `(.-a v)` 1, `(class c)` = `(class v)`, `(.-a c)` 99 (`create` from the `java.util.Map` view).
- [ ] K-08 test, `IType` before `IRecord`: `deftype*` with field `[a]` only (no hidden fields, so no `create`), the same `entrySet`, `v` from `(eval (list 'new <class> 1))`, constant as above.
  With `:implements [clojure.lang.IType clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]`: `(.-a c)` 1 (`IType` path).
  The same form without `IType` (own class name): the constant throws `java.lang.NoSuchMethodError` itself (`IRecord` path calls the missing `create`).
  Do not add `IType` to the K-06 layout: the `IType` path then throws `VerifyError` (see `implementation.md`).
- [ ] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli`, `clojure -M:test:clj-1.13 -m scry.cli`; all exit 0. If any rule fails: stop, record in `implementation.md`, ask the user.
- [ ] `clj-kondo --lint src test` reports no errors and no warnings (quoted `deftype*` forms, top-level `deftype`/`defrecord`).

## Slice 2: API skeleton and lint config

- [ ] Add `ClosedRecord` protocol with `-extra-keys` to `src/closed_record/core.cljc` (D-17). Portable: outside reader conditionals.
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

- [ ] Add `defclosedrecord` macro to `core.cljc` with `defrecord` syntax: reject reserved field names (`AssertionError`) and duplicate field names (`AssertionError`), split options and specs (no key/symbol validation), call the JVM emitter (required via `:clj` reader conditional only), return the emitter form unchanged. The whole `defmacro` form is inside `#?(:clj ...)`; the `:cljs` branch has no macro.
- [ ] Add `check` to `core.cljc` (D-09, D-18): closed record with no extras → the record; extra literal keys → `extra-keys` ex-info; other values (including nil) → `not-closed-record` ex-info with `:value-class` (nil for nil). Portable: outside reader conditionals, except the class of `x` (`:record`, `:value-class`, message) is `#?(:clj (class x) :cljs (type x))`.
- [ ] Write docstrings for `defclosedrecord` and `check`.
- [ ] `clj-kondo --lint src test` reports no errors and no warnings (`core.cljc` is linted for `:clj` and `:cljs`).

## Slice 5: Tests (V-01, V-10, V-15, errors)

- [ ] Create emitter/behaviour test namespace(s) using the public macro only (no form comparison).
- [ ] V-01: one test per operation per column of the purpose behaviour tables (declared key / undeclared key / etc.); equality table one test per row; `empty` one test; `.field` only in the declared column.
- [ ] V-01 reader-literal undeclared-key test: `read-string` a literal with an extra key; instance of type; `get`/keyword call → nil; `contains?` → false; `check` throws `extra-keys` with full `ex-data`.
- [ ] V-01 missing declared key (D-19): type with an unhinted field and a `^long` field; `map->Name` and `read-string` without the unhinted key → nil; without the `^long` key → `NullPointerException` itself (no wrapper, no cause); both rows, both cases.
- [ ] V-10: 22-field type; `->Name` with 22 args and `map->Name` with all keys give each field in order; `->Name` with 21 and 23 args throws `ArityException`.
  Make the wrong-arity calls with `apply` and an argument vector, for example `(apply ->Name (range 21))` and `(apply ->Name (range 23))`. With `:lint-as` `defrecord`, clj-kondo reports a direct call with a wrong argument count as an error; `apply` gives no finding.
- [ ] V-15: `check` of a clean closed record, a record from a literal with extra keys, and other values including nil.
- [ ] Expansion-time error tests via the macro: reserved names (`AssertionError`), duplicate fields (`AssertionError`), a `^:volatile-mutable` field (`IllegalArgumentException`), an `^:unsynchronized-mutable` field (`IllegalArgumentException`), unsupported option (`IllegalArgumentException`); assert the `ex-cause` class (acceptance 5).
  Each form is the acceptance 5 form, for example `(macroexpand-1 '(defclosedrecord T [__meta]))`, inside the `*ns*` binding of Slice 1. Put these tests in a namespace that refers `defclosedrecord` and also uses it outside quoted forms; otherwise clj-kondo warns that the referred var is never used.
- [ ] `ex-data` tests: each of the 7 "Error data" paths (assoc and cons undeclared-key as separate paths) checks `ex-data` equal to the table map; reuse V-01/V-15 tests where they reach a path.
- [ ] Assert acceptance 4 basics: `record?` true, `(instance? IType r)` false, prints as `#ns.Name{...}`.
- [ ] In one `implementation.md` section, record the V-01 test-unit-to-test mapping (acceptance 3) and the test of each of the 7 `ex-data` paths, with `assoc` and `cons` separate (acceptance 4).
- [ ] `clj-kondo --lint src test` reports no errors and no warnings (`:lint-as` resolves `->Name` and `map->Name`).

## Slice 6: Cleanup and docs

- [ ] Remove the smoke test from `test/closed_record/core_test.clj`.
- [ ] Update `mementum/state.md` to name `defclosedrecord`, `check` and the test files.
- [ ] Update `mementum/knowledge/dev-tooling.md`: remove smoke-test sentences from the "Scry" bullet (keep the zero-tests exit rule); remove the "Smoke-test lint" section.
- [ ] Do not change `mementum/memories/deftype-star-hidden-fields.md` or `ramora/` without user approval (Slice 7 architecture findings).

## Slice 7: Final verification

- [ ] `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli`, `clojure -M:test:clj-1.13 -m scry.cli` all exit 0, zero failures/errors.
- [ ] In the psi project REPL, `(:pass? (scry/run))` is true.
- [ ] `clj-kondo --lint src test` reports no errors and no warnings.
- [ ] `cljfmt check src test` passes.
- [ ] Confirm the only source namespaces are `closed-record.core` and `closed-record.impl.jvm`, and `impl/jvm.clj` does not require `core` (acceptance 6).
- [ ] Architecture findings: collect the open "Architecture finding(s)" bullets of `implementation.md` (design passes 1–7 and any from implementation), including the memory file text "With only 3 of these fields, the type does not compile" (it is `VerifyError` at class load). Show them to the user together and ask. Edit `ramora/` or `mementum/memories/deftype-star-hidden-fields.md` only with approval.
