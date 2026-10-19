# Qualities

## Targets

| Question | Target | Evidence |
|---|---|---|
| Does it represent the problem correctly? | I-01 to I-08 hold. | Behaviour tests, invariant tests. |
| Can we understand it? | It behaves like `defrecord`, except for closed keys. | Differential tests. Docs list each difference. |
| Can it withstand misuse? | A key error throws at the call site, with data. | Tests on each `ex-info` `:type`. |
| Can it meet runtime demands? | Same performance as `defrecord` (K-05). | criterium benchmarks (S-03). |
| Can we change it safely? | Host-specific code is in one emitter for each host. | Namespace layout (D-13). |
| Is the complexity justified? | Two public vars, plus the generated factories. | API surface. |
| Can it stay viable? | Clojure 1.11+. Other hosts can be added. | CI matrix: 1.11, 1.12, 1.13. |

## Trade-offs

- `deftype*` (D-01): `defrecord` parity and performance, but a dependency on an internal special form.
  Mitigation: the CI matrix finds changes early.
- Hidden `__extmap` (D-09): all operations stay closed, but the reader does not find extra keys in a literal.
  Mitigation: `check`, and the docs.
- No disable switch (D-12): the same behaviour everywhere, but a small cost in error paths only.
- Closed maps in an open-map ecosystem: errors are found early, but some generic map code throws.
  This is intended. The docs must name the common cases: malli default-value transformers and code that adds keys such as `:db/id`.

## Verification plan

| Area | Method |
|---|---|
| Behaviour table | One test for each row and column of the table in [purpose](purpose.md#behaviour). |
| Differential | The same operation sequence on a closed record and a `defrecord`. Compare the results for permitted operations. |
| Property tests | Random `assoc` and `dissoc` sequences. Check I-01 after each step. |
| Literals | Round trip (I-06). Literal with extra keys → `check` throws. Literal with missing keys → nil. Positional literal. |
| Code constants | A macro that returns a record value compiles and gives an equal record. |
| Keyword thunks | Keyword call sites on two closed types at the same site. |
| malli | `m/validate` and `m/explain` with `[:map ...]` and `[:map {:closed true} ...]`. Transformers that `assoc` throw. |
| nil | Nil field values: lookup, `contains?`, `=`, hash, print and read. |
| Type hints | Primitive fields: `assoc`, constructors, `create`. |
| Large types | More than 20 fields: `->Name`, `map->Name`. |
| Performance | criterium: lookup, `.field`, `assoc`, `hash`, `=`, against `defrecord`. |

## Risks

- R-01 A future Clojure changes `deftype*` or the generation of `create`. Likelihood: low. Effect: the emitter must change.
- R-02 babashka cannot implement the necessary interfaces. Likelihood: unknown. A spike is necessary before work on that host.
- R-03 Users expect `dissoc` to make a plain map, as with `defrecord`. Mitigation: a clear error message, and the docs.
