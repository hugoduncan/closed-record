# Plan: GitHub CI

## Approach

Add `.github/workflows/ci.yml`. Use three independent job definitions:
`quality`, `test` (three matrix entries), and `build`. Do not add job
dependencies, a task runner, caches, or reusable workflows. Run commands
from the repository root on `ubuntu-latest` with Temurin 21.

Use `push.branches: [master]` and an unfiltered `pull_request` event with
its default activity types. Set workflow permissions to `contents: read`.
Set `persist-credentials: false` on each checkout. Do not pass secrets or
add a token environment variable. The setup action can use its default
read-only GitHub token. Set job timeouts to 15 minutes for quality,
20 minutes for each runtime entry, and 30 minutes for build verification.

### Selected versions

| Component | Version or action reference |
|---|---|
| Java | Temurin 21 |
| Clojure CLI | `1.12.6.1673` |
| clj-kondo | `2026.04.15` |
| cljfmt | `0.16.4` |
| Checkout | `actions/checkout@v4.2.2` |
| Java setup | `actions/setup-java@v4.7.1` |
| Clojure/tool setup | `DeLaGuardo/setup-clojure@13.4` |
| Failure upload | `actions/upload-artifact@v4.6.2` |
| Local workflow validation | actionlint `1.7.7` |

These are explicit release references, not `latest`. Planning verified the
release tags, the CLI download, and the setup and upload action inputs.
Use setup-clojure's `cli`, `clj-kondo`, and `cljfmt` inputs; do not install
mise or Babashka. Only the quality job needs the two code-quality tools.
The CLI version does not override the project Clojure versions in `deps.edn`.
Keep every existing dependency version unchanged.

### Quality baseline

Run two separate, non-writing steps:

```sh
cljfmt check src test dev test-build build.clj
clj-kondo --lint src test dev test-build build.clj --fail-level warning
```

Retain `.clj-kondo/config.edn` and its `defclosedrecord` mapping. Use cljfmt's
default rules. Make only required formatting and narrow lint corrections.
Do not disable lint categories globally. If a correction requires a runtime
behaviour change, stop that correction and record the need for a separate task.

### Runtime and build jobs

Use a matrix `include` list with version labels and complete command strings:

| Label | Command |
|---|---|
| `1.11` | `clojure -M:test -m scry.cli` |
| `1.12` | `clojure -M:test:clj-1.12 -m scry.cli` |
| `1.13` | `clojure -M:test:clj-1.13 -m scry.cli` |

Set `fail-fast: false`. Do not use `continue-on-error` or shell error
suppression. The default suite includes benchmark-support tests, not the
performance benchmark.

The build job uses `fetch-depth: 0`. Run these in separate steps, in order:

```sh
clojure -M:test:build-test -m scry.cli --dir test-build
clojure -T:build jar
```

Keep the existing temporary-project and isolated-consumer tests intact. The
JAR step runs only after successful build tests. Do not install, publish,
deploy, or upload the JAR.

### Failure evidence

Give each test-command step the ID `tests`. Immediately after it, add an
upload step with `if: ${{ failure() && steps.tests.outcome == 'failure' }}`.
Use path `.scry-results/`, `include-hidden-files: true`, and
`if-no-files-found: warn`. Name runtime artifacts
`scry-runtime-${{ matrix.version }}` and the build-test artifact `scry-build`.
A missing directory must not replace or suppress the original test failure.
A JAR-only failure does not need a scry artifact.

### Documentation and evidence

Update README Development, D-16 in
`ramora/defclosedrecord/decisions/verification.md`, the artifact table in
`ramora/defclosedrecord/embodiment.md`, and
`mementum/knowledge/dev-tooling.md`. Describe CI triggers, environment,
commands, version ownership, and failure artifacts. Keep benchmarks manual.
Do not imply that branch protection or releases are configured.

Append dated evidence to `implementation.md`: installed versions, command
exit codes, structured scry failure records (or zero exit with no failure
records), actionlint results, and review findings. Save each test result
before the next scry invocation clears `.scry-results/`. Record the local
OS and Java version; local execution is not proof of hosted event execution.
Use an existing authorized hosted run if available. Otherwise record hosted
execution as unverified. Do not push solely to obtain evidence.

## Risks

- Existing formatting or lint findings can increase the baseline diff. Keep
  corrections mechanical or narrow, then run all suites to detect changes.
- Tool downloads and Maven resolution require network access. Record these
  failures separately from code failures; do not relax the checks.
- Hidden-file exclusion can silently omit scry results. The explicit upload
  input and a failure-path review are required.
- A local macOS run does not verify Ubuntu or fork-event behaviour. Hosted
  evidence can remain unavailable under the design's explicit allowance.
- The reference workflow uses `latest`, mise, extra checks, and a token
  environment variable. Copy its intent, not those details.
- No blocker was found. Build verification must retain full checkout history
  and fixture isolation.

## Slice order

The plan-review follow-up in `design-steps.md` corrects task navigation only.
It is complete: `munera/plan.md` now points to Slice 1 in `steps.md`, not to
another planning pass. This does not verify any CI implementation item.

1. Establish the pinned, passing formatting and lint baseline.
2. Add the workflow jobs, runtime matrix, build verification, and failure uploads.
3. Validate commands and failure paths; update documentation and record evidence.

Complete and commit each slice before the next. Do not close the task until
implementation review is complete.

## Implementation handoff

All three slices are complete. Local command verification, failure-path
review, and documentation are recorded in `implementation.md`. Hosted event
execution remains unverified. The approach and design are unchanged.
The task remains open for implementation review.

## Planning review — 2026-10-23

- Ambiguity review: read `steps.md` as read-only context and checked each
  slice against the approach and acceptance criteria. No blocking ambiguity
  remains. All implementation items remain unchecked.
- Design consistency review: checked internal contracts and compared
  `design.md` with `deps.edn`, README, the macro lint configuration, D-16,
  the artifact map, development-tooling and artifact-verification knowledge,
  and the referenced scry workflow and `bb.edn`. No contradiction was found.
- Non-blocking details: the design leaves tool versions and the exact
  development-tooling document to planning. The version table and
  `mementum/knowledge/dev-tooling.md` resolve these details.
- D-16 and tooling knowledge still defer CI. This is the documented starting
  state, not an inconsistency; slice 3 removes those claims. Differences
  from the reference workflow are explicit scope decisions in the design.
- No user decision is required. This review validates planning only; it does
  not claim that the future workflow or its commands have passed.
