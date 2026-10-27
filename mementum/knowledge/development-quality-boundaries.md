---
title: Development quality checks and verification boundaries
status: active
category: tooling
tags: [quality, scope, symlinks, pre-commit, clj-kondo, quickdoc, vale, testing]
related: [mementum/knowledge/dev-tooling.md, mementum/knowledge/artifact-verification.md, README/quality-checks.md, ramora/development-quality.md]
depends-on: [bb.edn, deps.edn, .pre-commit-config.yaml]
---

💡 Use these rules when changing quality tools, scopes, generated documentation,
or their tests. Task 018 found several ways that a successful check could omit
required content. Keep command instructions in the contributor guide; this page
records the reasons for the boundaries.

## Validate scope before tool setup

- Existence is insufficient. Required roots must be directories; required source
  and configuration files must be regular files. An empty directory named
  `README.md` can yield a successful Vale run with no findings.
- Share owned-file selection between project format/lint and indexed hooks.
  Passing entire directories to tools bypasses exclusions for nested caches,
  imports, and generated output. Include `.bb` alongside Clojure and EDN.
- Do not follow links in maintained code or prose traversal. Reject linked
  components in required paths, and exclude linked descendants and directory
  cycles. Checking only required roots does not protect nested content.
  Indexed source selection also requires regular-file Git modes.
- Explicit formatting is a separate contract: retain absolute paths, resolve
  relative paths against the root, and accept files and directories outside
  maintained scope. Do not apply project exclusions to explicit inputs.
- Keep expected required scope independent of production scope lists in tests.
  Permit additional scope and arbitrary order. Test each maintained prose
  location with a real warning; presence checks do not prove tool coverage.

## Isolate configuration at every analysis boundary

- Owned configuration does not necessarily disable global configuration.
  Native clj-kondo needs `--repro` for lint and dependency imports. Vale needs
  `--no-global`, including in policy tests. Use temporary hostile global settings
  to prove isolation; never change contributor settings.
- Native lint isolation does not cover embedded analysis. Quickdoc v0.2.6 does
  not expose reproducible analysis through its public entry point. The project
  calls embedded clj-kondo with `:repro true` and owned configuration, then the
  pinned Quickdoc renderer. Recheck this adapter when upgrading Quickdoc.
- JVM-only `.cljc` documentation requires owned
  `:cljc {:features #{:clj}}` configuration. A fixture with different JVM and
  browser docstrings, arguments, and a browser-only var proves branch selection;
  ordinary source with matching branches cannot.

## Dependency lint imports

Resolve the base and relevant development aliases independently with
`clojure -Srepro`, then import the union of distinct classpath entries.
Combining all aliases into one basis can let a dependency-replacing `:build`
alias remove dependencies contributed by additive aliases. Keep downloaded
lint imports ignored and regenerate them; they are not owned source.

Verify this with real local dependencies that export different binding rules
through additive and replacing aliases. Start with no imports, then require
both rules to work after shared setup. A classpath string-union test alone
does not prove resolution or usable imports.

Evidence: task 018 implementation log, independent lint imports and test-review
follow-up 2; commits `f89d4f1` and `e2bbadc`.

## Separate API freshness from correctness

- Freshness only compares generator output with the committed reference. Both
  can agree on incomplete or wrong content. Keep independent tests for the
  exact public API and required contracts.
- Validate source and owned configuration before analysis. Reject analysis
  errors before rendering or writing. Missing input or malformed source can
  otherwise produce an empty but repeatably fresh reference.
- Reject links in the output file and its parent before either API command.
  Input validation does not prevent regeneration from overwriting external files.
  Missing ordinary output paths must still permit generation.
- Compare exact UTF-8 bytes. Test non-ASCII generation and an LF-to-CRLF-only
  freshness defect; ASCII and unrelated stale text do not prove these contracts.
- Keep the controlled generator-defect probe outside the normal suite. Require
  its intended content assertion to fail, with no errors or unrelated failures,
  through an explicit structured result. A nonzero child exit alone can hide
  a namespace-load or setup error.

## Distinguish indexed paths from staged bytes

- `index-scope/paths!` and `lint-index!` select indexed paths but read working
  files. They require pre-commit's staged-content isolation. Do not present the
  internal staged tasks as independent staged-byte checks.
- Format only selected staged owned files. Lint all owned indexed source on
  every commit, including prose-only commits. Use separate probes: unchanged
  committed format defects must remain untouched, while unchanged committed
  lint defects must block a prose-only commit.
- Run formatting before lint with fail-fast ordering. A repair stops the commit
  for review and manual staging; lint must not run on repaired, unstaged bytes.
  Never stage repairs automatically.
- Test real installed hooks with staged defects hidden by unstaged corrections.
  An unchanged index proves only that no staging occurred, not which bytes were
  checked. Require the expected source diagnostic and unchanged HEAD.
- Partial-stage tests must prove that repair occurred and, for conflicting
  edits, that rollback occurred. Unchanged final bytes plus a failed commit
  can also result from an earlier setup failure.

## Verify public behavior and preservation

Use owned `bb.edn` tasks in disposable projects. Direct helper calls with custom
exit handling cannot detect broken public exit propagation or path forwarding.
Compare exact tracked bytes and index trees on success and failure; include
binary and unstaged CRLF files. Check untracked bytes and link targets separately,
since tracked-file inventories omit them. Reuse the Git isolation guidance in
`artifact-verification.md`.

## Evidence

Task 018 design, plan, steps, and implementation log (2026-10-27). Task-scoped
commits: `25d6ffb`, `f77b50b`, `19dd3bf` (scope); `da7a926`, `c6d3b2b`,
`4829105` (analysis); `14b1dd2`, `d25f05e`, `ff41f55`, `2b8760b` (API);
`877edce`, `17d0314`, `8b0b0de`, `963e20c`, `687e185` (hooks); `984abb8`,
`bf9dfbd` (independent scope and public commands). Local Linux setup evidence
is not hosted Actions evidence.
