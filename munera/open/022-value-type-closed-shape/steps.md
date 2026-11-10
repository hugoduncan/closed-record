# Steps

## Slice 1: Pin value-type 4ed8d0a3

- [x] Change the `:value-type-test` alias in `deps.edn` to explicit HTTPS
      `:git/url` and full SHA `4ed8d0a3c4ce8ed6a74607da312fd33a8c80dea2`.
- [x] Update the README deps snippet to the same SHA.
- [x] Update the two value-type links in `doc/value-type-records.md`
      (field contract `doc/usage.md#portable-non-nil-fields`, array
      documentation `doc/usage.md`) to the same SHA; add the closed
      contract link to `doc/usage.md#deftype-forms` at that SHA.
- [x] In `ramora/value-type-records/embodiment.md` ("Differences from
      the design"), change the pin sentence to the new pin, for example:
      "The pinned value-type (`4ed8d0a3`) has the closed record shape
      (VT-R6). closed-record does not use it yet. Until task 022:". The
      six entries do not change; they stay true until Slices 3 to 5.
- [x] Verify no `edc3f79` remains outside Munera, mementum and Git
      history (`git grep edc3f79`); keep the `8bb7c4f` evidence link in
      `doc/performance.md`.
- [x] Run the value-type suite on Temurin 21 and the Clojure 1.11 case;
      both pass with no emitter change.

## Slice 2: Value-mode exclusions (no emitter change)

- [x] Add the placeholder var
      `closed-record.checks.support/metadata-and-hidden-keys?` (value
      `true`, docstring) to `test/runtime/closed_record/checks/support.clj`.
      `transform` replaces the symbol with `false` when `:record-opts` has
      `:value-type true`, else with `true`. Update the `support.clj`
      namespace docstring (source conventions) and the `define-checks`
      docstring.
- [x] Exclude in value mode (VT-V3) with the guard forms of `plan.md`:
  - `checks/defclosedrecord.clj`: whole tests
    `reader-literal-undeclared-key-test`,
    `equality-ignores-metadata-and-hidden-keys-test`, `metadata-test`,
    `extra-key-retention-test` (top-level `when`); parts: the literal
    `testing` block of `check-test`, the `with-meta` and `meta` of
    `serializable-test` (the round trip of `r` stays).
  - `checks/differential.clj`: the `:with-meta` generator in `gen-step`
    and the `judge :meta` call.
  - `checks/equality.clj`: whole test `metadata-test`.
  - `checks/literal.clj`: require `closed-record.checks.support :as
    support`; whole test `extra-key-literal-print-test` (top-level
    `when`). Add the private macro `if-metadata-and-hidden-keys`
    (`[then & [else]]`, form in `plan.md`) before its first use; it reads
    the placeholder at expansion time (the `constant` macro reads at
    expansion time, so `when` cannot stop the read).
    - `value-constant-test`: body
      `(if-metadata-and-hidden-keys <ordinary> <value>)`; `<ordinary>` is
      the current `let` with its three assertions; `<value>` reads
      `"#$P.T{:a 1 :n 2}"` (no reader metadata) and keeps the `=` and
      `class` assertions (V-05), with no `meta` assertion.
    - `extra-key-constant-test`: wrap the whole `deftest` at top level,
      `(if-metadata-and-hidden-keys (deftest …))`, no `else`.
- [x] Remove `test/value-type/closed_record/value_type/hidden_key_test.clj`
      (V-14 has no value-mode part). Keep the ordinary caller.
- [x] VT-V7 (`aot_test.clj`): remove the `:meta` entry of the load script
      and of the expected map.
- [x] VT-V9 (`non_nil_test.clj`): remove the "with-meta keeps the fields"
      part.
- [x] Run the ordinary suite (no change in results; the placeholder is
      `true`), the value-type suite on Temurin 21 and the Clojure 1.11
      case. All pass.

## Slice 3: Emitter and closed-shape checks

- [x] Move the new mode differences into the `representation`
      description in `src/closed_record/impl/jvm.clj`: interface list
      (no `IObj`), `meta` and `withMeta` methods, `assoc` constructor
      arguments, `-extra-keys` body, and the option
      `:value-type/closed true` emitted after `:implements` and the user
      options, before the methods.
- [x] In value mode: no `IMeta` or `IObj`; `assoc` calls the constructor
      with the declared fields only; `-extra-keys` returns nil; no
      generated reference to `__meta`, `__extmap`, `__hash` or
      `__hasheq` in generated method and factory bodies (field vector
      keeps all four; user `specs*` methods may name them).
- [x] Confirm `generated-methods` and `base-interfaces` have no
      `value-type?` test (VT-D8), and no closed-record constructor-check
      or exception wrapping is emitted (VT-D13, VT-D15).
- [x] Run the ordinary suite; the expansion fixture is unchanged (VT-I2).
- [x] Extend `value-expansion-test` in `representation_test.clj` (VT-V2):
      the `:value-type/closed true` option is present; no `__meta`,
      `__extmap`, `__hash` or `__hasheq` in the `specs` of the
      `defvaluetype*` form (fixture `Q [a b]`, no user methods) and in
      the `->Q` and `map->Q` `defn` forms of the expansion; no
      `clojure.lang.IMeta` or `clojure.lang.IObj` in the `:implements`
      vector.
- [x] VT-V4: in the `:value` branch of `representation-test`, check that
      the instance fields of `VP` are `a` and `b` only (VT-I7).
- [x] VT-V7 (`aot_test.clj`): in the loaded JVM, check that the type is
      not `IObj` (VT-I9).
- [x] Add VT-V11 in `test/value-type/closed_record/value_type/closed_shape_test.clj`
      (`closed-record.value-type.closed-shape-test`), on both
      representations:
  - (a) the type is not `IMeta` or `IObj`; `meta` gives nil; `with-meta`
    gives `ClassCastException`;
  - (b) a map literal with an undeclared key throws at read, with
    `IllegalArgumentException` in the cause chain; `map->Name` with an
    undeclared key throws `:closed-record/extra-keys`;
  - (c) the main constructor `[Object Object Object Object int int]`
    and the `__meta __extmap` constructor `[Object Object Object Object]`
    of the fixture `[a b]`, each from `getConstructor` with the exact
    parameter types and called with `Constructor/newInstance` (form in
    `plan.md`): nil for both gives an instance of the fixture class; a
    non-nil `__meta` (`{:m 1}`) or a non-nil `__extmap` (`{:x 1}`, `{}`)
    throws `InvocationTargetException` whose `getCause` is an
    `IllegalArgumentException`;
  - (d) `check` returns each record that `->Name`, `map->Name`, `assoc`
    and literals make;
  - (e) on identity fallback, `__meta` and `__extmap` hold nil after each
    of these paths; on value output, the instance fields are the
    declared fields (VT-I7). The branch follows
    `expectation/representation`.
- [x] Run the value-type suite on Temurin 21 and the Clojure 1.11 case.
      Both pass (local JDK 25, identity).
- [x] Run the value-type suite on Valhalla `27-jep401ea3+1-1` (local JDK
      `~/.local/jdks/jdk-27.jdk`, with `JAVA_CMD`; or the CI Valhalla job).

## Slice 4: Flattening test and CI

- [x] Add VT-V12 in `test/value-type/closed_record/value_type/flattening_test.clj`
      (`closed-record.value-type.flattening-test`): `:flat? true` for
      arrays of `[^long x]` and `[^int x ^int y]` value records, and the
      instance fields of each are the declared fields (VT-I7). The test is
      defined only when `expectation/representation` is `:value`
      (top-level `when`); no skip. The test body gets
      `value-type.array/into-array` and `array-info` through
      `requiring-resolve` (no load of `value-type.array` on Temurin 21).
- [x] Add `--add-exports=java.base/jdk.internal.value=ALL-UNNAMED` to
      the Valhalla test command in `.github/workflows/ci.yml` and in
      `doc/development.md`.
- [x] Run the full value-type suite on Valhalla `27-jep401ea3+1-1`
      (value output) and on Temurin 21. VT-V12 passes on Valhalla. The
      `[^long x]` fixture of VT-V12 is the evidence for acceptance
      criterion 4 (the shape of `Cell [^long v]`: only the declared
      field; the array is flat).

## Slice 5: Documentation

- [x] README: Approach (no metadata in value mode); Differences list (no
      metadata; undeclared-key literal throws at read; user method
      reading `__meta`/`__extmap` fails at definition on value output);
      Requirements (`value-type.value-classes` next to
      `value-type.fallback`; `off` selects identity fallback); "Arrays
      and flattening" (64-bit limit of VT-K8, a flat example, flattening
      is a JVM choice).
- [x] `doc/value-type-records.md`: behaviour table rows (`__meta`,
      `__extmap`, metadata, map literal), user-method limit,
      `value-type.value-classes`, flattening section.
- [x] `defclosedrecord` docstring: one sentence on metadata and on map
      literals in value mode; regenerate `doc/API.md`.
- [x] `CHANGELOG.md` `## [Unreleased]`: no metadata in value mode;
      undeclared-key literals throw; records with at most 64 bits of
      fields can flatten.
- [x] Ramora `value-type-records/embodiment.md`:
  - "Status": VT-D13 to VT-D15 are implemented; remove "except VT-D13 to
    VT-D15 (task 022)".
  - Remove the whole "Differences from the design" section (the
    pin sentence that Slice 1 changed and the six "until task 022"
    entries). After
    this task none applies. If a difference stays, keep only that entry
    and record it in `implementation.md`.
  - "Elements that change": the `closed-record.impl.jvm` row (today
    "`withMeta` with no hash fields") states the closed shape: the option
    `:value-type/closed true`, no `IMeta`/`IObj`, no `meta`/`withMeta`,
    `assoc` with the declared fields only, `-extra-keys` gives nil. The
    Tests row adds VT-V11 (`closed_shape_test.clj`), VT-V12
    (`flattening_test.clj`) and the value-mode guards of the VT-V3
    shared checks (no V-14 in the value-type suite). The `ci.yml` row
    names the `jdk.internal.value` export of the Valhalla job. The
    `closed-record.core` row (today "describes the option" and "one
    sentence on `:non-nil`") adds the docstring sentence on metadata and
    map literals in value mode. The `doc/` and `README.md` row (today
    "the option, the differences table, the runtime dependency, the hash
    cost and non-nil fields") adds: no metadata in value mode, map
    literals with undeclared keys, the user-method limit,
    `value-type.value-classes` and flattening (VT-K8).
  - "Generated type in value mode": add `:value-type/closed true`; the
    `:implements` comment says no `IObj`.
  - "Verification limits": add that VT-V12 verifies flattening for two
    field shapes (`[^long x]`, `[^int x ^int y]`) on one Valhalla build
    only; flattening is a JVM choice (VT-K8).
- [x] Ramora `defclosedrecord/embodiment/artifacts.md`: in the
      `test/value-type/` row, add VT-V9, VT-V11 and VT-V12 and state that
      the value-type suite does not run V-14; in the `checks/*.clj` and
      `checks/support.clj` rows, add the placeholder
      `metadata-and-hidden-keys?` (from `:record-opts`) and the
      value-mode guards.
- [x] Update `mementum/state.md` and `munera/plan.md`.

## Slice 6: Final verification

- [x] Ordinary suites pass on Clojure 1.11, 1.12 and 1.13; expansion
      fixture unchanged.
- [x] Full value-type suite passes on Temurin 21 and Valhalla; Clojure
      1.11 case passes. (Local identity run and 1.11 case pass; Valhalla
      can run locally with `~/.local/jdks/jdk-27.jdk` or in CI.)
- [x] Reconcile the local branch with `origin/020-value-type-records`
      (diverged: ahead 63, behind 128), then push (human decision).
  - Done 2026-11-10: the user force pushed; origin is at `5e84d9f0`.
- [x] Quality checks pass: format, lint, prose, API freshness, links.
- [x] All CI jobs pass on the last commit that changes files outside the
      task directory.
