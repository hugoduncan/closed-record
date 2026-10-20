# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

## Design review (2026-10-19)

- Architectural review added 2 new design steps (`design-steps.md`).
- Architecture sources: `ramora/META.md` and `doc/architecture.md` do not exist. The architecture is in
  `ramora/INDEX.md` → `ramora/defclosedrecord.md` → `ramora/defclosedrecord/`.
- Checked and fit: the new test file agrees with embodiment artifacts (`test/closed_record/*_test.clj`); an emitter
  fix in `impl/jvm.clj` agrees with D-13; the V-04 rows that are not in Scope 2 (missing key → nil, extra key →
  `check` throws) are in the 002 tests, so state.md can mark V-04 done.
- Not a finding: D-09 "Qualities", the qualities trade-offs and R-04 also say that constants drop the extra keys.
  D-09 cites I-07, so the exact condition in I-07 (Scope 4) is enough. A change to those files is outside Scope 4.
- Ambiguity review added 3 new design steps. Observed (2026-10-19, Clojure 1.11.4): `eval` of a bare record object
  also goes through `emitValue` (`Compiler.eval` wraps a collection form in `(fn [] form)`), so it drops `__extmap`.
  `eval` of a `T/create` or `read-string` form keeps it.
- Inconsistency review added 1 new design step. The `T/create` example in the first ambiguity step was changed:
  Scope 3 requires a constant "from a literal". Checked and consistent: the test names in Context exist in
  `defclosedrecord_test.clj`; "V-13 checks indirectly" is correct, because `compiler_rules_test.clj` checks K-08
  and E-03 on hand-written `deftype*` types, not on closed records (its `constant` helper is `((eval (list 'fn [] v)))`).

## Notes for the design-steps work (2026-10-19)

- Resolve these steps together: the architecture step on the I-07 record shape and the inconsistency step on
  top-level forms both change Scope 4 and the constant Decision. The `print-dup` steps (architecture 1, ambiguity 2)
  both change the `print-dup` item in Scope 2.
- Keep: the scope items as they are (scope is frozen); the I-07 change only in `ramora/defclosedrecord/invariants.md`;
  edit design.md in place, without revision notes; simplified technical English.
- Source of the top-level fact: Clojure `Compiler.eval` compiles a collection form into `(fn [] form)` unless its
  first symbol starts with `def`. `DefExpr.eval` evaluates the init, and `ConstantExpr.eval` returns the object.
- The probes ran from `/tmp/probe003/` (not in git). To run one again: a file with `(ns probe ...)`,
  `(defclosedrecord P [a ^long n])` and `check`, run with `clojure -M:test -e '(load-file "<file>")'`.
- Related files: `ramora/defclosedrecord/{invariants,qualities,context}.md`,
  `ramora/defclosedrecord/decisions/closed-key-set.md` (D-09), `mementum/knowledge/testing-generated-types.md`.
  `test/closed_record/compiler_rules_test.clj` has private helpers (`eval-in-ns`, `class-name`, `thrown`,
  `constant`). Copy them; do not refer to them.

## Design steps done (2026-10-19)

- All 5 design steps resolved in design.md. No `SCOPE_QUESTION:` items.
- Probe `/tmp/probe003/r.clj` (not in git, Clojure 1.11.4) checked the Decisions: a macro body
  `(read-string s)` with no `eval` keeps `__extmap` at expansion (`macroexpand-1` → `check` throws `extra-keys`), and
  the compiled result in a `defn` passes `check`; metadata `^{:m 1}` on the literal is kept in the constant;
  `(def d (let [x (m)] x))` drops `__extmap` (a `let` init is compiled), `(def d (m))` keeps it.
- At file load, macro expansion runs with `*ns*` = the test namespace. Only a run-time `macroexpand-1` call needs
  the `*ns*` binding.
- Mutation hint (not checked): with `IType`, `emitValue` emits all fields, `__extmap` included, so the extra-key
  constant test should fail; the value-constant test probably does not.

## Design review, pass 2 (2026-10-19)

- No architectural review feedback.
- Checked and fit, not findings: the I-07 `basis:` cites the task 003 observation and the `Compiler.eval` path, with
  no new E-xx entry in `context.md`. The first architecture step chose this, to keep the change in `invariants.md`.
  The `print-dup` printer fact has no K-xx constraint and its test is in `literal_test.clj`, not V-13. The first
  architecture step permits a change-signal test of this kind.
- No ambiguity review feedback (pass 2).
- No inconsistency review feedback (pass 2).

## Notes after design review pass 2 (2026-10-19)

- Pass 2 added no design steps. All items in `design-steps.md` are ticked. The design is ready for `create-task-plan`.
- If a later pass adds steps, keep the principles in "Notes for the design-steps work". Do not file the 2 items that
  pass 2 accepted again (I-07 `basis:` with no E-xx entry; `print-dup` test not in V-13).
- The I-07 text to change is in `ramora/defclosedrecord/invariants.md`, record `id: I-07`, line `scope: all instances.
  Print, code constants and positional reads drop __extmap; ...`. C-07 is in `ramora/defclosedrecord/purpose.md`.

## Plan review (2026-10-19)

- Plan ambiguity review added 1 new design step (`design-steps.md`): metadata on the round-trip records.
- Checked, not findings: the nested round-trip record can go only in the unhinted field `a` of `T [a ^long n]`;
  `defclosedrecord_test.clj` has `in-test-ns`, `read-literal` and `thrown`, which fit the slice 1 helper step;
  "each new test" in the mutation check means each of the 6 tests in steps.md.
- Plan inconsistency review added 1 new design step (`design-steps.md`): the `munera/plan.md` task summary.
- Checked, consistent: each Acceptance item has a step; the extra-key constant step says "`=` to the declared-keys
  value" and the design says "`=` to the literal", which is the same value (`=` ignores `__extmap`).

## Notes for the plan-review design steps (2026-10-19)

- Both open steps are plan-level. They do not change the scope or the design commitments. Edit `plan.md`,
  `steps.md` and `munera/plan.md` in place. Change `design.md` only if a Decision must name the no-metadata rule.
- Metadata step: the emitter defines no `print-method` or `print-dup` (`src/closed_record/impl/jvm.clj`,
  `src/closed_record/core.cljc`). Closed records use the Clojure `IRecord` printer (`clojure/core_print.clj`,
  `print-meta` prints metadata when `*print-dup*` is true). So the `defrecord` observation applies to `T`. The
  value-constant test keeps its `^{:m 1}` literal. Only the round-trip records have no metadata.
- `munera/plan.md` step: keep the entry short and keep the order list format.

## Plan-review design steps done (2026-10-19)

- Both plan-review steps resolved in `plan.md`, `steps.md` and `munera/plan.md`. `design.md` not changed: the
  no-metadata rule is a plan-level test-data choice, not a Decision.
- Probe `/tmp/probe003/meta.clj` (not in git, Clojure 1.11.4) confirmed the `defrecord` observation on a closed
  record: `print-dup` of `(with-meta (->P 1 2) {:m 1})` gives `^#=(clojure.lang.PersistentArrayMap/create {:m 1})
  #probe.P[1, 2]`; plain `pr-str` drops the metadata.
- Do not add metadata to the round-trip `r` values or to the nested field value. The `print-dup` prefix assertion
  fails if `r` has metadata.

## Plan review, pass 2 (2026-10-19)

- Plan ambiguity review added 1 new design step (`design-steps.md`): criterion for a valid mutation.
- Why actionable: Acceptance 3 wording ("the behaviour that it checks") is lost in plan.md and steps.md ("a temporary
  breaking change"). With no criterion, a global `.equals` or `check` break satisfies the step for all 6 tests.
- Checked, not findings: `check` ex-data is `{:type :closed-record/extra-keys :record T :keys #{...}}`
  (`core.cljc`), as in the 002 tests; the compiled constant keeps exactly `{:m 1}` (the `emitValue` meta branch emits
  `withMeta` of the elided meta, and `read-string` adds no `:line`); `mementum/state.md` lists "Not done: V-02 to
  V-09", so the update also changes that range.
- No plan inconsistency review feedback (pass 2).
- Not findings: the plan Risks credit "the design" with the `IType` value-test hint, but the hint is in this file
  ("Mutation hint (not checked)"), not in design.md; the `design-steps.md` header says "Tick an item when design.md
  is updated", but the plan-review items change plan.md and steps.md. Both have no effect on the work.

## Notes for the plan-review pass 2 design step (2026-10-19)

- The open step is plan-level. Edit in place the plan.md mutation bullet, the steps.md "Mutation check" step and the
  "Record each mutation" step. design.md Acceptance 3 already has the criterion ("the behaviour that it checks"), so
  do not change design.md. Keep the permitted locations and the "one change can cover more than one test" rule.
- A valid change can also make other tests fail. The criterion is the cause: the change breaks the path or property
  that the test asserts, not `=` or `check` for all values.
- Candidate changes per test (not checked; the implementation slice confirms them):
  - round trip, extra-key literal print: the record `seq` adds `__extmap` entries or drops a declared entry
    (the `IRecord` printer iterates the record).
  - `print-dup`: bind `*verbose-defrecords*` true, or change the order of `vals`.
  - value constant: `withMeta` or `meta` drops the metadata.
  - extra-key constant: the type implements `IType` (`emitValue` then emits `__extmap`).
  - positional literal: no candidate found yet. The reader calls the positional constructor that the compiler makes
    from the `deftype*` fields, and `->T` uses the same constructor. Record this as an open option if no change works.
- Files: `src/closed_record/impl/jvm.clj` (type methods; `-extra-keys` near line 213), `clojure/core_print.clj`
  (`print-method` and `print-dup` for `IRecord`), `clojure.lang.Compiler` (`emitValue`, `IType` before `IRecord`).

## Plan-review pass 2 design step done (2026-10-19)

- Mutation criterion added to the plan.md mutation bullet, a plan.md Risk, the steps.md "Mutation check" step and
  the "Record in `implementation.md`" step. design.md not changed (Acceptance 3 already has the criterion).
- `implementation.md` records two items per test: the change and the failed assertion. The positional-literal test
  still has no checked candidate change (see "Notes for the plan-review pass 2 design step"). A global `=` or
  `check` break is not a fallback for it.
- All items in `design-steps.md` are ticked. No plan step is done yet; implementation starts at Slice 1.

## Plan review, pass 3 (2026-10-19)

- No ambiguity review feedback (pass 3).
- Positional-literal mutation candidate (not checked): `->T` calls the same basis constructor as the reader
  (`positional-factory` emits `(new classname a n)`), so a hidden-field change breaks both. Swap the argument order
  in the `new` form of `positional-factory`. Then `#T[1 2]` and `(->T 1 2)` differ. The round-trip tests still pass,
  because their `r` and the read-back value both come from the same constructor arguments.
- Checked, not a finding: the extra-key print test reads `lit` at run time with the step 1 helper ("Read literals with
  `*ns*` bound"), as in `reader-literal-undeclared-key-test`. A literal in the `deftest` source is a code constant and
  drops `__extmap`. The mutation check (`seq` adds `__extmap` entries) catches this error.
- No inconsistency review feedback (pass 3).
- Correction to the pass 3 mutation candidate: the plain and `print-dup` round trips still pass with the swap, but
  the nested round trip does not. Its inner record goes into field `a` (the only unhinted field), and the swap puts
  it into the `^long` parameter, so `->T` throws `ClassCastException` (`RT.longCast`). The plan permits a valid
  change that also makes other tests fail, so the candidate is still valid for the positional-literal test.

## Notes after plan review pass 3 (2026-10-19)

- Pass 3 added no design steps. All items in `design-steps.md` are ticked. The plan is ready for implementation at
  Slice 1.
- If a later pass adds steps, keep the principles in the earlier "Notes for ..." sections. Do not file again the
  items that passes 2 and 3 accepted.
- Positional-literal mutation candidate: `positional-factory` in `src/closed_record/impl/jvm.clj` (the `new` form
  near line 231). Pass 3 entries above give its expected effect.

## Implementation (2026-10-19)

- All slices done in one pass. `test/closed_record/literal_test.clj`: 6 tests. No emitter defect; `src` not changed.
- Round-trip and `print-dup` tests each loop over a plain and a nested `r`.
- The constant macro is private; the extra-key test calls `macroexpand-1` on the fully qualified symbol.
- I-07 `scope:`/`basis:` updated (continuation lines indented 2 spaces). `mementum/state.md`: V-04, V-05 done.
- Mutation check (run with `clojure -M:test -e "(require ...) (run-tests ...)"`, each change reverted):
  - round trip: `seq` maps keys to `(reverse fields)` → `(= r v)` fails (plain), nested errors. Also fails
    `print-dup`, extra-key print and both constant tests (`emitValue` uses `entrySet` = `(set this)`).
  - `print-dup`: `alter-var-root #'*verbose-defrecords*` true in the test ns → the prefix assertion fails.
  - positional literal: `positional-factory` emits `(new classname ~@(reverse field-args))` → `(= (->T 1 2) v)`
    fails (others fail or error too).
  - extra-key print: the `seq` reverse change → `(= lit v)` fails.
  - value constant: `withMeta` passes `nil` as meta → `(= {:m 1} (meta v))` fails (only this test).
  - extra-key constant: `entrySet` = `(set (concat this __extmap))` → `check` of the compiled result throws
    `extra-keys` (only this test). Confirms that `emitValue` reads `entrySet` (I-09).
- Rejected changes: `seq` drops `:n` (NPE at namespace load, no per-test signal); `IType` in the interfaces
  (VerifyError at namespace load: `emitValue` emits all fields); `iterator` adds `__extmap` (no test fails:
  `emitValue` does not use `iterator`).
