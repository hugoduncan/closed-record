# Value-type records embodiment

## Status

Implemented. The tests use value-type at
the Git SHA of the `:value-type-test` alias (VT-R3).

## Elements that change

| Element | Change |
|---|---|
| `closed-record.core` | The docstring of `defclosedrecord` describes the option. It also has one sentence on `:non-nil` with `:value-type true`, and one sentence on metadata and map literals in value mode. |
| `closed-record.impl.jvm` | `check-options` accepts `:value-type` and checks its value. The option is removed from the type options. One representation description holds the mode differences. In value mode: resolve `defvaluetype*` (VT-D4), a `require` of `value-type.core` first in the expansion, the VT-D5 layout, hash methods with no cache (VT-D6), and the closed shape (VT-D13 to VT-D15): the option `:value-type/closed true`, no `IMeta` or `IObj`, no `meta` or `withMeta`, `assoc` with the declared fields only, and `-extra-keys` gives nil. Field metadata, `:non-nil` included, goes into the `defvaluetype*` field vector with no change, and nowhere else (VT-I6). Non-nil fields need no emitter change. |
| `.github/workflows/ci.yml` | Job `Value-type tests (Temurin 21)`: the value-type suite (identity) and the Clojure 1.11 case. Job `Value-type tests (Valhalla)` (VT-D10): the value-type suite (value output), with `--enable-preview` and `--add-exports=java.base/jdk.internal.value=ALL-UNNAMED`. Each scry step uploads `.scry-results/` on failure: `scry-value-type-temurin-21`, `scry-value-type-clj-1.11`, `scry-value-type-valhalla`. |
| `deps.edn` | A test alias with value-type at a pinned Git SHA (VT-R3). The base `:deps` do not change. |
| Tests | VT-V1 to VT-V7 and VT-V9 to VT-V12. VT-V11: `test/value-type/closed_record/value_type/closed_shape_test.clj`. VT-V12: `test/value-type/closed_record/value_type/flattening_test.clj`. VT-V9: `test/value-type/closed_record/value_type/non_nil_test.clj`. The VT-I6 check of VT-V2: `test/value-type/closed_record/value_type/representation_test.clj`. VT-V10: `test/runtime/closed_record/value_type_option_test.clj` and the `:non-nil` form of the expansion fixture (`test/runtime/closed_record/expansion_fixture.clj`). VT-V1 has three placements: the ordinary suite `test/runtime` (expansion fixture, option errors, absent-var subprocess); the value-type suite `test/value-type` (does-not-load subprocess); `test/value-type-clj-1.11` (Clojure 1.11 case). The VT-V3 shared checks are in `test/runtime/closed_record/checks/` and load without value-type. In value mode they exclude the metadata and hidden-key checks (placeholder `metadata-and-hidden-keys?`); the value-type suite does not run V-14. The value-type suite requires the property `closed-record.test.representation`. |
| `dev/bench` | VT-V8: entry namespace `closed-record.bench.value-type`, which no other namespace requires. `dev/bench/timing/run.sh` reads `BENCH_ALIASES` and `BENCH_JVM_OPTS`. |
| `doc/` and `README.md` | The option, the differences table, the runtime dependency, the hash cost and non-nil fields. No metadata in value mode, map literals with undeclared keys, the user-method limit, `value-type.value-classes` and flattening (VT-K8). The value-type version requirement (`4ed8d0a3`; an earlier value-type has no closed shape) and the full-expansion limit (`:not-installed` on all JVMs, also on identity fallback). |

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
  :implements [... as the ordinary type ...]   ; not IType (K-08), not IObj
  ... user options ...
  :value-type/closed true
  ...)
```

## Verification limits

- CI verifies value output on one Valhalla build (`27-jep401ea3+1-1`) and on
  Linux x64 only.
- Identity fallback is verified on Temurin 21 only.
- VT-V12 verifies flattening for two field shapes (`[^long x]`,
  `[^int x ^int y]`) on one Valhalla build only. Flattening is a JVM choice
  (VT-K8).
- VT-V8 is manual and informational. Its results apply only to the machine
  and the JDK of the run (`doc/performance.md`).
