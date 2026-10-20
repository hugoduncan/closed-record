# Design steps

- [x] Architecture (D-19, V-09): correct the Context row "`map->Name` | `^boolean` missing | `ClassCastException`".
  D-19 states that a missing primitive field throws `NullPointerException` on the JVM. Observed on Clojure 1.11.4:
  `map->Name` and `create` with only `:b` missing throw `NullPointerException` for `C` and `D`. The
  `ClassCastException` comes from the `^int` field: a `Long` value for `:i` (for example `:i 1`) fails the cast to
  `Integer` in `create`. Give the row with an input that has only `^boolean` missing and the result
  `NullPointerException` (D-19), so that the V-09 test checks D-19 and not the `^int` cast. If the `^int` box-class
  cast in `create` is wanted as a case, give it as a separate row.
- [x] Ambiguity (Scope 2, helper): "It compares the results" does not say how. A `C` value and a `D` value are never
  `=` (`=` requires the same class, D-07), and `=` does not distinguish `Integer` from `Long`
  (`(= (int 5) 5)` is true), so a cast to the wrong box class passes. State the comparison: for example, a record
  result is compared as `(into {} x)` plus the class of each value, and a scalar result by value and class. Also state
  that the test fails if only one side throws.
- [x] Ambiguity (Scope 4, Scope 5, Context nil paragraph): the Decisions make only V-09 differential. State whether
  the V-08 and V-06 tests use the helper or fixed expected values. For V-08, state what "`=` and `hash` agree" compares.
  `hash` of a `C` value and of a `D` value differ (the type hash is part of `hasheq`, D-07), so a differential
  comparison of `hash` fails. For example: two equal `N` values are `=` and have the same `hash`; `.hashCode` and
  `.equals` agree with `{:a nil :b nil}`; each result is the same as for the `defrecord`.
- [x] Ambiguity (Acceptance 3): "each new test" does not give the unit. State if one mutation is necessary for each
  `deftest`, or for each row of the type hint table and each operation in Scope 4 and Scope 5. One `deftest` that
  holds all table rows can pass Acceptance 3 with one mutation, while most rows have no failure signal.
- [x] Inconsistency (Context, "Observed facts"): the heading says "current `main`". The repository branch is
  `master`; there is no `main`. Name the branch `master`, or give the commit sha, so that the facts can be reproduced.
