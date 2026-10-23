# Design steps

- [x] Architecture: state the namespace and the visibility of the shared throw helper (Scope 2), and make it fit the
  dependency boundaries in `ramora/defclosedrecord/embodiment.md#boundaries`.
  - The boundaries permit generated code → `closed-record.core/ClosedRecord`, `clojure.lang`, public `clojure.core`
    vars and JDK classes only. They forbid generated code → `impl.*`. Thus the helper has no permitted location now.
  - A public var in `closed-record.core` breaks D-20 and principle 9 (two public vars). A private var in `core`
    is not on the permitted list. A helper in `impl.*` breaks the "generated code → `impl.*`" rule (the emitter
    namespace is for expansion time only).
  - `core` is a `.cljc` file (D-13, K-02). A helper there must be portable or behind a reader conditional.
  - Choose one location. Record the new permitted dependency, with its rationale, in the new decision (Scope 6), and
    align the embodiment boundary list with it.
- [x] Architecture: make the new decision in `decisions/type-structure.md` (Scope 6) a full decision record:
  a new ID (D-21), the shape `⟨subject, choice, rejected, rationale, assumptions, qualities, scope, commitment⟩`
  from `decisions.md`, and an index entry in `decisions.md`. `ramora/defclosedrecord.md` says "D-01 to D-20";
  align it.
- [x] Ambiguity: state the dispatch of the new `assoc`. The decision "Fix the size, not the dispatch" says to keep
  `case`. The decision "Follow the `defrecord` shape where it is smaller" and the `defrecord` facts in Context point
  to `condp identical?`. Say which one, or say that the implementer can choose, with the size target as the rule.
- [x] Ambiguity: give the contract of the shared throw helper (Scope 2).
  - Its arguments, for example the `:type` keyword, the class and the key.
  - Whether the helper builds the message and the `ex-info` data. If the call site still builds the `str` form or the
    map literal, the method stays large.
  - How one helper gives the two different messages of `assoc` and `without` with the same text as now (the message
    uses `cname-str`, the helper has the class).
  - Whether the helper throws, or returns the exception for a `throw` at the call site.
- [x] Ambiguity: define the measurement for acceptance criteria 1 and 2.
  - The Clojure version (the default alias, 1.11.4?). The design says that bytecode size depends on the version.
  - What "bytes" is. The table gives `P::assoc` 994 (`javap`), and `PrintInlining` gives 995.
  - The JVM flags and the hot loop for `PrintInlining`. Record the commands in `implementation.md`, so that a
    reviewer can repeat the measurement.
- [x] Ambiguity: state when the bench acceptance run occurs and what follows if it fails.
  - "The first full run" in task 011 was of the finished harness. Here, is it the first full run after the final
    emitter change? Are shorter runs (one fork, one operation) permitted before it?
  - If `assoc` still fails (acceptance 4), is the task complete and closed, with V-11 not done? Or does the task
    stay open? Say the same for a row that passed in task 011 and fails now.
- [x] Inconsistency: Scope 6 says "V-11 is done only if all six operations pass". S-03 (`purpose.md`) and the task
  011 rule also require the field declaration test: field access passes only if the `(.a x)` row and that test pass.
  Align Scope 6 and acceptance 4 with S-03.
- [x] Inconsistency: `mementum/state.md` says "The probable cause of the `assoc` gap is the emitter dispatch (`case`
  against `condp identical?`)". The design finds that the cause is the method size, not the dispatch. Scope 6 updates
  `state.md` with the V-11 status only. Say that this update also replaces the stale cause statement.
- [x] Architecture: D-21 rests on host facts that the architecture description does not record. Record them, so that
  D-21 traces to context and risks as D-01 does (K-06, E-xx, R-01).
  - `context.md` has the compiler facts that decisions use (Constraints K-xx, Evidence E-xx, `observed`). D-21 uses
    two new facts: C2 inlines a hot method only below `FreqInlineSize` (325 bytes), and a `case` on keywords is a
    `tableswitch` over the keyword hash range (size depends on the hashes). Now they are only in Context of
    `design.md` and in `implementation.md`.
  - `qualities.md` puts decision assumptions in Risks with a signal (R-01 for D-01) and decision costs in
    Trade-offs. D-21 has the assumption "C2 inline limits stay near their current defaults" and the cost "generated
    code depends on a private var of `core`; AOT user classes refer to it by name". No risk or trade-off is named.
  - Say in Scope 6 where these facts, the risk (signal: V-11) and the trade-off go, or say why D-21 does not need them.
- [x] Architecture: Scope 6 updates the embodiment boundary lists, but other architecture text still describes the
  old dependency set. Align it with D-21.
  - `embodiment.md` Forbidden dependencies: "`impl.*` → `core`. ... It emits the protocol name only as a symbol."
    The emitter now also emits the `key-error` var name as a symbol (Scope 2).
  - `embodiment.md` Artifacts: the `core.cljc` row lists the macro, `check` and the `ClosedRecord` protocol. It
    must also name the internal run-time helper `key-error` that generated code calls.
  - `principles.md`: principle 6 (Explicit boundaries) lists D-13 and D-17. D-21 adds a permitted dependency, so add
    D-21 there (and to principle 28 or 15 if D-21 applies it).
- [x] Ambiguity: state how the private helper `key-error` (Scope 2) passes acceptance 5 (`clj-kondo`: no warnings).
  - Only generated code calls `key-error`, through its var. `core` does not call it. clj-kondo then reports
    "Unused private var closed-record.core/key-error" (observed on a scratch copy of the shape). A test call through
    `#'closed-record.core/key-error` from another namespace does not remove the warning (observed).
  - Say which method the implementer uses: for example a `#_:clj-kondo/ignore` on the `defn-`, an entry in
    `.clj-kondo/config.edn`, or an other method. Say if D-21 or `embodiment.md` must name it.
- [x] Inconsistency: the D-21 commitment (Scope 6) does not agree with the commitment levels in `decisions.md`.
  - `decisions.md`: "medium: internal mechanism. A change affects one emitter or one internal namespace." "high:
    public behaviour or API. After the first release, a change is breaking (D-20)."
  - D-21 is "medium", but its own text says that a change affects the emitter and `core` together, and that user
    classes AOT-compiled with an earlier version fail. That is two namespaces, one of them public, and a break for
    users.
  - Make the level and its reason agree: choose "high", or keep "medium" and say why (for example the D-17
    precedent, K-03 before the first release, or a change by a new helper name per D-20).
- [x] Inconsistency: the Observed facts size table uses a different unit from the rest of the design.
  - The table gives the offset of the last instruction (`P::assoc` 994). Problem, D-21, `state.md` text, Scope 4
    and acceptance 1 use the code length (`P::assoc` 995, "below 325 bytes, code length").
  - Scope 4 asks for "the bytecode size table above for the changed emitter", so the evidence table can copy the
    offset unit, and a 324 offset (code length 325) then looks like a pass.
  - Give the table in code length (add 1 to each value), as the measurement command in `implementation.md` prints.
- [x] Architecture: D-02 (`ramora/defclosedrecord/decisions/closed-key-set.md`) rests on the `case` mechanism that
  D-21 removes. Scope 6 does not update it.
  - D-02 Qualities: "The check costs nothing on the declared-key path, because only the `case` default branch
    throws." After this task, `assoc` and `without` have no `case` (Scope 1). D-12 uses this D-02 claim as its
    rationale, and the design Decisions cite D-02 and D-12 for the out-of-line throw.
  - Say in Scope 6 how D-02 Qualities changes, so that it names the D-21 mechanism (the `identical?` dispatch of
    `defrecord`, and the throw helper call only on the rejected path), or say why D-02 stays.
- [x] Ambiguity: state what the `mementum/knowledge/benchmarks.md` update (Scope 6) does with the current `assoc`
  gap text.
  - The "criterium 0.5.251-ALPHA" section says: "The `assoc` gap is a real emitter cost that the folding hid." and
    "The task 011 acceptance run ... gives `assoc` `P / R` 1.193 ... This is the current gap, the start point for the
    `assoc` task."
  - Scope 6 only adds "method size and inlining as a cause of a gap, and how to find it". After this task, "current
    gap" and "start point" are stale.
  - Say if the update replaces these sentences with the task 012 acceptance-run result for `assoc` (ratio and pass
    or fail) and the measured cause (method size), or keeps them as history.

## Plan review (ambiguity)

- [x] Ambiguity: state when the `:clj-1.12` and `:clj-1.13` test runs occur, and what follows if a final check fails
  after the acceptance run.
  - Slice 2 runs the tests on the default alias only. Slice 4 "Confirm the emitter is final: tests pass" does not
    name the versions. Slice 6 runs the three versions after the acceptance run.
  - Scope 5 and the plan Risks say that the emitter does not change after the acceptance run. If a slice 6 check
    fails on `src` (a test on 1.12 or 1.13, clj-kondo, cljfmt), the fix needs a `src` change that is not permitted.
  - Say if the three-version tests (and lint and format) run before the acceptance run, as part of the "emitter is
    final" gate. Say what the task does if a check fails after the run.
- [x] Ambiguity: state what follows if the acceptance run exits 2 (a fork fails).
  - Scope 5 and Slice 4 give the rules for a failed row (exit 1) only. With exit 2 the parent prints no table, so
    there are no ratios for `state.md` and V-11 has no result. "The first full run is the acceptance run. No later
    run replaces it" then leaves the task with no bench result.
  - Say if a run that exits 2 counts as the acceptance run, or if the task records it and does one more full run
    (the emitter does not change).
  - Say how the run is captured, so that "complete output and exit code" is repeatable: for example the
    `tee` and `PIPESTATUS` command of `mementum/knowledge/benchmarks.md` (🎯 acceptance run).

## Plan review (inconsistency)

- [x] Inconsistency: the rows of the evidence size table differ between Scope 4 and Slice 3.
  - Scope 4: "the bytecode size table above for the changed emitter". The Observed facts table has 8 rows
    (`without`, `assoc`, `getLookupThunk`, `entryAt`, `containsKey`, `valAt(k, nf)`, `cons`, `equiv`).
  - Slice 3 and the recorded `javap` command (`grep -E " (assoc|without|containsKey|entryAt|valAt)\("`) give 5
    methods. Acceptance 1 says only "the size table for `P` and `R`".
  - Choose one row set (for example the 5 rows, with `valAt` as the unchanged reference), and make Scope 4,
    acceptance 1 and Slice 3 name it.
- [x] Inconsistency: the `closed-record.impl.jvm` namespace docstring is not in the plan, but Scope 6 changes the
  same claim in `embodiment.md`.
  - `src/closed_record/impl/jvm.clj` lines 5–6: "This namespace does not require closed-record.core. It emits the
    ClosedRecord protocol interface only as a symbol." After Slice 2 it also emits `#'closed-record.core/key-error`.
  - Scope 6 changes the `embodiment.md` sentence to "the protocol name and the `key-error` var name only as
    symbols". The docstring then disagrees with it.
  - The docstring is in `src`, so the change must occur before the acceptance run (Scope 5, plan Risks). Add it to
    Scope 1 or Scope 6 and to Slice 2.

## Plan review 2 (ambiguity)

- [x] Ambiguity: state how Slice 4 classifies a full run that exits 1 (or with another code) and prints no table.
  - Plan Key decisions and Slice 4: "Exit 0 or 1: the run printed the table. It is the acceptance run." The
    exit code does not prove this. `clojure -M -m` exits 1 on an uncaught exception, for example a start-up or
    dependency failure, or an exception in `report/fork-stats` or `report/table` before `println`
    (`dev/bench/closed_record/bench/fork.clj` `parent`). Then exit 1 has no table. A kill or an interrupt gives
    another code (for example 137 or 130), and the plan does not name it.
  - With such a run, the two rules disagree: "exit 1 → acceptance run" and "no table → no timed result" (the
    exit 2 rule).
  - Say which signal classifies the run (for example: the table is in the captured output). Say that a run with
    no table, with any exit code, follows the exit 2 rule (record it, no `src`, `test` or `dev` change, one more
    full run, a second such run → ask the user). Align plan Key decisions, Risks and Slice 4.

## Plan review 3 (ambiguity)

- [x] Ambiguity: state how the Slice 3 commands select Clojure 1.11.4 (Scope 4: "Clojure 1.11.4 (the default
  alias)").
  - Slice 3 says "using the recorded commands in `implementation.md`". These commands run in a scratch directory
    outside the repository with `-Sdeps "{:paths [...]}"` only. Thus the CLI uses the Clojure version of the root
    deps (observed: 1.12.6 with CLI 1.12.6.1673), not the 1.11.4 of the repository `deps.edn`.
  - The same applies to the `PrintInlining` command. Slice 3 records the `java -version` output, but not the
    Clojure version.
  - Say how the commands pin 1.11.4 (for example `:deps {org.clojure/clojure {:mvn/version "1.11.4"}}` in each
    `-Sdeps`), and that `implementation.md` records the `(clojure-version)` output with the evidence. Align plan
    Key decisions (size table) and `steps.md` Slice 3.
