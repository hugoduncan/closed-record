# Verification and Operation

## Acceptance evidence

Use [task 016](../../munera/open/016-simple-release-workflow/design.md)'s six
acceptance criteria. Required evidence is proportionate to the small release path:

- Pure notes checks and real temporary Git preparation tests.
- Real build fixtures for JAR/POM version, coordinate, content, and runtime
  dependency isolation. Credential rejection before build/network work.
- Review the short stock deps-deploy call and ordinary Git/gh commands. No real
  publication or mocked service is needed for acceptance.
- Inspect manual trigger, master guard, typed dry-run gate, one fixed checkout,
  checks-before-writes, secret placement, timeout, and concurrency. Run actionlint.
- Run Java 21 format/lint, three runtime suites, and focused build/release tests.
  Use exit codes and structured scry results, not terminal-output scraping.
- Confirm all task-015 implementation was reverted before replacement, and old
  transport requirements are not active. Do not maintain tests for transport policies no longer required.

Transport source audits, exactly-once tests, CDN absence proofs, retry suppression,
and custom checksum guards are not acceptance requirements. Local tests and
workflow inspection do not establish hosted permissions or successful publication.

## Maintainer procedure

[README release instructions](../../README/releases.md) give exact commands:

1. Supply CLOJARS_USERNAME and CLOJARS_PASSWORD, with artifact authorization.
   Reuse secret names, not another repository's stored values.
2. Write notes and commit intended changes. On clean full-history master, run
   preparation, inspect its notes-only commit/version, then push separately.
3. Dispatch from master with dry run enabled. Inspect version, notes, and checks.
4. Dispatch with explicit dry-run false. Each dispatch fixes its own master SHA;
   if master advances, the earlier preview does not verify the new commit.
   The publishing run checks again and requires matching prepared notes.
5. Inspect the tag, Clojars coordinate/version/files, GitHub Release, notes, and
   assets before declaring the release complete.

GitHub uses the workflow token, not stored login state. Dry runs do not need
Clojars credentials. Normal gh and deps-deploy behaviour is intentionally used.

## Failure handling

Local prepare failure: inspect Git status, changelog, and HEAD before repair.
Do not automatically reset, amend, or push partial changes.

Publication failure: preserve run URL, SHA/version, logs, and available artifacts.
Inspect GitHub and Clojars before another attempt. The tag may already exist;
Clojars may have partial uploads; gh may have retried assets or deleted its
own temporary draft. Neither failure nor success logs independently prove the
state of every remote file.

The workflow stops on command failure and adds no rollback or partial resume.
Do not force tags, overwrite published assets, or blindly rerun deployment.
Manual recovery can require service support or a new version. The standard
libraries' internal actions are accepted; do not reintroduce custom transports
to eliminate them.

No secret setup, push, real release, or hosted success is required for task
acceptance. Record these evidence limits in implementation.md.
