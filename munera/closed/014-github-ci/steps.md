# Steps: GitHub CI

## Plan-review follow-up

- [x] Correct the stale task 014 planning status in `munera/plan.md` (inconsistency review, plan-review turn 2; `design-steps.md`). Preserved the task entry and linked to Slice 1 here. Checked the index against `plan.md`; all CI implementation items remain unchecked.

## Slice 1 — Quality baseline

- [x] Make Clojure CLI `1.12.6.1673`, cljfmt `0.16.4`, clj-kondo `2026.04.15`, and Temurin 21 available locally; record their reported versions and the host OS in `implementation.md`.
- [x] Run `cljfmt check src test dev test-build build.clj`; record its exit code and affected paths without changing files.
- [x] Apply only the formatting changes needed on those paths; rerun the same cljfmt check and verify exit code zero.
- [x] Run `clj-kondo --lint src test dev test-build build.clj --fail-level warning`; record its exit code and findings.
- [x] Resolve only narrow lint findings, if any; retain the macro mapping, avoid global suppression, and verify the same lint command exits zero. Record a blocker instead of making a runtime behaviour change.
- [x] Review the baseline diff for changes beyond formatting or narrow lint corrections; record the review and commit the slice.

## Slice 2 — Workflow

- [x] Create `.github/workflows/ci.yml` with push-to-`master` and default unfiltered pull-request triggers, `contents: read`, and no privileged events, path filters, secrets, or token environment variable.
- [x] Add the `quality` job on Ubuntu with a 15-minute timeout, the pinned checkout/setup actions and tools from `plan.md`, Temurin 21, and checkout credential persistence disabled; add separate non-writing format and lint commands over all five paths.
- [x] Add the `test` job on Ubuntu with a 20-minute timeout, the pinned checkout/setup actions, Temurin 21, credential persistence disabled, `fail-fast: false`, and the three exact labelled alias commands from `plan.md`; give the command step ID `tests`.
- [x] Add the `build` job on Ubuntu with a 30-minute timeout, the pinned checkout/setup actions, Temurin 21, credential persistence disabled, and `fetch-depth: 0`; add focused build tests with step ID `tests`, then a separate `clojure -T:build jar` step.
- [x] Add failure uploads immediately after each test-command step, using the condition, versioned action, distinct artifact names, hidden-file setting, and missing-file policy in `plan.md`; keep ordinary command failure propagation.
- [x] Run actionlint `1.7.7` against `.github/workflows/ci.yml`; fix workflow findings and record the final zero exit code.
- [x] Inspect all jobs for the planned timeout and setup, no error suppression, no job dependencies, no benchmark command, no dependency overrides, and no publication steps; record the review and commit the slice.

## Slice 3 — Verification and documentation

For each test command, record the process exit code and inspect
`.scry-results/*.edn` if present. Save the evidence before another scry run.
Do not extract failure details from terminal progress text.

- [x] Run the final cljfmt and clj-kondo commands from `plan.md`; verify both exit zero and leave checked files unchanged.
- [x] In a temporary directory outside the checkout, run cljfmt on a deliberately misformatted Clojure file; verify a nonzero exit and unchanged file bytes, then remove the fixture and record the result.
- [x] In a temporary directory outside the checkout, run clj-kondo with `--fail-level warning` on a Clojure file with an unused binding; verify a warning and nonzero exit, then remove the fixture and record the result.
- [x] Run `clojure -M:test -m scry.cli` on Java 21; verify success and record its exit code and structured failure-file status.
- [x] Run `clojure -M:test:clj-1.12 -m scry.cli` on Java 21; verify success and record its exit code and structured failure-file status.
- [x] Run `clojure -M:test:clj-1.13 -m scry.cli` on Java 21; verify success and record its exit code and structured failure-file status.
- [x] Run `clojure -M:test:build-test -m scry.cli --dir test-build` on Java 21; verify success and record its exit code and structured failure-file status without changing fixture or consumer isolation.
- [x] Confirm `git rev-parse --is-shallow-repository` reports `false`, then run `clojure -T:build jar`; record its exit code and the resulting JAR path, and verify the version matches `git rev-list --count HEAD` at build time.
- [x] Review the failure-upload paths for a failing test with result files, a failing test without result files, and a JAR-only failure; verify the step conditions, hidden-file inclusion, distinct names, missing-file warning policy, and unchanged failed-job status. Record this as static review unless hosted evidence exists.
- [x] Update README Development with CI triggers, environment, pinned tools, all matching local commands, and failure-artifact behaviour; state that benchmarks remain manual.
- [x] Update D-16 in `ramora/defclosedrecord/decisions/verification.md` to describe automated verification on all three aliases and manual performance benchmarks.
- [x] Add `.github/workflows/ci.yml` and its orchestration responsibility to the artifact table in `ramora/defclosedrecord/embodiment.md`.
- [x] Update `mementum/knowledge/dev-tooling.md` to remove the CI deferral and record alias ownership, quality commands, separate build verification, and failure artifacts.
- [x] Run actionlint again and compare the final workflow and documentation with every design acceptance criterion; verify `deps.edn` is unchanged and the code diff has no runtime behaviour change, then record the findings.
- [x] Record an existing authorized hosted Actions run and its scope if available; otherwise explicitly record hosted event execution as unverified. Do not push solely for evidence.
- [x] Confirm `implementation.md` contains versions, commands, exit codes, structured test evidence, and verification limits; update completed checkboxes with evidence or commit references and commit the final slice.

Slice 3 evidence: see the dated verification entry in `implementation.md`.
