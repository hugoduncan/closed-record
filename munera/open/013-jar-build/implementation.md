# Implementation notes

## 2026-10-23 — Architecture review

- No architectural review feedback. The scope is unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture context
  came from `AGENTS.md` and `ramora/defclosedrecord.md`, with its linked
  principles, embodiment, invariants, qualities, and platform decisions.
  Use these sources for later design-review turns.

## 2026-10-23 — Ambiguity review

- No ambiguity review feedback. The scope is unchanged.

## 2026-10-23 — Inconsistency review

- No inconsistency review feedback. The scope is unchanged.
- The scry reference generates an SCM tag `v<version>` and its core JAR
  builder does not copy the license. Do not copy these details: this task's
  explicit metadata and license contracts take precedence over the example.

## 2026-10-23 — Review handoff

- Verified license source: `~/projects/hugoduncan/scry/scry-master/LICENSE`
  contains EPL-2.0 text. Use this file, not the reference build's POM metadata,
  as the source for the license file.

## 2026-10-23 — Plan ambiguity review

- No new ambiguity review feedback on `plan.md` and `steps.md`.

## 2026-10-23 — Plan inconsistency review

- No new inconsistency review feedback across the task files.

## 2026-10-23 — Plan review handoff

- Checked the pinned scry `0.1.61` JAR, not only the reference checkout:
  `scry/cli.clj` supports `--dir`; `scry/core.clj` documents default discovery
  under `test`. The planned build-test directory needs no runner upgrade.
- For consumer error assertions, use the `:type` values in
  `src/closed_record/core.cljc` and the assoc/dissoc checks in
  `test/closed_record/defclosedrecord_test.clj`; both rejected operations
  throw `ExceptionInfo`, so the class alone does not distinguish them.

## 2026-10-23 — Slice 1: build and metadata

- Added tools.build 0.10.10 only in `:build`. Base paths, dependencies, and
  existing aliases are unchanged. Runtime source is unchanged.
- `version-from-result` is private pure validation; tests can use its Var.
  Git launch errors retain their cause. Version lookup precedes output writes.
- tools.build supplies the POM name from the artifact ID. An explicit name
  in `:pom-data` produced a duplicate; removed it and verified one name element.
  No design or approach change was needed.
- With temporary `CLJ_CONFIG` containing `{}`: `clojure -T:build clean`
  twice and `clojure -T:build jar` all exited 0. Version at verification was
  `0.1.132`; the next commit will increase it. JAR and POM are at planned paths.
- Direct Clojure assertions passed for numeric output with newline, failed
  exit, blank output, and non-numeric output. Errors include process data.
  Persistent automated coverage remains in slice 2.
- `jar tf` and POM inspection confirmed metadata, no SCM tag, and only
  Clojure 1.11.4 as a direct dependency. Python ZIP/XML assertions passed for
  the exact file inventory, source/license byte equality, embedded POM equality,
  version, dependency, and single name element. A second build removed a stale
  staging sentinel and obsolete JAR. No classes or bundled dependencies exist.
- `cmp LICENSE ~/projects/hugoduncan/scry/scry-master/LICENSE`,
  `git check-ignore target/probe`, and `git diff --check` succeeded.
- Next slice: add the planned build-test alias and persistent artifact tests,
  including no-Git failure and isolated consumer JVM coverage. No runtime suite
  was run in this build-only slice; full suite verification remains in slice 3.

## 2026-10-23 — Slice 2a: version failures and clean

- Added the separate build-test alias and two tests with real temporary
  files and CLI processes. No mocks or production changes. Design unchanged.
- tools.build 0.10.10 `b/process` requires `clojure.java.process`, absent in
  the base Clojure 1.11 runtime. Test subprocesses therefore use
  `clojure.java.shell/sh`; `-T:build` retains its separate tooling runtime.
- Temporary CLI configuration contains `{}`. The no-Git fixture sets
  `GIT_CEILING_DIRECTORIES` to prevent parent repository discovery and is
  deleted in `finally`. Failed version lookup creates no target directory.
- `clojure -M:test:build-test -m scry.cli --dir test-build`: exit 0,
  2 tests, 23 assertions, no failures/errors; `.scry-results` empty.
- `clojure -M:test -m scry.cli`: exit 0, 97 tests, 379 assertions,
  no failures/errors; `.scry-results` empty. Build tests are not discovered.
  Both commands used an isolated `CLJ_CONFIG`.
- Next: complete the remaining slice 2 archive, metadata, isolated consumer,
  and stale-output checks. No artifact version was generated in this pass.

## 2026-10-23 — Slice 2b: artifact contracts

- Added real two-build verification with stale staging and obsolete artifact
  sentinels. Exact archive file inventory excludes development files, classes,
  and bundled dependencies. Source, license, and generated POM bytes match.
- Parsed both POMs and Maven properties; coordinates, version, public metadata,
  license, no SCM tag, and base dependency equality pass. Design unchanged.
- With temporary empty CLI configuration:
  `clojure -M:test:build-test -m scry.cli --dir test-build` exited 0;
  3 tests, 62 assertions, no failures/errors; `.scry-results` is empty.
  Artifact: `target/closed-record-0.1.134.jar`; POM at the planned staging path.
- Next slice must add the isolated consumer JVM check. These tests do not yet
  prove macro expansion from the JAR. Documentation and version-matrix checks
  remain in slice 3. No runtime source or dependencies changed in this pass.

## 2026-10-23 — Slice 2c: isolated consumer

- Added a real consumer JVM to the artifact check. Its classpath uses only
  the built artifact and dependency paths from an alias-free, user-free basis.
  It runs in a temporary directory and verifies both resource URLs, macro
  expansion, construction, lookup, declared-key update, and distinct error
  types for rejected assoc and dissoc operations. No mocks or runtime changes.
- With temporary empty CLI configuration,
  `clojure -M:test:build-test -m scry.cli --dir test-build` exited 0:
  3 tests, 66 assertions, no failures/errors; `.scry-results` is empty.
  Artifact: `target/closed-record-0.1.135.jar`; generated POM remains at
  the planned staging path. The commit count changes after this commit.
- Design contracts and approach are unchanged. Slice 2 is complete; next is
  README documentation and the full Clojure version matrix in slice 3.

## 2026-10-23 — Slice 3: documentation and final checks

- README now documents the build interface, artifact paths, version rule,
  full-history requirement, and focused tests. Git installation and the
  no-Clojars-release status are unchanged. No design or approach change.
- All commands ran from the repository root with a temporary `CLJ_CONFIG`
  containing `{}`. `clojure -T:build jar` exited 0 and produced
  `target/closed-record-0.1.136.jar` and
  `target/classes/META-INF/maven/org.hugoduncan/closed-record/pom.xml`.
  The version matched HEAD before this commit; later commits increase it.
- `clojure -M:test:build-test -m scry.cli --dir test-build` exited 0:
  3 tests, 66 assertions, no failures/errors, empty `.scry-results`.
  Existing tests cover all artifact contracts; no new tests were needed.
- `git check-ignore target/probe` succeeded. `clojure -T:build clean`
  exited 0 twice; `target/` was absent afterward.
- Each command below exited 0 with 97 tests, 379 assertions, no failures or
  errors, and an empty `.scry-results` checked before the next run:
  - `clojure -M:test -m scry.cli` (Clojure 1.11.4).
  - `clojure -M:test:clj-1.12 -m scry.cli` (Clojure 1.12.6).
  - `clojure -M:test:clj-1.13 -m scry.cli` (Clojure 1.13.0-alpha9).
- Final review against all seven acceptance criteria found no remaining
  implementation work. Runtime source and API are unchanged. No install,
  deploy, release-tag, or CI machinery was added. The task remains open for
  implementation review; build output was removed by final clean checks.
