# 002 Compiler rules and JVM emitter

## Intent

Make `defclosedrecord` work on the JVM. A user must be able to define a closed record type, construct it, use it as
a map and find extra literal keys with `check`. First, fix in tests the compiler rules that the type depends on. A
change in Clojure then causes a failure in a known test, not in the behaviour of the type.

## Problem

The repository has a project skeleton and the architecture in `ramora/defclosedrecord/`. It has no implementation.

The type depends on facts about the Clojure compiler that are not public API (K-06 to K-08). The architecture
observed these facts in temporary tests on Clojure 1.11.4 and 1.13.0-alpha6. The design review checked some facts
again on 1.12.6 and 1.13.0-alpha9 (Scope 1, "Observed facts" and the Decisions). No test in the repository fixes
them. If a rule fails on a version, an emitter test fails with a symptom that does not show the cause. Thus the rule
tests must exist before the emitter, and independently of it.

## Context

- Architecture: `ramora/defclosedrecord.md`, then `ramora/defclosedrecord/`:
  - [embodiment](../../../ramora/defclosedrecord/embodiment.md): artifacts, boundaries, generated type, method
    behaviour, state, allocation. This is the specification of the emitter.
  - [context](../../../ramora/defclosedrecord/context.md): K-06 to K-08 and evidence E-01 to E-07.
  - [purpose](../../../ramora/defclosedrecord/purpose.md#behaviour): the behaviour tables.
  - [decisions](../../../ramora/defclosedrecord/decisions.md): D-01 to D-20.
  - [invariants](../../../ramora/defclosedrecord/invariants.md): I-01 to I-09.
  - [qualities](../../../ramora/defclosedrecord/qualities.md#verification-plan): V-01 to V-15.
- Memory: `mementum/memories/deftype-star-hidden-fields.md` (the K-06 to K-08 rules in short form).
- Tooling: `mementum/knowledge/dev-tooling.md` and `README.md` "Development". Test runner: scry.
- Task 001 (closed) made `deps.edn`, the aliases `:test`, `:nrepl`, `:clj-1.12`, `:clj-1.13`, the empty namespace
  `closed-record.core` in `src/closed_record/core.cljc` and a smoke test in `test/closed_record/core_test.clj`.

### Observed facts (2026-10-19)

Observed on Clojure 1.11.4. A fact that names other versions was also observed on them.

- `defrecord` does not implement `clojure.lang.IType`.
- `defrecord` with 22 fields compiles. `->Name` accepts 22 arguments.
- `conj` onto a `defrecord` (`imap-cons`, also on 1.12.6 and 1.13.0-alpha9): `[:a 9 9]` → assoc `:a 9`; `[:a]` →
  `IndexOutOfBoundsException`; `:x` → `IllegalArgumentException`; `'([:a 3])` → `ClassCastException`; nil → the
  record unchanged; a map, another record or a seq of map entries → assoc of each entry.
- `(.getLookupThunk r :undeclared)` of a `defrecord` returns nil.

## Concepts

- Compiler rule: a fact about `deftype*`, `create`, the reader or `Compiler.emitValue` that the type depends on.
  K-06 to K-08 name them.
- Emitter: a function that receives the `defclosedrecord` arguments after the macro steps of "Allocation of argument
  processing" (Decisions) and returns the expansion form ("Expansion form", Decisions). This form defines the type
  and its factories. It runs at macro expansion time. On the JVM, it is in `closed-record.impl.jvm`.
- Generated type: the class that the emitter form defines. Its fields, interfaces and methods are in
  [embodiment](../../../ramora/defclosedrecord/embodiment.md#generated-type).
- `D(T)`: the declared keys of type `T`.

## Scope

In scope:

1. Compiler rule tests (V-13) in `test/closed_record/compiler_rules_test.clj`. They use only `defrecord`, `deftype`,
   `deftype*`, the reader and the compiler. They do not use closed-record code. They fix these rules:
   - K-06: with the fields `__meta __extmap __hash __hasheq` at the end, the compiler generates the static method
     `create` and 3 public constructors:
     - all fields (`deftype*` always generates it),
     - the declared fields, `__meta` and `__extmap` (alternative constructor, used by `assoc` of a declared key),
     - the declared fields only (alternative constructor, used by positional literals and `->Name`).

     The four-field form has one declared field `a` and `:implements [clojure.lang.IRecord]`. The test checks that
     the static method `create` exists and that the two alternative constructors exist, by their parameter types
     (`.getConstructors` and `.getParameterTypes`): `[Object Object Object]` and `[Object]` (`java.lang.Object`).
     The all-fields constructor and the static method `getBasis` do not fix the rule, so the test does not check
     them. The compiler generates `getBasis` for each `deftype*` (E-02). Observed on 1.11.4, 1.12.6 and
     1.13.0-alpha9: `(deftype Q [a])` has `getBasis` and no `create`.
   - K-06: with three of these four fields, `eval` of the `deftype*` form returns, and the class fails JVM
     verification. The test covers all four three-field layouts. Each layout has one declared field,
     `:implements [clojure.lang.IRecord]` and its own class name. For each layout, the test `eval`s the form, then
     calls `(Class/forName class-name true (clojure.lang.RT/baseLoader))`. It expects `java.lang.VerifyError`
     itself, not a wrapper or a cause (observed on 1.11.4, 1.12.6 and 1.13.0-alpha9: no cause).
   - Field metadata of the K-06 `deftype*` forms (the four-field form and each three-field form): each `__hash`
     and `__hasheq` that the form has gets the D-01 metadata `^int ^:unsynchronized-mutable`. The declared fields
     have no metadata. Reason: without `^int`, the four-field form also fails with `VerifyError` (the alternative
     constructor puts int 0 into an `Object` field), so a three-field test without `^int` does not fix the rule.
     Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9: the four-field form with this metadata loads and has the 3
     public constructors of the first K-06 bullet. Each three-field form with this metadata throws `VerifyError`
     with no cause.
   - K-06: the `deftype` macro rejects these four field names. The test has one form for each name:
     `(macroexpand-1 '(deftype T [a <name>]))`. For each form, it expects an exception and checks that the class of
     its `ex-cause` is `AssertionError` (`validate-fields`). It does not check the wrapper
     (`clojure.lang.Compiler$CompilerException`), as "Argument errors" says. Observed on 1.11.4, 1.12.6 and
     1.13.0-alpha9.
   - K-07: `create` puts the extra keys of a map literal into `__extmap`, a missing key gives nil, and `create` does
     not call methods of the type (for example `assoc`).
   - K-08: `defrecord` does not implement `IType`. `Compiler.emitValue` uses the `IType` path before the `IRecord`
     path. A record constant without `IType` is built through `create` from the `java.util.Map` view.

   Each `deftype*` form in the tests (also in the K-08 tests) is quoted data that the test gives to `eval`. clj-kondo
   does not lint quoted forms, so acceptance 7 holds without hooks, `#_:clj-kondo/ignore` or test configuration.
2. Internal protocol `ClosedRecord` with the method `-extra-keys` in `closed-record.core` (D-17).
3. JVM emitter in `src/closed_record/impl/jvm.clj`. It emits the generated type as the embodiment specifies:
   - The `deftype*` field layout of D-01. The declared fields keep their metadata (for example type hints), as in
     `defrecord`. The emitter rejects declared fields with `:volatile-mutable` or `:unsynchronized-mutable` metadata,
     as `defrecord` does, thus the declared fields are immutable.
   - The emitter steps of "Allocation of argument processing" (Decisions).
   - The expansion form of the Decisions ("Expansion form"): the `declare`s, the type, the `import`, the factories
     and the class as the value.
   - The interfaces of D-06 and the `ClosedRecord` protocol interface. Not `IType`.
   - Each method in the embodiment "Method behaviour" table, and `valAt k`, `isEmpty` and `containsValue`
     ("Methods not in the embodiment table", Decisions).
   - The user protocol and interface implementations, as `defrecord` accepts them (D-15).
   - The factories `->Name` and `map->Name`, also for more than 20 fields (D-08, D-15).
   - The emitter does not require `closed-record.core`. It emits the protocol only as a symbol.
   - The generated code calls no function in `closed-record.impl.*` and no private `clojure.core` var. Apart from the
     generated type and the names in the user arguments (field metadata, protocols, interfaces and method bodies), it
     refers only to `closed-record.core/ClosedRecord`, `clojure.lang`, public `clojure.core` vars and the JDK packages
     `java.lang`, `java.util` and `java.io`. Examples of JDK classes: `java.util.Map` and `java.io.Serializable`
     (D-06), `java.util.Map$Entry` (`cons`) and `UnsupportedOperationException` (the `java.util.Map` mutators).
     `defrecord` uses the same classes. The emitter emits inline the `ex-info` construction (D-03), the `cons` entry
     loop and the extra-key check of `map->Name`. Example: `defrecord` `cons` calls the private
     `clojure.core/imap-cons` through `(var imap-cons)`. The emitter emits an equivalent loop in the `cons` method.
4. Macro `defclosedrecord` in `closed-record.core`. It has the `defrecord` syntax (D-15). At expansion time it
   rejects the reserved field names `__meta __extmap __hash __hasheq` and duplicate field names. Then it splits the
   options and the specs, calls the JVM emitter and returns the emitter form with no change. The emitter rejects declared fields with mutable metadata and
   unsupported options (Scope 3). The Decisions give the allocation of these steps ("Allocation of argument
   processing") and the errors ("Argument errors").
5. Function `check` in `closed-record.core` (D-09, D-18).
6. Tests:
   - V-01: the behaviour tables in [purpose](../../../ramora/defclosedrecord/purpose.md#behaviour), including the
     equality table and the reader literal row. The test unit is one operation in one column. Each operation that a
     row names (for example `assoc`, `conj`, `merge`, `into` and `update`) has a test for each column. The equality
     table has one column, thus its unit is the row. Exceptions:
     - `empty` takes no key. One test covers its row.
     - `.field` exists only for a declared field. It has a test only in the "Declared key" column.
     - Reader literal row, "Undeclared key" column ("read, hidden, `check` throws"): the test reads a literal with
       an extra key with `read-string`. It checks that the result is an instance of the type, that `get` and the
       keyword call of the extra key give nil, that `contains?` of the extra key gives false, and that `check` throws
       `extra-keys` with the full `ex-data`. These assertions are "hidden" for V-01. The other views of hidden keys
       (`count`, `seq`, `=`, hash, print, `java.util.Map` views) are V-14 (Adjacent work).
     - `map->Name` and reader literal rows, "Declared key" column ("missing key → nil (D-19)"): the test type has a
       field with no type hint and a `^long` field. The test of each row checks two cases. A map or literal without
       the key of the field with no hint (and with the `^long` key) gives nil for that field. A map or literal without
       the `^long` key throws `java.lang.NullPointerException` itself, not a wrapper or a cause (D-19, D-03 scope:
       host exception, no `ex-data`). The reader test reads the literal with `read-string`. Observed for `defrecord`
       on 1.11.4, 1.12.6 and 1.13.0-alpha9: `map->Name` and `read-string` throw `NullPointerException` with no cause.
   - V-10: `->Name` and `map->Name` of a type with more than 20 fields (for example 22). The test checks that
     `->Name` with one argument for each field and `map->Name` with a map of all the declared keys give each field
     value in field order. It checks that `->Name` with one argument too few and with one argument too many throws
     `clojure.lang.ArityException`.
   - V-15: `check` of a closed record, of a record from a literal with extra keys and of other values (including nil).
   - The expansion-time argument checks of the macro and the emitter, through the macro: reserved names, duplicate
     fields, mutable field metadata and unsupported options.
   - `ex-data` (D-03): each path of the Decisions "Error data" table has a test that checks that the `ex-data` is
     equal to the map of the table. `assoc` and `cons` of an undeclared key are separate paths. Thus the table has 7
     paths for 5 `:type` values. A V-01 or V-15 test that reaches a path can be its test (for example the `conj` test
     for `cons`, and the V-15 tests for the two `check` paths).
7. Remove the smoke test in `test/closed_record/core_test.clj`. The tests of this task replace it.
8. Docstrings for `defclosedrecord` and `check`.
9. `.clj-kondo/config.edn` with `:lint-as {closed-record.core/defclosedrecord clojure.core/defrecord}`. Without it,
   clj-kondo reports the generated `->Name` and `map->Name` as unresolved symbols.
10. Update `mementum/state.md` and `mementum/knowledge/dev-tooling.md`. In `dev-tooling.md`, change the two places
    that describe the smoke test:
    - The "Scry" bullet: remove the sentences about the smoke test in `core_test.clj`. The rule "The scry CLI exits
      non-zero when it finds zero tests" stays, because it is still true.
    - The section "Smoke-test lint": remove it.

## Out of scope

- The verification plan items other than V-01, V-10, V-13 and V-15.
- Benchmarks and criterium (V-11).
- ClojureScript and babashka emitters. The behaviour of `defclosedrecord` on these hosts is not specified.
- Value validation (D-11) and a switch that disables the checks (D-12).
- User methods that replace generated methods (for example a user `equals`). The task supports only what
  `defrecord` supports.
- README usage documentation for the public API.
- clj-kondo hooks, and clj-kondo configuration for users of the library (`clj-kondo.exports`).
- CI configuration.
- Changes to the architecture documents. If the implementation finds an error in them, record it in
  `implementation.md` and ask the user.

## Adjacent work

Later tasks own this work. This task must not block it:

- Differential tests against `defrecord` (V-02) and property tests (V-03).
- Literal round trip, code constant, keyword thunk, malli, nil, type hint, equality and hidden-key tests (V-04 to
  V-09, V-12, V-14).
- Benchmarks (V-11).
- README usage section and docs for the closed-map cases (malli transformers, `:db/id`).
- clj-kondo hook and exported configuration, CI matrix, ClojureScript emitter, babashka spike (R-02).

## Approaches

- Emit `deftype*` directly from a closed-record emitter (selected, D-01). All methods are generated by closed-record.
  The closed checks are in the `case` default branches, so the declared-key paths have no extra cost (D-02, K-05).
- Call the private `clojure.core/emit-defrecord` and replace the generated methods. Rejected: almost every method of
  `defrecord` reads `__extmap` (`valAt`, `count`, `seq`, `assoc`, `without`, `equiv`, the `java.util.Map` views), so
  most methods must be replaced. The result depends on a private function that can change in each Clojure version.
- Wrap `defrecord`. Rejected in D-01: the user cannot replace `assoc` and `without`.

## Decisions

- Order: the compiler rule tests come first. If a rule does not hold on Clojure 1.11.4, 1.12.6 or 1.13.0-alpha9, stop,
  record the result in `implementation.md` and ask the user. Do not change the design to work around it.
- The compiler rule tests do not use closed-record code. A failure in them shows a change in Clojure, not an error in
  closed-record.
- The emitter tests use the public macro and the generated type. They do not compare generated forms. The form is an
  internal detail (D-13: low commitment for `impl`).
- Emitter interface: one public function in `closed-record.impl.jvm` that the macro calls after its steps of
  "Allocation of argument processing" and that returns the expansion form ("Expansion form"). The arguments are the
  type name, the field vector with its metadata, the option map and the specs. The function name and the argument
  shape are internal.
- Allocation of argument processing (D-13, K-02). `core.cljc` contains the public API and the host-neutral steps.
  `closed-record.impl.jvm` contains the JVM steps. The macro steps come before the emitter steps. All steps run at
  expansion time.

  - Macro, `closed-record.core`:
    - Reserved field names (`defrecord`: `validate-fields`).
    - Duplicate field names.
    - Split of the options and the specs: an option map, then each protocol, interface or `Object` symbol with its
      method forms (`defrecord`: `parse-opts`, `parse-impls`). This step does not check the option keys or the
      symbols.
  - Emitter, `closed-record.impl.jvm`:
    - `:volatile-mutable` or `:unsynchronized-mutable` metadata on a declared field (`defrecord`: `emit-defrecord`).
      These keys are JVM `deftype*` keys.
    - Option keys: `:no-print` and `:load-ns` go to `deftype*`. Another key throws.
    - Symbol to interface: a symbol that resolves to a var gives `(:on @var)`. `Object` and `java.lang.Object` are not
      interfaces. Their methods stay.
    - Destructured method arguments (`defrecord`: `maybe-destructured`).
    - `->Name` (`defrecord`: `build-positional-factory`) and `map->Name`.
    - The expansion form ("Expansion form").
- Expansion form. The emitter returns the full expansion, in the shape of the `defrecord` expansion (Clojure 1.11.4
  `core_deftype.clj` lines 394–404, the same in 1.12.6 and 1.13.0-alpha9):

  ```clojure
  (let []
    (declare ->Name)
    (declare map->Name)
    (deftype* ...)
    (import classname)
    (defn ->Name ...)
    (defn map->Name ...)
    classname)
  ```

  `classname` is the symbol of the "Type hash" decision. Thus, as for `defrecord`:
  - A method body can call `->Name` and `map->Name`.
  - After the form, `Name` resolves to the class in the namespace of the form.
  - The value of the form is the class.

  Observed for `defrecord` on 1.11.4, 1.12.6 and 1.13.0-alpha9, ns `my-ns`: `(defrecord P [a] ...)` returns the
  class `my_ns.P`, `(resolve 'P)` gives the class, and a protocol method body that calls `map->P` works. The emitter
  makes all these forms, because `import` and the class are JVM-specific (D-13, K-02). The macro returns the emitter
  form with no change. `let`, `declare`, `import` and `defn` are public `clojure.core` (Scope 3). Scope 9
  (`:lint-as` `defrecord`) makes clj-kondo expect the same result. The factory docstrings and `:arglists` are free.
  No Scope 6 test checks the `declare`s, the `import` or the value directly. The tests that use `Name`, `->Name` and
  `map->Name` depend on them.
- `core.cljc` requires `closed-record.impl.jvm` only in a `:clj` reader conditional. `impl.jvm` does not require
  `core` (embodiment, boundaries).
- Error data (D-03). The emitter and `check` use exactly these maps:

  | Path | `ex-data` |
  |---|---|
  | `assoc` or `cons` of an undeclared key | `{:type :closed-record/undeclared-key, :record T, :key k}` |
  | `dissoc` of a declared key | `{:type :closed-record/declared-key-dissoc, :record T, :key k}` |
  | `empty` | `{:type :closed-record/empty, :record T}` |
  | `map->Name` with extra keys | `{:type :closed-record/extra-keys, :record T, :keys ks}` |
  | `check` of a record with extra literal keys | `{:type :closed-record/extra-keys, :record T, :keys ks}` |
  | `check` of another value | `{:type :closed-record/not-closed-record, :value-class (class x)}` |

  `T` is the class of the generated type. `ks` is a set of all the extra keys. For nil, `:value-class` is nil.
  Message content for each row:
  - `undeclared-key` and `declared-key-dissoc`: the class name of `T` and the key.
  - `empty`: the class name of `T`.
  - `extra-keys`: the class name of `T` and the keys.
  - `not-closed-record`: the value class (nil for nil).

  The other words of the message are free. The message is not public API. The tests check the `ex-data`, not the
  message.
- Argument errors. The macro or the emitter ("Allocation of argument processing") throws a host exception at
  expansion time (D-03 scope: other failures keep the host exception). The classes are the `defrecord` classes:
  - Reserved field name: `AssertionError`, as `defrecord` does.
  - Duplicate field name: `AssertionError`. `defrecord` has no expansion-time check for this. Its `deftype*` fails at
    compile time with `ClassFormatError`. The macro uses the class of the `defrecord` field-vector checks
    (`validate-fields`).
  - `:volatile-mutable` or `:unsynchronized-mutable` metadata on a declared field: `IllegalArgumentException`, as
    `defrecord` does.
  - An option other than `:no-print` and `:load-ns`: `IllegalArgumentException`, as `defrecord` does.

  These errors have no `ex-data`. They are not public API (embodiment "Boundaries"). The message text is free.
  `macroexpand-1` and `eval` wrap the error in `clojure.lang.Compiler$CompilerException`. The tests check the class
  of the cause (`ex-cause`), not the wrapper.
- `assoc` of a declared key makes a new instance with the constructor of the declared fields, `__meta` and
  `__extmap` (K-06, Scope 1). It copies `__meta` and `__extmap`. The new instance has hash caches 0, as `defrecord`
  does.
- Methods not in the embodiment table. `defrecord` also generates `valAt k`, `isEmpty` and `containsValue` (Clojure
  1.11.4 `core_deftype.clj` lines 193, 247 and 248). The interfaces of D-06 declare them. Without them, a call
  throws `AbstractMethodError`. The emitter generates them with the `defrecord` bodies:
  - `valAt k`: `(.valAt this k nil)`. `get` and the keyword call use it.
  - `isEmpty`: `(= 0 (.count this))`.
  - `containsValue v`: `(boolean (some #{v} (vals this)))`.

  `count` and `seq` give the declared entries only, thus these methods see the declared entries only (I-09). As in
  `defrecord`, `containsValue` of nil or false gives false, also if a field has that value (observed on 1.11.4).
- `withMeta` makes a new instance with the new metadata. It copies `__extmap` and the hash caches `__hash` and
  `__hasheq`, as `defrecord` does. Metadata has no effect on the hash (D-07), thus the copied caches stay correct.
- Expansion-time dependencies. The macro and the emitter call no private `clojure.core` var, as the generated code
  does not (Scope 3). The reason is the reason of "Approaches": a private function can change in each Clojure
  version, and V-13 gives a signal only for K-06 to K-08. They reimplement with public functions these private
  `defrecord` steps (Clojure 1.11.4 `core_deftype.clj` and `core.clj`, the same in 1.12.6 and 1.13.0-alpha9).
  "Allocation of argument processing" gives the namespace of each step.
  - `validate-fields`: the reserved-name check (Scope 4).
  - `parse-opts+specs` (with `parse-opts` and `parse-impls`): the options, then each protocol or interface symbol
    with its methods. A symbol that resolves to a var gives the protocol interface (`:on` of the var value).
    `Object` and `java.lang.Object` are not interfaces. The options `:no-print` and `:load-ns` go to `deftype*`.
    Another option throws ("Argument errors").
  - `maybe-destructured`: a method argument that is not a symbol becomes a generated symbol, and a `let` in the
    method body destructures it.
  - `build-positional-factory`: `->Name` for more than 20 fields, with `ArityException` for a wrong argument count
    (D-02: fixed arity, D-15: more than 20 fields).

  The results of these steps, and the `ArityException` of `->Name`, are those of `defrecord`. The expansion-time
  errors are those of "Argument errors". This task does not reimplement the two other `validate-fields` checks (a
  field list that is not a vector, a field that is not a symbol). The result of these two cases is not specified,
  and no test checks it.
- `cons` copies the three branches of `defrecord` `imap-cons` (the same in 1.11.4, 1.12.6 and 1.13.0-alpha9). In
  the embodiment `cons` row ("map entry, vector pair or map"), "vector pair" is any vector and "map" is any seqable
  of map entries. The emitter uses these branches, in this order:
  1. A map entry (`map-entry?`): `assoc` once.
  2. Any `IPersistentVector`: `assoc` of `(nth v 0)` and `(nth v 1)`, with no count check. Thus `[:a 9 9]` gives
     `assoc` of `:a` and 9, and `[:a]` throws `IndexOutOfBoundsException` before `assoc`.
  3. Any other value: `assoc` of each element of `(seq o)`, in seq order. Each element must be a
     `java.util.Map$Entry`. Thus a map, another record or a seq of map entries gives `assoc` for each entry, and nil
     gives the record unchanged. A value that `seq` does not accept (for example `:x`) throws
     `IllegalArgumentException`. An element that is not a map entry (for example in `'([:a 3])`) throws
     `ClassCastException`. The loop stops at the first element that throws.

  The `IndexOutOfBoundsException`, `IllegalArgumentException` and `ClassCastException` are host exceptions (D-03
  scope). An undeclared key throws `undeclared-key`. In branches 1 and 2, `:key` is the key of the pair. In branch
  3, `:key` is the first undeclared key in seq order. `merge` calls `cons` with the map. `into` calls `cons` with each
  element. In both cases `:key` is the first undeclared key in the seq order of the argument.
- Type hash (D-07). The type hash is `(hash classname)`, as `defrecord`. `classname` is the symbol of the class
  name, `(symbol (str (namespace-munge *ns*) "." Name))`, for example `my_ns.Name` for the type `Name` in the
  namespace `my-ns`. The emitter calculates it at expansion time and emits it as a constant. Thus it does not change
  between JVM runs, and it is the value of `defrecord` for the same class name (I-05). `hasheq` is
  `(int (bit-xor type-hash (clojure.lang.APersistentMap/mapHasheq this)))`. The V-01 `hash` test expects
  `(int (bit-xor (hash 'my_ns.Name) (clojure.lang.APersistentMap/mapHasheq r)))` for a record `r` of type
  `my_ns.Name`. The test writes the class name of its type as a literal symbol (observed on 1.11.4 for `defrecord`).
- `getLookupThunk` of an undeclared key returns nil, as `defrecord` does. The keyword call site then calls `valAt`.
- `map->Name` takes the arguments that `defrecord` `map->Name` takes (Clojure 1.11.4 `core_deftype.clj` lines
  400–403). If the argument is a `clojure.lang.MapEquivalence`, it uses the argument. Else it uses `(into {} m)`.
  Thus nil gives no keys, and a `java.util.Map`, a record or a seqable of map entries or vector pairs gives its
  entries. A value that `into` does not accept (for example `:x`) throws the host exception
  (`IllegalArgumentException`). The extra-key check uses the keys of this converted map. If a key is not in `D(T)`, it
  throws `extra-keys` with all the extra keys. Then it calls `Name/create` with the converted map. Thus a missing key
  gives nil, a missing primitive field throws as D-19 says, and the result never has `__extmap` entries.

## Acceptance criteria

1. `test/closed_record/compiler_rules_test.clj` has a test for each rule in Scope 1. It requires no `closed-record`
   namespace.
2. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors.
3. The tests of Scope 6 exist. Each V-01 test unit of Scope 6 maps to one or more tests. `implementation.md` lists
   the mapping.
4. A type defined with `defclosedrecord`:
   - `record?` is true. `(instance? clojure.lang.IType r)` is false.
   - Prints as `#ns.Name{...}`.
   - Has the `ex-data` of the Decisions "Error data" table for each failure path. `assoc` and `cons` are separate
     paths (Scope 6).
5. `(macroexpand-1 '(defclosedrecord T [__meta]))`, a form with a duplicate field, a form with a `^:volatile-mutable`
   field, a form with an `^:unsynchronized-mutable` field and a form with an unsupported option throw at expansion
   time. The cause has the class that
   the Decisions give.
6. `src/closed_record/impl/jvm.clj` does not require `closed-record.core`. The only source namespaces are
   `closed-record.core` and `closed-record.impl.jvm`.
7. `clj-kondo --lint src test` reports no errors and no warnings.
8. `cljfmt check src test` passes.
9. In the psi project REPL, `(:pass? (scry/run))` is `true`.
10. `test/closed_record/core_test.clj` has no smoke test. `mementum/state.md` names `defclosedrecord`, `check` and
    the test files. `mementum/knowledge/dev-tooling.md` does not say that a smoke test exists.
