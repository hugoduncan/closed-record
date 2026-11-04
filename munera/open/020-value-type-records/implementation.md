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
