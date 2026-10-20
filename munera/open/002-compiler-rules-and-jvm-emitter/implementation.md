# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

## Design review (2026-10-19)

- Architectural review added 3 new design steps (`design-steps.md`).
- Architecture sources: `ramora/META.md` and `doc/architecture.md` do not exist. The architecture is in
  `ramora/INDEX.md` → `ramora/defclosedrecord.md` → `ramora/defclosedrecord/`.
- Reference: Clojure 1.11.4 `core_deftype.clj` `emit-defrecord` (lines 150–265). It shows the `defrecord` method
  bodies: `cons` calls the private `imap-cons`, `withMeta` copies the hash caches, `assoc` of a declared key uses the
  constructor without the hash fields, `empty` throws `UnsupportedOperationException`.
- Embodiment "Allocation" says "emitter selection by host" at expansion time. With only a JVM emitter, the
  `:clj`-only call to `impl.jvm` is enough. Out of scope says the behaviour on other hosts is not specified, so this
  is not a finding.
- Ambiguity review added 3 new design steps.
- Inconsistency review added 2 new design steps. clj-kondo check (2026-10-19): a literal `deftype*` form in a
  linted file gives one "Unresolved symbol" error for each field, method name and argument.

## Notes for the design-steps work (2026-10-19)

- Principles:
  - Put the resolutions in the Scope items or the Decisions of `design.md`. Do not add scope items. Do not edit
    `ramora/`. If a resolution needs an architecture change, record it here and ask the user.
  - If there is a choice, use the `defrecord` behaviour (O-02, principle 15). If the choice is different, write the
    reason in `design.md`. Decisions that users can see become public API after the first release (D-20). Examples
    are macro errors and the `cons` `ex-data`.
- Links between the steps:
  - `cons` `ex-data` and generated-code dependencies: `defrecord` `cons` calls the private `imap-cons`. That
    function calls `.assoc` for each entry in seq order, so with `:key` the first undeclared key throws. If the
    emitter does the same with inline code, it also fixes the dependency step.
  - clj-kondo step: the same choice applies to every `deftype*` form in the tests, including the K-08 `emitValue`
    tests.
- Paths:
  - `defrecord` source: `~/.m2/repository/org/clojure/clojure/1.11.4/clojure-1.11.4.jar`, file
    `clojure/core_deftype.clj` (`imap-cons` line 133, `emit-defrecord` line 149, `validate-fields` line 294).
    The 1.12.6 and 1.13.0-alpha9 jars are in the same directory.
  - `ramora/defclosedrecord/decisions/failure-and-validation.md` (D-03, D-18) and `type-structure.md` (D-01, D-06,
    D-07, D-15). `ramora/defclosedrecord/embodiment.md` (Boundaries, State).
  - `mementum/knowledge/dev-tooling.md` (sections "Scry" and "Smoke-test lint").

## Design-steps resolution (2026-10-19)

- All 8 design steps are done in `design.md`. No `SCOPE_QUESTION:` items.
- Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9:
  - `macroexpand-1` wraps a macro error in `Compiler$CompilerException`. `:clojure.error/phase` is `:macroexpansion`
    for an `Error` (`AssertionError`) and `:macro-syntax-check` for an `Exception`. Do not test the phase.
  - `defrecord` with duplicate fields expands. `eval` fails with `ClassFormatError` ("Duplicate field name").
  - `(eval '(deftype* ...))` in a test gives no clj-kondo findings.
- Observed on 1.11.4:
  - A record class has 3 constructors: all fields; declared fields + `__meta __extmap`; declared fields only.
    `defrecord` `assoc` uses the second (hash caches 0). `withMeta` uses the first (it copies the caches).
  - `defrecord` `cons` (`imap-cons`) of a seq of vector pairs, for example `(conj r '([:b 1]))`, throws
    `ClassCastException`, not `IllegalArgumentException`. `(conj r :x)` throws `IllegalArgumentException`. The design
    statement "throws as `defrecord` does (`IllegalArgumentException`)" is exact only for the second case. An inline
    copy of the `imap-cons` loop gives the `defrecord` behaviour for both. Tests must not expect
    `IllegalArgumentException` for the first case.
- Architecture findings (do not change `ramora/`; ask the user):
  - purpose "Behaviour": the row `get`, keyword call, `.field` gives nil for an undeclared key. `.field` of an
    undeclared name has no field (host error). The design excludes this cell from V-01.
  - embodiment "Allocation" names only reserved names and duplicate fields as argument checks. The design adds the
    mutable-metadata check (embodiment "State": declared fields are immutable).
  - embodiment "Boundaries" does not list public `clojure.core` vars for the generated code. The design permits them.

## Design review, pass 2 (2026-10-19)

- Architectural review added 1 new design step (expansion-time private `clojure.core` vars). The pass-1 step
  "Run-time dependencies" covers only the generated code, so this step is not a duplicate.
- Ambiguity review, pass 2, added 3 new design steps. Observed K-06 check (2026-10-19): a `deftype*` with three of
  the four hidden fields gets an alternative constructor with bad bytecode (`Compiler.java` pushes 4 values for
  `altCtorDrops` = 3). The class is defined, and the JVM verifier rejects it at initialisation. Thus a test that only
  `eval`s the `deftype*` form passes on all three versions and does not fix the rule.
- Inconsistency review, pass 2, added 1 new design step. Checked and consistent: the macro error classes against
  `validate-fields` and `emit-defrecord` (1.11.4 line 164: the mutable check tests the presence of the metadata key,
  not its value), the `dev-tooling.md` sections of Scope 10, and the README test commands of acceptance 2.

## Notes for the pass-2 design-steps work (2026-10-19)

- Principles: the same as "Notes for the design-steps work". Also: keep the observed facts of a version in the
  design. Do not copy the K-06 and K-07 wording of `ramora/` or the memory file without a check.
- Links between the steps:
  - `cons` step and private-vars step: `imap-cons` is private. An inline copy of all three branches gives `defrecord`
    parity, and it also obeys Scope 3. Then change the `conj` bullet in "Observed facts" and the `cons` decision.
  - Private-vars step: public replacements exist. `resolve` + `(:on @var)` gives the protocol interface, and
    `clojure.core/destructure` does destructuring. `parse-opts+specs` and `parse-impls` are about 30 lines.
  - K-06 step: the wrong wording "does not compile" is also in `ramora/defclosedrecord/context.md` (K-06) and
    `mementum/memories/deftype-star-hidden-fields.md`. Neither file is in Scope 10. Record this as an architecture
    finding for the user. Do not edit those files.
- K-06 reproduction (observed): `(eval '(deftype* user/T user.T [a __extmap __hash __hasheq] :implements
  [clojure.lang.IRecord]))` returns. Then `(Class/forName "user.T" true (clojure.lang.RT/baseLoader))` throws
  `java.lang.VerifyError` with no cause. The result is the same for each three-field subset and on all three versions.
- Paths: `~/.m2/repository/org/clojure/clojure/1.13.0-alpha6/clojure-1.13.0-alpha6-sources.jar`
  (`clojure/lang/Compiler.java`: `altCtorDrops` line 8605, alternative constructor lines 8733–8760). The 1.11.4 jar
  has no `.java` sources. `core.clj` line 4517 is `maybe-destructured`.

## Pass-2 design-steps resolution (2026-10-19)

- All 5 pass-2 design steps are done in `design.md`. No `SCOPE_QUESTION:` items.
- Checked on 1.11.4, 1.12.6 and 1.13.0-alpha9 (same results):
  - `parse-opts`, `parse-impls`, `parse-opts+specs`, `maybe-destructured` and `imap-cons` have the same source.
  - K-06 three-field reproduction for all four layouts (fields `[a <three of four>]`, class names `ns.T0`..`ns.T3`
    in a non-`user` namespace): `eval` returns, `Class/forName` with initialise true throws `VerifyError`, no cause.
  - The `conj` results in "Observed facts", also `"ab"` and `'({:a 3})` → `ClassCastException`.
- Observed on 1.11.4: `(macroexpand-1 '(defrecord T [a] :foo 1))` throws `CompilerException` with cause
  `IllegalArgumentException`. The design adds this check (unsupported option) to Scope 4, Scope 6 and acceptance 5,
  because the reimplemented `parse-opts+specs` includes it.
- The design does not add tests for the parsing of protocol symbols and destructured method arguments. Only V-10 and
  the macro argument tests cover the reimplemented private steps.
- Architecture finding (do not change; ask the user): "does not compile" for three of the four hidden fields is
  wrong in `ramora/defclosedrecord/context.md` (K-06) and `mementum/memories/deftype-star-hidden-fields.md`. The
  class compiles and fails JVM verification.

## Design review, pass 3 (2026-10-19)

- Architectural review added 1 new design step (allocation of host-specific parsing between `core.cljc` and
  `impl.jvm`). Other areas checked against the architecture with no new finding: D-01 layout, D-06 interfaces, D-07
  hash-cache copy, D-17 protocol as a symbol, Boundaries, State, the `cons` branches and the `ex-data` table. The
  earlier architecture findings for the user (Boundaries, Allocation, K-06 wording) stay open. They are not repeated.
- Ambiguity review, pass 3, added 1 new design step (`map->Name` argument and construction path). Observed on 1.11.4
  for `defrecord` `map->Name`: the results in that step. D-19 rationale ("a map literal calls `create` directly, so
  the two paths then fail differently") assumes that `map->Name` also uses `create`.
- Inconsistency review, pass 3, added 2 new design steps. Embodiment "Boundaries" (generated code → `ClosedRecord`
  and `clojure.lang` only) also omits JDK classes. That is an architecture finding for the user, like the omission of
  public `clojure.core` vars. Do not edit `ramora/`.

## Notes for the pass-3 design-steps work (2026-10-19)

- Principles: the same as "Notes for the design-steps work" and its pass-2 notes. Also: keep `core.cljc`
  host-neutral (D-13, K-02). Put a JVM-only step in `impl.jvm`, also if `defrecord` does it in one place.
- Links between the steps:
  - Allocation step and Concepts/Decisions: "the parsed arguments" is in two places (Concepts "Emitter" and
    Decisions "Emitter interface"). Change both. The emitter function name and argument shape stay internal.
  - `map->Name` step and JDK-class step: the `defrecord` choice (`MapEquivalence` test, `into {}`, `Name/create`)
    uses only `clojure.lang` and public `clojure.core`. If the design takes it, the extra-key check applies to the
    converted map, and D-19 holds through `create`. The check must also be inline (Scope 3).
  - Versions step: change only the "Problem" text and the "Observed facts" heading. The facts stay.
- Paths: `defrecord` `map->Name` is in `clojure/core_deftype.clj` lines 400–403 of the 1.11.4 jar (path in "Notes
  for the design-steps work"). The `java.util.Map` methods are at lines 244–253.

## Pass-3 design-steps resolution (2026-10-19)

- All 4 pass-3 design steps are done in `design.md`. No `SCOPE_QUESTION:` items.
- The mutable-metadata check moved from the macro to the emitter. Reason: the keys are JVM `deftype*` keys
  (ClojureScript uses `:mutable`, and its `defrecord` does not reject it), and `defrecord` does this check in
  `emit-defrecord`, not in `validate-fields`. Scope 3, Scope 4 and "Allocation of argument processing" agree.
- The unsupported-option check is also in the emitter. Both errors still occur in `macroexpand-1` of the macro form,
  so acceptance 5 and the Scope 6 tests do not change.
- Scope 3 permitted references exclude the generated type itself (`new`, `Name/create`) and the names that the user
  gives (type hints, protocols, interfaces, method bodies).
- A record is not a `clojure.lang.MapEquivalence`. `map->Name` of a record goes through `(into {} r)`.
- Architecture finding update (do not change `ramora/`; ask the user): embodiment "Allocation" puts the argument
  checks at "macro expansion time". The emitter also runs then, so the emitter checks agree with it.

## Design review, pass 4 (2026-10-19)

- No architectural review feedback. Checked again after the pass-3 changes: the allocation of the macro and emitter
  steps (D-13, K-02), the emitter checks at expansion time (embodiment "Allocation"), the Scope 3 permitted
  references, and `map->Name` through `create` (D-08, D-19). Considered and not a finding: no test for user protocol
  implementations or destructured method arguments. The verification plan does not require one, and Scope 6 is frozen.
- Ambiguity review, pass 4, added 3 new design steps. The K-06 reproduction in "Notes for the pass-2 design-steps
  work" has no `^int` hints, so its `VerifyError` does not show the three-field rule. Repeat it with hints.
  Not raised (test method, not design): how the K-07 test sees that `create` calls no method, and how the K-08 test
  sees the `IType` order. Observed on 1.11.4: a `deftype*` with the K-06 layout (`^int` hints), `IRecord`,
  `IPersistentMap` and `assoc`, `without`, `cons` that record calls in an atom: `read-string` of `#ns.P{:a 1 :x 2}`
  gives `a` 1, `__extmap` `{:x 2}` and no calls.
- Inconsistency review, pass 4, added 2 new design steps. `.clj-kondo/` does not exist yet, so Scope 9 creates it.

## Notes for the pass-4 design-steps work (2026-10-19)

- Principles: the same as the earlier "Notes for the design-steps work" sections. Also: put each resolution in the
  Scope 1 K-06 bullets, Scope 6 or the Decisions. Do not add checks or tests that a scope item does not name. For
  the `validate-fields` step, limiting the parity sentence changes no scope. If you add the two checks instead,
  name them in Scope 4, "Allocation of argument processing" and "Argument errors" together.
- Links between the steps:
  - K-06 hints step: D-01 already gives `^int ^:unsynchronized-mutable` for the hash fields. Only `^int` matters for
    verification. `ramora/defclosedrecord/context.md` (K-06) and `mementum/memories/deftype-star-hidden-fields.md`
    also omit the hints. Record this with the open K-06 architecture finding. Do not edit those files.
  - Type-hash step: `classname` is `(symbol (str (namespace-munge *ns*) "." name))`, so the V-01 oracle is
    `(bit-xor (hash 'my_ns.Name) (clojure.lang.APersistentMap/mapHasheq r))`, with `int` cast as `defrecord` does.
  - V-10 step and arity-reference step: both concern `build-positional-factory`. Resolve them together.
- K-06 reproduction with hints (observed, all three versions): at top level,
  `(eval '(deftype* ns/T4 ns.T4 [a __meta __extmap ^int __hash ^int __hasheq] :implements [clojure.lang.IRecord]))`,
  then `(Class/forName "ns.T4" true (clojure.lang.RT/baseLoader))` loads, with 3 public constructors. Remove one of
  the four fields (keep `^int` on the hash fields that stay): `VerifyError`, no cause.
- Paths (Clojure 1.11.4 jar `clojure/core_deftype.clj`): `classname` line 153, `type-hash` line 163, `hasheq` line
  176, `build-positional-factory` line 267, `validate-fields` line 294.

## Pass-4 design-steps resolution (2026-10-19)

- All 5 pass-4 design steps are done in `design.md`. No `SCOPE_QUESTION:` items.
- `validate-fields`: the design limits the parity sentence. It does not add the non-vector and non-symbol field
  checks. Their result is not specified and not tested.
- Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9: the K-06 results are the same with `^int ^:unsynchronized-mutable`
  (D-01) as with `^int` only. The `defrecord` `classname`, `type-hash`, `hasheq` and `build-positional-factory`
  source is the same (line numbers against 1.11.4: 1.12.6 +1, 1.13.0-alpha9 +14).
- Observed on 1.11.4 for a `defrecord` with 22 fields: `->Name` with 19, 21 or 23 arguments throws
  `ArityException`. With 21 or 23 the generated `count overage` check throws. With 20 or fewer, the `fn` arity
  throws. Both are `clojure.lang.ArityException`.
- Architecture finding (add to the open K-06 finding; ask the user): `ramora/defclosedrecord/context.md` (K-06) and
  `mementum/memories/deftype-star-hidden-fields.md` do not say that the hash fields need `^int`.

## Design review, pass 5 (2026-10-19)

- No architectural review feedback. Checked the pass-4 changes: the type hash `(hash classname)` as an
  expansion-time constant (D-07, I-05), the K-06 `^int` metadata (D-01), V-10 (D-02, D-15). Not a finding: the
  non-vector and non-symbol `validate-fields` cases stay unspecified. D-15 fixes the valid syntax only.
- Ambiguity review, pass 5, added 2 new design steps. `defrecord` source check (1.11.4 `emit-defrecord`): the
  generated methods that the embodiment table omits are `valAt k`, `isEmpty` and `containsValue`. `assocEx` is not
  generated by `defrecord` either, so it is not a finding.
- Inconsistency review, pass 5, added 1 new design step (K-06 constructors). Checked and consistent: the `ex-data`
  table against D-03 and D-18, the D-06 interfaces, the verification items in Scope, Out of scope and Adjacent work
  (V-01 to V-15), and the `dev-tooling.md` sections of Scope 10.

## Notes for the pass-5 design-steps work (2026-10-19)

- Principles: the same as the earlier "Notes for the design-steps work" sections. Put each resolution in Scope 1,
  Scope 3 or the Decisions. Do not add scope items or tests that a scope item does not name. Do not edit `ramora/`.
- Links between the steps:
  - Methods step: `valAt k` is necessary for `get` and the keyword call (`RT.get` → one-argument `valAt`), so V-01
    already depends on it. The new methods are inline code of `clojure.lang` and public `clojure.core` only
    (Scope 3). `containsKey`, `entryAt`, `isEmpty` and `containsValue` of `defrecord` call `.valAt`, `.count` and
    `vals`, so they see only the declared entries when those methods do. Add the three methods to the open
    architecture finding about the embodiment "Method behaviour" table.
  - Constructors step and `assoc` decision: if the design names the declared + `__meta __extmap` constructor for
    `assoc`, the K-06 test must check it. Otherwise the type depends on a rule that no test fixes (Intent). This
    constructor gives hash caches 0. The all-fields constructor with `0 0` gives the same result.
  - K-06 rejection step and constructors step: both change the Scope 1 K-06 bullets. Resolve them together.
- Observed on 1.11.4, ns `my-ns`, the K-06 four-field form with one declared field `a` and the D-01 metadata: the
  public constructors are `(Object Object Object int int)`, `(Object Object Object)` and `(Object)`. `create` and
  `getBasis` exist. Read them with `.getConstructors` and `.getParameterTypes`.
- Paths (Clojure 1.11.4 jar `clojure/core_deftype.clj`, path in "Notes for the design-steps work"): `valAt k` line
  193, `containsKey` line 225, `isEmpty` line 247, `containsValue` line 248, `validate-fields` line 294.
  `ramora/defclosedrecord/context.md` lines 27 and 66–70 (alternative constructors, K-06).

## Pass-5 design-steps resolution (2026-10-19)

- All 3 pass-5 design steps are done in `design.md`. No `SCOPE_QUESTION:` items.
- Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9 (ns `my-ns`, K-06 four-field form, one field `a`, D-01 metadata):
  the declared-only constructor gives `__meta` and `__extmap` nil. The `__hash` and `__hasheq` fields are private
  (mutable), so `(.-__hash o)` throws `IllegalArgumentException`. Do not read them in tests.
- `containsValue` copies the `defrecord` body, with its nil/false quirk (observed on 1.11.4). No test in Scope 6
  covers `valAt k`, `isEmpty` or `containsValue` directly. The V-01 `get` and keyword-call tests use `valAt k`.
- Architecture findings (add to the open findings; do not change `ramora/`; ask the user):
  - embodiment "Method behaviour" omits `valAt k`, `isEmpty` and `containsValue`.
  - embodiment "Generated type" names 2 constructors, not 3 (the declared + `__meta __extmap` constructor is
    missing). `context.md` line 27 names only the declared-only alternative constructor.

## Design review, pass 6 (2026-10-19)

- No architectural review feedback. Checked the pass-5 changes: `valAt k`, `isEmpty` and `containsValue` see only
  the declared entries through `count` and `vals` (I-01, I-09). `assoc` uses the declared + `__meta __extmap`
  constructor (K-06, I-03, I-07). The K-06 constructor and `deftype` tests use no closed-record code (V-13).
- Ambiguity review, pass 6, added 1 new design step (D-19 case in V-01). Not raised: the docstring content
  (Scope 8) and the choice of "other values" for V-15. They are implementation choices, not design gaps.
- Inconsistency review, pass 6, added 1 new design step (`ex-data` test unit: Scope 6 vs acceptance 4). Checked and
  consistent: the `valAt k`, `isEmpty`, `containsValue` and `map->Name` bodies and line numbers against the 1.11.4
  jar, the Context references (task 001, README "Development", the memory file, D-01 to D-20).

## Notes for the pass-6 design-steps work (2026-10-19)

- Principles: the same as the earlier "Notes for the design-steps work" sections. Put each resolution in Scope 6,
  acceptance 4 or the Decisions. Do not add scope items. Do not edit `ramora/`.
- Links between the steps:
  - Both steps change only the test text of Scope 6. Resolve them together. A test of a D-19 case is a test of a host
    exception (D-03 scope: other failures keep the host exception), so it checks the class, not `ex-data`.
  - `ex-data` step: the V-01 "Undeclared key" tests of `assoc`, `conj`, `map->Name` and the V-15 tests already reach
    each path. The step needs only a statement that they check the full `ex-data`. `merge`, `into` and `conj` all go
    through `cons`.
  - D-19 step: the reader test reads with `read-string` (as the existing reader-literal unit does). A field with
    `^long` in the test type is a declared-field type hint (Scope 3), so it needs no new emitter behaviour.
- Paths: D-19 is in `ramora/defclosedrecord/decisions/closed-key-set.md` (lines 64–73). The `ex-data` table is in
  D-03, `ramora/defclosedrecord/decisions/failure-and-validation.md` (lines 3–18). The purpose tables are in
  `ramora/defclosedrecord/purpose.md` "Behaviour".

## Pass-6 design-steps resolution (2026-10-19)

- Both pass-6 design steps are done in Scope 6 and acceptance 4 of `design.md`. No `SCOPE_QUESTION:` items.
- Checked again on 1.11.4, 1.12.6 and 1.13.0-alpha9 (`defrecord` with `^long a b`, ns `my-ns`): `map->P {:b 1}` and
  `read-string "#my_ns.P{:b 1}"` throw `NullPointerException` with no cause. Without `:b`, `:b` is nil. The reader
  throws at read time, so the D-19 reader test needs no `eval`.
- The 7 `ex-data` paths: `assoc` and `cons` (undeclared key), `dissoc`, `empty`, `map->Name`, `check` of a record
  with extra keys, `check` of another value. Each one needs at least one test with `=` on the full `ex-data`. List
  the test of each path with the V-01 mapping (acceptance 3).

## Design review, pass 7 (2026-10-19)

- No architectural review feedback. Checked the pass-6 changes: the 7 `ex-data` paths agree with D-03. The D-19 case
  in the V-01 `map->Name` and reader tests agrees with D-19 and D-03 scope (host exception, no `ex-data`). Not a
  finding: it overlaps V-09 ("a missing primitive field throws"). The purpose table cell names D-19, and the overlap
  does not block the V-09 task (Adjacent work).
- Ambiguity review, pass 7, added 1 new design step (expansion forms: `declare`, `import`, return value). Not
  raised: the factory docstrings and `:arglists` (`defrecord` gives both), a zero-field type and the munged
  namespace in the printed `#ns.Name` (the V-01 hash decision already uses `my_ns.Name`). These are implementation
  choices or follow from "as `defrecord`".
- Inconsistency review, pass 7, added 1 new design step (`getBasis` in the K-06 test against E-02). Checked and
  consistent: the `deps.edn` aliases (`:clj-1.12` 1.12.6, `:clj-1.13` 1.13.0-alpha9) against acceptance 2 and the
  "Order" decision, the D-06 interface set against Scope 3, the purpose tables against the V-01 units, and K-07
  against E-02 (`create` calls `valAt` and `without` on its map argument, not on the type).

## Notes for the pass-7 design-steps work (2026-10-19)

- Principles: the same as the earlier "Notes for the design-steps work" sections. Put each resolution in Scope 1,
  Scope 3, Concepts or the Decisions. Do not add scope items or tests that a scope item does not name. Do not edit
  `ramora/`.
- Expansion-forms step:
  - `defrecord` shape: `(let [] (declare ->N) (declare map->N) (deftype* ...) (import classname) (defn ->N ...)
    (defn map->N ...) classname)`. `classname` is the same symbol as in the "Type hash" decision.
  - "the form that defines the type and its factories" is in Concepts "Emitter". "Emitter interface" says "returns
    a form". Change them together.
  - `declare`, `import` and `defn` are public `clojure.core`, so the Scope 3 rule permits them.
  - Scope 9 (`:lint-as` `defrecord`) makes clj-kondo expect the `defrecord` result: `Name` resolves to the class.
  - The factory docstrings and `:arglists` stay free. The ambiguity review did not raise them.
- `getBasis` step: change only the first K-06 bullet of Scope 1. `create` is the method that shows the rule
  (generated only with dropped fields). Embodiment "Generated type" also lists `getBasis` under "The compiler
  generates the following (K-06)". Add this to the open architecture findings for the user.
- Paths: Clojure 1.11.4 jar `clojure/core_deftype.clj` lines 388–404 (`defrecord` macro). `clojure/lang/Compiler.java`
  `emitStatics` near line 8805 in `clojure-1.13.0-alpha6-sources.jar`. `ramora/defclosedrecord/context.md` line 84
  (E-02). `ramora/defclosedrecord/embodiment.md` "Generated type".

## Pass-7 design-steps resolution (2026-10-19)

- Both pass-7 design steps are done in Scope 1, Scope 3, Scope 4, Concepts and the Decisions ("Expansion form").
  No `SCOPE_QUESTION:` items.
- Observed on 1.11.4, 1.12.6 and 1.13.0-alpha9: the `defrecord` macro body is the same (lines 394–404, 395–405,
  408–418). `macroexpand-1` of `(defrecord Q [a])` gives `(let [] (declare ..) (declare ..) (deftype* ..) (import ..)
  (defn ..) (defn ..) my_ns.Q)`. `emit-defrecord` returns the `deftype*` form itself, with no wrapper.
- Test pitfall: a `defrecord` `Object` `toString` that calls `str` on a record recurses (`StackOverflowError`).
  Use a protocol method to check that a method body can call `map->Name`.
- Architecture findings (add to the open findings; do not change `ramora/`; ask the user):
  - embodiment "Generated type" lists `getBasis` under the K-06 compiler output. E-02: every `deftype*` has it.
  - embodiment does not name the expansion forms (`declare`s, `import`, the class as the value).

## Design review, pass 8 (2026-10-19)

- No architectural review feedback. Checked the pass-7 changes: the expansion form in `impl.jvm` (D-13, K-02: the
  `import` and the class value are JVM-specific), the macro returns the emitter form unchanged (D-13), the forms use
  only public `clojure.core` (Scope 3), and the `defrecord` expansion shape (principle 15, D-15). The K-06 test
  without `getBasis` agrees with E-02. The embodiment findings of pass 7 stay open for the user.
- No ambiguity review feedback. Not raised: the presence-or-value test of the mutable metadata keys ("as `defrecord`
  does" fixes it: presence), the `#ns.Name{...}` print content (`print-map` uses `seq`, so declared entries only),
  and the `import` of a name that the namespace already imports (host error, as `defrecord`).
- No inconsistency review feedback. Checked: the Context anchors (`purpose.md#behaviour`,
  `qualities.md#verification-plan`) and paths exist; `core.cljc` is still the empty namespace and `core_test.clj`
  still has the smoke test (Scope 7, Scope 10). Not raised (format only): Scope 4 line 122 is longer than the
  120-column wrap of the file.

## Notes after design review, pass 8 (2026-10-19)

- Pass 8 added no design steps. `design-steps.md` has no unchecked items. The three reviews found no new problems,
  so `design.md` is ready for `create-task-plan`.
- Open architecture findings for the user are in the "Architecture finding(s)" bullets of the resolution sections
  (passes 1–7, `implementation.md` lines 58, 109, 151, 199, 243, 329). Show them to the user together before or
  at task close. Do not edit `ramora/` or `mementum/memories/deftype-star-hidden-fields.md` without approval.
- If a later step edits `design.md`, also wrap Scope 4 line 122 to 120 columns.

## Plan ambiguity review (2026-10-19)

- Ambiguity review of `plan.md` and `steps.md` added 3 new design steps (K-06 class loader, K-08 assertions,
  `core.cljc` `:cljs` branch and clj-kondo).
- The K-06 class-loader step is a blocker, not only wording: with `RT/baseLoader` the Slice 1 gate fails in the scry
  CLI with `ClassNotFoundException`. In the psi REPL, `Compiler/LOADER` can be bound, so the same test can pass
  there. Do not read a CLI failure of this test as a K-06 rule failure.
- Reproductions: `/tmp`-only scripts, not in the repository. K-08 type: the methods that `create` and
  `PersistentArrayMap.create(java.util.Map)` call are enough (`valAt`, `without` on the argument map; `entrySet` on
  the type). `deftype*` does not require all interface methods.
- Not raised (implementation choice, no acceptance risk): names of the new test namespaces, and whether
  `core_test.clj` is kept for the new tests or deleted (acceptance 10 holds in both cases); how the Slice 3
  "generated code refers only to" step is checked (REPL `macroexpand-1` inspection is enough); the return shape of
  `-extra-keys` (embodiment: `__extmap`).
- Plan inconsistency review added 3 new design steps (architecture-findings step, clj-kondo timing,
  acceptance 5 mutable-metadata forms). Checked and consistent: the 7 `ex-data` paths, the argument-error classes,
  the expansion form, the `cons` and `map->Name` branches, the type hash and the slice order against `design.md`.

## Notes for the plan-review design-steps work (2026-10-19)

- Principles:
  - Put each fix where the text is. Class-loader and K-08 items: Scope 1 of `design.md` and Slice 1 of `steps.md`.
    `:cljs` item: Decisions of `design.md` and Slices 2 and 4. Other items: `plan.md` and `steps.md` only. Do not add
    scope items. Do not edit `ramora/`.
  - Check every new observation in the scry CLI (`clojure -M:test -m scry.cli`) on the three versions, not only
    with `-e` or the REPL. `Compiler/LOADER` is bound in `eval`, `load` and the REPL, and not bound in a CLI test.
  - `design.md` changes: also wrap Scope 4 line 122 (see "Notes after design review, pass 8").
- Links between the steps:
  - The `:cljs` item and the clj-kondo timing item meet in Slice 4: run clj-kondo on `core.cljc` after the fix.
  - The architecture-findings step must include the memory file wording "With only 3 of these fields, the type does
    not compile" (it is `VerifyError` at class load, see the pass-2 finding).
- Reproduction shape (temp project with scry dep, outside the repository; the CLI hides test output, so `spit` the
  results to a file):
  - Class loader: `(eval '(deftype* x/T3 x.T3 [a __meta __extmap ^int ^:unsynchronized-mutable __hash]
    :implements [clojure.lang.IRecord]))`, then `Class/forName` with `RT/baseLoader` vs a new `DynamicClassLoader`.
    `RT/classForName` finds the class in memory without initialization, so it gives no `VerifyError`.
  - K-08: run the two-type script with
    `clojure -Sdeps '{:deps {org.clojure/clojure {:mvn/version "<v>"}}}'` or the `:clj-1.12`/`:clj-1.13` aliases.
- Paths: `RT.java` `baseLoader`, `DynamicClassLoader.java` (static `classCache`), `Compiler.java` `emitValue`
  (E-03) in the Clojure jars under `~/.m2/repository/org/clojure/clojure/`. clj-kondo: `~/.local/bin/clj-kondo`.

## Plan-review design-steps resolution (2026-10-19)

- All 6 plan-review items are done in `plan.md` and `steps.md`. `design.md` is not changed (read-only in this pass).
  Scope 4 line 122 is still longer than 120 columns.
- K-06 lookup keeps the Scope 1 call: `Class/forName ... (RT/baseLoader)` inside `with-bindings` of
  `Compiler/LOADER` to a new `DynamicClassLoader`. Observed in the scry CLI on 1.11.4, 1.12.6 and 1.13.0-alpha9:
  four-field form loads (3 constructors, `create`); each three-field form throws `VerifyError`, no cause.
- K-08, observed in the scry CLI on the three versions (differs from the plan-ambiguity item text):
  - K-06 layout (D-01 `^int`) plus `IType`: the constant throws `VerifyError`. The `IType` path of `emitValue` uses
    `getConstructors()[0]` with the `getBasis` values (`[a]`). Constructor order: 1.11.4 and 1.12.6 `[5 3 1]`
    params, 1.13.0-alpha9 `[3 1 5]`. Thus the `IType` order test uses a `[a]`-only `deftype*`.
  - `[a]`-only with `IType` + `IRecord`: constant `a` 1. Without `IType`: `NoSuchMethodError` (no `create`).
  - K-06 layout without `IType`: constant `a` 99 from `entrySet`.
- K-07 atom method checked in the CLI: `read-string` finds the `eval`ed class (in-memory lookup), no method calls.
- clj-kondo v2026.04.15 on a `core.cljc` of the `plan.md` `:cljs` shape plus a test that uses `defclosedrecord`,
  `->P`, `map->P`, `check` with `:lint-as`: 0 errors, 0 warnings.
- A scry test with no assertions counts as "unknown" and makes the CLI exit 1. Every test needs an assertion.
- Reproduction project: `/tmp/cr-planrev` (not in the repository).

## Plan ambiguity review, pass 2 (2026-10-19)

- Ambiguity review of `plan.md` and `steps.md` added 2 new design steps (`*ns*` of quoted test forms in the CLI,
  V-10 wrong-arity calls and clj-kondo).
- Both are CLI or lint failures, not rule failures. A K-07 `eval` error "Unable to resolve symbol" in the CLI is a
  test-form error. Do not read it as a K-07 failure (the "Order" stop rule).
- The pass-1 reproduction (`/tmp/cr-planrev/test/k/k_test.clj`) already used `k.k-test/calls` with `intern`. The
  steps text does not say it. Reproductions: `/tmp/cr-nscheck` (scry CLI, `*ns*`), `/tmp/cr-kondo` (clj-kondo
  arity). Both are outside the repository.
- Not raised (implementation choice or settled earlier): how `check` detects a closed record (`satisfies?` or the
  protocol interface), the exact print string of acceptance 4 (munged namespace), and which form throws in the K-08
  `NoSuchMethodError` case (the reproduction catches the `eval` and the call together; the error has no wrapper).
- Plan inconsistency review, pass 2, added 1 new design step (list of the `ex-data` path tests). Not raised: the
  Slice 1 three-field step does not repeat the declared field `a` and `:implements [clojure.lang.IRecord]` of Scope 1
  ("layouts" refers to them; the observations used `IRecord`). The K-06 control test (`deftype Q`, `getBasis`, no
  `create`) is extra to Scope 1 but agrees with E-02. The plan-level `with-bindings` of the K-06 lookup wraps the
  Scope 1 call and does not contradict it.

## Notes for the plan-review pass-2 design-steps work (2026-10-19)

- Principles:
  - Fix in `plan.md` and `steps.md` first. Change `design.md` only for acceptance 5. If you change it, also wrap
    Scope 4 line 122 to 120 columns. Do not add scope items. Do not edit `ramora/`.
  - Check each new test-form choice in the scry CLI on the three versions, and lint it with clj-kondo. A REPL pass
    does not show the CLI result (`*ns*` is `user` in the CLI).
- Links between the steps:
  - `*ns*` step: the same choice applies to the K-06 reserved-name `macroexpand-1` forms. They pass now only
    because `user` refers `clojure.core/deftype`. The emitter reads `*ns*` for `classname` (type hash) and for
    protocol resolution, so `binding [*ns*]` changes both. A fully qualified macro symbol does not.
  - Arity and `*ns*` steps: an `eval` of a quoted wrong-arity call also avoids clj-kondo. Under `*ns*` `user`, it
    needs the qualified `->Name`. `apply` is simpler.
  - `ex-data` list step: put the list in the same `implementation.md` section as the V-01 mapping. Then a review of
    acceptance 3 and 4 reads one place.
- Paths: clj-kondo `~/.local/bin/clj-kondo` (v2026.04.15). Reproductions `/tmp/cr-nscheck/test/k/n_test.clj` and
  `/tmp/cr-kondo/src/x/t.clj`.

## Plan-review pass-2 design-steps resolution (2026-10-19)

- All 3 plan-review pass-2 items are done in `plan.md` and `steps.md`. `design.md` is not changed; acceptance 5
  keeps its form because the tests bind `*ns*`. Scope 4 line 122 is still longer than 120 columns.
- `*ns*` choice: `(binding [*ns* (the-ns '<test-ns>)] ...)`, not qualified symbols. Observed in the scry CLI on
  1.11.4, 1.12.6 and 1.13.0-alpha9 (`/tmp/cr-nscheck`): inside the binding, a referred macro with `__meta` and
  `(deftype T [a __meta])` give `ex-cause` `AssertionError`; an `eval`ed `deftype*` method body with an unqualified
  test-namespace var (`(swap! calls conj ..)`) works. The binding is local: `*ns*` is `user` again after it.
- clj-kondo v2026.04.15 (`/tmp/cr-kondo/test/x/`): `(apply ->P (range 2))` gives no finding. A namespace that uses a
  `:refer`red `defclosedrecord` only in quoted forms gives two warnings (namespace required but never used, var
  referred but never used). Keep the Slice 5 error tests in a namespace that also uses the macro unquoted.

## Plan ambiguity review, pass 3 (2026-10-19)

- No ambiguity review feedback. The pass-2 resolutions (`*ns*` binding, `apply` for V-10 wrong-arity calls) are
  clear in `plan.md` and `steps.md`.
- Not raised (implementation choice, or the design or a gate settles it): how the `ClosedRecord` interface reaches
  `:implements` (literal `closed_record.core.ClosedRecord` or var resolution, D-17 names both); the empty-extras
  test of `check` (`-extra-keys` gives `__extmap`, nil after `create` with no extras); the reset of the K-07 `calls`
  atom between REPL runs; the purpose sentence "Metadata and hidden literal keys do not have an effect on equality
  or hash" (not a table row, so not a V-01 unit; hidden keys are V-14); the content of `core_test.clj` after the
  smoke test is removed (the Slice 7 clj-kondo gate catches unused refers). `.clj-kondo/.cache/` is already in
  `.gitignore`.
- Plan inconsistency review, pass 3: no inconsistency review feedback. Checked and consistent: the clj-kondo
  steps (Slices 1, 4, 5, 7) against the plan risk, the Slice 6 and 7 architecture-findings steps against
  "Notes after design review, pass 8" (its line references are still valid), the Slice 5 `ex-data` list step
  against `plan.md` Slice order 5, and acceptance 5 against the Slice 5 error step. Not raised (wording only, the
  emitter shape is internal): Slice 3 "name/type/fields/options/specs" and Slice 5 "declared key / undeclared key /
  etc." (the tables have two key columns).

## Notes after plan review, pass 3 (2026-10-19)

- Pass 3 added no design steps. `design-steps.md` has no unchecked items, so there is no design-steps work. The next
  step is implementation, Slice 1 of `steps.md`.
- If a later review adds design steps, use the principles of the earlier "Notes for the plan-review ... design-steps
  work" sections. Do not raise again the items under "Not raised" in the pass-3 review notes.

## Slice 1 implementation (2026-10-19)

- `test/closed_record/compiler_rules_test.clj` done: 9 tests, pass on 1.11.4, 1.12.6, 1.13.0-alpha9; clj-kondo
  and cljfmt clean. No closed-record require.
- Forms are built as data by helper `deftype*-form` (`list*` of symbols), not literal quoted forms; same effect for
  clj-kondo (no lint of runtime data).
- Field values are read with `clojure.lang.Reflector/getInstanceField`, not `(eval '(.-a o))`. Reason: `eval` of a
  form that holds an object embeds it as a constant, which runs `Compiler.emitValue` (K-08) and mixes rules.
- The K-08 constant is `(eval (list (list 'fn [] v)))` inside the `*ns*` binding.
- No rule failed. Next: Slice 2.

## Slices 2–4 implementation (2026-10-19)

- Emitter: `closed-record.impl.jvm/emit [tname fields opts impls]`. `impls` is a vector of
  `[symbol method-forms]` pairs in source order (macro `split-opts+specs`). Returns the full expansion form.
- Generated method params: `this` is the literal symbol `this`; other params are auto-gensyms, or plain
  `k` in `containsKey`/`without` (nested syntax-quote cannot share `k#`). `assoc` uses `gensym` params so
  the constructor `replace` map can name the new value.
- `:record` in method ex-data is `(class this)`; in `map->Name` it is the imported `classname` symbol.
- `cons` branch 3 uses public `clojure.core/assoc` in the loop (no reflection, calls `.assoc`).
- Zero-field types work (`containsKey` emits `false`; `without` has no throw clause).
- `check` uses `satisfies? ClosedRecord`; extra keys `ks` = `(set (map key (-extra-keys x)))`.
- Ad-hoc smoke script (not committed, `/tmp/smoke.clj`) gave identical output on 1.11.4, 1.12.6,
  1.13.0-alpha9 for: table operations, all 7 ex-data paths, cons host exceptions, D-19 NPE, literal +
  `check`, equality/hash formula, 22-field factory + `ArityException`, code constants via `eval`,
  keyword thunks. No reflection warnings from generated code. Slice 5 must turn this into real tests.
- `core_test.clj` smoke test still present (removed in Slice 6). clj-kondo and cljfmt clean.

## Slice 5 implementation (2026-10-19)

- One test namespace: `test/closed_record/defclosedrecord_test.clj` (refers `defclosedrecord`, `check`).
  33 tests total with compiler rules; pass on 1.11.4, 1.12.6, 1.13.0-alpha9; clj-kondo, cljfmt clean.
- Reader literals: `read-string` inside the `*ns*` binding works in the scry CLI (no K-06 loader binding needed).
- V-01 test-unit mapping (acceptance 3), purpose table row → test:
  - `get` → `get-test`; keyword call → `keyword-call-test`; `.field` (declared only) → `field-access-test`.
  - `contains?` → `contains-test`.
  - `assoc`/`conj`/`merge`/`into`/`update` → `assoc-test`/`conj-test`/`merge-test`/`into-test`/`update-test`
    (each: `testing` declared, `testing` undeclared).
  - `dissoc` → `dissoc-test`; `empty` → `empty-test`.
  - `map->Name` → `map-factory-test` (declared: nil + `^long` NPE; undeclared: extra-keys).
  - Reader literal → `reader-literal-declared-key-test`, `reader-literal-undeclared-key-test`.
  - Equality rows → `equal-same-type-test`, `equal-other-type-test`, `equals-java-map-test`, `hash-test`,
    `hash-code-test`; metadata/hidden-key note → `equality-ignores-metadata-and-hidden-keys-test`.
- 7 `ex-data` paths (acceptance 4): assoc undeclared → `assoc-test`; cons undeclared → `conj-test`;
  dissoc declared → `dissoc-test`; empty → `empty-test`; `map->Name` extra → `map-factory-test`;
  `check` extra → `check-test` (also `reader-literal-undeclared-key-test`); `check` other → `check-test`.
- Acceptance 4 basics → `record-basics-test`; V-10 → `big-factories-test`; acceptance 5 → `argument-errors-test`.
- Next: Slice 6 (remove smoke test, docs).

## Slice 6 implementation (2026-10-19)

- Removed `test/closed_record/core_test.clj` (smoke test). Test count now 32 (was 33); CLI exit 0.
- `mementum/state.md` names `defclosedrecord`, `check`, emitter and both test files.
- `dev-tooling.md`: kept only the zero-tests exit rule; removed "Smoke-test lint" section.
- Next: Slice 7 (final verification, then ask user about architecture findings).

## Slice 7 implementation (2026-10-19)

- CLI on 1.11.4, 1.12.6, 1.13.0-alpha9: exit 0, 32 tests, 95 assertions, 0 failures/errors.
- REPL: `(:pass? (scry/run))` true. The REPL had a stale `closed-record.core` (no `check`); run
  `(require 'closed-record.core :reload-all)` first.
- clj-kondo 0/0; cljfmt clean; source namespaces are only `core` and `impl.jvm`; `impl.jvm` does not require `core`.
- Remaining: the architecture findings (lines 58, 109, 151, 199, 243, 329) need user approval before any edit of
  `ramora/` or `mementum/memories/deftype-star-hidden-fields.md`.

<!-- IMPLEMENTATION_BLOCKER: START -->
- blocker: The architecture findings need a user decision before `ramora/` or `mementum/memories/deftype-star-hidden-fields.md` can change (rule from the design).
- required-human-action: Read the architecture findings in implementation.md (lines 58, 109, 151, 199, 243, 329). Approve or reject each edit to `ramora/` and to the memory file.
<!-- IMPLEMENTATION_BLOCKER: END -->
