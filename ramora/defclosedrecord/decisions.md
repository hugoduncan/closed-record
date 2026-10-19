# Decisions

Each decision has this shape: `⟨subject, choice, rejected, rationale, qualities, commitment⟩`.
The status of each decision is `intended`.

## D-01 Type base

- Choice: emit `deftype*` with the `defrecord` field layout: the declared fields, then `__meta __extmap __hash __hasheq`.
- Rejected:
  - Wrap `defrecord`. `defrecord` generates `assoc` and `without`, and the user cannot replace them.
  - Use `deftype` with the declared fields only. Then there is no metadata, no hash cache, no `create`, no map
    literal and no code constants.
  - Use `deftype` with extra fields that have other names. The constructor arity then changes, and positional literals fail.
- Rationale: K-06. This is the only layout that gives metadata, hash caching, both literal forms and code constants.
- Qualities: + `defrecord` parity, + performance. − It depends on an internal special form.
- Commitment: medium. If `deftype*` changes, only the emitter changes.

## D-02 Closed key set

- Choice: check the key set in `assoc`, `without`, `cons` and `map->Name`. `->Name` has fixed arity.
- Rationale: these are the only paths that can change the key set. `conj`, `merge`, `into` and `update` all use them.
- Qualities: + correctness. The check costs nothing on the declared-key path, because the `case` default branch throws.

## D-03 Failure signal

- Choice: `ex-info` with data `{:type t, :record <class>, :key k}`, or `:keys ks` for a set of keys.
- `t` is one of:
  - `:closed-record/undeclared-key`
  - `:closed-record/declared-key-dissoc`
  - `:closed-record/empty`
  - `:closed-record/extra-keys`
- Rejected: `IllegalArgumentException` and `UnsupportedOperationException`. They do not carry data.
- Qualities: + diagnosability, + portability (`ex-info` exists on all hosts).

## D-04 `dissoc` of an undeclared key

- Choice: return the record unchanged.
- Rationale: the key cannot be present, so the result is correct. A plain map does the same.

## D-05 `empty`

- Choice: throw `:closed-record/empty`.
- Rationale: a record without its declared keys does not exist. `defrecord` also throws.

## D-06 Record identity

- Choice: implement `IRecord`, `IObj`, `IHashEq`, `ILookup`, `IKeywordLookup`, `IPersistentMap`,
  `java.util.Map` (read-only) and `Serializable`.
- Rationale: `record?` is true. Printing, `print-dup` and constant emission come from Clojure with no extra code.

## D-07 Equality and hash

- Choice: the same as `defrecord`. Two records are equal only if they have the same class and equal fields.
  Metadata is ignored. A record is not equal to a plain map. The hash is cached in `__hash` and `__hasheq`.

## D-08 `map->Name`

- Choice: an extra key throws `:closed-record/extra-keys`. A missing key gives nil.
- Rationale: `map->Name` is the explicit construction path. It must enforce the key set.

## D-09 Reader literals and printing

- Choice:
  - Print as `#ns.Name{...}`.
  - Read map literals and positional literals.
  - Extra keys in a map literal go into `__extmap`. No operation reads `__extmap`.
  - The public function `check` throws `:closed-record/extra-keys` if `__extmap` is not empty.
- Rejected:
  - (i) Ignore the extra keys and give no way to find them.
  - (ii) Expose `__extmap` as `defrecord` does. This breaks I-01.
  - Positional printing only. It does not close the gap, because map literals read in all cases.
- Rationale: K-07 prevents a read-time check. With this choice, every operation keeps I-01, and the gap can be found.
- Qualities: + correctness of operations, + round trip of printed output. − Hand-written literal errors are not found automatically.

## D-10 Keyword lookup

- Choice: `valAt` uses `case` on the declared keywords. `getLookupThunk` returns a thunk for each declared field, as `defrecord` does.
- Rationale: K-05. Keyword call sites read fields directly.

## D-11 Value validation

- Choice: none. Tests show that the type works with malli map schemas and with nil field values.
- Rationale: K-04. Validation is a separate concern.

## D-12 No disable switch

- Choice: the checks always run.
- Rationale: they cost nothing on permitted paths (D-02). A switch adds variability and allows different behaviour in test and in production.

## D-13 Platform layout

- Choice:
  - Start with the JVM.
  - Put the public namespace `closed-record.core` in a `.cljc` file.
  - Put host-specific emitters in internal namespaces behind the macro.
- Rationale: K-02. ClojureScript and babashka can add emitters and keep the same API.

## D-14 Clojure version

- Choice: Clojure 1.11 minimum.
- Rationale: K-01. All the mechanisms that the type uses exist in 1.3 and later. The 1.13 directives are not necessary.

## D-15 Syntax

- Choice: the same syntax as `defrecord`: name, fields with type hints, options, protocol and interface implementations.
  `->Name` and `map->Name` are generated, also for more than 20 fields.
- Reserved field names: `__meta`, `__extmap`, `__hash`, `__hasheq`.
- Rationale: O-02.

## D-16 Verification

- Choice:
  - Tests for each row of the behaviour table.
  - Differential tests against `defrecord`.
  - Tests for malli and nil values.
  - Print and read round-trip tests.
  - Tests for code constants.
  - criterium benchmarks against `defrecord`.
- Rationale: S-01 to S-04.
