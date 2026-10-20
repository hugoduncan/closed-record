# Steps

## Slice 1: Compiler rule tests (V-13)

- [x] Create `test/closed_record/compiler_rules_test.clj` with no `closed-record` requires; `deftype*` forms as quoted data passed to `eval`.
- [x] Namespace of quoted test forms (here and in Slice 5): each `macroexpand-1` or `eval` of a quoted form runs inside `(binding [*ns* (the-ns '<test-ns>)] ...)`, where `<test-ns>` is the namespace of the test.
  In the scry CLI `*ns*` is `user` when a test runs. Without the binding, a referred macro does not expand (no throw) and an unqualified test-namespace var fails at `eval` ("Unable to resolve symbol"). Such a failure is a test-form error, not a rule failure.
- [x] K-06 class lookup (four-field and three-field tests, same in the scry CLI and the REPL): `eval` of a `deftype*` form returns nil, so get the class with
  `(with-bindings {clojure.lang.Compiler/LOADER (clojure.lang.DynamicClassLoader.)} (Class/forName class-name true (clojure.lang.RT/baseLoader)))`.
  The binding makes `RT/baseLoader` a `DynamicClassLoader`, which finds the `eval`ed class in its static class cache. Without it, the CLI throws `ClassNotFoundException` (`Compiler/LOADER` is not bound in a CLI test).
- [x] K-06 four-field test: `deftype*` with declared field `a`, the four hidden fields at the end (`__hash`/`__hasheq` with `^int ^:unsynchronized-mutable`), `:implements [clojure.lang.IRecord]`; get the class with the K-06 class lookup (it loads); assert static `create` exists and the alternative constructors `[Object Object Object]` and `[Object]` exist via `.getConstructors`/`.getParameterTypes`.
- [x] K-06 control test: top-level `(deftype Q [a])` in the test namespace; the class `Q` has `getBasis` and no `create`.
- [x] K-06 three-field tests: all four three-field layouts (each with D-01 metadata on any `__hash`/`__hasheq` present, its own class name); `eval` returns, then the K-06 class lookup throws `java.lang.VerifyError` itself (not wrapper, not cause).
- [x] K-06 reserved-name tests: for each of `__meta __extmap __hash __hasheq`, `(macroexpand-1 '(deftype T [a <name>]))` (inside the `*ns*` binding) throws with `ex-cause` class `AssertionError`.
- [x] K-07 test: `deftype*` with field `a`, the K-06 hidden fields (D-01 metadata), `:implements [clojure.lang.IRecord clojure.lang.IPersistentMap]`, and methods `assoc`, `without` and `cons` that `swap!` their name into an atom and return `this`. The atom is held by a var `calls` of the test namespace; the quoted method bodies name it by the unqualified symbol `calls`, and the `eval` runs inside the `*ns*` binding. `read-string` of `#<class>{:a 1 :x 2}` gives `(.-a o)` 1 and `(.-__extmap o)` `{:x 2}`; `read-string` of `#<class>{:x 2}` gives `(.-a o)` nil; the atom stays `[]` (no calls).
- [x] K-08 test, `defrecord`: top-level `(defrecord R [a])`; `(instance? clojure.lang.IType (->R 1))` is false.
- [x] K-08 test, `IRecord` path: `deftype*` with field `a`, the K-06 hidden fields (D-01 metadata), `:implements [clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]`, and `entrySet` that returns `#{(clojure.lang.MapEntry/create :a 99)}`. `v` is `(eval (list 'new <class> 1 nil nil))`; the constant `c` is `((eval (list 'fn [] v)))`. Assert `(.-a v)` 1, `(class c)` = `(class v)`, `(.-a c)` 99 (`create` from the `java.util.Map` view).
- [x] K-08 test, `IType` before `IRecord`: `deftype*` with field `[a]` only (no hidden fields, so no `create`), the same `entrySet`, `v` from `(eval (list 'new <class> 1))`, constant as above.
  With `:implements [clojure.lang.IType clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]`: `(.-a c)` 1 (`IType` path).
  The same form without `IType` (own class name): the constant throws `java.lang.NoSuchMethodError` itself (`IRecord` path calls the missing `create`).
  Do not add `IType` to the K-06 layout: the `IType` path then throws `VerifyError` (see `implementation.md`).
- [x] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli`, `clojure -M:test:clj-1.13 -m scry.cli`; all exit 0. If any rule fails: stop, record in `implementation.md`, ask the user.
- [x] `clj-kondo --lint src test` reports no errors and no warnings (quoted `deftype*` forms, top-level `deftype`/`defrecord`).

## Slice 2: API skeleton and lint config

- [x] Add `ClosedRecord` protocol with `-extra-keys` to `src/closed_record/core.cljc` (D-17). Portable: outside reader conditionals.
- [x] Create `.clj-kondo/config.edn` with `:lint-as {closed-record.core/defclosedrecord clojure.core/defrecord}`.

## Slice 3: JVM emitter

- [x] Create `src/closed_record/impl/jvm.clj` with one public emitter function (name/type/fields/options/specs → expansion form); no require of `closed-record.core`.
- [x] Emitter argument checks: reject `:volatile-mutable`/`:unsynchronized-mutable` declared-field metadata (`IllegalArgumentException`); accept only options `:no-print` and `:load-ns` (pass to `deftype*`), throw `IllegalArgumentException` on others.
- [x] Symbol-to-interface resolution: var-resolving symbol → `(:on @var)`; `Object`/`java.lang.Object` methods stay; reimplement `maybe-destructured` for non-symbol method args.
- [x] Emit `deftype*` with D-01 field layout (declared fields keep their metadata, then `__meta __extmap` and `^int ^:unsynchronized-mutable __hash __hasheq`), D-06 interfaces plus the `ClosedRecord` protocol interface (as a symbol), not `IType`.
- [x] Emit each method of the embodiment "Method behaviour" table, with closed checks in `case` default branches (D-02) and inline `ex-info` with the exact "Error data" `ex-data` maps.
- [x] Emit `valAt k` (`(.valAt this k nil)`), `isEmpty` (`(= 0 (.count this))`), `containsValue` (`(boolean (some #{v} (vals this)))`).
- [x] Emit `cons` with the three `imap-cons` branches inline (map entry → assoc; any vector → `(nth v 0)`/`(nth v 1)` no count check; else seq of map entries, loop, nil → unchanged).
- [x] Emit `assoc` of a declared key via the declared+`__meta`+`__extmap` constructor, hash caches 0; `withMeta` copies `__extmap` and both hash caches; `getLookupThunk` of undeclared key returns nil.
- [x] Emit the type hash as an expansion-time constant `(hash classname)` with `classname` = `(symbol (str (namespace-munge *ns*) "." Name))`; `hasheq` = `(int (bit-xor type-hash (APersistentMap/mapHasheq this)))`.
- [x] Emit `->Name` (positional-factory reimplementation; works for >20 fields; `ArityException` on wrong count) and `map->Name` (`MapEquivalence` → use argument, else `(into {} m)`; extra-key check → `extra-keys` ex-info with all extra keys as a set; then `Name/create`).
- [x] Emit the full expansion form: `(let [] (declare ->Name) (declare map->Name) (deftype* ...) (import classname) (defn ->Name ...) (defn map->Name ...) classname)`.
- [x] Verify generated code refers only to `closed-record.core/ClosedRecord`, `clojure.lang`, public `clojure.core` vars and `java.lang`/`java.util`/`java.io`; no `closed-record.impl.*` calls, no private `clojure.core` vars.

## Slice 4: Macro and check

- [x] Add `defclosedrecord` macro to `core.cljc` with `defrecord` syntax: reject reserved field names (`AssertionError`) and duplicate field names (`AssertionError`), split options and specs (no key/symbol validation), call the JVM emitter (required via `:clj` reader conditional only), return the emitter form unchanged. The whole `defmacro` form is inside `#?(:clj ...)`; the `:cljs` branch has no macro.
- [x] Add `check` to `core.cljc` (D-09, D-18): closed record with no extras → the record; extra literal keys → `extra-keys` ex-info; other values (including nil) → `not-closed-record` ex-info with `:value-class` (nil for nil). Portable: outside reader conditionals, except the class of `x` (`:record`, `:value-class`, message) is `#?(:clj (class x) :cljs (type x))`.
- [x] Write docstrings for `defclosedrecord` and `check`.
- [x] `clj-kondo --lint src test` reports no errors and no warnings (`core.cljc` is linted for `:clj` and `:cljs`).

## Slice 5: Tests (V-01, V-10, V-15, errors)

- [x] Create emitter/behaviour test namespace(s) using the public macro only (no form comparison).
- [x] V-01: one test per operation per column of the purpose behaviour tables (declared key / undeclared key / etc.); equality table one test per row; `empty` one test; `.field` only in the declared column.
- [x] V-01 reader-literal undeclared-key test: `read-string` a literal with an extra key; instance of type; `get`/keyword call → nil; `contains?` → false; `check` throws `extra-keys` with full `ex-data`.
- [x] V-01 missing declared key (D-19): type with an unhinted field and a `^long` field; `map->Name` and `read-string` without the unhinted key → nil; without the `^long` key → `NullPointerException` itself (no wrapper, no cause); both rows, both cases.
- [x] V-10: 22-field type; `->Name` with 22 args and `map->Name` with all keys give each field in order; `->Name` with 21 and 23 args throws `ArityException`.
  Make the wrong-arity calls with `apply` and an argument vector, for example `(apply ->Name (range 21))` and `(apply ->Name (range 23))`. With `:lint-as` `defrecord`, clj-kondo reports a direct call with a wrong argument count as an error; `apply` gives no finding.
- [x] V-15: `check` of a clean closed record, a record from a literal with extra keys, and other values including nil.
- [x] Expansion-time error tests via the macro: reserved names (`AssertionError`), duplicate fields (`AssertionError`), a `^:volatile-mutable` field (`IllegalArgumentException`), an `^:unsynchronized-mutable` field (`IllegalArgumentException`), unsupported option (`IllegalArgumentException`); assert the `ex-cause` class (acceptance 5).
  Each form is the acceptance 5 form, for example `(macroexpand-1 '(defclosedrecord T [__meta]))`, inside the `*ns*` binding of Slice 1. Put these tests in a namespace that refers `defclosedrecord` and also uses it outside quoted forms; otherwise clj-kondo warns that the referred var is never used.
- [x] `ex-data` tests: each of the 7 "Error data" paths (assoc and cons undeclared-key as separate paths) checks `ex-data` equal to the table map; reuse V-01/V-15 tests where they reach a path.
- [x] Assert acceptance 4 basics: `record?` true, `(instance? IType r)` false, prints as `#ns.Name{...}`.
- [x] In one `implementation.md` section, record the V-01 test-unit-to-test mapping (acceptance 3) and the test of each of the 7 `ex-data` paths, with `assoc` and `cons` separate (acceptance 4).
- [x] `clj-kondo --lint src test` reports no errors and no warnings (`:lint-as` resolves `->Name` and `map->Name`).

## Slice 6: Cleanup and docs

- [x] Remove the smoke test from `test/closed_record/core_test.clj`.
- [x] Update `mementum/state.md` to name `defclosedrecord`, `check` and the test files.
- [x] Update `mementum/knowledge/dev-tooling.md`: remove smoke-test sentences from the "Scry" bullet (keep the zero-tests exit rule); remove the "Smoke-test lint" section.
- [x] Do not change `mementum/memories/deftype-star-hidden-fields.md` or `ramora/` without user approval (Slice 7 architecture findings).

## Slice 7: Final verification

- [x] `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli`, `clojure -M:test:clj-1.13 -m scry.cli` all exit 0, zero failures/errors.
- [x] In the psi project REPL, `(:pass? (scry/run))` is true.
- [x] `clj-kondo --lint src test` reports no errors and no warnings.
- [x] `cljfmt check src test` passes.
- [x] Confirm the only source namespaces are `closed-record.core` and `closed-record.impl.jvm`, and `impl/jvm.clj` does not require `core` (acceptance 6).
- [x] Architecture findings: collect the open "Architecture finding(s)" bullets of `implementation.md` (design passes 1–7 and any from implementation), including the memory file text "With only 3 of these fields, the type does not compile" (it is `VerifyError` at class load). Show them to the user together and ask. Edit `ramora/` or `mementum/memories/deftype-star-hidden-fields.md` only with approval.

## Implementation review (2026-10-19)

- [x] Field names `this` and `gtarget` give wrong results. `generated-methods` in `src/closed_record/impl/jvm.clj`
  uses the literal symbol `this` as the method target and `gtarget` as the `equiv` local. A declared field with the
  same name is shadowed. Observed on 1.11.4: `(defclosedrecord T [this b])` → `(get t :this)`, `seq` and `assoc :b`
  give the record, not the field value. `(defclosedrecord G [gtarget])` → `(= (->G 1) (->G 1))` is false.
  `defrecord` gives the field value and true (it uses gensyms). Use gensyms for the target parameter of each
  generated method and for the `equiv` local, as `emit-defrecord` does. Add a test with fields `this` and `gtarget`
  in `defclosedrecord_test.clj` (`get`, keyword call, `seq`, `assoc` of the other field, `=`).
- [x] `cons` branch 3 does not copy the `imap-cons` loop. It calls `seq` on the argument at each step, and `first`
  and `rest` on the argument, not on its seq. For a map, the first step makes three seqs. Use the `imap-cons` shape:
  `(loop [r this es (seq e)] (if es (recur (assoc r ...) (next es)) r))`. The results and the host exceptions do not
  change.

## Test review (2026-10-19)

- [x] User implementations (Scope 3, D-15) have no test. No test type in `defclosedrecord_test.clj` has specs. Add
  a type with a protocol method, a Java interface method (for example `Comparable`), an `Object` method (for
  example `toString`), a method with a destructured argument, and a protocol method body that calls `map->Name`
  (Decisions "Expansion form"). Check the results through calls only. Observed on 1.11.4: all work.
- [x] Accepted options have no test. `argument-errors-test` checks only the rejected option. Add a test that
  `defclosedrecord` with `:no-print true` and with `:load-ns true` expands and gives a working type (Decisions
  "Allocation of argument processing").
- [x] `cons` branches (Decisions "`cons`") have no test except the success paths. Add to `conj-test` (or a new
  `cons-branches-test`): `[:a 9 9]` → assoc `:a` 9; `[:a]` → `IndexOutOfBoundsException`; `:x` →
  `IllegalArgumentException`; `'([:a 3])` → `ClassCastException`; nil → the record unchanged. Add a `merge` of an
  `array-map` with two undeclared keys: `:key` is the first key in seq order. Observed on 1.11.4: all as the design
  says.
- [x] `map->Name` argument values (Decisions "`map->Name`") have no test. Add to `map-factory-test`: nil → all
  fields nil; a `java.util.HashMap` and a vector of pairs → their entries; `:x` → `IllegalArgumentException`.
- [x] Metadata and extra-key retention have no test. `meta`/`withMeta` (embodiment table) and the copy of `__meta`
  and `__extmap` by `assoc` and `withMeta` (Decisions, embodiment "State") are not checked. Add: `(meta (with-meta r
  m))` is `m`; `assoc` of a declared key keeps the metadata; `check` of `(assoc lit :a 5)` and of
  `(with-meta lit m)` still throws `extra-keys` for a literal `lit` with extra keys.
- [x] Generated methods with no test: `isEmpty`, `containsValue` (with the nil/false quirk, Decisions "Methods not
  in the embodiment table"), `entryAt` (`find`), `iterator`, the `java.util.Map` views (`size`, `get`,
  `containsKey`, `keySet`, `values`, `entrySet`) on declared entries and the mutators (`put`, `remove`, `putAll`,
  `clear` → `UnsupportedOperationException`). Add one test for each method group on a record with no extra keys
  (hidden-key views stay V-14).

## Test review, pass 2 (2026-10-19)

- [x] `equal-other-type-test` does not isolate the type check. `(map->H {:a 1 :n 2})` has other keys than `r`, so the
  assertion holds also without the class test in `equiv`. Replace it with a type that has the same fields and values:
  a second closed record `[a b]` and a `defrecord` `[a b]`, both `(not= r ...)` (purpose "`=` with another type").
  Observed on 1.11.4: both give false.
- [x] The spec symbol `java.lang.Object` (Decisions "Allocation of argument processing": `Object` and
  `java.lang.Object` are not interfaces) has no test. Only `Object` is tested (`U`). Add a type with a
  `java.lang.Object` `toString` and check `str`. Observed on 1.11.4: works.
- [x] `map->Name` of a record argument (Decisions "`map->Name`": a record is not a `MapEquivalence`, so it goes through
  `(into {} m)`) has no test. Add to `map-factory-test`: `(map->R r)` and `map->R` of a `defrecord` `[a b]` give
  `(->R 1 2)`. Observed on 1.11.4: both work.

## Test review, pass 3 (2026-10-19)

- [x] The hash caches after `assoc` (Decisions: `assoc` of a declared key gives hash caches 0) have no test. A copy of
  `__hash` and `__hasheq` into the new instance passes all tests, because no test hashes a record before it calls
  `assoc`. Add to `assoc-test` (or `hash-test`): call `hash` and `.hashCode` on a record, then check that `hash` and
  `.hashCode` of `(assoc r :a 5)` are equal to those of `(->R 5 2)`. Observed on 1.11.4: both are equal.

## Test review, pass 4 (2026-10-19)

- [x] `java.io.Serializable` (D-06, Scope 3 interfaces) has no test. If you remove it from the emitter interfaces, all
  tests pass. Add a test: write `(with-meta r {:m 1})` with `ObjectOutputStream` and read it with
  `ObjectInputStream`. The result is `=` to `r`, has the class `R`, has the metadata `{:m 1}` and has the same `hash`.
  Observed on 1.11.4: all true.

## Test review, pass 5 (2026-10-19)

- [x] The `overage` gensym of `positional-factory` (review follow-ups: "same shadow defect for a field named
  `overage` in a type with >20 fields") has no test. If you change it back to the literal symbol `overage`, all
  tests pass (observed: CLI exit 0). Then `(defclosedrecord OV [overage f1 ... f21])` gives
  `(:overage (apply ->OV (range 22)))` = `(20 21)`, not 0. Add a type with more than 20 fields and the field
  `overage` in the first 20 to `generated-local-names-test` (or `big-factories-test`). Check that `->Name` gives
  each field in order.

## Test review, pass 6 (2026-10-19)

- [x] The pass of `:load-ns` to `deftype*` (Decisions "Allocation of argument processing") has no test.
  `accepted-options-test` checks only that the type works. If the emitter drops the options, all tests pass.
  `:load-ns true` makes the class initialisation `require` the namespace of the type. Add a test: in a new namespace
  from `create-ns` (not a loaded lib, with `refer-clojure`), `eval` of
  `(closed-record.core/defclosedrecord P [a] :load-ns true)` throws with `ex-cause` class
  `ExceptionInInitializerError`, and the same form without the option in another new namespace gives the class.
  Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9 (also for `defrecord`). `:no-print` has no effect in `deftype*`, so
  no test can show it.

## Test review, pass 7 (2026-10-19)

- [x] The copy of the hash caches by `withMeta` (Decisions: `withMeta` copies `__hash` and `__hasheq`) has a test only
  through the order of the tests. All tests use the shared record `r`. A test finds a wrong copy only if an earlier
  test calculated the hashes of `r`. Observed: with `__hash` and `__hasheq` swapped in the `withMeta` constructor
  call, the full CLI run fails, but `serializable-test` alone and `metadata-test` alone pass. Add to `metadata-test`
  (or `hash-test`): make a new record, call `hash` and `.hashCode` on it, then check that `hash` and `.hashCode` of
  `(with-meta x {:m 1})` are equal to those of `x`. The test must fail alone with the swapped caches.

## Test review, pass 9 (2026-10-19)

- [x] The class check of the `getLookupThunk` thunk (`(identical? (class target) gclass)`, else return the thunk) has
  no test. Each keyword call in `keyword-call-test` sees only one type. Observed: with the check removed (thunk always
  reads the field), the full CLI run passes (46 tests). With the check removed, a keyword call site in one compiled
  function gives `ClassCastException` for a second type: `(defn f [x] (:a x))`, `(f (->R 1 2))`, then `(f {:a 3})`.
  Add to `keyword-call-test`: one local `fn` with `(:a x)`; call it on `r`, then on `(->R2 7 8)`, a `(->DR 5 6)` and
  `{:a 3}`; expect `1`, `7`, `5`, `3`. The test must fail with the check removed.

## Test review, pass 10 (2026-10-19)

- [x] `assoc-test` "declared key gives hash caches 0" fills the caches with assertions that are not about `assoc`.
  The first assertion repeats the `hash-test` type-hash formula, and the second repeats `hash-code-test`. Thus a
  wrong type hash also fails `assoc-test`, with a message that points at `assoc` (not orthogonal). Fill the caches
  with no assertion, for example the `let` binding `_ [(hash h) (.hashCode ^Object h)]`, and keep only the two
  assertions on `(assoc h :a 5)`. The test must still fail when `assoc` copies `__hash` and `__hasheq`. Observed
  (clj-kondo v2026.04.15): the `_` binding gives no finding; `(doto x hash .hashCode)` gives "Unused value".

## Test review, pass 11 (2026-10-19)

- [x] Thrown exceptions are captured in two styles. `defclosedrecord_test.clj` has the helper `thrown` (and
  `expansion-cause` for `(some-> (thrown f) ex-cause class)`), but `load-ns-option-test` uses its own
  `(try ... nil (catch Exception e (class (ex-cause e))))`. `compiler_rules_test.clj` repeats
  `(try ... nil (catch Throwable t t))` inline in `k-06-three-hidden-fields-test`, `k-06-reserved-names-test` and
  `k-08-itype-before-irecord-test`. Use `thrown` in `load-ns-option-test` (`(some-> (thrown #(eval-in-new-ns ...))
  ex-cause class)`). Add a private `thrown` helper to `compiler_rules_test.clj` (no closed-record require) and use it
  in the three tests. No assertion changes; the CLI test and assertion counts stay the same.

## Test review, pass 12 (2026-10-19)

- [x] `check-test` "other values throw" does not test a record that is not a closed record. The values are nil, a map
  and `1`, and none is a record. Observed on 1.11.4: if `check` uses `(record? x)` in place of
  `(satisfies? ClosedRecord x)`, the CLI run passes (46 tests, 170 assertions). Add `(check (->DR 1 2))` →
  `ex-data` `{:type :closed-record/not-closed-record :value-class DR}` (D-18, purpose "other values"). The test must
  fail with the `record?` change.
- [x] `load-ns-option-test` does not remove the namespaces that it makes. Each run of `eval-in-new-ns` adds a
  `closed-record.load-ns-probe<n>` namespace that stays in `all-ns` of the REPL. Remove the namespace after the
  `eval` (`try`/`finally` with `remove-ns` in `eval-in-new-ns`). No assertion changes.

## Test review, pass 13 (2026-10-19)

- [x] `=` of the same type (purpose: "true if the declared fields are equal") is checked only for a different last
  field. `equal-same-type-test` has `(not= r (->R 1 3))`, and `generated-local-names-test` has
  `(not= (->S 1 2) (->S 1 3))`. Observed on 1.11.4: with `equiv` comparing `(rest fields)` (the first field is not
  compared), the CLI run passes (46 tests, 171 assertions). `(= (->R 9 2) (conj r [:a 9 9]))` and the other `=`
  assertions then also pass for a wrong first field. Add to `equal-same-type-test`: `(not= r (->R 9 2))`. The test
  must fail with the `(rest fields)` change.

## Test review, pass 14 (2026-10-19)

- [x] The zero-field boundary has only the `isEmpty` test. The emitter has special branches for a type with no fields
  (`containsKey` emits the constant `false`; `without` has no throw clause), but no test calls `contains?`, `dissoc`,
  `assoc` or `->Name` on `Z0`. Observed on 1.11.4: with `containsKey` of a zero-field type changed to `true`, the CLI
  run passes (exit 0). Add a `zero-fields-test` with `(->Z0)`: `(contains? z :a)` false, `(dissoc z :a)` identical to
  `z`, `(seq z)` nil, `(= z (map->Z0 {}))`, `(check z)` identical to `z`, `(assoc z :a 1)` → `ex-data`
  `{:type :closed-record/undeclared-key :record Z0 :key :a}`. Observed on 1.11.4: all as stated. The test must fail
  with the `true` change.

## Test review, pass 15 (2026-10-19)

- [x] `count` and `seq` of a record with fields (embodiment "Method behaviour": `count` is the number of declared
  fields; `seq` gives the declared fields in declaration order) have no test of their own. Only `Z0` (zero fields)
  and `S` (`generated-local-names-test`) call `seq`; no test calls `count`. Observed on 1.11.4: with `count` giving
  `(inc n)`, only `equals-java-map-test`, `is-empty-test` and `java-map-views-test` fail; with `seq` in reverse
  order, only `java-map-views-test`, `record-basics-test` and `generated-local-names-test` fail. Thus the failure
  does not name the defect. Add a `count-seq-test` on `r`: `(count r)` 2, `(seq r)` `[[:a 1] [:b 2]]`, `(keys r)`
  `[:a :b]`, `(vals r)` `[1 2]`. The test must fail with each of the two changes.

## Test review, pass 16 (2026-10-19)

- [x] The split of options and specs (Decisions "Allocation of argument processing": an option map, then each
  protocol, interface or `Object` symbol with its methods) has a test only for one option with no specs (`NP`, `LN`)
  and for specs with no option (`U`). Observed on 1.11.4: with `split-opts+specs` changed to drop the keywords after
  the first option (`(drop-while keyword? (nnext s))`), the CLI run passes (48 tests, 182 assertions). Add to
  `accepted-options-test` a type with two options and a spec, for example
  `(defclosedrecord NPS [a b] :no-print true :load-ns false Shape (area [_] (* a b)) ...)`, and check `area` and
  `(= (->NPS 2 3) (map->NPS {:a 2 :b 3}))`. Observed on 1.11.4: works. The test must fail with the change.

## Code-shaper review (2026-10-19)

- [x] The key parameter of the generated methods has three idioms in `generated-methods`
  (`src/closed_record/impl/jvm.clj`): the auto-gensym `k#` (`valAt`, `getLookupThunk`, `entryAt`, `get`, ...), the
  shared gensym `k` of the outer `let` (`containsKey`, `without`), and a second `k` in the `assoc` `let` that shadows
  the outer `k`. Use one idiom: bind `k` and `v` once in the outer `let` (next to `this`) and use them in every method
  that takes a key or a value. Remove the inner `let` of `assoc`. No behaviour change.
- [x] The closed-record `ex-info` throws have two shapes. `undeclared-key-throw` is a helper with one caller
  (`assoc`). `empty` and `without` build their `ex-info` inline, and `map-factory` builds its message with
  `(str classname)` again, not with `cname-str`. Use one shape for the generated throws: either a helper for each
  `:type` (or one helper that takes the message and the `ex-data` map) or all inline. Give `map-factory` the
  `cname-str` of `emit`. The `ex-data` maps do not change (Decisions "Error data").
- [x] The type hint of the other instance has two placements. `getLookupThunk` has the hint at the use site
  (`(. ~hinted-target -f)` on the unhinted param `gtarget`). `equiv` has the hint at the binding
  (`(let [~hinted-target o#] ...)`) and the use is unhinted (`(. ~gtarget -f)`). Use one placement in both methods.
  Check with `*warn-on-reflection*` that the generated code has no reflection warning.
- [x] `distinct` runs twice on the interfaces. `user-interfaces` calls `distinct` and `vec`, and `emit` calls
  `(vec (distinct (concat ...)))` on the result. Remove `distinct` and `vec` from `user-interfaces`.

## Code-shaper review, pass 2 (2026-10-19)

- [x] The pairs of keyword and field have two argument orders in `generated-methods`
  (`src/closed_record/impl/jvm.clj`). `valAt`, `entryAt`, `seq` and `assoc` use `kws fields` (`(fn [kw fld] ...)`).
  `getLookupThunk` uses `fields kws` (`(fn [fld kw] ...)`). Use `kws fields` in `getLookupThunk`. No behaviour change.
- [x] The three `cons` branches have two idioms for one operation (`assoc` of the key and the value of an entry).
  Branch 1 uses `(.assoc this (key e) (val e))`. Branch 2 uses `.assoc`. Branch 3 uses a `^java.util.Map$Entry` hint
  with `.getKey` and `.getValue`, and `assoc` (not `.assoc`). Use one idiom in all branches, for example `assoc`
  with `key` and `val` (no hint). The results and the host exceptions do not change (Decisions "`cons`": a non-entry
  element still throws `ClassCastException`). Check with `*warn-on-reflection*` that there is no reflection warning.
- [x] The zero-field case and the `case` group of all keys have two forms. `containsKey` makes the group with
  `(~@kws)` and tests `(seq kws)` around the whole `case`. `without` makes the group with `(apply list kws)` and
  tests `(seq kws)` inside the `case` (`~@(when ...)`). Use one form for the group and one place for the zero-field
  test in both methods. No behaviour change (`zero-fields-test` covers both).
- [x] The hidden field names have two definitions. `validate-fields` in `core.cljc` has the set
  `'#{__meta __extmap __hash __hasheq}`, and `emit` in `impl/jvm.clj` writes the same four names in the `deftype*`
  field vector. A change of one does not change the other. Define the hidden fields once in `impl/jvm.clj` (a public
  constant with their metadata) and use it in `emit` and in `validate-fields` (`core.cljc` already requires
  `impl.jvm` in the `:clj` branch, and `validate-fields` is `:clj` only). `impl/jvm.clj` still does not require
  `core` (acceptance 6). No behaviour change.

## Code-shaper review, pass 3 (2026-10-19)

- [x] `hasheq` and `hashCode` in `generated-methods` (`src/closed_record/impl/jvm.clj`) repeat one cache pattern: read
  the field, test zero, calculate, `set!` the field, return the value. Only the field (`__hasheq`, `__hash`) and the
  calculation differ. Add one private helper (for example `cached-hash [field expr]`) that returns the form, and use it
  in both methods. No behaviour change (`hash-test`, `hash-code-test`, `assoc-test` and `metadata-test` cover the
  caches).
- [x] The emitter helpers take the class name in different shapes. `generated-methods` takes `tagname` (the value of
  `tname` in `emit`) and `cname-str`. `map-factory` takes `classname` and `cname-str`. `positional-factory` takes only
  `classname` and makes the docstring with `(str ... classname)`. Use one name for each value (`tname`, `classname`,
  `cname-str`) and give each helper the same arguments in the same order for the same data. No behaviour change.
- [x] `split-opts+specs` (`src/closed_record/core.cljc`) applies `take-while seq?` and `drop-while seq?` to the same
  `(next s)`. Use one `split-with` and destructure the two parts. No behaviour change (`accepted-options-test` covers
  options and specs together).

## Code-shaper review, pass 4 (2026-10-19)

- [x] The `extra-keys` message has two shapes for one `:type`. `map->Name` (`closed-throw` in `impl/jvm.clj`) gives
  "Extra keys #{:x} for closed record my_ns.R". `check` (`core.cljc`) gives "Extra keys #{:x} in closed record class
  my_ns.R" (`str` of a class adds "class "). `not-closed-record` also gives "class java.lang.Long". Observed on
  1.11.4. Use the same words in both `extra-keys` messages, and the class name without the "class " prefix in the
  `check` messages (nil stays nil for `:value-class`). The `ex-data` maps do not change (Decisions "Error data").
- [x] The `ClosedRecord` docstring of `-extra-keys` (`core.cljc`) says "The extra keys of a map literal, or nil". The
  method returns `__extmap` (embodiment: "Returns `__extmap`"), a map of the extra entries, and `check` calls
  `(map key extra)` on it. Change the docstring to name the map of extra entries. No behaviour change.
- [x] The name `kws` has two data shapes in `impl/jvm.clj`. In `generated-methods` it is the vector of the declared
  keywords in field order. In `map-factory` it is a set (used as the `remove` predicate). Give the set in
  `map-factory` its own name (for example `declared`). No behaviour change.


## Code-shaper review, pass 5 (2026-10-19)

- [x] `generated-methods` (`src/closed_record/impl/jvm.clj`) repeats one dispatch shape four times: a `case` on `k`
  with one clause for each pair of keyword and field, then a default. `valAt` (field, `nf`), `entryAt` (map entry,
  nil), `getLookupThunk` (thunk, nil) and `assoc` (constructor, `undeclared-key` throw) each write
  `(case ~k ~@(mapcat (fn [kw fld] [kw ...]) kws fields) default)`. Add one private helper (for example
  `key-case [k kws fields clause-fn default]`) that returns the form, and use it in the four methods. A zero-field
  type gives `(case k default)`, which is valid. No behaviour change (`zero-fields-test`, `get-test`,
  `keyword-call-test`, `assoc-test` and `java-map-views-test` cover the methods).

## Code-shaper review, pass 6 (2026-10-19)

- [x] `check-options` (`src/closed_record/impl/jvm.clj` line 39) has a reflection warning when the namespace loads
  with `*warn-on-reflection*` true: "call to java.lang.IllegalArgumentException ctor can't be resolved" (observed on
  1.11.4 with `(require 'closed-record.core :reload-all)`). `(apply print-str ...)` has no `String` type, and
  `check-field-metadata` uses `str` for its message. Make the argument a `String` (for example `str` around the
  `apply`, or a `^String` hint) and keep the message text. Check that the load gives no reflection warning.

## Code-shaper review, pass 7 (2026-10-19)

- [x] `ctor-short` in `generated-methods` (`src/closed_record/impl/jvm.clj`) is a local function with one caller
  (`assoc`). `withMeta` writes its constructor call inline (`(new ~tname ~@fields m# ~'__meta ...)`). Thus two idioms
  make a new instance. Remove `ctor-short` and write the `assoc` clause inline, for example
  `` (fn [_ fld] `(new ~tname ~@(replace {fld v} fields) ~'__meta ~'__extmap)) ``. No behaviour change (`assoc-test`
  covers it).
- [x] `map-factory` (`impl/jvm.clj`) refers to the class in code through the message string:
  `(symbol (str cname-str "/create"))`. All other generated class references use the symbol `classname`
  (`(new ~classname ...)`, `:record ~classname`), and `cname-str` is otherwise only message and docstring text. Call
  `create` from `classname`, for example `(. ~classname ~'create ~m)`. No behaviour change (`map-factory-test`).
- [x] `check` (`src/closed_record/core.cljc`) calls `(value-class x)` in both branches. Bind `c` once, before the
  `if`. No behaviour change (`check-test`).
