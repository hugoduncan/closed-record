# Implementation

## Design review, architecture (2026-10-20)

- architectural review added 3 new design steps
- `ramora/META.md` and `doc/architecture.md` do not exist. The architecture is `ramora/defclosedrecord.md` and
  `ramora/defclosedrecord/` (purpose, qualities, invariants, embodiment, decisions/).
- Uncovered methods were found from the embodiment Method behaviour table and the call-path list in
  `mementum/knowledge/testing-generated-types.md` (task 006): `into {}` → `iterator`; `seq`, `.hashCode`, `.equals`
  → `seq`; `count`, `get`, `contains?`, `find` → `count`, `valAt`, `containsKey`, `entryAt`. The `java.util.Map`
  views and `getLookupThunk` have their own methods.

## Design review, ambiguity (2026-10-20)

- ambiguity review added 4 new design steps

## Design review, inconsistency (2026-10-20)

- inconsistency review added 3 new design steps

## Notes for the design-steps work (2026-10-20)

- Edit only `design.md`. Keep the Scope and Out of scope lists as they are. Where a step gives two options (add a
  check, or state in Decisions which V-id covers it), use the smaller change that is still true.
- Do these steps together, because they need the same facts:
  - Architecture 1 (coverage map) and Inconsistency 2 (mutation reason) both need the V-id that verifies each
    method: V-01 `defclosedrecord_test.clj`, V-06 `field_value_test.clj` (call sites), V-12 `equality_test.clj`,
    V-14 `hidden_key_test.clj`. Read the namespace docstrings. Do not guess.
  - Architecture 2 (`hash` in the same type, cache copy) and Inconsistency 3 (the `.hashCode` claim) both depend on
    what `equality_test.clj` already does.
  - Ambiguity 2 (exceptions): take the outcome model from `property_test.clj` (`attempt`, `judge`, `failure`,
    `run-check`). Then Ambiguity 3 can give the check names that the property reports.
- Architecture 3: the design must state the rule for an intended difference. It must not decide a new difference.
  Decision files: `ramora/defclosedrecord/decisions/closed-key-set.md` (D-04, D-05, D-19) and `type-structure.md`
  (D-07, D-10). Index: `ramora/defclosedrecord/decisions.md`.

## Design steps addressed (2026-10-20)

- All 10 steps done in `design.md`. No `SCOPE_QUESTION:` items.
- Choices where a step gave two options:
  - `java.util.Map` views: new checks, not a V-id. No test compares the views with `defrecord`: V-01 uses fixed
    values, V-03 a model, V-14 the `plain` value of the same type.
  - `hash`: new check `:start-hash`. Metadata on hash: V-12 only (Decisions).
  - `getLookupThunk`, `.field`, `record?`, `map->Name`: V-ids (Decisions coverage table).
- Corrections made on the way: Out of scope bullet 1 cited D-04 (that is `dissoc` of an undeclared key). Now D-02,
  D-05, and D-08 for `map->Name`. D-09 added to the hidden-keys bullet. These are the D-ids in Intent.
- Verified (Clojure 1.11.4, probe scripts in `/tmp`, not in git):
  - `RecordIterator` calls `valAt k` (one argument), not `valAt k nf`.
  - `(conj x [k v])`, `(merge x {k v})`, `(into x {k v})`, `(into x [[k v]])` give equal entries on `P` and `R`.
    `(conj x [:a 9 9])` also agrees: `imap-cons` of `defrecord` uses `nth 0` and `nth 1`, as the emitter does.
  - `.values` is `APersistentMap$ValSeq` and `.keySet` is `PersistentHashSet` on both types, so `=` of the view
    results needs no conversion.
- Mutation evidence for the Out of scope bullet: task 006 implementation.md (Slice 3 list), task 005 (M1–M12),
  task 004 plan.md "Mutation map" (M1–M15), task 007 (13 named mutations), task 002 (test review passes).
- `mementum/state.md` has two "Tests:" lines. Scope 6 means the first one (the list of test files). The second
  gives the run command.

## Design review 2, architecture (2026-10-20)

- no architectural review feedback. The 3 architecture steps of review 1 are resolved in `design.md`.

## Design review 2, ambiguity (2026-10-20)

- no ambiguity review feedback

## Design review 2, inconsistency (2026-10-20)

- inconsistency review added 1 new design step
- Checked and consistent: emitter `seq` reads the fields directly and `iterator` is `RecordIterator`
  (`src/closed_record/impl/jvm.clj`), so acceptance 3 "no earlier check reads the changed result" holds for both
  mutations. The Problem claim about V-08, V-09 and V-12 matches `equality_test.clj` and `field_value_test.clj`.

## Notes for the design-steps work, review 2 (2026-10-20)

- Only one open step: the coverage table row for `=` with another type or a plain map. Edit only that row in
  `design.md` Decisions. Do not edit Out of scope, Scope or Intent: the scope is frozen.
- `=` with a plain map is false for `P` and for `R`. It is not a difference from `defrecord`, so it must not get an
  Out of scope bullet or a D-id of its own. Its source is the purpose Equality table
  (`ramora/defclosedrecord/purpose.md#behaviour`) and D-07 (`ramora/defclosedrecord/decisions/type-structure.md`).
  The V-12 test is `test/closed_record/equality_test.clj`.
- Keep "(Out of scope, ...)" only on rows that match an Out of scope bullet.

## Design steps addressed, review 2 (2026-10-20)

- Coverage table row split in `design.md` Decisions. No `SCOPE_QUESTION:` items.
  - `=` with an `R`: V-12 `cross-type-test` (only `->E` against `->DE`, closed value first). Out of scope (D-07).
  - `=` with a plain map: V-12 `plain-map-test`. It is differential (runs with both factories), so no D-id.
- No V-12 test compares `=` with a closed record of a different closed type. The purpose row "another type" gives
  `false`; this design does not cover that case.

## Design review 3, architecture (2026-10-20)

- no architectural review feedback

## Design review 3, ambiguity (2026-10-20)

- no ambiguity review feedback

## Design review 3, inconsistency (2026-10-20)

- inconsistency review added 1 new design step
- Checked and consistent: each test name in the Decisions coverage table exists in the cited file; `metadata-test`
  is in `equality_test.clj` (and also in `defclosedrecord_test.clj`).

## Notes for the design-steps work, review 3 (2026-10-20)

- One open step: the evidence in Out of scope, bullet 2. Edit only the "V-12 tests them" sentence. Keep the bullet
  text that names the excluded items, and the D-07 reference. Do not edit Scope, Intent or the coverage table.
- Facts for the new sentence (`test/closed_record/`):
  - `equality_test.clj` `cross-type-test`: `=` false, `.equals` true, `.hashCode` equal, `->E` against `->DE`.
  - `defclosedrecord_test.clj` (V-01) `hash-test`: `(hash r)` = type hash XOR `mapHasheq`. Closed type only; it
    does not compare with a `defrecord`. Say "the `hash` formula", not "`hash` against `defrecord`".
  - `defclosedrecord_test.clj` (V-01) `equal-other-type-test`: `not=` with a `defrecord` (`->DR`) and with a
    different closed type (`->R2`).
- D-07 source: `ramora/defclosedrecord/decisions/type-structure.md` (`## D-07 Equality and hash`).

## Design steps addressed, review 3 (2026-10-20)

- Out of scope, bullet 2: only the evidence sentence changed. `=`: V-12 `cross-type-test`. `hash`: V-01 `hash-test`
  (formula, closed type only). No `SCOPE_QUESTION:` items.
- Verified (Clojure 1.11.4): `defrecord` `hash` is also type hash XOR `mapHasheq`, so `P` and `R` hash differ only
  by the type name hash. No test compares `hash` of a `P` and an `R`; this design does not add one.

## Design review 4, architecture (2026-10-20)

- no architectural review feedback

## Design review 4, ambiguity (2026-10-20)

- no ambiguity review feedback
- The helpers in `property_test.clj` (`failure`, `attempt`, `judge`, `run-check`) are `defn-`. Scope 1 and Scope 6
  name one new file, so the new namespace defines its own helpers with the same model. It does not require
  `closed-record.property-test`.

## Design review 4, inconsistency (2026-10-20)

- no inconsistency review feedback
- Checked and consistent: each D-id in Intent and Out of scope matches its heading in
  `ramora/defclosedrecord/decisions/closed-key-set.md` and `type-structure.md`. Each row of the purpose Behaviour
  tables and each capability C-02 to C-08 has a row in the Decisions coverage table.
- `=` with a different closed type (the purpose row "another type") is in V-01 `equal-other-type-test` (`->R2`). It
  is not a difference from `defrecord`, so the coverage table does not need a row for it. Do not raise it again.

## Notes for the design-steps work, review 4 (2026-10-20)

- Review 4 added no design steps. All items in `design-steps.md` are checked. Do not edit `design.md`.
- The next step is planning (`plan.md`, `steps.md`). The facts for the plan are in the earlier notes in this file:
  the outcome model (review 1 notes), the probes and call paths ("Design steps addressed"), and the private helpers
  (review 4, ambiguity).

## Plan review, ambiguity (2026-10-20)

- ambiguity review added 1 new design step (plan.md Risks: view comparison fallback)
- Checked and clear: step forms, check order, throw model and `result-data` keys match Scope 2 and 3. Both
  acceptance-3 mutations show first in the expected check: emitter `seq` reads the fields; `.hashCode`
  (`mapHash`), `hash` and `.equals` do not depend on entry order; `:entries` reads `valAt k`, not `valAt k nf`.
- Probe (Clojure 1.11.4): `=` of a Clojure set and a `java.util.HashSet`, and of a seq and an `ArrayList`, is true
  in both orders. `=` of two seqs in a different order is false.

## Plan review, inconsistency (2026-10-20)

- inconsistency review added 1 new design step (plan.md Key decisions: Observed facts claim)
- Not actionable: plan.md and steps.md (Slice 1, item 5) say that each check operation runs once per side. Design
  Scope 3 uses the same words, and also says that `:type` and `:equals` have one operation, on `x`. steps.md item 6
  gives that rule (`:closed` only). Do not raise it again.

## Notes for the design-steps work, plan review (2026-10-20)

- Two open steps. Both edit only `plan.md`. Do not edit `design.md` (frozen) or `steps.md`: no step item depends on
  the two changed bullets.
- Risks, bullet 3 (views): replace it, do not add a fallback. The checks must stay as in design Scope 3. Keep the
  probe fact (both types give the same view classes) as the reason to expect no difference.
- Key decisions, last bullet (Observed facts): use the operation list of the design Observed facts table
  (`design.md#observed-facts-2026-10-20-clojure-1114-branch-master-commit-820e889`). Refer to Risks, bullet 1, for
  a failure on unchanged `src`. Do not repeat Scope 5.
- After the edits, all items in `design-steps.md` are checked. The next step is implementation, Slice 1.

## Plan review steps addressed (2026-10-20)

- Both plan-review steps done in `plan.md` only. `design.md` and `steps.md` not changed.
- Risks, bullet 3: no fallback comparison. The view checks stay `=` (Scope 3) on all three aliases.
- Key decisions, last bullet: the "agree" claim now covers only the design Observed facts operations. The
  unobserved checks are listed there; a failure on unchanged `src` goes to Risks, bullet 1 (Scope 5).
- All items in `design-steps.md` are checked. Next: implementation, Slice 1 in `steps.md`.

## Plan review 2, ambiguity (2026-10-20)

- no ambiguity review feedback
- Checked and clear: the two `plan.md` bullets changed by plan review 1, the step forms, the 21 checks, the throw
  model, the mutation run method (`mementum/knowledge/testing-generated-types.md`: new JVM, read
  `[:shrunk :result-data]`), and the Scope 6 targets (`mementum/state.md` lines "Tests:" and "Verification plan
  done"; `embodiment.md` Artifacts table).
- Probe (Clojure 1.11.4): `(.containsValue x nil)` is false on `P` and on `R` (both use `(some #{v} (vals x))`).
  `:contains-value` with a nil field value is not a difference. Do not raise it again.

## Plan review 2, inconsistency (2026-10-20)

- no inconsistency review feedback
- Checked and consistent across `design.md`, `plan.md` and `steps.md`: 21 checks and 7 step kinds (same names and
  order), the acceptance-3 mutations and expected checks, the three aliases, and the Scope 6 targets.

## Notes for the design-steps work, plan review 2 (2026-10-20)

- Plan review 2 added no design steps. `design-steps.md` has no open item, so no design-steps work is necessary.
  Do not edit `design.md`, `plan.md` or `steps.md`.
- Do not raise the closed points again: one attempt per side per check (plan review 1), and `.containsValue nil`
  (plan review 2, ambiguity).
- Next: Slice 1. Model source: `test/closed_record/property_test.clj`. Rules:
  `mementum/knowledge/testing-generated-types.md` ("Property tests", "Equality and hash tests").
