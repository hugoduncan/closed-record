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
