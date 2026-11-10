# Steps

## Slice 1: Pin value-type 4ed8d0a3

- [ ] Change the `:value-type-test` alias in `deps.edn` to explicit HTTPS
      `:git/url` and full SHA `4ed8d0a3c4ce8ed6a74607da312fd33a8c80dea2`.
- [ ] Update the README deps snippet to the same SHA.
- [ ] Update the two value-type links in `doc/value-type-records.md`
      (field contract `doc/usage.md#portable-non-nil-fields`, array
      documentation `doc/usage.md`) to the same SHA; add the closed
      contract link to `doc/usage.md#deftype-forms` at that SHA.
- [ ] Verify no `edc3f79` remains outside Munera, mementum and Git
      history (`git grep edc3f79`); keep the `8bb7c4f` evidence link in
      `doc/performance.md`.
- [ ] Run the value-type suite on Temurin 21 and the Clojure 1.11 case;
      both pass with no emitter change.

## Slice 2: Emitter (value mode only)

- [ ] Move the new mode differences into the `representation`
      description in `src/closed_record/impl/jvm.clj`: interface list
      (no `IObj`), `meta` and `withMeta` methods, `assoc` constructor
      arguments, `-extra-keys` body, and the option
      `:value-type/closed true` emitted after `:implements` and the user
      options, before the methods.
- [ ] In value mode: no `IMeta` or `IObj`; `assoc` calls the constructor
      with the declared fields only; `-extra-keys` returns nil; no
      generated reference to `__meta`, `__extmap`, `__hash` or
      `__hasheq` in generated method and factory bodies (field vector
      keeps all four; user `specs*` methods may name them).
- [ ] Confirm `generated-methods` and `base-interfaces` have no
      `value-type?` test (VT-D8), and no closed-record constructor-check
      or exception wrapping is emitted (VT-D13, VT-D15).
- [ ] Run the ordinary suite; the expansion fixture is unchanged (VT-I2).
- [ ] Extend `representation_test.clj` (VT-V2): the
      `:value-type/closed true` option is present, no hidden-field
      references in generated bodies, the type is not `IMeta` or `IObj`.
- [ ] Run the value-type suite on Temurin 21.

## Slice 3: Shared checks and suite updates

- [ ] Choose and implement the value-mode exclusion mechanism in
      `define-checks` (`test/runtime/closed_record/checks/support.clj`):
      record options or a new key; record the choice in
      `implementation.md`.
- [ ] Exclude in value mode (VT-V3): in `defclosedrecord.clj`
      `reader-literal-undeclared-key-test`,
      `equality-ignores-metadata-and-hidden-keys-test`, `metadata-test`,
      `extra-key-retention-test`, the literal part of `check-test`, the
      metadata part of `serializable-test`; in `differential.clj` the
      `:with-meta` step and `:meta` comparison; in `equality.clj`
      `metadata-test`; all of `hidden_key.clj`; in `literal.clj`
      `extra-key-literal-print-test`, `extra-key-constant-test` and the
      reader metadata/`meta` of `value-constant-test`.
- [ ] VT-V4: check that the instance fields are the declared fields.
- [ ] Add VT-V11: the closed shape on both representations.
- [ ] VT-V7 (`aot_test.clj`): remove the metadata read in the loaded
      JVM; check the loaded type is not `IObj` (VT-I9).
- [ ] VT-V9 (`non_nil_test.clj`): remove the `with-meta` part.
- [ ] Run the value-type suite on Temurin 21 and the Clojure 1.11 case.
- [ ] Update the shared-check and `define-checks` rows in
      `ramora/defclosedrecord/embodiment/artifacts.md` if they changed.

## Slice 4: Flattening test and CI

- [ ] Add VT-V12: `:flat? true` for arrays of `[^long x]` and
      `[^int x ^int y]` value records; selects itself on
      `expectation/representation` `:value`; no skip.
- [ ] Add `--add-exports=java.base/jdk.internal.value=ALL-UNNAMED` to
      the Valhalla test command in `.github/workflows/ci.yml` and in
      `doc/development.md`.
- [ ] Run the full value-type suite on Valhalla `27-jep401ea3+1-1`
      (value output); VT-V12 passes; acceptance criterion 4 holds
      (`Cell [^long v]` has only `v`; its array is flat).

## Slice 5: Documentation

- [ ] README: Approach (no metadata in value mode); Differences list (no
      metadata; undeclared-key literal throws at read; user method
      reading `__meta`/`__extmap` fails at definition on value output);
      Requirements (`value-type.value-classes` next to
      `value-type.fallback`; `off` selects identity fallback); "Arrays
      and flattening" (64-bit limit of VT-K8, a flat example, flattening
      is a JVM choice).
- [ ] `doc/value-type-records.md`: behaviour table rows (`__meta`,
      `__extmap`, metadata, map literal), user-method limit,
      `value-type.value-classes`, flattening section.
- [ ] `defclosedrecord` docstring: one sentence on metadata and on map
      literals in value mode; regenerate `doc/API.md`.
- [ ] `CHANGELOG.md` `## [Unreleased]`: no metadata in value mode;
      undeclared-key literals throw; records with at most 64 bits of
      fields can flatten.
- [ ] Ramora `value-type-records/embodiment.md`: remove stale
      "Differences from the design" (the export and VT-V7 entries);
      update elements, generated type, verification limits.
- [ ] Ramora `defclosedrecord/embodiment/artifacts.md`: add VT-V9,
      VT-V11 and VT-V12 to the `test/value-type/` row.
- [ ] Update `mementum/state.md` and `munera/plan.md`.

## Slice 6: Final verification

- [ ] Ordinary suites pass on Clojure 1.11, 1.12 and 1.13; expansion
      fixture unchanged.
- [ ] Full value-type suite passes on Temurin 21 and Valhalla; Clojure
      1.11 case passes.
- [ ] Quality checks pass: format, lint, prose, API freshness, links.
- [ ] All CI jobs pass on the last commit that changes files outside the
      task directory.
