# Plan

## Approach

Add one test namespace `closed-record.equality-test` in `test/closed_record/equality_test.clj` with
differential tests against `defrecord` (Scope 3, 4, 5) and fixed-value cross-type tests (Scope 6).

Key decisions:

- Types local to the namespace: `(defclosedrecord E [a b])` and `(defrecord DE [a b])`, as in the design
  Observed facts and Scope 6.
- Helper local to the namespace. It takes two factories and a function `f`. `f` gets a factory and returns a
  vector of booleans. The helper asserts the two vectors are `=`; a failure reports both vectors. Pattern after
  `is-diff` in `test/closed_record/field_value_test.clj`, but with plain boolean vectors (design Decisions).
- Scope 3: one differential test per row of the first Context table. `f` builds `x = (factory a1 2)` and
  `y = (factory a2 2)` and returns `[(= x y) (= (hash x) (hash y)) (.equals x y) (= (.hashCode x) (.hashCode y))]`.
- Scope 4: one differential test per case of the second Context table except the metadata row. `r = (factory 1 2)`,
  `m = {:a 1 :b 2}`. The `hash` case compares `hash` of `r` with `hash` of `m` inside `f` (not across record types).
  Include the reverse calls (`(.equals m r)`, `HashMap.equals`) — they reach emitter `Map` methods
  (implementation.md, pass 2 notes).
- Scope 5: two differential metadata tests, each returning the four results of the first table. Build both values
  before any `hash` or `.hashCode` call, so the `withMeta` hash-cache copy cannot hide a mutation.
- Scope 6: fixed expected values, closed value first only: `(not (= (->E 1 2) (->DE 1 2)))`,
  `(.equals (->E 1 2) (->DE 1 2))`, `.hashCode` equal.
- Acceptance 3: mutation checks on `src/closed_record/impl/jvm.clj` after tests pass. Targets:
  `equiv`, `equals`/`mapEquals`, `hasheq`/`mapHasheq`, `hashCode`/`mapHash`, `cached-hash`, `withMeta` cache copy.
  Record each mutation and the failing assertion in `implementation.md`; follow the mutation rules in
  `mementum/knowledge/testing-generated-types.md` (Test design). If no mutation breaks an item, record the reason.
- Emitter fixes only if a test exposes a defect (Scope 7). None expected: Observed facts hold on 1.11.4 and 1.13.

## Risks

- A mutation may fail at namespace load (`VerifyError`, NPE) and give no signal; such a change does not count and
  another mutation must be found (mutation rules).
- `0.0`/`-0.0` and `(int 1)`/`1` behaviour could vary across Clojure versions; the design facts were re-checked on
  1.11.4 and 1.13, so the risk is low. If the test matrix diverges, record it in `implementation.md` and stop for
  a design decision.
- The hash-cache copy in `withMeta` can mask metadata mutations if values are constructed in the wrong order; the
  Scope 5 construction order guards this.

## Slice order

1. Test namespace, types, differential helper.
2. Scope 3 field-pair tests.
3. Scope 4 plain-map tests.
4. Scope 5 metadata tests.
5. Scope 6 cross-type tests.
6. Full test matrix, lint, format.
7. Mutation checks and `implementation.md` entries (Acceptance 3).
8. `mementum/state.md`: V-12 done.
