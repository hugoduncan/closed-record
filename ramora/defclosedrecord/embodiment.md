# Embodiment

## Artifacts

| Artifact | Responsibility |
|---|---|
| `src/closed_record/core.cljc` | Public API: the `defclosedrecord` macro and the `check` function. |
| `src/closed_record/impl/jvm.clj` | JVM emitter. Builds the `deftype*` form and the factories. |
| `test/closed_record/*_test.clj` | Behaviour, differential, malli, nil, literal and constant tests. |
| `dev/bench/` | criterium benchmarks against `defrecord`. |
| `deps.edn` | Clojure 1.11 dependency. Test and bench aliases. |

Dependency direction: `core` → `impl.*`. An `impl` namespace does not depend on `core`.

## Public interface

```clojure
(defclosedrecord Name [field*] opts* specs*)   ; same syntax as defrecord
(check r)                                      ; → r, or throw :closed-record/extra-keys
```

- Generated vars: `->Name` and `map->Name`.
- Error data: see [D-03](decisions.md#d-03-failure-signal).

## Generated type

```text
deftype* ns/Name ns.Name
  [declared-field* __meta __extmap ^int __hash ^int __hasheq]
  :implements [IRecord IHashEq IObj ILookup IKeywordLookup
               IPersistentMap java.util.Map Serializable
               closed_record.core.ClosedRecord   ; internal, for check
               user-interfaces*]
```

The compiler generates the following (K-06):

- a constructor with the declared fields only (positional literals and `->Name`),
- a constructor with all fields,
- `create(IPersistentMap)` (map literals and code constants),
- `getBasis()`.

## Method behaviour

| Method | Behaviour |
|---|---|
| `valAt k nf` | `case` on the declared keywords. Default → `nf`. |
| `getLookupThunk k` | A thunk for each declared field. The thunk checks the class identity. Undeclared key → nil. |
| `assoc k v` | Declared key → new instance. Other key → throw `undeclared-key`. |
| `without k` | Declared key → throw `declared-key-dissoc`. Other key → `this`. |
| `cons e` | Map entry, vector pair or map → `assoc` for each entry. |
| `empty` | Throw `empty`. |
| `count` | Number of declared fields. A constant. |
| `seq`, `iterator` | Declared fields only, in declaration order. |
| `containsKey`, `entryAt` | Declared keys only. |
| `equiv`, `equals` | Same class and equal fields. |
| `hasheq`, `hashCode` | Cached. Calculated from the declared entries only. |
| `meta`, `withMeta` | Use `__meta`. |
| `java.util.Map` mutators | Throw `UnsupportedOperationException`, as `defrecord` does. |
| `extraKeys` (internal) | Returns `__extmap`. Only `check` calls it. |

`__extmap` is set only by `create` (from a map literal). No method other than `extraKeys` reads it.
`assoc` and `withMeta` copy it to the new instance, so `check` still finds it.

## Allocation

- Macro expansion time: argument checks (reserved names, duplicate fields) and emitter selection by host.
- Class load time: the compiler generates the class, the constructors and `create`.
- Run time: key checks in `assoc`, `without`, `cons` and `map->Name`.
