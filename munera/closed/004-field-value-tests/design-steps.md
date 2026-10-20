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
- [x] Ambiguity (plan.md decision 5, steps.md Slice 5 mutation items): the plan does not say which change in `src` is
  each mutation. It names "constructor casts", "`create` box-class cast" and "D-19 missing-primitive path" as paths in
  `src/closed_record/impl/jvm.clj`, but these casts are not in `jvm.clj`. The Clojure compiler emits them from the
  `deftype*` field hints: `create` is a compiler-generated static method (`jvm.clj` only calls it, line 249), and
  `->Name` and `assoc` use `new`, which casts each argument to the field type. Also "break equality/hash or lookup
  handling of nil" names no change: the emitter has no nil-specific code. In plan.md, give for each row of the type
  hint table, each Scope 4 operation and each Scope 5 case the mutation (file, form and change) or the reason that
  no change in `src` breaks it (Acceptance 3). Examples (not verified): `assoc` builds the value with
  `(. Name create ...)` (`9.9` gives `ClassCastException`, not `9`); `->Name` calls `create` with a map (`2.7` gives
  `ClassCastException`); `map->Name` calls `->Name` (`:n 2.5` gives `2`, `^int` ← `1` gives no
  `ClassCastException`); `deftype*` gets `plain-fields` (no hints: all cast, NPE and box-class rows fail). State
  which rows have no mutation (for example `^String` ← `5`, `count`, `.field`). Update the Slice 5 items to match.
- [x] Inconsistency (plan.md Risks "CLI vs REPL divergence", steps.md Slice 3 print/read item, implementation.md
  "Design-steps fix"): plan.md says the tests use no `eval` of quoted forms, "so the known pitfalls mostly do not
  apply". implementation.md says that print-read in the CLI needs `*ns*` bound, and `literal_test.clj` reads with
  `read-string` inside `(binding [*ns* (the-ns test-ns)] ...)` (`in-test-ns`, `read-literal`). The V-08 print/read
  step gives no read function and no binding. In plan.md, state that the print/read case uses `pr-str` and then
  `read-string` with `*ns*` bound to the test namespace (the `literal_test.clj` pattern). Correct the Risks text.
  Add this detail to the Slice 3 print/read item.
- [x] Inconsistency (plan.md "Mutation map" print/read row, steps.md Slice 5 "tests that no mutation breaks" item):
  the row gives "Mutations: none", but its note says "Only M14 (the `=` path) fails it". Slice 5 lists V-08
  print/read as a test that no mutation breaks, and the M14 item lists only V-08 `=`. M14 breaks only the nil path,
  so by the rules in `mementum/knowledge/testing-generated-types.md` ("Test design") it is a counted mutation, and
  Acceptance 3 then needs the change and the failed assertion, not a reason. Choose one rule and apply it in both files:
  (a) list M14 for print/read in the map and in the Slice 5 M14 item, and remove print/read from the
  "no mutation breaks" item (note: no change on the print or read path itself fails it); or (b) keep "none" and
  state why M14 does not count for print/read (it breaks `=`, not the round trip), and remove "Only M14 ... fails it".
