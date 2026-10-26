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

## Continuous integration

- `.github/workflows/ci.yml` defines read-only checks for pushes to `master`
  and pull requests: code quality, three Clojure runtime versions, and separate
  build verification on Ubuntu with Temurin 21. Failed tests upload hidden
  `.scry-results/` files. Local Java 21 checks passed for all three runtime
  aliases, focused build tests, and JAR packaging; quality checks and
  actionlint passed. Hosted execution is not yet verified.

## Release architecture

- Intended architecture: `ramora/release-workflow.md`. Follow scry's build.clj:
  check credentials, build the one JAR/POM, call stock deps-deploy, return paths.
- Manual master dispatch, default dry run, one full-history dispatch-SHA checkout,
  one job, checks before writes, and step-scoped secrets. Deployment may rebuild
  from that checkout. GitHub assets use the deployment outputs.
- Intended changelog contract: exact Unreleased/dated version headings, unique
  versions, nonblank dated bodies, and shared validation before preparation or
  note selection. Body Markdown is opaque; empty Unreleased is valid unless used.
- Use ordinary Git tag push and `gh release create --verify-tag --notes-file`.
  Standard tool-internal retries, redirects, checksum handling, and temporary
  draft cleanup are accepted. No custom transport or remote-absence proof.
- All task-015 implementation is reverted to baseline `c9066bd`: no release or
  deployment source/tests, changelog, or release/deploy aliases remain. Packaging
  docs also match the baseline. Build the small path without restoring that
  module hierarchy; the architecture above is intended, not implemented.
- Existing packaging remains in `build.clj`. No release has been published by
  this work. The new path will add stock deps-deploy 0.2.2 outside runtime deps.
- Historical transport findings are in the closed release task's implementation
  log. They describe library behaviour, not active release blockers. For current
  work and acceptance, follow `munera/plan.md`.

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
- README Development documents build, clean, version/history requirements,
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
