# Plan

## Approach

Change the value-mode emitter so a `:value-type true` closed record uses
the value-type closed shape (VT-D13 to VT-D15). Pin value-type `4ed8d0a3`
first and confirm the current suites pass with no emitter change (the
probe in `implementation.md` already shows this). Then change the emitter,
then the tests, then CI and documentation.

Key decisions:

- All mode differences live in the `representation` description in
  `closed-record.impl.jvm`. The new differences join it: the interface
  list (no `IObj`), the `meta` and `withMeta` methods, the `assoc`
  constructor arguments, the `-extra-keys` body, and the option
  `:value-type/closed true`. `generated-methods` and `base-interfaces`
  get no `value-type?` test (VT-D8). The ordinary values of the
  description give the current expansion unchanged (VT-I2); the expansion
  fixture is the guard.
- `support/representation` is `:identity` in the Temurin 21 value-type
  job, so it cannot select the value-mode check exclusions. Use the
  record options (`:value-type true`) or a new `define-checks` key to
  select them. Decide at implementation time; record the choice in
  `implementation.md`.
- VT-V12 selects itself on the expected representation `:value`
  (`expectation/representation`). It does not skip: without the
  `jdk.internal.value` export or with `value-type.value-classes=off` it
  fails.
- Pin rule: this task pins `4ed8d0a3` only. A different pin is a design
  change (Ramora first, then design).
- Documentation and Ramora embodiment changes come after the code and
  tests pass, so the recorded state is final.

## Risks

- The `define-checks` exclusion mechanism is left to the plan by design.
  A wrong choice touches many shared-check rows in
  `ramora/defclosedrecord/embodiment/artifacts.md`; keep the change
  small and record it.
- The closed form is a receiving form also on identity fallback, so
  VT-V5 (policy) and VT-V7 (AOT) exercise a new value-type path. The
  probe passed the suites, but the emitter change may surface new
  identity-fallback behaviour. Mitigate: run the Temurin 21 value-type
  suite after each slice.
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
   `#deftype-forms` contract link; verify the suites pass unchanged.
2. **Emitter**: the closed shape in value mode via the representation
   description; expansion fixture unchanged; VT-V2 extended.
3. **Shared checks**: value-mode exclusions (VT-V3, VT-V4); new VT-V11
   closed-shape check on both representations; VT-V7 and VT-V9 updates.
4. **Flattening and CI**: VT-V12; the export in `ci.yml` and
   `doc/development.md`.
5. **Documentation**: README, `doc/value-type-records.md`, docstring,
   `doc/API.md`, CHANGELOG, Ramora embodiments, `mementum/state.md`,
   `munera/plan.md`.
6. **Final verification**: all suites, quality checks, CI green.
