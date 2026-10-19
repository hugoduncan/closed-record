# Context

## Human environment

- Stakeholders:
  - Clojure developers who use records as internal data types.
  - The library maintainer (one person).
  - AI agents that write code in this project.
- Expectations:
  - It feels like `defrecord`.
  - Errors have clear data.
  - No silent behaviour change.
- Risk: a user can give a closed record to a library that expects an open map. That library can then fail.

## Interacting systems

### Clojure compiler and runtime

- `deftype*` special form. `defrecord` uses it. It is internal, but it has been stable since Clojure 1.3.
- Generated static `create(IPersistentMap)`.
  - The reader uses it for map literals.
  - The compiler uses it to emit record constants (`Compiler.emitValue`).
- Generated alternative constructor with one argument for each declared field. The reader uses it for positional literals.
- `print-method` and `print-dup` for `IRecord` (`core_print.clj`). They print `#ns.Name{...}`.
- Record literal reader (`LispReader.CtorReader`). It requires `*read-eval*` true.

### clojure.core map functions

- `assoc`, `dissoc`, `conj`, `merge`, `into`, `update`, `assoc-in`: go through `assoc`, `without` and `cons`.
- `select-keys` and `into {}`: return plain maps.
- `clojure.walk`: rebuilds a record with `conj` of its own entries. This works when the keys do not change.

### Clojure 1.13 destructuring

- `:keys!` uses `RT.req`. `RT.req` calls `get` with a sentinel value (observed).
- Declared keys are always present. Thus `:keys!` never fails for a declared key.

### malli

- Map schemas validate a record as a map.
- Some transformers add undeclared keys or remove declared keys. With a closed record, these throw. This is intended, and the docs must say it.

### Other libraries

- Some code adds keys to any map (for example `:db/id`). With a closed record, this code throws. This is intended.

### Future hosts

- ClojureScript: `defrecord` is built on `deftype` and protocols. It does not use lookup thunks.
- babashka (SCI): can implement only some host interfaces. Status `assumed`. This needs a spike.

## Constraints

- K-01 Clojure 1.11 is the minimum version on the JVM. Use a newer feature only for a significant reason.
- K-02 The public API must be possible on ClojureScript and babashka. Host-specific code stays behind the macro.
- K-03 No backward compatibility before the first release.
- K-04 Minimal design. Do not add extension points that limit later extension.
- K-05 Performance must be equal to `defrecord`.
- K-06 Compiler layout rule (observed):
  - The compiler generates `create` and the alternative constructors only when the field list ends with exactly
    `__meta __extmap __hash __hasheq`.
  - With three of these fields, the type does not compile.
  - The `deftype` macro does not accept these names. Only `deftype*` accepts them.
- K-07 `create` puts all extra keys into `__extmap`. It does not call user code. Thus a read-time check is not possible (observed).
