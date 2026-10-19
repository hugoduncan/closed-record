# 002 Compiler rules and JVM emitter

## Intent

Make `defclosedrecord` work on the JVM. A user must be able to define a closed record type, construct it, use it as
a map and find extra literal keys with `check`. First, fix in tests the compiler rules that the type depends on. A
change in Clojure then causes a failure in a known test, not in the behaviour of the type.

## Problem

The repository has a project skeleton and the architecture in `ramora/defclosedrecord/`. It has no implementation.

The type depends on facts about the Clojure compiler that are not public API (K-06 to K-08). These facts were
observed in temporary tests on Clojure 1.11.4 and 1.13.0-alpha6. Those tests are not in the repository. Clojure
1.12.6 and 1.13.0-alpha9 were not tested. If a rule fails on a version, an emitter test fails with a symptom that does
not show the cause. Thus the rule tests must exist before the emitter, and independently of it.

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

### Observed facts (2026-10-19, Clojure 1.11.4)

- `defrecord` does not implement `clojure.lang.IType`.
- `defrecord` with 22 fields compiles. `->Name` accepts 22 arguments.
- `conj` of a value that is not a map entry, a vector pair or a map onto a `defrecord` throws
  `IllegalArgumentException`.
- `(.getLookupThunk r :undeclared)` of a `defrecord` returns nil.

## Concepts

- Compiler rule: a fact about `deftype*`, `create`, the reader or `Compiler.emitValue` that the type depends on.
  K-06 to K-08 name them.
- Emitter: a function that receives the parsed `defclosedrecord` arguments and returns the form that defines the type
  and its factories. It runs at macro expansion time. On the JVM, it is in `closed-record.impl.jvm`.
- Generated type: the class that the emitter form defines. Its fields, interfaces and methods are in
  [embodiment](../../../ramora/defclosedrecord/embodiment.md#generated-type).
- `D(T)`: the declared keys of type `T`.

## Scope

In scope:

1. Compiler rule tests (V-13) in `test/closed_record/compiler_rules_test.clj`. They use only `defrecord`, `deftype`,
   `deftype*`, the reader and the compiler. They do not use closed-record code. They fix these rules:
   - K-06: with the fields `__meta __extmap __hash __hasheq` at the end, the compiler generates `create`,
     `getBasis`, a constructor with the declared fields only and a constructor with all fields.
   - K-06: with three of these four fields, the type does not compile.
   - K-06: the `deftype` macro rejects these four field names.
   - K-07: `create` puts the extra keys of a map literal into `__extmap`, a missing key gives nil, and `create` does
     not call methods of the type (for example `assoc`).
   - K-08: `defrecord` does not implement `IType`. `Compiler.emitValue` uses the `IType` path before the `IRecord`
     path. A record constant without `IType` is built through `create` from the `java.util.Map` view.
2. Internal protocol `ClosedRecord` with the method `-extra-keys` in `closed-record.core` (D-17).
3. JVM emitter in `src/closed_record/impl/jvm.clj`. It emits the generated type as the embodiment specifies:
   - The `deftype*` field layout of D-01. The declared fields keep their type hints.
   - The interfaces of D-06 and the `ClosedRecord` protocol interface. Not `IType`.
   - Each method in the embodiment "Method behaviour" table.
   - The user protocol and interface implementations, as `defrecord` accepts them (D-15).
   - The factories `->Name` and `map->Name`, also for more than 20 fields (D-08, D-15).
   - The emitter does not require `closed-record.core`. It emits the protocol only as a symbol.
4. Macro `defclosedrecord` in `closed-record.core`. It has the `defrecord` syntax (D-15). At expansion time it
   rejects the reserved field names `__meta __extmap __hash __hasheq` and duplicate field names, then calls the JVM
   emitter.
5. Function `check` in `closed-record.core` (D-09, D-18).
6. Tests:
   - V-01: one test for each row and each column of the behaviour tables in
     [purpose](../../../ramora/defclosedrecord/purpose.md#behaviour), including the equality table and the reader
     literal row.
   - V-10: `->Name` and `map->Name` of a type with more than 20 fields.
   - V-15: `check` of a closed record, of a record from a literal with extra keys and of other values (including nil).
   - The macro argument checks: reserved names and duplicate fields.
   - Each `ex-info` `:type` of D-03 has a test of the full `ex-data`.
7. Remove the smoke test in `test/closed_record/core_test.clj`. The tests of this task replace it.
8. Docstrings for `defclosedrecord` and `check`.
9. `.clj-kondo/config.edn` with `:lint-as {closed-record.core/defclosedrecord clojure.core/defrecord}`. Without it,
   clj-kondo reports the generated `->Name` and `map->Name` as unresolved symbols.
10. Update `mementum/state.md` and `mementum/knowledge/dev-tooling.md` (the smoke test section).

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
- Emitter interface: one public function in `closed-record.impl.jvm` that the macro calls with the parsed arguments
  and that returns a form. Its name and argument shape are internal.
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

  `T` is the class of the generated type. `ks` is a set of all the extra keys. For nil, `:value-class` is nil. The
  message text is free, but it names the type and the key or keys.
- `assoc` of a declared key and `withMeta` make a new instance. They copy `__meta` (or set the new metadata) and
  `__extmap`. The new instance has hash caches 0.
- `cons` of a value that is not a map entry, a vector pair or a map throws as `defrecord` does
  (`IllegalArgumentException`).
- `getLookupThunk` of an undeclared key returns nil, as `defrecord` does. The keyword call site then calls `valAt`.
- `map->Name` checks the extra keys before it constructs the instance. Thus the result never has `__extmap` entries.

## Acceptance criteria

1. `test/closed_record/compiler_rules_test.clj` has a test for each rule in Scope 1. It requires no `closed-record`
   namespace.
2. `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`
   exit 0 with zero failures and zero errors.
3. The tests of Scope 6 exist. Each row and each column of the purpose behaviour tables maps to one or more tests.
   `implementation.md` lists the mapping.
4. A type defined with `defclosedrecord`:
   - `record?` is true. `(instance? clojure.lang.IType r)` is false.
   - Prints as `#ns.Name{...}`.
   - Has the `ex-data` of the Decisions table for each failure path.
5. `(macroexpand-1 '(defclosedrecord T [__meta]))` and a form with a duplicate field throw at expansion time.
6. `src/closed_record/impl/jvm.clj` does not require `closed-record.core`. The only source namespaces are
   `closed-record.core` and `closed-record.impl.jvm`.
7. `clj-kondo --lint src test` reports no errors and no warnings.
8. `cljfmt check src test` passes.
9. In the psi project REPL, `(:pass? (scry/run))` is `true`.
10. `test/closed_record/core_test.clj` has no smoke test. `mementum/state.md` names `defclosedrecord`, `check` and
    the test files. `mementum/knowledge/dev-tooling.md` does not say that a smoke test exists.
