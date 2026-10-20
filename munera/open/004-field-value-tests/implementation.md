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
