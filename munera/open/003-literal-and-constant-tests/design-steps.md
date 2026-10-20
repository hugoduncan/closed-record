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
