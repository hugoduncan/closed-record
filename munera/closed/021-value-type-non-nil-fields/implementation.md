# Implementation

## Design review

- 2026-11-04 architecture review: `ramora/META.md` and `doc/architecture.md`
  do not exist; used `AGENTS.md`, `ramora/INDEX.md` (document rules),
  `ramora/defclosedrecord` (principles, D-03, D-11, D-19, D-20) and
  `ramora/value-type-records`. VT-D12 item is actionable because its
  rationale contradicts D-20, which also decides its commitment. VT-D11
  fits D-03 scope (host exceptions for value casts) and D-19.
- architectural review added 2 new design steps
- 2026-11-04 ambiguity review: the emitter puts field metadata only in the
  `deftype*`/`defvaluetype*` field vector; factories and methods use
  `plain-fields` (metadata removed). Thus VT-I6 part (a) is checkable. The
  expansion fixture compares printed strings with `*print-meta*` true.
- ambiguity review added 2 new design steps
- 2026-11-04 inconsistency review: the current VT-V6 test
  (`test/value-type/closed_record/value_type/errors_test.clj`,
  `non-nil-field-test`) expects `:non-nil-unsupported`; it fails with the new
  pin until VT-V6 changes. Only Ramora (not README or `doc/`) states that
  `:non-nil` is rejected.
- inconsistency review added 1 new design step
- For the design-steps slice:
  - Edit design.md in place; no amendment notes (Munera evolution rule).
    Keep the scope sections unchanged.
  - Write in ASD-STE100 prose. Proposed Ramora records follow the shapes in
    `ramora/value-type-records/decisions.md` (8-tuple) and
    `ramora/value-type-records/invariants-and-verification.md` (assertion
    record). Commitment levels: `ramora/defclosedrecord/decisions.md`.
  - D-20 text: `ramora/defclosedrecord/decisions/platform-and-evolution.md`;
    D-03, D-11: `.../failure-and-validation.md`; D-19:
    `.../closed-key-set.md`. Ramora page rules: `ramora/INDEX.md`.
  - VT-I6 and VT-V10 depend on
    `test/runtime/closed_record/expansion_fixture.clj` (`expansion`,
    `normalise`) and the field-metadata path in
    `src/closed_record/impl/jvm.clj` (`plain-fields`, field vector).

## Design-steps slice (2026-11-04)

- All 5 design steps done; no `SCOPE_QUESTION:` items.
- VT-V10 fixture form: `record!` rewrites the whole
  `expansion_fixture.edn` from `forms`; after adding the `:non-nil` form,
  check by diff that the six existing entries are unchanged. No `src/` or
  `test/` commit since `f034647`, so the current emitter is the `f034647`
  emitter until this task changes it.
- VT-I6 needs the unprinted expanded form for part (a); the fixture
  `expansion` returns only the printed string. Reuse `normalise` and the
  position stripping (private `strip-positions`), or expose a helper.
- Compiled constants are not in the failing-path list; value-type owns
  their load (ownership list). VT-V9 has no compiled-constant check.

## Design review, pass 2 (2026-11-04)

- architecture review (pass 2): no architectural review feedback. Checked
  the revised design against D-03 scope (value casts keep host exceptions),
  D-11, D-19, D-20, principles 9 and 15, and I-03 (an `assoc` that throws
  on nil does not break it, as for `^long` fields today).
- ambiguity review (pass 2) added 1 new design step
- inconsistency review (pass 2) added 1 new design step

## For the pass-2 design steps (2026-11-04)

- Two open steps only; prior notes on edit rules and Ramora shapes apply.
- "Eligible tag" step: take the wording from design.md Context,
  "Enforcement" bullet (value-class tag ∧ native-field JVM → native;
  else generated checks). Check that Intent, the README part and the
  "non-nil enforcement" row (README differences table,
  `README.md` "Value-type records"; Ramora table in
  `ramora/value-type-records.md` "Public contract") then say the same.
  On identity fallback, enforcement is always generated checks.
- VT-I6 step: VT-V2 is the "Expansion" row of the table in
  `ramora/value-type-records/invariants-and-verification.md`; it runs in
  the Temurin 21 job (value-type suite). If VT-I6 goes there, also make
  acceptance criterion 4 and the VT-R1 signal list (Documentation) agree;
  VT-V2 is already a VT-R1 signal.

## Pass-2 design-steps slice (2026-11-04)

- Both steps done; no `SCOPE_QUESTION:` items.
- VT-I6 check now lives in VT-V2 (design.md Verification). Both value-type
  CI jobs run all of `test/value-type` (`ci.yml`), so VT-V2 runs on
  Temurin 21 and Valhalla. The Ramora sentence "VT-V2 ... run in Temurin 21
  jobs" (`invariants-and-verification.md`, after VT-D10) is incomplete; fix
  it when VT-V2 changes.
- Current VT-V2 table text has no VT-I6 part; extend the row, keep its id.

## Design review, pass 3 (2026-11-04)

- architecture review (pass 3): no architectural review feedback. The
  pass-2 edits (Intent enforcement wording, VT-I6 `verified-by: VT-V2`)
  fit the Ramora record shapes. I-06 to I-08 stay true: a literal that
  omits a non-nil key fails before an instance exists.
- ambiguity review (pass 3): no ambiguity review feedback. `conj`,
  `merge` and `into` are not in the public contract path list, but the
  emitted `cons` calls `assoc` (`src/closed_record/impl/jvm.clj`), so the
  `assoc` path covers them.
- inconsistency review (pass 3) added 1 new design step. Checked: no
  `8bb7c4f` reference outside `deps.edn`, README and `doc/performance.md`;
  the README and Ramora differences tables have the columns Ordinary,
  Value output and Identity fallback, so the non-nil enforcement row
  needs one cell for each.

## For the pass-3 design step (2026-11-04)

- One open step: a wording fix in design.md Problem, second bullet. Change
  only that bullet. Do not add a Documentation item: no user doc has a
  `:non-nil` claim to remove (`git grep -i non-nil -- . ':!munera'`).
- Rejection claims to name: `ramora/value-type-records.md` (Context fact
  "`:non-nil` field metadata is rejected"; Public contract, value-type
  errors pass-through sentence),
  `ramora/value-type-records/invariants-and-verification.md` (VT-V6 row),
  `test/value-type/closed_record/value_type/errors_test.clj`
  (`non-nil-field-test`). The Documentation section already changes all
  of them.

## Pass-3 design-steps slice (2026-11-04)

- Step done; no `SCOPE_QUESTION:` items. Only the Problem second bullet
  changed. Documentation already covers each named artifact: Ramora context
  fact and public contract ("update the public contract"), VT-V6 row and
  test (Verification, VT-V6).
- Ramora rejection claims: `value-type-records.md` line ~69 (Context,
  "Interacting systems") and ~113 (Public contract example); VT-V6 row in
  `invariants-and-verification.md` line ~78.

## Design review, pass 4 (2026-11-04)

- architecture review (pass 4): no architectural review feedback. Only the
  Problem bullet changed since pass 3; it names artifacts and does not
  change the contract, decisions or ownership split.
- ambiguity review (pass 4): no ambiguity review feedback. The new Problem
  bullet names each artifact by path and section.
- inconsistency review (pass 4): no inconsistency review feedback.
  `git grep -i non-nil` outside `munera/` and `mementum/` finds exactly the
  four places that the Problem bullet names. The current VT-V6 test form
  (`^:non-nil a`, no tag) is the new VT-V6 example; with the new pin only
  the expected reason changes (`:missing-tag`).

## After design review, pass 4 (2026-11-04)

- No open design steps: pass 4 added none in any of the three reviews.
  design.md is stable; the next step is plan creation, not a design-steps
  slice.
- For the plan: the earlier "For the ..." notes in this file keep the
  file paths (emitter `src/closed_record/impl/jvm.clj`, expansion fixture
  `test/runtime/closed_record/expansion_fixture.clj`, Ramora pages, CI
  `ci.yml`). Change the pin first: with the `8bb7c4f` pin VT-V9 cannot pass,
  and with the new pin the current VT-V6 test fails until its reason changes.

## Plan review (2026-11-04)

- ambiguity review: the existing VT-V2 test is `value-expansion-test` in
  `test/value-type/closed_record/value_type/representation_test.clj`;
  extend it or add the VT-I6 check beside it. The value-type CI command
  uses the `:test` alias, so `closed-record.expansion-fixture` (in
  `test/runtime`) is on the value-type class path; `strip-positions` is
  private. Remote link fragments are not checked (`dev/link_check.clj`
  uses `--include-fragments=none` online), so a link to value-type
  `doc/usage.md#portable-non-nil-fields` passes the link check.
- ambiguity review added 3 new design steps
- inconsistency review: both value-type jobs run all of `test/value-type`,
  so the job placement sentence is incomplete for VT-V3, VT-V5 and VT-V6 too,
  not only VT-V2. CHANGELOG `## [Unreleased]` has one value-type entry
  to extend.
- inconsistency review added 2 new design steps

## For the plan-review design steps (2026-11-04)

- Five open steps. Four change only plan.md and steps.md. The fifth (stale
  Ramora id lists) also adds to design.md Documentation. Do not change the
  design contract. Edit in place, ASD-STE100, no amendment notes.
- Run commands: `doc/development.md` lines ~22-46. Local JVMs:
  Valhalla `/Users/duncan/.local/share/mise/installs/java/openjdk-27.0.0-jep401ea3+1-1/bin/java`
  (as `JAVA_CMD`); Temurin 21 `.../java/temurin-21`; default Java 25
  (identity).
- Push pattern to copy: `munera/closed/020-value-type-records/steps.md`
  lines ~208-220 (`git push`, `gh run list/watch/view`, record run URLs).
  The task 020 approval covered branch `020-value-type-records` only. If the
  step cannot decide whether that approval still applies, add a "stop and
  ask the user before the first push" step. Do not assume the approval.
- Field storage cells: ordinary `deftype` stores a primitive tag as the
  primitive and any other tag as `Object` (probe: `c` is `Object`). Tables:
  `README.md` ~179 and `ramora/value-type-records.md` ~121.
- Ramora targets: public contract bullets `ramora/value-type-records.md`
  ~97-118; index ranges ~15-17; job placement sentence
  `ramora/value-type-records/invariants-and-verification.md` ~110 (make it
  complete for VT-V2, VT-V3, VT-V5, VT-V6, VT-V9 and VT-V10).

## Plan-review design-steps slice (2026-11-04)

- All 5 plan-review steps done in plan.md and steps.md. design.md not
  changed (read-only in this pass): the stale Ramora id lists are in steps
  slice 5 only; a design pass can add them to design.md Documentation.
- Push: steps slice 7 stops to ask the user before the first push; the
  task 020 approval is not assumed. PR #1 is already open for the branch.
- VT-V5 runs only on identity output (`subprocess_test.clj`, `when (=
  :identity expected)`); VT-V2 (`representation_test.clj`) and VT-V6
  (`errors_test.clj`) run unconditionally in both value-type jobs. The
  current Ramora placement sentence says "Temurin 21 jobs", which also
  covers the runtime matrix (Temurin 21).
- `ramora/value-type-records/decisions.md` has a VT-D10 stub that points to
  `invariants-and-verification.md`; thus the index range "VT-D1 to VT-D12"
  is correct after VT-D11 and VT-D12 go into `decisions.md`.
- Quality bb tasks: `bb.edn` (`clj-fmt:check`, `clj-kondo:lint`, `vale`,
  `api-docs`, `lychee:check`).

## Plan review, pass 2 (2026-11-04)

- ambiguity review (pass 2): `ordinary-expansion-test`
  (`test/runtime/closed_record/value_type_option_test.clj`) checks that
  the fixture keys equal `forms` keys, and expands each form with no
  option and with `:value-type false`; the new `:non-nil` form gets both
  checks. Embodiment item is actionable: the Tests row id range and test
  placements go stale, and no step names the VT-V9 and VT-V10 files. The
  README has no value-type doc link today; the only pinned value-type
  link is in `doc/performance.md`.
- ambiguity review (pass 2) added 2 new design steps
- inconsistency review (pass 2): Ramora placement bullets in steps slice 5
  agree with the VT-V3/VT-V4/VT-V7 rows and the tests (`subprocess_test.clj`
  VT-V1 runs in both jobs; VT-V5 identity only). The task closure and
  knowledge extraction also commit outside the task directory (`munera/plan.md`,
  `mementum/`) after the slice-7 run; the link job covers those `*.md`
  files, so run `bb lychee:check` before such commits.
- inconsistency review (pass 2) added 2 new design steps

## For the plan-review pass-2 design steps (2026-11-04)

- Four open steps (two ambiguity, two inconsistency). All change only
  plan.md and steps.md; do not change the design contract. Edit in place,
  ASD-STE100, no amendment notes.
- Fixture order: still no `src/` or `test/` commit since `f034647`, so a
  move of the fixture form and `record!` into slice 1 is the simplest fix;
  then slice 1 also runs T-RT (already listed). `ordinary-expansion-test`
  in `test/runtime/closed_record/value_type_option_test.clj` reads all
  `forms` keys, so the form and the `.edn` entry go in one commit.
- Test file names: follow `test/value-type/closed_record/value_type/*_test.clj`
  (ns `closed-record.value-type.<name>-test`). VT-V10 can extend
  `value_type_option_test.clj` (the ordinary VT-V1 placement) or be a new
  `test/runtime` file; name the choice so the embodiment Tests row can
  cite it.
- Embodiment: `ramora/value-type-records/embodiment.md`, "Elements that
  change" table (Tests, `closed-record.core`, `closed-record.impl.jvm`,
  `doc/` and `README.md` rows). Keep the table shape.
- README link: value-type heading "Portable non-nil fields" →
  `#portable-non-nil-fields`; use the full pinned SHA, as the
  `doc/performance.md` link does.
- Link check: CI job `Markdown links` (`.github/workflows/ci.yml`) runs
  `bb lychee:check`; scope and options in `dev/link_check.clj` and
  `lychee.toml`.

## Plan-review pass-2 design-steps slice (2026-11-04)

- All 4 steps done in plan.md and steps.md; design.md not changed.
- Fixture form moved to the first step of slice 1 (key `:non-nil`, type
  `NonNil`). `git log f034647..HEAD -- src test` is empty at this commit.
  `record!` writes entries sorted by key, so the new entry is an insert;
  the `.edn` diff must show added lines only.
- Test files chosen: VT-V9 `non_nil_test.clj` (new, value-type suite);
  VT-I6 in `representation_test.clj`; VT-V10 in
  `value_type_option_test.clj`. The embodiment Tests row today lists
  "VT-V1 to VT-V7"; VT-V8 is in its `dev/bench` row.
- Pinned README link target checked: HTTP 200, and `doc/usage.md` at
  `edc3f79` has heading "Portable non-nil fields" (line 308).
- Link check: `lychee.toml` `include_verbatim = false`, so URLs in code
  spans in task files are not checked; bare URLs are checked online.

## Plan review, pass 3 (2026-11-04)

- ambiguity review (pass 3): quoted forms keep symbol metadata in a
  compiled test namespace (checked: `^String ^:non-nil s` in a quoted
  `def` and in a `defn` body keeps `{:non-nil true :tag String}`), so the
  VT-I6 test can use a quoted form, as `expansion_fixture.clj` does. The
  `errors_test.clj` "text" comment does not apply. Literal checks with
  nil or a wrong type must read the literal at run time (`read-string`,
  `eval`): the reader calls the constructor, so such a literal in the
  test source fails the load of the namespace.
- ambiguity review (pass 3) added 1 new design step
- inconsistency review (pass 3): no inconsistency review feedback. The
  ns docstrings of `representation_test.clj` ("VT-V2 ... VT-I4 check on
  Temurin 21") and `value_type_option_test.clj` ("VT-V1 in the ordinary
  suite") name only the current ids; update them when slices 3 and 4 add
  VT-I6 and VT-V10. The design.md Documentation gap for the Ramora index
  and job placement edits is the known pass-1 item (design-steps.md, Plan
  review: inconsistency); not filed again.

## For the plan-review pass-3 design step (2026-11-04)

- One open step: the missing Ramora record fields. Put the values in steps
  slice 5 (and plan.md if needed). Do not change the design contract or
  the decisions' choices; only fill fields. Prior edit rules apply.
- Shapes and examples:
  - Decision 8-tuple: `ramora/value-type-records/decisions.md` lines 1-6.
    Scope wording in use: "public API, all hosts" (VT-D1), "all hosts"
    (VT-D2), "JVM emitter" (VT-D5). An empty field is omitted only when
    it does not apply (`ramora/defclosedrecord/decisions.md`, which also
    defines low/medium/high).
  - Assertion record: id, subject, predicate, object, scope, basis,
    enforced-by, verified-by, confidence
    (`ramora/value-type-records/invariants-and-verification.md`, VT-I3 is
    the nearest model: an expansion property, confidence high).
  - Risk record: "Likelihood / Effect / Signal" (same file, Risks; VT-R1
    effect today "VT-D3 and VT-D5 change", signal "VT-V2").

## Plan-review pass-3 design-steps slice (2026-11-04)

- Step done in steps slice 5 and plan.md; design.md not changed.
- Ramora record field values now fixed in steps slice 5: VT-D11 (scope
  public API, JVM; commitment high), VT-D12 (scope public API, all hosts;
  no assumptions field), VT-I6 full text record, VT-R1 effect adds VT-D11
  and VT-I6. VT-D11 commitment is high, unlike VT-D3 (medium): it is
  public error behaviour, not an emitter mechanism.
- VT-D9 limits `:value-type true` to the JVM; thus VT-D11 scope "JVM".

## Plan review, pass 4 (2026-11-04)

- ambiguity review (pass 4): no ambiguity review feedback. Checked the
  pass-3 edits (steps slice 5 record fields) and the slices against the
  fixture code, test helpers, `deps.edn`, `bb.edn` and the Ramora tables.
- For slice 1: `git diff --quiet f034647 -- src test` holds at `2128e8ba`.
  `record!` command (task 020 plan.md line ~53):
  `clojure -M:test -e "(require 'closed-record.expansion-fixture) (closed-record.expansion-fixture/record!)"`.
  `bb api-docs` (no `--check`) regenerates `doc/API.md` in slice 6.
- inconsistency review (pass 4): no inconsistency review feedback.
  Checked design.md (contract, decisions, Verification, CI,
  Documentation, acceptance) against plan.md and steps slices 1-7, and
  the Ramora index and risk ids. Not filed: design.md VT-V2 says
  `macroexpand-1` "in the fixture namespace"; steps slice 3 omits it. The
  comparison holds in any one namespace, but bind `*ns*` to
  `closed-record.expansion-fixture` (as `expansion` does) to match the
  design text; `value-expansion` binds the test namespace.

## After plan review, pass 4 (2026-11-04)

- No open design steps: pass 4 added none (0 unchecked items in
  design-steps.md). plan.md and steps.md are stable; the next step is
  implementation from steps slice 1, not a design-steps slice.
- Keep: the slice-1 fixture form comes first (the `f034647` baseline holds
  only while `src/` and `test/` have no change); stop before the first push.

## Slice 1 (2026-11-04)

- `git diff --quiet f034647 -- src` held before the fixture change; the
  `:non-nil` fixture entry is recorded from the `f034647` emitter. `git
  diff` of `expansion_fixture.edn`: two added lines only.
- Pin `edc3f79` in `deps.edn` and README. VT-V6 now expects
  `:missing-tag`. No emitter change.
- Results: T-ID 82 tests, T-VAL 80, T-11 1, T-RT 186 on 1.11/1.12/1.13;
  all pass.

## Slice 2 (2026-11-04)

- VT-V9 in `test/value-type/closed_record/value_type/non_nil_test.clj`.
  Fixtures `Inner` (value record) and `NN [^Inner ^:non-nil i ^String
  ^:non-nil s u]`. No emitter change.
- Literals with nil or wrong types are read at run time (`read-string` in
  the test ns). Extra check beyond design: a map literal that omits a
  non-nil key also gives `NullPointerException`.
- Key-set precedence types: `:closed-record/undeclared-key`,
  `:closed-record/declared-key-dissoc`, `:closed-record/extra-keys`.
- Results: T-ID 88 tests / 417 assertions, T-VAL 86 / 418; all pass.

## Slice 3 (2026-11-04)

- VT-I6 check `non-nil-expansion-test` in `representation_test.clj`.
  `strip-positions` in `expansion_fixture.clj` is now public (reused);
  `*ns*` bound to `closed-record.expansion-fixture`. Tag of `p` is the
  fully qualified `VP` class name. Part (a) also counts `:non-nil` in the
  printed expansion (exactly 2). No emitter change.
- Results: T-ID 89 / 421, T-VAL 87 / 422, T-RT 186 on 1.11/1.12/1.13; pass.

## Slice 4 (2026-11-04)

- VT-V10 `non-nil-ordinary-test` in `value_type_option_test.clj`; type
  `NonNilField [^String ^:non-nil s]`. Nil accepted on `->Name`,
  `map->Name`, `assoc`. Extra check: field storage type is `Object`
  (supports the "Field storage type" Ordinary cell). ns docstring names
  VT-V10.
- Results: T-RT 187 tests on 1.11/1.12/1.13; pass.

## Slice 5 (2026-11-04)

- Ramora done. VT-D11 and VT-D12 go after the VT-D10 stub in
  `decisions.md` (numeric order). VT-I6 after VT-I5. VT-V9 and VT-V10 rows
  go before VT-V8 in the table (VT-V8 stays last: informational).
- Job placement sentence is now a bullet list (all VT-V ids placed).
- The `c366f15` defect is in the VT-V9 row as technical evidence only.
- No emitter change; the embodiment `impl.jvm` row states this.
- `bb vale` and `bb lychee:check` pass.

## Slice 6 (2026-11-04)

- README: "Non-nil fields" is a `####` part after the differences table
  (inside "Value-type records"); the two new table rows use the plan.md
  cells. Docstring: one sentence after the differences sentence.
  CHANGELOG: a second bullet under `[Unreleased]`.
- `bb api-docs` regenerated `doc/API.md`; `api-docs --check`, `vale`,
  `clj-fmt:check`, `lychee:check` pass. Test suites not rerun (docstring
  only); slice 7 runs them.

## Slice 7 (2026-11-04)

- `mementum/state.md` names task 021 state (VT-D11, VT-D12, pin `edc3f79`).
- Results: T-ID 89 / 421, T-VAL 87 / 422, T-11 1 / 7, T-RT 187 / 2798 on
  1.11, 1.12, 1.13. All quality checks pass (fmt, kondo, vale, api-docs,
  link_check_test, lychee).
- Remaining: push (needs user approval, plan.md CI and push), read CI run,
  record it.

<!-- IMPLEMENTATION_BLOCKER: START -->
- blocker: The plan requires user approval before the first push of branch `020-value-type-records`, and acceptance criterion 7 needs CI results for the pushed commit.
- required-human-action: Approve `git push origin 020-value-type-records` (or push it yourself). Then the next pass reads the CI run and records the result.
<!-- IMPLEMENTATION_BLOCKER: END -->

## Implementation review (2026-11-04)

- T-ID rerun: 89 tests / 421 assertions pass. Design contract, Ramora,
  README, docstring and CHANGELOG agree; no emitter change.
- added 2 steps to be addressed

## Review follow-up (2026-11-04)

- addressed 2 review steps: `non_nil_test.clj` uses
  `closed-record.probe-support` (`thrown`, `data-of`);
  `non-nil-expansion-test` derives the `:non-nil` count from the field
  symbols. T-ID 89 / 421, T-VAL 87 / 422; fmt and kondo clean.
- Push steps stay blocked (user approval, see blocker above).

## Implementation review, pass 2 (2026-11-04)

- added 1 step to be addressed

## Review follow-up, pass 2 (2026-11-04)

- addressed 1 review step: the `representation_test.clj` ns docstring
  names the VT-I6 check of VT-V2 (`non-nil-expansion-test`). T-ID 89 /
  421; fmt clean.
- Push steps stay blocked (user approval, see blocker above).

## Implementation review, pass 3 (2026-11-04)

- no new issues; T-VAL 87 / 422 pass

## Test review (2026-11-04)

- added 1 step to be addressed

## Test review follow-up (2026-11-04)

- addressed 1 review step: `invalid-value-test` checks `(data-of f)` is
  nil on the direct paths (`->NN`, `map->NN` nil value and missing key,
  `assoc`); literal paths not checked (reader wraps). T-ID 89 / 435,
  T-VAL 87 / 436; fmt and kondo clean.
- Push steps stay blocked (user approval, see blocker above).

## Test review, pass 2 (2026-11-04)

- no new issues; T-VAL 87 / 436 pass

## Test review, pass 3 (2026-11-04)

- added 1 step to be addressed

## Test review, pass 3 follow-up (2026-11-04)

- addressed 1 review step: `non_nil_test.clj` gives `u` the value `1` in
  `valid`, the base records and the positional literal; only
  `untagged-nil-test` checks nil in `u`. T-ID 89 / 435, T-VAL 87 / 436.
- Push steps stay blocked (user approval, see blocker above).

## Test review, pass 4 (2026-11-04)

- added 1 step to be addressed

## Test review, pass 4 follow-up (2026-11-04)

- addressed 1 review step: `key-set-precedence-test` labels each case
  (`assoc :z`; per field: `dissoc`, `map->NN with an extra key`).
  T-ID 89 / 435, T-VAL 87 / 436; fmt and kondo clean.
- Push steps stay blocked (user approval, see blocker above).

## Test review, pass 5 (2026-11-04)

- no new issues; T-ID 89 / 435 pass

## Docs review (2026-11-04)

- added 1 step to be addressed

## Docs review follow-up (2026-11-04)

- addressed 1 review step in part: `README.md` ("Non-nil fields") and
  `ramora/value-type-records.md` (public contract) now state no
  `ex-data` only on `->Name`, `map->Name` and `assoc`; on record literals
  the reader can wrap the host exception (cause chain). `bb vale` and
  `bb lychee:check` clean.
- Blocker: design.md (Public contract, line ~185, "They have no
  `ex-data`") still claims all paths. This pass had design.md as
  read-only context. Step stays unchecked until a pass may edit design.md.
- Push steps stay blocked (user approval, see blocker above).

## Docs review, pass 2 (2026-11-04)

- no new issues

## Code-shaper review (2026-11-04)

- added 2 steps to be addressed

## Code-shaper review follow-up (2026-11-04)

- addressed 2 review steps: `paths` entries carry `:direct?`;
  `check-host-exception` calls `f` once; `direct-paths` removed.
  `plain-form` is `(remove-non-nil non-nil-form)`.
  T-ID 89 / 435, T-VAL 87 / 436; fmt and kondo clean.
- Docs review follow-up step stays open: design.md still read-only
  context in this pass (blocker above).
- Push steps stay blocked (user approval, see blocker above).

## Code-shaper review, pass 2 (2026-11-04)

- added 1 step to be addressed

## Code-shaper review, pass 2 follow-up (2026-11-04)

- addressed 1 review step: one private `base` from `valid`
  (`field-keys`, `positional-values`); no other copy of the field values.
  T-ID 89 / 435, T-VAL 87 / 436; fmt and kondo clean.
- Docs review follow-up step stays open: design.md still read-only
  context in this pass (blocker above).
- Push steps stay blocked (user approval, see blocker above).

## Code-shaper review, pass 3 (2026-11-04)

- added 2 steps to be addressed

## Code-shaper review, pass 3 follow-up (2026-11-04)

- addressed 2 review steps: one private `non-nil-keys` (`(keys bad)`)
  in both `doseq` forms; `check-fields` and the `assoc` cases read
  `valid`; `r` alias removed. T-ID 89 / 435, T-VAL 87 / 436; fmt and
  kondo clean.
- Docs review follow-up step stays open: design.md still read-only
  context in this pass (blocker above).
- Push steps stay blocked (user approval, see blocker above).

## Code-shaper review, pass 4 (2026-11-04)

- added 1 step to be addressed

## Code-shaper review, pass 4 follow-up (2026-11-04)

- addressed 1 review step: private `declared-non-nil-fields` from
  `non-nil-form`; the field check and the count check use it. T-ID
  89 / 435, T-VAL 87 / 436; fmt and kondo clean.
- Docs review follow-up step stays open: design.md still read-only
  context in this pass (blocker above).
- Push steps stay blocked (user approval, see blocker above).

## Code-shaper review, pass 5 (2026-11-04)

- added 2 steps to be addressed

## Code-shaper review, pass 5 follow-up (2026-11-04)

- addressed 2 review steps: private `missing-key-paths` with the `paths`
  shape; `paths` docstring corrected; `causes` in
  `closed-record.probe-support`, used in `non_nil_test.clj` and
  `errors_test.clj`. T-ID 89 / 435, T-VAL 87 / 436, T-RT 187 / 2798 on
  1.11/1.12/1.13; fmt and kondo clean.
- Docs review follow-up step stays open: design.md still read-only
  context in this pass (blocker above).
- Push steps stay blocked (user approval, see blocker above).

## Code-shaper review, pass 6 (2026-11-04)

- added 1 step to be addressed

## Code-shaper review, pass 6 follow-up (2026-11-04)

- addressed 1 review step: `paths` docstring and `probe-support` ns
  docstring rewrapped to 80 columns. Docstring-only change; fmt and kondo
  clean.
- Docs review follow-up step stays open: design.md still read-only
  context in this pass (blocker above).
- Push steps stay blocked (user approval, see blocker above).

## Code-shaper review, pass 7 (2026-11-04)

- added 2 steps to be addressed

## Code-shaper review, pass 7 follow-up (2026-11-04)

- addressed 2 review steps: `expand`, `printed` and `strip-meta` in
  `closed-record.expansion-fixture`; `expansion` is `printed` of `expand`;
  `strip-positions` and `remove-non-nil` use `strip-meta`. T-ID 89 / 435,
  T-VAL 87 / 436, T-RT 187 / 2798 on 1.11/1.12/1.13; fmt and kondo clean.
- Docs review follow-up step stays open: design.md still read-only
  context in this pass (blocker above).
- Push steps stay blocked (user approval, see blocker above).

## Code-shaper review, pass 8 (2026-11-04)

- no new issues

## Close (2026-11-04)

- design.md now states the `ex-data` rule per path (Public contract,
  VT-D11 qualities, Documentation), as README and Ramora do.
- Closed at user request with the slice 7 push and CI steps not done.
  The branch `020-value-type-records` is not pushed, so acceptance
  criterion 7 (all CI jobs pass on the last commit) is not verified. The
  push needs user approval; read the CI run of the pushed commit then.
