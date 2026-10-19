# Qualities

## Targets

| Question | Target | Evidence |
|---|---|---|
| Does it represent the problem correctly? | I-01 to I-09 hold. | `verified-by` of each invariant. |
| Can we understand it? | It behaves like `defrecord`, except for closed keys. | Differential tests. Docs list each difference. |
| Can we change it safely? | Host-specific code is in one emitter for each host. Boundaries hold. | Namespace layout (D-13), boundaries in [embodiment](embodiment.md#boundaries). |
| Can it withstand misuse? | A key-set error throws at the call site, with data. | Tests on each `ex-info` `:type` (V-01, V-15). |
| Can it meet runtime demands? | Performance as S-03. | criterium benchmarks (V-11). |
| Can we operate and verify it? | Each invariant has a test. Compiler rules are tested on each Clojure version. | `verified-by`, V-13, CI matrix. |
| Is the complexity justified? | Two public vars, plus the generated factories. | API surface (D-20). |
| Can it stay viable? | Clojure 1.11+. Other hosts can be added. | CI matrix: 1.11, 1.12, 1.13. |

## Trade-offs

- `deftype*` (D-01): `defrecord` parity and performance, but a dependency on an internal special form.
  Mitigation: V-13 on the CI matrix finds changes early.
- Hidden `__extmap` (D-09): all operations stay closed, but the reader does not find extra keys in a literal.
  Print, constants and positional reads drop them. Mitigation: `check`, and the docs.
- Equality as `defrecord` (D-07): parity and `java.util.Map` interop, but `=` is false and `.equals` is true for an
  equal plain map.
- Missing primitive field (D-19): parity, but the error is a host exception with no data.
- No disable switch (D-12): the same behaviour everywhere, but a small cost in error paths only.
- Closed maps in an open-map ecosystem: errors are found early, but some generic map code throws.
  This is intended. The docs must name the common cases: malli default-value transformers and code that adds keys such as `:db/id`.

## Verification plan

| Id | Area | Method |
|---|---|---|
| V-01 | Behaviour table | One test for each row and column of the tables in [purpose](purpose.md#behaviour). |
| V-02 | Differential | The same operation sequence on a closed record and a `defrecord`. Compare the results for permitted operations. |
| V-03 | Property tests | Random `assoc` and `dissoc` sequences. Check I-01 after each step. |
| V-04 | Literals | Round trip (I-06). Literal with extra keys → `check` throws. Literal with missing keys → nil. Positional literal. Print of a literal with extra keys drops them. |
| V-05 | Code constants | A macro that returns a record value compiles and gives an equal record with no extra keys. |
| V-06 | Keyword thunks | Keyword call sites on two closed types at the same site. |
| V-07 | malli | `m/validate` and `m/explain` with `[:map ...]` and `[:map {:closed true} ...]`. Transformers that `assoc` throw. |
| V-08 | nil | Nil field values: lookup, `contains?`, `=`, hash, print and read. |
| V-09 | Type hints | Primitive fields: `assoc`, constructors, `create`. A missing primitive field throws as with `defrecord` (D-19). |
| V-10 | Large types | More than 20 fields: `->Name`, `map->Name`. |
| V-11 | Performance | criterium: lookup, `.field`, `assoc`, `hash`, `=`, against `defrecord`. Pass criterion S-03. |
| V-12 | Equality and hash | `=`, `.equals`, `hash` and `.hashCode` against `defrecord` and against plain maps. Metadata has no effect. |
| V-13 | Compiler rules | Fix K-06 to K-08: the 4-field rule, `create` behaviour, `IType` order in `emitValue`, `defrecord` without `IType`. |
| V-14 | Hidden keys | Literal with extra keys: `count`, `seq`, `=`, hash, print and the `java.util.Map` views show declared keys only. `assoc` and `withMeta` keep the extra keys for `check`. |
| V-15 | `check` | Closed record → returns it. Extra keys → `extra-keys`. Other value → `not-closed-record`. |

## Risks

- R-01 A future Clojure changes `deftype*`, the generation of `create`, or `emitValue`. Likelihood: low. Effect: the emitter must change. Signal: V-13.
- R-02 babashka cannot implement the necessary interfaces. Likelihood: unknown. A spike is necessary before work on that host.
- R-03 Users expect `dissoc` to make a plain map, as with `defrecord`. Mitigation: a clear error message, and the docs.
- R-04 A literal with extra keys loses them when it is printed or made into a constant. After that, `check` passes. Mitigation: call `check` where literals are read, and the docs.
