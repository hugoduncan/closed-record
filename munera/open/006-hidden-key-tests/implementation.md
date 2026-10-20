# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

- Design review (architecture), 2026-10-19: no architectural review feedback. `ramora/META.md` and
  `doc/architecture.md` do not exist; the architecture source is `ramora/INDEX.md` → `ramora/defclosedrecord.md`.
- Design review (ambiguity), 2026-10-19: no ambiguity review feedback. Fact for the inconsistency pass: `map->Name`
  with extra keys throws `:closed-record/extra-keys` (`map-factory` in `jvm.clj`, docstring in `core.cljc`), not
  `undeclared-key` as "Out of scope" says.
- Design review (inconsistency), 2026-10-19: inconsistency review added 2 new design steps.
- Notes for the design steps (2026-10-19):
  - Edit design.md in place (no revision markers). Scope is frozen: no new or removed cases in Scope 3, Scope 4 or
    "Out of scope". Use simplified technical English.
  - `map->Name` step: the V-01 row is in `ramora/defclosedrecord/purpose.md` (behaviour table, `map->Name`:
    "extra key → throw"). Code: `map-factory`, `src/closed_record/impl/jvm.clj`.
  - Retention rule step: paths in the emitter (`cons` and `without`, `src/closed_record/impl/jvm.clj`):
    `(conj lit [:a 5])` → `cons` vector branch → `assoc`. `(merge lit {:b 9})` → `conj` of a map → `cons` `:else`
    branch (loop of `assoc`), so `merge` covers a different path. `update` → `assoc`. `into` of pairs → `cons` vector
    branch, as `conj`. `(dissoc lit :z)` → `without` returns `this`. A rule that fits: "an operation is a case if it
    reaches an emitter path that no other case reaches". No case reaches the `cons` map-entry branch. Do not add one
    (scope).
- Design steps done (2026-10-19): "Out of scope" now says `map->Name` throws `extra-keys`. The `update`/`into` rule
  is replaced by one emitter path per case and a reason per non-case. Checked at the REPL (Clojure 1.11.4):
  `check` throws `extra-keys` `#{:z}` for `(into lit {:b 9})`, `(into lit [[:b 9]])`, `(conj lit (first {:b 9}))`
  and `(update lit :a inc)`. Known gap, not in scope: no Scope 4 case reaches the `cons` map-entry branch. Only a
  human may add a case for it.
- Design review (architecture, pass 2, after design steps), 2026-10-19: no architectural review feedback.
- Design review (ambiguity, pass 2, after design steps), 2026-10-19: no ambiguity review feedback. Checked
  (Clojure 1.11.4): `.values` is an `APersistentMap$ValSeq`, so `=` compares it by value. `=` of two `.iterator`
  results is false; compare them through `iterator-seq`, as the Context table says.
- Design review (inconsistency, pass 2, after design steps), 2026-10-19: no inconsistency review feedback.
- Design review pass 2 (2026-10-19): no new design steps; all items in design-steps.md are done. Next step:
  plan (`create-task-plan`). For the plan: the helpers `read-literal` (`in-test-ns` + `read-string`) and `data-of`
  are private copies in `test/closed_record/defclosedrecord_test.clj` and `literal_test.clj`. The new namespace
  needs its own copy, as the other test namespaces have.
- Plan review (ambiguity), 2026-10-19: no ambiguity review feedback. Facts for the inconsistency pass:
  - Plan risk says `check` may use `count`. `check` (`src/closed_record/core.cljc`) reads only `-extra-keys`, so a
    `count` mutation does not break `check`.
  - Unit of the mutation record: plan.md says each operation; steps.md Slice 3 says each operation group.
    Acceptance 3 needs a record for each operation.
- Plan review (inconsistency), 2026-10-19: inconsistency review added 2 new design steps.
- Notes for the plan design steps (2026-10-19):
  - Edit plan.md and steps.md in place (no revision markers). No step in steps.md is ticked, so a change there loses
    no progress. Do not change design.md or the scope. Use simplified technical English.
  - Risk example: `check` is `src/closed_record/core.cljc` lines 70-89; `count` is a constant `n` in
    `generated-methods` (`src/closed_record/impl/jvm.clj`). A true example of a change that gives no signal is in
    `mementum/knowledge/testing-generated-types.md` ("Test design"): a change that fails at namespace load, or a
    hash body that is not `int`.
  - Slice 3 step: keep the example emitter changes. Only the unit of the record changes (each operation).
- Plan design steps done (2026-10-19): plan.md risk now uses a true no-signal example (a `hasheq` body that is not
  `int`, from `testing-generated-types.md`), not `count`. steps.md Slice 3 records each Scope 3 operation, not each
  group. design.md not changed. Facts for Slice 3 (`generated-methods`, `src/closed_record/impl/jvm.clj`): some
  `java.util.Map` methods delegate, so one change can break several operations: `size` and `isEmpty` → `.count`;
  `get` → `.valAt`; `keySet`, `values`, `entrySet`, `containsValue` → `keys`/`vals`/`set` of the value (its `seq`).
  `find` → `entryAt`. `hasheq`, `hashCode` and `equals` call `APersistentMap` static methods on the value. `count` is
  the constant `n`. Run and record each part of a multi-method change alone (knowledge "Test design").
- Plan review (ambiguity, pass 2, after plan design steps), 2026-10-19: no ambiguity review feedback. Facts for
  Slice 3: `src` has no print code. `pr-str` goes through the Clojure `IRecord` `print-method` → `print-map` →
  `seq`, so a `seq` change is the print path. `(.equals {:a 1 :b 2 :z 3} x)` (`mapEquals`) reads `.size` first, so
  it fails only if `.size` and `containsKey`/`get` both show `:z`; use the multi-method rule ("Test design").
- Plan review (inconsistency, pass 2, after plan design steps), 2026-10-19: inconsistency review added 1 new design
  step (emitter-fix step placement). Low risk: the design Context facts show that all Scope 3 and Scope 4 rows pass on
  1.11.4, so a fix is not expected; the step only removes the contradiction. Lines: steps.md 19, 30, 48-50, 57;
  plan.md 34, 51.
- Notes for the emitter-fix design step (2026-10-19):
  - Same edit rules as the plan design steps above (in place, no ticked steps, design.md and scope frozen).
  - Option that needs the smallest edit: change steps.md Slice 3 line 48 to a general rule: "If a test fails on code
    that is not mutated (any run in Slice 1 to Slice 4), fix `src/closed_record/impl/jvm.clj`, record the cause,
    and run the failed slice again". Then change plan.md line 51 ("emitter fixes if any") and line 34 to agree.
  - A fix is made only in the work tree, not in the `/tmp` mutation copy. A Slice 4 failure on 1.12 or 1.13 only
    needs a fix that keeps all three versions passing (acceptance 2).
- Emitter-fix design step done (2026-10-19): the Slice 3 fix step is removed. steps.md now has a "Rule for emitter
  fixes (all slices)" section before Slice 1; the work-tree runs in Slice 1, 2 and 4 point to it. plan.md Approach
  and "Slice order" agree; slice order not changed. design.md not changed. Added to keep acceptance 3 true: if a
  fix after Slice 3 changes the path of an operation, do its mutation check again on the fixed code. All items in
  design-steps.md are done; next step: plan review pass 3.
- Plan review (ambiguity, pass 3, after emitter-fix design step), 2026-10-19: no ambiguity review feedback. Facts
  for Slice 3 (Clojure 1.11.4): a closed record is `Iterable` and not `IReduceInit`, so `reduce`, `(into {} x)` and
  `reduce-kv` (`IKVReduce` through the `IPersistentMap` extension) use `iterator`, not `seq`. The `RecordIterator`
  change covers them. A single-path Scope 4 change for a `cons` branch: start that branch from
  `(new T fields __meta nil)` and keep `assoc` unchanged; an `assoc` change also breaks the `conj` and `merge`
  cases. A Slice 4 fix that needs a new mutation check needs a new `/tmp` copy with the fix.
- Plan review (inconsistency, pass 3, after emitter-fix design step), 2026-10-19: no inconsistency review feedback.
  Checked: `scry.cli -n` (Slice 1) is `--namespace` in scry 0.1.61 `--help`.
- Plan review pass 3 (2026-10-19): no new design steps; design-steps.md has no open items. Plan and steps are
  stable. Next step: implementation (`implement-task`), Slice 1. Principle: do not edit plan.md or steps.md again
  unless the approach changes; record discoveries here.
