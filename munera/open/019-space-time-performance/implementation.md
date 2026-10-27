# Implementation notes

## 2026-10-27 — Architecture design review

- No architectural review feedback. Scope is unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture context
  came from `AGENTS.md` and `ramora/defclosedrecord.md`: purpose (including
  S-03), principles, invariants, qualities, embodiment, and its artifact map.
  `mementum/knowledge/benchmarks.md` supplies the existing measurement and
  harness constraints for later review turns.

## 2026-10-27 — Ambiguity design review

- No ambiguity review feedback. Scope is unchanged.

## 2026-10-27 — Inconsistency design review

- No inconsistency review feedback. Scope is unchanged.

## 2026-10-27 — Review handoff

- Benchmark rule ownership: `dev/bench/closed_record/bench/report.clj` owns
  S-03 timing statistics and pass decisions; `dev/bench/closed_record/bench/fork.clj`
  owns comparison order and child processes. Keep both independent of Criterium.
  Their regression tests are `test/closed_record/bench_report_test.clj` and
  `test/closed_record/bench_fork_test.clj`.

## 2026-10-27 — Ambiguity plan review

- No new ambiguity review feedback. Reviewed `plan.md` and read-only `steps.md`
  against the design, prior notes, benchmark code/tests, and referenced docs.

## 2026-10-27 — Inconsistency plan review

- No new inconsistency review feedback. The plan and read-only steps agree
  with the design and prior review notes.

## 2026-10-27 — Plan review handoff

- The S-03 field-layout check is `field-declaration-test` in
  `test/closed_record/field_value_test.clj`. In `deps.edn`, `:test` includes
  `dev/bench` without Criterium; preserve this dependency boundary when adding
  capability probes and experimental fixtures.

## 2026-10-27 — Feature API discovery

- Read all task artifacts and benchmark knowledge. No actor-step path or PR
  metadata was supplied; used the requested task in the current worktree.
- Upstream documentation distinguishes nullable JEP 401 value arrays, draft
  `!` types, and internal Valhalla storage APIs. `features.md` records URLs,
  source revision, available EA build, applicability limits, and the decision.
- Verification: retrieved the OpenJDK project, JEP 401, draft 8316779, and EA
  download pages with curl; inspected internal API source and resolved the
  lworld revision with git ls-remote. No runtime probes or benchmarks ran.
  This is discovery evidence, not proof of executable support or layout.
- No code or dependency changes; no tests are applicable to this prose-only
  slice. Next slice must obtain API intent before installing an experimental
  runtime. Design remains unchanged; plan and steps record the gate.

<!-- IMPLEMENTATION_BLOCKER: START -->
- blocker: Intended value-array and null-restricted-field APIs remain ambiguous between JEP 401, draft ! types, and internal Valhalla storage APIs.
- required-human-action: Select the intended API; confirm whether internal Valhalla APIs with atomic arrays and external comparison types are acceptable.
<!-- IMPLEMENTATION_BLOCKER: END -->

## 2026-10-27 — Plan ambiguity review after API permission

- Ambiguity review added 1 new design step; `steps.md` is unchanged.
- Commit `adfa652` changed only the design. The actionable issue is the
  proceed/stop decision after that permission, not a request to repeat API
  discovery. The later inconsistency review should use this same follow-up
  for the stale gate statements and blocker, rather than add a duplicate.

## 2026-10-27 — Plan inconsistency review after API permission

- No new inconsistency review feedback. The existing design step covers the
  stale feature-gate statements across task files; `steps.md` is unchanged.

## 2026-10-27 — Gate-resolution handoff

- Runtime setup must select the parent JVM explicitly: `:bench` in `deps.edn`
  does not select Java 21. `dev/bench/closed_record/bench/fork.clj` uses the
  parent's `java.home`, classpath, and JVM flags for children. Keep ordinary
  and experimental launch environments separate when resolving the gate.
  See `README/tool-installation.md` for the ordinary Java setup.

## 2026-10-27 — Plan-review API gate resolution

- The earlier `IMPLEMENTATION_BLOCKER` and its required human action are
  resolved by design commit `adfa652`. They remain above as historical notes,
  not active instructions. No human decision is pending before runtime setup.
- Runtime setup may proceed on the internal atomic-array/`@NullRestricted`
  path. Build-specific API verification and executable capability/layout checks
  remain prerequisites for the matrix; all six implementation slices remain open.
- Verification: reviewed the updated plan, features, and checklists against
  `adfa652` and the read-only design; `git diff --check` passes. No code,
  dependencies, runtime installation, or probe results changed. Runtime tests
  and benchmarks were not run for this plan-only follow-up.

## 2026-10-27 — Plan ambiguity review after gate resolution

- No new ambiguity review feedback. Reviewed the resolved API gate and remaining
  plan against task artifacts, benchmark code/tests, and referenced docs;
  `steps.md` is unchanged. The completed design follow-up needs no new item.

## 2026-10-27 — Plan inconsistency review after gate resolution

- No new inconsistency review feedback. The plan, read-only steps, feature gate,
  and completed design follow-up agree. The earlier blocker is explicitly
  resolved in the append-only notes; it is not an active stop condition.

## 2026-10-27 — Evidence transport handoff

- `bench-op` in `dev/bench/closed_record/bench.clj` reduces `last-bench` to
  per-call statistics before transport. The planned raw counts and batch divisors
  must be captured before that reduction; the current child EDN cannot recover
  them. Preserve this evidence when extending transport, not only its schema.

## 2026-10-27 — First executable runtime slice

- No actor-step metadata was supplied; used the requested task and current worktree.
- Added a standalone Java capability probe and shell launcher, with no library,
  dependency, or ordinary test changes. Runtime archive and classes stay in target.
- Build-specific discovery: this EA's annotation requires initialization before
  super(); the initial VerifyError and correction are retained in the manifest.
  Nullable value fields also flatten, so the next slice needs a non-flat control.
- Verification: checksum matches upstream; compiled and ran the real assertion
  probe successfully (exit 0); ordinary Clojure 1.11.4 loads on Temurin 21 without
  experimental flags. Structured evidence includes hashes for the uncommitted
  probe sources, default flags, machine, command, and prior failure.
- The next slice must check experimental Clojure and closed-record applicability
  and finish atomicity/ordering contracts. No matrix or performance claim is fixed.
  Original slice 1 checkboxes remain open because their full contracts are not done.

## 2026-10-27 — Non-flat field control

- Added an Object-declared nullable reference holder with the same Point payload.
  The real VM asserts non-flat layout independently from null acceptance and
  restriction metadata. Both reads and replacement retain the expected values;
  replacement creates a distinct holder and leaves the source unchanged.
- Retained the nullable Point field to expose the confound: it also flattens.
  Future comparisons must disclose declared type and the reference-read cast,
  not describe this as a test of null restriction alone.
- Verification: pinned Valhalla probe compiles and exits 0; structured results,
  stderr, command, and source hashes are in evidence/reference-field. Original
  probe evidence is preserved. No Clojure code or dependencies changed, so no
  Clojure tests were needed. Design is unchanged; plan and steps reflect this
  bounded slice. Clojure applicability and ordering contracts remain next.

## 2026-10-27 — Clojure applicability

- Added an isolated Clojure assertion probe and launcher using the base classpath.
  Made the Java Point fixture public for direct, reflection-free Clojure access.
- Actual generated fields remain Object even with the Point hint; they accept
  null and are neither restricted nor flat. Records remain identity classes.
  External values and flat arrays work as payloads, including visible mutation.
- Retained the initial missing-import failure and successful corrected run under
  evidence/clojure-applicability. No runtime dependency or library change.
- Verification: real Java/Clojure probes exit 0; explicit format and shared lint
  pass. On Temurin 21, Scry field-value tests pass (12 tests, 34 assertions),
  exit 0, with no failure artifacts. Design is unchanged.
- Next slice needs build-specific atomicity/ordering documentation and final
  gate reconciliation before selecting the inventory and memory tools.

## 2026-10-27 — Operation contracts and gate closure

- Build archive source confirms atomic-layout metadata, not volatile ordering.
  Documented thread-confined plain array operations and immutable final-holder
  replacement separately. No concurrency or performance claim is made.
- Strengthened the real Java probe: required flat layouts now fail assertions
  on mismatch; both arrays check atomicity and restriction metadata. Repeated
  Java and Clojure applicability probes pass (exit 0); bounded evidence is in
  evidence/operation-contract. No Clojure source or dependencies changed.
- Slice 1 is complete; design remains unchanged. Next slice must select the
  fixed inventory and validate allocation and graph tools, especially flat
  layouts. Capability success is not memory-tool compatibility evidence.
- Verification: both real probe launchers and git diff --check pass. No long
  benchmarks ran. No actor-step metadata was supplied beyond the requested path.

## 2026-10-27 — Fixed inventory and accounting contract

- Added 17 stable comparison pairs; preserved all six legacy expressions and
  their exclusive S-03 scope. Widths are 3/16; array lengths are 4/1024.
- Graph size counts shared identities once and flat payloads within their
  container. Incremental size is union minus source, not result minus source.
  Prebuilt replacement payloads can add reachable bytes without operation allocation.
- Next slice must validate memory tooling and executable fixture equivalence.
  Changed array stores require reset outside each measured operation, not only
  once per batch. Memory budgets and acceptance readiness remain open.
- Verification: Temurin 21 Scry inventory tests pass (2 tests, 23 assertions),
  exit 0, with no failure files. Shared format and lint pass. No dependencies,
  runtime code, or design changed; no benchmarks ran. No actor-step metadata
  beyond the requested task path was supplied.

## 2026-10-27 — Ordinary assoc fixture equivalence

- Added `closed-record.bench.fixtures/assoc-pair`: returns matched actual record
  sources, key, and shared replacement from the inventory. Fixed generated types
  cover widths 3/16; unknown widths throw. Setup is outside measurement and no
  operation wrapper is introduced into future timing call sites.
- Real tests verify changed values, source preservation, distinct result identity,
  type preservation, and shared unchanged/replacement payload identities. Corrected
  an initial test assumption: `check` returns the valid record, not nil.
- Temurin 21 benchmark regression tests pass: 16 tests, 166 assertions, exit 0,
  no Scry failure artifacts. Shared formatting and lint pass after format repair.
  No dependencies, library semantics, or design changed. No timing ran.
- Next slice still needs experimental fixture equivalence and validated memory
  tools. This bounded fixture subset does not close the full equivalence item
  or the measured assoc item. No additional actor-step metadata was supplied.

## 2026-10-27 — Experimental fixture equivalence

- Added an inventory-driven Clojure launcher and Java assertions for lengths
  4/1024 and field pairs. Java assertions inspect actual payloads and layouts;
  no mocks or measurement tools are used. The capability probe runs first.
- Two successive changed stores with explicit resets verify all array elements.
  Field replacement checks distinct holders and unchanged sources. Value objects
  are compared by payload, not identity. Setup checks are not timing call sites.
- Pinned EA probe exits 0; bounded output and source hashes are in
  evidence/fixture-equivalence. Shared format and lint pass after adding the
  explicit Java import. No ordinary library code or dependencies changed.
- Next slice needs memory-tool validation and budgets. These checks do not prove
  the future timed reset boundary or allocation/graph accounting. No performance
  acceptance run occurred. Design is unchanged. No actor-step metadata was supplied.

## 2026-10-27 — Allocation counter control subset

- Added standalone real-JVM controls using the JDK ThreadMXBean. No Clojure,
  library, or dependency changes. Roots escape; setup is outside counter reads.
- Both pinned runtimes pass: 4096 arrays give 327680 bytes for 64-byte payloads
  and 589824 bytes for 128-byte payloads; the empty boundary gives zero. A worker
  independently reports the same count. Raw EDN and runtime versions are retained.
- Next slice must test cross-thread exclusion and flat-value allocation, then
  select and validate reachable-graph tooling. These byte-array controls do not
  establish Valhalla graph support. The parent allocation checklist stays open.
- Verification: both allocation-probe.sh runs exit 0; shell syntax and
  git diff --check pass. No timing acceptance or Clojure tests ran because only
  isolated Java/shell controls and task documentation changed. Design is unchanged.
  No actor-step metadata beyond the task path was supplied.

## 2026-10-27 — Allocation thread exclusion

- Added a real concurrent control: volatile handshakes enclose worker allocation
  within the parent's counter interval; thread setup and join stay outside it.
  Both pinned JDKs report zero parent bytes and 327680 worker bytes for 4096
  escaping arrays, including 20 repeated controls before the recorded sample.
- Verification: both probe commands exit 0; shell syntax and git diff --check
  pass. Evidence is in evidence/allocation-thread-scope. No Clojure code changed,
  so no Clojure suite ran. No timing or acceptance measurements ran.
- Next slice still needs flat-layout allocation and independently validated graph
  accounting. Thread exclusion does not approve those metrics. Design and
  dependencies are unchanged. No actor-step metadata was supplied beyond the
  requested task path; used the current worktree.

## 2026-10-27 — Flat-array allocation growth

- Added a real flat-array control with escaping roots and warmed counter paths.
  Class-based Unsafe layout queries reject concrete value arrays: use instance
  queries because one class can have different array representations.
- Raw allocation growth matches the runtime stride. Construction has 24 bytes
  per call beyond the array layout; its cause and flat-field controls remain
  follow-up work. Do not use these counts as retained array sizes.
- Verification: pinned EA probe exits 0; shell syntax and git diff --check pass.
  Retained two failed attempts (base-offset type, then class-based query) and
  successful evidence. No Clojure source, dependencies, or design changed; no
  Clojure tests or timing acceptance ran. No actor-step metadata was supplied.

## 2026-10-27 — Array argument allocation control

- Added a real preboxed Object control beside the typed Point call site. Both
  use the same native constructor and logical payload. Preboxing outside the
  counter removes 24 bytes per operation at both lengths; allocation then equals
  instance layout. This isolates the argument boundary, not a general VM rule.
- Future construction cases must specify whether argument adaptation is inside
  the measured operation. No overhead is subtracted. Flat-field allocation and
  graph validation remain required before memory-tool approval.
- Verification: pinned EA allocation probe exits 0; payload/layout assertions,
  shell syntax, and git diff --check pass. Evidence includes raw counts and
  source hashes. No Clojure code or dependencies changed, so no Clojure tests
  ran. No timing acceptance ran. Design is unchanged. No actor-step metadata
  beyond the requested task path was supplied; used the current worktree.

## 2026-10-27 — Flat-field allocation control

- Added real escaping-holder assertions and a benchmark-only Instrumentation
  agent. Flat allocation is 16 bytes per holder; typed reference construction
  allocates 40 bytes: a 16-byte holder plus a 24-byte payload box. Counts agree
  with independently queried shallow sizes; no overhead is subtracted.
- Retained the initial failed assumption that reference construction allocates
  only the holder. Next slice needs a preboxed reference argument control.
  Shallow-size agreement does not validate reachable-graph accounting.
- Verification: pinned EA field-allocation.sh exits 0; payload, distinct-holder,
  layout, counter, and shallow-size assertions pass. Shell syntax and
  git diff --check pass. Evidence includes source hashes and the failed attempt.
  No Clojure source or dependencies changed; no Clojure tests or timing ran.
  Design is unchanged. No additional actor-step metadata was supplied.

## 2026-10-27 — Preboxed field allocation boundary

- Added an Object constructor overload while retaining the typed Point path.
  Preboxing outside sampling reduces reference allocation from 40 to 16 bytes
  per holder, equal to Instrumentation shallow size. No overhead is subtracted.
- Real VM assertions check payloads, distinct escaping holders, layout, and
  allocation after 100 warmup samples. Evidence is in
  evidence/preboxed-field-allocation; the command exits 0.
- Shell syntax and git diff --check pass. No Clojure code or dependencies
  changed, so no Clojure tests ran. No timing acceptance ran. Design is unchanged.
- Next slice needs shared/unshared retained-graph controls before memory-tool
  approval. No actor-step metadata was supplied; used the requested task and
  current worktree.

## 2026-10-27 — Ordinary-array graph accounting

- Added real Instrumentation controls for shared and distinct equal arrays,
  duplicate roots, cycles, nulls, and unsupported objects. Root argument arrays
  are transport only; cast an array to Object to count it as a root container.
- Both pinned runtimes report source/result 192 bytes each, union 304 bytes,
  and incremental 112 bytes. Expected totals use independent shallow queries.
  Evidence and source hashes are in evidence/array-graph.
- Both launcher runs, shell syntax, and git diff --check pass. No Clojure code
  or dependencies changed; no Clojure tests or timing acceptance ran.
- Next slice must validate record traversal and flat layouts. The array-only
  walker is a control, not an approved general graph tool. Design is unchanged.
  No actor-step metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Flat-container graph accounting

- Added isolated real-VM controls for primitive-only Point containers. Do not
  read flat payloads during traversal: materialized boxes are not graph nodes.
  Arrays use instance-specific Unsafe layout queries with Object[] arguments.
- Pinned EA controls pass: length 4/1024 graphs are 32/4112 bytes; flat
  holders are 16 bytes. Union, incremental, duplicate-root, null, and
  unsupported reference-holder checks pass. Evidence retains two compile
  failures before the corrected instance-query calls.
- Shell syntax and git diff --check pass. No Clojure code or dependencies
  changed; no Clojure tests or timing acceptance ran. Design is unchanged.
- Next slice needs record traversal and reference-container graph controls;
  memory tooling is not yet approved. No additional actor-step metadata was
  supplied; used the requested task and current worktree.

## 2026-10-27 — Value-payload identity boundary

- Real reference-container controls expose a graph-tool limit: separately
  constructed equal Points collapse in IdentityHashMap. This is not proof of
  physical box sharing. A physical accounting method is still required.
- Retained the failed expectation that identity hashing throws, then the
  corrected passing control in evidence/value-identity. Do not interpret the
  passing semantic control as approval of graph measurement.
- Verification: pinned EA value-identity.sh exits 0; shell syntax and
  git diff --check pass. No Clojure source or dependencies changed; no Clojure
  tests or timing acceptance ran. Design is unchanged. No actor-step metadata
  was supplied; used the requested task and current worktree.

## 2026-10-27 — Ordinary record graph traversal

- Added a bounded Instrumentation walker for explicitly selected identity record
  classes and primitive arrays. All instance reference fields are edges, including
  metadata; static caches are excluded. Unknown graphs fail without partial totals.
- Both pinned runtimes pass real width-3/16 closed-record and defrecord controls
  for sharing, distinct equal arrays, nulls, duplicate roots, and union/incremental
  accounting. Evidence: evidence/record-graph. No mocks or dependencies added.
- Both launcher exits are 0; shell syntax, explicit formatting, shared lint, and
  git diff --check pass. Fixed the initial missing Java import lint warning.
  These executable assertions are isolated controls; no Scry or timing run.
- Next: boxed Long inventory payloads still need validation, as does physical
  value-box accounting. This walker is not yet an approved matrix memory tool.
  Design is unchanged. No actor-step metadata was supplied; used the requested
  task and current worktree.

## 2026-10-27 — Inventory Long graph controls

- The bounded walker now treats exact Long objects as leaves. Inventory-driven
  controls verify both widths and record types against independent shallow sums,
  including source/result sharing and repeated cached payload roots. Cached data
  is reachable size, not exclusive retention or operation allocation.
- Both pinned runtime commands exit 0; existing primitive-array and rejection
  controls also pass. Evidence: evidence/boxed-long-graph. Explicit formatting,
  focused lint, shell syntax, and git diff --check pass. Shared lint reports an
  unrelated duplicate require in dev/acceptance/api_content_defect.clj; left it
  unchanged. No Scry or timing acceptance run; these are real JVM tool controls.
- Next: physical value-box accounting still blocks full memory-tool approval.
  No design or dependency changes. No actor-step metadata was supplied; used
  the requested task in the current worktree.

## 2026-10-27 — Allocation divisor and empty boundary

- Unequal real allocation batches now verify the actual operation divisor; an
  empty batch retains raw zero bytes with unavailable normalisation, not zero
  bytes/op. Both pinned runtimes pass all allocation controls (exit 0).
- Evidence: evidence/allocation-divisor. Shell syntax and git diff --check pass.
  No Clojure source or dependencies changed; no Clojure tests or timing ran.
- Allocation-control checklist is complete. Next: physical value-box accounting,
  full memory-tool approval, and budgets remain open. Design is unchanged.
  No actor-step metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Pure metric normalisation

- Added a pure reporting helper with no measurement dependencies. Measured
  outcomes retain raw totals and divisors; failed/unavailable outcomes require
  a reason and reject numeric results. Graph bytes cannot have a divisor.
  Allocation and graph totals must be integral; time can be fractional.
- Took this independent slice 3 subset while physical value-box accounting
  remains open. No memory-tool approval, design change, or acceptance run.
- Temurin 21 benchmark regression tests pass: 19 tests, 197 assertions, exit 0,
  no Scry failure artifacts. Focused format and lint pass. Used structural repair
  for an initial test delimiter error, then corrected the repaired call form.
- Next: add case/configuration/representation identity validation, duplicate
  detection, and aggregation before transport integration. The old S-03 path is
  unchanged. No actor-step metadata was supplied; used the requested task in
  the current worktree.

## 2026-10-27 — Result identity validation

- Added `metrics/validate-results` against the supplied inventory. Identity is
  case/configuration/representation/fork/metric/root; duplicate identities fail
  even across outcome states. Graph roots are required only for graph metrics.
- Partial vectors, including empty vectors, are valid failure evidence, not
  complete-run evidence. Next add coverage checks and aggregation before transport.
  No design, dependency, S-03, or memory-tool gate changes.
- Temurin 21 benchmark regression tests pass (20 tests, 214 assertions), exit 0,
  with no Scry failure artifacts. Focused format, lint, and git diff --check pass.
  Delimiter repair fixed the initial test form. An initial shell Java selection
  was wrong; reran successfully with the explicit pinned Temurin installation.
  No timing acceptance ran. No actor-step metadata was supplied.

## 2026-10-27 — Complete result coverage

- Added `metrics/validate-complete-results`; missing identities are in `:missing`
  exception data. It reuses partial validation and retains input order. Complete
  coverage does not imply measurement success or acceptance readiness.
- Mock-free tests remove each expected identity in turn and check both graph
  roots, five forks, reversed order, duplicates, bad units, and explicit failures.
- Benchmark regression tests pass: 21 tests, 249 assertions, exit 0, empty
  Scry failure directory. Ran on Homebrew OpenJDK 27; pinned-runtime final
  verification remains open. Focused formatting, lint, and diff checks pass.
  Initial formatting invocation timed out; explicit JAVA_HOME resolved it.
- Next: five-fork aggregation and transport integration. No design, dependency,
  S-03, or memory-tool gate changes. No timing acceptance ran. No actor-step
  metadata was supplied; used the requested task and current worktree.

## 2026-10-27 — Five-fork summary

- Added `metrics/aggregate-results` over complete inventory evidence. Each fork
  has equal weight; unequal operation counts do not weight the cross-fork mean.
  Range and sample standard deviation describe variation, not confidence bounds.
- Missing measurements suppress numeric summaries but retain sorted raw outcomes
  and failure reasons. Missing identities still fail coverage validation.
- Benchmark regression tests pass: 22 tests, 261 assertions, exit 0, on Homebrew
  OpenJDK 27. Scry failure directory is empty. Focused format, lint, and diff
  checks pass. No timing acceptance run or dependency changes.
- Next: integrate raw metric transport. Memory-tool gates remain open. Design
  and S-03 are unchanged. No actor-step metadata was supplied; used the requested
  task in the current worktree.

## 2026-10-27 — Metric table rendering

- Added `metric-table/render` from inventory and complete raw outcomes. It
  validates and aggregates before rendering; it cannot publish partial coverage.
  Fixed-locale mean, range, and sample SD do not imply confidence or parity.
- Missing summaries show no numbers and retain each failed/unavailable fork
  reason. Markdown cells escape pipes, line breaks, and HTML opening characters.
- Real-inventory tests cover all topics, dimensions, roots, reversed input,
  German default locale, missing measurements, and incomplete coverage. Temurin
  21 benchmark tests pass: 23 tests, 279 assertions; exit 0, no Scry failure
  artifacts. Focused format, lint, and diff checks pass.
- Next: durable metric transport and process-failure tests. Memory-tool approval
  remains open. Design and dependencies are unchanged; no acceptance run occurred.
  No actor-step handoff metadata was supplied; used the requested task and current
  worktree.

## 2026-10-27 — Raw EDN envelope

- Added pure schema-version 1 transport with exact parent run/revision/config/fork
  association. Child results must match config/fork; raw counts remain unchanged.
  Empty/partial vectors are failure evidence only. Reject trailing forms and
  unsafe/unknown reader syntax. Legacy S-03 transport is unchanged.
- Next slice must persist manifests and child artifacts before aggregation and
  test real process failures. No memory-tool approval or acceptance run occurred.
- Temurin 21 benchmark tests pass: 24 tests, 296 assertions, exit 0; Scry failure
  directory is empty. Focused format, lint, and diff checks pass. No dependencies
  or design changes. No actor-step metadata was supplied; used the requested task
  in the current worktree.

## 2026-10-27 — Durable run manifest

- Added a filesystem boundary that validates child associations before creating
  a run directory and writes an exclusive manifest with the inventory snapshot.
  Duplicate attempts fail without replacing prior bytes. Caller metadata is
  retained, but required metadata fields are not yet enforced.
- Real filesystem tests cover round trips, duplicate attempts, invalid forks,
  duplicate children, and mixed revisions. Benchmark tests pass on Homebrew
  OpenJDK 27: 25 tests, 309 assertions, exit 0; no Scry failure artifacts.
  Focused format, lint, and diff checks pass. No dependencies or design changes.
- Next: persist child outputs, raw results, exit status, and validation failures
  against this manifest; then integrate with the fork runner. No acceptance
  measurement ran. No actor-step metadata was supplied beyond the task path;
  used the current worktree.

## 2026-10-27 — Durable child capture

- Added manifest-indexed capture slots with exclusive creation. Save stdout,
  stderr, raw result text, and process status before envelope validation. Missing
  and invalid EDN retain diagnostics; a valid envelope with nonzero exit remains
  valid transport evidence, not a successful process or complete measurement.
- Real filesystem tests cover malformed/missing results, wrong child association,
  nonzero exit, UTF-8 output, slot reuse, and preservation of earlier results.
  These tests supply completed process data; real launch/file-read tests are next.
- Temurin 21 benchmark tests pass: 26 tests, 354 assertions, exit 0, empty Scry
  failure directory. Focused format/lint and diff checks pass. Design reviewed
  and unchanged; no dependency changes or acceptance measurements.
- Next: connect the fork runner, preserve launch failures, and enforce required
  runtime/command metadata. Memory-tool gates remain open. No actor-step metadata
  was supplied; used the requested task and current worktree.

## 2026-10-27 — Real child process capture

- Added `fork/capture-process!`: command builder receives the saved child
  association and an absent absolute result path. Streams redirect to files,
  avoiding pipe deadlocks. Exclusive process directories preserve raw evidence
  before capture validation; duplicate launch attempts cannot replace it.
- Real shell tests cover valid and wrong-association envelopes, malformed and
  absent files, nonzero exits, UTF-8 streams, paths with spaces, and preservation
  after later failure. No mocks or measurement dependencies were added.
- Temurin 21 benchmark tests: 27 tests, 382 assertions pass, exit 0, no Scry
  failure artifacts. Focused formatting, lint, and diff checks pass.
- Next: structured launch/read failure outcomes and required metadata validation.
  Launch/read exceptions currently propagate with available raw files retained.
  No acceptance measurement ran. Design remains unchanged. No actor-step or PR
  metadata was supplied; used the requested task in the current worktree.

## 2026-10-27 — Structured launch failure

- `capture-process!` now returns and saves launch IOException outcomes in
  `process-N/outcome.edn`, with child association and failure stage/reason/class.
  No exit or validation is invented. Catch scope excludes waiting and capture;
  interruption and programming errors still propagate.
- Real missing-executable tests verify saved command, diagnostics, absent exit,
  and unchanged evidence after retry. Existing real-process preservation tests
  also pass. Temurin 21 benchmark tests: 28 tests, 393 assertions, exit 0; Scry
  failure directory empty. Focused format, lint, and diff checks pass.
- Next: result-read failure outcomes and runtime/command metadata validation.
  Design reviewed and unchanged; no dependencies or acceptance measurements.
  No actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Structured result-read failure

- Result-file IOException outcomes now persist in process-N/outcome.edn with
  :stage :result-read and the completed exit. The catch excludes stream reads
  and evidence writes; those errors still propagate. Missing files retain the
  existing missing-result validation path, not an IO-failure outcome.
- Real shell children create a directory at the result path and exit 7. Tests
  verify saved diagnostics, streams, exit, and unchanged evidence after retry.
  Temurin 21 benchmark tests: 29 tests, 406 assertions, exit 0; Scry failure
  directory empty. Focused format, lint, and git diff --check pass.
- Next: required runtime/command metadata validation. Design reviewed and
  unchanged; no dependencies or acceptance measurements. No actor-step or PR
  metadata was supplied; used the requested task in the current worktree.

## 2026-10-27 — Required manifest metadata

- Run creation now rejects absent or malformed command/runtime declarations
  before creating files. Runtime declarations are keyed by the exact planned
  configuration set and include version, Java home, and explicit flags.
  Additional provenance remains unchanged. No dependencies changed.
- Real filesystem tests cover rejected input, absent directories, preserved
  metadata, and existing retry/process contracts without mocks. The first run
  exposed an overly narrow test message expectation for nil metadata; corrected
  the assertion to check the rejection contract.
- Temurin 21 benchmark tests: 30 tests, 435 assertions pass, exit 0; no Scry
  failure artifacts. Focused formatting, lint, and diff checks pass.
- Next: generated-command validation and child runtime verification. Declarations
  alone do not verify runtime identity or complete acceptance provenance.
  Design reviewed and unchanged. No acceptance measurements ran. No actor-step
  or PR metadata was supplied; used the requested task and current worktree.

## 2026-10-27 — Generated command validation

- `capture-process!` rejects malformed command vectors before creating the
  process directory. Empty arguments remain valid; NULs and blank executables
  fail. Rejected commands leave the child slot available for correction.
- Real filesystem and shell tests verify rejection and a subsequent successful
  launch with an empty argument. No mocks or dependencies added. OpenJDK 27
  benchmark tests: 31 tests, 461 assertions pass, exit 0; Scry failure directory
  empty. Focused format, lint, and diff checks pass.
- Next: verify declared runtime metadata against measured children. Command
  structure alone cannot establish provenance. Design reviewed and unchanged;
  no acceptance measurements ran. No actor-step or PR metadata was supplied;
  used the requested task in the current worktree.

## 2026-10-27 — Runtime observation contract

- Added a collector for runtime build, Java home, and ordered JVM input flags.
  Pure reconciliation preserves extra fields and reports differences without
  normalising paths or flag order. Callers supply validated declarations.
- Next slice must connect child observations to durable capture and test real
  child JVMs. Current-process checks do not yet establish child provenance.
- Temurin 21 benchmark tests: 33 tests, 479 assertions pass, exit 0; Scry failure
  directory empty. Focused format and lint pass. Initial test delimiter damage
  was repaired with clj-paren-repair before verification. No mocks, dependencies,
  design changes, or acceptance measurements. No actor-step or PR metadata was
  supplied; used the requested task in the current worktree.

## 2026-10-27 — Durable runtime reconciliation

- Capture now saves `:runtime-validation` separately from envelope validity and
  exit status. Missing observations fail; invalid envelopes leave runtime checks
  unavailable. Raw evidence and field-specific differences remain saved.
- Real child JVM tests cover matching, mismatched, and absent observations.
  OpenJDK 27 benchmark tests pass: 34 tests, 495 assertions, exit 0; Scry failure
  directory empty. Focused format, lint, and diff checks pass. Initial test
  delimiter damage was repaired before verification. No mocks or dependencies.
- Next: measured children must emit `:runtime`, and orchestration must require
  both validations plus exit success. Memory-tool gates remain open. Design
  reviewed and unchanged; no acceptance measurements ran. No actor-step or PR
  metadata was supplied; used the requested task in the current worktree.

## 2026-10-27 — Child success gate

- Added `evidence/successful-child?` as the shared process/transport gate. It
  does not certify metric coverage or measurement success; orchestration must
  still apply those contracts. Measured-child integration remains next.
- Mock-free rejection tests cover missing outcomes, nonzero exits, IO failures,
  and each invalid or absent diagnostic. OpenJDK 25.0.2 benchmark tests pass:
  35 tests, 512 assertions, exit 0; Scry failure directory empty. Focused format,
  lint, and diff checks pass. Initial test delimiter damage was repaired before
  verification. No dependencies, design changes, or acceptance measurements.
- No actor-step or PR metadata was supplied; used the requested task and current
  worktree. Memory-tool approval and full harness integration remain open.

## 2026-10-27 — Child envelope writer

- Added `child/write-results!` for measured-child integration. It supplies actual
  runtime metadata, validates before writing, and uses exclusive UTF-8 creation.
  IO failures propagate; this is not an atomic or crash-safe publication scheme.
- Real JVM capture tests use the writer for matching and mismatched declarations.
  Filesystem tests cover invalid results without file creation and retry safety.
- OpenJDK 25.0.2: benchmark Scry tests pass (36 tests, 519 assertions), exit 0,
  empty failure directory. Focused format and lint pass. No new dependencies.
- Design reviewed and unchanged. Next connect measured call sites and require
  the child-success gate before aggregation. No acceptance measurements ran.
  No actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Complete-run assembly

- Added a pure assembly boundary that requires the child-success gate and full
  inventory coverage before reporting. Raw counts and explicit unavailable
  outcomes remain unchanged. Captures follow manifest child order.
- Next connect saved capture reads and measured call sites. This helper does
  not establish runtime provenance independently of capture diagnostics.
- OpenJDK 25.0.2 benchmark Scry tests pass: 37 tests, 524 assertions, exit 0;
  failure directory empty. Focused format and lint pass. Initial test delimiter
  damage was repaired, then the test scope corrected before verification.
- No dependencies, design changes, or acceptance measurements. No actor-step
  metadata was supplied; used the requested task and current worktree.

## 2026-10-27 — Saved-run assembly

- Added read-only saved-capture loading and `run-results/read-results`. Missing
  capture files throw rather than invent outcomes; failed captures fail assembly.
  Launch failures without child captures therefore cannot produce a report.
- Real filesystem tests verify complete raw results, absent captures/results,
  nonzero exits, and byte-for-byte evidence preservation. OpenJDK 25.0.2 benchmark
  Scry tests pass: 38 tests, 533 assertions, exit 0; failure directory empty.
  Focused format and lint pass. No mocks or dependencies added.
- Next connect measured call sites. Saved diagnostics are trusted as capture
  artifacts; this reader is not an evidence-tampering detector. Design reviewed
  and unchanged. No acceptance measurements ran. No actor-step or PR metadata
  was supplied; used the requested task and current worktree.

## 2026-10-27 — Saved-run Markdown reporting

Added `run-results/render-saved`: read one manifest/capture snapshot, apply
assembly gates, then return Markdown without writing evidence. Measured call
sites still need integration. No design or dependency change. The first test
used one fork and correctly hit the existing aggregation minimum; corrected
it to two saved forks rather than weaken variation validation.

Verification: Temurin 21 benchmark regression suite passes, 39 tests and 544
assertions, exit 0, no Scry failures. Focused format/lint and diff checks pass.

## 2026-10-27 — S-03 regression boundary

Added a mock-free report regression with large timing differences across all
three exploratory topics. The table retains measured outcomes and has no parity
labels or legacy ratio/interval columns. Existing S-03 and field-declaration
checks pass unchanged; no production code, dependency, or design change.

Verification: Temurin 21 benchmark and field-value Scry tests pass, 52 tests and
592 assertions, exit 0, no failure artifacts. Focused format, lint, and diff
checks pass. The same tests also pass on OpenJDK 25.0.2. No benchmark ran.
Next slice still needs measured-call-site integration and memory-tool approval;
these tests do not establish performance acceptance. No actor-step or PR
metadata was supplied; used the requested task and current worktree.

## 2026-10-27 — Raw timing adapter

Added pure `timing/outcome` for the measured-child boundary. It preserves the
bootstrap mean batch estimate and divisor, validates through metric normalization,
and returns raw evidence without `:value`. It does not retain individual samples
or change legacy call sites. Next connect it before `per-call-stats` discards
the divisor. No dependencies or design changes; memory-tool approval stays open.

Temurin 21 benchmark Scry tests pass: 41 tests, 574 assertions, exit 0, no
failure artifacts. Focused format/lint and diff checks pass. No measurement
ran. No actor-step or PR metadata was supplied; used the requested worktree task.

## 2026-10-27 — Capture IO failure boundary

Added real shell-child tests for both stream-read failures and a capture-directory
write conflict. Children move stream files before creating unreadable directory
paths; saved stream bytes, result, command, exit, and manifest remain intact.
Exceptions propagate without a saved success outcome. No production code, design,
or dependency change. Measured-child runtime verification remains next.

OpenJDK 25.0.2 benchmark Scry tests pass: 42 tests, 602 assertions, exit 0;
no failure artifacts. Focused formatting, lint, and diff checks pass. No timing
or acceptance run. No actor-step or PR metadata was supplied; used the requested
task in the current worktree.

## 2026-10-27 — Invalid result identity process checks

Real shell children now emit valid EDN with unknown case, configuration, or
representation IDs. Exit zero does not bypass inventory validation. Tests check
rejection diagnostics, the failed success gate, and exact raw evidence in both
process and capture directories. No production, design, or dependency changes.
The remaining command-level exit contract needs the measured command integration;
this slice does not close that checklist item or the memory-tool gates.

Temurin 21 benchmark Scry tests pass: 43 tests, 626 assertions, exit 0; no
failure artifacts. Focused formatting, lint, and diff checks pass. No acceptance
measurement ran. No actor-step or PR metadata was supplied; used the requested
task in the current worktree.

## 2026-10-27 — Saved runtime rejection

Saved-run tests now reject missing and mismatched runtime observations through
both raw-result reading and Markdown reporting. They use real capture files and
verify byte-for-byte preservation after rejection. No production, dependency,
or design change. Measured-call-site integration remains next; this regression
does not approve memory tools or establish acceptance evidence.

OpenJDK 25.0.2 benchmark Scry tests pass: 43 tests, 636 assertions, exit 0;
no failure artifacts. Focused formatting, lint, and diff checks pass. Delimiter
repair corrected the initial test edit before verification. No measurement ran.
No actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Saved report command

Added a manual saved-report entry point with no measurement dependencies. It
prints only a fully rendered table; argument, evidence, and file IO failures
exit 2 with stderr diagnostics. Exit 0 permits explicit unavailable metrics and
is not acceptance success. Original S-03 behavior is unchanged. Real JVM tests
cover both successful states, failed children, missing files, and argument errors;
existing snapshots verify read-only preservation.

OpenJDK 25.0.2 benchmark Scry tests pass: 44 tests, 654 assertions, exit 0;
no failure artifacts. Focused format, lint, and diff checks pass. No dependencies,
design changes, or measurements. Next connect measured call sites; memory-tool
approval remains open. No actor-step or PR metadata was supplied; used the
requested task and current worktree.

## 2026-10-27 — Raw allocation adapter

Added pure `allocation/outcome` for the memory-child boundary. It retains both
counter readings, delta, and divisor without overhead subtraction. Readings must
be nonnegative signed-long integers in increasing order; zero-operation controls
cannot produce bytes/op. Same-thread provenance and escaping results remain
collector obligations. No dependencies or design changes. Next connect counter
collection and measured call sites; memory-tool approval remains open.

OpenJDK 25.0.2 benchmark Scry tests pass: 45 tests, 678 assertions, exit 0;
no failure artifacts. Focused format/lint and diff checks pass after formatting.
No acceptance measurement ran. No actor-step or PR metadata was supplied; used
the requested task in the current worktree.

## 2026-10-27 — Raw graph adapter

Added pure `graph/outcomes` for replacement graph metrics. It retains all three
input counts, uses inventory root names, and rejects impossible union bounds.
The upper-bound sum uses overflow-safe arithmetic. Physical deduplication remains
a collector obligation; this does not approve reference-value graph accounting.
Next connect validated collectors and measured call sites. Design reviewed and
unchanged; no dependencies or acceptance measurements.

OpenJDK 27 benchmark Scry tests pass: 46 tests, 745 assertions, exit 0;
no failure artifacts. Focused format/lint and diff checks pass. Tests cover
shared, disjoint, identical, empty, invalid, and maximum-long counts without
mocks. No actor-step or PR metadata was supplied; used the requested task in
the current worktree.

## 2026-10-27 — Allocation counter access

Added benchmark-only JDK counter access without dependencies. Preparation enables
the counter outside measurement; reads return a primitive long and require the
expected current thread. Unavailable readings throw rather than return zero.
Real escaping-array and joined-thread tests use no mocks. Next connect literal
batches, result escape, and operation counts; these tests do not quantify reader
overhead or approve full memory tooling. Design reviewed and unchanged.

OpenJDK 25.0.2 benchmark Scry tests pass: 47 tests, 752 assertions, exit 0;
no failure artifacts. Focused format/lint and diff checks pass. No acceptance
measurement ran. No actor-step or PR metadata was supplied; used the requested
task in the current worktree.

## 2026-10-27 — Escaping assoc allocation batches

Added `assoc-allocation/collect!`, returning an Object array of all results,
thread ID, and raw allocation outcome. Root setup is outside the interval;
loop/store/counter costs remain included. Reject invalid batch sizes and unchanged
or absent keys before sampling. No dependency or design change. Next integrate
warmup, bounded budgets, and child transport; do not publish these tests as
acceptance measurements or infer exact assoc cost from uncharacterized overhead.

OpenJDK 25.0.2 benchmark Scry tests pass: 49 tests, 1175 assertions, exit 0;
no failure artifacts. Focused format/lint and diff checks pass. Tests use real
inventory records and JDK counters without mocks. Memory-tool approval remains
open. No actor-step or PR metadata was supplied; used the requested task and
current worktree.

## 2026-10-27 — Assoc boundary collector

Added `assoc-allocation/collect-boundary!` with an explicit `:assoc-boundary`
control tag. Every root references the prebuilt source; no assoc or root setup
is inside sampling. Counter/loop/store costs remain raw and must not be subtracted.
Tests deliberately avoid a JIT-dependent zero-byte assertion. Next retain warmed
controls with measured-child evidence before selecting budgets. No dependency or
design change; no acceptance measurement ran.

OpenJDK 27 benchmark Scry tests pass: 50 tests, 1276 assertions, exit 0; no
failure artifacts. Focused formatting, lint, and diff checks pass. No actor-step
or PR metadata was supplied; used the requested task and current worktree.

## 2026-10-27 — Fixed assoc allocation warmup

Added `collect-warmed!`: positive explicit warmup count, raw assoc/boundary
outcomes per warmup batch, and final escaping roots. Warmup roots are not retained
in the returned evidence. No subtraction or convergence claim is made. Next:
serialize outcomes without roots in child evidence and establish actual budgets;
test counts (2 batches of 17) are not benchmark defaults. Design is unchanged.

Verification: Scry targeted namespace passed, 5 tests / 753 assertions; exit 0
and no failure artifacts. `bb clj-fmt:check` passed after indentation correction.
`bb clj-kondo:lint` reports an existing duplicate `clojure.edn` require in
`dev/acceptance/api_content_defect.clj`, outside this slice; no changed-file
findings. No acceptance measurements ran.

## 2026-10-27 — Serializable warmed allocation evidence

Added `assoc-allocation-result/collect!`. It validates identity before collection
and emits one final allocation row with `:allocation-controls` holding raw warmup
and boundary outcomes. Java roots do not enter transport; control counts are not
subtracted or counted as separate metrics. Real inventory/counter/file tests round
trip all four rows through the child writer and transport. Next connect measured
process orchestration and select budgets from stabilization evidence. This does
not close the broader persistence/budget item or memory-tool approval. Design and
dependencies are unchanged; no acceptance measurements ran.

Verification: OpenJDK default runtime benchmark Scry suite passes, 54 tests /
1579 assertions, exit 0, no failure artifacts. Focused format, lint, and diff
checks pass. A test delimiter error was repaired with clj-paren-repair before
verification. No actor-step or PR metadata was supplied; used the requested task
and current worktree.

## 2026-10-27 — Measured allocation child capture

Added `assoc-allocation-child/write-results!` for both ordinary widths. It takes
explicit budgets, alternates representation order by fork, and writes runtime
observations with raw allocation controls. Real child JVM tests now pass through
process capture and saved evidence reads. Next: the bounded manual matrix command
and observed stabilization budgets. The test workload (17 operations, two warmup
batches) is not an acceptance default. Design and dependencies are unchanged.

Verification: OpenJDK 25.0.2 benchmark Scry suite passes, 55 tests / 1618
assertions, exit 0 and no failure files. Focused format and lint checks pass.
An initial test delimiter error required clj-paren-repair and a structural
correction to the child association constructor. No acceptance run occurred.
No actor-step or PR metadata was supplied; used the requested task and current
worktree.

## 2026-10-27 — Allocation child CLI

Added `assoc-allocation-child/-main` with six positional arguments and strict
positive decimal budgets. Invalid arguments, associations, and IO failures exit
2; exit zero means partial allocation evidence was written, not acceptance.
Real capture tests now launch this command rather than evaluated forms. Next
connect the bounded parent command and select stabilization budgets. Design and
dependencies are unchanged; no acceptance measurement ran.

OpenJDK 25.0.2 benchmark Scry suite passes: 56 tests / 1638 assertions, exit 0,
no failure artifacts. Focused format, lint, and diff checks pass. No actor-step
or PR metadata was supplied; used the requested task and current worktree.

## 2026-10-27 — Allocation parent command builder

Added `assoc-allocation-command/command` for the next orchestration slice. It
inherits Java home, flags, and classpath and rejects invalid positive-int budgets
before launch. Real capture tests now use this builder rather than constructing
commands with flags removed. Parent orchestration and stabilization budgets remain
open. Design reviewed and unchanged; no dependencies or acceptance runs.

OpenJDK 25.0.2 targeted Scry tests pass: 3 tests / 71 assertions, exit 0,
no failure artifacts. Repository formatting, focused lint, and diff checks pass.
No actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Allocation parent runner

Added `assoc-allocation-run/collect!`: five sequential ordinary children, manifest
and budgets saved before launch, and all returned outcomes checked for success.
Invalid budgets leave no run directory; retries cannot overwrite evidence.
Exceptions still propagate with prior artifacts retained. Next add the manual
CLI and observed stabilization budgets. Exit 0 remains partial evidence only.
Design reviewed and unchanged; no dependencies or acceptance measurements.

OpenJDK 25.0.2 benchmark Scry tests pass: 58 tests / 1673 assertions, exit 0,
no failure artifacts. Focused format/lint and diff checks pass. Initial test
delimiter damage was repaired with clj-paren-repair before verification. No
actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Manual allocation parent CLI

Added `assoc-allocation-cli` with explicit budgets, effective Java invocation,
and basic OS metadata. Exit 0 remains partial evidence only. Documented the
smoke command and resource/failure limits in features.md. Next select budgets
from retained stabilization evidence; full matrix and memory-tool gates remain
open. Design reviewed and unchanged; no dependencies or acceptance runs.

OpenJDK 25.0.2 targeted Scry tests pass: 2 tests / 43 assertions, exit 0,
no failure artifacts. Focused format, lint, and diff checks pass. Corrected an
initial test to apply the success predicate to saved :outcome, not its capture
wrapper. No actor-step or PR metadata was supplied; used the requested task and
current worktree.

## 2026-10-27 — Observed ordinary allocation budget

Ran the committed manual parent on pinned Temurin 21 with 10,000 operations and
20 warmup batches. All five process/runtime gates pass (exit 0). All 20 rows
stabilize after the first warmup batch; final boundary counts are zero. Retained
unmodified raw evidence and the first-batch overhead in evidence/assoc-budget-diagnostic.
Selected these counts as a candidate ordinary allocation budget, not acceptance
or a timing budget. Next integrate it with the full matrix and retain controls.

No source or dependencies changed; design reviewed and unchanged. The real run
is verification for this evidence slice; no unit tests ran. A read-only inspection
expression had a parse error, then corrected inspection succeeded. Diff checks
pass. No actor-step or PR metadata was supplied; used the requested worktree task.

## 2026-10-27 — Allocation-stability summary

Added pure `assoc-allocation-result/stability` for later matrix reporting.
Batch indices are one-based and include the final sample. It rejects inconsistent
raw intervals, divisors, or missing controls instead of reporting stability.
Late changes remain visible; boundary bytes are never subtracted. Design reviewed
and unchanged; no dependencies or acceptance measurements changed.

Temurin 21 benchmark Scry suite passes: 60 tests / 1703 assertions, exit 0,
no failure artifacts. Focused format/lint checks pass. Read-only application to
all 20 retained diagnostic rows confirms 19 trailing warmup matches and a single
transition at batch 2. An initial file-discovery query matched no files; corrected
inspection used process result files and changed no evidence. Next integrate
summaries with full-matrix reporting; memory-tool approval remains open. No
actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Independent boundary stability

`stability` now includes `:boundary-changed-batches` and
`:matching-trailing-boundary-warmup-batches`. Indices include the final sample
and remain one-based. Constant assoc counts do not imply stable boundary costs.
Raw counts remain unchanged; no subtraction or convergence claim is made.
Next connect both series to full-matrix reporting. Design reviewed and unchanged;
no dependencies or acceptance measurements changed.

OpenJDK 25.0.2 targeted Scry tests pass: 4 tests / 104 assertions, exit 0,
empty failure directory. Focused format and lint pass after formatting correction.
No actor-step or PR metadata was supplied; used the requested task and current
worktree. Memory-tool approval remains open.

## 2026-10-27 — Saved allocation-control report

Saved reports now append per-fork allocation and boundary raw series, transitions,
and trailing matches. Supplied malformed controls fail rendering; absent controls
are not assessed. Existing process/runtime/coverage gates remain mandatory. Next
connect the complete measured matrix; partial allocation diagnostics still cannot
pass complete-run reporting. Design reviewed and unchanged; no dependencies or
acceptance measurements changed.

OpenJDK 25.0.2 targeted Scry tests pass: 6 tests / 62 assertions, exit 0, empty
failure directory. Focused format, lint, and diff checks pass. Tests use pure
values and real saved captures without mocks. No actor-step or PR metadata was
supplied; used the requested task and current worktree.

## 2026-10-27 — Partial allocation coverage gate

Transport success alone allowed empty or incomplete allocation diagnostics. The
child now validates all ordinary assoc allocation identities and raw controls
before publication; the parent repeats that check on saved captures before success.
Invalid coverage throws; the CLI maps it to exit 2 without replacing evidence.
General transport still permits partial failure evidence. Design reviewed and
unchanged. Next integrate the complete measured matrix; no memory gate is closed.

OpenJDK 25.0.2 targeted Scry verification: 5 tests, 128 assertions pass, exit 0,
empty failure directory. Focused formatting and lint pass. Tests use real child
results with incomplete/malformed variants, without mocks. No dependencies or
acceptance measurements changed. No actor-step or PR metadata was supplied.

## 2026-10-27 — Declared allocation budget gate

Parent success now requires saved control budgets to match manifest metadata.
The pure check preserves rows and rejects invalid declarations or any mismatched
row. Coverage and raw interval validation remain separate prerequisites. Next
connect the full measured matrix; no memory-tool gate is closed. Design reviewed
and unchanged; no dependencies or acceptance measurements changed.

OpenJDK 25.0.2 targeted Scry tests pass: 6 tests, 135 assertions, exit 0, empty
failure directory. Format checks pass after indentation correction; focused lint
and diff checks pass. No actor-step or PR metadata was supplied; used the requested
task and current worktree.

## 2026-10-27 — Saved allocation diagnostic validation

Added `assoc-allocation-run/read-results` for later reuse of partial diagnostics.
Parent success now uses this same reader. It rejects changed manifest scope,
fork order, associations, process/runtime failures, missing results, and budget
mismatches without writing evidence. Returns raw rows, not acceptance status.
Design reviewed and unchanged; no dependencies changed. Next connect the full
measured matrix; memory-tool approval remains open.

OpenJDK 25.0.2 targeted Scry: 3 tests, 64 assertions pass, exit 0, empty failure
directory. Focused format and lint pass. A test delimiter error was repaired with
clj-paren-repair before verification. Tests use real JVMs and files, without mocks.
No acceptance measurements ran. No actor-step or PR metadata was supplied.

## 2026-10-27 — Saved allocation runtime revalidation

The saved allocation reader now reconciles raw envelope runtime observations
with the manifest, in addition to checking persisted capture status. This closes
a stale-validation gap when saved declarations or raw observations change.
Real-file variants cover absent observations and differences in version, Java
home, and flags on either side; rejection leaves the altered file unchanged.
Design reviewed and unchanged; no dependencies or acceptance measurements changed.
Next: full measured-matrix integration and memory-tool approval remain open.

OpenJDK 25.0.2 targeted Scry: 3 tests, 78 assertions pass, exit 0, empty failure
directory. Focused format, lint, and diff checks pass. No mocks were used.
No actor-step or PR metadata was supplied; used the requested task and current
worktree.

## 2026-10-27 — Complete saved-run runtime revalidation

Complete saved-result reading and rendering now reconcile raw child runtime
observations with manifest declarations, matching the partial allocation reader.
Pure assembly remains unchanged. Real-file tests alter each runtime field or
remove the observation/declaration after successful capture; both readers reject
the change and preserve all bytes. No mocks, dependencies, or design changes.
Next: full measured-matrix integration and memory-tool approval remain open.

OpenJDK 25.0.2 targeted Scry: 5 tests, 85 assertions pass, exit 0, empty failure
directory. Focused formatting, lint, and diff checks pass. No acceptance
measurements ran. No actor-step or PR metadata was supplied; used the requested
task and current worktree.

## 2026-10-27 — Saved manifest schema gate

`evidence/read-run` now rejects absent or unsupported manifest schema versions
before reading child slots. This applies to both saved-result paths. Real-file
tests check rejection and unchanged bytes; existing version-1 reader tests pass.
No design or dependency change. Next: measured-matrix integration and memory-tool
approval remain open. No acceptance measurements ran.

OpenJDK 25.0.2 targeted Scry: 13 tests, 282 assertions pass, exit 0, empty failure
directory. Focused format, lint, and diff checks pass. No actor-step or PR metadata
was supplied; used the requested task and current worktree.

## 2026-10-27 — Saved manifest declaration gate

Saved reads now reuse creation validation for child plans and required metadata,
before reading capture slots. Missing runtime declarations fail at this earlier
gate; valid but different declarations still fail runtime reconciliation. Tests
use real files and verify rejection without writes. Design reviewed and unchanged;
no dependencies or acceptance measurements changed. Next: measured-matrix
integration and memory-tool approval remain open.

OpenJDK 25.0.2 targeted Scry: 14 tests, 315 assertions pass, exit 0, empty failure
directory. The first run exposed two assertions expecting the later runtime
error; updated them for the earlier declaration gate. Focused formatting, lint,
and diff checks pass. No actor-step or PR metadata was supplied; used the
requested task and current worktree.

## 2026-10-27 — Partial allocation diagnostic report

Added `assoc-allocation-run/render-saved`, reusing saved validation and the raw
control table. It does not weaken complete-run coverage or write evidence.
Documented the manual expression in features.md. Next: full measured-matrix
integration and memory-tool approval remain open. Design and dependencies are
unchanged; no acceptance measurement ran.

OpenJDK 25.0.2 targeted Scry: 3 tests, 99 assertions pass, exit 0, empty failure
directory. Focused formatting, lint, and diff checks pass. The initial row-count
assertion used a hyphen instead of the inventory keyword namespace separator;
corrected the test after inspecting structured failure evidence. Tests use real
JVMs and files without mocks. No actor-step or PR metadata was supplied; used the
requested task and current worktree.

## 2026-10-27 — Partial allocation report CLI

Added `assoc-allocation-report` over the validated saved reader. Exit 0 means
report success only; exit 2 rejects arguments, evidence, or file IO errors before
printing a table. Real JVM tests verify exact output and byte preservation,
including a changed manifest budget. No mocks or dependency changes. Design
reviewed and unchanged. Next: full measured-matrix integration and memory-tool
approval remain open; no acceptance measurements ran.

Targeted Scry: 1 test, 20 assertions pass, exit 0; no failure artifacts. Focused
format and lint checks pass. No actor-step or PR metadata was supplied; used the
requested task and current worktree.

## 2026-10-27 — Ordinary assoc timing sites

Added four distinct literal Criterium sites, with fixture setup outside timing
and raw batch adaptation before normalization. Fixed inventory validation stops
unsupported dimensions before measurement. Use a fresh JVM per invocation; the
known repeated-site limitation remains. Next connect these rows to child writer
and durable run capture. Design reviewed and unchanged; no dependencies changed.

Real isolated Scry test on OpenJDK 25.0.2: 1 test, 27 assertions pass, exit 0,
no failure artifacts. Focused format/lint and diff checks pass. Short measurements
warn about insufficient warmup/samples and are not acceptance evidence. An initial
load command retained `:bench` main options and invoked the legacy entry point
with invalid arguments; corrected the command with an empty-main final alias.
No acceptance run occurred. No actor-step or PR metadata was supplied; used the
requested task and current worktree.

## 2026-10-27 — Ordinary timing child persistence

Added `assoc-timing-child/write-results!` to collect once and persist through the
existing validated, exclusive child writer. The real timing smoke test now checks
saved EDN and observed JVM metadata as well as all four raw timing rows. No new
dependencies or design changes. Next: independent child process capture and budget
provenance; the writer alone does not establish full-matrix coverage.

Verification: isolated Scry timing command from features.md passed (1 test,
29 assertions, exit 0). Short measurements are test evidence, not acceptance.
Focused formatting, lint, and `git diff --check` also pass. Scry produced no
failure artifacts.

## 2026-10-27 — Timing child CLI and process capture

Added the timing child CLI with explicit per-site seconds and exit 2 for invalid
input or IO failure. Real child tests inherit JVM flags and check durable rows,
runtime observations, and rejected zero/NaN budgets. Next add parent command
construction and saved budget provenance. Design reviewed and unchanged; no
dependencies or acceptance measurements changed.

The initial 0.1-second child produced no result; its test masked the process
failure with a decode error. Added diagnostic exception data and raised the smoke
budget to one second after a successful rerun. Final isolated Scry: 1 test,
10 assertions pass, exit 0, no failure files. Focused format/lint checks pass.
No actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Timing parent command builder

Added a pure command builder with inherited Java home, flags, and classpath.
Budgets serialize as doubles; ratios work and underflow-to-zero values fail
before launch. No measurement dependencies or design changes. Next retain budget
provenance and connect the parent after resolving the timing harness failure.

OpenJDK 25.0.2 targeted Scry command tests pass: 1 test, 14 assertions, exit 0,
empty failure directory. Focused format/lint checks pass. An attempted real
process integration failed with exit 1: Criterium `collect/batch-size` (line 133)
divided by zero with a one-second budget. Reverted only the test integration;
the existing process test is unchanged, not verified passing. Recorded a concrete
follow-up rather than retry away this failure. No acceptance measurement ran.
No actor-step or PR metadata was supplied; used the requested task and worktree.

## 2026-10-27 — Timing estimator diagnostic

Pinned Criterium `collect.clj:204–211` takes one evaluation without a duration
floor. `collect_plan.clj:124–131` divides by that estimate both in batch sizing
and sample budgeting. Thus site reuse is not necessary for the zero boundary;
a longer seconds budget cannot remove it. Added a deterministic real-tool test
for zero and positive estimates, not a workaround or acceptance measurement.
JAR SHA-256: b2640cac514a15f1af9582345ed0c15bdee45bbeda93a2a7e4651aa96ed7e033.
Next select a supported corrected measurement path and verify original cases;
catching/retrying or fixing only batch-size would not resolve both divisions.

OpenJDK 25.0.2 isolated Scry: 1 test, 4 assertions pass, exit 0, no failure
artifacts. Repository format check, focused lint, and diff check pass. Design
reviewed and unchanged; no dependency changes. No actor-step or PR metadata was
supplied; used the requested task and current worktree.

## 2026-10-27 — Released timing candidate screen

Repository lookup found 0.5.253-ALPHA. Its collect and collect-plan source entries
are byte-identical to the pin; the existing mock-free diagnostic confirms the
zero-estimate exception. Rejected this candidate before any upgrade or original
six-case smoke run. One-shot collection avoids estimation but cannot replace
warmed batch statistics. Next inspect corrected upstream source or a supported
extension, not retries. Design reviewed and unchanged; no dependency changes.

Candidate-only override Scry run: exit 0, 1 test, 4 assertions, no failure
artifacts. Commands, source/JAR hashes, runtime, and output are retained under
`evidence/timing-tool-candidate/`. No acceptance measurements ran. No actor-step,
worktree override, or PR metadata was supplied; used the requested task in the
current worktree.

## 2026-10-27 — Upstream timing extension inspection

Upstream HEAD 4e91647 has byte-identical collect and collect-plan sources to the
pin (both cmp exits 0; SHA-256 matches retained evidence). Warmed-plan options
have no estimator/floor override. Multimethod extension points exist, but no
corrected collector is supplied. Next evaluate an isolated source correction,
not global rebinding, retries, or an unverified upgrade. Full reproduction and
source locations: evidence/timing-tool-candidate/upstream.md.

No production code, dependency, or design change; no tests or measurements ran.
Verification was source comparison and diff checking. No actor-step or PR
metadata was supplied; used the requested task in the current worktree.

## 2026-10-27 — Isolated timing source correction

Retained a local patch and mock-free boundary test under
`evidence/timing-source-correction`. Initial planning now floors duration before
both divisions. Subsequent minimum estimation already had a floor; the patch
shares it through a pure helper. Raw samples are unchanged. This is not an
upstream release or adopted dependency. Next run original six-case smoke and
fresh child capture against the overlay before adoption. Design is unchanged.

OpenJDK 25.0.2 isolated Scry: exit 0, 1 test, 19 assertions, no failure files.
No acceptance measurements ran. No actor-step or PR metadata was supplied; used
the requested task in the current worktree. Memory-tool gates remain open.

## 2026-10-27 — Timing candidate real smoke checks

Added a separate original-six-case smoke test: each literal site runs once per
fresh JVM, checks finite positive statistics and interval ordering, and does not
assert parity. With the isolated overlay, Temurin 21 Scry results pass on first
attempt: original cases 1 test/26 assertions; fresh timing-child capture 1/10.
Structured results and process outputs are in evidence/timing-source-correction.
Focused format/lint and diff checks pass. No dependency or design change.

Next choose reproducible benchmark-only packaging for the local correction and
check reporting regressions before adoption. Smoke evidence is not acceptance;
the process test removes temporary captures. No actor-step/PR metadata was
supplied; used the requested task and current worktree. Memory gates remain open.

## 2026-10-27 — Reproducible timing candidate packaging

Selected an opt-in source overlay, not a full vendored tool or cache mutation.
`dev/bench/timing/prepare.sh` verifies the pinned JAR and patched source hashes,
uses exclusive output creation, and preserves failed build output. Its patch is
byte-identical to the verified candidate. Default aliases and dependencies remain
unchanged. Next verify packaged-path real smoke tests and record the correction
identity in measured-run provenance before adoption. Design reviewed, unchanged.

Temurin 21: packaged boundary Scry test passes (1 test, 19 assertions); ordinary
report/metric-table/metrics regressions pass without Criterium (15 tests, 159
assertions). Both exits 0; no Scry failure artifacts. Real script checks verify
success, existing-output rejection (exit 1), and wrong-JAR rejection (exit 2)
without creating output. Shell syntax, patch equality, and diff checks pass.
No acceptance run or memory-tool approval. No actor-step or PR metadata supplied.

## 2026-10-27 — Packaged timing smoke verification

The actual prepare.sh output passes both fresh-JVM smoke suites on Temurin 21:
original six cases 1 test/26 assertions; independent timing child 1/10. All three
process exits (preparation and suites) are zero; no retries. Retained evidence
includes collector resource URLs, which resolve to the packaged overlay.

Full Scry assertion data contains printed #object values; strict EDN inspection
rejected those tags. Plain EDN summaries were extracted using tagged-literal
fallback, without rerunning tests. See evidence/timing-packaged-smoke for commands,
raw results, and limits. Next add correction identity to measured-run provenance
before default adoption. Design reviewed and unchanged; no code, dependencies,
or acceptance measurements changed. Diff checks pass. No actor-step or PR metadata
was supplied; used the requested task in the current worktree.

## 2026-10-27 — Observed timing collector provenance

Ordinary timing child rows now retain source-resource identity before measurement:
upstream versus local correction, source hash, resource URL, and correction/patch
identity. Unknown bytes reject collection. This does not verify all dependencies
or runtime var replacements. Next reconcile parent tool declarations and cover
legacy S-03 provenance before default adoption. No dependencies or design changed.

Temurin 21: packaged overlay identity plus real child capture pass (2 tests,
18 assertions); unmodified dependency identity passes (1 test, 6 assertions).
Both Scry exits are zero with no failure files. Focused format, lint, and diff
checks pass. No acceptance run or memory-tool approval. No actor-step or PR
metadata supplied; used the requested task in the current worktree.

## 2026-10-27 — Portable timing identity validation

Added pure declaration and observation checks without a measurement dependency.
Known hashes must agree with library, base version, distribution, and correction
metadata. Resource URLs are preserved but excluded from equality because child
paths can differ. Next connect these checks to parent manifests and saved reads;
this slice does not establish that integration or default correction adoption.
Design reviewed and unchanged. No actor-step or PR metadata was supplied.

Focused Scry: 2 tests, 22 assertions pass; exit 0, no failure files. Focused
format/lint and diff checks pass. Test discovery initially exposed a nested test
form; structural extraction corrected it and both tests now execute. No acceptance
run, dependency change, or memory-tool approval.

## 2026-10-27 — Timing manifest declaration gate

Optional `:timing-tool` metadata now uses portable identity validation on creation
and saved reads. Explicit nil is invalid; absent metadata remains valid for
allocation-only runs. Next require declarations at timing-specific boundaries
and compare saved child observations. No dependency or design change; no correction
adoption or acceptance measurement. No actor-step or PR metadata was supplied;
used the requested task in the current worktree.

Scry evidence tests pass: 7 tests, 165 assertions, exit 0, no failure files.
Repository formatting, focused lint, and diff checks pass. Real-file tests verify
rejection before creation/capture loading and unchanged saved bytes, without mocks.

## 2026-10-27 — Saved timing provenance gate

Complete saved readers reconcile every measured time row with manifest
`:timing-tool` metadata after process/runtime/coverage checks. Resource paths
remain portable; missing, unknown, and different identities fail without writes.
Capture still preserves invalid provenance for diagnosis. Parent prelaunch and
partial timing integration remain next. Design reviewed and unchanged. No
actor-step or PR metadata was supplied; used the requested task/current worktree.

Focused Scry on the default JDK 25.0.2: 15 tests, 286 assertions pass, no failures.
Focused format/lint and diff checks pass. The initial new test incorrectly tried
to aggregate a one-fork fixture; removed that assertion (the existing two-fork
report test checks successful rendering). No measurement or dependency change.

## 2026-10-27 — Timing child budget provenance

Each ordinary timing row now records the canonical double per-site limit under
`:timing-budget`. The child validates it before collection; pure reconciliation
rejects missing or changed limits without changing raw samples. Next connect
parent metadata and partial saved reads. No dependency or design change, default
correction adoption, or acceptance run. No actor-step/PR metadata was supplied;
used the requested task and current worktree.

Mock-free Scry: pure budget test passes (1 test/22 assertions); fresh-JVM capture
with the packaged overlay on Temurin 21 passes (1/12). Both final exits are zero,
with no failure artifacts. Focused lint and diff checks pass. Initial test
syntax damage required delimiter repair and a binding-vector correction;
formatting was then corrected. Short timing is smoke evidence only.

## 2026-10-27 — Partial timing result contract

Added pure `assoc-timing-result/validate-results` for the next saved-reader slice.
It returns unchanged raw rows only after complete ordinary assoc timing coverage
and tool/budget reconciliation. Missing declarations fail even for empty input.
Next connect saved captures and parent success; no measured call site changed.
Design reviewed and unchanged; no dependency or acceptance measurement changes.
No actor-step or PR metadata was supplied; used the requested task/current worktree.

Mock-free targeted Scry: 1 test, 11 assertions pass, exit 0; no failure files.
Repository formatting, focused lint, and diff checks pass. Tests cover EDN round
trips, missing/duplicate rows, invalid counts/identity, unavailable results, and
missing or changed provenance without loading Criterium.

## 2026-10-27 — Saved partial timing reader

Added `assoc-timing-run/read-results`. Manifest measurement metadata uses
`:scope :ordinary-assoc-timing`, `:acceptance? false`, and `:limit-time-s`;
portable tool identity remains at metadata `:timing-tool`. Five ordered ordinary
forks are required. Raw observations are revalidated, not inferred from saved
capture status. Next connect parent orchestration and prelaunch declarations.
Design reviewed and unchanged; no dependencies, adoption, or acceptance run.
No actor-step or PR metadata was supplied; used the requested task/current worktree.

Mock-free real-file tests and pure result tests pass on OpenJDK 25.0.2:
2 tests, 43 assertions, exit 0, no Scry failure files. Changed scope, fork plans,
budgets, tool identity, coverage, runtime, association, and process status reject
without writes. Numeric fixtures are not measurements. Focused lint and format
checks pass after formatting the test.

## 2026-10-27 — Partial timing parent

Added `assoc-timing-run/collect!` with explicit parent tool declaration, canonical
per-site budget, manifest-first persistence, five sequential children, and saved
validation before success. It reuses the existing command builder. Exceptions
retain evidence; returned process failures do not trigger retries. No dependency
or design change. Next expose the manual CLI; default correction adoption and
legacy S-03 provenance remain separate work.

Verification on Temurin 21: ordinary reader/prelaunch Scry tests pass (2 tests,
40 assertions); isolated packaged-overlay parent test passes on its first run
(1 test, 9 assertions, five real children, one-second sites). Retry rejection
preserves saved rows. Tests use no mocks and make no timing threshold assertions.
The ordinary test initially had a malformed doseq binding; corrected it before
the passing run. Targeted clj-kondo: zero warnings/errors. Scry failure directory
is empty. This smoke is not acceptance evidence.

## 2026-10-27 — Manual timing parent CLI

Added `assoc-timing-cli` with observed tool identity, effective Java command,
basic machine metadata, and explicit per-site budget. The documented opt-in
overlay command retains five forks; exit zero means partial diagnostic success.
Next: legacy S-03 provenance before default adoption and separate acceptance
budgets. Design reviewed and unchanged; no dependencies or acceptance run.
No actor-step or PR metadata was supplied; used the requested task/worktree.

Temurin 21 packaged-overlay Scry test passes: 1 test, 15 assertions, exit 0,
empty failure directory. Real JVM checks cover success, bad/zero/NaN budgets,
argument errors, provenance, and unchanged evidence after retry rejection.
Focused lint and diff checks pass. Repository formatting passes after correcting
existing indentation in this task's timing-run prelaunch test. No mocks used.

## 2026-10-27 — Original timing transport contract

Added `legacy-transport/encode` and `decode` with schema version 1, tool identity,
and unchanged original statistics. Decode returns error values for rejected
provenance. Legacy commands are not yet connected; next wire child publication
and parent reconciliation without changing S-03 decisions. Design reviewed and
unchanged; no dependencies or acceptance measurements changed. No actor-step or
PR metadata was supplied; used the requested task in the current worktree.

Mock-free pure transport and report tests pass: 8 tests, 48 assertions, exit 0,
empty Scry failure directory. Focused lint and repository formatting pass.
An initial test delimiter error was repaired with clj-paren-repair.

## 2026-10-27 — Original command transport integration

Original child publication now wraps unchanged statistics with observed collector
identity. `fork/parent` requires a second argument, the declared tool identity,
and rejects old bare-vector or mismatched results before aggregation. The CLI
supplies its observed identity. No dependency or design change. Next verify the
actual original measured child with the packaged overlay before adoption; this
pass ran no measurements. No actor-step or PR metadata was supplied; used the
requested task and current worktree.

Mock-free transport, shell-process, and original report Scry tests pass: 15 tests,
88 assertions, exit 0, no failure files. Focused formatting and lint pass after
format correction. S-03 pass/fail exits remain covered. Full matrix and memory
tool gates remain open.

## 2026-10-27 — Original measured-child provenance

Ran the real original child CLI once with the checksum-built overlay on Temurin
21 (fork 0, existing five-second sites). Exit 0; all six saved rows reconcile
with independently observed local-correction identity and have finite positive,
ordered intervals. Raw EDN, streams, exits, revision, and reproduction are in
evidence/original-child-provenance. No retries, code or dependency changes.

Transport/fork/report Scry regressions pass: 15 tests, 88 assertions, exit 0,
empty failure directory. Diff checks pass. Design reviewed and unchanged.
Next implement reproducible default adoption; this one-fork smoke does not
establish parity, acceptance budgets, full-matrix coverage, or memory-tool approval.
No actor-step or PR metadata was supplied; used the requested task/current worktree.

## 2026-10-27 — Default corrected timing launcher

Adopted the verified local correction through `timing/run.sh`. Each invocation
resolves the pinned benchmark dependency and prepares a fresh checksum-verified
overlay before JVM launch. Keep overlays under target until children finish;
resource URLs in evidence refer to them. The launcher forwards Clojure arguments
and exit codes. The documented default is now this launcher, not the uncorrected
`:bench` alias. No dependency or design change; no acceptance measurements ran.
No actor-step or PR metadata was supplied; used the requested task/current worktree.

Real-process launcher tests on Temurin 21 pass (1 test, 4 assertions, exit 0,
empty Scry failure directory): corrected resource selection, collector loading,
and exit-code propagation. Focused format/lint, shell syntax, and diff checks
pass. Existing measured-overlay smoke evidence supports this adoption; this pass
adds launcher verification without repeating measurements. Next fix acceptance
budgets and integrate the full matrix; memory-tool approval remains open.

## 2026-10-27 — Saved partial timing report

`assoc-timing-run/render-saved` validates saved diagnostics before rendering a
timing-only inventory projection through the existing metric table. Full-matrix
coverage remains mandatory in the complete report path. Reports include equal-fork
mean, range, and sample SD, with explicit non-acceptance and non-parity limits.
Design and dependencies are unchanged. No actor-step or PR metadata was supplied;
used the requested task and current worktree.

Temurin 21 targeted Scry passes (2 tests, 60 assertions), exit 0, no failure files.
Real-file tests check varying fork values, normalized statistics, repeatability,
read-only preservation, and rejection of every existing invalid-evidence variant.
Focused formatting and lint pass. Next expose the report CLI; full measured
matrix, acceptance budgets, and memory-tool approval remain open. No measurements
ran in this pass.

## 2026-10-27 — Partial timing report CLI

Added `assoc-timing-report` over the validated saved renderer. Exit 0 means report
success only; exit 2 rejects arguments, evidence, or file IO errors before table
output. Real JVM tests use numerical saved fixtures, not measurements, and verify
exact output and byte preservation, including a changed declared budget. No mocks,
dependencies, or design changes. Next: full measured-matrix integration and
acceptance budgets; memory-tool approval remains open. No actor-step or PR
metadata was supplied; used the requested task and current worktree.

OpenJDK 25.0.2 targeted Scry passes: 3 tests, 80 assertions, exit 0, empty failure
directory. Repository formatting, focused lint, and diff checks pass. No timing
or acceptance measurements ran.

## 2026-10-27 — Complete saved timing budgets

Complete saved readers now require measured time rows to match the manifest's
`:measurement {:limit-time-s ...}` declaration. This closes the difference from
the partial timing reader; pure assembly remains a coverage/process boundary.
Unavailable time outcomes still need no budget. No design or dependency change.
Real-file mutation tests cover missing, changed, and zero budgets on both sides,
including unchanged evidence after rejection. Targeted Scry: 7 tests, 123
assertions pass; exit 0. Full matrix integration and acceptance budgets remain
open. No measurements ran.

## 2026-10-27 — Complete saved allocation budgets

Complete saved reads now validate supplied allocation controls and compare their
operation/warmup counts with `:metadata :measurement`, using the existing partial
allocation checks. Rendering uses the same gate. Absent controls remain allowed
and do not imply stability. No design, schema, or dependency change. No actor-step
or PR metadata was supplied; used the requested task in the current worktree.

Real-file tests cover matching, missing, changed, and invalid declarations,
malformed controls, absent controls, unavailable outcomes, and unchanged files.
An initial one-fork report fixture failed the existing two-fork aggregation rule;
the fixture now supplies two forks. Final targeted Scry: 8 tests, 147 assertions,
exit 0, no failures. Focused formatting, lint, and diff checks pass. Next carry
both allocation budgets into complete orchestration. Memory-tool approval and
acceptance remain open; no measurements ran.

## 2026-10-27 — Ordinary assoc graph rows

Added an agent-only collector that connects the verified bounded physical-field
walker to raw inventory graph outcomes. Result, source/result union, and union
minus source remain distinct; unknown reachable types fail. No dependency or
design change. No actor-step or PR data was supplied; used the requested task
in the current worktree. Next persist graph rows in independent instrumented
children and record agent provenance; do not put this collector on timing paths.

Temurin 21 existing record graph controls pass. Real-agent Scry regression passes
1 test, 29 assertions, exit 0, no failure files. Both widths and representations
match independent shallow-layout sums; invalid associations and unsupported
payloads reject. Focused formatting and lint pass. No acceptance measurements ran;
experimental value-box accounting and full memory-tool approval remain open.

## 2026-10-27 — Ordinary graph child persistence

Added `assoc-graph-child/write-results!` for both widths and alternating fork
orders. It validates association before collection and preserves raw graph rows
with observed runtime metadata through the exclusive child writer. Next add
independent process capture and agent identity; do not load this path for timing.
Design reviewed and unchanged; no dependencies or acceptance measurements changed.
No actor-step or PR metadata was supplied; used the requested task/worktree.

Real-agent Temurin 21 Scry passes: 2 tests, 46 assertions, exit 0, no failure
files. Format and focused lint pass. Initial runtime selection through macOS
java_home resolved Java 8 and failed agent loading before tests; explicit pinned
Temurin 21 corrected setup. Tests use real files and instrumentation, without mocks.

## 2026-10-27 — Independent graph child command

Added a four-argument graph child CLI. Real instrumented process capture retains
all 12 raw rows and reconciles inherited runtime flags. Invalid fork syntax,
range, and integer overflow exit 2 without publication. Agent identity remains
unverified; next establish source/binary provenance before parent orchestration.
Design reviewed and unchanged; no dependency or acceptance measurement changes.
No actor-step or PR metadata was supplied; used the requested task/current worktree.

Temurin 21 real-agent Scry: 2 tests, 37 assertions pass, exit 0, no failure files.
Repository formatting, focused lint, and diff checks pass. No mocks used.

## 2026-10-27 — Graph parent command builder

Added `assoc-graph-command/command` without agent dependencies. It validates
ordinary associations and inherits the current JVM invocation. The real child
capture test now uses it. Next verify agent identity before parent orchestration;
flags and successful capture alone are not provenance. Design reviewed and
unchanged; no dependencies or acceptance measurements changed. No actor-step or
PR metadata was supplied; used the requested task and current worktree.

Ordinary Scry command test: 1 test, 5 assertions pass. Temurin 21 real graph
controls and child Scry tests pass: 2 tests, 37 assertions, exit 0, no failure
files. Focused formatting, lint, and diff checks pass. No mocks used.

## 2026-10-27 — Isolated graph agent build

Added explicit preparation with a pinned source checksum, Java 21 target, fixed
archive timestamps, and retained compiler/source/class/JAR checksums. Existing
or failed output directories cannot be replaced. Next observe loaded identity
in children; build checksums alone do not prove runtime selection. Design reviewed
and unchanged; no dependency or acceptance measurement changes. No actor-step or
PR metadata was supplied; used the requested task/current worktree.

Real shell tests on Temurin 21.0.10 pass (exit 0): two identical builds, checksum
verification, overwrite and changed-source rejection, and JVM agent loading.
Shell syntax and diff checks pass. No Clojure code changed; no Scry tests ran.

## 2026-10-27 — Active graph-agent identity

Added Java identity access for the next child-provenance slice. It requires an
active agent and a JAR origin, returning JAR/class resource SHA-256 and location.
Preparation pins the updated source. These hashes identify resource bytes, not
runtime class transformations. Next attach observations to child rows and compare
with declared build hashes before parent success. Design reviewed and unchanged;
no dependencies or acceptance measurements changed. No actor-step or PR metadata
was supplied; used the requested task/current worktree.

Real build/identity shell tests pass on the available Valhalla JDK 27, compiling
with --release 21: repeatable builds, checksum validation, overwrite/changed-source
rejection, agent loading, inactive-agent rejection, and exact runtime/build hash
agreement. This is not ordinary Temurin 21 acceptance evidence. Shell syntax and
diff checks pass. No Clojure code changed or Scry tests ran.

## 2026-10-27 — Graph child identity persistence

Each graph child row now carries `:graph-tool` with active agent JAR/class hashes
and location, observed before collection. Child commands must use the built JAR
on the classpath; loose classes fail identity checks. Next reconcile declared
build hashes in saved reads and parent success. Resource hashes do not detect
class transformation. Design reviewed and unchanged; no dependencies or acceptance
measurements changed. No actor-step or PR metadata was supplied.

Real-agent Scry on OpenJDK 25.0.2 passes: 2 tests, 40 assertions, exit 0, no
failure files. Tests verify saved EDN and independent child identity without
mocks. Repository formatting, focused lint, and diff checks pass. This is not
pinned Temurin 21 acceptance evidence.

## 2026-10-27 — Portable graph identity validation

Added pure graph-tool declaration and observation checks without agent loading.
Both JAR and class hashes must match; locations remain preserved but excluded
from equality. Hash syntax is not build approval: next obtain declarations from
verified build output and wire manifest/saved-reader gates. Resource identity
still cannot detect runtime class transformation. Design reviewed and unchanged;
no dependencies or acceptance measurements changed. No actor-step or PR metadata
was supplied; used the requested task and current worktree.

OpenJDK 25.0.2 targeted Scry passes: 2 tests, 69 assertions, exit 0, no failure
files. Mock-free tests use values and EDN round trips. Focused lint, repository
formatting, and diff checks pass after formatting the new test.

## 2026-10-27 — Graph manifest declaration gate

Optional `:graph-tool` metadata now uses portable identity validation before run
creation and saved capture loading. Explicit nil fails; absence remains valid for
non-graph runs. Next require verified build declarations at graph boundaries and
reconcile saved observations. Design reviewed and unchanged; no dependencies or
acceptance measurements changed. No actor-step or PR metadata was supplied; used
the requested task in the current worktree.

Mock-free real-file Scry tests pass: 8 tests, 178 assertions, exit 0, empty failure
directory. Repository formatting, focused lint, and diff checks pass. Invalid
saved declarations leave file bytes unchanged; invalid creation leaves no directory.

## 2026-10-27 — Complete saved graph provenance

Complete saved reads and reports reconcile every measured graph row with manifest
JAR/class hashes. Raw capture still retains invalid provenance; non-measured
outcomes need no identity. Next connect verified build output and partial graph
parent validation. Design reviewed and unchanged; no dependencies or acceptance
measurements changed. No actor-step or PR metadata was supplied; used task 019 in
the current worktree.

Mock-free real-file Scry verification on OpenJDK 25.0.2 passes: 9 tests, 171
assertions, exit 0, no failure files. Formatting and focused lint pass. Test
construction errors were corrected before final verification (delimiter repair
and the report's required two-fork inventory).

## 2026-10-27 — Partial graph result validation

`assoc-graph-result/validate-results` returns unchanged raw rows only after full
ordinary assoc graph coverage and agent hash reconciliation. It also derives
all roots from shared source/result/union counts, rejecting inconsistent evidence
across roots. Next connect verified build declarations and partial saved reads;
this pure gate does not approve memory tooling. Design reviewed and unchanged;
no dependency changes or acceptance measurements. No actor-step or PR metadata
was supplied; used task 019 in the current worktree.

Mock-free value/EDN tests and graph helper regressions pass on OpenJDK 25.0.2:
4 tests, 155 assertions, Scry exit 0, no failure files. Focused format/lint and
diff checks pass. Initial test delimiter damage was repaired with the repair
tool; an incorrect regression namespace selector was corrected before verification.

## 2026-10-27 — Saved partial graph reader

Added `assoc-graph-run/read-results` with five-fork diagnostic scope, process,
association, raw runtime, coverage, count-consistency, and agent-hash checks.
Returns unchanged raw rows without agent loading or writes. Next obtain parent
declarations from verified preparation output and connect parent success.
Design reviewed and unchanged; no dependency or acceptance measurement changes.
No actor-step or PR metadata was supplied; used task 019 in the current worktree.

Mock-free real-file Scry passes: 1 test, 32 assertions, exit 0, no failure files.
Changed scope, fork order/count, declarations, raw rows, runtime, associations,
and process status reject without changing files. Repository formatting, focused
lint, and diff checks pass. Numeric fixtures are not measurements; full matrix
and memory-tool gates remain open.

## 2026-10-27 — Prepared graph build declaration

Added `graph-build/read-declaration` without agent loading. It checks the exact
checksum inventory, pinned source, four artifacts, JAR premain entry, and packaged
class equality. This is consistency verification, not compiler trust or proof
of source-to-binary derivation. Next wire it into graph parent metadata before
launch and use the existing saved reader before success. Design reviewed and
unchanged; no dependencies or acceptance measurements changed. No actor-step or
PR metadata was supplied; used task 019 in the current worktree.

Real compiler/filesystem tests are isolated in test-bench. OpenJDK 25.0.2 Scry
passes: 3 tests, 82 assertions, exit 0, no failure files. Changed artifacts reject
without writes; repeat reads agree. Initial test delimiter damage was repaired
with clj-paren-repair. Repository formatting, focused lint, and diff checks pass.

## 2026-10-27 — Partial graph parent

Added `assoc-graph-run/collect!` with verified preparation input, manifest-first
persistence, five sequential children, and saved validation before success.
It inherits the parent agent/classpath rather than installing an agent. A different
loaded build cannot pass saved hash reconciliation. Next expose the manual CLI;
full-matrix integration and memory-tool approval remain open. Design reviewed and
unchanged; no dependencies or acceptance measurements changed. No actor-step or
PR metadata was supplied; used task 019 in the current worktree.

Mock-free real-agent Scry on OpenJDK 25.0.2 passes: 2 tests, 43 assertions,
exit 0, no failure files. Tests cover five child JVMs, alternating order, verified
manifest identity, missing-build rejection before creation, and retry preservation.
Repository formatting, focused lint, and diff checks pass. This is integration
smoke evidence, not pinned Temurin 21 acceptance evidence.

## 2026-10-27 — Manual graph parent CLI

Added `assoc-graph-cli` with explicit prepared-build input, effective command,
and machine metadata. Five child captures remain available after inactive-agent
failure; exit 0 means partial evidence only. Next integrate full-matrix collection;
memory-tool approval remains open. Design reviewed and unchanged; no dependencies
or acceptance measurements changed. No actor-step or PR metadata was supplied;
used task 019 in the current worktree.

Mock-free real-process Scry on OpenJDK 25.0.2 passes: 1 test, 14 assertions,
exit 0, no failure files. Tests cover success, invalid arguments, missing build,
inactive agent, saved provenance, and retry preservation. Focused formatting,
lint, and diff checks pass. This is not pinned Temurin 21 acceptance evidence.

## 2026-10-27 — Saved partial graph report

Added `assoc-graph-run/render-saved` over the validated saved reader and existing
metric table. The graph-only projection does not weaken complete-run coverage.
Next expose the report CLI; full-matrix and memory-tool gates remain open.
Design reviewed and unchanged; no dependencies or acceptance measurements changed.
No actor-step or PR metadata was supplied; used task 019 in the current worktree.

Mock-free real-file Scry: 1 test, 52 assertions pass, exit 0, no failure files.
Focused lint and formatting pass after indentation correction. Initial assertions
used incorrect root names; structured failure data identified the correct inventory
names. Tests cover all roots, repeatability, rejection, and unchanged saved bytes.

## 2026-10-27 — Partial graph report CLI

Added `assoc-graph-report` over the validated saved renderer, without agent
loading. Exit 0 means report success only; exit 2 rejects arguments, evidence,
or file IO errors before table output. Next: full-matrix integration and separate
memory-tool approval. Design reviewed and unchanged; no dependencies or acceptance
measurements changed. No actor-step or PR metadata was supplied; used task 019
in the current worktree.

Mock-free real-JVM/file Scry tests pass on OpenJDK 25.0.2: 2 tests, 72 assertions,
exit 0, empty failure directory. Numerical fixtures test exact output, changed
agent declarations, argument/IO failures, and byte preservation; they are not
measurements. Focused formatting, lint, and diff checks pass.

## 2026-10-27 — Ordinary diagnostic compatibility

Added `assoc-suite/validate-manifests` as the first composition boundary. It
expects already validated manifests; it does not replace saved-reader checks.
Require equal inventory, revision, machine declarations, and Java home/build,
but preserve separate run IDs and metric-specific flags. Graph instrumentation
must not enter timing. Next connect all three saved readers and retain manifests
rather than flattening away provenance. Design reviewed and unchanged; no
measurement, dependency, or acceptance change. No actor-step/PR metadata supplied.

Mock-free pure Scry test: 1 test, 16 assertions pass on the current JVM; exit 0.
Focused formatting and clj-kondo pass. This is compatibility verification only.

## 2026-10-27 — Saved ordinary diagnostic composition

Connected all three saved readers through `assoc-suite/read-results`. Sources
remain metric-keyed maps of manifest and raw results; no provenance is flattened.
Read completed evidence only, without concurrent changes. Next render combined
ordinary diagnostics; full matrix and acceptance gates remain open. Design
reviewed and unchanged; no dependencies or measurements changed. No actor-step
or PR metadata was supplied; used task 019 in the current worktree.

Mock-free real-file Scry: 2 tests, 33 assertions pass, exit 0, empty failure
directory. Tests cover source preservation, missing/extra members, failed captures
in each metric, incompatible machine declarations, and unchanged bytes. Initial
test delimiter damage was repaired with clj-paren-repair. Focused formatting,
lint, and diff checks pass.

## 2026-10-27 — Combined ordinary diagnostic report

Added `assoc-suite/render-saved` with a combined metric table, allocation controls,
and labelled source manifests. Validation precedes rendering; source run IDs,
flags, budgets, and tool identities remain available. Next expose the manual
report CLI. Design reviewed and unchanged; no dependencies or measurements changed.
No actor-step or PR metadata was supplied; used task 019 in the current worktree.

Mock-free real-file Scry: 2 tests, 47 assertions pass, exit 0, no failure files.
Focused formatting and lint pass. An initial test expected `B/op`; corrected it
to the existing `bytes/op` unit after inspecting structured failure data.
Tests check repeatability, provenance, invalid-source rejection, and unchanged
files. Full-matrix and memory-tool gates remain open.

## 2026-10-27 — Combined ordinary report command

Added `assoc-suite-report` with ordered timing/allocation/graph directory arguments.
Validation completes before stdout output. Exit 2 covers argument, evidence, and
file IO errors; exit 0 is report success only. No dependency or design change.
Real JVM tests cover success, paths with spaces, argument counts, missing/swapped
sources, incompatible machine metadata, and unchanged saved files. Scry on local
OpenJDK 25.0.2: 3 tests, 75 assertions pass; no failure artifacts. These are report
fixtures, not measured acceptance evidence. Complete matrix integration remains.

Targeted formatting and clj-kondo checks pass; `git diff --check` passes.
The full `bb clj-kondo:lint` exits 2 on an existing duplicate `clojure.edn`
require in `dev/acceptance/api_content_defect.clj`. That unrelated acceptance
fixture was not changed. The affected two Clojure files have no lint findings.

## 2026-10-27 — Matrix budget accounting

Added pure `matrix-budget/summarise`: 17 cases, five forks, 490 required outcome
rows, including 170 timing sites. An explicit five-second limit sums to 850
seconds, not wall-clock duration. Startup, warmup, analysis, and memory work need
separate duration evidence before acceptance budget approval. No dependencies,
measurements, or design scope changed. No actor-step or PR metadata was supplied;
used task 019 in the current worktree.

Mock-free Scry on OpenJDK 25.0.2 passes (2 tests, 19 assertions), exit 0, empty
failure directory. Counts agree with the independent complete-coverage validator;
invalid inventories and budgets reject. Focused formatting, lint, and diff checks
pass. Next connect full-matrix orchestration and obtain actual duration evidence;
experimental collectors and memory-tool approval remain open.

## 2026-10-28 — Completion-focused plan

User approved a change from repeated framework slices to end-to-end delivery.
Replaced the accumulated plan and helper-level checklist with four milestones:
ordinary results, experimental results, acceptance evidence, and the linked
performance page with final checks. Prior completion evidence remains here and
in Git; no code, probes, tests, or collected results were removed.

The design now explicitly permits an evidenced unavailable metric at completion,
not an unimplemented collector or failed process. Physical value-box accounting
must not block unaffected metrics. Separate existing metric commands plus a
bounded evidence index can satisfy acceptance; a universal runner is unnecessary.
Framework work is limited to demonstrated acceptance defects. Each pass should
finish an end-to-end milestone rather than stop after one helper or CLI.

Updated feature and measurement guidance and the task index to agree. No runtime
or benchmark work ran during this planning change. Verification: git diff --check
and local Markdown link-target checks. The workflow definition is unchanged;
these execution constraints are task-specific.

## 2026-10-28 — Ordinary end-to-end attempt 01

Ran existing timing/allocation/graph commands on pinned Temurin 21, five forks
each, and saved the combined report. All exits zero; elapsed seconds 40/4/4
(agent preparation 1). Allocation is stable at 40/96 bytes per operation;
graph shared-root accounting agrees across forks. The timing audit found
0.029–0.064 ns/op, so milestone 1 remains open despite successful validation.
Cause is not established. Next inspect actual expression/loop and raw batch
statistics, add a cause-specific regression, then collect under a new identity.
Preserve this first attempt. See evidence/ordinary-end-to-end/assessment.md.
Design reviewed and unchanged; no code or dependency changes. No actor-step or
PR override supplied. This is not acceptance or a human-action blocker.

Verification: real-file/JVM suite report Scry tests pass, 3 tests/75 assertions,
exit 0, no failure files. Initial selector used a nonexistent namespace; corrected
without changing tests. Shell syntax and git diff checks pass. No mocks used.

Staged evidence check reports one trailing blank line in raw `report.stdout`.
Preserved the exact command output rather than edit evidence. The earlier diff
check covered tracked edits only; shell syntax and Scry results above still pass.

## 2026-10-28 — Ordinary milestone completed

Fixed the demonstrated timing defect: syntax-quoted assoc was a Cons that pinned
Criterium factored to nil. Explicit list construction preserves the operation.
Real generated-expression regression checks arguments and changed results, not
speed. Committed harness before attempt 02; all five-fork metric commands and
combined report pass on pinned Temurin 21. See evidence/ordinary-end-to-end-02
for bounded raw evidence, durations, budgets, and assessment. Attempt 01 remains.

Next implement experimental end-to-end coverage, not more ordinary framework.
Width-16 variation and slower closed-record mean must remain in later reporting.
Design reviewed and unchanged; no dependencies or library semantics changed.
No actor-step, worktree override, or PR metadata supplied. Verification: real
Criterium Scry 2 tests/33 assertions; ordinary regressions 4/91, all pass with
empty failure directory. Focused format/lint pass. Raw report has its original
trailing blank line; preserve exact evidence rather than rewrite output.

## 2026-10-28 — Experimental store boundary check

Pinned Valhalla fixtures pass. Real Criterium invocation demonstrates that a
plain batched store cannot meet the changed-value contract: operation two writes
23 over 23. Added a diagnostic for both layouts/lengths; no production collector
or dependency changed. Criterium returns the first expression value, not the last;
the initial test mistake and final evidence are retained in
`evidence/experimental-store-boundary`. Final Scry: 1 test/16 assertions pass,
exit 0, empty failure directory. Focused format/lint pass; no mocks used.

This pass ends before the experimental milestone. Next verify a custom Measured
reset/timer boundary, then connect all experimental operations, allocation and
graph outcomes, five-fork capture, and reporting. This is unfinished integration,
not an unavailable metric or human-action blocker. Design remains unchanged.
No actor-step path override or PR metadata was supplied.

## 2026-10-28 — Reset-aware timing boundary

Added a custom Measured wrapper that resets before each single-operation timed
invocation and sums raw durations. It retains the first result and warmup argument
source. Timer/opaque-input costs remain included; wrapper/reset allocations are
outside timing, so this must not supply allocation measurements. Real generated
expressions enforce changed stores on both layouts and lengths for batches of
1, 2, and 100, with normal and warmup state. No mocks or dependencies changed.

Pinned Valhalla Scry: 2 tests, 104 assertions pass, exit 0, no failure files.
Focused format/lint and diff checks pass. Design reviewed and unchanged. No
actor-step override or PR metadata supplied. This pass ends before the milestone:
next wire experimental operations, separate allocation and graph outcomes,
five-fork capture, and saved reporting. No acceptance measurements ran; this is
unfinished integration, not a support limit or human-action blocker.

## 2026-10-28 — Reset boundary collection smoke

Real Criterium estimation, warmup, collection, and bootstrap analysis now pass
with the reset-aware checked store on both layouts and lengths. One-second
sites on pinned Valhalla with the verified overlay: Scry 1 test/16 assertions,
exit 0, no failure files. Focused format/lint and diff checks pass. No mocks,
dependency changes, acceptance results, or design changes.

This pass ends before the experimental milestone. Next connect unchecked
acceptance expressions for all experimental operations, separate allocation
and graph outcomes, five-fork capture, and reporting. The diagnostic checks
are deliberately not acceptance timings. No actor-step override or PR metadata
was supplied; used task 019 in the current worktree. No human blocker.

## 2026-10-28 — Unchecked experimental operations

Added Java operations with Object transport for prebuilt value payloads, scalar
reads, array-returning stores, and fresh holders. Construction fills all elements;
replacement uses the same holder constructor with the replacement payload.
Real generated Criterium expressions return expected results, including stores
through the reset-aware boundary. No diagnostic precondition runs in these
operations. No library, dependency, or design changes.

Pinned Valhalla fixtures pass. Mock-free Scry: 4 tests, 76 assertions pass,
exit 0, empty failure directory. Focused clj-kondo and diff checks pass. Initial
test delimiter damage was fixed with clj-paren-repair. No acceptance measurement.
This pass ends before the milestone: collection of all experimental timing sites,
separate allocation/graph outcomes, five-fork persistence, and reports remain.
No actor-step override or PR metadata supplied; used task 019/current worktree.

## 2026-10-28 — Experimental timing collector smoke

Connected all nine experimental inventory pairs to real Criterium collection in
`valhalla.timing`. Expressions use prebuilt opaque payloads; stores use the
per-operation reset boundary and retain timer costs. Pair order alternates by
fork. Exact collector expressions return expected scalar/container values under
repeated invocation. No library or dependency changes; design remains unchanged.

Pinned Valhalla with the verified timing overlay: mock-free Scry 2 tests,
146 assertions pass, exit 0, no failure files. All 18 timing rows pass real
estimation/warmup/collection/analysis with a one-second smoke limit. Focused
format/lint and diff checks pass. Initial delimiter damage was repaired with
clj-paren-repair before verification. These are smoke results, not acceptance.

The experimental milestone remains incomplete: persist these rows with tool and
budget metadata through child capture, collect separate allocation and supported
graph outcomes, retain five forks, and render saved reports. No human blocker;
no actor-step override or PR metadata was supplied. Existing feature commands
compile the Java operations; add the verified overlay path and select
`valhalla.timing-test` to reproduce this isolated smoke.

## 2026-10-28 — Experimental timing child publication

Connected experimental timing to the existing exclusive child writer and CLI.
Association and budget validation precede measurement. Saved rows retain raw
batches, observed collector identity, budget, and JVM metadata. No schema,
dependency, library, or design change. No actor-step override or PR metadata
was supplied; used task 019 in the current worktree.

Pinned Valhalla with the verified overlay: real collection/publication Scry
passes (1 test, 81 assertions), exit 0, empty failure directory. Invalid fork,
configuration, and budget create no result file. Focused format/lint and diff
checks pass. No mocks or acceptance measurements. This pass ends before the
milestone: next connect five independent captures, separate allocation and graph
outcomes, and saved experimental reporting. No human blocker.

## 2026-10-28 — Five-fork experimental timing evidence

Ran the existing child command five times on pinned Valhalla, without a new runner
framework. All exits zero; 90 rows and a validated timing table are retained in
`evidence/experimental-timing-01` (374 child seconds). Small flat reads/stores
have large fork variation; reference stores fall below typical timer-call costs.
Next inspect clock resolution/raw duration accounting before approval, then
connect allocation and graph outcomes. This is not acceptance or a support limit.
Design unchanged; no dependencies or production code changed. No actor-step or
PR override supplied; used task 019 in the current worktree.

Real-expression/store-boundary Scry passes: 4 tests, 250 assertions, exit 0,
empty failure directory. Initial selector named a nonexistent namespace; corrected
before verification. Report script formatting/lint and shell syntax pass. This
pass ends before milestone 2; remaining integration is recorded above.

## 2026-10-28 — Experimental clock quantisation audit

Real pinned-runtime distributions show 54–70% zero-duration single stores and
41/42 ns smallest positive durations. Raw frequencies and reproduction are in
`evidence/experimental-clock-audit`. This establishes quantisation, not a corrected
estimate or the cause of flat-read variation. Next verify a longer changed-store
boundary without timed resets, or justify error bounds. Memory integration and
milestone 2 remain incomplete. Design reviewed and unchanged; no dependencies,
production code, or acceptance measurements changed. No actor-step override or
PR metadata was supplied; used task 019 in the current worktree.

Mock-free store-boundary Scry: 2 tests, 104 assertions pass, exit 0, no failure
files. Diagnostic process exits zero. No speed assertions or favourable reruns.
This is unfinished measurement work, not a human-action blocker.

## 2026-10-28 — Changed-store batch feasibility

Added a real Java diagnostic, not an acceptance collector. Distinct pre-reset
arrays preserve changed values and untouched elements across repeated batches.
All 12 layout/length/count combinations pass; 1,024-store batches have no zero
samples. Smaller batches still expose quantisation. Multiple arrays alter cache
and ownership conditions, so adoption needs explicit matched semantics. See
`evidence/experimental-store-batch/assessment.md`; no speed assertion or mocks.

Pinned Valhalla process exits zero. The initial missing internal misc export
compile failure is retained and fixed in the reproduction command. Shell syntax
and diff checks pass. Design reviewed and unchanged; no dependency changes.
This pass ends before milestone 2: real Criterium batch integration, flat-read
variation, separate memory collection, and saved reports remain. No human blocker
or actor-step override was supplied; used task 019/current worktree.

## 2026-10-28 — Experimental allocation boundary

Added Java Allocation.sample over existing unchecked operations and the real
thread counter. Inputs/result sinks precede counters; every store changes a
separate source once. Primitive read sinks avoid boxing. Post-counter checks
verify contents and ownership. No dependency, library, or design change.
No actor-step override or PR metadata supplied; used task 019/current worktree.

Pinned Valhalla smoke exits zero: 18 sites, 20 warmup batches and five recorded
batches per site. Raw evidence and limits are in experimental-allocation-boundary.
Shell syntax and diff checks pass; no Clojure files changed or Scry tests ran.
No mocks or acceptance measurements. This pass ends before milestone 2:
allocation inventory/persistence, controls and invalid-input tests, five forks,
graph integration, and saved reports remain. Timing issues remain separate.

## 2026-10-28 — Experimental allocation controls

Allocation.sample now rejects null operations and nonzero holder lengths with
IllegalArgumentException before setup. Real pinned-Valhalla checks pass: nine
invalid sites and ten array allocation comparisons against independent physical
stride. Raw evidence is in experimental-allocation-controls. No mocks, dependencies,
library, or design changes. Java-only slice; no Scry run. Shell syntax and diff
checks pass. No actor-step override or PR metadata supplied; used task 019/current
worktree.

This pass ends before milestone 2. Next retain holder controls and connect
inventory rows, persistence, five independent forks, and saved memory reporting.
Timing quantisation/read variation and graph integration remain open. These are
verification results, not acceptance evidence or a human-action blocker.

## 2026-10-28 — Holder allocation boundary controls

Real Allocation.sample matches independent Instrumentation holder sizes for both
layouts and construction/replacement: 20 controls pass at 16 bytes per operation.
Prebuilt payloads stay outside the counter; this is not total graph ownership.
Pinned Valhalla process exits zero on its first run; raw evidence and reproduction
are in experimental-holder-allocation. No mocks, dependencies, library, or design
changes. Java-only verification; no Scry run. Shell syntax and diff checks pass.
No actor-step override or PR metadata supplied; used task 019/current worktree.

This pass ends before milestone 2. Next connect allocation inventory/persistence,
five independent forks, graph outcomes, and saved reporting. Store quantisation
and flat-read variation remain separate open work. No acceptance measurement or
human-action blocker.

## 2026-10-28 — Five-fork experimental allocation

Connected the verified Java boundary to existing child envelopes and inventory
reporting. Fixed diagnostic budgets retain 20 warmup and five final 1,024-operation
batches per site; raw totals use 5,120 operations. Five independent pinned
Valhalla children pass (11 seconds total), with 90 rows and alternating order.
Harness commit precedes capture. See experimental-allocation-01 for reproduction.

Restricted-holder reads vary 0–24 bytes/op; flat-array reads allocate about 24.
Do not discard these results or assume scalar results eliminate internal boxes.
Next assess retained allocation batch variation, connect graph outcomes, and
resolve store quantisation/read timing variation. Milestone 2 remains incomplete;
this is not acceptance or a human blocker. Design reviewed and unchanged.
No dependencies, library changes, actor-step override, or PR metadata.

Real-counter Scry on pinned Valhalla: 1 test/188 assertions pass, exit 0, no
failure files. Focused lint/format and shell syntax pass. Initial report/test
delimiter errors were repaired with clj-paren-repair. No mocks used.

## 2026-10-28 — Saved allocation variation assessment

Audited all 90 saved rows without recollection. Holder reads are constant across
25 batches per fork; 0/24-byte differences align with alternating pair order.
This does not prove cause. Keep fixed budgets and disclose order/history limits,
not convergence. Five rows vary; only one varies in final batches. Raw counts
and reproducible assertions are in experimental-allocation-01/audit.clj and
its output. Design reviewed and unchanged; no dependencies or runtime code changed.

Existing JVM report validation passes and reproduces report.md byte for byte.
The saved-data audit assertions pass; focused lint and git diff checks pass.
No Scry tests ran: this slice adds an evidence audit, not collector behavior.
Babashka cannot load the report's ManagementFactory import; used JVM Clojure
for report verification and Babashka only for the pure saved-data audit.

Milestone 2 remains incomplete: next connect experimental graph outcomes and
resolve store timing quantisation plus read timing variation. No human blocker,
actor-step override, or PR metadata supplied. No acceptance run occurred.

## 2026-10-28 — Saved experimental read audit

Read all task contracts; design remains unchanged. No actor-step, worktree, or PR
handoff was supplied; used task 019 in the current worktree. Audited existing
experimental timing evidence rather than recollecting. Small flat-array reads
split by pair order and batch divisor; large arrays and holder reads do not.
Saved bootstrap estimates lack trajectories or compiled-code evidence, so cause
and convergence remain unknown. Separate allocation children cannot explain
these timing modes. Preserve all forks and disclose the range.

Verification: the existing saved report validator passes and reproduces report.md
byte-for-byte. The new mock-free audit checks 90 rows and 30 read outcomes; a
second execution reproduces read-audit.md byte-for-byte. Focused format, lint,
and diff checks pass after formatting. No dependencies or measurements changed.
This pass closes saved-read inspection only, not experimental milestone 2.
Next integrate supported flat graph outcomes and evidenced unavailable reference
roots, then resolve store timing quantisation and complete experimental reporting.
No human blocker exists; acceptance and public documentation remain unfinished.

## 2026-10-28 — Experimental graph operation roots

Real pinned-Valhalla controls pass for all nine flat operation sites and two
reference rejection cases. Holder read roots alias (zero increment); fresh holder
roots do not. Object[] must be cast to Object at the Java varargs graph boundary,
otherwise elements become roots. Initial failed attempt and corrected evidence
are retained in experimental-graph-operations. No mocks, dependencies, library,
or design changes. No actor-step override or PR metadata supplied.

Real agent process exits zero; shell syntax and diff checks pass. Java-only
verification; no Scry run. This pass ends before milestone 2: next connect graph
inventory rows, verified agent identity, five independent captures, unavailable
reference outcomes, and saved reporting. Store timing resolution remains open.
No acceptance run or human blocker.

## 2026-10-28 — Experimental graph collection boundary

Added Graph.sample with explicit Object roots and source/result/union counts.
Reads and array stores alias roots; construction/replacement do not. Invalid
sites reject before setup. Only supported flat containers enter this boundary;
reference unavailability belongs in the pending inventory adapter. Design reviewed
and unchanged; no dependencies or library changes. No actor-step override or PR
metadata supplied; used task 019/current worktree.

Pinned Valhalla real-agent checks pass: nine supported sites, eight invalid sites,
and existing graph controls; process exit zero. Shell syntax and diff checks pass.
Java-only slice, no Scry run or mocks. This pass ends before milestone 2: next
connect inventory rows, verified agent provenance, five independent captures,
unavailable reference outcomes, and saved reporting. Timing resolution remains
open. No acceptance measurement or human blocker.

## 2026-10-28 — Experimental graph inventory adapter

Added `valhalla.graph/collect!`: 30 rows per fork, including explicit unavailable
reference graphs. Arrays use the result-array root; fields retain all three
ownership roots. Do not reuse ordinary RecordGraphProbe hashes: this collector
uses FlatGraphProbe. Next connect its verified provenance, child persistence,
five independent captures, and saved reports. Timing resolution remains open.
Design reviewed and unchanged; no dependencies or library changes. No actor-step
or PR override supplied; used task 019/current worktree.

Pinned Valhalla real-agent controls and mock-free Scry pass (1 test, 69 assertions,
exit 0, empty failure directory). Tests check EDN preservation, root counts,
unavailable outcomes, alternating order, and invalid forks. Focused format/lint
and diff checks pass. No acceptance measurements ran. This pass ends before
milestone 2; remaining integration is unfinished work, not a human blocker.

## 2026-10-28 — Experimental active-agent identity

FlatGraphProbe now exposes active JAR/class resource hashes and location. Load
its JAR before loose classes; inactive or loose-class identity rejects. Resource
hashes do not detect runtime class transformation. Next connect child publication
and prepared-artifact reconciliation, then five-fork graph capture and reporting.
Design reviewed and unchanged; no dependency or library changes. No actor-step
path override or PR metadata supplied; used task 019/current worktree.

Pinned Valhalla real-process identity checks and existing flat graph controls
pass, exit 0. Independent artifact hashes agree; inactive and loose-class JVMs
reject. Shell syntax and diff checks pass. Java-only slice; no Scry tests or mocks.
No acceptance measurement ran. This pass ends before milestone 2; timing resolution
and graph integration remain unfinished, not a human-action blocker.

## 2026-10-28 — Experimental graph capture connection

Connected the flat graph collector to exclusive child publication with active
FlatGraphProbe identity. The bounded run script declares independent artifact
hashes before launching five JVMs; saved reporting reconciles each row, runtime,
exit, identity, and projected inventory coverage. Reference graphs stay unavailable.
Real-agent Scry on pinned Valhalla passes (2 tests, 105 assertions, exit 0).
No mocks or dependency changes. Design reviewed and unchanged. Next execute the
committed script and assess its saved report; store timing resolution remains open.

## 2026-10-28 — Five-fork experimental graph evidence

Executed the committed graph command on pinned Valhalla. All five children exit
zero; saved reporting validates 150 outcomes and independently declared artifact
hashes. Flat roots agree across forks; reference roots remain unavailable. First
report failed on a missing require quote; retained report-failure.edn and fixed
reporting without recollection. Assessment records sizes, durations, and limits.
Repeat rendering is byte-identical. Focused lint, formatting, shell syntax, and
diff checks pass. No dependency or design change, and no actor-step override or
PR metadata supplied. This is diagnostic evidence, not acceptance. Next slice:
resolve store timing quantisation, then freeze budgets/limits and run acceptance.
Milestone 2 remains open for timing, not graph wiring; no human blocker.

## 2026-10-28 — Experimental memory coverage decision

Closed the milestone 2 memory item from existing five-fork evidence: 90 measured
allocation rows, 75 measured flat graph roots, and 75 evidenced unavailable
reference roots. Added a bounded assessment that separates allocation from graph
ownership and preserves order effects. Both existing saved validators pass and
reproduce reports byte for byte; allocation audit assertions pass. No mocks,
new measurements, code, or dependencies; no Scry run needed for this prose slice.
Design reviewed and unchanged. No actor-step or PR override supplied.

This pass ends before the full experimental milestone. Next resolve store timing
quantisation, then freeze acceptance commands/budgets. Memory wiring is complete;
do not repeat it. No human blocker.

## 2026-10-28 — Matched store batch contract

Added StoreBatch with distinct arrays and slot-only untimed resets at index zero.
The measurement contract explicitly selects 1,024 stores and includes traversal
cost; equal logical workloads need not have equal physical footprints. The old
probe used index one and full-array resets; do not use its numbers as corrected
evidence. Design reviewed and unchanged; no dependencies or library changes.
No actor-step override or PR metadata supplied; used task 019/current worktree.

Pinned Valhalla real state checks pass (28 checks, exit 0), including full identity
uniqueness, untouched elements, repeated resets, and invalid dimensions. No mocks,
speed assertions, or Clojure changes; no Scry run. Shell syntax and diff checks
pass. This pass ends before milestone 2: Criterium integration, raw divisor and
clock verification, and corrected five-fork timing remain. No human blocker.

## 2026-10-28 — Criterium store traversal integration

Timing stores now use the matched 1,024-array StoreBatch with untimed slot resets.
Raw rows retain Criterium invocation count and stores per invocation; operations
is their product, with raw nanoseconds unchanged. Existing normalisation therefore
reports ns/store, including traversal and amortised timer/input costs. Compile
StoreBatch alongside Operations in the next capture command. No dependency,
library, or design change; no actor-step override or PR metadata supplied.

Pinned Valhalla real collection and generated normal/warmup expression Scry pass:
4 tests, 196 assertions, exit 0, no failure files. Java working-set controls pass
28 checks. Focused formatting, lint, and diff checks pass. No mocks or acceptance
measurements. This pass ends before milestone 2: verify raw traversal clock
resolution, then retain corrected five-fork timing and saved reporting. Existing
attempt 01 remains unchanged. No human blocker.

## 2026-10-28 — Actual traversal clock resolution

Used StoreBatch itself with slot-only untimed reset, not the earlier exploratory
probe. Pinned Valhalla first attempt passes final changed/untouched-slot checks;
all four sites have zero zero-duration observations out of 1,000. Small-flat
minimum is 208 ns, so clock quantisation is still material. Retained raw summaries,
command, machine/runtime, and exit in experimental-traversal-clock. No speed
threshold, mocks, Clojure changes, or dependency changes. Design reviewed and
unchanged; no actor-step/PR override supplied. Shell syntax and diff checks pass.

Pass ends before milestone 2. Next compile StoreBatch in the corrected five-fork
timing command, retain both divisors and report, and assess variation and timer
limits. Memory integration is complete; do not repeat it. No human blocker.

## 2026-10-28 — Corrected five-fork timing and experimental milestone

Committed capture commands before five pinned-Valhalla children; all exit zero
(122 child seconds). Attempt 02 retains 90 rows and validates both normalization
divisors. Small-flat read/store modes still vary; keep all forks and disclose
clock/traversal costs. Together with existing memory reports, this completes
diagnostic milestone 2, not acceptance. Next freeze acceptance commands/budgets
and run original S-03 plus the full matrix. No dependencies or design changes;
no actor-step override or PR metadata supplied; used task 019/current worktree.

Real pinned-runtime Scry: 6 tests, 300 assertions pass, exit zero, empty failure
directory. Saved rendering is repeatable; focused format/lint and shell syntax
pass. No mocks or speed assertions. Attempt 01 remains unchanged.

## 2026-10-28 — Acceptance command freeze

Reserved acceptance-01 with existing metric scripts and saved validators under
new identities. Budgets preserve verified diagnostic workloads and disclosed
limits. Original S-03 uses zero-based forks; retain its raw child envelopes,
not only the temporary-file parent's table. Design reviewed and unchanged;
no dependencies or library code changed. No actor-step override or PR supplied.

Pinned Temurin 21 base/test classpaths exclude Criterium, Valhalla, and agent/
overlay artifacts. Four shell syntax checks pass. No measurements or Scry tests
ran; this pass changes capture commands and documentation, not collectors.
This pass ends before milestone 3: next execute S-03 and all frozen metric runs,
validate 490 matrix outcomes, and assess the first complete evidence. No human
blocker. Do not mark acceptance complete from this command reservation.

Each saved report script passes clj-kondo in its own invocation. A combined
invocation treats these namespace-free scripts as one namespace and reports
duplicate requires; separate processes match their execution model. Diff checks
pass. The copied report checks have not yet run against acceptance data because
that data does not exist.

## 2026-10-28 — First acceptance matrix capture

Executed all frozen matrix commands at f292f62, without collector changes or
retries. Ordinary timing/allocation/graph take 41/4/4 seconds; experimental child
totals are 126/12/2. All exits and saved report validators pass. Raw evidence and
unfavourable results remain in acceptance-01; partial report disclaimers do not
constitute acceptance approval. No dependencies, design, or library changes.
No actor-step override or PR metadata supplied; used task 019/current worktree.

This pass ends before milestone 3: original S-03 raw children, field-declaration
test, and the 490-outcome cross-inventory audit remain. Do not rerun completed
matrix captures. No mocks or Scry tests ran; this slice executes existing real
collectors and saved validators. Public documentation and final checks remain.
No human blocker exists.

Staged diff check flags only the raw ordinary report's trailing blank line;
preserved exact stdout rather than edit evidence. The capture set is bounded
(about 1.3 MB). All four frozen scripts still pass shell syntax checks.

## 2026-10-28 — Original S-03 acceptance retained

Committed the capture command before running five original children on pinned
Temurin 21. All exits zero (97 child seconds); all six unchanged S-03 decisions
pass. Field-declaration Scry passes: 1 test/3 assertions, exit zero, no failure
files. Saved rendering is byte-identical; focused formatting/lint and shell syntax
pass. No mocks, dependencies, library, or design changes. No actor-step override
or PR metadata supplied; used task 019/current worktree.

Original envelopes retain per-call statistics and collector identity, not raw
Criterium batches. See acceptance-01/s03/assessment.md. This pass ends before
milestone 3: the 490-outcome cross-inventory audit remains, followed by public
reporting and final checks. Do not recollect completed measurements. No human
blocker exists.

## 2026-10-28 — Acceptance inventory audit

Audited saved evidence without recollection: 430 raw metric outcomes (355 measured,
75 evidenced unavailable reference graphs) plus 60 original S-03 statistics.
Legacy statistics are not raw batches; keep this distinction in the public page.
Reused existing readers/report validators and complete inventory validation.
Initial report-loading alias conflict was fixed with separate report namespaces;
no captures changed. Two successful audit executions produce identical EDN.

Temurin 21 Scry metric/table regressions pass (8 tests, 127 assertions), exit 0,
empty failure directory. Focused format/lint and diff checks pass. Design reviewed
and unchanged; no dependency or library change. No actor-step override or PR
metadata supplied. Next publish tables/limits from acceptance-01, then final
quality and three-version tests. See acceptance-01/audit.md for reproduction.

## 2026-10-28 — Public performance page

Published README/performance.md from acceptance-01, with compact measured tables,
source links, graph ownership, runtime boundaries, and explicit unavailable
reference graphs. Qualified the README speed claim and linked the development
guide. No code, dependencies, measurement budgets, or evidence changed. Design
reviewed and unchanged. No actor-step override or PR metadata was supplied.

Vale initially rejected technical terms, including existing Criterium/JVMs in
the development guide. Added the specific terms to the shared vocabulary;
full Vale now passes. Tracked Markdown link checks pass (192 successful,
zero errors), and git diff --check passes. No tests were added for prose;
existing real-evidence tables supply the numbers. Final runtime/benchmark suites,
format/lint/API checks, and acceptance review remain for the next slice. Do not
recollect measurements or interpret publication as completed final verification.
