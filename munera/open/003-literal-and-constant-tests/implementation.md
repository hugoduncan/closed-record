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
