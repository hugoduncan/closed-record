# Implementation notes

## 2026-10-23 — Scope and plan

- User requested scope correction to the scry build/deploy pattern. Created this
  replacement task under the Munera scope-change rule. Task 015 is closed without
  completion; its code and historical evidence remain available in Git.
- Chose stock deps-deploy, ordinary gh Release creation, one job/checkout, and
  step-scoped secrets. Accepted tool-internal retries, redirects, checksum
  handling, and temporary draft cleanup. Deployment may rebuild, as scry does.
- Removed target selection, cross-job bundles, remote-absence proof, custom
  transport controls, and dependency audits from acceptance. The plan requires
  removal of their unused implementation rather than keeping dormant alternatives.
- Retained manual master dispatch, default dry run, local changelog preparation,
  runtime/build/quality checks, Git-count versioning, and manual failure recovery.
- Source reference: scry-master/build.clj and .github/workflows/release.yml.
  The reference has tag-triggered publication and two artifacts; neither is copied.
- This pass changes task/architecture documents and working memory only. Validation:
  local Markdown links and git diff --check. No code tests or hosted checks ran;
  no credentials were configured and no release was published.

## 2026-10-23 — Full task-015 implementation revert

- User requested reverting all existing 015 work and replacing it with 016.
  Restored deps.edn and packaging embodiment from pre-015 commit `c9066bd`.
  Removed all 015 release/deploy source, tests, and CHANGELOG.md, including notes
  and preparation. Kept closed task records and Git history, not its code.
- Updated this design, plan, checklist, architecture, and working memory to start
  from that baseline. Do not retain or restore the old module hierarchy.
- Baseline comparison: deps.edn, build.clj, src, test, test-build, .github, README,
  and packaging embodiment match `c9066bd`. Only replacement/historical task
  documentation and release architecture remain different outside those paths.
- Java 21: `clojure -M:test -m scry.cli` passed (97 tests, 379 assertions);
  `clojure -M:test:build-test -m scry.cli --dir test-build` passed (3 tests,
  71 assertions). Both exited 0 with no scry failure files. Markdown link and
  whitespace checks passed. No remote writes or publication.
- Next: implement the small 016 build/deploy, notes, workflow, and docs from this
  baseline. The revert is complete; the replacement implementation is not.

## 2026-10-23 — Architecture review

- No architectural review feedback. Scope unchanged; no new design steps.
- `ramora/META.md` and `doc/architecture.md` are absent. Review used `AGENTS.md`,
  the release architecture linked from `ramora/INDEX.md`, and the library's
  principles and embodiment. The release architecture is intended; the packaging
  embodiment describes the pre-release baseline, not a ban on deployment.

## 2026-10-23 — Ambiguity review

- Ambiguity review added 1 new design step: define the changelog contract.
  Scope unchanged.

## 2026-10-23 — Inconsistency review

- No inconsistency review feedback. Scope unchanged; no new design steps.

## 2026-10-23 — Design follow-up handoff

- `ramora/release-workflow/design.md` assigns notes logic outside build tooling.
  Keep changelog validation in notes support, not in the packaging/deployment
  interface.
- `ramora/release-workflow/contracts-and-flow.md` describes both preparation and
  note selection. Check it against the resolved contract so both entry points
  use the same rules; do not restore task-015 parser rules by default.

## 2026-10-23 — Changelog contract follow-up

- Checked the contract against release architecture preparation and selection;
  no architecture conflict found. No scope-question items were present.
- Release Git fixtures must select `master` explicitly. Existing
  `test-build/build_test.clj` uses plain `git init`; its private fixture config
  disables hooks/signing and isolates Git settings but sets no default branch.
- Validation: Markdown links and `git diff --check`. Design-only change; no
  runtime tests, hosted checks, or publication.

## 2026-10-23 — Ambiguity review after changelog contract

- No new ambiguity review feedback. Scope unchanged; no new design steps.

## 2026-10-23 — Review reference check

- In `~/projects/hugoduncan/scry/scry-master/build.clj`, `deploy` is the
  single-artifact reference: it returns the artifact map from `jar` after
  deployment. Use that entry point as the reference, not `deploy-all`, which
  builds and deploys both artifacts.

## 2026-10-23 — Plan ambiguity review

- No new actionable plan ambiguity feedback. Reviewed steps.md as read-only
  context; no new design steps.

## 2026-10-23 — Plan inconsistency review

- No new actionable plan inconsistency feedback. Task files agree; steps.md
  remains unchanged. No new design steps.

## 2026-10-23 — Slice 1: build/deploy contract

- Added tooling-only deps-deploy 0.2.2. `jar` now returns `:pom-file` with its
  existing version/JAR fields. `deploy` checks environment credentials before
  build effects, builds, loads stock deps-deploy, makes one remote installer
  call, then returns the artifact map. No transport injection or upload tests.
- Optional `:github-output` appends `version`, `jar-file`, and `pom-file` only
  after deployment succeeds. Use these keys in the later workflow.
- Real subprocess tests cover absent/empty/blank credentials for each variable,
  secret-free diagnostics, and preserved output. Archive tests now exclude
  release tooling probes and verify returned paths against real files.
- Initial verification failed because the new direct build invocation used
  Clojure 1.11, while tools.build's process task needs clojure.java.process.
  The fixture now uses the existing :clj-1.12 alias for that invocation; normal
  -T builds remain unchanged. The isolated library consumer still uses 1.11.
- Java 21: focused scry build suite exited 0 (5 tests, 99 assertions), with an
  empty .scry-results directory. cljfmt check and clj-kondo (warning threshold)
  for build.clj/test-build exited 0. Runtime deps are unchanged. No remote writes.
- Design remains unchanged. Next slice: shared changelog validation and local
  preparation. Notes must remain outside build.clj; target/ is deleted by jar.

## 2026-10-23 — Slice 2a: pure notes contract

- Added `closed-record.notes/parse`, `stamp`, and `select-notes`. Selection returns
  `:body`, `:source`, and `:preparation-required?`; both operations validate the
  entire document before producing a result. Stamping takes an explicit date.
- Added tooling-only path aliases and factual Unreleased notes. No dependencies
  or runtime paths changed. Design scope is unchanged; split slice 2 into pure
  logic and Git/CLI integration so each has separate verification.
- Java 21 focused release tests passed (3 tests, 35 assertions); default runtime
  tests passed (97 tests, 379 assertions), confirming separate discovery. Both
  commands exited 0 with empty .scry-results. Changed-code cljfmt and clj-kondo
  checks exited 0. An initial test expected a newline for an empty section body;
  corrected that expectation after inspecting the structured failure.
- Next: implement prepare and notes CLI over these pure functions, with real
  isolated Git fixtures. No Git writes beyond this implementation commit, no
  remote operations, and no publication were performed.

## 2026-10-26 — Slice 2b: local preparation

- Added `closed-record.release prepare` CLI and `prepare` function (directory
  argument, returns version/SHA). Uses UTC date and checks the resulting count;
  Git failures retain changes with an inspection diagnostic. No design change.
- Real isolated Git tests cover all planned preparation guards, changelog-only
  success, unchanged local bare remote/tags, and a rejecting commit hook.
- First verification found a duplicate-key set literal in the date assertion and
  a shallow-clone fixture URL that Git treated as SSH (`file:/...`). Use
  `file:///...` for local shallow clones. Corrected both; no production change.
- Java 21 focused release CLI exited 0: 6 tests, 66 assertions, no failure files.
  `cljfmt check dev/release test-release` and `clj-kondo --lint dev/release
  test-release` exited 0. No hosted checks or publication.
- Next: notes CLI (slice 2c), including output file and step outputs, then workflow.
  Local preparation is tested through its real function; add CLI subprocess
  coverage with notes integration. Commit-failure test checks thrown Git error
  and retained state; CLI nonzero-exit coverage remains for that next slice.

## 2026-10-26 — Slice 2c: notes CLI

- Added `notes NOTES_FILE GITHUB_OUTPUT [--preview]`; requires full history,
  validates before output writes, and appends the output keys listed in plan.md.
  Keep workflow notes under RUNNER_TEMP. Design scope is unchanged.
- Real subprocess tests cover preparation success, invalid arguments, retained
  commit failure, preview-only fallback, prepared selection, output append, and
  invalid matching notes without output changes. No mocks or new dependencies.
- Initial combined verification timed out: shell subprocess futures kept successful
  CLI JVMs alive. The CLI now shuts down agents in finally; subsequent verification
  passed. This does not change the callable preparation function.
- Java 21 focused release suite exited 0 (8 tests, 94 assertions); .scry-results
  was empty. Changed-code cljfmt, clj-kondo (warning threshold), and diff whitespace
  checks passed. No hosted checks or publication. Next: workflow and CI slice 3.

## 2026-10-26 — Slice 3: workflow and CI

- Added the single-job manual workflow and ordinary CI release suite. No design
  change. All publication steps require `toJSON(inputs['dry-run']) == 'false'`;
  true, missing/null, and string values cannot authorize writes. Default success
  gating stops publication after any failed check. Failure uploads run directly
  after each suite; later suites cannot erase failure evidence.
- Checkout fixes the dispatch SHA with full history and no persisted credentials.
  Tag push uses command-scoped `gh auth git-credential`, with an empty helper
  first to discard inherited helpers. No token is placed in the remote URL.
  Clojars secrets occur only in credential validation and deployment. Notes stay
  under RUNNER_TEMP; GitHub assets use successful deployment output paths.
- The deploy CLI argument includes EDN string quotes around GITHUB_OUTPUT. Shell
  quoting alone would not supply a Clojure string to the tools CLI.
- Java 21: full-path cljfmt and clj-kondo checks exited 0 (no warnings). Focused
  release CLI exited 0 (8 tests, 94 assertions), with no .scry-results files.
  actionlint 1.7.12 passed both workflows. Manual inspection checked master guard,
  typed gates, secret scope, fixed checkout, ordering, timeout, concurrency, and
  normal Git/gh commands. No source-text assertion tests or service mocks added.
- Next: README and architecture alignment, then the full three-runtime/build/
  release acceptance run. Hosted permissions, credentials, and publication are
  not verified. No push, secret configuration, or publication occurred.

## 2026-10-26 — Slice 4: documentation and acceptance

- Added exact maintainer commands and recovery guidance. Split README development
  and release sections into linked files to keep each document below 1,000 words.
  Updated architecture status, packaging return values, tooling boundaries, and
  working memory. Design scope and implementation interfaces are unchanged;
  no new tests were needed for this documentation-only slice.
- Java 21.0.10: `cljfmt check src test dev test-build test-release build.clj`
  and `clj-kondo --lint src test dev test-build test-release build.clj
  --fail-level warning` exited 0, with no formatting differences or warnings.
- Java 21 CLI verification (each exited 0; .scry-results contained no files
  immediately after each run):
  - `clojure -M:test -m scry.cli`: 97 tests, 379 assertions.
  - `clojure -M:test:clj-1.12 -m scry.cli`: 97 tests, 379 assertions.
  - `clojure -M:test:clj-1.13 -m scry.cli`: 97 tests, 379 assertions.
  - `clojure -M:test:build-test -m scry.cli --dir test-build`: 5 tests, 99 assertions.
  - `clojure -M:test:release:release-test -m scry.cli --dir test-release`:
    8 tests, 94 assertions.
  - `clojure -T:build jar`: exit 0.
- actionlint 1.7.12 on both workflow files exited 0. Local Markdown target-path
  checks and `git diff --check` passed.
- Acceptance audit: (1) inspected credential-before-build and one stock remote
  deploy call; src and LICENSE match pre-015 baseline, and base deps remain only
  Clojure 1.11.4. (2) pure notes and real isolated Git/process tests pass.
  (3) workflow retains manual master-only dispatch, default preview, typed-false
  publication gates, fixed full-history SHA, all checks before writes, scoped
  secrets, 60-minute timeout, and non-cancelling concurrency. (4) normal Git/gh
  uses matching notes and successful deploy output paths; preview has no release
  writes. (5) prior revert evidence remains in this log; active release tooling
  consists of notes/release namespaces and build deploy, without old transports,
  bundles, or publication framework. (6) all local checks and maintainer docs
  are complete. No push, credentials setup, benchmark, or real publication ran.
- Hosted permissions, service credentials, remote files, and publication success
  remain unverified by design. No implementation follow-up or blocker remains;
  task remains open for review rather than being closed by this pass.

## 2026-10-26 — Implementation review

- No new actionable findings.

## 2026-10-23 — Test review

- Added 2 steps to be addressed.

## 2026-10-23 — Test review follow-up: shallow notes

- Addressed 1 review step. Real depth-one clone has valid matching notes; both
  CLI modes reject it and preserve notes/output bytes.
- Java 21 release suite: exit 0, 9 tests, 104 assertions; no scry failure files.
  Changed-file cljfmt and clj-kondo passed. No publication.

## 2026-10-23 — Test review follow-up: valid credentials

- Addressed 1 review step. The pure credential helper accepts two nonblank
  fixture values; the new test does not build or deploy.
- Repaired an extra delimiter with clj-paren-repair. Java 21 build suite:
  exit 0, 6 tests, 100 assertions; no scry failure files. Changed-file cljfmt
  and clj-kondo passed. Both review steps are complete; no blockers.

## 2026-10-23 — Test review after follow-up

- No new actionable test findings.

## 2026-10-23 — Documentation review

- Added 1 step to be addressed.

## 2026-10-23 — Documentation review follow-up

- Addressed 1 review step: added factual Unreleased release-tooling notes.
- Java 21 shared changelog validation and preview selection passed;
  `git diff --check` passed. No publication.

## 2026-10-23 — Documentation review after follow-up

- No new actionable documentation findings.

## 2026-10-23 — Code-shaper review

- No new actionable code-shaping findings.
