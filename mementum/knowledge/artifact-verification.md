---
title: Source JAR verification and isolated build fixtures
status: active
category: testing
tags: [jar, packaging, git, fixtures, classpath, tools.build]
related: [mementum/knowledge/dev-tooling.md, test-build/build_test.clj, build.clj, README.md]
depends-on: [deps.edn]
---

💡 Verify the packaged library, not the source checkout. These rules came from
task 013 implementation and repeated test-review corrections (2026-10-23).
Use them when changing packaging, runtime source layout, or build tests.

## Isolate the build and Git

- Run artifact builds in a temporary project with real Git metadata. A build
  in the checkout deletes maintainer output under `target/`; concurrent commits
  can also change a commit-count version. Keep fixture cleanup in `finally`.
- A temporary working directory alone does not isolate Git. Remove inherited
  `GIT_*` variables before setting fixture-specific values, for both direct Git
  commands and build subprocesses. Repository, worktree, and index overrides
  can redirect writes outside the fixture.
- Set `GIT_CONFIG_NOSYSTEM=1` and a fixture-owned `GIT_CONFIG_GLOBAL`. Use an
  empty hooks directory, fixture identity, and disabled commit signing. A user
  `core.hooksPath` with a rejecting hook can otherwise stop fixture creation.
- For a no-Git fixture, set `GIT_CEILING_DIRECTORIES` to prevent discovery of a
  parent repository. Give child Clojure CLI processes a temporary `CLJ_CONFIG`
  with an empty `deps.edn` map.
- Check isolation with hostile external Git configuration and repository
  overrides. Verify that protected external files and checkout output remain
  unchanged. Do not confuse CLI cache changes with changes to protected files.

## Branch and shallow-history fixtures

- Select the branch explicitly when behavior depends on it. Release fixtures
  use `git init -b master`; isolated Git configuration does not select a branch.
- For a real local shallow clone, use `git clone --depth=1 file:///absolute/path`.
  The `file:/...` form was treated as SSH during release fixture verification.
  Assert that the resulting repository is shallow and has the expected count.
- Each entry point that derives a version from Git history needs its own
  shallow-history rejection test. A preparation test does not cover notes
  export. Give the shallow fixture otherwise valid notes matching its truncated
  count, so unrelated validation cannot hide a missing history guard. Exercise
  both publication and preview modes; assert nonzero exit and unchanged output
  bytes. Preview must not report a version from incomplete history.

Evidence: task 016 implementation log, slice 2b and shallow-notes review
follow-up; commits `a55e8ba` and `0c9ec7b`.

## Test the archive contract, not a fixed inventory

- Derive expected runtime entries from fixture sources and compare packaged
  bytes. Retain explicit checks for required public source and emitter files,
  license, and Maven metadata. Permit standard JAR metadata; reject other
  entries, compiled classes, and bundled dependencies.
- Add an extra runtime source to prove that source-tree growth is accepted.
  A fixed list of today's source files rejects valid library extensions.
- Add real files under excluded directories (`test/`, `dev/bench/`, `munera/`,
  `mementum/`). An exclusion check cannot detect accidental copying when the
  fixture contains none of the excluded files.
- Rebuild after adding stale staging content and obsolete output. Check both
  filesystem cleanup and absence of the stale content from the new archive.

## Prove consumer isolation

`closed-record.impl.jvm` is required when a consumer expands `defclosedrecord`;
it is not build-only code. Start a separate JVM from a temporary consumer
folder with only the artifact and base runtime dependency JARs. Resolve those
dependencies without user configuration or development aliases. Do not reuse
the test runner's classpath.

Check that both core and emitter resource URLs point into the built JAR, then
expand the macro and exercise record operations. A successful `require` with
checkout sources on the classpath can hide missing packaged code.

Evidence: task 013 `implementation.md`, review follow-ups 1–4; commits
`2736c45`, `fc1c2e9`, `5f37809`, `ac62c6b`, and `1d64776`.
