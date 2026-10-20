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
  Include the reverse calls. `(.equals m r)` and `HashMap.equals` call the `Map` methods of `r`. `(= m r)` calls no
  emitter method: `APersistentMap.equiv` returns false because `r` is an `IPersistentMap` and not a
  `MapEquivalence`. Mutation M7 (`base-interfaces`) covers it.
- Scope 5: two differential metadata tests, each returning the four results of the first table. Build both values
  before any `hash` or `.hashCode` call, so the `withMeta` hash-cache copy cannot hide a mutation.
- Scope 6: fixed expected values, closed value first only: `(not (= (->E 1 2) (->DE 1 2)))`,
  `(.equals (->E 1 2) (->DE 1 2))`, `.hashCode` equal.
- Acceptance 3: mutation checks on `src/closed_record/impl/jvm.clj` after tests pass. See "Mutation checks".
- Emitter fixes only if a test exposes a defect (Scope 7). None expected: Observed facts hold on 1.11.4 and 1.13.

## Mutation checks (Acceptance 3)

Items: each Scope 3 row (4), each Scope 4 case (7), each Scope 5 case (2) and the Scope 6 case (1). Each Scope 3,
4 and 5 item is one differential assertion. The Scope 6 item is one test with three fixed-value assertions. An item
is covered if at least one recorded mutation makes one of its assertions fail.

Rules:

- Targets in `src/closed_record/impl/jvm.clj`: `equiv`, `equals`, `hasheq`, `hashCode` (in `generated-methods`) and
  `base-interfaces`. Each mutation follows the mutation rules in `mementum/knowledge/testing-generated-types.md`
  (Test design).
- A Slice 7 step can use more than one mutation. Run each mutation alone, from an unchanged `src`.
  `implementation.md` gets one entry for each mutation: the change and the items whose assertion failed.
- A mutation can change more than one method. It counts as one change only if each part alone does not make the
  item fail. `implementation.md` then records each part and the result of each part alone. Only M12 does this.
- `withMeta` is not a target. It calculates no hash. If it does not copy the caches, the hash is calculated again
  with the same result. Scope 5 makes both values before a `hash` call, so no `withMeta` change can make a Scope 5
  case fail. A `withMeta` change does not count. `metadata-test` (`test/closed_record/defclosedrecord_test.clj`)
  covers the cache copy.
- `cached-hash` is not a target. A change to it changes when a hash is calculated, not the hash value. `assoc-test`
  and `metadata-test` (V-01) cover the caches.
- Hash method bodies must stay `int`: wrap a `bit-xor` in `(int ...)`. A `long` body fails at `deftype*` compile.

Mutations. Each change was checked in a scratch copy (Clojure 1.11.4, 2026-10-19) with a probe of all items:

| Id | Method | Change | Items that fail |
|---|---|---|---|
| M1 | `equiv` | field compare `=` → `.equals` | Scope 3 rows `1N`, `(int 1)`, `-0.0` |
| M2 | `equiv` | field compare `(or (= a b) (and (number? a) (number? b) (== a b)))`, `a` and `b` the two field values | Scope 3 row `1.0` |
| M3 | `equals` | `(mapEquals this o)` → `(.equiv this o)` | Scope 3 rows `1N`, `(int 1)`, `-0.0`; Scope 4 `.equals`, `HashMap`, `HashSet`; Scope 6 |
| M4 | `hasheq` | `mapHasheq` → `mapHash` | Scope 3 row `-0.0` |
| M5 | `hasheq` | remove the type-hash `bit-xor` | Scope 4 `hash` |
| M6 | `hashCode` | `mapHash` → `mapHasheq` | Scope 3 row `-0.0`; Scope 4 `.hashCode`, `HashSet`; Scope 6 |
| M7 | `base-interfaces` | add `clojure.lang.MapEquivalence` | Scope 4 `=` (`(= m r)` becomes true) |
| M8 | `equiv` | also require `(= __meta (meta o))` | Scope 5 both cases |
| M9 | `equals` | `(and (mapEquals this o) (= __meta (meta o)))` | Scope 5 both cases |
| M10 | `hasheq` | add `(hash __meta)` to the `bit-xor` | Scope 5 both cases |
| M11 | `hashCode` | `(int (bit-xor (mapHash this) (hash __meta)))` | Scope 5 both cases |
| M12 | `hasheq` and `equiv` | M5, and `equiv` for another class returns `(and (map? o) (= (into {} this) o))` | Scope 4 `contains?`. Scope 4 `=` and `hash` also fail, but do not count (one part alone makes each fail) |

M12 parts alone: the `hasheq` part (M5) fails only Scope 4 `hash`. The `equiv` part fails only Scope 4 `=`.
`(contains? #{m} r)` calls `hasheq` of `r`, then `(.equiv r m)`, so it fails only with both parts.

Coverage: Scope 3 rows `1N`, `(int 1)`: M1, M3. Row `-0.0`: M1, M3, M4, M6. Row `1.0`: M2. Scope 4 `=`: M7.
`.equals`, `HashMap`: M3. `.hashCode`: M6. `hash`: M5. `contains?`: M12. `HashSet`: M3, M6. Scope 5 both
cases: M8–M11. Scope 6: M3, M6. Each item has a mutation, so no reason entry is expected.

## Risks

- A mutation may fail at namespace load (`VerifyError`, NPE) and give no signal; such a change does not count and
  another mutation must be found (mutation rules).
- `0.0`/`-0.0` and `(int 1)`/`1` behaviour could vary across Clojure versions; the design facts were re-checked on
  1.11.4 and 1.13, so the risk is low. If the test matrix diverges, record it in `implementation.md` and stop for
  a design decision.
- The hash-cache copy in `withMeta` can mask M10 and M11 if a Scope 5 case calls `hash` or `.hashCode` before
  `with-meta`. The Scope 5 construction order guards this.

## Slice order

1. Test namespace, types, differential helper.
2. Scope 3 field-pair tests.
3. Scope 4 plain-map tests.
4. Scope 5 metadata tests.
5. Scope 6 cross-type tests.
6. Full test matrix, lint, format.
7. Mutation checks and `implementation.md` entries (Acceptance 3).
8. `mementum/state.md`: V-12 done.
