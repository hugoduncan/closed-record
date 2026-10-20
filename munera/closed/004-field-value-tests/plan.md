# Plan

## Approach

Build one new test namespace `closed-record.field-value-test` in
`test/closed_record/field_value_test.clj`. The namespace defines at the top level:

- `(defclosedrecord C [a ^long n ^double d ^int i ^boolean b ^String s])` and a `defrecord` `DC`
  with the same fields, for V-09.
- `(defclosedrecord N [a b])` and a `defrecord` `DN` with the same fields, for V-08.
- `(defclosedrecord N2 [a b])`, the second closed type for the V-06 A → B → A test.

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
   Inputs that the mutation map (decision 5) needs: `vals` of `(->Name 1 2 3 4 true "s")`
   (`^double` ← `3` and `^int` ← `4` are `Long`s that the constructor casts), returned as
   `(mapv class (vals x))`; the keyword row reads `(:n x)` in a function from a value built
   with `->Name` and `^long` ← `2.7` (result `2`, class `Long`).
3. **V-08 tests are differential** on class-independent results only: `get` with not-found,
   keyword call, `.field` access, `contains?`, `count`, `=` of equal values, equal `hash` of
   equal values, `.hashCode`/`.equals` agreement with `{:a nil :b nil}`, print/read round-trip
   (`=`), and nil ≠ `false` (`(->N nil nil)` not `=` `(->N nil false)`). The helper never
   compares the `hash` of a closed value with a `defrecord` value (D-07).
   - Keyword call: one function `(fn [x] (:a x))`, called two times on the nil value.
   - `.equals`: both directions, `(.equals x {:a nil :b nil})` and
     `(.equals {:a nil :b nil} x)`.
   - Print/read: `(read-string (pr-str x))` with `*ns*` bound to the test namespace, as in
     `test/closed_record/literal_test.clj` (`in-test-ns`, `read-literal`). The function returns
     `(= x read-back)`.
4. **V-06 uses fixed expected values** (no `defrecord` counterpart): one keyword call site on a
   primitive-hinted field returns the boxed value (`2`, class `Long`); a site that sees closed
   type A, then closed type B, then A again returns correct values each time.
   - Boxed value: `(:n x)` in a function, `x` built with `->C` and `^long` ← `(int 2)`. The
     result is `2`, class `Long` (not `Integer`).
   - A → B → A: `(fn [x] (:a x))` on an `N`, then on a second closed type with fields `[a b]`
     (`N2`), then on the `N` again.
5. **Mutation check per acceptance criterion 3**: after the suite passes, apply each mutation
   in the "Mutation map" section below, one at a time. Run the scry CLI, confirm that the
   listed tests fail, revert, and record the pairing in `implementation.md`.
6. **Emitter defects found by tests are fixed in this task** and recorded in
   `implementation.md`.

Test rules from `mementum/knowledge/testing-generated-types.md` apply: run via the scry CLI on
all three Clojure versions, not only the REPL; use `apply` for wrong-arity factory calls if
clj-kondo `:lint-as defrecord` complains; capture throwables with a helper.

## Mutation map

The emitter has no cast code and calls no `create` inside the type. The Clojure compiler emits the casts, the
boxing, `create` and the D-19 `NullPointerException` from the `deftype*` field hints: `new` (in `->Name` and
`assoc`) casts each argument to the field type, and `create` casts each map value to the box class and unboxes it.
Thus a mutation changes how the emitter reaches these compiler paths, or drops the hints.

Each mutation is in `src/closed_record/impl/jvm.clj` (line numbers at commit `a74aff8`). Each mapping below was
observed on Clojure 1.11.4 in the scry CLI, with a scratch namespace that had one `deftest` for each test below and
the inputs of decisions 2 to 4 (not committed). No mutation fails at namespace load.

| ID | Form | Change |
|---|---|---|
| M1 | `emit`, `deftype*` field vector (:272) | `(into (vec fields) hidden-fields)` → `(into plain-fields hidden-fields)` (no hints) |
| M2 | `positional-factory`, `(new ~classname ~@field-args)` (:231) | → `(. ~classname ~'create ~(zipmap (map keyword field-args) field-args))` |
| M3 | `assoc` clause (:184) | `(new ~tname ~@(replace {fld v} fields) ~'__meta ~'__extmap)` → `(clojure.lang.Reflector/invokeStaticMethod (class ~this) "create" (object-array [~(zipmap kws (replace {fld v} fields))]))` |
| M4 | `assoc` clause (:184) | the same form → `(clojure.lang.Reflector/invokeConstructor (class ~this) (object-array [~@(replace {fld v} fields) ~'__meta ~'__extmap]))` |
| M5 | `map-factory`, `(. ~classname ~'create ~m)` (:249) | → ``(new ~classname ~@(map (fn [f] `(get ~m ~(keyword f))) fields))`` |
| M6 | `getLookupThunk`, field read (:139) | `(symbol (str "-" fld))` → `(symbol (str "-" (first fields)))` |
| M7 | `getLookupThunk`, class check (:138) | `(identical? (class ~gtarget) ~gclass)` → `true` |
| M8 | `getLookupThunk`, field read (:139) | `(. ~hinted-target …)` → `(let [r# (. ~hinted-target …)] (if (nil? r#) ~thunk r#))` |
| M9 | `valAt` with not-found (:130) | `(fn [_ fld] fld)` → ``(fn [_ fld] `(if (nil? ~fld) ~nf ~fld))`` |
| M10 | `containsKey` (:172) | body → `(some? (.valAt ~this ~k))` |
| M11 | `count` (:143) | `~n` → `(count (remove nil? [~@fields]))` |
| M12 | `equals` (:122) | `(mapEquals ~this o#)` → `(mapEquals (into {} (remove (comp nil? val)) ~this) o#)` |
| M13 | `seq` (:178) | `(seq [...])` → `(seq (filterv (comp some? val) [...]))` |
| M14 | `equiv`, field clause (:169) | `(= ~fld …)` → `(and (some? ~fld) (= ~fld …))` |
| M15 | `equiv`, field clause (:169) | `(= ~fld …)` → `(or (nil? ~fld) (= ~fld …))` |

M3 and M4 use `Reflector`: a direct `(. ~tname ~'create ...)` in a method body fails at load ("No matching method
create found taking 1 args for class compile__stub..."), so it gives no signal. M8 to M15 change the result only for
a nil value, so they break the nil path and not all values.

For each row, operation and case, the mutations that make its test fail:

| Test | Mutations | Note |
|---|---|---|
| `->Name` `^long` ← `2.7`; `^double` ← `3` | M1, M2 | |
| `->Name` `^long` ← `"x"`; `^long` ← nil | M1 | M2 keeps the exception class |
| `assoc` `^long` ← `9.9` | M1, M3 | |
| `assoc` `^long` ← `"x"` | M1, M4 | |
| `assoc` `^long` ← nil; `^boolean` ← nil | M1 | M3 and M4 keep `NullPointerException` |
| `assoc` `^int` ← `5000000000` | M1, M3, M4 | |
| `assoc` `^String` ← `5` | none | The field of an object hint has the type `Object`. No path casts it. |
| `map->Name` `^long` ← `2.5`; `^int` ← `1` | M1, M5 | |
| `map->Name` `^long` ← `"x"`; `^boolean` missing (D-19) | M1 | M5 keeps the exception class (`new` also unboxes nil with `NullPointerException`) |
| `vals` | M1 | |
| V-09 keyword call | M1, M6 | M1 fails only with the `2.7` input |
| V-08 `get` with not-found | M9 | |
| V-08 keyword call | M8 | The site returns the thunk object |
| V-08 `.field` | none | `.-a` compiles to a read of the class field. The emitter has no code on this path. |
| V-08 `contains?` | M10 | |
| V-08 `count` | M11 | |
| V-08 `=` of equal values | M14 | |
| V-08 equal `hash` | none | `hasheq` is the type hash and `mapHasheq` of the entries, so equal values have equal hashes; M13 keeps them equal. A `System/identityHashCode` term in `hasheq` (:117) fails the test, but it breaks all values, so it does not count. |
| V-08 `.hashCode` | M13 | |
| V-08 `.equals` | M12 | M10 and M11 also fail the `{:a nil :b nil}`-first direction |
| V-08 print/read | M14 | The test asserts `(= x read-back)`, and M14 breaks `=` only for nil values, so it counts. No change on the print or read path itself fails it: print gives the map form, and read calls the compiler-generated `create`, which gives nil for a missing key, so with M13 the value reads back `=`. |
| V-08 nil ≠ `false` | M15 | |
| V-06 boxed value | M1, M6 | M1 fails only with the `(int 2)` input (class `Integer`) |
| V-06 A → B → A | M7 | The test errors (`ClassCastException`) |

A `getLookupThunk` that returns nil for each key makes no test fail: the keyword call site then uses `valAt`, which
gives the same results.

## Risks

- **CLI vs REPL divergence**: tests may pass in the REPL and fail in `scry.cli` (ns/loader
  binding issues). The tests use only top-level defined types and the public API, with no
  `eval` of quoted forms. One pitfall applies: `*ns*` is `user` in the CLI, so the print/read
  case reads with `read-string` inside `(binding [*ns* (the-ns test-ns)] ...)` (the
  `literal_test.clj` pattern). Verify on all three version aliases.
- **Clojure-version differences in cast behaviour** (1.11/1.12/1.13): the differential helper
  absorbs these by construction, but the V-06 fixed-value test could diverge; expected values
  (`2`, `Long`) are stable across versions.
- **Emitter defects discovered**: any parity failure requires a `src` fix, expanding scope
  beyond tests. Budgeted for by design Scope 6.
- **Mutation-check cost**: 15 emitter mutations (M1 to M15) across many assertions; apply
  them one at a time and revert after each (`git checkout src/closed_record/impl/jvm.clj`).
- **Mutation map drift**: the map was observed with scratch tests. If a final test uses other
  inputs, a listed mutation can stop failing it. Then change the input back to the one in
  decisions 2 to 4, or record the new pairing.
- **clj-kondo warnings on generated factories**: unused-var or arity warnings for
  `->C`/`map->C` etc.; handle with existing config patterns, not suppressions in `src`.

## Slice order

1. **Scaffold**: namespace, type definitions (`C`/`DC`, `N`/`DN`, `N2`), differential helper,
   `thrown` capture helper; one smoke differential test; suite green on 1.11.
2. **V-09**: differential tests for every row of the type-hint table (constructor, `assoc`,
   `map->Name`, `vals`, keyword call).
3. **V-08**: differential nil-value tests (lookup, `contains?`, equality, hashing, print/read,
   nil vs `false`).
4. **V-06**: fixed-expected-value call-site tests (boxed value; polymorphic site A→B→A).
5. **Verification and hardening**: run all three CLI aliases, clj-kondo, cljfmt; mutation
   checks M1 to M15 per the mutation map (acceptance criterion 3); fix any emitter defects; write `implementation.md`
   mutation table; update `mementum/state.md` (V-06, V-08, V-09 done).
