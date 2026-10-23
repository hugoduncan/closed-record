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
