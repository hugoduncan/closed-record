# Design steps

- [x] Architecture: make the check list match the claim "each permitted operation" (S-02, V-02), or narrow the
  claim. The design closes V-02 (Scope 6: `mementum/state.md` lists V-02 as done). The embodiment Method behaviour
  table (`ramora/defclosedrecord/embodiment.md`) gives separate emitter methods for permitted operations that no
  check reaches:
  - the `java.util.Map` views `.size`, `.get`, `.containsKey`, `.keySet`, `.values`, `.entrySet` (I-09), and
    `containsValue` and `isEmpty`;
  - `getLookupThunk` (D-10, C-02). The `:keyword` check takes `k` from a vector, so `(k x ::nf)` calls
    `Keyword.invoke` and then `valAt`. It does not make a keyword call site;
  - `record?` (C-08).
  For each permitted operation in the purpose Behaviour tables and in C-02 to C-08, give the check in Scope 3 that
  compares it, or the V-id that covers it (for example V-06 for call sites, V-14 for the views). Put this map in
  Decisions, so that "V-02 done" is a true statement.
- [x] Architecture: make the property cover the hash part of I-05. I-05 (`verified-by: V-02, V-12`) says that the
  equality and hash of `r` are equal to the `defrecord` result, and that metadata has no effect. Scope 3 compares
  `.hashCode` and `.equals` across types, and `=` in the same type (`:start-equal`). It does not compare `hash`
  (`hasheq`) in the same type. The rule in `mementum/knowledge/testing-generated-types.md` is: compare `hash`
  equality inside each type, then compare the two results. Add a check (for example `:start-hash`:
  `(= (hash x) (hash x0))` = `(= (hash y) (hash y0))`), or state in Decisions that V-12 alone verifies the hash part.
  Also, `withMeta` copies the `__hash` and `__hasheq` caches. The property calls `.hashCode` after the start and
  after each step, thus before each `[:with-meta m]` step. A fault that puts `__meta` into a hash method then does
  not change the result. State that V-12 verifies "metadata has no effect" for hash, or make the hash checks read
  values that have no cached hash.
- [x] Architecture: make the "intended difference" path in Scope 5 follow the decision records. The architecture
  records each intended difference from `defrecord` as a decision (D-04, D-05, D-07, D-19), and the purpose
  Behaviour tables refer to it. The quality target "It behaves like `defrecord`, except for closed keys" needs the
  docs to list each difference. Scope 5 says only: record it in `purpose.md`, and remove it from the property. State
  that an intended difference (a) refers to an existing D-id, or gets a new decision in
  `ramora/defclosedrecord/decisions/` and in the index in `decisions.md`, (b) is in the purpose Behaviour tables with
  that D-id, and (c) gets an Out of scope bullet in this design with that D-id, as the current bullets have. A new
  difference changes public behaviour (commitment high, D-20, principle 15). State that a human decides if a
  difference is intended, and that the agent does not decide it.
- [x] Ambiguity: give the exact form of the `:conj`, `:merge` and `:into` steps in Scope 2. `cons` accepts a map
  entry, a vector pair or a map (embodiment, `cons e`), so each step has more than one form. State the form for each
  step, for example `(conj x [k v])`, `(merge x {k v})` and `(into x [[k v]])`, or state that the generator picks one
  of these forms.
- [x] Ambiguity: specify what occurs when an operation throws. (a) Scope 3 says that each check catches a
  `Throwable` from its operation. It does not say what occurs when a step (Scope 2) throws on `x` or on `y`. In task
  007, the step is an `attempt` and a named check judges the outcome (`:assoc-entries`). If the step is not caught,
  test.check gives only `{:clojure.test.check.properties/error e}`, with no check name. Give the name of the failure
  for a step that throws. (b) State whether a check fails when the operation throws on one side only, or on either
  side. If the same exception class on both sides passes, say so. (c) Give the keys of the failure `result-data`
  apart from the check name: for example `:ex`, and, for `:get`, `:keyword`, `:contains` and `:find`, the key and
  the two values.
- [x] Ambiguity: give, in acceptance 3, the check that each mutation must make fail. The property stops at the first
  failed check, and shrinking can name a different check (`mementum/knowledge/testing-generated-types.md`). State
  the expected check name in `[:shrunk :result-data]`: for example `:seq` for the `seq` order change, and `:get` for
  the `valAt` change. Also state whether the `valAt` change is in `valAt k nf` only (default → nil).
- [x] Ambiguity: specify the `mementum/state.md` change in Scope 6 and acceptance 5. The Status section has a
  "Tests:" list with one line for each test file, and a "Verification plan done" line with a "Not done" list. State
  that the change adds `test/closed_record/differential_test.clj` (V-02) to the "Tests:" list, and moves V-02 from
  "Not done" to "done".
- [x] Inconsistency: Intent says "The only intended differences are the closed key set and the type". Out of scope
  gives two more intended differences: `dissoc` of an undeclared key returns the same `P` and a new `R` (I-04, D-04),
  and `hash` and `=` between a `P` and an `R` (D-07). Make the Intent agree with Out of scope, for example: "The
  intended differences are the closed key set, the type (D-07), and the identity of a `dissoc` result (D-04)".
- [x] Inconsistency: Out of scope, bullet 5, says that no mutation check for each named check is necessary because
  "the operations here call the same methods" as task 007 (V-03). The task 007 property
  (`test/closed_record/property_test.clj`) calls `seq` (`keys`), `keySet`, `entrySet`, `containsKey`, `count`,
  `size`, `values`, `iterator` (`into {}`), `assoc` and `without`. The checks in Scope 3 also call `valAt`,
  `entryAt`, `meta`, `withMeta`, `cons`, `hashCode`, `equals` and `equiv`. Task 007 does not call these. Acceptance 3
  also has a mutation in `valAt`. Correct the reason, for example: name the tests whose mutation runs covered those
  methods (V-01, V-12, V-14), or give a different reason.
- [x] Inconsistency: Problem says that no test names `.hashCode` in a comparison with `defrecord`. The V-12 test
  `test/closed_record/equality_test.clj` compares `.hashCode` of a closed record with `.hashCode` of a `defrecord`
  (task 005). Remove `.hashCode` from that list, or say what this task adds, for example `.hashCode` after random
  operation sequences.
- [x] Inconsistency: the Decisions coverage table row "`=` with another type or a plain map" says "V-12 (Out of
  scope, D-07)". The Out of scope bullet gives only "`hash` and `=` between a `P` and an `R`". It does not give `=`
  with a plain map. Do not change Out of scope (the scope is frozen). Make the table row agree with it, for example:
  split the row into "`=` with an `R`: V-12 (Out of scope, D-07)" and "`=` with a plain map: V-12".
- [x] Inconsistency: Out of scope, bullet 2, says that V-12 tests `hash` and `=` between a `P` and an `R`. The V-12
  test `cross-type-test` (`test/closed_record/equality_test.clj`) tests `=`, `.equals` and `.hashCode` of `->E`
  against `->DE`. It does not test `hash`. No V-12 test compares `hash` across types. The `hash` formula
  (type hash XOR `mapHasheq`, D-07) has a test in V-01 (`hash-test`, `test/closed_record/defclosedrecord_test.clj`).
  Correct only the evidence in that bullet (for example: "V-12 (`cross-type-test`) tests `=`. V-01 (`hash-test`)
  tests the `hash` formula."). Do not change the excluded items: the scope is frozen.
- [x] Ambiguity (plan review): plan.md Risks, bullet 3, says that if a view result differs under `:clj-1.12` or
  `:clj-1.13`, the implementer can "compare as seqs/sets only if both sides change identically". It does not say
  what "change identically" means, or how to tell it from a real difference. The rule also changes the Scope 3
  comparison (`(.keySet x)` = `(.keySet y)`, `(.values x)` = `(.values y)`, `(.entrySet x)` = `(.entrySet y)`)
  without a design change. `=` already compares a Clojure set with a `java.util.Set`, and a seq with a
  `java.util.List`, by content (Clojure 1.11.4: all true). A set comparison of `.values` hides an order difference.
  Replace the bullet: the view checks use `=` as in Scope 3 on each version. A failure on any version is a
  difference and follows Scope 5 (emitter fault → fix; can be intended → stop and ask). Edit only plan.md.
- [x] Inconsistency (plan review): plan.md Key decisions, last bullet, says that the design Observed facts show that
  "the two types agree on every checked operation" at commit `820e889`. The design table has only `dissoc`, `seq`,
  `conj`/`update`/`merge` entries, `get`, keyword call, `find`, `.hashCode`, `.equals` and `hash` (Clojure 1.11.4).
  It does not have `:count`, `:contains`, the `java.util.Map` view checks, `:meta`, `:start-equal` or `:start-hash`.
  The implementation.md probes add only the view classes and the `conj`/`merge`/`into` forms. Make the bullet agree
  with the design: the types agree on the operations in the Observed facts table. The other checks were not
  observed, so a failure on unchanged `src` is possible and follows Scope 5 (Risks, bullet 1). Edit only plan.md.
