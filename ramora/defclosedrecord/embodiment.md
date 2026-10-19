# Embodiment

## Artifacts

| Artifact | Responsibility |
|---|---|
| `src/closed_record/core.cljc` | Public API: the `defclosedrecord` macro and `check`. Internal protocol `ClosedRecord` (D-17). |
| `src/closed_record/impl/jvm.clj` | JVM emitter. Builds the `deftype*` form and the factories. |
| `test/closed_record/*_test.clj` | Behaviour, differential, equality, malli, nil, literal and constant tests. |
| `test/closed_record/compiler_rules_test.clj` | Tests that fix K-06 to K-08 (V-13). |
| `dev/bench/` | criterium benchmarks against `defrecord`. |
| `deps.edn` | Clojure 1.11 dependency. Test and bench aliases. |

## Boundaries

| Side | Contents |
|---|---|
| Public | `defclosedrecord`, `check`, generated `->Name` and `map->Name`, the `ex-data` of D-03. |
| Internal | `closed-record.impl.*`, the `ClosedRecord` protocol, the fields `__meta __extmap __hash __hasheq`. |

Permitted dependencies:

- User code → public names only.
- `core` → `impl.*`. The macro calls the emitter at expansion time.
- Generated code → `closed-record.core/ClosedRecord` and `clojure.lang`.

Forbidden dependencies:

- User code → `impl.*`. User code must not extend `ClosedRecord`.
- `impl.*` → `core`. An emitter does not require `core`. It emits the protocol name only as a symbol.

## Public interface

```clojure
(defclosedrecord Name [field*] opts* specs*)   ; same syntax as defrecord
(check x)   ; → x, or throw :closed-record/extra-keys, or throw :closed-record/not-closed-record (D-18)
```

- Generated vars: `->Name` and `map->Name`.
- Error data: see [D-03](decisions/failure-and-validation.md#d-03-failure-signal).

## Generated type

```text
deftype* ns/Name ns.Name
  [declared-field* __meta __extmap
   ^int ^:unsynchronized-mutable __hash
   ^int ^:unsynchronized-mutable __hasheq]
  :implements [IRecord IHashEq IObj ILookup IKeywordLookup
               IPersistentMap java.util.Map Serializable
               closed_record.core.ClosedRecord   ; protocol interface, D-17
               user-interfaces*]
  ; not IType (K-08)
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
| `java.util.Map` views | `size`, `get`, `containsKey`, `keySet`, `values`, `entrySet`: declared entries only (I-09). |
| `equiv` | Same class and equal declared fields (D-07). |
| `equals` | `APersistentMap/mapEquals` (D-07). |
| `hasheq` | Cached. `type-hash XOR APersistentMap/mapHasheq` (D-07). |
| `hashCode` | Cached. `APersistentMap/mapHash` (D-07). |
| `meta`, `withMeta` | Use `__meta`. |
| `java.util.Map` mutators | Throw `UnsupportedOperationException`, as `defrecord` does. |
| `-extra-keys` (`ClosedRecord`) | Returns `__extmap`. Only `check` calls it. |

## State

| State | Owner | Mutability | Rules |
|---|---|---|---|
| Declared fields | the instance | immutable | Set by a constructor. |
| `__meta` | the instance | immutable | Set by `withMeta` (new instance). |
| `__extmap` | the instance | immutable | Set only by `create` from a map literal (I-07). `assoc` and `withMeta` copy it to the new instance, so `check` still finds it. Only `-extra-keys` gives its value (I-02). |
| `__hash`, `__hasheq` | the instance | unsynchronized mutable cache | 0 means not calculated. A race can calculate the same value twice. This is benign, as with `defrecord`. |

## Allocation

- Macro expansion time: argument checks (reserved names, duplicate fields) and emitter selection by host.
- Class load time: the compiler generates the class, the constructors and `create`.
- Run time: key checks in `assoc`, `without`, `cons` and `map->Name`.
