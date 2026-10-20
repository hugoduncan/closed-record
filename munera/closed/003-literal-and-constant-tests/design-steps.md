# Design steps

Follow-up items from design reviews. Tick an item when design.md is updated.

## Architecture review (2026-10-19)

- [x] Basis of the `print-dup` positional-form assertion. Scope 2 says that, with `*print-dup*` true, "the output is
  the positional form". D-09 and C-07 name the print form `#ns.Name{...}` only. No invariant or decision names the
  `print-dup` output, and I-06 names only the round trip. D-16 requires that each test traces to an invariant
  (`verified-by`). The Decisions also say that a Clojure detail that is not a design commitment gets no test (the
  top-level evaluated path). Add to the design the architecture item that the positional-form assertion verifies
  (for example: I-07 "positional reads drop `__extmap`", through the positional constructor), or record it as an
  observed Clojure printer fact (`*verbose-defrecords*` false) that the test keeps as a change signal, as V-13 does.
  Do not change the scope items.
- [x] Assertion-record shape of the I-07 change. Scope 4 adds an observed condition to I-07: an evaluated top-level
  form keeps the object, and only a constant that the compiler emits into a class goes through `emitValue`. The
  architecture records `observed` facts with a source (`ramora/defclosedrecord.md`, context evidence). E-03 names only
  `emitValue`, not the evaluation path that does not call it. Say in the design which I-07 fields change: the
  condition goes into `scope:`, and `basis:` names the source of the observation (the 2026-10-19 observation in this
  task, and the `Compiler` evaluation path of a constant). Keep the change in `invariants.md`, as Scope 4 says.

## Ambiguity review (2026-10-19)

- [x] The form that the constant macro evaluates. The Decisions say that the macro "calls `eval` at expansion time on
  a form that builds the record". For the extra-key case of Scope 3, the design does not say which form builds a
  record with extra keys. Observed (2026-10-19, Clojure 1.11.4): `eval` of a form that contains the record object
  (`(eval lit)` or `(eval (list 'identity lit))`) compiles it as a constant, so the extra keys are dropped before the
  macro returns. Then the test passes even if the `deftest` constant keeps `__extmap`. `eval` of
  `'(T/create {...})` or `'(read-string "#ns.T{...}")` keeps the extra keys. A macro body that calls `T/create`
  without `eval` also works. Name the form (Scope 3 says "from a literal", so `read-string` of a literal agrees with
  it; `T/create` does not read a literal), and say that the macro result must fail `check` before compilation. Say
  if `eval` is still necessary.
- [x] Assertions on printed output. Scope 2 says "the output is the positional form" (`print-dup`) and "the output
  has the declared keys only" (extra keys). The design does not say how to assert this: exact string equality with
  an expected literal string (for example `"#closed_record.literal_test.T[1, 2]"`), equality with `pr-str` of a
  record made by `->T`, or a check of the read-back keys. Exact strings depend on the Clojure printer separators.
  State the assertion for each printed-output test.
- [x] Location of the mutation in Acceptance 3. The tested paths are in Clojure, not in closed-record code. The design
  does not say where a breaking change can be made: only a temporary edit to `src/closed_record/impl/jvm.clj`, or
  also a change in the test (for example a `defrecord` instead of the closed type). It also does not say if one
  change can cover more than one test. State the permitted locations, and that each change is reverted.

## Inconsistency review (2026-10-19)

- [x] Top-level evaluated forms. The observed facts say that the object is kept in "a top-level `def` that is
  evaluated". Scope 4 says "An evaluated top-level form keeps the object", and the Decisions say "The top-level
  evaluated path keeps the object". Observed (2026-10-19, Clojure 1.11.4, `load-file`): `(def d (m))` and
  `(def d (identity (m)))` keep `__extmap`. A top-level `(check (m))` or `(let [x (m)] ...)` drops it, because
  `Compiler.eval` compiles a non-`def` top-level form into a function, so the constant goes through `emitValue`.
  Also, the Decisions call this path "not a design commitment", but Scope 4 puts the claim into the invariant I-07.
  Change the Scope 4 text and the Decision so that they agree with the observation: only the init of a top-level
  `def` keeps the object. Say if I-07 states this as a fact or only limits "code constants" to constants that the
  compiler emits into a class. See also the architecture step on the I-07 assertion-record shape.

## Plan ambiguity review (2026-10-19)

- [x] Metadata on the round-trip records. Plan slice 1 says "round trip (plain, nested, metadata-free)". The design,
  plan Approach and steps.md do not say what "metadata-free" means. Observed (2026-10-19, Clojure 1.11.4,
  `defrecord`): with `*print-dup*` true, `pr-str` of a record with metadata gives
  `^#=(clojure.lang.PersistentArrayMap/create {:m 1}) #user.P[1, 2]`. Then the output does not start with `#` and
  the class name, and the read-back goes through `#=` (`*read-eval*`). Without `*print-dup*`, metadata is not
  printed. Say in plan.md (and the `print-dup` step) that the `r` of each round-trip test has no metadata, and that no
  test asserts on the metadata of a read-back value (no invariant names it, D-16). Or remove "metadata-free" from the
  plan.

## Plan inconsistency review (2026-10-19)

- [x] Task summary in `munera/plan.md`. The entry says "Tests only, unless a test finds an emitter defect". Design
  Scope 4, Acceptance 5, plan slice 3 and the I-07 step in steps.md change `ramora/defclosedrecord/invariants.md`,
  and Acceptance 6 changes `mementum/state.md`. The entry dates from task creation (`27529f1`), before the design
  review added Scope 4. Change the entry so that it names the I-07 wording change.

## Plan ambiguity review, pass 2 (2026-10-19)

- [x] Criterion for a valid mutation. Acceptance 3 says that each new test "fails when the behaviour that it checks
  is broken", and gives path-specific examples (`seq` returns `__extmap` entries, the type implements `IType`). The
  plan Approach and the steps.md mutation step say only "a temporary breaking change". They do not say that the
  change must break the behaviour that the test checks. A change that breaks all values (for example `.equals`
  returns false, or `check` always throws) makes each of the 6 tests fail, and it agrees with the step text, but it
  does not show that a change in the printer, reader, positional constructor or `emitValue` path causes a failure
  (Intent). The Risks say that the value-constant test may be hard to break, which makes such a change more likely.
  Say in plan.md and in the mutation step that each change breaks the path or property that the test checks, and
  that a change that makes all tests fail through `=` or `check` does not count. Say also if `implementation.md`
  records the assertion that failed for each change.
