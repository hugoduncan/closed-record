# Design steps

- [x] Fix the "Out of scope" item on `map->Name`. It says that `map->Name` of a map with extra keys throws
  `undeclared-key`. It throws `:closed-record/extra-keys` (`map-factory` in `src/closed_record/impl/jvm.clj`; the
  `defclosedrecord` docstring in `src/closed_record/core.cljc`; the test at `test/closed_record/defclosedrecord_test.clj`
  line 200). Change only the error name. Keep the item out of scope.
- [x] Make the decision on `update` and `into` agree with the retention cases. The decision says that `update` and
  `into` are not cases because they call `assoc` and `conj`, which are cases. By the same rule, `merge` is not a case,
  because it calls `conj`. But Scope 4 has `merge` as a case. Give a rule that includes `merge` and excludes `update`
  and `into`, or remove the rule and give the reason for each operation. Do not change the list of cases in Scope 4.
