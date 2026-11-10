# Implementation

## Design check against value-type `4ed8d0a3` (2026-11-10)

- value-type task 022 (`e8e0c407`) is closed. Its contract
  (`ramora/deftype-star-design/public-contract.md`, "Closed record shape";
  the `defvaluetype*` docstring) agrees with VT-D13. D10 is firm, D9 is
  provisional. No Ramora decision changed; Ramora now records the pin, the
  receiving expansion, `:removed-field-access`, `value-type.value-classes`
  and the closed rows of VT-K8.
- New since `edc3f79`: a closed form is a receiving form also on identity
  fallback; the value-type fallback policy is read at first use;
  `value-type.value-classes` (`auto`, `off`); array adapters load lazily
  (the JVM flags do not change).
- Probe (EA3, `--enable-preview`, `jdk.internal.value` export,
  `defvaluetype*` forms written by hand): closed `[^long x]` and
  `[^int x ^int y]` have only the declared fields and `:flat? true`; closed
  `[^long x ^long y]` gives `:flat? false`; record shape `[^long x]` keeps
  `__meta __extmap` and gives `:flat? false`. A non-nil `__meta` and `create`
  with an undeclared key both throw `IllegalArgumentException`, with no
  wrapper.
- Current suites with the pin overridden to `4ed8d0a3` (`-Sdeps`
  `:override-deps`), no emitter change:
  - `test/value-type`, Temurin 21, identity: 89 tests, 435 assertions pass;
  - `test/value-type-clj-1.11`: 1 test, 7 assertions pass;
  - `test/value-type`, EA3, value: 87 tests, 436 assertions pass.

## Architecture review (2026-11-10)

- architectural review added 2 new design steps
- Sources: `ramora/META.md` and `doc/architecture.md` do not exist. The
  architecture is in Ramora (`ramora/INDEX.md`, `defclosedrecord*`,
  `value-type-records*`).
- No misfit with VT-D13 to VT-D15, D-11 or the ownership split. The two
  steps are about Ramora pages that the design does not name (parent
  design value-mode notes, VT-D10) and the place of the new mode
  differences in `impl.jvm` (`representation`, today:
  `:hidden-fields :hash-form :with-meta-form`; `base-interfaces`, the
  `IObj` methods, `assoc` and `-extra-keys` are shared).

## Ambiguity review (2026-11-10)

- ambiguity review added 3 new design steps
- Checked: value-type `4ed8d0a3` `doc/usage.md` has the heading
  "`deftype*` forms", so the anchor `#deftype-forms` exists. `edc3f79`
  occurs outside Munera in `deps.edn`, README (deps snippet),
  `doc/value-type-records.md` (lines 55 and 75) and the Ramora embodiment.
- Left for the inconsistency turn: acceptance criterion 1 permits "a later
  commit", but "Dependencies" pins one SHA; "If VT-V12 gives other results,
  change VT-K8" against acceptance criterion 2 (VT-V12 passes).

## Inconsistency review (2026-11-10)

- inconsistency review added 3 new design steps
- The value-type suite reads metadata outside the shared checks in
  `aot_test.clj` (VT-V7) and `non_nil_test.clj` (VT-V9, "with-meta keeps
  the fields"; the design already removes it). `representation_test.clj`
  (VT-V2) today checks only `__hash`/`__hasheq` in `specs`.

## Notes for the design-steps work (2026-11-10)

- Principles: Ramora first, then `design.md` (design rule; VT-R6). Ramora
  design pages state the current design only; differences and limits go
  only in `embodiment.md` (`ramora/INDEX.md`). Prose in ASD-STE100 style.
  Do not change the scope sections.
- Several steps edit one file:
  `ramora/value-type-records/invariants-and-verification.md` (VT-D10
  export; VT-V3 V-01 part; VT-V12 gate and pin rule). Do them in one
  change, so that VT-V3, VT-V12 and VT-D10 agree.
- The pin-rule step decides the VT-V12 gating step: if a later pin is
  allowed, VT-V12 and acceptance criterion 4 follow the pin.
- Paths: emitter `src/closed_record/impl/jvm.clj` (`representation`,
  `base-interfaces`, `generated-methods`); shared checks
  `test/runtime/closed_record/checks/` (`support.clj` has
  `define-checks` and `support/representation`); value-type suite
  `test/value-type/closed_record/value_type/` (`expectation.clj`,
  `representation_test.clj`, `aot_test.clj`, `non_nil_test.clj`); parent
  design `ramora/defclosedrecord/decisions/{type-structure,closed-key-set}.md`,
  `ramora/defclosedrecord/embodiment.md`,
  `ramora/defclosedrecord/embodiment/artifacts.md`.

## Design steps done (2026-11-10)

- All 8 design steps are done. No `SCOPE_QUESTION:` items.
- Ramora design pages changed in this pass (Ramora first): D-01, D-06
  (`type-structure.md`) and D-09 (`closed-key-set.md`) value-mode notes;
  VT-I9 verified-by, VT-V2, VT-V3 (V-01 and V-05 extra-key parts), VT-V7,
  VT-V12 gate, VT-D10 export (`invariants-and-verification.md`). The
  value-type embodiment now lists the missing export and the VT-V7 metadata
  read as differences.
- Found beyond the review: `literal.clj` `extra-key-constant-test` (V-05)
  and the reader metadata of `value-constant-test` also fail in value mode.
- `support/representation` is `:identity` in the ordinary suite and in the
  Temurin 21 value-type job. It cannot select the value-mode exclusions;
  use the record options (`:value-type true`) or a new `define-checks` key.
- value-type `4ed8d0a3`: `array-info` without the export throws
  `:array-inspection-unavailable`; `into-array` with
  `value-type.value-classes=off` throws `:array-unavailable`.
- The parent embodiment "Generated type" pointer already covers "Method
  behaviour" and "State"; no new pointer is needed. The README has no
  value-type doc link, only the deps snippet.

## Architecture review, pass 2 (2026-11-10)

- no architectural review feedback
- Checked: the "Emitter" list of mode differences is complete. In
  `impl/jvm.clj` the generated hidden-field references are only `withMeta`
  (`:with-meta-form`), `hash-form`, `meta`, `assoc` and `-extra-keys`.
  `base-interfaces` lists `IObj` (not `IMeta`).
- Not a finding: parent C-08, C-09 (`purpose.md`) and I-02, I-05, I-07,
  I-08 (`invariants.md`) have no value-mode notes. By convention, VT-I1 and
  VT-V3 state the exceptions; only parent decisions get value-mode notes.

## Ambiguity review, pass 2 (2026-11-10)

- no ambiguity review feedback
- Left to the plan, not design ambiguities: how `define-checks` selects the
  value-mode exclusions (see "Design steps done"), and the test files for
  VT-V11 and VT-V12. Acceptance criterion 4 (`Cell`) is covered by the
  VT-V12 `[^long x]` fixture with the VT-I7 field check (VT-V11 (e)).

## Inconsistency review, pass 2 (2026-11-10)

- no inconsistency review feedback
- README map for "Documentation" (the README has no single "Value-type
  records" section with these parts): Approach → `## Approach` (value-type
  paragraph); Requirements → `### Value-type records (optional)` under
  Installation (deps snippet, `value-type.fallback`); Differences → the list
  in `### Value-type records` under Usage; "Arrays and flattening" → its
  `####` subsection. The README already links `doc/value-type-records.md`.
- `artifacts.md` `test/value-type/` row also omits VT-V9 today; add it with
  VT-V11 and VT-V12.

## After design review pass 2 (2026-11-10)

- Pass 2 (architecture, ambiguity, inconsistency) added no design steps.
  `design-steps.md` has no unchecked items. The design is stable. Next
  step: plan (`create-task-plan`), not design-steps work.
- If a later pass adds steps, apply the rules in "Notes for the
  design-steps work" above. Do not raise again the items recorded as "not
  a finding" or "left to the plan" in the pass 2 notes.

## Plan ambiguity review (2026-11-10)

- ambiguity review added 4 new design steps
- Checked: the shared checks already have an in-source condition on
  `support/representation` (`checks/defclosedrecord.clj`,
  `serializable-test`). The ordinary suite calls `define-checks` with
  `{:record-opts [] :representation :identity}`, the value-type suite with
  `{:record-opts [:value-type true] …}`. Thus `:record-opts` can tell the
  modes apart with no new key.
- For the inconsistency turn: the plan risk "run the Temurin 21 value-type
  suite after each slice" against the Slice 2 emitter change, which breaks
  the metadata checks until Slice 3 (raised here as an ambiguity step).

## Plan inconsistency review (2026-11-10)

- inconsistency review added 2 new design steps
- Not raised again: the "after each slice" risk against Slice 2, and the
  exclusion mechanism that the plan defers (plan ambiguity review steps 1
  and 2). Not a finding: steps.md adds VT-V9 to the `artifacts.md` row,
  though the design names only VT-V11 and VT-V12 (see the inconsistency
  review pass 2 note).

## Notes for the plan design-steps work (2026-11-10)

- The 6 plan-review steps change `plan.md` and `steps.md`, not
  `design.md` or Ramora design pages. Exception: the embodiment step edits
  only `steps.md` now; Slice 5 edits `embodiment.md`. Keep the "Notes for
  the design-steps work" rules (ASD-STE100 prose, Ramora authority).
- Order: decide the exclusion mechanism (ambiguity step 1) first. Then the
  exclusions can move before the emitter change (step 2), and Slices 2
  and 3 can each end green.
- Mechanism hook: `transform` in
  `test/runtime/closed_record/checks/support.clj` swaps the symbol
  `closed-record.checks.support/representation` for the caller value. A
  new placeholder works the same way and can read `:record-opts` (already
  in the `transform` arguments). Also add a `def` for it in `support.clj`,
  so clj-kondo lints the check sources.
- Parts to guard in the check source: `gen-step` (`:with-meta`) and
  `judge :meta` in `checks/differential.clj`; `check-test` literal part and
  `serializable-test` `meta` assertion in `checks/defclosedrecord.clj`.
- VT-V2 factories: `positional-factory` and `map-factory` in
  `src/closed_record/impl/jvm.clj` are top-level `defn` forms in the
  expansion, outside the `defvaluetype*` form that
  `closed-record.value-type.support/type-form` returns.
- Stale-state paths: `munera/plan.md` item 1; `mementum/state.md` "Current
  state" (the task 022 item).

## Plan design-steps done (2026-11-10)

- All 6 plan-review steps are done; `plan.md` and `steps.md` changed.
  `design.md` and Ramora design pages did not change.
- Slices changed: Slice 2 is now the value-mode exclusions with no
  emitter change; Slice 3 is the emitter and the checks that need the
  closed shape. All `artifacts.md` edits are in Slice 5.
- Checked: `eval` of `(when false …)` still expands the macros in its
  body. Thus a `when` guard cannot stop the expansion-time read of the
  `constant` macro in `checks/literal.clj`; the plan uses a private macro
  there.
- scry fails a test with no assertion (comment at `serializable-test` in
  `checks/defclosedrecord.clj`). Thus whole-test guards are top-level
  `when` forms, also for VT-V12.
- `hidden_key.clj` is excluded by removal of its value-type caller, not by
  guards. No other file names `closed-record.value-type.hidden-key-test`.
- `munera/plan.md` item 1 and `mementum/state.md` now say "next: plan
  review, then implementation". Slice 5 sets the final state.

## Plan ambiguity review, pass 2 (2026-11-10)

- ambiguity review added 1 new design step
- Checked: the other guard sites of Slice 2 (`check-test`,
  `serializable-test`, `gen-step`, `judge :meta`, `metadata-test`,
  `extra-key-literal-print-test`) read at run time, so a `when` works.
  No other shared check or value-type test uses metadata or an
  undeclared-key literal. The factories (`positional-factory`,
  `map-factory`) have no hidden-field reference today. Fixture
  normalisation numbers gensyms by first appearance, so moving `meta` and
  `withMeta` into `representation` does not change the fixture.
- For implementation: `field-names` in `representation_test.clj` uses
  `getDeclaredFields`, which also gives static fields (a `deftype` class
  has `const__N` static fields for keyword constants). The VT-I7 checks
  (VT-V4, VT-V11 (e), VT-V12) compare instance fields: remove static
  fields (`java.lang.reflect.Modifier/isStatic`) before the comparison.

## Plan inconsistency review, pass 2 (2026-11-10)

- no inconsistency review feedback
- Checked: no file outside Munera names the value-type
  `hidden_key_test.clj`. The stale Ramora rows (`artifacts.md`
  `test/value-type/`, embodiment Tests and "Differences") are already
  covered by Slice 5. `munera/plan.md` and `mementum/state.md` agree with the
  task files. The open `value-constant-test` step (ambiguity pass 2) is
  not raised again.

## Notes for the plan design-steps work, pass 2 (2026-11-10)

- One open step: the `value-constant-test` form and the private macro
  shape (design-steps "Plan ambiguity review, pass 2"). It changes only
  `plan.md` (the "Literals read at macroexpansion time" item) and
  `steps.md` Slice 2 (the `checks/literal.clj` item). Not `design.md` or
  Ramora: VT-V3 already excludes the metadata part.
- Keep: the ordinary test does not change (the placeholder is `true`);
  value mode keeps assertions in each defined test (scry); a guard that
  must stop an expansion-time read is a macro, not `when`.
- The macro can take the flag as an argument
  (`support/metadata-and-hidden-keys?`) or name it in its body: `transform`
  replaces the symbol in all forms, the `defmacro` form included. The
  macro is defined in the calling namespace, as each form is.
- Paths: `test/runtime/closed_record/checks/literal.clj` (`constant`,
  `value-constant-test`, `extra-key-constant-test`);
  `test/runtime/closed_record/checks/support.clj` (`transform`);
  the mode-only test pattern in
  `mementum/knowledge/testing-generated-types.md`.

## Plan design-steps done, pass 2 (2026-11-10)

- The one open step is done: `plan.md` ("Literals read at macroexpansion
  time", and an exception note in "Whole test") and `steps.md` Slice 2
  (`checks/literal.clj` item). `design.md` and Ramora did not change.
- Decision: option (a), the two-branch macro `if-metadata-and-hidden-keys`
  (`[then & [else]]`). `extra-key-constant-test` uses it at top level
  with no `else`.
- Checked (plain `clojure -M -e`): `eval` of a top-level call of a macro
  that returns nil, with a `deftest` holding a `constant` read that is not
  valid, defines no test and does not read the literal. The branch that is
  not chosen is not read.
- `checks/literal.clj` has no `support` alias today; Slice 2 adds it.

## Plan ambiguity review, pass 3 (2026-11-10)

- ambiguity review added 1 new design step (VT-V11 (c) constructor call)
- For the inconsistency turn: steps.md Slice 1 checks "no `edc3f79`
  outside Munera, mementum and Git history", but
  `ramora/value-type-records/embodiment.md` ("Differences from the
  design", line 10) names `edc3f79` until Slice 5 removes the section.
- Implementation hints, not findings:
  - `gen-step` (`checks/differential.clj`): `(conj … (when flag …))` adds
    nil to the `gen/one-of` vector, and `gen/one-of` rejects it. Use
    `cond->`. A guard of `judge :meta` inside the `or` can be `when` (nil
    is a pass).
  - `serializable-test`: `with-meta` is in the `let` binding, so a `when`
    cannot guard it; bind `r` itself in value mode, then guard the `meta`
    assertion.
  - VT-V12: define the fixture records at top level, outside the `when`
    that holds the `deftest`. With a `defclosedrecord` in the same `when`
    form, `->L` resolves (`declare`), but the short class name `L` (the
    `into-array` argument) does not: the `import` runs after the compiler
    compiles the `deftest` body (checked, plain `clojure -M -e`).

## Plan inconsistency review, pass 3 (2026-11-10)

- inconsistency review added 1 new design step (Slice 1 `edc3f79` check
  against the embodiment pin sentence)
- Checked, no finding: the VT-V3 exclusion list in Slice 2 matches Ramora
  VT-V3 (metadata; extra-key parts of V-01, V-04, V-05, V-15; V-14); the
  export and VT-V12 land in one slice; `munera/plan.md` and
  `mementum/state.md` agree with the task files.

## Notes for the plan design-steps work, pass 3 (2026-11-10)

- Two open steps (design-steps "Plan ambiguity review, pass 3" and "Plan
  inconsistency review, pass 3"). Both change only `plan.md` and
  `steps.md`. Exception: the recommended option of the `edc3f79` step adds
  an edit of `ramora/value-type-records/embodiment.md` to Slice 1 (a
  steps.md item now; the edit itself happens in Slice 1). Not `design.md`;
  not Ramora design pages (VT-V11 stays as it is). Keep the earlier rules:
  ASD-STE100 prose, Ramora authority, embodiment states only the current
  state.
- VT-V11 (c) facts (checked on an ordinary closed record `[x y]`):
  constructor parameter types are `[Object Object]`,
  `[Object Object Object Object]` and
  `[Object Object Object Object int int]` (`__meta`, `__extmap` are
  `Object`). `clojure.lang.Reflector/invokeConstructor` unwraps
  `InvocationTargetException`, but throws `IllegalArgumentException` itself
  for an arity with no constructor. Thus the Clojure reflector is no
  better than `Constructor/newInstance` for a clean signal; the test needs
  an exact constructor (`getConstructor` with the parameter types) and a
  check that the exception comes from the constructor body.
- `edc3f79` step paths: `ramora/value-type-records/embodiment.md` lines
  10–11 (the pin sentence; the six "Until task 022" entries stay true until
  Slice 3/4/5); steps.md Slice 1 (`git grep edc3f79` item) and Slice 5
  (embodiment item, which names the `edc3f79` pin sentence and must then
  match the new wording).

## Plan design-steps done, pass 3 (2026-11-10)

- Both open steps are done: `plan.md` (VT-V11 part (c) form; Slice 1
  exception in the documentation item; "Slice order" item 1) and
  `steps.md` (Slice 1 embodiment item; Slice 3 (c); Slice 5 pin-sentence
  wording). `design.md` and Ramora design pages did not change.
- Decisions: VT-V11 (c) calls both the main and the `__meta __extmap`
  constructor through `getConstructor` + `newInstance` and expects
  `InvocationTargetException` with a direct `IllegalArgumentException`
  cause; nil for both gives an instance. Slice 1 changes the embodiment
  pin sentence to `4ed8d0a3` (recommended option); the `git grep
  edc3f79` check stays in Slice 1.
- Checked (value-type `4ed8d0a3`, Java 25 without preview, so identity
  fallback; a hand-written closed `defvaluetype*` `[a b …]` with
  `IRecord`): the three constructors exist with the descriptors above;
  both constructors give `InvocationTargetException` → `IllegalArgumentException`
  for `{:m 1}`, `{:x 1}` and `{}`; nil for both gives an instance;
  `newInstance` with a wrong argument count gives a top-level
  `IllegalArgumentException` with no cause; `create` with an undeclared
  key gives `IllegalArgumentException` with no wrapper. Value output keeps
  the descriptors (value-type `doc/usage.md`, closed shape paragraph); not
  run on EA3 in this pass.
- value-type source for contract checks: the gitlib checkout
  `~/.gitlibs/libs/org.hugoduncan/value-type/4ed8d0a3c4ce8ed6a74607da312fd33a8c80dea2`
  (`doc/usage.md`, "`deftype*` forms").

## Plan ambiguity review, pass 4 (2026-11-10)

- no ambiguity review feedback
- Checked, no finding: the option position (the user options are only
  `:no-print` and `:load-ns`); the one Valhalla test command in `ci.yml`
  and `doc/development.md`; the Slice 2 guard sites against the check
  sources (`serializable-test` already sits in a top-level
  `(when (= :identity support/representation) …)`); `transform` already
  gets `:record-opts`.
- Implementation hint, not a finding: VT-V11 (e) on identity can read
  `__meta` and `__extmap` with
  `clojure.lang.Reflector/getInstanceField`, as `field` does in
  `test/runtime/closed_record/compiler_rules_test.clj`. On value output,
  the source must not need these fields at compile time.

## Plan inconsistency review, pass 4 (2026-11-10)

- inconsistency review added 1 new design step (Slice 5 embodiment rows
  `closed-record.core` and `doc/` and `README.md`)
- Checked, no finding: the other embodiment parts that Slice 5 names
  ("Status", "Differences from the design", "Generated type in value
  mode", "Verification limits") and the `deps.edn` row (it says only "a
  pinned Git SHA"). The `artifacts.md` rows that Slice 5 names exist
  (`test/value-type/`, `checks/*.clj`, `checks/support.clj`); the
  `deps.edn` and `ci.yml` rows of `artifacts.md` need no change (the
  `:enable-preview` alias already has the export).
- Not raised: Slices 1 and 2 do not run the Valhalla suite, but the plan
  says "each slice ends with all suites green". Slice 3 runs it, and
  Slice 2 only removes checks.

## Notes for the plan design-steps work, pass 4 (2026-11-10)

- One open step (design-steps "Plan inconsistency review, pass 4"). It
  changes only steps.md, the Slice 5 item for
  `ramora/value-type-records/embodiment.md`: add the
  `closed-record.core` row and the `doc/` and `README.md` row to the
  "Elements that change" sub-item. The embodiment itself changes in
  Slice 5, not now. Not `plan.md` (its Slice 5 line names only "Ramora
  embodiments"), not `design.md`, not Ramora design pages.
- Keep: ASD-STE100 prose; the embodiment states the current state only;
  the row text names what exists after Slice 5, as the other sub-items do.
- Current row text (embodiment table): `closed-record.core` says "The
  docstring of `defclosedrecord` describes the option. It also has one
  sentence on `:non-nil` with `:value-type true`." The `doc/` and
  `README.md` row says "The option, the differences table, the runtime
  dependency, the hash cost and non-nil fields."
- Docstring source: `src/closed_record/core.cljc` (`defclosedrecord`;
  the value-type text starts near line 57, the `:non-nil` sentence near
  line 70).

## Plan design-steps work, pass 4 (2026-11-10)

- Applied the one open step: steps.md Slice 5, "Elements that change"
  sub-item, now names the `closed-record.core` row and the `doc/` and
  `README.md` row. `plan.md`, `design.md` and Ramora did not change.
- The `doc/` and `README.md` row text also names the user-method limit
  (the review list did not). The Slice 5 README and
  `doc/value-type-records.md` items add that limit, so the row must name
  it to state the current state.
- All design-steps items are now ticked.

## Plan ambiguity review, pass 5 (2026-11-10)

- no ambiguity review feedback
- Checked, no finding: the VT-V11 parts (a) to (e) and VT-V12 against
  Ramora VT-V11 and VT-V12; no other value-type test or shared check uses
  metadata or an undeclared-key literal (`malli`, `field_value`,
  `property`, `rejected_key_message`, `errors_test`, `non_nil_test`
  line 183 are `map->NN` or declared-key paths).
- Implementation hints, not findings:
  - `checks/differential.clj` and `checks/equality.clj` have no
    `closed-record.checks.support :as support` require today (only
    `defclosedrecord.clj` has it). Slice 2 adds it there too, as for
    `literal.clj`; else `transform` does not match the symbol.
  - `base-interfaces` has `IObj` third. If the representation description
    gives the interface list, keep the ordinary order (the expansion
    fixture compares the printed form).

## Plan inconsistency review, pass 5 (2026-11-10)

- no inconsistency review feedback
- Not raised: from Slice 3 to Slice 5 the "Differences from the design"
  entries of `ramora/value-type-records/embodiment.md` are stale. The plan
  chooses this (embodiment edits after code and tests; Slice 1 changes
  only the pin sentence), and Slice 5 removes the section.

## After plan review pass 5 (2026-11-10)

- Pass 5 (ambiguity, inconsistency) added no design steps; `design-steps.md`
  has no unchecked items. No design-steps work is due. Next: implementation,
  Slice 1. Then change "Next" in `munera/plan.md` (item 1) and
  `mementum/state.md` ("Current state", task 022 item) from plan review to
  implementation.
- For a later review pass: do not raise again the items recorded as "not a
  finding", "not raised" or "implementation hint" in the plan review notes
  (passes 1 to 5).

## Slice 1: pin (2026-11-10)

- Pinned value-type `4ed8d0a3c4ce8ed6a74607da312fd33a8c80dea2` in
  `deps.edn`, README and `doc/value-type-records.md`. The closed contract
  link (`#deftype-forms`) is in the "Representation" section of
  `doc/value-type-records.md`.
- Verification ran on a local JDK 25 (no Temurin 21 here), identity
  representation: value-type suite 89 tests pass; Clojure 1.11 case
  passes. `bb vale` and `bb lychee:check` pass. CI covers Temurin 21.
- Next: Slice 2. Local command:
  `clojure -J-Dclosed-record.test.representation=identity -M:test:value-type-test:clj-1.12 -m scry.cli --dir test/value-type`.

## Slice 2: value-mode exclusions (2026-11-10)

- `support/metadata-and-hidden-keys?`: `transform` replaces it through a
  `case` on the resolved symbol, next to `support/representation`. The
  value comes from `(:value-type (apply hash-map record-opts))`.
- `gen-step` in `differential.clj` adds the `:with-meta` generator with
  `cond->`; `judge :meta` is a `when` in the `or` chain (nil passes).
- `serializable-test`: the round trip uses `r`; the metadata round trip is a
  guarded extra assertion.
- Results (JDK 25, identity): value-type suite 76 tests pass (was 89: V-14
  caller removed, guarded tests not defined); ordinary 187 tests, same
  before and after; Clojure 1.11 case passes; clj-kondo and cljfmt clean.
- Next: Slice 3 (emitter).

## Slice 3: emitter and closed-shape checks (2026-11-10)

- `representation` has new keys: `:interfaces`, `:type-options`
  (`[:value-type/closed true]` in value mode, after the user options),
  `:meta-methods` (fn of `this m tname fields`; `[]` in value mode),
  `:assoc-hidden-args` and `:extra-keys-body`. `:with-meta-form` is gone.
  `generated-methods` builds its vector with `concat` round the meta
  methods. Gensym order is unchanged; ordinary suite 187 pass, the
  expansion fixture is unchanged.
- VT-V2 tests: `type-options` reads the keyword pairs after the field
  vector; `factory-forms` finds the `->Q`/`map->Q` defns (count 2 asserted).
- VT-V11 `closed_shape_test.clj`: (e) identity reads `__meta`/`__extmap`
  by reflection on the public final fields.
- Results (JDK 25, identity): value-type 81 tests pass; Clojure 1.11 case
  passes; lint and format clean. Valhalla not run locally: first value-output
  run of the closed shape is the CI Valhalla job (check it after Slice 4).
- Next: Slice 4 (VT-V12, export in ci.yml and doc/development.md).

## Slice 4: flattening test and CI (2026-11-10)

- `flattening_test.clj`: fixtures `L [^long x]`, `II [^int x ^int y]`;
  instance fields exclude static fields (`Modifier/isStatic`).
- Export added to the Valhalla command in `ci.yml` and `doc/development.md`
  as `-J--add-exports=...`.
- Local (JDK 25, identity): 81 tests pass; VT-V12 not defined there. The
  Valhalla run (VT-V11 value branch, VT-V12) is open: CI only. If it fails,
  fix before Slice 6.
- Next: Slice 5 (documentation).

## Slice 5: documentation (2026-11-10)

- README, `doc/value-type-records.md`, docstring (+ `doc/API.md`),
  CHANGELOG, both Ramora embodiments, `mementum/state.md`,
  `munera/plan.md` updated. The value-type embodiment has no
  "Differences from the design" section now; no difference stays.
- The flattening text (README `Cell` example, doc page) states the
  EA3 results of the design table; the Valhalla CI run is still the
  first check of them for closed-record records. If VT-V12 fails there,
  change these texts with VT-K8.
- Checks: vale, lychee, api-docs --check, cljfmt; ordinary 187 and
  value-type (identity) 81 tests pass.
- Next: Slice 6 (CI jobs incl. Valhalla, Clojure 1.11/1.13 suites).

## Slice 6: final verification (2026-11-10)

- Local pass: runtime suites on Clojure 1.11, 1.12, 1.13; value-type
  (identity, JDK 25); Clojure 1.11 case; clj-fmt, clj-kondo, vale,
  api-docs --check, lychee. Expansion fixture not changed by task 022.
- No local Valhalla JDK. VT-V11 value branch and VT-V12 need the CI
  Valhalla job.
- The local branch and `origin/020-value-type-records` have diverged
  (ahead 63, behind 128; merge base `ac259424`). Origin has 128 commits
  of task 020 follow-up work (for example `7534b9cd`) that are not in
  the local branch. A push needs a merge, a rebase or a force push.

## Branch reconciled (2026-11-10)

- The blocker is resolved. The user force pushed the local branch, so
  `origin/020-value-type-records` is at `5e84d9f0`, the same as the local
  branch. CI can run on the task commits (pull request #1).
- Correction to the Slice 6 note: a local Valhalla JDK exists at
  `~/.local/jdks/jdk-27.jdk/Contents/Home/bin/java` (`27-jep401ea3+1-1`;
  use `JAVA_CMD`). The value-output part of VT-V11 and VT-V12 can run
  locally before CI.
- Open: the full value-type suite on Valhalla (local or CI), then all CI
  jobs green on the last commit that changes files outside the task
  directory.

## Local Valhalla run (2026-11-10)

- First run on local Valhalla (`27-jep401ea3+1-1`): VT-I7 field checks
  in `representation_test.clj` and `closed_shape_test.clj` failed. They
  counted the static `const__N` fields of the class. Fix: count only
  non-static fields (as `flattening_test.clj` does).
- After the fix: Valhalla value output 80 tests pass (VT-V11 value
  branch and VT-V12 included; acceptance criterion 4 holds locally);
  identity (`:clj-1.12`, as CI) 81 tests pass.
- CI run 42841466683 on `d0f340c9`: all 9 jobs pass (the earlier links failure on `5e84d9f` was GitHub 503s). Task complete.

## Implementation review (2026-11-10)

- added 2 steps to be addressed

## Review follow-up (2026-11-10)

- addressed 2 review steps: `support/instance-field-names` replaces the
  three copies; `generated-methods` layout tidied (fixture unchanged).
  Ordinary (187), identity (81) and Valhalla value (80) suites pass.

## Implementation review, pass 2 (2026-11-10)

- added 1 step to be addressed

## Review follow-up, pass 2 (2026-11-10)

- addressed 1 review step: `munera/plan.md` and `mementum/state.md` state
  that task 022 is complete and ready to close.

## Implementation review, pass 3 (2026-11-10)

- added 1 step to be addressed

## Review follow-up, pass 3 (2026-11-10)

- addressed 1 review step: pushed `7a469a46` (fast-forward); CI run
  42848080888 on `7a469a46`: all 9 jobs pass. Acceptance criterion 6
  holds.

## Implementation review, pass 4 (2026-11-10)

- no new issues

## Test review (2026-11-10)

- no new issues

## Test review, pass 2 (2026-11-10)

- added 1 step to be addressed

## Review follow-up, test review pass 2 (2026-11-10)

- addressed 1 review step: the `construct` docstring in
  `closed_shape_test.clj` names `params`, `m` and `ext`.

## Test review, pass 3 (2026-11-10)

- added 1 step to be addressed

## Review follow-up, test review pass 3 (2026-11-10)

- addressed 1 review step: VT-V12 gives the `array-info` map as the `is`
  message. Value-type suite passes on local Valhalla (value) and on
  identity.

## Test review, pass 4 (2026-11-10)

- no new issues

## Docs review (2026-11-10)

- added 2 steps to be addressed

## Review follow-up, docs review (2026-11-10)

- addressed 2 review steps: `doc/value-type-records.md` Requirements names
  value-type `4ed8d0a3`; Behaviour states the `:not-installed` full-expansion
  limit; one CHANGELOG line. Vale and lychee pass.

## Docs review, pass 2 (2026-11-10)

- added 1 step to be addressed

## Review follow-up, docs review pass 2 (2026-11-10)

- addressed 1 review step: the `doc/` and `README.md` row of the ramora
  embodiment names the value-type `4ed8d0a3` requirement and the
  full-expansion limit.

## Docs review, pass 3 (2026-11-10)

- no new issues

## Code-shaper review (2026-11-10)

- added 3 steps to be addressed

## Review follow-up, code-shaper review (2026-11-10)

- addressed 3 review steps: `transform` local renamed to
  `metadata-and-hidden-keys?`; `value-constant-test` selects only the
  literal and guards only the `meta` assertion; `cause-class` removed,
  `constructor-test` asserts `InvocationTargetException` and an
  `IllegalArgumentException` cause. Ordinary suite, value-type suite
  (identity, Valhalla value), format and lint pass.

## Code-shaper review, pass 2 (2026-11-10)

- no new issues
