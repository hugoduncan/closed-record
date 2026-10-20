# Design steps

- [x] Ambiguity: metadata cases (Scope 4, Scope 5). The second table in Context has the row
      `r` and `(with-meta r {:m 1})`. That row is not a plain-map case. State if Scope 4 includes it or if only
      Scope 5 tests it. In Scope 5, state how "two equal values with different metadata" are made, so that the case
      differs from `r` and `(with-meta r {:m 1})`. For example: two values from separate factory calls, each with
      `with-meta` and different maps (`{:m 1}`, `{:m 2}`), and no `hash` call before the comparison.
- [x] Ambiguity: cross-type directions (Scope 6). Scope 4 names both directions for `=` and `.equals`. Scope 6 does
      not. State if Scope 6 asserts `(= (->E 1 2) (->DE 1 2))` and `(.equals (->E 1 2) (->DE 1 2))` only, or also
      the reverse calls. The reverse calls run `defrecord` code, not emitter code, so Acceptance 3 then needs a reason
      for each of them in `implementation.md`.

## Plan review: ambiguity (2026-10-19)

- [x] Ambiguity: `withMeta` mutation (plan.md Acceptance 3 targets, steps.md Slice 7). The step says "Mutate
      `withMeta` so metadata enters a hash or the caches are not copied; confirm a Scope 5 case fails". `withMeta`
      does not calculate a hash, so "metadata enters a hash" is a change in `hasheq`/`hashCode` (for example, mix
      `(hash __meta)` into the value). If `withMeta` does not copy the caches, the hash is calculated again with the
      same result. No Scope 5 case can fail, because Scope 5 makes both values before a `hash` call
      (`metadata-test` covers the cache copy). In plan.md, state the method and the change for each Scope 5 item,
      and remove the cache-copy option or say that it does not count.
- [x] Ambiguity: one mutation or more for each Slice 7 step (plan.md Acceptance 3, steps.md Slice 7). Each step
      names one mutation and a set of items that must fail. For some steps, one change cannot fail all items:
      - `equiv` with `.equals`: Scope 3 rows 1–3 fail. Row 4 (`1`, `1.0`) does not fail.
      - `hasheq` with `mapHash`: Scope 3 row `0.0`/`-0.0` fails. Scope 4 `hash` does not fail (the type hash
        stays). `hasheq` without the type hash: Scope 4 `hash` fails. Scope 3 does not fail.
      - `(contains? #{m} r)` calls `hasheq` of `r`, then `(.equiv r m)`. It changes only if both methods change.
      In plan.md, state if a step can use more than one mutation, and if a change in two methods counts as one
      change under Acceptance 3.

## Plan review: inconsistency (2026-10-19)

- [x] Inconsistency: the Acceptance 3 mutation targets (plan.md), the Slice 7 steps (steps.md) and the Scope 4 call
      paths (implementation.md, "Notes for the next slice"). The note says that each Scope 4 call reaches emitter
      code and needs a mutation entry. `(= m r)` calls no emitter method: `APersistentMap.equiv` returns false
      because `r` is an `IPersistentMap` and not a `MapEquivalence`. The only `src` change that breaks it is in
      `base-interfaces` (add `clojure.lang.MapEquivalence`: `(= m r)` becomes true). This was checked in a scratch
      copy. No plan target or Slice 7 step names `base-interfaces`. Also, plan.md names `cached-hash` as a target,
      but no Slice 7 step uses it. In plan.md, add `base-interfaces` to the targets with the `(= m r)` item. For
      `cached-hash`, add a step or remove it from the targets.

## Plan review pass 2: inconsistency (2026-10-19)

- [x] Inconsistency: M12 coverage (plan.md "Mutation checks" rules, mutation table and Coverage list; steps.md
      Slice 7). The rule says that a change in two methods counts as one change only if each part alone does not
      make the item fail. The parts alone fail Scope 4 `=` (the `equiv` part) and Scope 4 `hash` (M5). Thus M12
      counts only for `contains?`, as in the Slice 7 M12 step. But the Coverage list gives `Scope 4 =: M7, M12`, and
      it gives `hash: M5` without M12. In plan.md, remove M12 from the Scope 4 `=` coverage. In the M12 table row,
      show that `=` and `hash` also fail but do not count.
