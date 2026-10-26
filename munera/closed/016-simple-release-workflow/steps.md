# Implementation steps

Follow [plan.md](plan.md) and the stable [design.md](design.md). No real
publication for acceptance. Record evidence in implementation.md.

## Completed baseline

- [x] Restore pre-015 implementation and packaging docs; remove task-015 release code, tests, aliases, and changelog. Preserve closed task history. Completed in `6e66d98`; baseline comparison recorded in implementation.md.

## 1. Deployment contract

- [x] Add pinned stock deps-deploy 0.2.2 in a tooling-only `:deploy` alias; confirm base runtime dependencies are unchanged.
- [x] Extend `jar` to return its existing POM path alongside version/JAR path; verify both files in the real build fixture.
- [x] Add credential validation for absent, empty, and whitespace-only Clojars values; errors name missing variables without values.
- [x] Add `deploy` with credential-check → jar → lazy stock deploy → artifact-map return; inspect the single remote installer call and absence of alternate transports.
- [x] Add optional GitHub step-output writing from the successful deploy result; test emitted version/JAR/POM fields without publication.
- [x] Add isolated subprocess tests for missing credentials; assert build output remains unchanged and diagnostic output contains no supplied secret value.
- [x] Extend artifact fixtures with release-tooling exclusion probes; verify source/license content, Maven metadata, runtime dependency isolation, and isolated consumer loading.
- [x] Run focused build tests and format/lint for changed code; record exit codes and structured failures in implementation.md.
- [x] Commit slice 1 after its checks pass.

## 2. Notes and preparation

Pure notes, local preparation, and notes CLI integration are complete (slices 2a–2c).

- [x] Add composable `:release` and `:release-test` aliases for `dev/release` and `test-release`; verify release tests remain outside default runtime discovery.
- [x] Create CHANGELOG.md with exact Unreleased heading and factual notes; do not add historical versions.
- [x] Implement shared line-based validation for the full design contract; keep body Markdown opaque and notes logic outside build.clj.
- [x] Add validator tests for exact headings, canonical positive versions, calendar dates, trailing spaces, unknown headings, duplicate versions across dates, and repeated/missing/misplaced Unreleased.
- [x] Add body tests for preamble, zero dated sections, whitespace-only bodies, empty prior sections, subheadings/comments, and section headings inside fences/comments.
- [x] Implement pure stamping with explicit UTC date and next version; verify prior entries survive and Unreleased becomes empty.
- [x] Implement notes selection; test exact match, preview-only absent-version fallback, empty Unreleased rejection, and no fallback for malformed/empty matching notes.
- [x] Implement `prepare` guards for clean master and complete history; reject invalid notes or an existing next version before modification.
- [x] Implement the changelog-only `Release vVERSION` commit and resulting version check; leave partial failures for inspection without reset/tag/push.
- [x] Add isolated real Git fixtures initialized on master, with fixture identity/configuration and no inherited Git overrides, hooks, or signing.
- [x] Test successful preparation: UTC date, one commit, exact message, changelog-only diff, preserved notes, matching count/version, and unchanged tags/local bare remote.
- [x] Test preparation rejection for staged/unstaged/untracked changes, wrong branch, detached HEAD, shallow history, invalid notes, and duplicate next version; assert no modification.
- [x] Test commit failure with a fixture-owned rejecting hook; assert nonzero exit, unchanged HEAD, and retained changelog changes for inspection.
- [x] Verify and commit local preparation slice 2b (66 assertions; format/lint pass).
- [x] Add subprocess CLI coverage for prepare success, invalid arguments, and commit failure exit status.
- [x] Add the `notes` CLI with an explicit preview option, supplied notes-file path, and SHA/version/tag/source/preparation outputs; test prepared and preview modes in real Git fixtures.
- [x] Run `clojure -M:test:release:release-test -m scry.cli --dir test-release` and changed-code format/lint; record results in implementation.md.
- [x] Commit slice 2 after its checks pass.

## 3. Workflow and CI

- [x] Create release.yml with manual-only dispatch, master guard, typed default-true dry run, one fixed full-history checkout, disabled persisted credentials, timeout, and non-cancelling concurrency.
- [x] Add Ubuntu/Temurin 21 setup with CI-consistent pins and a logged gh version.
- [x] Add format/lint, three runtime commands, focused build tests, and focused release tests before publication; exclude benchmarks.
- [x] Add failure-only uploads of hidden scry results before a later test invocation can erase them.
- [x] Add notes selection under RUNNER_TEMP and dry-run build/summary; report preparation required on fallback without exposing publication secrets.
- [x] Add explicit-false credential validation before tagging; check Clojars variables and GitHub token without logging values.
- [x] Add gated ordinary tag creation/push at HEAD using command-scoped token authentication; do not force or update master.
- [x] Add gated build/deploy with result step outputs; provide Clojars secrets only where needed.
- [x] Add gated gh Release creation from deploy outputs with file-existence checks, explicit repository/title, --verify-tag, and --notes-file; no intervening build or clobber.
- [x] Add a failed-step summary instructing GitHub/Clojars inspection before another attempt; add no retry, rollback, or resume.
- [x] Extend ordinary read-only CI quality paths and focused test coverage for release tooling, including scry failure uploads.
- [x] Inspect true/false/missing-input gates, secret placement, fixed SHA, and fail-stop sequence against acceptance 3–4; record actionlint results for both workflows.
- [x] Commit slice 3 after its checks pass.

## 4. Documentation and acceptance

- [x] Update README with exact prepare, inspect, separate push, preview, publish, and credential instructions; explain partial failures and independent remote inspection.
- [x] Align release architecture and packaging documentation with implemented commands, ownership, and returned artifact paths.
- [x] Run Java 21 format/lint across src, test, dev, test-build, test-release, and build.clj; record commands/results.
- [x] Run all three runtime suites plus focused build/release suites on Java 21; inspect exit codes and .scry-results after each run.
- [x] Run actionlint on both workflows and check documentation links; record results.
- [x] Audit all six acceptance criteria, runtime isolation, and absence of task-015 infrastructure; record local evidence and hosted limits in implementation.md.
- [x] Commit slice 4 without pushing, configuring secrets, or publishing.

## 5. Test review follow-up

- [x] Cover shallow-history rejection for notes export in `test-release/closed_record/release_test.clj`. The shallow fixture currently exercises only `prepare`; removing the separate guard in `write-notes` would not fail a test. Run the notes CLI in a real shallow clone with otherwise valid notes, in both publication and preview modes. Assert a nonzero exit and unchanged notes/output files, so a truncated Git count cannot supply release metadata.
- [x] Cover valid credential acceptance in `test-build/build_test.clj`. The current deployment credential tests exercise only rejection; a validator that always rejects would pass them. Call the existing pure `assert-deploy-credentials!` helper with an environment map containing two nonblank fixture values and assert success. Keep this test local: do not invoke deployment, inject transport behaviour, or add a test-only production option.

## 6. Documentation review follow-up

- [x] Add factual Unreleased notes to `CHANGELOG.md` for the new release tooling. The current entries describe only `defclosedrecord` and JAR packaging; they omit the user-visible `prepare` command, manual default-dry-run workflow, and Clojars/GitHub publication path added by this task. Summarize these additions without claiming that a release has been published, and keep the headings valid under the shared changelog contract.
