# Decisions: type structure

## D-01 Type base

- Subject: the form that the JVM emitter generates.
- Choice: emit `deftype*` with the `defrecord` field layout: the declared fields, then
  `__meta __extmap ^int ^:unsynchronized-mutable __hash ^int ^:unsynchronized-mutable __hasheq`.
- Rejected:
  - Wrap `defrecord`. `defrecord` generates `assoc` and `without`, and the user cannot replace them.
  - Use `deftype` with the declared fields only. Then there is no metadata, no hash cache, no `create`, no map
    literal and no code constants.
  - Use `deftype` with extra fields that have other names. The constructor arity then changes, and positional literals fail.
- Rationale: K-06. This is the only layout that gives metadata, hash caching, both literal forms and code constants.
- Assumptions: `deftype*` and the K-06 layout rule stay stable (R-01).
- Qualities: + `defrecord` parity, + performance. − It depends on an internal special form.
- Scope: JVM emitter.
- Commitment: medium. If `deftype*` changes, only the emitter changes.

## D-06 Record identity

- Subject: the interfaces of the generated type.
- Choice: implement `IRecord`, `IObj`, `IHashEq`, `ILookup`, `IKeywordLookup`, `IPersistentMap`,
  `java.util.Map` (read-only), `Serializable` and the `ClosedRecord` protocol (D-17).
  Do not implement `clojure.lang.IType` (K-08).
- Rejected:
  - Omit `IRecord`. Then `record?` is false, and printing and constant emission need extra code.
  - Add `IType`, as `deftype` does. Then `Compiler.emitValue` uses its `IType` path, not `create`.
- Rationale: the same set as `defrecord`. `record?` is true. Printing, `print-dup` and constant emission come from
  Clojure with no extra code.
- Qualities: + `defrecord` parity, + minimality.
- Scope: JVM emitter. Other hosts use their own record identity.
- Commitment: high for `record?` and printing. Medium for the interface list.

## D-07 Equality and hash

- Subject: `equiv`, `equals`, `hasheq`, `hashCode`.
- Choice: the same as `defrecord`. The entries are the declared entries only (I-02).
  - `=` (`equiv`): true only if the other value has the same class and equal declared fields. A record is not `=` to a plain map.
  - `.equals`: `APersistentMap/mapEquals`. It follows the `java.util.Map` contract. Thus `.equals` is true for a
    `java.util.Map` with the same entries.
  - `hash` (`hasheq`): `type-hash XOR APersistentMap/mapHasheq`.
  - `.hashCode`: `APersistentMap/mapHash`. It is the same as for an equal plain map.
  - Both hashes are cached in `__hash` and `__hasheq`. Metadata does not have an effect.
- Rejected: `.equals` that also requires the same class. It breaks the `java.util.Map` contract and S-02 parity.
- Rationale: O-02. Code that puts records into Java collections must see no change.
- Qualities: + parity, + interop. − `=` and `.equals` disagree for a plain map. This comes from `defrecord`.
- Scope: all hosts. Each host follows its own `defrecord`.
- Commitment: high.

## D-10 Keyword lookup

- Subject: `valAt` and `getLookupThunk`.
- Choice: `valAt` uses `case` on the declared keywords. `getLookupThunk` returns a thunk for each declared field, as `defrecord` does.
- Rejected: a static map from key to field index. It adds a hash lookup and a cast on each read.
- Rationale: K-05. Keyword call sites read fields directly.
- Qualities: + performance.
- Scope: JVM emitter.
- Commitment: low.

## D-15 Syntax

- Subject: the `defclosedrecord` macro syntax.
- Choice: the same syntax as `defrecord`: name, fields with type hints, options, protocol and interface implementations.
  `->Name` and `map->Name` are generated, also for more than 20 fields.
- Reserved field names: `__meta`, `__extmap`, `__hash`, `__hasheq`. The macro rejects them.
- Rejected: a new syntax with key specifications (optional keys, defaults). It breaks O-02 and K-04.
- Rationale: O-02.
- Scope: all hosts.
- Commitment: high.

## D-21 Method size

- Subject: the size of the generated `assoc`, `without`, `containsKey` and `entryAt`, and their throw paths.
- Choice: each method has the `defrecord` shape, within the size of the `defrecord` method plus the helper call.
  - `assoc`: `condp identical?` on the declared keywords. `without`: `identical?` tests on the declared keywords.
  - `containsKey` and `entryAt` call `(.valAt this k this)`, with `this` as the not-found value.
  - The throw paths of `assoc` and `without` call the private helper `closed-record.core/key-error` through its var.
    The helper returns the `ex-info` of D-03; the call site throws it. Generated code → this helper is a permitted
    dependency.
- Rejected: `case` in these methods (the `tableswitch` size depends on the keyword hashes, K-10). The inline throw
  form (cold path, but it adds bytecode to the method). A public helper in `core` (D-20, principle 9). A helper in
  `impl.*` (generated code → `impl.*` is forbidden; the emitter is for expansion time only).
- Rationale: K-05, K-09, K-10. C2 does not inline a hot method above `FreqInlineSize` (325 bytes, K-09). A `case` on
  keywords is large (K-10). Observed (task 012): `P::assoc` with `case` (995 bytes) is not inlined, `R::assoc`
  (228 bytes) is inlined. After the change `P::assoc` is 217 bytes and is inlined. The helper location: the earlier
  boundaries permit no location for it. A private var in `core` is the smallest change: the generated code already
  depends on `core` (D-17), and the public API does not change.
- Assumptions: the generated code can refer to `closed-record.core`, because the macro is in that namespace (as
  D-17). The C2 inline limits stay near their current defaults (R-05).
- Qualities: + performance, + `defrecord` parity. − The generated code depends on a private var of `core`. AOT
  classes of user code refer to the var by name.
- Scope: JVM emitter. The helper in `core` is portable.
- Commitment: medium, as D-17. The helper is an internal mechanism: it is not in the public API of D-20, and only the
  generated code calls it. A change of the helper name or arguments changes the JVM emitter and the private helper in
  `core` together, in one release. User classes that were AOT-compiled with an earlier version refer to the helper by
  name, as they refer to the D-17 protocol interface. Before the first release, K-03 applies. After it, a change adds
  a helper with a new name and keeps the old helper, so that the earlier AOT classes still work. The message and the
  data of D-03 do not change.
