# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

## Design review

- 2026-10-19 architecture review added 1 new design step. Fit is otherwise good: tests only (D-16 differential),
  emitter fixes stay in `impl/jvm.clj` (D-13, embodiment: the emitter builds the factories), host exceptions kept
  (D-03 scope, D-19). Finding: the "`^boolean` missing" row is confounded. With `:i 1` (a `Long`), `create` throws
  `ClassCastException` from the `^int` cast before the missing `:b` is unboxed. With only `:b` missing, `C` and `D`
  throw `NullPointerException`, as D-19 states. No `ramora/META.md` or `doc/architecture.md` exists; architecture
  sources are `ramora/INDEX.md` → `ramora/defclosedrecord/`.
- 2026-10-19 ambiguity review added 3 new design steps.
- 2026-10-19 inconsistency review added 1 new design step. Re-checked on 1.11.4: `assoc` rows `^int` ← `5000000000`
  (`ArithmeticException`), `^long` ← `9.9` (`9`, `Long`), `^String` ← `5` (accepted) and `map->Name` `^long` ← `2.5`
  (`ClassCastException`) hold for `C` and `D`. In `map->Name` inputs, give `:i` as `(int ...)`; a `Long` `:i` throws
  `ClassCastException` first and hides the case under test.
- 2026-10-19 architecture review (pass 2, after the design-steps fix): no architectural review feedback. The
  `^boolean` row now gives NPE (D-19). `map->Name` calls `create` (`impl/jvm.clj`), so the `map->Name` rows also
  cover the V-09 `create` path.
- 2026-10-19 ambiguity review (pass 2): no ambiguity review feedback. The `vals` row is clear only with the
  note below (return `(mapv class (vals x))`). Scope 2 compares a non-record result by `=` and its own class, so
  `(vals x)` returned as is cannot fail on a wrong box class.
- 2026-10-19 inconsistency review (pass 2): no inconsistency review feedback. `keyword-call-test` uses R → R2 →
  DR → map and never returns to R, so the Scope 5 A → B → A case is new (no overlap with Out of scope).

## Notes for the design-steps fix

- Principles: scope is frozen. Edit `design.md` in place (no revision markers, munera). Write in ASD-STE100 style.
  Keep the V-09 parity commitment (differential); answer each ambiguity with a stated rule, not a new scope item.
- Check each changed Context row on Clojure 1.11.4 before writing it: run `clojure -M:test -e` with `C` and `D`
  defined as in Context. Give only the field under test a bad or missing value.
- The other open item (`hash` across types): the `hasheq` type hash is D-07 in
  `ramora/defclosedrecord/decisions/type-structure.md`. D-19 is in `ramora/defclosedrecord/decisions/closed-key-set.md`.
- Mutation-check rules for Acceptance 3: `mementum/knowledge/testing-generated-types.md` (section "Test design").

## Design-steps fix

- 2026-10-19 all 5 design steps done (no `SCOPE_QUESTION:` items). Re-checked on 1.11.4 at `462f852` (source
  unchanged since `976f906`): only `:b` missing → NPE for `map->C`, `map->D`, `C/create`, `D/create`; `:i 1`
  (`Long`) → CCE in all four, also with all keys present. `N` nil facts hold; `hash` of `N` ≠ `hash` of the
  `defrecord` value, `.hashCode` equal. `(= (int 5) 5)` is true, so the helper must compare classes.
- Helper outcome forms for implementation: `[:ok normalised]` / `[:ex class]`. Normalise a record with
  `(update-vals (into {} x) (juxt identity class))` (`update-vals` exists from 1.11). For `vals`, return
  `(mapv class (vals x))`. Print-read in the CLI needs `*ns*` bound (knowledge file).

## Design review pass 2

- 2026-10-19 pass 2 (architecture, ambiguity, inconsistency) added no design steps. `design-steps.md` has no open
  items. Next lifecycle step: plan (`create-task-plan`).
- For planning: the V-06 types to reuse or mirror are `R`, `R2` (`[a b]`) and `DR` at the top of
  `test/closed_record/defclosedrecord_test.clj`. Generated `map->Name` is emitted near `src/closed_record/impl/jvm.clj:249`.

## Plan review

- 2026-10-19 ambiguity review added 1 new design step (mutation per row). Context: `jvm.clj` has no cast code and
  no `create`; the compiler emits casts, `create` and the D-19 NPE from the `deftype*` field hints. A mutation for
  these rows must change how the emitter reaches the compiler path (`new` vs `create`, hints kept or dropped).
- 2026-10-19 inconsistency review added 1 new design step (print/read `*ns*` binding vs plan Risks).

## Notes for the plan-review design-steps fix

- Both open steps change `plan.md` and `steps.md` only. Do not change `design.md`: scope and Acceptance 3 are fixed.
- Mutation map: test each candidate mutation in the scry CLI on 1.11.4 before you write it in `plan.md`, or mark it
  "candidate". Rules: a mutation counts only if it breaks the asserted path. A change that fails at namespace load
  gives no signal (`mementum/knowledge/testing-generated-types.md`, "Test design"). Dropping all hints is one change
  for many rows. This is permitted if each row fails with it, but path-specific changes give a better signal.
- Emitter places in `src/closed_record/impl/jvm.clj`: `valAt` with not-found at :129, `getLookupThunk` at :131,
  `assoc` at :182 (`new` with the field replaced), `positional-factory` at :215 (`new`), `map-factory` at :233
  (calls `create`), and the hinted field vector of `deftype*` at :272.
- Print/read pattern: the `in-test-ns` and `read-literal` helpers in `test/closed_record/literal_test.clj`.

## Plan-review design-steps fix

- 2026-10-19 both plan-review design steps done: plan.md has a "Mutation map" (M1 to M15, each mapped to the tests
  that it fails) and the print/read `*ns*` rule; steps.md Slice 5 lists each mutation. `design.md` not changed.
- The map was observed on 1.11.4 in the scry CLI with a scratch file `test/closed_record/zz_scratch_test.clj` (one
  `deftest` per row, deleted). Run one namespace: `clojure -M:test -m scry.cli -n <ns>`. Failed vars: one file per
  var in `.scry-results/`. Baseline: all rows passed.
- Surprises: `new` unboxes nil to `boolean` with NPE (not `false`), so `map->Name` through `new` (M5) keeps the D-19
  NPE. `Reflector/invokeConstructor` accepts `9.9` for `^long` (gives `9`) and gives NPE for nil. `vals`, keyword
  and V-06 boxed-value rows detect M1 only with a cast input (`3`/`4` Longs, `2.7`, `(int 2)`).
- Unobservable: `getLookupThunk` that returns nil (site falls back to `valAt`); a nil-dropping `seq` for print/read
  (read gives nil for the missing key).

## Plan review pass 2

- 2026-10-19 ambiguity review: no ambiguity review feedback. Mutation map line numbers match `jvm.clj` at
  `b9f538d`. Inputs that the map does not fix (the `a` value in the keyword rows, the `get` not-found value) can
  hide a mutation (M6 if `a` = 2, M9 if not-found is nil); the Slice 5 mutation check finds this.
- 2026-10-19 inconsistency review: added 1 new design step (print/read row: "none" vs M14). The rest of the
  mutation map matches the Slice 5 items. Slice 5 lists only the main tests for each mutation. The extra failures in
  the plan notes (M10 and M11 on `.equals` in the map-first direction) are not missing items.

## Notes for the plan-review pass 2 design-steps fix

- Change `plan.md` (the print/read row in the "Mutation map") and `steps.md` (the Slice 5 M14 item and the
  "no mutation breaks" item) only. Do not change `design.md`. Use ASD-STE100 style.
- Option (a) is the better fit: the map already counts M14 for the V-08 `=` test, and M14 breaks only the nil path
  (rule: `mementum/knowledge/testing-generated-types.md`, "Test design"). With (a), the "no mutation breaks" item
  keeps three tests: `assoc` `^String` ← `5`, V-08 `.field`, V-08 equal `hash`.
- The M14 → print/read failure was observed in the scratch run (plan note). No new check is needed unless the
  print/read form changes from `(= x read-back)`.

## Plan-review pass 2 design-steps fix

- 2026-10-19 the print/read step is done with option (a): the plan.md "Mutation map" row gives M14, and the Slice 5
  M14 item lists V-08 print/read. The "no mutation breaks" item now has three tests: `assoc` `^String` ← `5`, V-08
  `.field`, V-08 equal `hash`. No new check was run. The M14 failure on print/read comes from the scratch run, and it
  holds only while the test returns `(= x read-back)`. `design.md` was not changed.

## Plan review pass 3

- 2026-10-19 ambiguity review: no ambiguity review feedback. The print/read row (M14) and the Slice 5 M14 item now
  give one rule. Each mutation form uses only locals in scope at its place (`kws`, `nf`, `k`, `m`, `fields`), and
  the line numbers still match `jvm.clj` (`src` unchanged since `976f906`).
- 2026-10-19 inconsistency review: no inconsistency review feedback. Each Slice 5 mutation item matches its
  "Mutation map" rows, and the "no mutation breaks" item matches the three "none" rows.
- 2026-10-19 pass 3 added no design steps; `design-steps.md` has no open items, so there is no design-steps fix.
  Next lifecycle step: implementation (steps.md Slice 1). Do not change the plan inputs (decisions 2 to 4) without
  a new mutation run (plan Risks, "Mutation map drift").
