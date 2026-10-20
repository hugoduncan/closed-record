# Design steps

Follow-up items from design reviews. Tick an item when design.md is updated.

## Architecture review (2026-10-19)

- [x] Run-time dependencies of the generated code. Embodiment "Boundaries" permits generated code to refer only to
  `closed-record.core/ClosedRecord` and `clojure.lang`, and forbids user code → `impl.*`. The design does not say
  where the run-time code of the generated methods is (the `ex-info` construction of D-03, the `cons` entry loop,
  the extra-key check of `map->Name`). Add a decision to Scope 3 or Decisions: the generated code calls no function
  in `closed-record.impl.*` and no private `clojure.core` var. Example: `defrecord` `cons` calls the private
  `clojure.core/imap-cons` through `(var imap-cons)`, and the design rejects a dependency on private functions in
  "Approaches". The emitter emits this code inline, or the generated code uses only public `clojure.core` functions.
- [x] Immutability of the declared fields. Embodiment "State" says that the declared fields are immutable. `deftype*`
  makes a field mutable if it has `^:volatile-mutable` or `^:unsynchronized-mutable` metadata. `defrecord` rejects
  this metadata with `IllegalArgumentException`. Scope 3 says "The declared fields keep their type hints", and Scope 4
  names only the reserved-name and duplicate-field checks, so the design does not enforce the State rule. Add to the
  design how the emitter or the macro keeps the declared fields immutable, as `defrecord` does (D-15). Use the
  existing scope items. Do not add a new scope item.
- [x] Hash caches after `withMeta`. The Decisions say that `withMeta` makes a new instance with hash caches 0.
  `defrecord` `withMeta` copies `__hash` and `__hasheq` (Clojure 1.11.4 `core_deftype.clj`, `iobj`). D-07 says that
  metadata has no effect on the hash, so the copied caches stay correct. K-05 and principle 15 require `defrecord`
  parity. Change the decision so that `withMeta` copies the hash caches, or record why the difference is necessary.
  `assoc` of a declared key keeps caches 0, as `defrecord` does.

## Architecture review, pass 2 (2026-10-19)

- [x] Expansion-time dependencies on private `clojure.core` vars. Scope 3 limits only the generated code. The
  `defrecord` parsing of options and specs (D-15) is private: `parse-opts+specs` (protocol symbol → interface,
  `:no-print` and `:load-ns`, unsupported-option error), `parse-impls` and `clojure.core/maybe-destructured`
  (destructured method arguments) (Clojure 1.11.4 `core_deftype.clj` lines 40–68, `core.clj` line 4517). "Approaches"
  rejects `emit-defrecord` because a private function can change in each Clojure version, and V-13 gives a signal
  only for K-06 to K-08 (R-01). State in Scope 3 or Decisions whether the macro and the emitter can call private
  `clojure.core` vars at expansion time. If they cannot, state that the emitter reimplements this parsing with
  public functions. If they can, name the vars and the test that fails when they change.

## Ambiguity review (2026-10-19)

- [x] Error of the macro argument checks. Scope 4 and acceptance 5 say only that a reserved field name or a duplicate
  field "throws at expansion time". They do not give the exception class or data. D-03 does not include these
  errors, and D-20 makes public behaviour hard to change after the first release. `defrecord` throws `AssertionError`
  for a reserved name. Since Clojure 1.10, `macroexpand-1` wraps a macro exception in `CompilerException`. State
  the exception class, whether it has `ex-data` (and the keys if it has), whether it is public API, and whether the
  tests check the wrapper or its cause.
- [x] `ex-data` of `cons` of a map with undeclared keys. The Decisions table gives `:key k` (one key) for `assoc` or
  `cons`. It gives `:keys ks` (all keys) for `map->Name`. `merge` and `into` call `cons` with a map that can contain
  more than one undeclared key. State which form `cons` of a map uses. If it is `:key`, state which key (for example,
  the first undeclared key in the seq order of the argument). If it is `:keys`, state that `cons` checks all entries
  before it calls `assoc`.
- [x] Test unit for the behaviour tables. Scope 6 (V-01) says "one test for each row and each column", and acceptance
  3 says "each row and each column maps to one or more tests". It is not clear if the unit is a cell (row × column),
  and if a row that names more than one operation (`get`, keyword call, `.field`; `assoc`, `conj`, `merge`, `into`,
  `update`) needs a test for each operation. State the unit. Acceptance 3 is then verifiable from the mapping in
  `implementation.md`.

## Ambiguity review, pass 2 (2026-10-19)

- [x] K-06 "three of these four fields, the type does not compile" (Scope 1). Observed on 1.11.4, 1.12.6 and
  1.13.0-alpha9: `eval` of a `deftype*` form with any three of the four fields succeeds. The class fails with
  `VerifyError` at class initialisation (for example `Class/forName` with initialise true, or the first `new`). State
  which three-field layouts the test covers (one or all four), and what the test does and checks: the operation that
  causes the failure, and the class it expects (`VerifyError`, any `Throwable`, or the cause of a wrapper).
- [x] `cons` argument classes. The Decisions name three cases (map entry, vector pair, map) and say that other values
  throw `IllegalArgumentException` "as `defrecord` does". `defrecord` `imap-cons` (same in 1.11.4, 1.12.6 and
  1.13.0-alpha9) has three branches: map entry, any `IPersistentVector` (it uses `nth` 0 and 1, with no count check),
  and else a loop over `(seq o)` of map entries. Observed on 1.11.4: `[:a 9 9]` → assoc `:a 9`; `[:a]` →
  `IndexOutOfBoundsException`; `:x` → `IllegalArgumentException`; `'([:a 3])` → `ClassCastException`; nil → the
  record unchanged; a seq of map entries or another record → assoc of each entry. State whether "map" means any
  seqable of map entries and whether the emitter copies all the `imap-cons` branches (then state the result for a
  vector that is not a pair, for nil and for other values), or state the cases where it differs and why.
- [x] "hidden" in the reader literal row (Scope 6, V-01). The row says "extra key → read, hidden, `check` throws".
  The design does not say what the V-01 test checks for "hidden". V-14 (count, seq, `=`, hash, print, the
  `java.util.Map` views) is adjacent work. State the assertions of the V-01 test for "hidden" (for example `get`
  gives nil and `contains?` gives false for the extra key).

## Inconsistency review (2026-10-19)

- [x] Compiler rule tests and clj-kondo. Scope 1 says the tests use `deftype*` directly. Acceptance 7 requires
  `clj-kondo --lint src test` with no errors. Scope 9 configures only `lint-as` for `defclosedrecord`, and clj-kondo
  hooks are out of scope. The local clj-kondo reports a literal `deftype*` form as errors: "Unresolved symbol" for each
  field, method name and method argument (observed). State how the tests satisfy acceptance 7 in the existing scope.
  Examples: build the `deftype*` forms as data and `eval` them, or use `#_:clj-kondo/ignore`, or add a
  `.clj-kondo/config.edn` entry for the test namespace.
- [x] Smoke-test text in `dev-tooling.md`. Scope 10 says to update "the smoke test section". The page has two places
  that describe the smoke test: the "Scry" bullet ("`core_test.clj` has a smoke test for this reason") and the
  section "Smoke-test lint". Acceptance 10 applies to both. Name both places in Scope 10. Also state that the rule
  "the scry CLI exits non-zero when it finds zero tests" stays, because it is still true.

## Inconsistency review, pass 2 (2026-10-19)

- [x] Message text of the `ex-info` errors. The Decisions say for the whole error table "the message names the type
  and the key or keys". The `empty` row has no key. The `not-closed-record` row has no type `T`, only
  `:value-class`. State the message content for each row (for example: `empty` names the type; `not-closed-record`
  names the value class), or limit the rule to the rows that have `:key` or `:keys`.

## Architecture review, pass 3 (2026-10-19)

- [x] Allocation of the host-specific argument parsing. The Concepts and the Decisions say that the macro calls the
  emitter "with the parsed arguments". "Expansion-time dependencies" says that "the macro and the emitter" reimplement
  `parse-opts+specs`, but it does not say which namespace does each step. Some steps are JVM-specific: a protocol
  symbol → its interface (`:on` of the var value), the exclusion of `Object` and `java.lang.Object` (ClojureScript
  `defrecord` accepts `Object` methods), and the `deftype*` options `:no-print` and `:load-ns`. D-13 and K-02 put
  host-specific code in the emitter, behind the macro. The portable `core.cljc` contains only the public API and
  host-neutral checks. State which steps the macro in `core.cljc` does (for example the reserved-name, duplicate-field
  and mutable-metadata checks, and the split of options and specs) and which steps `closed-record.impl.jvm` does (for
  example the protocol-to-interface resolution, the `Object` rule and the option check). Use the existing scope
  items.

## Ambiguity review, pass 3 (2026-10-19)

- [x] Argument and construction path of `map->Name`. The design says only that `map->Name` "checks the extra keys
  before it constructs the instance". `defrecord` `map->Name` (Clojure 1.11.4 `core_deftype.clj` lines 400–403) calls
  `create` with the argument if it is `MapEquivalence`, else with `(into {} m)`. Observed on 1.11.4: nil → all fields
  nil; `[[:a 1]]` → `:a` 1; a `java.util.HashMap` → its entries; `:x` → `IllegalArgumentException`; a missing
  `^long` field → `NullPointerException` (D-19, through `create`). State the argument values that `map->Name` accepts
  (for example the same as `defrecord`: nil, any map, a `java.util.Map` and a seqable of pairs), the key set that the
  extra-key check uses (after the `into {}` conversion or before it), and whether it constructs through `create`
  (then D-19 holds by construction) or through a constructor.

## Inconsistency review, pass 3 (2026-10-19)

- [x] Versions in "Problem". "Problem" says "Clojure 1.12.6 and 1.13.0-alpha9 were not tested". Scope 1 (K-06
  three-field layouts: "observed on 1.11.4, 1.12.6 and 1.13.0-alpha9"), "Observed facts" (`conj`: "also on 1.12.6
  and 1.13.0-alpha9") and the `cons` and "Expansion-time dependencies" decisions ("the same in 1.11.4, 1.12.6 and
  1.13.0-alpha9") say that facts were checked on these versions. The heading "Observed facts (2026-10-19, Clojure
  1.11.4)" also names only 1.11.4. Make "Problem" and the heading agree with these statements. For example: the
  architecture observations (K-06 to K-08) were made on 1.11.4 and 1.13.0-alpha6 with temporary tests; some facts
  were checked again on 1.12.6 and 1.13.0-alpha9 during the design review; no test in the repository fixes them.
- [x] Permitted references of the generated code. Scope 3 says that the generated code "refers only to
  `closed-record.core/ClosedRecord`, `clojure.lang` and public `clojure.core` vars". The same design requires other
  host classes in the generated code: the D-06 interfaces `java.util.Map` and `java.io.Serializable` (Scope 3), the
  `java.util.Map$Entry` elements of the `cons` loop (`cons` decision, branch 3), and `UnsupportedOperationException`
  for the `java.util.Map` mutators (embodiment "Method behaviour"). `defrecord` uses the same classes (Clojure 1.11.4
  `core_deftype.clj` lines 137, 145, 244 and 250–253). Change the Scope 3 rule so that it also permits JDK classes
  (for example `java.lang`, `java.util` and `java.io`), and keep the rule against `closed-record.impl.*` and private
  `clojure.core` vars.

## Ambiguity review, pass 4 (2026-10-19)

- [x] `^int` hints in the K-06 test forms (Scope 1). The K-06 bullets name only the field names
  `__meta __extmap __hash __hasheq`. They do not say if `__hash` and `__hasheq` have the `^int` hints of D-01.
  Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9: `[a __meta __extmap __hash __hasheq]` without hints also fails with
  `VerifyError` at `Class/forName` (the alternative constructor pushes int 0 into an `Object` field). With
  `^int __hash ^int __hasheq` the class loads and has 3 constructors. Each of the four three-field layouts with
  `^int` on the hash fields that it has still fails with `VerifyError`. Thus a three-field test without hints gets
  `VerifyError` from the missing hints, not from the field count, and it does not fix the rule. State the field
  metadata of each K-06 test form (the four-field form and the three-field forms). For example: `^int` on each
  `__hash` and `__hasheq` that the form has, as D-01.
- [x] Type hash of `hasheq` (Decisions, V-01 `hash` row). The design and the embodiment say `type-hash XOR
  APersistentMap/mapHasheq` and "as `defrecord`" (D-07), but they do not define the type hash. `defrecord` uses
  `(hash classname)`, where `classname` is the symbol `ns.Name` (with the munged namespace), calculated at expansion
  time (Clojure 1.11.4 `core_deftype.clj` line 163). Other readings (for example the hash of the class object) give
  a different value in each JVM run and break I-05. State the type hash, and the expected value that the V-01 `hash`
  test uses.
- [x] Assertions of the V-10 test (Scope 6). Scope 6 says only "`->Name` and `map->Name` of a type with more than 20
  fields". The "Expansion-time dependencies" decision says that `->Name` throws `ArityException` for a wrong argument
  count. State what the V-10 test checks. For example: all field values in order after `->Name` and `map->Name`, and
  `ArityException` for too few and too many arguments to `->Name`.

## Inconsistency review, pass 4 (2026-10-19)

- [x] `validate-fields` parity. "Expansion-time dependencies" says that the macro reimplements `validate-fields` as
  "the reserved-name check", and then "The results and errors are those of `defrecord`". Clojure 1.11.4
  `validate-fields` (`core_deftype.clj` lines 294–305) has three checks: the field list is not a vector
  (`AssertionError`), reserved names (`AssertionError`), and a field that is not a symbol (`CompilerException`).
  Scope 4, "Allocation of argument processing" and "Argument errors" name only the reserved-name check (and the
  duplicate-field check, which `defrecord` does not have). Make the two statements agree. Either limit the parity
  sentence to the checks that the design names, or state the result of the other two `validate-fields` checks.
  Do not change the scope.
- [x] Decision reference for the `->Name` arity. "Expansion-time dependencies" (`build-positional-factory`) says
  "`ArityException` for a wrong argument count (D-08)". D-08 is about `map->Name` and says nothing about arity.
  The fixed arity of `->Name` is in D-02 ("`->Name` has fixed arity"), and the factories for more than 20 fields are
  in D-15. Change the reference to D-02 (and D-15).

## Ambiguity review, pass 5 (2026-10-19)

- [x] Methods that the embodiment table does not name (Scope 3). Scope 3 says "Each method in the embodiment
  'Method behaviour' table". The table names `valAt k nf` and the `java.util.Map` methods `size`, `get`,
  `containsKey`, `keySet`, `values` and `entrySet`. `defrecord` also generates `valAt k` (one argument), `isEmpty`
  and `containsValue` (Clojure 1.11.4 `core_deftype.clj` `ilookup` and `ijavamap`). The interfaces of D-06 declare
  them. A `deftype*` without them throws `AbstractMethodError` when they are called, for example `(.isEmpty r)` or
  `(get r :a)`. State whether the emitter generates these three methods, and their behaviour. For example: as
  `defrecord`, on the declared entries only (I-09): `valAt k` is `valAt k nil`, `isEmpty` is `(= 0 (count this))`,
  `containsValue` searches the declared values. Use the existing scope items.
- [x] K-06 test "the `deftype` macro rejects these four field names" (Scope 1). The design does not give the test
  forms or the expected result. State if the test has one form for each of the four names or one form with all of
  them, and what it expects. Observed on 1.11.4: `macroexpand-1` of `(deftype T [a <name>])` throws
  `Compiler$CompilerException` with cause `AssertionError` for each of the four names (`validate-fields`). For
  example: one form for each name; the test checks the class of `ex-cause`, as "Argument errors" does.

## Inconsistency review, pass 5 (2026-10-19)

- [x] Constructors of the K-06 layout (Scope 1). The first K-06 bullet says that the compiler generates `create`,
  `getBasis`, "a constructor with the declared fields only and a constructor with all fields". The field-metadata
  bullet of the same scope item says that the four-field form "has 3 public constructors". The third constructor
  takes the declared fields, `__meta` and `__extmap` (observed on 1.11.4, "Design-steps resolution"). `defrecord`
  `assoc` uses it, and the `assoc` decision ("hash caches 0, as `defrecord` does") copies that path. Also,
  `ramora/defclosedrecord/context.md` K-06 says "`create` and the alternative constructors". `deftype*` always
  generates the all-fields constructor, so a test of that constructor does not fix the K-06 rule. Make the first
  K-06 bullet agree with the metadata bullet and with K-06: name the three constructors and state which ones the test
  checks. The embodiment "Generated type" also names only two constructors. Do not edit `ramora/`; record it with the
  open architecture findings.

## Ambiguity review, pass 6 (2026-10-19)

- [x] D-19 case in the V-01 declared-key tests (Scope 6). The purpose table cells "`map->Name`, Declared key" and
  "reader literal, Declared key" are "missing key → nil (D-19)", with the note "a missing primitive-hinted field
  throws a host exception". Scope 6 defines the test unit as one operation in one column. It does not say if the
  test of these two cells also covers a missing primitive-hinted field (for example `^long`), or only the nil result
  of a field with no hint. The `map->Name` decision says "a missing primitive field throws as D-19 says", but no
  test item names it. State if the two tests check the D-19 case, and if they do, the expected class. Observed for
  `defrecord` on 1.11.4, 1.12.6 and 1.13.0-alpha9 (`^long a`): `map->P`, `read-string` and `eval` of
  `(read-string ...)` throw `java.lang.NullPointerException` itself, with no cause. Use the existing scope items.

## Inconsistency review, pass 6 (2026-10-19)

- [x] Unit of the `ex-data` tests. Scope 6 says "Each `ex-info` `:type` of D-03 has a test of the full `ex-data`".
  Acceptance 4 says that the type "has the `ex-data` of the Decisions table for each failure path". The table has 6
  paths and 5 `:type` values: `extra-keys` has two paths (`map->Name` and `check`), and the `undeclared-key` row
  has two operations (`assoc` and `cons`). With one test for each `:type`, the full `ex-data` of `map->Name` (or
  of `check`) and of `cons` (or of `assoc`) has no test, but acceptance 4 requires it. Make Scope 6 and
  acceptance 4 agree. For example: Scope 6 says one test of the full `ex-data` for each path of the table, and for
  `assoc` and `cons` separately. The V-01 and V-15 tests of these paths can be those tests. Use the existing scope
  items.

## Ambiguity review, pass 7 (2026-10-19)

- [x] Forms of the expansion other than the type and the factories (Scope 3, Scope 4). Concepts "Emitter" says
  only that the emitter returns "the form that defines the type and its factories". `defrecord` (Clojure 1.11.4
  `core_deftype.clj` lines 388–404) also expands to: `declare` of `->Name` and `map->Name` before `deftype*`,
  `(import classname)` after it, and `classname` as the last form. User code sees each of them (D-15, D-20).
  Observed on 1.11.4, ns `my-ns`: `(defrecord P [a] ...)` returns the class `my_ns.P`. After it, `(resolve 'P)` is
  the class. A method body that calls `map->P` compiles and works. State whether the `defclosedrecord` expansion
  does the same (the `declare`s, the import and the return value), and in which namespace each form is made
  ("Allocation of argument processing"). For example: as `defrecord`, all in the emitter form, because `import`
  and the class are JVM-specific. Use the existing scope items.

## Inconsistency review, pass 7 (2026-10-19)

- [x] `getBasis` in the K-06 test (Scope 1). The first K-06 bullet says that with the four fields at the end "the
  compiler generates `create`, `getBasis` and 3 public constructors", and the test checks that `getBasis` exists.
  The same bullet does not check the all-fields constructor, because `deftype*` always generates it, so it does not
  fix the rule. E-02 (`ramora/defclosedrecord/context.md`) says "`getBasis` always. `create` only with dropped
  fields". `Compiler.java` `emitStatics` (1.13.0-alpha6) emits `getBasis` for each `deftype`. Observed on 1.11.4:
  `(deftype Q [a])` has `getBasis` and no `create`. Thus the `getBasis` check also does not fix the rule. Make the
  bullet agree with E-02 and with its own reason. For example: name `getBasis` as generated for every `deftype*`,
  and check only `create` and the two alternative constructors. Or keep the `getBasis` check and state that it is
  not part of the rule.

## Plan ambiguity review (2026-10-19)

Scope: `plan.md` and `steps.md`. A resolution can change `design.md` (Scope 1, the Decisions), `plan.md` or
`steps.md`.

- [x] Class loader of the K-06 three-field test (Scope 1, Slice 1). Scope 1 and `steps.md` say
  `(Class/forName class-name true (clojure.lang.RT/baseLoader))`. The result of `RT/baseLoader` depends on the run
  context. In the scry CLI, `Compiler/LOADER` is not bound in a test, so `RT/baseLoader` is the application class
  loader. Observed with `clojure -M:test -m scry.cli` on 1.11.4, 1.12.6 and 1.13.0-alpha9: after `eval` of a
  three-field form, this call throws `ClassNotFoundException`, not `VerifyError`. Thus the test fails in the CLI
  (acceptance 2), and the "Order" decision then stops the task for a false rule failure. Observed in the same CLI
  context: `(Class/forName class-name true (clojure.lang.DynamicClassLoader.))` throws `VerifyError` with no cause
  for a three-field form, and returns the class for the four-field form (the `DynamicClassLoader` class cache is
  static). `eval` of a `deftype*` form returns nil, so it does not give the class. State the class loader (or the
  lookup) that each K-06 test uses to get the class, for the CLI and the REPL.
- [x] Assertions of the K-08 `emitValue` test (Scope 1, Slice 1). `steps.md` says "a record constant without
  `IType` is built through `create` from the `java.util.Map` view (`Compiler.emitValue` IType-before-IRecord path)".
  It does not say how the test makes a constant or what it checks. The design reviews left this to the plan
  (`implementation.md`, ambiguity review pass 4: "test method, not design"). `__extmap` does not show the path: the
  `IType` path also gives `__extmap` nil. Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9: two `deftype*` forms with
  the K-06 layout, `IRecord`, `IPersistentMap` and `java.util.Map`, where `entrySet` gives `:a 99` and the field `a`
  is 1. Constant made with `((eval (list 'fn [] v)))`. Without `IType`, the constant has `a` 99 (`create` from the
  `java.util.Map` view). With `IType` added, it has `a` 1 (the `IType` path). State the constant construction and
  the assertions of each K-08 test. Also point the K-07 "no calls" step to the observed method (the atom that
  records calls, `implementation.md` ambiguity review pass 4).
- [x] ClojureScript branch of `core.cljc` and clj-kondo (Slices 2 and 4, acceptance 7). clj-kondo lints a `.cljc`
  file for `:clj` and `:cljs`. `steps.md` puts `defclosedrecord` and `check` in `core.cljc` and requires
  `closed-record.impl.jvm` only under `:clj`. It does not say what these forms are in the `:cljs` branch. Observed
  with the local clj-kondo on a `.cljc` file of this shape: `(class x)` in a function gives "error: Unresolved symbol:
  class" (cljs), and a macro body that calls a namespace required only under `:clj` gives "warning: Unresolved
  namespace". ClojureScript behaviour is out of scope, but D-18 scope is "all hosts". State the `:cljs` content of
  `core.cljc`. For example: `ClosedRecord` and `check` portable, with `:value-class` from a reader conditional, and
  the macro body (or the whole macro) under `:clj` only.

## Plan inconsistency review (2026-10-19)

Scope: `plan.md` and `steps.md` against `design.md` and `implementation.md`.

- [x] Open architecture findings have no plan step. `implementation.md` ("Notes after design review, pass 8") says:
  show the open architecture findings (passes 1–7) to the user together, before or at task close, and do not edit
  `ramora/` or `mementum/memories/deftype-star-hidden-fields.md` without approval. Out of scope ("Changes to the
  architecture documents") also says "ask the user". `plan.md` and `steps.md` have no item for this, and Slice 6
  edits `mementum/` files. Add a step (for example at the end of Slice 7) that collects the findings and asks the
  user, and state in Slice 6 that the memory file is not changed without approval.
- [x] Time of the clj-kondo check. The `plan.md` risk says "verify early, not at the end" for the clj-kondo
  zero-warning criterion (`:lint-as` and quoted `deftype*` forms). `steps.md` runs `clj-kondo --lint src test` only
  in Slice 7. Make them agree. For example: run clj-kondo at the end of Slice 1 (quoted forms), Slice 4
  (`core.cljc`, both `:clj` and `:cljs`) and Slice 5 (`:lint-as` for `->Name` and `map->Name`), or remove the risk
  text.
- [x] Mutable-metadata forms of acceptance 5. Acceptance 5 names two forms: a `^:volatile-mutable` field and an
  `^:unsynchronized-mutable` field. The Slice 5 step says only "mutable field metadata", so one test can satisfy the
  step but not acceptance 5. Name both forms in the Slice 5 step (the cause class is `IllegalArgumentException` for
  each).

## Plan ambiguity review, pass 2 (2026-10-19)

Scope: `plan.md` and `steps.md`. A resolution can change `plan.md`, `steps.md` or `design.md` (acceptance 5).

- [x] Namespace of the quoted forms that tests give to `macroexpand-1` and `eval` (Slices 1 and 5, acceptance 5).
  In the scry CLI, `*ns*` is `user` when a test runs, not the test namespace. Observed with
  `clojure -M:test -m scry.cli` on 1.11.4, 1.12.6 and 1.13.0-alpha9: `(macroexpand-1 '(m [__meta]))` of a macro `m`
  of the test namespace returns the form unchanged (no throw). With `'(k.n-test/m [__meta])` or a syntax-quoted
  form, the `AssertionError` cause occurs. A quoted `deftype*` method body that refers to a var of the test namespace
  by an unqualified symbol fails at `eval` with `CompilerException` "Unable to resolve symbol". In the REPL, `*ns*`
  can be the test namespace, so the same test can pass there. Thus:
  - The Slice 5 expansion-time error tests, written as acceptance 5 writes them
    (`(macroexpand-1 '(defclosedrecord T [__meta]))`), get no exception in the CLI and fail.
  - The Slice 1 K-07 step says "an atom (held by a var of the test namespace)", but not how the quoted method body
    names the var.

  State how these test forms name the macro and the var. For example: a fully qualified symbol
  (`closed-record.core/defclosedrecord`, `<test-ns>/calls`), or `(binding [*ns* (the-ns '<test-ns>)] ...)`. With the
  `binding`, the emitter also takes `classname` and the protocol symbols from the test namespace. A syntax-quoted form
  also qualifies the type name and the field names, so it is not the simple choice. If the fix changes the form of
  acceptance 5, change acceptance 5 in `design.md` too.
- [x] Wrong-arity `->Name` calls of the V-10 test and clj-kondo (Slice 5, acceptance 7). The V-10 step calls `->Name`
  with 21 and 23 arguments. With `:lint-as` `defrecord` (Scope 9), clj-kondo knows the arity of `->Name`. Observed
  with clj-kondo v2026.04.15: a direct call of `->P` (3 fields) with 2 or 4 arguments gives "error: x.t/->P is called
  with 2 args but expects 3", also for a `defrecord`. `(apply ->P [1 2])` gives no finding. Thus a direct call fails
  the Slice 5 and Slice 7 clj-kondo checks. State how the V-10 test makes the wrong-arity calls. For example: `apply`
  with an argument vector of 21 and 23 values. Scope 1 rejects `#_:clj-kondo/ignore` only for the `deftype*` forms.

## Plan inconsistency review, pass 2 (2026-10-19)

Scope: `plan.md` and `steps.md` against `design.md` and `implementation.md`.

- [x] List of the `ex-data` path tests. `implementation.md` ("Pass-6 design-steps resolution") says that each of
  the 7 "Error data" paths needs at least one test with `=` on the full `ex-data`, and "List the test of each path
  with the V-01 mapping (acceptance 3)". `steps.md` Slice 5 ("Record the V-01 test-unit-to-test mapping") and
  `plan.md` Slice order 5 name only the V-01 mapping. Scope 6 lets a V-01 or V-15 test be the test of a path, so
  without the list a review of acceptance 4 cannot find the test of each path. Make them agree. For example: the
  Slice 5 mapping step also lists the test of each of the 7 paths (`assoc` and `cons` separate). Or state in
  `implementation.md` that the list is not necessary.
