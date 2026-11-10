# Plan

## Approach

Change the value-mode emitter so a `:value-type true` closed record uses
the value-type closed shape (VT-D13 to VT-D15). Pin value-type `4ed8d0a3`
first and confirm the current suites pass with no emitter change (the
probe in `implementation.md` already shows this). Then remove the
metadata and hidden-key uses from the value-type suite, then change the
emitter and add the new checks, then CI and documentation. Each slice
ends with all suites green.

Key decisions:

- All mode differences live in the `representation` description in
  `closed-record.impl.jvm`. The new differences join it: the interface
  list (no `IObj`), the `meta` and `withMeta` methods, the `assoc`
  constructor arguments, the `-extra-keys` body, and the option
  `:value-type/closed true`. `generated-methods` and `base-interfaces`
  get no `value-type?` test (VT-D8). The ordinary values of the
  description give the current expansion unchanged (VT-I2); the expansion
  fixture is the guard.
- Value-mode exclusion mechanism (VT-V3): an in-source condition in the
  shared checks.
  - Placeholder: a new var `closed-record.checks.support/metadata-and-hidden-keys?`
    in `support.clj`, value `true`, with a docstring (so clj-kondo lints
    the check sources). `transform` replaces the symbol with a literal
    boolean, as it replaces `closed-record.checks.support/representation`.
  - Source: the `:record-opts` of the `define-checks` call. The value is
    `false` when `:record-opts` has `:value-type true`, else `true`. No
    new `define-checks` key. `support/representation` cannot select the
    exclusions: it is `:identity` also in the Temurin 21 value-type job,
    and the closed shape applies on both representations (VT-I9).
  - Whole test: a top-level `(when support/metadata-and-hidden-keys? (deftest …))`,
    as `serializable-test` does today. A guard inside the test body is
    not sufficient: scry fails a test with no assertion. Exception: a
    test that reads a literal at expansion time uses the macro below.
  - Part of a test: `(when support/metadata-and-hidden-keys? …)` inside
    the test body (the `check-test` literal `testing` block, the `meta`
    assertion of `serializable-test`), or a guarded element of a
    collection (the `:with-meta` generator of `gen-step` and the
    `judge :meta` call in `checks/differential.clj`).
  - Literals read at macroexpansion time (the `constant` macro of
    `checks/literal.clj`): `eval` of `(when false …)` still expands the
    macros in its body, so a `when` guard does not stop the read. In
    value mode the reader throws `IllegalArgumentException` for
    `"^{:m 1} #$P.T{:a 1 :n 2}"` (`T` is not `IMeta`) and for
    `"#$P.T{:a 1 :n 2 :x 3}"` (undeclared key). `checks/literal.clj`
    gets a private two-branch macro, defined before its first use:
    ```clojure
    (defmacro ^:private if-metadata-and-hidden-keys
      [then & [else]]
      (if support/metadata-and-hidden-keys? then else))
    ```
    It reads the placeholder at expansion time (`transform` replaces the
    symbol also in the `defmacro` body, so it is a literal boolean) and
    gives only one branch. The compiler does not see the other branch, so
    its `constant` forms are not read. `checks/literal.clj` requires
    `closed-record.checks.support :as support`.
    - `value-constant-test`: the test body is
      `(if-metadata-and-hidden-keys <ordinary> <value>)`. `<ordinary>` is
      the current `let` (the metadata literal and the three assertions,
      the `meta` assertion included). `<value>` is the same `let` with the
      literal `"#$P.T{:a 1 :n 2}"` and with no `meta` assertion. Value
      mode keeps the V-05 value constant check (VT-K7: the constant comes
      from `create` with the Map view), so the test has assertions in
      both modes. The ordinary assertions do not change.
    - `extra-key-constant-test`: the macro wraps the whole `deftest` at
      top level, with no `else`:
      `(if-metadata-and-hidden-keys (deftest extra-key-constant-test …))`.
      In value mode it gives nil, so no test is defined. A guard in the
      test body would give a test with no assertion.
  - `checks/hidden_key.clj` (V-14) has no part that applies in value mode.
    Remove its value-type caller,
    `test/value-type/closed_record/value_type/hidden_key_test.clj`. The
    ordinary caller does not change.
  - In the ordinary suite the placeholder is `true`, so the ordinary
    tests do not change.
- Slice order for green slices: Slice 2 removes all metadata and
  hidden-key uses from the value-type suite while the emitter is
  unchanged. Slice 3 changes the emitter and adds the checks that need
  the closed shape (VT-V2, VT-V4 field check, VT-V7 `IObj` check,
  VT-V11).
- New test files in `test/value-type/closed_record/value_type/`:
  - VT-V11: `closed_shape_test.clj`
    (`closed-record.value-type.closed-shape-test`), on both
    representations; part (e) selects its branch on
    `expectation/representation`. The fixture record has the untagged
    declared fields `[a b]`.
    - Part (c), constructor call: get each constructor with
      `getConstructor` and the exact parameter types, and call it with
      `Constructor/newInstance`. The constructors keep their descriptors
      on both representations (value-type `doc/usage.md`, closed shape).
      - The main constructor: `[Object Object Object Object int int]`
        (`a b __meta __extmap __hash __hasheq`); hash arguments `(int 0)`.
      - The `__meta __extmap` constructor: `[Object Object Object Object]`.
        It delegates to the main constructor; `create` also calls the
        main constructor (VT-K7).
    - Part (c), assertions, for each of the two constructors:
      - nil for `__meta` and `__extmap` gives an instance of the fixture
        class. Thus a wrong parameter list fails the test.
      - A non-nil `__meta` (`{:m 1}`), and a non-nil `__extmap` (`{:x 1}`
        and `{}`), each throw `InvocationTargetException`, and its
        `getCause` is an `IllegalArgumentException`. Do not accept an
        `IllegalArgumentException` at top level or deeper in the cause
        chain: `newInstance` itself throws it for a wrong argument count
        or type. Do not use `clojure.lang.Reflector/invokeConstructor`: it
        unwraps the exception and also throws `IllegalArgumentException`
        for an arity with no constructor.
  - VT-V12: `flattening_test.clj`
    (`closed-record.value-type.flattening-test`). A top-level
    `(when (= :value (expectation/representation)) (deftest …))` defines
    the test only for value output (scry fails a test with no
    assertion). The test body gets `value-type.array/into-array` and
    `value-type.array/array-info` through `requiring-resolve`, so the
    namespace does not load `value-type.array` on Temurin 21. It does not
    skip: without the `jdk.internal.value` export or with
    `value-type.value-classes=off` it fails.
- Acceptance criterion 4 evidence: the VT-V12 `[^long x]` fixture, which
  has the shape of `Cell [^long v]`. `flattening_test.clj` checks for
  each fixture that the instance fields are the declared fields (VT-I7)
  and that the array is flat. No separate `Cell` check.
- VT-V4 field check: in the `:value` branch of `representation-test`
  (`representation_test.clj`): the declared fields of `VP` (`a b`) are
  the instance fields. Value output only.
- VT-V2 scope: the check walks the `specs` of the `defvaluetype*` form of
  the fixture `Q [a b]` (no user methods, so all `specs` are generated)
  and the two factory `defn` forms of the expansion (`->Q`, `map->Q`).
  The `IMeta`/`IObj` check reads the `:implements` vector of the
  `defvaluetype*` form.
- Pin rule: this task pins `4ed8d0a3` only. A different pin is a design
  change (Ramora first, then design).
- Documentation and Ramora embodiment changes come after the code and
  tests pass, so the recorded state is final. Exception: Slice 1 changes
  the pin sentence of "Differences from the design" in
  `ramora/value-type-records/embodiment.md` to the new pin, so the
  embodiment states the current pin after each slice and no `edc3f79`
  stays outside Munera, mementum and Git history after Slice 1.

## Risks

- The exclusion mechanism touches the `define-checks` and shared-check
  rows in `ramora/defclosedrecord/embodiment/artifacts.md`, and the
  `test/value-type/` row (no V-14). Keep the change small and record it.
- The closed form is a receiving form also on identity fallback, so
  VT-V5 (policy) and VT-V7 (AOT) exercise a new value-type path. The
  probe passed the suites, but the emitter change may surface new
  identity-fallback behaviour. Mitigate: run the Temurin 21 value-type
  suite after each slice; each slice ends green.
- VT-V12 depends on JVM flattening behaviour of Valhalla
  `27-jep401ea3+1-1` (VT-K8, 64-bit limit). The probe confirmed
  `:flat? true` for both fixtures; a CI JVM difference would fail
  acceptance criterion 4 and requires a Ramora change first.
- Missing `jdk.internal.value` export in the CI Valhalla command makes
  VT-V12 fail (by design). The export must land in `ci.yml` in the same
  slice as VT-V12.

## Slice order

1. **Pin**: value-type `4ed8d0a3` in `deps.edn`, README deps snippet and
   the two links in `doc/value-type-records.md`; add the
   `#deftype-forms` contract link; the pin sentence of the value-type
   embodiment "Differences from the design"; verify the suites pass
   unchanged.
2. **Value-mode exclusions** (no emitter change): the placeholder in
   `define-checks`; the VT-V3 exclusions in the shared checks; remove the
   value-type V-14 caller; remove the VT-V7 metadata read and the VT-V9
   `with-meta` part.
3. **Emitter and closed-shape checks**: the closed shape in value mode
   via the representation description; expansion fixture unchanged;
   VT-V2 extended; VT-V4 field check; VT-V7 `IObj` check; VT-V11.
4. **Flattening and CI**: VT-V12; the export in `ci.yml` and
   `doc/development.md`.
5. **Documentation**: README, `doc/value-type-records.md`, docstring,
   `doc/API.md`, CHANGELOG, Ramora embodiments, `mementum/state.md`,
   `munera/plan.md`.
6. **Final verification**: all suites, quality checks, CI green.
