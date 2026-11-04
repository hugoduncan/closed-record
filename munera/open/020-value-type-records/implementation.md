# Implementation

## Design review (2026-11-03)

- architectural review added 2 new design steps
- Context: `ramora/META.md` and `doc/architecture.md` do not exist. The
  architecture sources are `AGENTS.md`, `ramora/INDEX.md`, the defclosedrecord
  pages (principles, embodiment, artifact map) and
  `ramora/development-quality.md`.
- Plan note, not a design finding: lint setup resolves each alias in
  `closed-record.lint-dependencies/dependency-bases` with `-Srepro`. The new
  value-type alias must go in that list, so lint setup (CI quality job and
  pre-commit) fetches the pinned value-type Git SHA.
- Plan note: `default-child-cmd` in `dev/bench/closed_record/bench/fork.clj`
  copies `java.home` and the JVM input arguments to each fork, so a VT-V8 run
  on Valhalla keeps `--enable-preview` in the child JVMs.
- `dev/bench/valhalla/` (task 019) also exists. "The existing bench
  structure" can mean the criterium forks or that directory; left to the
  ambiguity turn.
- ambiguity review added 4 new design steps
- "value-type AOT rules" (VT-V7) resolve to value-type
  `ramora/defvaluetype-design.md` D7 (cited from
  `ramora/deftype-star-design.md`): AOT fixes the representation. Not filed.
- inconsistency review added 1 new design step

## Notes for the design-steps pass

Principles:
- Resolve each step by editing design.md in place. Do not change VT-D1 to
  VT-D10 or the scope. Ramora pages get no progress history.
- VT-I2 comes first: the ordinary expansion, the default `:bench` run and the
  `:test` alias stay the same and do not load value-type.
- A representation expectation in the tests is test data, not a library
  switch (VT-D1). Detection of the JVM capability alone cannot catch silent
  fallback.

Paths:
- Gensym sites (VT-I2 comparison step): `src/closed_record/impl/jvm.clj`
  parameter gensyms `p__` and `this__`, the auto-gensyms of `cached-hash`,
  and `m#` in `withMeta`. Existing expansion test helpers (`in-test-ns`,
  `thrown`): `test/runtime/closed_record/defclosedrecord_test.clj` near
  line 489, `literal_test.clj` line 82.
- Subprocess JVM patterns (VT-V5, VT-V1 Clojure 1.11 case, VT-V7):
  `dev/bench/closed_record/bench/fork.clj`, `test/build/build_test.clj`
  (isolated consumer JVM).
- Bench: `dev/bench/closed_record/bench.clj` (S-03 exit code),
  `bench/report.clj`, `dev/bench/valhalla/*.java`, `test/bench/` (no CI job).
  The `dev/bench/` row in `ramora/defclosedrecord/embodiment/artifacts.md`
  already omits `memory/`, `timing/` and `valhalla/`.
- CI runtime matrix: `.github/workflows/ci.yml`, job "Runtime tests", with
  `matrix` entries that each have a `command`.
- value-type sources: `~/projects/hugoduncan/value-type/value-type-master/ramora/`
  `deftype-star-design.md` and `defvaluetype-design.md`.

## Design-steps pass (2026-11-03)

- All 7 design steps resolved in design.md. No `SCOPE_QUESTION:` items.
- `src/` at the task start equals `ac25942` (last `src/` commit `3a37315` is
  an ancestor). The fixture can be recorded from the working tree before the
  first emitter change.
- Observed: the normalised expansion (`*print-meta*` true, regex
  `\b([A-Za-z-]+__)(\d+)(__auto__)?\b` → first-appearance index) of
  `(defclosedrecord P [a ^long b] :load-ns true Object (toString [_] "x"))`
  is the same string on Clojure 1.11.4, 1.12.6 and 1.13.0-alpha9. The type
  hash literal in `hasheq` depends on the class name, so fix the namespace.
- value-type reads `value-type.fallback` once, in the `policy` def of
  `value-type.impl.runtime` (at load); later changes are unsupported. This is
  why VT-V5 needs a subprocess.
- Bench facts for VT-V8:
  - `dev/bench/timing/run.sh` runs `clojure … -M:bench:corrected-timing "$@"`.
    `"$@"` comes after `-M`, so extra aliases and `-J` options cannot come
    through the arguments now.
  - `fork/default-child-cmd` fixes `-m closed-record.bench`. `fork/parent`
    prints the six-op S-03 table and exit code through `report/*`. The VT-V8
    entry needs its own child command and aggregation; reuse
    `report/per-call-stats` and `fork/in-order`.
  - The child calls `timing-tool/observe`, which rejects an unknown
    Criterium collector SHA. VT-V8 children need the corrected overlay of
    `run.sh` on the class path.
  - Many `test/runtime` tests (`bench_*_test`, `matrix_budget_test`,
    `assoc_timing_result_test`) load `dev/bench` namespaces. None may require
    the VT-V8 entry namespace.
  - `dev/bench/cases.edn` `:valhalla` is the task-019 probe configuration
    (Clojure 1.11.4). It is not the VT-V8 configuration.

## Design review, second round (2026-11-03)

- architectural review added 1 new design step
- The defclosedrecord design pages already name value mode
  (`decisions/type-structure.md`). Only the embodiment page lags. Format and
  lint scope already covers all of `test/` and `dev/`
  (`dev/quality/closed_record/quality_scope.clj`), so new test directories
  need no scope change.
- ambiguity review (round 2) added 1 new design step
- inconsistency review (round 2) added 1 new design step
- `dev/bench/timing/run.sh` clears the `:bench` main opts (`:corrected-timing`
  alias) and passes `"$@"` as main args, so `run.sh -m <vt-v8-entry-ns>` can
  select the VT-V8 parent entry. Only extra aliases and `-J` options need the
  new way of the Verification section. value-type `66d5ff7` is still not on
  `origin` (local `master` ahead 119); the Prerequisite stands.

## Notes for the round-2 design-steps pass

- Three open steps: the Public boundary of the defclosedrecord embodiment,
  the `:class-name` value, and the Intent wording. The earlier principles
  apply: edit design.md in place, keep VT-D1 to VT-D10 and the scope, and
  add no progress history to Ramora.
- The `:class-name` choice is public API. Record it in design.md and in the
  Public contract of `ramora/value-type-records.md`. This adds precision to
  the contract and does not change a decision. Make the value agree with the
  current `ex-data`: the `map->Name` error uses `:record` with the class
  symbol (`src/closed_record/impl/jvm.clj`, `map-factory`). D-03 is in
  `ramora/defclosedrecord/decisions/failure-and-validation.md`.
- Public boundary step: add an item to the Documentation list for
  `ramora/defclosedrecord/embodiment.md`. It covers the Boundaries "Public"
  row and the "Public interface" section ("Error data: see D-03"). A pointer
  to the value-type public contract is enough. Do not add a new D-03 `:type`,
  because D-03 is about key-set failures.
- Intent step: use the wording of the Summary in
  `ramora/value-type-records.md`.

## Design-steps pass, round 2 (2026-11-03)

- All 3 design steps resolved in design.md. No `SCOPE_QUESTION:` items.
- `:class-name` is the `classname` symbol of `emit`. It carries the metadata
  of `tname` (`with-meta … (meta tname)`); metadata does not affect `=`, so
  VT-V1 can compare with a plain symbol. Checked: `(namespace-munge
  'my-app.geo)` is `my_app.geo`.
- The Ramora edits that the steps ask for (`:class-name` value in the
  `ramora/value-type-records.md` Public contract, the defclosedrecord Public
  row and Public interface section) are items in the Documentation list.
  The implementation makes them; Ramora is not edited in the design pass.

## Design review, third round (2026-11-03)

- no architectural review feedback
- Also checked `ramora/release-workflow/` (every CI job gates a release;
  VT-D10 already accepts the early-access download dependency) and
  `ramora/development-quality.md`. No design change needed. Scry `--dir`
  per command (as `:test:build-test --dir test/build`) fits the separate
  value-type test directories.
- For the later turns, not architecture: `doc/development.md` says "Clojure
  jobs use Temurin Java 21". The Valhalla job makes this false; the
  Documentation item for that file names only the commands.
- ambiguity review (round 3) added 1 new design step
- Checked, not filed: a quoted form in a test file gives no `:line`
  metadata in the printed expansion (`*print-meta*` true), so the VT-V1
  fixture does not depend on test source positions.
- inconsistency review (round 3) added 1 new design step

## Notes for the round-3 design-steps pass

- Two open steps: the cause of `:closed-record/value-type-unavailable` when
  the var is absent, and the Temurin statements in `doc/`. The earlier
  principles apply: edit design.md in place, keep VT-D1 to VT-D10 and the
  scope, and add no progress history to Ramora.
- Cause step: the choice is public contract. Record it in the design.md
  Public contract, and extend the Documentation item for the
  `ramora/value-type-records.md` Public contract (as for `:class-name`).
  Observed on Clojure 1.11.4: with no value-type, `requiring-resolve`
  throws `java.io.FileNotFoundException` ("Could not locate
  value_type/core__init.class …"); an absent var gives `nil`. No suite can
  make the absent-var case without a stub `value-type.core`, which would
  conflict with the real library; do not require a test for it unless the
  design adds one on purpose.
- Doc step paths: `doc/development.md` line 53 (Continuous integration),
  `doc/tool-installation.md` line 11. Leave `doc/performance.md` line 12:
  it describes task-019 evidence (Valhalla on macOS arm64), not CI.

## Design-steps pass, round 3 (2026-11-03)

- Both design steps resolved in design.md. No `SCOPE_QUESTION:` items.
- Cause choice: absent var gives no cause (`ex-cause` `nil`). The design
  adds the absent-var check on purpose (ordinary suite, subprocess JVM, stub
  `value-type.core` in a temporary directory). Checked: with a stub that has
  only `(ns value-type.core)` on the class path, `requiring-resolve` of
  `value-type.core/defvaluetype*` gives `nil` on each call.

## Design review, fourth round (2026-11-03)

- architectural review added 1 new design step
- Checked, not filed: the round-3 absent-var subprocess in `test/runtime`
  fits; the ordinary suite already starts real child JVMs
  (`bench_fork_test`, `bench_assoc_allocation_run_test`), and the stub never
  shares a class path with value-type (VT-I2 holds).
- no ambiguity review feedback (round 4)
- Checked, not filed: `macroexpand-1` wraps expansion errors in
  `CompilerException`; the existing tests read `ex-cause`
  (`expansion-cause`, `defclosedrecord_test.clj`). So the
  `:closed-record/value-type-unavailable` `ex-info` is the cause of that
  wrapper, and its own `ex-cause` is the cause of the Public contract.
  value-type's fallback `Error` is a plain `java.lang.Error`
  (`value_type/impl/runtime.clj`). `:value-type nil` is an "other value"
  (`IllegalArgumentException`); use `contains?`, not truthiness.
- inconsistency review (round 4) added 1 new design step

## Notes for the round-4 design-steps pass

- Two open steps: the artifact-map rows (architecture) and the scry
  failure uploads (inconsistency). Earlier principles apply: edit design.md
  in place, keep VT-D1 to VT-D10 and the scope, no progress history in
  Ramora. Both resolve as edits to the Documentation list and the CI
  section; no new decision.
- Artifact map step: extend the existing
  `ramora/defclosedrecord/embodiment/artifacts.md` Documentation item. Rows:
  `src/closed_record/impl/jvm.clj` (now "Builds the `deftype*` form and the
  factories"), the `test/runtime/closed_record/*_test.clj` glob row, and new
  rows for the expansion fixture and, if in `test/runtime`, the shared
  VT-V3/VT-V4 checks. The `ci.yml` row ("three alias-based runtime test
  entries", "structured test-failure uploads") also changes; the existing
  item names it, so keep its wording consistent with the upload step.
- Upload step: pattern in `.github/workflows/ci.yml` is a step
  `if: failure()`, `actions/upload-artifact@v4.6.2`, `name: scry-…`,
  `path: .scry-results/` (lines ~137-142 runtime matrix). Names now used:
  `scry-runtime-<version>`, `scry-build`, `scry-release`, `scry-tooling`.
  One job with two scry steps (Temurin suite and Clojure 1.11 case) shares
  `.scry-results/`; a second run can overwrite the first. Give each step
  its own upload, or a distinct results directory if scry allows it.
- Doc targets: `doc/development.md` Continuous integration, lines ~49-79
  (artifact list at ~74, "three test commands above" at ~70).

## Design-steps pass, round 4 (2026-11-03)

- Both design steps resolved in design.md. No `SCOPE_QUESTION:` items.
- Choice: the Temurin 21 value-type suite is a separate job,
  `Value-type tests (Temurin 21)`, not a runtime-matrix entry. A matrix
  entry would need a per-entry artifact name and a Clojure 1.11 step that
  runs in one entry only. The runtime matrix and its sentence in
  `doc/development.md` stay as they are.
- scry 0.1.61 facts: the CLI always writes `<cwd>/.scry-results/` and
  clears it at the start of each run (`results/prepare-results-dir!`). It
  has no option for another directory. Thus each scry step needs its upload
  directly after it.
- The location of the VT-V3/VT-V4 shared check definitions is still a plan
  choice. They must load without value-type if ordinary tests use them.

## Design review, fifth round (2026-11-03)

- no architectural review feedback
- Checked, not filed: the separate `Value-type tests (Temurin 21)` job fits
  VT-D10 ("Temurin 21 jobs") and the release gate (`release.yml` calls all
  of `ci.yml` through `workflow_call`). The "No other closed-record namespace
  depends on value-type" boundary in `ramora/value-type-records/embodiment.md`
  is about library namespaces; test and `dev/bench` namespaces are outside it.
- Implementation hint: the Documentation item "replace Not implemented with
  the current embodiment" for `ramora/value-type-records/embodiment.md`
  covers the whole page. The Elements table rows `ci.yml` (names only the
  Valhalla job), Tests ("VT-V1 runs in the ordinary `:test` alias"; VT-V1
  now has three placements) and Verification limits ("Until the job
  exists") are then false and must change too.
- ambiguity review (round 5) added 1 new design step
- Context: the round-trip test is `serializable-test`
  (`defclosedrecord_test.clj` about line 314). It also checks `meta` and
  `hash` after the round trip. Ordinary tests that use reference
  `identical?` (`conj nil`, `dissoc` of an undeclared key, `check`) still
  hold on value output, because `identical?` then compares fields. They need
  no differences-table assertion.
- inconsistency review (round 5) added 1 new design step
- Checked, not filed: `CHANGELOG.md` `## [Unreleased]` is empty, and the
  design adds no entry for the option. The release contract
  (`ramora/release-workflow/contracts-and-flow.md`) needs nonempty
  Unreleased notes only at release time, and a release is out of scope.
  The release task must write the note for `:value-type`.

## Notes for the round-5 design-steps pass

- Two open steps: the shared-check selection with serialization on value
  output (ambiguity), and the `doc/development.md` suite list
  (inconsistency). Earlier principles apply: edit design.md in place, keep
  VT-D1 to VT-D10 and the scope, no progress history in Ramora.
- Selection step: edit Scope, Verification (the VT-V3/VT-V4 bullets). Keep
  the selection rule a rule, not a list of test names; the plan maps it to
  tests. Out of scope already excludes serialization of value output, so
  "skip on value output" needs no new decision. The differences table row
  ("not promised") is the authority. Consider adding the rule to the Ramora
  VT-V3 row through the Documentation item for
  `ramora/value-type-records/invariants-and-verification.md`, as for VT-V1.
- Ordinary tests outside V-rows: `test/runtime/closed_record/`
  `defclosedrecord_test.clj` (`serializable-test`, the section "Generated
  methods not in the V-01 tables", hidden-field check near line 495, which
  is emitter-level), `hidden_key_test.clj`, `differential_test.clj`
  (V-02), `property_test.clj` (V-03), `literal_test.clj` (V-04),
  `compiler_rules_test.clj` (V-13, excluded). V-rows:
  `ramora/defclosedrecord/qualities.md` (Verification plan).
- Suite-list step: extend the existing Documentation item for
  `doc/development.md`; add the `test/` path constraint to the Dependencies
  section (where the alias and the two directories are defined). Format and
  lint scope: `dev/quality/closed_record/quality_scope.clj`.

## Design-steps pass, round 5 (2026-11-03)

- Both design steps resolved in design.md. No `SCOPE_QUESTION:` items.
- Selection rule facts for the plan mapping:
  - Excluded by the rule: `field_value_test.clj` `field-declaration-test`
    (labelled V-11, reflects on class fields), `compiler_rules_test.clj`
    (V-13), `argument-errors-test`, `accepted-options-test` and
    `load-ns-option-test` (option checks), and the `bench_*`, `assoc_*` and
    `matrix_budget_test` files.
  - The hash-cache tests (`assoc-test` "hash caches 0", `metadata-test`
    "with-meta copies the hash caches") read only `hash` and `.hashCode`,
    not the fields. Their assertions hold in both representations.
- Path facts: `deps.edn` `:test` has `:extra-paths ["test/runtime"
  "dev/bench"]`. The value-type alias adds both new directories
  to `:extra-paths`, and each command selects its directory with `--dir`. scry discovery
  without `--dir` scans all of `test/`, as for `test/bench/`.

## Design review, sixth round (2026-11-03)

- no architectural review feedback
- Checked, not filed: the round-5 shared-check rule fits VT-D1 and VT-I2
  (definitions load without value-type; the value-type commands add
  `test/runtime` through `:test`). D-16 Scope
  (`ramora/defclosedrecord/decisions/verification.md`, "GitHub CI runs all
  three test aliases … with Temurin Java 21") stays true: it does not say
  "only", and VT-D10 owns the Valhalla job.
- no ambiguity review feedback (round 6)
- Checked, not filed: an invalid `-Dvalue-type.fallback` value makes the
  `policy` def of `value-type.impl.runtime` throw `IllegalArgumentException`
  at load. That is a load failure, so it gives
  `:closed-record/value-type-unavailable` with that cause. It does not pass
  through. No ordinary test uses `locking` or weak references
  (`grep test/runtime`), so the identity-property rule adds no `locking`
  assertion. VT-V4 lists only its own extra checks.
- no inconsistency review feedback (round 6)
- Checked, not filed: the value-type suite runs in both jobs. Thus VT-V2,
  VT-V6 and the VT-V1 subprocess check also run in the Valhalla job. The
  VT-D10 placement paragraph ("run in Temurin 21 jobs") does not say "only",
  so it stays true. `doc/development.md` line 53 also names Clojure CLI
  `1.12.6.1673`. Keep that clause when the Temurin statement is replaced.

## Notes after round 6

- No open design steps. All three round-6 reviews found nothing, so
  design.md is stable. The next step is `create-task-plan`, not another
  design-steps pass.
- The plan must make the choices that design.md leaves to it: the emitter
  mode shape, the alias and directory names, where the shared VT-V3/VT-V4
  checks live (they must load without value-type), the fixture forms and
  file format, and how `run.sh` gets extra aliases and `-J` options.
  Earlier notes have the facts for each (round-1, 4 and 5 notes).
- Plan-only items that are not in design.md: add the value-type alias to
  `dependency-bases` in `dev/quality/closed_record/lint_dependencies.clj`.
  Keep the Clojure CLI clause at `doc/development.md` line 53.

## Plan review (2026-11-03)

- ambiguity review added 5 new design steps
- Context: scry 0.1.61 discovers namespaces with the default pattern
  `.*-test$`. Shared check and helper namespaces in `test/runtime` are not
  run as tests if their names do not end in `-test`.
- Shared-check step is the main one: ordinary tests bind to top-level types
  by name (reader literals, `.field` interop), so a function-only extraction
  loses checks. A macro over the record options keeps them.
- inconsistency review added 1 new design step
- Checked, not filed: plan.md copies the design list of identity
  properties (hash cache, `locking`, reference `identical?`). The round-5
  and round-6 notes show that no shared check needs a branch for these;
  D-06 serialization is the only branch, and steps.md slice 5 states it.

## Notes for the plan-review design-steps pass

- Six open steps (5 ambiguity, 1 inconsistency). All are plan choices:
  resolve them in plan.md and steps.md. design.md is stable; change it only
  if a choice contradicts it. Keep VT-D1 to VT-D10, VT-I2 first, and no
  `:local/root` in a committed alias.
- Shared-check step facts:
  - Reader literal checks build the literal as a string with the class name
    (`read-literal "#closed_record.defclosedrecord_test.H{…}"`,
    `defclosedrecord_test.clj` ~line 219), and `literal_test.clj` has a
    `constant` macro (line 29) for code constants. A shared macro must
    derive these class names from the calling namespace
    (`namespace-munge *ns*`), not hard-code them.
  - Existing helpers to reuse or move: `in-test-ns`
    (`defclosedrecord_test.clj` line 61, `literal_test.clj` line 13),
    `eval-in-new-ns` (line 403), `expansion-cause` (line 488).
- Subprocess helper: `fork/default-child-cmd`
  (`dev/bench/closed_record/bench/fork.clj` line 19) is the pattern (java
  from `java.home`, input arguments, `java.class.path`) but fixes
  `-m closed-record.bench`. The test helper needs a free main and extra
  class path entries and `-D` properties.
- run.sh step: keep `run.sh -m <ns>` and the no-argument default working;
  the JAR lookup (`-Spath -M:bench`) must find exactly one Criterium JAR
  with the extra aliases too.
- Slice-3 step: for a local value-mode check before the alias exists, the
  value-type source is `~/projects/hugoduncan/value-type/value-type-master`
  at `66d5ff7` (through `-Sdeps` only, not committed).
- VT-V6 step: reachable value-type errors are `:invalid-field` from
  `^:non-nil` / `^:nil-restricted` fields (both JVMs) and
  `:removed-field-access` from a user method that names `__hash` (value
  output only, so Valhalla expectation only).

## Plan-review design-steps pass (2026-11-03)

- All 6 plan steps resolved in plan.md and steps.md. design.md unchanged.
- value-type error facts (`src/value_type/core.clj`, `defvaluetype*`
  docstring, ~line 370): the key is `:value-type/error`. `^:non-nil` gives
  `:reason :non-nil-unsupported`, `^:nil-restricted` gives
  `:reason :removed-nil-restricted`; both are checked at macroexpansion
  before the policy, so both JVMs. `:removed-field-access` comes when the
  capture loader defines the class (evaluation), value output only.
- value-type: lib coordinate `org.hugoduncan/value-type` (`build.clj`). The
  local repository HEAD is `66d5ff7` with a clean tree (checked now; check
  again before the slice-3 `:local/root` run).
- `test/bench/closed_record/timing_launcher_test.clj` calls
  `run.sh -e …` (arguments as main arguments). The environment-variable
  choice keeps it valid; run `test/bench` after the `run.sh` change.
- `.clj-kondo/config.edn` has only
  `:lint-as {closed-record.core/defclosedrecord clojure.core/defrecord}`.
- The `:test` alias has no `:main-opts`, so `clojure -M:test -e …` (the
  fixture record command) reaches `clojure.main`.

## Plan review, second round (2026-11-03)

- ambiguity review added 1 new design step
- Context for the VT-V8 step: `report/ops` (six S-03 texts),
  `report/fork-count` 5, `report/fork-stats`, `report/table` and
  `report/exit-code` serve the default run only. `fork/parent` takes a
  `child-cmd` but always prints the S-03 table and returns the S-03 exit
  code. The step is actionable because `legacy-transport/decode` rejects a
  result that is not six `[op p r]` entries, so `fork/parent` cannot take
  a two-op VT-V8 result as it is.
- Checked, not filed: `macroexpand-1` of `defclosedrecord` gives the
  `jvm/emit` output directly (`core.cljc`), so the fixture captures the
  emitter. A user method can name `__hash` (no closed-record check), so
  the VT-V6 `:removed-field-access` case is reachable. `run.sh` must put
  `BENCH_ALIASES` before `:corrected-timing`, which clears the main opts.
- inconsistency review added 2 new design steps
- Both steps carry earlier implementation.md notes (run `test/bench` after
  the `run.sh` change; keep the Clojure CLI clause) into steps.md, the
  active surface. All existing CI jobs pin `cli: '1.12.6.1673'`.

## Notes for the round-2 plan-review design-steps pass

- Three open steps (VT-V8 parent and report, `test/bench` launcher test,
  Clojure CLI pin). All are plan choices: resolve them in plan.md and
  steps.md. design.md stays unchanged. Keep VT-I2: the default `:bench`
  run, its output, exit code and the tests of `fork`, `report` and
  `legacy-transport` do not change.
- VT-V8 precedent: the `assoc-timing` family in
  `dev/bench/closed_record/bench/` (`assoc_timing_cli.clj`, `_run.clj`,
  `_command.clj`, `_child.clj`, `_report.clj`) is a non-S-03 forked
  Criterium measurement with its own command, child, result and report. It
  reuses `fork/in-order` and `fork/capture-process!` and validates
  `timing-tool` observations, with no change to `fork/parent`. Prefer this
  pattern (smaller, in the VT-V8 entry namespace) over making
  `fork/parent` generic.
- `test/bench` has no alias and no documented command. Earlier tasks ran it
  with an ad hoc `-Sdeps` alias that adds `test/bench` to the class path
  (`munera/closed/019-space-time-performance/features.md`). Find the
  aliases it needs (scry from `:test`, Criterium from `:bench`) before the
  plan states the command. The launcher test inherits the environment, so
  run it with `BENCH_ALIASES` and `BENCH_JVM_OPTS` unset.
- CLI pin: `.github/workflows/ci.yml` lines 27, 133, 161
  (`DeLaGuardo/setup-clojure@13.4`, `cli: '1.12.6.1673'`). The Clojure CLI
  script uses `JAVA_CMD` for dependency resolution and for the program, so
  the Valhalla job can work without Temurin. State the choice.

## Plan-review design-steps pass, round 2 (2026-11-03)

- All 3 plan steps resolved in plan.md and steps.md. design.md unchanged.
  No step of steps.md is done; this pass changes the plan only.
- VT-V8 choice: own parent loop, child result and table in
  `closed-record.bench.value-type`; no change to any other `dev/bench`
  namespace. The S-03 parts that do not fit: `report/header` (fixed, has
  "Result"), private `report/mean-interval` (t quantile for 5 forks) and
  `report/stats-error`. Thus VT-V8 shows the min–max range of fork means,
  not a t interval. `report/ratio` takes any two `{:mean …}` maps.
- `closed-record.bench/bench-op` is a public macro; its syntax-quote
  resolves to `criterium.bench/bench` and `report/per-call-stats`, so
  requiring `closed-record.bench` loads what it needs. That direction of
  require is allowed (only the reverse is forbidden).
- `test/bench` launcher test: `--dir test/bench` does not put the
  directory on the class path (scry error "Could not locate
  closed_record/timing_launcher_test…"); the `-Sdeps` `:bench-test` alias
  does. Do not use `-M:bench` with scry: its `:main-opts` run
  `closed-record.bench` first. Run left only ignored `target/timing-run.*`
  directories.
- Clojure CLI 1.12.6.1673 script (`libexec/bin/clojure`): `JAVA_CMD`
  defaults to `type -p java` and is used for `make-classpath2` (resolution)
  and the program JVM. CI pins: `.github/workflows/ci.yml` lines 21-27,
  127-133, 155-161, 185-191.

## Plan review, third round (2026-11-03)

- ambiguity review (plan round 3) added 3 new design steps
- Facts for the steps: value-type `origin` is
  `git@github.com:hugoduncan/value-type.git`; the HTTPS URL is public, its
  HEAD is `5d2656b`; `66d5ff7` is still not on GitHub. value-type has only
  `.clj` sources and no `:deps/prep-lib`, so a Git dependency needs no
  prep step. Local JDKs: `~/.local/share/mise/installs/java/temurin-21.0.10+7.0.LTS`
  and `.../openjdk-27.0.0-jep401ea3+1-1` (macOS aarch64, build
  `27-jep401ea3+1-1`); `~/.local/jdks/jdk-27.jdk` is the same build.
- Checked, not filed: VT-V4 `.isValue` on Temurin 21 (method absent) can
  use the reflective rule that plan.md gives for the VT-V8 child. In the
  Clojure 1.11 case, the top-level Clojure 1.11.4 wins over the Clojure
  1.12.6 of value-type `:deps`.
- no inconsistency review feedback (plan round 3)
- Checked, not filed: the public vars that plan.md names for VT-V8 exist
  (`fork/in-order`, `report/per-call-stats`, `report/fork-count`,
  `report/ratio`, `timing-tool/observe`, `declaration`,
  `validate-observation`, macro `closed-record.bench/bench-op`); `src/`,
  `deps.edn`, `.github`, `dev/bench` and `test` have no diff from `ac25942`.
  The tooling test `classpath_cache.clj` also resolves every entry of
  `dependency-bases`, so after slice 4 it fetches the value-type Git
  dependency too (network needed).

## Notes for the plan-review round-3 design-steps pass

- Three open steps (value-type coordinate, slice-6 push, local JDKs and
  VT-V8 platform). All are plan choices: resolve them in plan.md and
  steps.md. design.md stays unchanged; it already says AC4 (SHA on GitHub)
  and the CI-only "Linux x64" limit. Keep VT-I2 and no committed
  `:local/root`.
- Coordinate step: the existing Git dependency pattern is the `:quickdoc`
  alias in `deps.edn` (`:git/tag` + `:git/sha`). value-type has no tags,
  so use `:git/url` + `:git/sha` only. Use the same coordinate in the
  README instruction for unreleased value-type (README Installation shows
  only the Clojars coordinate of closed-record).
- Push step: do not choose an autonomous push to `master`; a stop-and-ask
  point (as at the slice-4 gate) agrees with the precedents in
  `munera/closed/014-github-ci/steps.md` and
  `munera/closed/017-markdown-link-checks/steps.md` (line 46 of each).
  `ci.yml` triggers: `push` to `master`, `pull_request` (any branch),
  `workflow_call`. `gh` is at `/opt/homebrew/bin/gh`.
- JDK step: the VT-V8 limits in `doc/performance.md` follow the existing
  style of that page (it names the machine "macOS arm64 MacBook Pro" and
  the runtime builds).

## Plan-review design-steps pass, round 3 (2026-11-03)

- All 3 plan steps resolved in plan.md (Alias, Local JDKs, VT-V8 limits, CI
  Push) and steps.md (slices 4 to 8). design.md unchanged. No step of
  steps.md is done.
- value-type `66d5ff7` full SHA: `66d5ff7680c227bcf44ffe0824628288ffab0ebf`.
  tools.deps needs the full SHA when no `:git/tag` is given.
- Observed: `git fetch https://github.com/hugoduncan/value-type.git
  66d5ff7680c2…` exits 0 although the commit is not on GitHub (the object
  is local). `merge-base --is-ancestor 66d5ff7 FETCH_HEAD` after a fetch of
  `master` gives 1. Use only the merge-base check.
- Local JDK binaries checked: `temurin-21.0.10+7.0.LTS/bin/java` gives
  `21.0.10+7-LTS`; `openjdk-27.0.0-jep401ea3+1-1/bin/java` gives
  `27-jep401ea3+1-1` (both under `~/.local/share/mise/installs/java/`).
  `uname -sm` is `Darwin arm64`.
- `gh` 2.102.0 has `gh run list --commit`. For a `pull_request` run,
  `headSha` is the head commit of the pull request branch.
- Open point for review: slices 7 to 9 commit after the slice-6 CI run
  (bench, docs, quality fixes). The plan does not require a second push;
  the quality job (lint, prose, links) checks those files in CI only after
  a later push.

## Plan review, fourth round (2026-11-03)

- ambiguity review (plan round 4) added 1 new design step
- The step resolves the "Open point for review" of the round-3
  design-steps pass. It is actionable because slice 8 changes `src` (the
  docstring), so the slice-6 run does not test the final tree.
- Slice-6 context: value-type `release.yml` (at `66d5ff7`, lines 47-60)
  has the full archive URL
  `https://download.java.net/java/early_access/valhalla/27/1/openjdk-27-jep401ea3+1-1_linux-x64_bin.tar.gz`,
  the `sha256sum -c` check and `tar -xzf … --strip-components=1`. It sets
  `JAVA_CMD` for each step. Its `--add-exports` option is for value-type's
  own tests; closed-record does not need it (the probe got value output
  with `--enable-preview` only).
- Checked, not filed: value-type is not on the `:test` class path, so a
  `dev/bench` load of the VT-V8 entry would fail the ordinary suite. A grep
  for requires of `closed-record.bench.value-type` and a green ordinary
  suite are enough for the slice-7 check. The test namespace names of
  VT-V2, VT-V4, VT-V5, VT-V6, does-not-load and the Clojure 1.11 case are
  free choices (they must end in `-test`). An uncaught parent exception in
  VT-V8 (for example from `timing-tool/declaration`) exits 1, the same as
  the default run.
- inconsistency review (plan round 4) added 1 new design step
- Checked, not filed: VT-V8 table brackets (fork-mean min and max, not a
  confidence interval) agree with `doc/performance.md` "Evidence and
  runtime". The artifact-map rows that steps.md names exist as stated
  (`jvm.clj` line 7, `*_test.clj` glob line 8, `dev/bench/` line 16,
  `ci.yml` line 22, `deps.edn` line 25).

## Notes for the plan-review round-4 design-steps pass

- Two open steps (post-slice-6 CI evidence, value-type embodiment page
  rows). Both are plan choices: resolve them in plan.md and steps.md.
  design.md stays unchanged; AC3 says only "pass in CI" and names no
  commit. Keep the slice-6 stop-and-ask rule and "merge is the user's
  decision".
- CI evidence step: the commits after slice 6 that the CI jobs test are
  `src/closed_record/core.cljc` (docstring, slice 8), `dev/bench/` and
  `dev/bench/timing/run.sh` (slice 7; no CI job runs them), and any slice-9
  fix. The quality job (`ci.yml` job `quality`) runs format, lint, Vale,
  API freshness and `test/tooling`; job `links` runs lychee. Locally the
  same checks are `bb clj-fmt:check`, `bb clj-kondo:lint`, `bb vale`,
  `bb api-docs --check`, `bb lychee:check`. Option (a) reuses the slice-6
  ref and the `gh run …` commands of plan.md, CI.
- Embodiment step: page `ramora/value-type-records/embodiment.md`. Edit the
  existing slice-8 item; do not add a new step for each row. Ramora rule:
  no progress history (state the current embodiment only).

## Plan-review design-steps pass, round 4 (2026-11-03)

- Both plan steps resolved in plan.md (CI: Final push, record list, slice
  order 9) and steps.md (slices 6, 8 and 9). design.md unchanged. No step
  of steps.md is done.
- Choice: option (a). Slice 9 pushes the final tree to the slice-6 ref and
  reads a second run. The slice-6 question asks for approval of both
  pushes; if the user approves only the first, slice 9 stops and asks.
  The AC3 evidence is the run of the last commit outside the task
  directory; the evidence commit (task files only) is not pushed, so no
  run-record recursion.
- The slice-8 embodiment item now names the rows of
  `ramora/value-type-records/embodiment.md` that become false: Status,
  Elements `ci.yml` and Tests rows, Verification limits. The Permitted
  dependencies and Generated type sections hold as written; check them
  against the code at slice 8.
