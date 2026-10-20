# Design steps

- [x] Architecture: make the I-01 check in Scope 4 cover the subject of I-01. The subject of I-01 is "seq, count,
  contains?, java.util.Map views" (`ramora/defclosedrecord/invariants.md`). The design checks `keys`, `.keySet`,
  `contains?` and `count`. It does not check `.size`, `.values` and `.entrySet`. V-14 (`hidden_key_test.clj`) checks
  all of these views. Add these views to the check (for example: `.size` is 3, the keys of `.entrySet` are `=` to the
  declared keys, `(count (.values x))` is 3), or state in Decisions why the subset is sufficient.
- [x] Architecture: keep `ramora/defclosedrecord/embodiment.md` aligned with the new artifacts. The Artifacts table
  names a test file for each V-id that has its own namespace (`compiler_rules_test.clj` V-13, `hidden_key_test.clj`
  V-14; task 006 added that row). The `deps.edn` row gives the contents of the aliases. Add an acceptance criterion:
  the Artifacts table has a row for `test/closed_record/property_test.clj` (V-03), and the `deps.edn` row names
  `test.check` in the `:test` alias.
- [x] Ambiguity: specify the value generator for field values and `assoc` values ("random value, nil included").
  With `gen/any`, a value can be `##NaN`, and `(= ##NaN ##NaN)` is false, so the model check `(into {} result)` `=`
  model fails with no emitter fault. Name the generator (for example `gen/any-equatable`, or `gen/small-integer` with
  `nil`).
- [x] Ambiguity: give the number of trials and the length range of the step sequence. The intent says "long
  sequences", and Scope 5 says "a fixed number of trials", but the design gives no values.
- [x] Ambiguity: specify how the property reports to `clojure.test` (Scope 5, acceptance 3). State whether the
  property body returns a boolean (no `is` in the body, one `is` on `(:pass? result)` in the `deftest`) or uses `is`
  for each check. With `is` in the body, each trial and each shrink step adds assertions and failures. State which
  keys of the `quick-check` result the failure message shows (for example `:seed` and `[:shrunk :smallest]`).
- [x] Ambiguity: make the I-01 check in Scope 4 exact. (a) Give the candidate keys for "the keys for which
  `contains?` is true" (for example the declared keys and the two undeclared keys of the step generator). (b) State
  whether "`check` returns the value" means `identical?` or `=`.
- [x] Ambiguity: define "each check in Scope 4" for acceptance 4. The last bullet of Scope 4 has five sub-checks
  (`keys`, `.keySet`, `contains?`, `count`, `check`), and the bullet "the sequence continues" is not a check. State
  whether each sub-check needs its own `src` change, and list the checks that need a change.
- [x] Inconsistency: Out of scope, bullet 1, says "V-02 covers them" (`conj`, `merge`, `update`, `into`,
  `with-meta`). `mementum/state.md` lists V-02 as not done. The V-01 example tests in `defclosedrecord_test.clj`
  (`conj-test`, `merge-test`, `into-test`, `update-test`, `metadata-test`) cover these operations now. Change the
  reason to agree: V-03 is about `assoc` and `dissoc` sequences only; V-01 has example tests for the other
  operations, and V-02 (not done) is the planned differential test.
- [x] Ambiguity: state which exceptions a check catches, and how a throw inside a check is reported (Scope 4,
  Scope 5, acceptance 4). Scope 4 says that "an exception that no check expects" makes the trial fail, with the
  exception in `[:shrunk :result-data]`. It does not say if a throw in the operation of a check is a failure of that
  check (`{:check <name>}`) or an unexpected exception. Examples: `(check x)` throws `:closed-record/extra-keys`
  (`:check`); `assoc` of a declared key throws (`:assoc-type`); `assoc` of an undeclared key throws an exception
  that is not `ex-info` (`:assoc-undeclared-throws`). Acceptance 4 counts a change only for the check that
  `[:shrunk :result-data]` names. A valid `:check` change makes `check` throw, because a change that makes `check`
  return a different value breaks `check` for all values (mutation rules). If a throw is an unexpected exception,
  no change can count for `:check`. State the rule, for example: each check catches any `Throwable` from its own
  operation and fails with its name.
- [x] Inconsistency: make the emitter method list in Context agree with Scope 4 and the Decisions. Context names the
  methods `assoc`, `without`, `seq`, `count`, `containsKey` and `keySet`. The I-01 checks also use `size`, `values`
  and `entrySet`, and `:assoc-entries` reads the result through `iterator` (`(into {} result)`). The Decisions say
  "Each view is a different emitter method". Add `size`, `values`, `entrySet` and `iterator` to the list. Also
  name `check` and `-extra-keys` (`src/closed_record/core.cljc`), which the `:check` check uses.
- [x] Inconsistency: make the Decisions bullet on the I-01 views agree with the emitter. It says "Each view is a
  different emitter method, so a fault in one method shows only in its view". In `src/closed_record/impl/jvm.clj`,
  `size` calls `.count`, and `keySet`, `values` and `entrySet` call `keys`, `vals` and `set` (all through `seq`).
  Thus a fault in `count` shows in `:count` and `:size`, and a fault in `seq` shows in `:keys`, `:key-set`,
  `:entry-set` and `:values`. Only a fault in the body of a view method shows only in its view. Change the sentence
  to agree, for example: "Each view has its own method body. A fault in that body shows only in its view. A fault in
  `seq` or `count` shows first in `:keys` or `:count`." This also tells acceptance 4 to change the body of the view
  method, not `seq` or `count`.
- [x] Ambiguity (plan review): specify the valid change for the `:check` mutation (steps.md Slice 3, plan.md
  approach 3). The step says "make `check` in `core.cljc` throw for a valid value". The mutation rules
  (`mementum/knowledge/testing-generated-types.md`, "Test design") say that a change that breaks `check` for all
  values does not count. The step does not say which valid values throw, so an unconditional throw in `check` agrees
  with the step text but does not count. The candidate in `implementation.md` (design pass 1) is a change in
  `jvm.clj`, not in `core.cljc`: the `assoc` clause passes `(assoc __extmap k v)` in place of `__extmap`. Change the
  plan and the step to name this change (or another change that makes `check` throw only for some values), and
  remove "or `core.cljc` for `:check`" if the change is in `jvm.clj`.
- [x] Inconsistency (plan review): put the emitter fix of design Scope 6 in the slice where a real fault shows.
  Scope 6 says: "If a test fails because of the emitter, fix the emitter ... Record the cause in
  `implementation.md`." steps.md has this fix only in Slice 3, with the condition "If a mutation run exposed a real
  emitter fault" (plan.md, slice order 3, is the same). A mutation run tests changed code, so it does not show a real
  fault. A real fault shows in the Slice 2 runs on unchanged `src`, and Slice 2 has no fix step and no step to
  record the cause. Move the fix step (fix `jvm.clj`, record the cause in `implementation.md`, run the three aliases
  again) to Slice 2, after the scry runs, and change plan.md to agree.
