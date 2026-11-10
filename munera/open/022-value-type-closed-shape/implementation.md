# Implementation

## Design check against value-type `4ed8d0a3` (2026-11-10)

- value-type task 022 (`e8e0c407`) is closed. Its contract
  (`ramora/deftype-star-design/public-contract.md`, "Closed record shape";
  the `defvaluetype*` docstring) agrees with VT-D13. D10 is firm, D9 is
  provisional. No Ramora decision changed; Ramora now records the pin, the
  receiving expansion, `:removed-field-access`, `value-type.value-classes`
  and the closed rows of VT-K8.
- New since `edc3f79`: a closed form is a receiving form also on identity
  fallback; the value-type fallback policy is read at first use;
  `value-type.value-classes` (`auto`, `off`); array adapters load lazily
  (the JVM flags do not change).
- Probe (EA3, `--enable-preview`, `jdk.internal.value` export,
  `defvaluetype*` forms written by hand): closed `[^long x]` and
  `[^int x ^int y]` have only the declared fields and `:flat? true`; closed
  `[^long x ^long y]` gives `:flat? false`; record shape `[^long x]` keeps
  `__meta __extmap` and gives `:flat? false`. A non-nil `__meta` and `create`
  with an undeclared key both throw `IllegalArgumentException`, with no
  wrapper.
- Current suites with the pin overridden to `4ed8d0a3` (`-Sdeps`
  `:override-deps`), no emitter change:
  - `test/value-type`, Temurin 21, identity: 89 tests, 435 assertions pass;
  - `test/value-type-clj-1.11`: 1 test, 7 assertions pass;
  - `test/value-type`, EA3, value: 87 tests, 436 assertions pass.
