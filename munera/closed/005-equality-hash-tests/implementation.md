# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

## Design review (2026-10-19)

- No architectural review feedback.
  - Architecture source: `ramora/INDEX.md` → `ramora/defclosedrecord/`. `ramora/META.md` and `doc/architecture.md`
    do not exist.
  - The design fits D-07, I-05, V-12, D-16 (CI matrix) and the embodiment test layout (equality tests in
    `test/closed_record/*_test.clj`).
  - The D-07 hash caches are already covered by V-01 tests: `assoc-test` (assoc does not copy the caches) and
    `metadata-test` (`withMeta` copies them). They are not an I-05 gap for this task.
- Ambiguity review added 2 new design steps.
- No inconsistency review feedback.
  - Re-checked both Context tables and the cross-type facts on Clojure 1.11.4 and 1.13 (`-M:test:clj-1.13`),
    2026-10-20: all hold for `E` and `DE`.
  - The Scope 4 / Scope 5 overlap on the metadata row is in the first ambiguity design step. It is not repeated.

## Notes for the design-steps slice

- Resolve each step with an edit in place in `design.md` (Scope 4, 5, 6 text). Do not add or remove scope items.
  Tick the step in `design-steps.md`.
- Metadata step: `withMeta` copies `__hash` and `__hasheq` (`src/closed_record/impl/jvm.clj`, `withMeta`). If a test
  calls `hash` on `r` before `with-meta`, the copied cache hides a mutation that puts `__meta` into a hash. Choose the
  construction so that the case can fail. The cache copy is already tested in `metadata-test`
  (`test/closed_record/defclosedrecord_test.clj`).
- Cross-type step: `equal-other-type-test` already asserts `(not= r (->DR 1 2))`, closed side first. A reverse call
  runs only `defrecord` code, so no emitter mutation can fail it.
- Quick fact check: put the forms in a file and run `clojure -M:test[:clj-1.13] -i <file>`.

## Design steps resolved (2026-10-19)

- Both ambiguity steps resolved in Scope 4, 5 and 6 text. No scope items added or removed.
- Fact check, Clojure 1.11.4: `(with-meta (f 1 2) {:m 1})` and `(with-meta (f 1 2) {:m 2})` give
  `[true true true true]` for `E` and `DE`.
- Scope 6 reverse calls are not asserted. Acceptance 3 needs no reason entry for them.

## Design review, pass 2 (2026-10-19)

- No architectural review feedback. The Scope 4, 5 and 6 edits (commit `c1e8b5a`) change only test construction.
  They fit D-07, I-05 (metadata has no effect) and V-12.
- No ambiguity review feedback.
- No inconsistency review feedback.

## Notes for the next slice (after design review pass 2)

- Pass 2 added no design steps. All steps in `design-steps.md` are ticked. The design is ready for `create-task-plan`.
- Each Scope 4 call reaches emitter code, also the reverse calls: `(.equals m r)` and `HashMap.equals` call the
  `Map` methods of `r`; `contains?` on `#{m}` calls `hasheq` of `r`; `HashSet.contains` calls `.hashCode` and
  `.equals` of `r`. Thus each Scope 4 item needs a mutation entry for Acceptance 3, not a reason entry.
- Mutation targets in `src/closed_record/impl/jvm.clj`: `hasheq`/`hashCode` (`cached-hash`, `mapHasheq`, `mapHash`),
  `equals` (`mapEquals`), `equiv`, `withMeta` (copies `__hash`, `__hasheq`).
- Acceptance 5: move V-12 from "Not done" to "done" in the verification-plan line of `mementum/state.md`.

## Plan review: ambiguity (2026-10-19)

- Ambiguity review added 2 new design steps (Slice 7 mutations).
  - Call paths checked in Clojure sources: `#{m}` lookup calls `Util.equiv(r, m)` → `(.equiv r m)` (emitter);
    `HashSet.contains` calls `(.equals r m)` (emitter). One `equals` change (for example, `mapEquals` → `equiv`)
    fails Scope 3 rows 1–3 `.equals`, Scope 4 `.equals`/`HashSet` and Scope 6 `.equals`. One `hashCode` change
    (`mapHash` → `mapHasheq`) fails Scope 3 row `0.0`/`-0.0`, Scope 4 `.hashCode`/`HashSet` and Scope 6
    `.hashCode`. These steps are not ambiguous.

## Plan review: inconsistency (2026-10-19)

- Inconsistency review added 1 new design step (`(= m r)` path and the mutation target list).
  - `MapEquivalence` is a marker interface. Adding it to `base-interfaces` loads without error. In `src`, only
    `map->Name` also checks it (it then does not call `into {}`), and a record input gives the same result. Thus
    the change is specific to `(= m r)` and is a valid mutation under the "Test design" rules.

## Notes for the plan design-steps slice (after plan review)

- The 3 open steps in `design-steps.md` (plan review sections) are plan-level. Resolve them in `plan.md`
  (Acceptance 3 targets), then make steps.md Slice 7 agree. Do not change design.md Scope or Acceptance.
- Principle: each Slice 7 step names the mutation(s) and the exact items that each one fails. The catch-all step
  stays for items that no listed mutation covers.
- Candidate mutations, not yet run (except `MapEquivalence`):
  - Scope 3 row 4 `=`: `equiv` compares fields with `==` (`(== 1 1.0)` is true).
  - Scope 5 `hash`/`.hashCode`: `hasheq`/`hashCode` mix in `(hash __meta)`. Scope 5 `=`: `equiv` also compares
    `__meta`.
  - `(contains? #{m} r)`: needs `hasheq` without the type hash and also `equiv` that accepts a map. This is the
    open decision about a change in two methods.
- `src/closed_record/impl/jvm.clj` locations: `base-interfaces` (top of file), `cached-hash`, `generated-methods`
  (`hasheq`, `hashCode`, `equals`, `withMeta`, `equiv`).
- To run a mutation without risk to the work tree: `cp -R` the repo to `/tmp`, edit there, run
  `clojure -M:test -i <file>` or `-m scry.cli`.

## Plan design-steps resolved (2026-10-19)

- The 3 plan-review steps are resolved in plan.md ("Mutation checks") and steps.md Slice 7. design.md is unchanged.
  `withMeta` and `cached-hash` are not targets. `base-interfaces` is a target (M7).
- Correction to "Notes for the next slice (after design review pass 2)": `(= m r)` calls no emitter method. M7
  covers it.
- M1–M12 were run in a scratch copy (`cp -R` of the repo to `/tmp`) with a probe file that printed the
  `E` and `DE` result vectors for each item. The probe file is not in git. The results are in the plan.md table.
- Discoveries from the scratch runs:
  - The M3 failure in Scope 4 `.equals` and `HashMap` is only in the `r`-first call. `(.equals m r)` calls
    `size`, `containsKey` and `get` of `r`, not `equals`.
  - The M12 `equiv` part does not change Scope 6: `(= (into {} E) DE)` is false, because `DE` is not a
    `MapEquivalence`.
  - A hash body of type `long` (for example a `bit-xor` that is not wrapped in `int`) fails at `deftype*` compile
    with `ArrayIndexOutOfBoundsException`. It gives no signal.
- No test code exists yet. Slice 1 is the next step.

## Plan review pass 2: ambiguity (2026-10-19)

- No ambiguity review feedback.
  - Checked M1–M12 against `generated-methods` and `base-interfaces` in `src/closed_record/impl/jvm.clj`: each
    change names one method and one edit.
  - Slice 2 and Slice 4 share the four-result function. Write it over two values `x` and `y`. The Scope 3 test
    builds `x` and `y` from the pair, then calls it.

## Plan review pass 2: inconsistency (2026-10-19)

- Inconsistency review added 1 new design step (M12 coverage). Coverage does not change: M7 covers Scope 4 `=`.

## Notes for the plan design-steps slice (after plan review pass 2)

- Edit only plan.md "Mutation checks": the M12 table row and the Coverage list. design.md, the rules and steps.md
  Slice 7 already agree with the counting rule. Do not change them.
- No scratch run is needed. The M12 part results (plan.md, "M12 parts alone") stay valid. Only the counting changes.
- Keep the counting rule. Do not add the M12 `equiv` part as a separate counted mutation for Scope 4 `=`. M7 covers it.
- Mutation rules source: `mementum/knowledge/testing-generated-types.md` ("Test design").

## Plan design-steps resolved, pass 2 (2026-10-19)

- M12 coverage step resolved in plan.md only: the M12 row now counts only Scope 4 `contains?`, and names Scope 4
  `=` and `hash` as failures that do not count. Coverage list: Scope 4 `=` is M7 only. steps.md and design.md are
  unchanged.
- Slice 7 M12 run: Scope 4 `=` and `hash` also fail. Record them in the M12 entry as not counted. They are not a
  mismatch with the plan.
- All steps in `design-steps.md` are ticked. Slice 1 is the next step.

## Plan review pass 3: ambiguity (2026-10-19)

- No ambiguity review feedback.

## Plan review pass 3: inconsistency (2026-10-19)

- No inconsistency review feedback.

## Notes for the next slice (after plan review pass 3)

- Pass 3 added no design steps. `design-steps.md` has no open items, so there is nothing for a design-steps slice
  to do. Start at Slice 1.

## Implementation (2026-10-19)

- Slices 1–6: `test/closed_record/equality_test.clj`. Helper `is-diff` (macro) calls `f` with `->E` and `->DE`
  and asserts `(= (f ->E) (f ->DE))`. `four` gives the first-table vector; `pair` and `with-r` build the
  Scope 3 and Scope 4 functions. Scope 4 pairs each case with its reverse call in one vector. Matrix 1.11.4,
  1.12, 1.13: 254 assertions pass. clj-kondo and cljfmt clean. No emitter change (Scope 7 not needed).
- Slice 7: each mutation ran alone in a scratch copy (`/tmp/mut-<id>`, Clojure 1.11.4), with
  `run-tests 'closed-record.equality-test`. Failed items (test `testing` labels):
  - M1 (`equiv`, `=` → `.equals`): Scope 3 `1N`, `(int 1)`, `-0.0`.
  - M2 (`equiv`, `=` or numeric `==`): Scope 3 `1.0`.
  - M3 (`equals` → `.equiv`): Scope 3 `1N`, `(int 1)`, `-0.0`; Scope 4 `.equals`, `HashMap`, `HashSet`;
    Scope 6 `.equals`.
  - M4 (`hasheq`, `mapHasheq` → `mapHash`): Scope 3 `-0.0`.
  - M5 (`hasheq`, no type-hash `bit-xor`): Scope 4 `hash`.
  - M6 (`hashCode`, `mapHash` → `mapHasheq`): Scope 3 `-0.0`; Scope 4 `.hashCode`, `HashSet`; Scope 6 `.hashCode`.
  - M7 (`base-interfaces` + `MapEquivalence`): Scope 4 `=`.
  - M8 (`equiv` also `(= __meta (meta o))`), M9 (`equals` also compares meta), M10 (`hasheq` xor `(hash __meta)`),
    M11 (`hashCode` xor `(hash __meta)`): Scope 5 both cases, each.
  - M12 (M5 + `equiv` for another class returns `(and (map? o) (= (into {} this) o))`): Scope 4 `contains?`
    (counted); `=`, `hash` also fail (not counted). `equiv` part alone: only Scope 4 `=`; `contains?` passes.
    `hasheq` part alone is M5: only `hash`.
- Coverage equals the plan.md list. No item without a mutation; no reason entry needed.
- Slice 8: V-12 moved to done in `mementum/state.md`. All steps ticked.

## Implementation review (2026-10-19)

- No implementation review feedback.

## Test review (2026-10-19)

- No test review feedback.

## Test review: test-shaper (2026-10-19)

- Added 1 step to be addressed.

## Review follow-up (2026-10-19)

- addressed 1 review step: differential results are maps keyed by operation; `is-diff` names the locals
  `closed` and `record` (no gensym in `expected:`). Scratch M6 check: failure shows
  `(not (= {:hashCode= false} {:hashCode= true}))`. Matrix (1.11, 1.12, 1.13) 254 pass; clj-kondo, cljfmt clean.

## Test review: test-shaper, pass 2 (2026-10-19)

- No test review feedback.

## Docs review (2026-10-19)

- No docs review feedback.

## Code review: code-shaper (2026-10-19)

- Added 1 step to be addressed.

## Review follow-up (2026-10-19, pass 2)

- addressed 1 review step: removed task design references ("Scope 3–6", "first Context table") from
  `equality_test.clj`; `four` docstring names its four operations; comments name the case. Matrix (1.11, 1.12,
  1.13) 254 pass; clj-kondo, cljfmt clean.

## Code review: code-shaper, pass 2 (2026-10-19)

- Added 1 step to be addressed.

## Review follow-up (2026-10-19, pass 3)

- addressed 1 review step: renamed `four` to `equality-results` in `equality_test.clj`; removed the caller rule
  from its docstring (the `metadata-test` comment states it). Matrix (1.11, 1.12, 1.13) 254 pass; clj-kondo,
  cljfmt clean.

## Code review: code-shaper, pass 3 (2026-10-19)

- Added 1 step to be addressed.

## Review follow-up (2026-10-19, pass 4)

- addressed 1 review step: renamed var `m` to `plain-map` and keys `:m-first` to `:map-first` in
  `equality_test.clj`. Matrix (1.11, 1.12, 1.13) 254 pass; clj-kondo, cljfmt clean.

## Code review: code-shaper, pass 4 (2026-10-19)

- No code review feedback.
