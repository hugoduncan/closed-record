# Context

## Human environment

- Stakeholders:
  - Clojure developers who use records as internal data types.
  - The library maintainer (one person).
  - AI agents that write code in this project.
- Activities: define types, construct and update records, print and read records, find key errors.
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
    It calls `create` with `PersistentArrayMap.create((java.util.Map) r)`. Thus the `java.util.Map` view sets the
    entries of a constant.
  - `emitValue` checks `IType` before `IRecord`. A type with `IType` does not use `create` (K-08).
- Generated alternative constructors (K-06):
  - One argument for each declared field. The reader uses it for positional literals.
  - The declared fields, then `__meta` and `__extmap`. The hash caches start at 0.
- `getBasis()`: every `deftype*` type has it, with or without the hidden fields (E-02).
- `print-method` and `print-dup` for `IRecord` (`core_print.clj`). They print `#ns.Name{...}`.
  With `*verbose-defrecords*` false, `print-dup` prints `#ns.Name[...]`.
- Record literal reader (`LispReader.CtorReader`). It requires `*read-eval*` true.

### clojure.core map functions

- `assoc`, `dissoc`, `conj`, `merge`, `into`, `update`, `assoc-in`: go through `assoc`, `without` and `cons`.
- `select-keys` and `into {}`: return plain maps.
- `clojure.walk`: rebuilds a record with `conj` of its own entries. This works when the keys do not change.

### Clojure 1.13 destructuring

- `:keys!` uses `RT.req`. `RT.req` calls `get` with a sentinel value (observed).
- Declared keys are always present. Thus `:keys!` never fails for a declared key, and it fails for an undeclared key.

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
  Basis: maintainer choice, for adoption. Most users run 1.11 or later (`assumed`).
- K-02 The public API must be possible on ClojureScript and babashka. Host-specific code stays behind the macro.
  Basis: evolution expectations in [purpose](purpose.md#evolution-expectations).
- K-03 No backward compatibility before the first release. Basis: `AGENTS.md` (`¬compat(backward)`).
- K-04 Minimal design. Do not add extension points that limit later extension. Basis: `AGENTS.md` (`design(x)`).
- K-05 Performance must be equal to `defrecord`. Basis: O-03. Metric: S-03.
- K-06 Compiler layout rule (observed, E-01, E-02, E-07):
  - The compiler generates `create` and the alternative constructors only when the field list ends with exactly
    `__meta __extmap __hash __hasheq`.
  - `__hash` and `__hasheq` must have the hint `^int`. The alternative constructors pass `int` 0 for them.
    Without the hint, the JVM verifier rejects the class (`VerifyError`).
  - With three of these fields, the compiler emits an alternative constructor with bad bytecode. The form compiles,
    and the JVM verifier rejects the class when it is initialised (`VerifyError`). A test must load the class.
  - The `deftype` macro does not accept these names. Only `deftype*` accepts them.
- K-07 `create` puts all extra keys into `__extmap`. It does not call user code. Thus a read-time check is not possible
  (observed, E-02, E-04).
- K-08 The type must not implement `clojure.lang.IType`. Otherwise `Compiler.emitValue` builds constants through the
  `IType` path, not `create` (observed, E-03). `defrecord` does not implement `IType` (observed, 1.11.4 and 1.13.0-alpha6).
- K-09 C2 inline limit (observed, OpenJDK 25.0.2, `PrintInlining`). C2 does not inline a hot method with a code length
  above `FreqInlineSize` (default 325 bytes). It reports "hot method too big".
- K-10 Keyword `case` size (observed, Clojure 1.11.4; `prep-hashes` in `core.clj`). A `case` on keywords dispatches on
  `Util.hash` with a `tableswitch` over the range of the hashes (after a shift and a mask if the range is 8192 or
  more; a `lookupswitch` if no mask makes the hashes distinct). Its size depends on the hashes, not only on the key
  count (`:a :b :c`: 192 entries).

## Evidence

Source: `clojure-1.13.0-alpha6-sources.jar`. Line numbers are for that version.
The tests that observed these facts on 1.11.4 and 1.13.0-alpha6 must be kept as V-13 (see [qualities](qualities.md#verification-plan)).

| Id | Location | Fact |
|---|---|---|
| E-01 | `Compiler.java` 8600–8605 | Trailing `__meta __extmap __hash __hasheq` fields are counted as `altCtorDrops`. |
| E-02 | `Compiler.java` 8805–8860 (`emitStatics`) | `getBasis` always. `create` only with dropped fields. `create` reads each declared key with `valAt k nil`, then `without k`. Primitive fields: `CHECKCAST` to the box class, then unbox. |
| E-03 | `Compiler.java` 5373–5401 (`emitValue`) | `IType` branch first. `IRecord` branch calls `create` with `PersistentArrayMap.create((java.util.Map) value)`. |
| E-04 | `LispReader.java` 1454–1500 (`readRecord`) | Requires `*read-eval*`. Map form calls `create`. |
| E-05 | `RT.java` 764–770 (`req`) | `get` with sentinel `REQ_NOT_FOUND`. |
| E-06 | `core_deftype.clj` 175–200 | `defrecord` `__hash`, `__hasheq` are `^:unsynchronized-mutable`. `equals` is `mapEquals`. `hashCode` is `mapHash`. `hasheq` is `type-hash XOR mapHasheq`. |
| E-07 | `core_deftype.clj` 313 (`validate-fields`) | The `deftype` macro rejects `__meta __hash __hasheq __extmap`. |
