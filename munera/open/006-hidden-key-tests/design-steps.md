# Design steps

- [x] Fix the "Out of scope" item on `map->Name`. It says that `map->Name` of a map with extra keys throws
  `undeclared-key`. It throws `:closed-record/extra-keys` (`map-factory` in `src/closed_record/impl/jvm.clj`; the
  `defclosedrecord` docstring in `src/closed_record/core.cljc`; the test at `test/closed_record/defclosedrecord_test.clj`
  line 200). Change only the error name. Keep the item out of scope.
- [x] Make the decision on `update` and `into` agree with the retention cases. The decision says that `update` and
  `into` are not cases because they call `assoc` and `conj`, which are cases. By the same rule, `merge` is not a case,
  because it calls `conj`. But Scope 4 has `merge` as a case. Give a rule that includes `merge` and excludes `update`
  and `into`, or remove the rule and give the reason for each operation. Do not change the list of cases in Scope 4.
- [x] Fix the first item in "Risks" in plan.md. It gives `count` as an example of an operation that `check` uses.
  `check` (`src/closed_record/core.cljc`) reads only `-extra-keys`, so a `count` mutation does not break `check`.
  Remove the example or replace it with a true one. Keep the rule that acceptance 3 permits a recorded reason.
- [x] Make Slice 3 in steps.md agree with plan.md and acceptance 3. Slice 3 says "For each Scope 3 operation group"
  and records one change and assertion for each group. plan.md and acceptance 3 need a record for each operation;
  one change can cover several operations only if each of them fails with it. Change the step to "each operation".
- [x] Make the emitter-fix step in steps.md agree with design Scope 5. Scope 5 says: fix the emitter if a test fails
  because of the emitter. Slice 3 says "If a mutation run found a real emitter defect (a test fails on unmutated
  code)". This does not agree with itself: a mutation run uses mutated code. Only the runs in Slice 1, Slice 2 and
  Slice 4 use code that is not mutated, and those slices need zero failures but have no fix step. Change the step so
  that it applies to a failure on unmutated code in any run, or put it in the slice where the failure can occur.
  Keep the plan.md slice order, or change plan.md to agree with steps.md. Do not change design.md.
