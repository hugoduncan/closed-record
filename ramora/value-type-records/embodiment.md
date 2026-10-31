# Value-type records embodiment

## Status

Not implemented. value-type does not have `defvaluetype*` yet (VT-R1).

## Elements that change

| Element | Change |
|---|---|
| `closed-record.core` | The docstring of `defclosedrecord` describes the option. |
| `closed-record.impl.jvm` | `check-options` accepts `:value-type` and checks its value. The option is removed from the `deftype*` options. In value mode: resolve `defvaluetype*` (VT-D4), the VT-D5 layout, hash methods with no cache (VT-D6), and `withMeta` with no hash fields (VT-D7). |
| `.github/workflows/ci.yml` | The Valhalla job of VT-D10. |
| `deps.edn` | A test alias with value-type at a pinned Git SHA (VT-R3). The base `:deps` do not change. |
| Tests | VT-V1 to VT-V7. VT-V1 runs in the ordinary `:test` alias. |
| `dev/bench` | VT-V8. |
| `doc/` and `README.md` | The option, the differences table, the runtime dependency and the hash cost. |

## Permitted dependencies

These are added to the defclosedrecord
[permitted dependencies](../defclosedrecord/embodiment.md), in value mode only:

- `impl.jvm` → `value-type.core/defvaluetype*`, through `requiring-resolve`
  at expansion time.
- Generated code → `value-type.core/defvaluetype*` and the public and
  run-time names that its expansion uses.

No other closed-record namespace depends on value-type.

## Generated type in value mode

```text
(value-type.core/defvaluetype* ns/Name ns.Name
  [declared-field* __meta __extmap ^int __hash ^int __hasheq]
  :implements [... as the ordinary type ...]   ; not IType (K-08)
  ...)
```

## Verification limits

The design puts value output on CI (VT-D10). Until the job exists, value
output is not verified. CI verifies only one Valhalla build and Linux x64.
VT-V8 is manual.
