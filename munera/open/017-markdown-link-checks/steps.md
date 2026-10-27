# Implementation steps

## Slice 1 — Local command and policy

- [x] Verify Git, Babashka `1.13.225`, and lychee `0.24.2` are available; record version output in `implementation.md`.
- [x] Add `lychee.toml` with the schemes, input inclusion, HTTP limits, TLS, status, and non-writing settings specified in `plan.md`; verify lychee loads it without errors.
- [x] Add the `lychee:check` task in `bb.edn` and its helper in `dev/link_check.clj`; select NUL-separated Git-tracked Markdown paths and pass them as explicit process arguments with the absolute checkout root.
- [x] Implement both lychee passes and combined failure handling in the helper; verify both execute and either failed pass gives a nonzero task exit with original diagnostics.
- [x] Run cljfmt and clj-kondo checks over `bb.edn` and `dev/link_check.clj`; correct any new formatting or lint findings.

## Slice 2 — Real contract fixtures

- [x] Add `dev/link_check_test.clj` with temporary Git repositories, copies of the actual task files, an isolated Git environment, a loopback HTTP server on an assigned port, and guaranteed resource cleanup; verify `bb dev/link_check_test.clj` runs without changing the checkout.
- [x] Add passing fixtures for source-relative files, directories, non-Markdown targets, spaces in document and target paths, repository-root paths, and same-document and cross-document headings, including duplicate-heading suffixes; assert task exit zero.
- [x] Add document-selection fixtures with tracked root, `README/`, `ramora/`, `mementum/`, open/closed `munera/`, and hidden-directory Markdown; use a broken link in each location in turn to prove every tracked source is checked.
- [x] Add selection-isolation fixtures with broken links in untracked, generated, and outside-checkout documents and a tracked document matched by a temporary global ignore file; prove only the tracked document causes failure.
- [x] Add separate missing-file, missing-directory, and missing-heading fixtures; assert a nonzero task exit and diagnostics containing the source document and failed target.
- [x] Add local HTTP success and redirect fixtures; assert task exit zero, including a successful HTTP page with an absent external fragment.
- [x] Add local HTTP 404, authentication-error, and exhausted-transient-error fixtures; assert nonzero task exits without accepted-error exceptions.
- [x] Add invalid-configuration, missing-lychee, missing-Git, and missing-tracked-input cases using real files and restricted process environments; assert nonzero exits rather than skipped success.
- [x] Check a shell invocation without Babashka on `PATH`; record its nonzero exit as the missing-runner result, without substituting a fake executable.
- [x] Add before/after file and Git-status checks to the fixture runner to detect document, cache, report, or other checkout writes; verify cleanup also occurs after failing checks.
- [x] Run the complete fixture command and format/lint checks for its source; record assertion totals, commands, and exit codes in `implementation.md`.

## Slice 3 — Repository baseline

- [x] Run `bb lychee:check` over the complete tracked Markdown inventory; record the inventory count, exit code, and local/external failures in `implementation.md`.
- [x] Correct each broken link whose intended target is clear; verify the repair preserves the document's historical meaning and does not remove its coverage.
- [x] Evaluate any remaining legitimate but uncheckable destinations; add only exact, anchored URL exceptions with reasons, or record unclear cases and stop for a maintainer decision.
- [x] Rerun `bb lychee:check` after repairs and verify exit zero with both passes enabled; record the live external result and confirm the check itself leaves files unchanged.

## Slice 4 — CI and maintainer documentation

- [x] Add the `Markdown links` job to `.github/workflows/ci.yml` with the pinned setup actions, Babashka version, verified lychee archive, and 15-minute timeout from `plan.md`; run fixtures before the single repository check command.
- [x] Extend the existing CI format and lint commands to include `bb.edn`, retaining all existing source paths and fail-on-warning behaviour.
- [x] Add pinned platform installation and checksum commands, version checks, task/fixture commands, scope, network requirements, and exception policy to `README/development.md`; make its CI environment description accurate for the new job.
- [x] Review the workflow diff against the design: unchanged push/PR events, fork support, read-only permissions, credential-free checkouts, existing jobs, and no secrets, publication, matrix duplication, or failure suppression.
- [x] Run actionlint on the workflow; record its version, command, and exit code and resolve all findings.

## Slice 5 — Final acceptance evidence

- [x] Run final format and lint checks over `src test dev test-build test-release build.clj bb.edn`; require zero exits and no new findings.
- [x] Run `bb dev/link_check_test.clj` after the final task and configuration changes; record passing fixture results and cleanup evidence.
- [x] Confirm all intended new Markdown files are Git-tracked before the final repository check, so the selected input set includes the full change.
- [x] Run the final `bb lychee:check`; record both-pass success, exit code, live external result, and evidence that the check made no checkout changes.
- [x] Complete `implementation.md` with evidence for each design acceptance criterion, including versions, installation checks, repairs/exceptions, and hosted CI evidence or an explicit unverified statement; do not push solely for evidence.
- [x] Review the final diff for runtime/dependency changes, unrelated rewrites, broad exclusions, and leftover temporary files; confirm none are present and commit the completed implementation.

## Test review follow-up

- [x] Add a mixed-failure fixture in `dev/link_check_test.clj`: put a missing local heading and a loopback HTTP 404 in the same tracked document. Assert a nonzero task exit and diagnostics for both failures. Current cases test each failure separately, so they do not detect a regression that stops after the local pass and omits online diagnostics.
- [ ] Add real loopback HTTPS fixtures that prove a trusted certificate succeeds and an untrusted certificate fails with source/link diagnostics. Supply certificate trust through the child process environment, not a production test option. Current HTTP-only fixtures cannot detect disabled TLS verification or omitted HTTPS checking.
- [ ] Extend real network fixtures to cover request timeout failure, HTTP 429 rejection, and recovery from a transient response within the retry budget. Bound fixture execution and assert observable results, not exact request counts. The current permanent HTTP 503 case passes even if retries are disabled; no current case checks the timeout or rate-limit contracts.
- [x] Remove the `is` assertion after `fs/delete-tree` in `with-project`. Keep unconditional cleanup in `finally`, but do not count fixture-helper cleanup as a production contract test. Keep the before/after checkout assertions, which test the command's non-writing contract.

## Documentation review follow-up

- [x] Add an entry under `CHANGELOG.md`'s `Unreleased` heading for the new `bb lychee:check` command and Markdown links CI check. The current changelog does not mention this maintainer-visible command or check.
- [x] Update `ramora/defclosedrecord/embodiment.md` to describe the link-check artifacts (`bb.edn`, `dev/link_check.clj`, `lychee.toml`, and `dev/link_check_test.clj`) and the separate Markdown links CI job. Link to `README/development.md#markdown-links` for installation and commands. Ramora currently has no link-check documentation, and its CI artifact description omits the new job.

## Code-shaper review follow-up

- [x] Separate command construction from process execution in `dev/link_check.clj`'s `check!`. The `mapv` callback currently nests three `into` calls inside `apply process/shell` and exit extraction. Build the two complete argument vectors in a small pure helper or named binding, then execute them in order. Keep eager execution of both passes, explicit `--` before document paths, and first-nonzero exit selection. Run the existing path-with-spaces and mixed-failure fixtures to verify that the change preserves these contracts.
