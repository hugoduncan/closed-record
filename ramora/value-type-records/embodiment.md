# Value-type records embodiment

## Status

Implemented, except VT-D13 to VT-D15 (task 022). The tests use value-type at
the Git SHA of the `:value-type-test` alias (VT-R3).

## Differences from the design

The pinned value-type (`edc3f79`) has no closed record shape. value-type
`4ed8d0a3` has it (VT-R6). Until task 022:

- The form has no `:value-type/closed` option. Value output keeps `__meta` and
  `__extmap`, so value records do not flatten (VT-K8). VT-I7 and VT-I8 do not
  hold.
- The type implements `IMeta` and `IObj`. `withMeta` calls the main
  constructor with the declared fields, `__meta` and `__extmap`, as `assoc`
  does. VT-I9 does not hold.
- A map literal with an undeclared key is read; the key is hidden in
  `__extmap`, and `check` throws `:closed-record/extra-keys` (I-02, I-07,
  I-08).
- VT-V3 runs the metadata and hidden-key checks. VT-V11 and VT-V12 do not
  exist.
- The README and `doc/value-type-records.md` describe this behaviour.

## Elements that change

| Element | Change |
|---|---|
| `closed-record.core` | The docstring of `defclosedrecord` describes the option. It also has one sentence on `:non-nil` with `:value-type true`. |
| `closed-record.impl.jvm` | `check-options` accepts `:value-type` and checks its value. The option is removed from the type options. One representation description holds the mode differences. In value mode: resolve `defvaluetype*` (VT-D4), a `require` of `value-type.core` first in the expansion, the VT-D5 layout, hash methods with no cache (VT-D6), and `withMeta` with no hash fields (VT-D7). Field metadata, `:non-nil` included, goes into the `defvaluetype*` field vector with no change, and nowhere else (VT-I6). Non-nil fields need no emitter change. |
| `.github/workflows/ci.yml` | Job `Value-type tests (Temurin 21)`: the value-type suite (identity) and the Clojure 1.11 case. Job `Value-type tests (Valhalla)` (VT-D10): the value-type suite (value output). Each scry step uploads `.scry-results/` on failure: `scry-value-type-temurin-21`, `scry-value-type-clj-1.11`, `scry-value-type-valhalla`. |
| `deps.edn` | A test alias with value-type at a pinned Git SHA (VT-R3). The base `:deps` do not change. |
| Tests | VT-V1 to VT-V7, VT-V9 and VT-V10. VT-V9: `test/value-type/closed_record/value_type/non_nil_test.clj`. The VT-I6 check of VT-V2: `test/value-type/closed_record/value_type/representation_test.clj`. VT-V10: `test/runtime/closed_record/value_type_option_test.clj` and the `:non-nil` form of the expansion fixture (`test/runtime/closed_record/expansion_fixture.clj`). VT-V1 has three placements: the ordinary suite `test/runtime` (expansion fixture, option errors, absent-var subprocess); the value-type suite `test/value-type` (does-not-load subprocess); `test/value-type-clj-1.11` (Clojure 1.11 case). The VT-V3 shared checks are in `test/runtime/closed_record/checks/` and load without value-type. The value-type suite requires the property `closed-record.test.representation`. |
| `dev/bench` | VT-V8: entry namespace `closed-record.bench.value-type`, which no other namespace requires. `dev/bench/timing/run.sh` reads `BENCH_ALIASES` and `BENCH_JVM_OPTS`. |
| `doc/` and `README.md` | The option, the differences table, the runtime dependency, the hash cost and non-nil fields. |

## Permitted dependencies

These are added to the defclosedrecord
[permitted dependencies](../defclosedrecord/embodiment.md), in value mode only:

- `impl.jvm` → `value-type.core/defvaluetype*`, through `requiring-resolve`
  at expansion time.
- Generated code → `value-type.core` (a `require`), `defvaluetype*` and the
  public and run-time names that its expansion uses.

No other closed-record namespace depends on value-type.

## Generated type in value mode

```text
(require 'value-type.core)
(value-type.core/defvaluetype* ns/Name ns.Name
  [declared-field* __meta __extmap ^int __hash ^int __hasheq]
  :implements [... as the ordinary type ...]   ; not IType (K-08)
  ...)
```

## Verification limits

- CI verifies value output on one Valhalla build (`27-jep401ea3+1-1`) and on
  Linux x64 only.
- Identity fallback is verified on Temurin 21 only.
- VT-V8 is manual and informational. Its results apply only to the machine
  and the JDK of the run (`doc/performance.md`).
