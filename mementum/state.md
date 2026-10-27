# State

Working memory. Read this file first in each session. Update it after each significant change.

## Project

closed-record: a `defrecord`-like record. It does not permit assoc of
undeclared keys. It does not permit dissoc of declared keys.
Main form: `defclosedrecord`.

## Status

- Repository initialised: README, AGENTS, psi extension config.
- Protocol directories initialised: ramora, mementum, munera.
- Design document: `ramora/defclosedrecord/` (status intended).
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

`closed-record.index-scope/paths!` validates required indexed paths and selects
owned Clojure/EDN files. It does not isolate working-tree content. Hook callers
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

- `.pre-commit-config.yaml` runs serial formatting before whole-index lint with
  fail-fast ordering. Internal `quality:format-staged` and `quality:lint-staged`
  tasks rely on pre-commit staged-content isolation; do not invoke them as
  independent staged-byte checks. Installed-hook tests use pre-commit 4.6.0.
  Clean commits, staged defects hidden by unstaged corrections, repair rejection,
  and manual restaging pass. Partial-stage tests also verify nonconflicting edit restoration and conflicting
  repair rollback, with unchanged index, HEAD, and unrelated tracked bytes.

- `bb clj-fmt check|fix [PATHS...]` and `clj-fmt:check`/`clj-fmt:fix`
  share the required scope and explicit `.cljfmt.edn`. The CLI file pattern
  includes EDN, including files found recursively. Check returns the tool exit
  without writing; fix is explicit.

- `dev/quality/closed_record/quality_scope.clj` owns required format/lint paths
  and Git-path selection. Validate required inputs before invoking tools.
  Dependency imports and caches are not owned inputs.
- `bb clj-kondo:copy-config` resolves independent reproducible dependency bases,
  then imports their union. Dependency imports are ignored and regenerated;
  required scope is checked before setup.
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
  repair command for absent or stale output. Invalid arguments fail without writes.
  Quickdoc uses the owned clj-kondo `:cljc {:features #{:clj}}` setting.
  Real-process divergent-branch tests verify JVM docstrings and arguments and
  exclude browser-only vars; without that setting Quickdoc selects browser content.
  `doc/API.md` is generated and linked from README. A real-process negative
  control proves freshness can pass while independent content tests fail. API docstrings describe
  constructor arguments, compiler option forwarding, error data, and literal-key
  loss through compilation and print/read round trips; content tests check these.

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
