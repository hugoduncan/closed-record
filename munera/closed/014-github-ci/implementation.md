# Implementation notes

## 2026-10-23 — Architecture review (design-review, turn 1)

- No architectural review feedback. Scope unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture context:
  `AGENTS.md`, `ramora/defclosedrecord.md`, and its `principles.md`,
  `embodiment.md`, and `decisions/verification.md`. Tooling and artifact
  verification rules are in `mementum/knowledge/dev-tooling.md` and
  `mementum/knowledge/artifact-verification.md`.

## 2026-10-23 — Ambiguity review (design-review, turn 2)

- No ambiguity review feedback. Scope unchanged.

## 2026-10-23 — Inconsistency review (design-review, turn 3)

- No inconsistency review feedback. Scope unchanged.

## 2026-10-23 — Review handoff

- Command checks used `README.md` (Development) and `deps.edn`.
  `.clj-kondo/config.edn` maps `closed-record.core/defclosedrecord` to
  `clojure.core/defrecord`; retain this mapping when adding CI lint checks.

## 2026-10-23 — Ambiguity review (plan-review, turn 1)

- No new actionable ambiguity review feedback.

## 2026-10-23 — Inconsistency review (plan-review, turn 2)

- Inconsistency review added 1 new design step: correct the stale planning status in `munera/plan.md`.

## 2026-10-23 — Follow-up handoff

- `munera/plan.md` had uncommitted edits before review; preserve them when correcting the status. This finding concerns task navigation only, not CI scope or implementation readiness. No source or tooling changes are needed.

## 2026-10-23 — Plan-review follow-up verification

- `design.md` is present but untracked. It was read as context and left
  unchanged and unstaged; a clean checkout will not contain it until it is
  committed separately. The index link to `steps.md` resolves locally.
- Verified the index correction and checklist agreement with
  `git diff --check` and file checks. No CI commands ran for this navigation-only change.

## 2026-10-23 — Inconsistency review after follow-up (plan-review, turn 2)

- No new actionable inconsistency review feedback. The completed navigation
  correction agrees across the task index, plan, and checklists.

## 2026-10-23 — Shared plan-review evidence

- The inconsistency pass reused the ambiguity pass's loaded context after
  checking for tracked changes. Neither pass rechecked remote tool releases;
  the version-availability claim remains planning evidence, not new validation.

## 2026-10-23 — Slice 1: quality baseline

- Host: macOS, Darwin 27.0.0, arm64. Reported tools: Clojure CLI
  1.12.6.1673, cljfmt 0.16.4, clj-kondo 2026.04.15, and
  Temurin 21.0.10+7-LTS. Java 21 is already installed at
  `/Users/duncan/.local/share/mise/installs/java/temurin-21`.
  Set `JAVA_HOME` to this path and prepend `$JAVA_HOME/bin` to `PATH`
  for subsequent verification; the default Java is 25, not 21.
- `cljfmt check src test dev test-build build.clj` exited 1 before changes:
  only `test-build/build_test.clj` needed indentation changes.
  `cljfmt fix test-build/build_test.clj` exited 0; the full check then exited 0.
- `clj-kondo --lint src test dev test-build build.clj --fail-level warning`
  exited 0 before and after formatting (zero errors and warnings).
  No lint configuration or source correction was needed.
- On Java 21, `clojure -M:test:build-test -m scry.cli --dir test-build`
  exited 0. `.scry-results/` contained no files. Existing real Git, JAR,
  and isolated-consumer tests were retained; no mock or new test was needed
  for indentation-only changes.
- Diff review: `git diff -w -- test-build/build_test.clj` was empty;
  `git diff --check` exited 0. Runtime behaviour and dependencies are unchanged.
- Next slice: add the workflow as planned. Full matrix and failure-path
  verification remain in Slice 3. Hosted execution remains unverified.
  The pre-existing untracked `design.md` is included in this commit so the
  task contract is available in a clean checkout; its content is unchanged.


## 2026-10-23 — Slice 2: workflow

- Added the three independent jobs with the planned action/tool references,
  timeouts, alias commands, read-only permissions, and checkout settings.
  No design change or dependency change was needed.
- Downloaded actionlint 1.7.7 for Darwin arm64 from its release page to
  `/tmp/closed-record-actionlint.jow59G`. Its reported version is 1.7.7.
  `/tmp/closed-record-actionlint.jow59G/actionlint .github/workflows/ci.yml`
  exited 0 with no findings. `git diff --check` also exited 0.
- Static review confirmed default PR activity types without target filters,
  push-to-master, Java 21 on Ubuntu, all five quality paths, three exact
  runtime commands, disabled matrix fail-fast, and full build history.
  No secrets, write permission, error suppression, job dependencies,
  benchmark execution, dependency overrides, or publication steps were added.
- Uploads immediately follow test steps. They include hidden result files,
  use distinct names, and warn if files are absent. They cannot clear a
  failed test outcome. The JAR command retains the default success condition.
- No source or test changes were needed for this orchestration slice. Used
  the real workflow validator, not mocks or tests of source text. Full CLI
  verification and failure-path review remain in Slice 3; no test suite ran
  in this slice. Hosted event execution remains unverified; no branch push
  was made. The design remains unchanged.

## 2026-10-23 — Slice 3: verification and documentation

- Used the same macOS host and Temurin 21.0.10+7-LTS as Slice 1. Rechecked
  CLI 1.12.6.1673, cljfmt 0.16.4, and clj-kondo 2026.04.15.
- `cljfmt check src test dev test-build build.clj` and
  `clj-kondo --lint src test dev test-build build.clj --fail-level warning`
  both exited 0. Checked paths remained unchanged.
- Real temporary files outside the checkout verified failure behaviour:
  cljfmt check exited 1 for bad indentation; `cmp` exited 0 against the
  original bytes. The first lint probe also had a namespace/file mismatch
  (exit 3). Corrected the fixture name and reran: zero errors, one unused
  binding warning, exit 2 with `--fail-level warning`. Removed both fixtures.
- Java 21 CLI results (each exited 0):
  - `clojure -M:test -m scry.cli`: 97 tests, 379 assertions.
  - `clojure -M:test:clj-1.12 -m scry.cli`: 97 tests, 379 assertions.
  - `clojure -M:test:clj-1.13 -m scry.cli`: 97 tests, 379 assertions.
  - `clojure -M:test:build-test -m scry.cli --dir test-build`: 3 tests,
    71 assertions.
  Each run had zero failures/errors. Inspected `.scry-results/` immediately
  after each command: no files. No failure details were scraped from progress
  output. Existing real build fixtures and consumer isolation are unchanged.
- `git rev-parse --is-shallow-repository` returned `false`;
  `git rev-list --count HEAD` returned 170. `clojure -T:build jar` exited 0
  and produced `target/closed-record-0.1.170.jar`, matching that count.
- Static upload review: failed test steps with files upload hidden results
  under distinct matrix/build names. Without files, upload warns; the test
  failure remains. No step suppresses failure. A JAR-only failure has no
  test upload: the upload precedes the JAR step and requires a failed test.
  This is static evidence, not a hosted failure-path test.
- Updated README, D-16, artifact map, and tooling knowledge. No design or
  approach change was needed. No production code, dependencies, or tests
  changed in this slice; real CLI probes covered the new failure checks.
- Acceptance review: workflow meets event, permission, setup, timeout,
  matrix, full-history build, and failure-artifact contracts. Benchmarks
  remain manual; no publication or branch-protection changes. All local
  commands passed. actionlint 1.7.7 and `git diff --check` exited 0.
- `gh run list --repo hugoduncan/closed-record --commit 18643df` (using the
  full commit SHA) returned an empty list, exit 0. Hosted event execution
  remains unverified, including Ubuntu and fork PRs. No push was made.
- Next review: implementation is complete; keep the task open for review.
  The design remains the acceptance contract. Build version evidence refers
  to the pre-documentation commit, not the next commit's count.

## 2026-10-23 — Implementation review

- No new actionable issues. Fresh cljfmt, clj-kondo, and actionlint checks
  exited 0. Test suites were not rerun; hosted execution remains unverified.

## 2026-10-23 — Test review

- Added 0 follow-up steps. Test suites were not rerun; hosted failure uploads
  remain unverified.

## 2026-10-23 — Documentation review

- No new actionable documentation issues.

## 2026-10-23 — Code-shaper review

- No new actionable code-shape issues. cljfmt, clj-kondo, and actionlint
  exited 0. The build-test baseline diff contains only whitespace changes.

## 2026-10-23 — Closure

- Closed at the user's request after implementation and review completed.
  All implementation steps are complete. This closure supersedes the earlier
  note to keep the task open for review.
- Hosted event execution, fork-PR behaviour, and failure uploads remain
  unverified. Closure does not change these verification limits.
- Moved the task to `munera/closed/014-github-ci/` and removed its entry from
  `munera/plan.md`. No code changed; tests were not rerun for closure.
