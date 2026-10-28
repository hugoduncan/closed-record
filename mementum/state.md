# State

Working memory. Read this file first in each session. Update it after each significant change.

## Project

closed-record: a `defrecord`-like record. It does not permit assoc of
undeclared keys. It does not permit dissoc of declared keys.
Main form: `defclosedrecord`.

## Status

- Repository initialised: README, AGENTS, psi extension config.
- Protocol directories initialised: ramora, mementum, munera.
- Design documents: `ramora/INDEX.md`. Design pages describe the current design,
  not implementation progress. Each design's `embodiment.md` owns current
  implementation differences and verification limits. Historical work records
  stay in Munera and Git history.
- Project skeleton: `deps.edn` (Clojure 1.11.4, aliases `:test`, `:bench`, `:nrepl`, `:clj-1.12`, `:clj-1.13`),
  `src/closed_record/core.cljc` (`defclosedrecord`, `check`, `ClosedRecord`),
  `src/closed_record/impl/jvm.clj` (JVM emitter). Test runner: scry.
- Tests: `test/closed_record/compiler_rules_test.clj` (K-06..K-08 compiler rules),
  `test/closed_record/defclosedrecord_test.clj` (behaviour, errors, factories),
  `test/closed_record/literal_test.clj` (V-04, V-05: print, read and code constants),
  `test/closed_record/field_value_test.clj` (V-06, V-08, V-09: field values; V-11: field declarations),
  `test/closed_record/hidden_key_test.clj` (V-14: hidden extra keys of a literal, and their retention),
  `test/closed_record/property_test.clj` (`closed-record.property-test`, V-03: test.check property of random
  `assoc`/`dissoc` sequences; `org.clojure/test.check` 1.1.3 is in the `:test` alias),
  `test/closed_record/differential_test.clj` (`closed-record.differential-test`, V-02: test.check property that
  compares a closed record with a `defrecord` after random permitted steps),
  `test/closed_record/malli_test.clj` (`closed-record.malli-test`, V-07: malli map schemas and transformers;
  `metosin/malli` 0.20.3 is in the `:test` alias),
  `test/closed_record/rejected_key_message_test.clj` (`closed-record.rejected-key-message-test`: the `ex-info`
  message of a rejected `assoc` or `dissoc`, Scope 2).
- Verification plan done: V-01 to V-15 (all). V-11: task 012 acceptance run (5 forks, criterium 0.5.251), all six
  rows pass (`assoc` `P / R` 1.000, `get` 0.996, `=` 0.902); the field declaration test (`field_value_test.clj`)
  passes. Measured cause of the earlier `assoc` gap (task 011: 1.193): the method size stopped C2 inlining
  (`P::assoc` 995 bytes, `FreqInlineSize` 325). The `case` `tableswitch` and the inline throw form made the size.
  D-21 fixed it (`P::assoc` 217 bytes, inlined).
- Tests: `clojure -M:test -m scry.cli`. Other versions: add `:clj-1.12` or `:clj-1.13`.
- Bench: `clojure -M:bench` (V-11), 5 child JVM forks, about 6 min. It exits 1 if an operation fails, 2 if a fork
  fails. The pure layer (`closed-record.bench.report`) and the fork layer (`closed-record.bench.fork`) have tests
  in the suite (`bench_report_test.clj`, `bench_fork_test.clj`).
- REPL: `clojure -M:test:nrepl`. In the REPL: `(require '[scry.core :as scry]) (scry/run)`.

## Fresh Linux quality verification

Documented pinned installation and shared format/lint, Vale, API freshness, and
focused tooling tests pass on Ubuntu 24.04 x86-64 under QEMU/Podman on macOS
arm64. The fresh environment used no host caches or user configuration.
All 28 tests and 316 assertions pass; tracked bytes and index trees are unchanged.
This is local Linux evidence, not hosted Actions evidence.

## Indexed quality scope

Independent contract tests in `quality_scope_test.clj` cover required roots and
files in project and real Git index selection, plus each missing-input diagnostic.
Expectations permit additional scope and do not depend on production path lists.

Maintained format/lint scope excludes source symlinks and does not descend
through linked directories. Required paths reject linked components, including
configuration parents. Indexed hooks select regular-file Git modes and exclude
linked working paths. Explicit-path formatting retains tool link handling.
Real commands and installed-hook tests cover external-byte and index preservation.

`closed-record.index-scope/paths!` validates required indexed paths and selects
owned Clojure/EDN files. Required roots need indexed descendants; required
files need regular-file Git modes. Working-tree scope validates required
directory/file types before tool setup. Explicit format inputs still accept
files and directories. It does not isolate working-tree content. Hook callers
must supply staged-content isolation before invoking tools. `closed-record.lint/lint-index!`
uses these paths with shared lint setup and warning-level failure. It excludes
untracked files but still reads working-tree bytes.

## Markdown link tooling

- `bb lychee:check` selects Git-tracked Markdown from the repository root.
  `dev/link_check.clj` runs local fragment validation then online validation
  with shared `lychee.toml` policy. It constructs both command vectors before
  eager execution and returns the first nonzero exit. Supported tools: Babashka 1.13.225 and
  lychee 0.24.2. `bb dev/link_check_test.clj` runs isolated real-tool
  contract tests (nine tests, 125 assertions), with a loopback HTTP server.
  Fixtures cover mixed local/online failures, HTTP 429, and request timeout,
  with a 90-second process deadline. TLS trust and HTTP-response retry recovery
  remain unverified; see task 017 implementation notes.
  The live repository check passes for 133 tracked Markdown files, with no
  URL exceptions or link repairs. Tracked bytes and Git status remain unchanged.
  CI runs fixtures and the shared task in one separate read-only job.
  The changelog records the command and CI check; the embodiment artifact map
  describes the link tooling and links to the development guide.
  Pinned installation commands are in README/development.md; macOS arm64
  installation and actionlint pass. Final local acceptance also passes: full-path
  format/lint, contract fixtures, and both live passes. Hosted CI remains unverified.

## Continuous integration

- `.github/workflows/ci.yml` defines read-only checks for pushes to `master`
  and pull requests: code quality, three Clojure runtime versions, and separate
  build verification on Ubuntu with Temurin 21. Failed tests upload hidden
  `.scry-results/` files. Local Java 21 checks passed for all three runtime
  aliases, focused build tests, and JAR packaging; quality checks and
  actionlint passed. Hosted execution is not yet verified.

## Release architecture

- Implemented architecture: `ramora/release-workflow.md`. Follow scry's build.clj:
  check credentials, build the one JAR/POM, call stock deps-deploy, return paths.
- Manual master dispatch, default dry run, one full-history dispatch-SHA checkout,
  one job, checks before writes, and step-scoped secrets. Deployment may rebuild
  from that checkout. GitHub assets use the deployment outputs.
- Implemented changelog contract: exact Unreleased/dated version headings, unique
  versions, nonblank dated bodies, and shared validation before preparation or
  note selection. Body Markdown is opaque; empty Unreleased is valid unless used.
- Use ordinary Git tag push and `gh release create --verify-tag --notes-file`.
  Standard tool-internal retries, redirects, checksum handling, and temporary
  draft cleanup are accepted. No custom transport or remote-absence proof.
- All task-015 implementation is reverted to baseline `c9066bd`: the old release and
  deployment source/tests, changelog, and aliases were removed before replacement. Packaging
  docs also match the baseline. Build the small path without restoring that
  module hierarchy; the replacement architecture above is now implemented.
- Existing packaging remains in `build.clj`. No release has been published by
  this work. `:deploy` now adds stock deps-deploy 0.2.2 outside runtime deps.
- `build/deploy` checks environment credentials before building, calls stock
  deps-deploy, and returns version/JAR/POM paths. Optional `:github-output`
  appends `version`, `jar-file`, and `pom-file` after deployment succeeds.
  Pure notes support is in `dev/release/closed_record/notes.clj`: full validation,
  stamping, and preview-aware selection. Focused tests use :release:release-test
  and --dir test-release. Local preparation is in `closed-record.release/prepare`, with clean-master
  and full-history guards and a changelog-only commit. The notes CLI exports a supplied notes file and appends GitHub outputs
  (`sha`, `version`, `tag`, `source`, `preparation-required`); only `--preview`
  permits Unreleased fallback. `.github/workflows/release.yml` implements the manual
  path with typed explicit-false publication gates and command-scoped Git token
  authentication. Ordinary CI includes release-tooling checks. Local actionlint
  passes; hosted execution and publication remain unverified.
- Historical transport findings are in the closed release task's implementation
  log. They describe library behaviour, not active release blockers. For current
  work, follow `munera/plan.md`. Release acceptance evidence is in
  `munera/closed/016-simple-release-workflow/implementation.md`.

## Development quality tooling

Reusable quality-boundary guidance is in
`mementum/knowledge/development-quality-boundaries.md`; related alias and
fixture rules are in `dev-tooling.md` and `artifact-verification.md` in the
same directory. Acceptance and review evidence is in
`munera/closed/018-development-quality-checks/`. Hosted Actions execution
remains unverified; Linux installation evidence uses QEMU/Podman.

- `.pre-commit-config.yaml` runs serial formatting before whole-index lint with
  fail-fast ordering. Internal `quality:format-staged` and `quality:lint-staged`
  tasks rely on pre-commit staged-content isolation; do not invoke them as
  independent staged-byte checks. Installed-hook tests use pre-commit 4.6.0.
  Selected-formatting tests also preserve an unchanged committed format defect
  during prose-only commits and separate staged-file repairs. Only the selected
  file is repaired; rejected commits preserve HEAD, index tree, and other bytes.
  Child hook probes return through fixture cleanup before process exit; both
  successful and rejected commits remove their temporary roots and caches.
  Installed-hook cases also check untracked source presence, exact bytes, and
  absence from the index after every commit attempt.
  Clean commits, staged defects hidden by unstaged corrections, repair rejection,
  and manual restaging pass. Staged-defect checks require the indexed source path,
  expected lint finding, and unchanged HEAD as well as byte/index preservation. Prose-only commit tests also verify whole-index lint:
  warnings and errors in unchanged committed source block commits without changes
  to HEAD, index tree, or working bytes. Partial-stage tests also verify nonconflicting edit restoration and conflicting
  repair rollback, with unchanged index, HEAD, and unrelated tracked bytes.
  Partial-stage probes require the modified-files diagnostic; the conflict case
  also requires pre-commit's conflict-and-rollback diagnostic, so an earlier
  setup failure cannot satisfy the rollback check. The successful-repair probe
  requires modified-files diagnostics and no lint-hook diagnostic, preserves
  HEAD and the index, and verifies that manual restaging permits the commit.

- `bb clj-fmt check|fix [PATHS...]` and `clj-fmt:check`/`clj-fmt:fix`
  share the required scope and explicit `.cljfmt.edn`. The CLI file pattern
  includes EDN, including files found recursively. Check returns the tool exit
  without writing; fix is explicit. Explicit files and directories accept absolute
  paths, including paths outside the repository; relative paths resolve from root.

- Project format and lint enumerate owned working-tree files with the same
  predicate as indexed hooks, including `.bb`. Nested `.cache`, `.cpcache`,
  `imports`, and `target` paths are excluded even from project format repair.
  Explicit formatting still checks the supplied paths without this filter.
- `dev/quality/closed_record/quality_scope.clj` owns required format/lint paths
  and Git-path selection. Validate required inputs before invoking tools.
  Dependency imports and caches are not owned inputs.
- `bb clj-kondo:copy-config` resolves independent reproducible dependency bases,
  then imports their union. Dependency imports are ignored and regenerated;
  required scope is checked before setup. A cold local-dependency regression checks
  usable exported rules from both additive and dependency-replacing alias bases,
  with tracked-byte and index preservation.
- Shared lint and dependency imports use clj-kondo `--repro` to ignore user-global
  configuration. Real-process fixtures disable warning/error rules globally;
  project lint and installed hooks still reject defects and preserve tracked bytes
  and index trees. Imports also pass with malformed isolated global configuration.
- `bb clj-kondo:lint` prepares imports automatically, checks shared scope, and
  emits EDN findings. Warnings fail. Real-tool probes cover the defrecord mapping
  and setup failure propagation; no extra suppressions were needed.
  Real Git fixtures also verify format/lint success and defect failures preserve
  all tracked bytes and index trees, including binary and unstaged CRLF files.
- Focused scope tests: `clojure -Srepro -M:tooling-test -m scry.cli --dir test-tooling`.
  The tooling alias adds no base runtime dependencies.
- `:quickdoc` pins Quickdoc v0.2.6 and adds `dev/quality`; its dependency
  resolution leaves the base runtime classpath unchanged. The combined
  `:quickdoc:tooling-test` aliases support the same focused test command.

- `closed-record.api-docs/generate` produces in-memory Quickdoc Markdown with
  internal-var exclusions and source links. `bb api-docs` writes UTF-8 output;
  `bb api-docs --check` compares exact bytes without writing and reports the
  repair command for absent or stale output. Real CLI coverage rejects line-ending-only
  LF-to-CRLF changes without changing tracked bytes or the index tree; regeneration
  restores the original bytes and passes freshness. Invalid arguments fail without writes.
  Both commands validate the required source as a regular file before analysis;
  missing source and analysis error findings cannot create or overwrite the reference.
  Analysis errors report source diagnostics before rendering. Real CLI tests cover
  malformed input with and without output for generation and freshness, preserving
  tracked bytes and the index. Missing-source tests cover
  both output states and preserve tracked bytes and index trees.
  API analysis uses embedded clj-kondo with `:repro true` and owned configuration,
  then the pinned Quickdoc renderer; the public Quickdoc entry point does not
  expose reproducible analysis. Global settings cannot change generated bytes.
  Quickdoc uses the owned clj-kondo `:cljc {:features #{:clj}}` setting.
  Real-process divergent-branch tests verify JVM docstrings and arguments and
  exclude browser-only vars; without that setting Quickdoc selects browser content.
  `doc/API.md` is generated and linked from README. A real-process negative
  control proves freshness can pass while independent content tests fail. Run it separately
  with `clojure -Srepro -M:quickdoc:tooling-test dev/acceptance/api_content_defect.clj`;
  the normal suite does not run tests of tests. The probe validates an explicit
  EDN-safe result artifact and accepts only the missing-support-notice failure,
  with zero errors; unrelated failures and load errors are rejected. API docstrings describe
  constructor arguments, compiler option forwarding, error data, and literal-key
  loss through compilation and print/read round trips; content tests check these.

- Independent real-command prose tests cover warnings in each maintained document
  and nested README Markdown, plus clean and excluded historical task/Ramora
  controls. All cases preserve tracked bytes and index trees.

- Prose scope uses the shared path validator before Vale synchronization.
  Required documents, configuration, and vocabulary must be regular files;
  `README/` must be a directory. Real-command wrong-type tests cover all six
  requirements and verify unchanged tracked bytes and index trees.

- `closed-record.vale-results/outcome` validates decoded Vale JSON with string
  keys. Suggestions pass and remain visible; warnings/errors fail; malformed
  findings and unknown severities throw. `bb vale` validates required prose paths,
  syncs styles with explicit configuration and no global configuration, and reports
  JSON findings. Real-process fixtures cover severity and setup failures without
  file changes. Git-tracked probes also compare exact bytes and index trees,
  including unrelated binary files and unstaged CRLF prose. Invalid arguments
  and missing executables have the same Git-tracked preservation coverage. The scoped prose baseline passes with explicit technical vocabulary and inline
  code for API literal syntax. No prose rule is disabled.

- Vale 3.17.1 uses `.vale.ini`, proselint v0.1.0, and owned ClosedRecord
  vocabulary. Downloaded `styles/proselint/` is ignored. Before focused tooling
  tests, run `vale sync --config=.vale.ini`. Real Vale tests cover narrow code
  and lambda exclusions plus ordinary prose and spelling failures.
  Policy tests use `--no-global` with temporary malformed global configuration
  at macOS and Linux configuration paths; contributor settings remain unchanged.

## Local verification environment

- Pinned quality tools are available: cljfmt 0.16.4 and clj-kondo 2026.04.15.
- Java defaults to 25. For Java 21 checks, set `JAVA_HOME` to
  `/Users/duncan/.local/share/mise/installs/java/temurin-21` and prepend
  `$JAVA_HOME/bin` to `PATH` (Temurin 21.0.10+7-LTS).

## Packaging

- `clojure -T:build jar` builds a source-only `org.hugoduncan/closed-record` JAR
  under `target/`; version is `0.1.<Git commit count>`. Git metadata is required.
- `clojure -T:build clean` removes `target/` without Git lookup.
- The build uses tools.build 0.10.10. Runtime dependencies are unchanged.
- Root `LICENSE` contains EPL-2.0 text and is included in the JAR.
- `ramora/defclosedrecord/embodiment.md` maps build artifacts and dependency
  boundaries; it links to README for build commands and version requirements.
- README links to `README/development.md` and `README/releases.md` for build, clean, version/history requirements,
  artifact paths, and the separate build-test command. No release is published.
- Focused build tests: `clojure -M:test:build-test -m scry.cli --dir test-build`.
  Test subprocesses use `clojure.java.shell` to retain Clojure 1.11 support.
  Artifact tests build in a temporary one-commit Git project, not the checkout.
  Private helpers separate project setup, source/archive inventories, and archive
  checks; the test keeps build orchestration and fixture cleanup.
  Fixture Git and build processes remove inherited `GIT_*` overrides, disable
  system configuration, and use fixture-owned global configuration and empty hooks.
  Archive tests derive runtime entries from fixture sources, permit standard JAR
  metadata, reject other content (with fixture probes under `test/`,
  `dev/bench/`, `munera/`, and `mementum/`), compare packaged bytes, parse Maven
  metadata, and rebuild with stale output to check cleanup. A separate consumer JVM
  uses only the artifact and base runtime dependency JARs; resource-origin checks
  confirm both core and emitter load from the artifact.

## Tasks

Munera tracks the tasks. Do not record tasks or task progress in this file.

- Task order: `munera/plan.md`.
- Open tasks: `munera/open/`. Progress: the `steps.md` file of each task.

## Key design facts

- The type uses `deftype*` with the `defrecord` layout. The `deftype` macro forbids the `__meta __extmap __hash __hasheq` fields.
- The compiler makes `create` and the n-argument constructor only with exactly those 4 trailing fields.
- Map literals with extra keys put them in `__extmap`. No observable result depends on it. `check` finds them.
- The type must not implement `IType`: `Compiler.emitValue` checks `IType` before `IRecord` (K-08).
- Equality and hash mirror `defrecord`: `.equals` = `mapEquals`, `.hashCode` = `mapHash`,
  `hasheq` = type-hash XOR `mapHasheq`, `=` requires the same class (D-07).
- `check` uses the internal `ClosedRecord` protocol (D-17). It throws for a value that is not a closed record (D-18).
- Design review done (2026-10-19). Decisions split into `ramora/defclosedrecord/decisions/`. Evidence E-01..E-07 in context.md.

## Related

- `README.md`
- `munera/plan.md`
- `ramora/INDEX.md`

## Local acceptance evidence

Java 21 verification on 2026-10-27 passed all three runtime versions, focused
build/release suites, link fixtures, and online links. Source-only packaging
and unchanged base dependencies were checked. Temporary actionlint 1.7.7 now
accepts both workflows. Final shared checks and tooling tests pass after the
documentation changes. Fresh Linux setup now passes under QEMU/Podman.

## Release verification

Java 21 full-path format/lint, all three runtime suites, focused build/release
suites, JAR packaging, and actionlint 1.7.12 pass locally. Maintainer commands
and manual recovery are in `README/releases.md`. No hosted publication is verified.

## Task 018 API encoding review

The API CLI lifecycle fixture now generates non-ASCII source documentation and
checks exact UTF-8 bytes, freshness, repeatability, and check-only preservation.
All 4 API CLI tests (63 assertions), explicit formatting, and shared lint pass.
The review follow-up is complete; library docstrings and design are unchanged.

## 2026-10-27 — API configuration presence follow-up

- 2026-10-27: addressed 1 review step. API generation now validates owned
  analysis configuration as a regular file before analysis. Real CLI missing-config
  probes preserve tracked bytes and the index, with and without output, for both
  commands. API CLI suite: 7 tests, 122 assertions pass, including JVM reader and
  global configuration isolation. Independent content suite: 1 test, 42 assertions
  pass. Both Scry runs exit 0 with no failure artifacts. Shared format, lint,
  Vale, and API freshness pass. Updated plan and guide; design is unchanged.

## API output boundary

Both API commands now reject symbolic links in `doc/API.md` or its parent before
analysis or writes, using the shared link check. Missing ordinary output remains
supported. Real CLI tests preserve external bytes, link targets, and index trees.
API CLI tests pass (8 tests, 146 assertions); independent content tests pass
(1 test, 42 assertions). Shared format, lint, Vale, and freshness pass.
The review follow-up is complete; design is unchanged.

## Public quality task coverage

Disposable projects use the owned `bb.edn` for tracked format/lint checks and
prose selection. These tests verify public exits and diagnostics, not custom
helper exit handling. Explicit formatting tests use a path with spaces outside
maintained scope, retain a separate maintained defect, and check selected-file
repair plus byte/index preservation.

## Shared tooling fixtures

Task 018 review follow-up is complete. Reusable fixtures are in
`test-tooling/closed_record/test_support/`: `code_project`, `hooks`, and
`api_project`. Child isolation and API defect probes load these helpers, not test
namespaces. Distinct project setups and cleanup remain unchanged. All affected
namespaces pass separately; full tooling suite passes 50 tests and 838 assertions.
The separate API defect probe and shared format/lint checks pass on Java 25.

## Task 018 documentation review

The Unreleased changelog now summarizes shared quality commands, the generated
API reference, staged-content hooks, and CI checks. Command names and local link
targets are verified. Both documentation review follow-ups are complete. The
artifact map now covers quality tasks, CI checks, and development aliases, with
a link to the quality architecture. It moved to
`ramora/defclosedrecord/embodiment/artifacts.md` to keep Ramora pages below
1000 words. Both link-check passes succeed; design and runtime are unchanged.

## Prose traversal review

Task 018 prose traversal follow-up is complete. Code and prose share link-safe
file traversal, with separate ownership predicates. Nested Markdown links and
directory cycles are excluded. Public-command tests enforce a 30-second bound,
ordinary nested warning detection, and external-byte, link-target, tracked-byte,
and index preservation. Full tooling suite: 51 tests, 852 assertions pass;
shared format, lint, Vale, and API freshness pass. Design is unchanged.

## Experimental benchmark runtime

The isolated Valhalla probe is `dev/bench/valhalla/probe.sh`; select its JDK
explicitly with JAVA_HOME. Build 27-jep401ea3+1-1 needs annotated strict final
fields initialized before super(). Nullable value-typed fields can also flatten;
use runtime layout queries, not nullability, to distinguish representations.
Build pins and executable evidence are in task 019's `features.md` and evidence.

The Valhalla capability probe also verifies an Object-declared nullable field
as a non-flat control with the same Point payload. Keep its cast and declared
type explicit in comparisons; a nullable Point-declared field still flattens.

The isolated `dev/bench/valhalla/applicability.sh` checks Clojure 1.11.4 on the
pinned EA with real generated records. Point hints still emit nullable, non-flat
Object fields. Records can hold external values and flat arrays but remain
identity classes; experimental representation comparisons need Java fixtures.

The Valhalla probe now fails if the selected flat array or restricted field is
not flat, and checks both array metadata contracts. Plain atomic element access
is not volatile ordering; holder replacement constructs an immutable final-field
object. Build-specific operation contracts are in task 019 features.md. Memory
tools still need independent validation for flat layouts.

## Benchmark inventory accounting

Task 019's fixed inventory is `dev/bench/cases.edn`; its accounting contract is
`munera/closed/019-space-time-performance/measurement-contract.md`. Graph union
minus source measures newly reachable data; prebuilt payloads can contribute to
that metric without operation allocation. Flat payloads belong to container
layout, not invented boxed objects. Memory tooling remains unvalidated.

## Ordinary benchmark fixtures

`closed-record.bench.fixtures/assoc-pair` constructs matched width-3/16 records
from the fixed inventory without Criterium. Keep setup outside measurement and
use literal assoc timing expressions, not a fixture operation wrapper. Real
fixture tests check payload sharing by identity as well as logical equivalence.

## Experimental fixture checks

`dev/bench/valhalla/fixtures.sh` reads the fixed inventory and checks real array
and field pair outputs on the pinned EA, after the capability probe. Array reset
is checked twice outside operations; this is not proof of a timed batch boundary.

## Allocation measurement controls

`dev/bench/memory/allocation-probe.sh` checks the JDK ThreadMXBean on either
explicit JAVA_HOME. Escaping byte-array and empty-boundary controls pass on
Temurin 21 and the pinned Valhalla EA. This does not validate flat allocation,
cross-thread exclusion, or retained graphs; those remain separate checks.

The allocation probe now checks cross-thread exclusion with volatile handshakes:
worker allocation occurs wholly inside the parent counter interval. Both pinned
JDKs report zero parent bytes and the expected worker bytes. Flat allocation and
retained-graph controls remain separate, unvalidated boundaries.

## Flat-array allocation controls

Task 019 now checks flat-array allocation growth against instance-specific
Unsafe layout queries. Class-based queries reject value arrays. Growth matches
the four-byte stride, but construction allocates 24 extra bytes per call beyond
array layout. Resolve this overhead and flat-field controls before approving
memory measurements; graph accounting remains unvalidated.


The flat-array allocation probe now isolates the extra 24 bytes per call to the
argument boundary: passing a preboxed Object outside the counter removes it at
both lengths. Preboxed counts equal instance layout. Keep typed versus Object
arguments explicit in future measurements; do not subtract adaptation costs.
Flat-field allocation and retained graphs remain unvalidated.

## Field allocation controls

Task 019's field-allocation.sh uses an isolated Instrumentation agent for shallow
sizes. Flat holders allocate 16 bytes each; typed reference holders allocate
16 plus a 24-byte payload box. Add a preboxed reference control next; graph
accounting remains unvalidated. Evidence retains the failed holder-only assumption.


Task 019 preboxed field control now passes: Object-argument reference holders
allocate only their 16-byte shallow size; typed Point arguments still allocate
40 bytes. Evidence: preboxed-field-allocation. Retained-graph validation is next;
memory tooling is not yet approved.

Task 019: ordinary-array graph controls pass on both pinned JDKs. See
`munera/closed/019-space-time-performance/evidence/array-graph/`. Record traversal
and flat-layout graph accounting remain open; the array walker is not a general
memory tool.

Task 019 flat-graph.sh validates primitive-only Point container graphs on the
pinned EA. Count inline data in shallow size; do not traverse materialized
boxes. Array sizes match instance-specific Unsafe queries. Record traversal
and reference-container controls remain open; this is not a general walker.

Task 019 value-identity.sh shows equal Points collapse in IdentityHashMap on
the pinned EA. Physical box sharing is not established; reference-payload graph
metrics remain unavailable. See evidence/value-identity and the new follow-up.

Record graph controls now pass on both pinned runtimes for width-3/16 records
with primitive-array payloads. Traverse physical instance fields, not map entries;
reject unknown reachable types. Boxed Long inventory payloads and physical
value-box accounting remain unvalidated. See task 019 evidence/record-graph.

Task 019 inventory Long graph controls now pass on both pinned runtimes;
see evidence/boxed-long-graph. Exact Long leaves are supported. Physical
Valhalla value-box accounting remains open. Shared lint currently reports an
unrelated duplicate require in dev/acceptance/api_content_defect.clj.

## Task 019 allocation divisor controls

Unequal-batch and empty-boundary controls pass on both pinned runtimes; see
`munera/closed/019-space-time-performance/evidence/allocation-divisor/`.
Allocation counter controls are complete. Full memory-tool approval still needs
physical value-box graph accounting; case-specific allocation remains unverified.

Task 019 now has pure metric normalisation in closed-record.bench.metrics.
It preserves raw totals, rejects invalid divisors and mixed outcome states,
and never converts missing metrics to zero. Benchmark regression tests pass
(19 tests, 197 assertions). Identity validation and aggregation remain open;
physical value-box accounting still gates full memory-tool approval.

Task 019 now validates partial metric result identities against the inventory,
including fork and graph root, and rejects duplicates across statuses. Temurin 21
benchmark tests pass (20 tests, 214 assertions). Complete-run coverage,
aggregation, and transport integration remain open; memory-tool gates are unchanged.

## Task 019 coverage validation

`metrics/validate-complete-results` now checks all inventory identities and
returns missing identities in exception data. Failed/unavailable rows count as
evidence coverage, not measurement success. Aggregation and transport remain
next; memory-tool approval remains open. Benchmark tests pass on OpenJDK 27
(21 tests, 249 assertions); pinned-runtime final verification remains required.

Task 019 reporting now includes equal-weight cross-fork summaries with sample
variation and preserved raw outcomes. Missing measurements suppress numeric
summaries. Transport integration and memory-tool approval remain open; see the
task steps and latest implementation entry.

Task 019 metric-table/render now validates complete raw evidence and emits
locale-independent Markdown with dimensions, roots, units, and fork failure
reasons. Missing summaries have no numeric cells. Temurin 21 benchmark tests
pass (23 tests, 279 assertions). Transport and memory-tool gates remain open.

## Task 019 — Raw metric transport

Pure `bench.transport` now validates schema-version 1 EDN envelopes against the
parent run/revision/configuration/fork association and the inventory. It retains
raw outcomes, including partial failure evidence. Temurin 21 benchmark tests:
24 tests, 296 assertions, all pass; focused format/lint/diff checks pass.
Next: durable manifests and real process-failure capture. Legacy S-03 and memory
tool approval are unchanged. See task 019 implementation notes and contract.

## Task 019 durable manifest subset

Run manifests now persist inventory, caller metadata, and validated planned
child associations via `closed-record.bench.evidence/create-run!`. Existing run
directories are never reused. Child capture and required metadata enforcement
remain open; this does not close transport or memory-tool gates.

Benchmark evidence capture now preserves supplied child output, raw EDN, and
process exits before envelope validation in exclusive manifest-indexed slots.
Envelope validity is independent of process exit and complete metric coverage.
Real process launch and result-file integration remain separate work.

Benchmark fork capture now runs real children with exclusive process directories
and absent absolute result paths. File redirects preserve streams without pipe
deadlocks. Raw files and exit survive later envelope failure. Launch/read errors
still propagate; structured outcomes and metadata enforcement remain open.

Task 019 launch IO failures now return and persist structured child-associated
outcomes in process directories, without invented exit or validation results.
Real missing-executable and retry-preservation tests pass. Result-read failures
and required runtime/command metadata validation remain next; see task steps.

## Task 019 result-read failure capture

Result-file IO failures now persist child association and completed exit with
`:stage :result-read`; no validation is invented. Real process regression tests
pass on Temurin 21 (29 tests, 406 assertions). Required runtime/command metadata
validation remains next. Stream-read and storage-write errors still propagate.
Memory-tool approval and acceptance measurements remain open.

Benchmark run manifests now require a command vector and runtime declarations
for exactly the planned configurations (version, Java home, explicit flags).
Validation precedes filesystem writes. These are declarations, not verified
child observations; generated-command validation and acceptance provenance
remain open. Task 019 benchmark tests pass on Temurin 21: 30 tests, 435 assertions.

## Task 019 generated command validation

Child command structure is checked before process-slot creation. Empty arguments
are valid; malformed vectors, blank executables, and NULs fail without consuming
the slot. OpenJDK 27 benchmark tests pass (31 tests, 461 assertions). Child runtime
metadata verification remains next; memory-tool and acceptance gates stay open.

## Task 019 runtime observation contract

Benchmark runtime collection and pure exact reconciliation now cover runtime
build, Java home, and ordered JVM flags. Current-process tests pass; durable
capture integration and real child JVM checks remain next. Declarations must
already be validated. No child provenance or acceptance claim is established.

## Task 019 runtime capture

Durable child outcomes now include separate `:runtime-validation` against the
manifest declaration. Child envelopes must emit `:runtime` observations; missing
observations cannot pass. Real JVM tests pass on OpenJDK 27. Measured-child
integration remains open; see task 019 steps and implementation notes.

Task 019: `evidence/successful-child?` now combines exit, envelope, runtime,
and IO-failure checks. It does not establish metric coverage. Measured-child
integration remains open. Benchmark regression tests pass on OpenJDK 25.0.2
(35 tests, 512 assertions); no acceptance measurements ran in this pass.

## 2026-10-27 — Child envelope writer

- `child/write-results!` validates before file creation, observes the actual JVM,
  and refuses overwrite. Real child runtime-capture tests now use this writer.
- Benchmark tests on OpenJDK 25.0.2: 36 tests, 519 assertions pass; exit 0 and
  no Scry failures. Focused format and lint pass. No dependencies changed.
- Next connect measured call sites and the success gate. Memory-tool gates and
  acceptance runs remain open; test envelopes do not establish measurements.

Task 019: `bench.run-results/assemble` now gates raw run results on child
success, association, and complete inventory coverage. Saved-capture reads and
measured-call-site integration remain open. Benchmark tests: 37/524 pass.

Task 019 saved-run follow-up: `run-results/read-results` now loads manifest-ordered
captures and enforces complete-run assembly without changing evidence. Missing
capture files throw. Measured call-site integration and memory-tool gates remain
open. Benchmark tests: 38 tests, 533 assertions pass on OpenJDK 25.0.2.

Task 019 saved reports: `run-results/render-saved` returns gated Markdown without
writing evidence. Real-file tests cover normalization, unavailable metrics, and
process failure. Temurin 21 benchmark tests pass (39 tests, 544 assertions).
Measured call sites and memory-tool approval remain open.

## Task 019 — S-03 regression boundary

Exploratory table regression now checks large timing differences without parity
labels. Original ratio/interval and field-declaration tests remain unchanged.
Temurin 21 focused Scry verification: 52 tests, 592 assertions, no failures.
Measured-call-site integration and memory-tool approval remain open. See task
019 steps and implementation notes; no acceptance measurement ran.

## Raw benchmark timing boundary

`closed-record.bench.timing/outcome` preserves Criterium bootstrap mean batch
time and its actual operation divisor as validated raw metric evidence. This
is an estimate, not an individual sample. Measured call sites must use it
before per-call reduction; integration remains open in task 019.

Task 019 capture IO boundary: real child tests now cover both stream-read errors
and exclusive capture-directory write conflicts with raw artifacts retained.
Benchmark suite: 42 tests, 602 assertions pass on OpenJDK 25.0.2. Measured-child
integration and memory-tool approval remain open.

Task 019 real child identity checks reject unknown case, configuration, and
representation IDs even with valid EDN and exit zero. Raw evidence is preserved.
Temurin 21 benchmark tests pass (43 tests, 626 assertions). Command-level exit
semantics and measured-call-site integration remain open.

Task 019 saved-run regressions reject missing/mismatched runtime observations
through result reading and reporting without changing evidence. OpenJDK 25.0.2
benchmark tests pass: 43 tests, 636 assertions. Measured-call-site integration
and memory-tool approval remain open.

Task 019 now has `closed-record.bench.saved-report`, a read-only CLI for saved
runs (0 = report emitted, 2 = rejected evidence/arguments/IO). It does not run
measurements or certify acceptance. Real JVM regression checks pass; measured
call-site integration and memory-tool approval remain open.

Task 019 raw allocation adaptation now preserves before/after counters and the
unadjusted delta/divisor. Invalid intervals and zero-operation outcomes fail.
Collector integration and memory-tool approval remain open; benchmark tests
pass (45 tests, 678 assertions) on OpenJDK 25.0.2.

## Task 019 raw graph adapter

`closed-record.bench.graph/outcomes` now preserves source/result/union counts
and derives replacement graph roots for transport. Benchmark regressions pass
(46 tests, 745 assertions). Measured collector integration and physical value-box
accounting remain open; no acceptance measurements ran.

## Task 019 allocation counter access

Benchmark-only `allocation-counter` now provides setup and same-thread primitive
readings. Real JVM tests pass; measured batches and memory-tool approval remain
open. See task 019 implementation notes for verification and the next boundary.

## Task 019 ordinary assoc allocation collector

`assoc-allocation/collect!` retains every result in a preallocated root array and
returns unadjusted raw allocation evidence. Real inventory/counter tests pass.
Warmup, budgets, overhead characterization, and child integration remain open;
no acceptance run or full memory-tool approval occurred.

Task 019 latest slice: assoc allocation has a separate loop/store/counter boundary
collector (`collect-boundary!`). It retains source identities and raw readings;
never subtract the control from assoc counts. Real inventory tests pass. Warmed
child evidence, batch budgets, and memory-tool approval remain open.

Task 019 now has explicit fixed warmup orchestration in
`assoc-allocation/collect-warmed!`. Raw warmup outcomes and final boundary counts
remain separate; final roots remain available for validation. Targeted Scry:
5 tests, 753 assertions pass. Child persistence, convergence evidence, and budgets
remain open. Format check passes; lint reports the existing duplicate require in
`dev/acceptance/api_content_defect.clj`.

Task 019 warmed allocation rows now retain raw controls under
`:allocation-controls` and omit object roots. Real child-writer EDN round trips
pass. Benchmark tests: 54 tests, 1579 assertions. Measured process orchestration,
stabilization budgets, and memory-tool approval remain open.

Task 019 now captures actual assoc allocation rows from independent child JVMs
through `assoc-allocation-child/write-results!`. Tests verify alternating pair
order and saved controls with runtime reconciliation. Explicit test budgets are
not acceptance defaults; manual matrix orchestration and stabilization remain
open. Benchmark tests pass: 55 tests, 1618 assertions on OpenJDK 25.0.2.

Task 019 allocation child now has a CLI with explicit positive decimal budgets.
Real capture tests launch it through `clojure.main -m`; bounded parent command
and stabilization budgets remain next. Benchmark tests pass (56 / 1638) on
OpenJDK 25.0.2. No acceptance run or memory-tool approval.

Task 019: allocation parent command builder now preserves parent JVM flags;
real child capture tests verify runtime reconciliation. Parent matrix orchestration
and stabilization budgets remain open; no new acceptance measurements.

Task 019 now has `assoc-allocation-run/collect!` for five manifest-backed ordinary
allocation children. Explicit budgets and controls persist; success is partial
evidence, not acceptance. Next: manual CLI and stabilization budget evidence.
OpenJDK 25.0.2 benchmark tests pass (58 tests, 1673 assertions).

## 2026-10-27 — Task 019 manual allocation command

- Added `closed-record.bench.assoc-allocation-cli`; smoke usage and limits are in
  task features.md. Five children, explicit budgets, exclusive run directory.
- Targeted real-JVM tests pass (2 tests, 43 assertions) on OpenJDK 25.0.2;
  focused format and lint pass. No acceptance run or design change.
- Next: retained stabilization evidence and budget selection. Partial CLI success
  is not complete-matrix acceptance or memory-tool approval.

Task 019 now retains a five-child Temurin 21 ordinary allocation diagnostic in
`evidence/assoc-budget-diagnostic`. Candidate budget: 10,000 operations and 20
warmup batches; all rows were unchanged after batch 1. This is partial evidence,
not acceptance. Full matrix integration and memory-tool gates remain open.

Task 019 allocation-stability summaries now validate fixed-budget raw controls
and report all transitions plus trailing warmup matches, without overhead
subtraction or convergence claims. All 20 retained diagnostic rows confirm 19
trailing matches. Temurin 21 benchmark tests pass (60 / 1703). Full-matrix
reporting integration and memory-tool approval remain open.

Task 019 latest slice: allocation stability now reports boundary transitions and
trailing warmup matches separately from assoc counts. Targeted Scry tests pass
(4 tests, 104 assertions); full-matrix integration and memory-tool gates remain
open. See the task implementation notes for the handoff.

## Task 019 — Saved allocation controls

Saved complete-run reports now append raw per-fork assoc/boundary controls with
independent transitions and trailing matches. Invalid supplied controls reject
reporting; absent controls do not imply stability. Full measured-matrix integration
and memory-tool approval remain open. See task 019 implementation notes.

Task 019 partial allocation success now requires complete ordinary assoc rows
and consistent raw warmup controls, both before child publication and after saved
capture reading. General transport remains partial-evidence capable. Targeted
real-JVM tests pass (5 tests, 128 assertions); full matrix and memory gates remain open.

Task 019 latest slice: partial allocation parent checks saved operation/warmup
budgets against manifest metadata before success. Targeted Scry: 6 tests, 135
assertions pass on OpenJDK 25.0.2. Full matrix and memory-tool approval remain open.

Task 019 saved allocation diagnostics now have a read-only validated reader in
assoc-allocation-run. Parent success shares its scope/fork/association and budget
gates. Real JVM/file tests pass (3 tests, 64 assertions). Full matrix integration
and memory-tool approval remain open.

Task 019 saved allocation reading now rechecks raw runtime observations against
manifest declarations rather than relying only on prior capture status. Real-file
runtime mutation tests pass (3 tests, 78 assertions). Full matrix integration
and memory-tool approval remain open.

Task 019: complete saved-run reading and rendering now revalidate raw runtime
observations against saved declarations, as the partial allocation reader does.
Targeted tests pass (5 tests, 85 assertions, OpenJDK 25.0.2). Full measured matrix
and memory-tool approval remain open; no acceptance measurement ran.

Task 019 saved-evidence update: `evidence/read-run` rejects missing or unsupported
manifest schema versions before capture reads. Version 1 remains supported.
Targeted evidence and both saved-reader suites pass (13 tests, 282 assertions).
Full measured matrix and memory-tool approval remain open.

Task 019 saved manifests now reuse creation checks for child plans and required
metadata before capture reads. Missing declarations fail before runtime comparison.
Targeted real-file/reader tests pass (14 tests, 315 assertions); measured-matrix
integration and memory-tool approval remain open.

Task 019: `assoc-allocation-run/render-saved` now renders validated partial
allocation controls read-only. This diagnostic is not the complete-matrix report;
its command and limits are in the task features.md. Targeted tests pass (3 tests,
99 assertions on OpenJDK 25.0.2). Full matrix and memory-tool gates remain open.

Task 019 now exposes validated partial allocation reports through
`closed-record.bench.assoc-allocation-report` (0 report success, 2 rejection).
Real JVM tests verify output and unchanged evidence. Full matrix integration
and memory-tool approval remain open.

Task 019 now has four literal ordinary assoc timing sites in `assoc-timing`.
Real Criterium smoke tests live in `test-bench`, outside ordinary discovery;
features.md records the command that clears default bench main options. Run once
per fresh JVM. Durable timing-child integration and memory-tool gates remain open.

Task 019 timing child persistence now connects the literal collector to the
validated envelope writer. The isolated real-Criterium smoke test verifies saved
raw rows and actual runtime metadata (1 test, 29 assertions). Independent process
capture and complete matrix integration remain open; no acceptance run occurred.

Task 019 latest slice: timing child CLI now has real fresh-JVM capture coverage
with inherited flags and explicit seconds. One-second smoke budget passes; it
is not acceptance evidence. Next: timing parent command and budget provenance.
Full matrix and memory-tool approval remain open.

Task 019 latest slice: added the pure timing parent command builder; targeted
Scry tests pass (14 assertions). Real timing process integration exposed Criterium
`collect/batch-size` divide-by-zero at a one-second budget. Integration test edit
was reverted; investigate this failure before relying on timing smoke runs.
See task implementation notes. Budget provenance and full matrix remain open.

Task 019 latest slice: deterministic timing-tool diagnostic confirms zero initial
duration fails pinned Criterium batch sizing. The same duration is a second
sample-budget divisor; do not fix only batch-size or retry away failures. See
task plan and implementation notes. Tool correction and original-case checks
remain open; no dependency or acceptance evidence changed.

## Task 019 timing candidate screen

Released Criterium 0.5.253-ALPHA has byte-identical collection sources to the pin
and fails the same zero-estimate boundary. Do not upgrade for this fault.
Task evidence/timing-tool-candidate retains the real diagnostic and hashes.
Next inspect a corrected upstream source or supported collection extension;
one-shot sampling does not satisfy warmed batch statistics. Dependency unchanged.

Task 019 timing follow-up: upstream Criterium HEAD `4e91647` has the same
collection source bytes as the pin. No built-in warmed-plan estimator override
exists. See task evidence `timing-tool-candidate/upstream.md`; next evaluate an
isolated traceable correction before six-case smoke tests or dependency changes.

Task 019 now retains an isolated Criterium planning-floor patch and passing
boundary tests in `evidence/timing-source-correction`. No dependency changed.
Next verify original six cases and fresh timing-child capture with the overlay
before adoption. Subsequent minimum estimation already had a one-ns floor;
the initial estimate lacked it. Raw samples remain unchanged.

Task 019 timing candidate now passes original six-case and fresh timing-child
smoke checks on Temurin 21 (26 and 10 assertions), first attempts. Evidence is
in timing-source-correction. Dependency remains unchanged; next choose reproducible
benchmark-only packaging and verify reporting regressions before adoption.

Task 019 timing candidate now has checksum-verified opt-in packaging in
`dev/bench/timing/prepare.sh`; default dependencies remain unchanged. Packaged
boundary tests and ordinary reporting regressions pass on Temurin 21. Next
verify packaged real smoke runs and tool provenance before default adoption.

Task 019 packaged timing correction passes first-attempt original and child smoke
checks on Temurin 21 (26 and 10 assertions). Resource URLs verify overlay loading;
evidence/timing-packaged-smoke retains results. Default adoption still needs
explicit correction provenance. Full matrix and memory-tool gates remain open.

Task 019 timing child rows now retain observed collector source identity and local
patch identity. Real packaged-overlay child persistence and upstream identity tests
pass on Temurin 21. Parent reconciliation and legacy S-03 provenance remain before
default adoption; dependencies and memory-tool gates are unchanged.

Task 019 timing-tool now validates portable parent declarations and reconciles
child identities without Criterium. Resource paths are excluded from equality;
known source and correction metadata must agree. Pure tests pass (2/22). Parent
manifest/saved-read integration and legacy provenance remain next.

Task 019 latest slice: optional `:timing-tool` manifest declarations are validated
on creation and saved read. Timing-specific required declarations and child-row
reconciliation remain next; default correction adoption is still open. Evidence
Scry suite passes (7 tests, 165 assertions); formatting and focused lint pass.

## Saved timing provenance gate

Complete saved-run reads and reports now require matching manifest/row tool
identities for measured timing outcomes. Raw capture still preserves failures.
Prelaunch timing declarations, partial timing integration, and legacy S-03
provenance remain open before correction adoption. Task 019 design is unchanged.

## Task 019 — Timing budget provenance

Ordinary timing child rows now retain canonical `:timing-budget` seconds.
Pure validation and real packaged-overlay child capture pass. Next connect parent
budget declarations and partial saved-read reconciliation. Tool adoption, full
matrix integration, and memory gates remain open; no acceptance run occurred.

Task 019 latest slice: pure `assoc-timing-result/validate-results` now checks
partial ordinary timing coverage and tool/budget provenance. Targeted Scry passes
(1 test, 11 assertions). Next connect saved timing reads and parent success;
full matrix and memory-tool gates remain open. No acceptance measurements ran.

Task 019 saved partial timing reader now validates five ordered ordinary forks
and raw process/runtime/tool/budget/coverage evidence without writes. Targeted
Scry tests pass (2 tests, 43 assertions). Parent orchestration and prelaunch
declarations remain next; default correction adoption and memory gates stay open.

Task 019 timing parent now persists explicit tool/budget declarations, launches
five children, and validates saved partial rows before success. Ordinary Scry
passes (2 tests, 40 assertions); first packaged-overlay five-child smoke passes
(1 test, 9 assertions). Next expose the manual timing CLI; legacy S-03 provenance,
default correction adoption, full matrix, and memory gates remain open.

Task 019: partial timing parent CLI is documented and tested with five real
children on Temurin 21 and the packaged overlay. Exit zero is partial diagnostic
success only. Next: legacy S-03 tool provenance and correction adoption; timing
acceptance budgets and full matrix remain open.

Task 019 latest slice: pure `legacy-transport` now carries original S-03
statistics and validated collector identity. Commands still use the old transport;
next connect child publication and parent reconciliation. Targeted transport/report
Scry tests pass (8 tests, 48 assertions). No correction adoption or acceptance run.

## Task 019 — Original transport connected

Original S-03 child and parent now use tool-aware envelopes. `fork/parent` takes
an explicit collector declaration; old vectors and mismatched identities fail.
Focused tests pass (15 tests, 88 assertions). Next verify the actual original
measured child with the packaged overlay before default adoption. No measurement
or dependency change in this pass; full matrix and memory gates remain open.

## Task 019 — Original child provenance verified

Actual original child CLI passes with packaged timing correction on Temurin 21;
all six rows and correction identity retained in evidence/original-child-provenance.
First attempt, exit 0; independent transport validation passes. Regression Scry:
15 tests, 88 assertions. Next reproducible default adoption, not acceptance claims.
Full matrix and memory-tool gates remain open; no dependencies changed.

Task 019 now documents `sh dev/bench/timing/run.sh` as the default timing path.
It prepares a fresh verified correction overlay and forwards JVM command exits;
real-process tests pass on Temurin 21. Direct :bench invocation still selects
upstream source. Acceptance budgets, full matrix, and memory gates remain open.

Task 019 now renders saved partial timing diagnostics through
`assoc-timing-run/render-saved`, after all saved-reader gates. The report uses
equal-fork mean/range/sample SD and does not claim full-matrix acceptance or
S-03 parity. Temurin 21 targeted tests pass (2/60). Manual report CLI and full
matrix integration remain open; memory-tool gates are unchanged.

Task 019 saved partial timing reporting now has a documented `assoc-timing-report`
CLI without measurement dependencies. Real-process output/error/preservation tests
and saved-reader tests pass on OpenJDK 25.0.2 (3 tests, 80 assertions). Exit 0 is
report success only. Full measured matrix, acceptance budgets, and memory-tool
approval remain open; no measurements ran in this slice.

Task 019 complete saved reporting now reconciles measured timing budgets with
manifest measurement declarations. Real-file mutation tests pass (7 tests, 123
assertions); unavailable timing outcomes remain exempt. Complete orchestration
must persist the declaration. Acceptance budgets and memory-tool gates stay open.

Task 019 complete saved readers now validate supplied allocation controls and
reconcile operation/warmup budgets with manifest measurement declarations before
reporting. Absent controls remain allowed, not stability evidence. Real-file
Scry tests pass (8/147); complete orchestration and memory-tool approval remain open.

Task 019 ordinary `assoc-graph/collect!` now emits raw inventory graph rows using
RecordGraphProbe. Load only with the compiled probe and agent; ordinary tests
remain isolated. Real-agent Temurin 21 regression passes (1/29). Durable child
capture and agent provenance are next; full memory-tool approval remains open.

Task 019 graph child persistence now writes both ordinary widths through the
exclusive envelope writer. Agent-only Scry passes on pinned Temurin 21 (2/46).
Independent child capture and verified agent identity remain next; runtime flags
alone are not provenance. No acceptance measurement or memory-tool approval.

Task 019 graph child now exposes a four-argument CLI, tested through independent
instrumented JVM capture (Temurin 21: 2 tests, 37 assertions). Agent identity
verification remains next; runtime flags alone do not establish provenance.
No acceptance measurement ran.

Task 019 graph command construction now has an agent-independent builder and
real instrumented capture coverage. Agent provenance remains required before
graph parent orchestration. See the task's latest implementation entry.

Task 019 now has isolated record graph agent preparation with pinned source,
compiler and binary checksums, and repeatable archive timestamps. Real Temurin 21
build/rejection/agent-loading tests pass. Loaded-agent observation and parent
reconciliation remain next; build identity alone is not runtime provenance.

Task 019 latest slice: active graph-agent identity returns JAR/class resource
hashes and origin; real shell tests pass on available JDK 27 with Java 21 target.
Next attach identity to child rows and reconcile retained build hashes. Resource
hashes do not detect transformed classes. Full matrix and memory gates stay open.

Task 019 graph child rows now retain active `:graph-tool` JAR/class hashes and
location. Use the prepared agent JAR on the classpath, not loose classes.
Real-agent child tests pass (2 tests / 40 assertions, JDK 25.0.2). Next reconcile
declared build hashes before graph parent success; no acceptance run occurred.

Task 019 now has pure portable graph-agent hash reconciliation without agent
loading. It validates both artifact hashes, excludes locations from equality,
and preserves observations. Next connect verified build declarations to manifests
and saved graph reads; syntax validation alone does not approve a build. Targeted
Scry passes (2 tests, 69 assertions); no acceptance measurements ran.

Task 019 optional graph-tool manifest declarations now validate before creation
and saved capture loading. Real-file Scry passes (8 tests, 178 assertions).
Next require verified build declarations and reconcile saved graph observations;
optional hash validation is not collector approval. No acceptance measurements.

Task 019 complete saved reads/reports now reconcile measured graph JAR/class
hashes against manifest declarations. Real-file tests pass (9/171, JDK 25.0.2).
Next connect verified build declarations and partial graph parent validation;
full matrix and memory-tool approval remain open. No acceptance run occurred.

Task 019 partial ordinary graph validation now checks complete roots, measured
states, matching agent hashes, and cross-root raw count consistency without
loading the agent. Targeted Scry passes (4/155). Next connect verified build
output and saved partial graph reads; full matrix and memory-tool gates remain open.

Task 019 partial graph saved reader now validates five ordered ordinary forks,
diagnostic scope, process/runtime observations, coverage, counts, and declared
agent hashes without loading instrumentation. Real-file Scry passes (1/32).
Next obtain declarations from verified preparation and connect graph parent
success. No acceptance run; full matrix and memory-tool gates remain open.

Task 019 now reads prepared graph declarations through graph-build/read-declaration:
checksums, pinned source, and packaged class must agree. Real compiler tests are
isolated in test-bench (Scry 3/82 with portable identity regressions). Next connect
this reader before graph parent launch and persist its declaration. No acceptance
run or memory-tool approval; artifact consistency does not establish compiler trust.

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

Task 019: saved partial graph reports now validate all five forks before rendering
separate graph roots. See the task implementation log. Full-matrix integration,
acceptance budgets, and memory-tool approval remain open.

Task 019: `assoc-graph-report` now provides the read-only graph diagnostic CLI.
Real JVM/file tests pass (2 tests, 72 assertions). Full-matrix integration and
memory-tool approval remain open; see task implementation.md for handoff.

Task 019 ordinary diagnostic composition now has a pure manifest compatibility
boundary. Use it only after existing saved readers validate each run. Separate
metric flags are allowed, but timing agents are rejected. Next connect readers
and retain all source manifests; no combined measurement or acceptance claim.

## Task 019 — Saved ordinary suite composition

`assoc-suite/read-results` now validates three saved diagnostics and their
compatibility, preserving metric-keyed manifests and raw results. It reads
completed evidence, not concurrently changing files. Next render the combined
ordinary report; no full-matrix or acceptance gate is closed. Focused mock-free
Scry passes (2 tests, 33 assertions), with formatting and lint checks passing.

Task 019 latest slice: `assoc-suite/render-saved` combines validated ordinary
metric rows and allocation controls, retaining all three source manifests.
Real-file Scry passes (2 tests, 47 assertions); focused format/lint pass.
Next expose the combined report CLI. No measurements or acceptance gates changed.

Task 019 now exposes `closed-record.bench.assoc-suite-report` for three saved
ordinary runs, ordered time/allocation/graph. All reader and compatibility checks
precede output. Real-process tests pass; report success is not acceptance.
Complete-matrix integration and experimental measurement remain open.

Task 019 now has pure matrix-budget accounting: 490 outcome slots and 170 timing
sites across five forks. The five-second limit sum is 850 seconds, not wall-clock
duration or acceptance approval. Tests cross-check complete coverage. Full matrix
orchestration, duration evidence, and memory-tool approval remain open.

## Task 019 ordinary measured attempt

Ordinary attempt 01 is retained under task 019 `evidence/ordinary-end-to-end`.
All three five-fork commands and combined reporting pass on pinned Temurin 21.
Memory observations are stable, but timing gives 0.029–0.064 ns/op and is not
approved. Next investigate measured expression/loop and batch statistics before
new collection; preserve this attempt. See its assessment and task checklist.

## Task 019 ordinary milestone complete

Attempt 02 retains valid five-fork time/allocation/graph evidence on Temurin 21.
The nested assoc macro generated a Cons that Criterium factored to nil; explicit
list construction fixes it, with real measured-result regression. Attempt 01
remains invalid timing evidence. Next execute experimental end-to-end milestone;
see task steps and evidence/ordinary-end-to-end-02/assessment.md. Width-16 timing
variation and slower closed-record mean remain visible. No acceptance claim.

Task 019 experimental boundary: real Criterium batches do not reset arrays per
operation. Diagnostic and reproduction are in the task's
`evidence/experimental-store-boundary/assessment.md`. A reset-aware measurement
boundary remains required; this is not a support limit. Experimental collection
and reporting remain incomplete. Ordinary milestone remains complete.

Task 019 reset boundary now passes real Valhalla/Criterium checks (2 tests,
104 assertions). `valhalla.reset-measured` resets outside each separately timed
operation; timer/input costs remain. Do not use it for allocation. Experimental
collector integration, five-fork results, and reporting remain unfinished; see
task plan and latest implementation entry.

Task 019: reset-aware experimental stores also pass full Criterium collection
smoke (Valhalla, corrected overlay, 1 test/16 assertions). This checks diagnostic
operations, not acceptance timings. Experimental collection/report integration
remains open; see the task implementation log.

Task 019 latest: unchecked experimental operations are in
`dev/bench/valhalla/Operations.java`; direct and generated-expression Scry checks
pass on pinned Valhalla (4 tests/76 assertions). Experimental milestone remains
open: connect timing, separate memory outcomes, five forks, and reports. Do not
use checked StoreBoundaryProbe operations as acceptance timing expressions.

Task 019 experimental timing collector now passes real pinned-Valhalla Criterium
smoke for all nine pairs (2 tests/146 assertions). Stores retain per-operation
timer costs with untimed resets. Saved timing child metadata, separate memory
collection, five forks, and reports remain unfinished; no acceptance run occurred.

Task 019 latest slice: `valhalla.timing-child` saves all 18 experimental timing
rows with runtime/tool/budget provenance. Pinned Valhalla real-file Scry passes
1 test/81 assertions. Five-fork capture, separate memory outcomes, and saved
experimental reports remain next; milestone 2 is not complete.

Task 019: five-fork experimental timing attempt 01 now retains 90 raw rows and
validated timing report in `evidence/experimental-timing-01`. All processes pass,
but small flat reads/stores vary strongly and reference stores need timer-resolution
audit before acceptance. Experimental allocation/graph integration remains open.

Task 019 clock audit: pinned Valhalla single-store durations are mostly zero;
smallest positive durations are 41/42 ns. See task evidence/experimental-clock-audit.
Attempt 01 remains diagnostic. Resolve store boundary/error bounds and separately
read variation before acceptance; experimental memory integration remains open.

Task 019 latest: changed-store multi-array diagnostic passes on pinned Valhalla;
1,024-store batches avoid sampled zeros but change the working set. See task
`evidence/experimental-store-batch/assessment.md`. Not acceptance or an adopted
collector. Experimental memory integration and timing assessment remain open.

Task 019 latest slice: experimental `Allocation.sample` now exercises all 18
sites with real counters and checked escaping results. Single-JVM evidence:
`munera/closed/019-space-time-performance/evidence/experimental-allocation-boundary`.
Not acceptance. Next integrate allocation inventory/persistence and controls,
five forks, graph outcomes, and reports. Store timing quantisation remains open.

Task 019 allocation boundary controls now pass on pinned Valhalla: invalid sites
reject before setup, and both array layouts match physical stride allocation
deltas. See task evidence/experimental-allocation-controls. Experimental milestone
still needs holder controls, five-fork allocation persistence/reporting, graph
integration, and resolution of timing limits; no acceptance claim.

Task 019: holder allocation boundary controls now pass on pinned Valhalla for
construction/replacement in both layouts (16 bytes/op, independent shallow-size
check). Evidence: experimental-holder-allocation. This is not retained graph or
acceptance evidence. Next connect experimental allocation inventory/persistence
and five-fork reporting; timing quantisation and graph integration remain open.

## Task 019 — Experimental allocation capture (2026-10-28)

Five independent pinned Valhalla allocation children now retain 90 rows and a
saved report in experimental-allocation-01. Restricted-holder reads vary 0–24
bytes/op; flat-array reads allocate about 24. Preserve results and assess raw
batch variation before acceptance. Graph integration and timing audits remain.
Real-counter Scry passes 1 test/188 assertions. See task implementation log.

Task 019 saved allocation audit: holder-read counts are constant within each
fork but 0/24-byte differences align with pair order. Do not claim convergence
or rerun for zeros. Retain fixed budgets and disclose the limitation; all 90
rows were audited without recollection. Graph integration and timing limits
remain next. See experimental-allocation-01 assessment and audit.

Task 019 saved read timing audit is complete: small flat-array reads split by
pair order/divisor, unlike large arrays and holder reads. Cause is unknown;
retain every fork and disclose variation, not convergence. Existing report and
new audit reproduce byte-for-byte. Graph integration and store timing resolution
remain next; see experimental-timing-01 assessment. No new measurements ran.

Task 019 latest: experimental flat graph operation controls pass on pinned
Valhalla (11 checks). See evidence/experimental-graph-operations. Java Object[]
roots require an Object cast at the varargs graph boundary; failed attempt retained.
Holder read roots alias, so graph increment is zero. Five-fork graph integration
and store timing resolution remain open; no acceptance or completion claim.

Task 019: Graph.sample now implements supported flat experimental root accounting.
Real-agent checks pass for nine sites and eight invalid inputs; see evidence/
experimental-graph-boundary. Next connect graph inventory/provenance, unavailable
reference rows, five forks, and saved reporting. Experimental milestone and store
timing resolution remain incomplete; no acceptance claim.

## 2026-10-28 — Task 019 graph inventory rows

Experimental graph adapter now emits all 30 rows per fork, preserving unavailable
reference graphs and flat ownership counts. Real-agent Scry passes 1 test/69
assertions on pinned Valhalla. Next attach FlatGraphProbe provenance (not ordinary
RecordGraphProbe hashes), persist five children, and render saved results.
Milestone 2 and store timing resolution remain open. See task implementation log.

Task 019: experimental FlatGraphProbe active artifact identity now passes real
pinned-Valhalla checks (`dev/bench/valhalla/graph-identity-test.sh`). Its JAR must
precede loose classes. Next persist/reconcile that identity with graph rows and
finish five-fork graph reporting; milestone 2 and timing resolution remain open.

Task 019 graph capture is connected to a bounded five-fork script and saved
report with independent flat-agent hashes. Real-agent Scry passes (2/105).
Next execute the committed experimental-graph-01 command; timing remains open.

Task 019 experimental graph attempt 01 retains five successful children and 150
validated outcomes (75 measured, 75 unavailable). Report-only require failure is
retained; corrected report reuses original children. No further graph wiring is
needed. Next resolve experimental store timing and freeze acceptance limits.

Task 019: experimental diagnostic memory coverage is complete; see evidence/
experimental-memory-assessment.md. Existing allocation/graph validators reproduce
saved reports byte for byte (240 outcomes). Timing quantisation remains next;
acceptance and public reporting are still open. Do not reopen memory wiring.

Task 019 store timing: StoreBatch now implements the documented 1,024-array,
index-zero, slot-only-reset contract. Real pinned-Valhalla checks pass. Next wire
Criterium and retain both call/store divisors; no corrected timing capture yet.
See evidence/experimental-store-batch-contract. Memory coverage remains complete.

Task 019 store timing now uses StoreBatch (1,024 changed stores per invocation).
Raw timing rows preserve both divisors; `:operations` is their product. Pinned
Valhalla Scry passes 4 tests/196 assertions. Next compile StoreBatch in corrected
capture commands, verify traversal clock resolution, and retain five-fork timing.
Milestone 2 and acceptance remain open; old timing evidence is unchanged.

Task 019 actual StoreBatch clock diagnostic passes on pinned Valhalla with no
zero durations across four sites. Small-flat minimum is 208 ns; timer/quantisation
limits remain. See experimental-traversal-clock. Next corrected five-fork timing
capture and report; milestone 2 is still open. Memory integration stays complete.

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

Task 019 acceptance commands are frozen in
`munera/closed/019-space-time-performance/evidence/acceptance-01/README.md`.
No acceptance measurements have run. Next retain original S-03 raw children
(zero-based forks) and run the four frozen metric scripts; validate 490 matrix
outcomes. Base/test dependency isolation verified on Temurin 21. Preserve the
first complete attempt and all failures; diagnostic milestones are complete.

Task 019: first acceptance matrix captures are retained in acceptance-01. All
six metric commands and reports pass; no retries. Next run original S-03 with
retained child envelopes and field test, audit complete inventory, then publish.
See acceptance-01/assessment.md. Do not recollect completed matrix captures.

Task 019: original S-03 acceptance and field test now pass; evidence is in
acceptance-01/s03. Next audit all 490 matrix outcomes, then publish the page
and run final checks. Do not repeat completed measurements.

Task 019 inventory audit passes: 430 raw outcomes plus 60 original statistics
cover 490 slots; 75 reference graph outcomes are evidenced unavailable. Do not
invent legacy raw batches. Next publish the performance page and run final checks;
no recollection. See acceptance-01/audit.md.

Task 019 public performance page now summarizes acceptance-01 and qualifies
README claims. Vale and tracked Markdown links pass. Next run final runtime and
benchmark suites, format/lint/API freshness, and acceptance review. Measurements
must not be repeated; reference graph limits remain explicit.

Task 019 implementation is complete pending review. Three Clojure suites pass
on pinned Temurin 21 (each 182 tests/2,770 assertions), and all shared quality
checks pass. Final verification repaired a missing test-fixture budget, one
formatting error, and an implicit-user-namespace lint collision. No measurements
changed. See task implementation.md for the six-criterion acceptance review;
do not repeat acceptance captures. Task remains in open/ for review.

## Test suite layout — 2026-10-27

All suites now live below `test/`: `runtime`, `build`, `release`, `tooling`,
and `bench`. Aliases keep separate source paths. Runtime CLI commands must use
`--dir test/runtime`; REPL calls use `(scry/run {:dirs ["test/runtime"]})`.
Default scry discovery scans the parent and is not suitable for this layout.
See README/development.md for current commands. Older knowledge pages and task
logs retain the previous paths; captured benchmark evidence is unchanged.
Shared format/lint scope now includes all suites through the `test/` root.
Verification on OpenJDK 25.0.2: runtime suites pass on Clojure 1.11/1.12/1.13
(each 182 tests/2,770 assertions); build 6/100, release 9/104, tooling 51/810,
and two focused benchmark support tests 2/19 pass. Format, lint, Vale, API
freshness, and offline Markdown links pass. Full measurement suites were not run.
