# Implementation checklist

Record verification in `implementation.md`. Check an item only after its
result is verified. Keep indexes unchanged in all hook probes.

## Slice 1 — Setup and shared scope

- [x] Document installation commands for the plan's pinned tools through `README/development.md`; verify host version output.
  - Linked installation guide tested with a temporary home on macOS arm64; Linux execution remains a separate acceptance check.
- [x] Add `:quickdoc` and `:tooling-test` aliases and helper/test paths; compare the base runtime dependency basis before and after.
  - [x] Add `:tooling-test` and helper/test paths; base `-Srepro -Spath` is unchanged.
  - [x] Add and resolve pinned `:quickdoc`; repeat the base dependency check.
- [x] Add one owned format/lint scope and validator; test missing required files and directories, excluded imports/caches, and explicit-path independence.
- [x] Implement separate dependency-basis resolution and `bb clj-kondo:copy-config`; verify cold imports include Malli and dependencies omitted by a build-only basis.
- [x] Remove the tracked generated Malli import and ignore dependency imports; verify cold preparation restores usable lint configuration without tracked edits.
- [x] Add style-download ignore rules when Vale configuration is introduced; existing lint cache rules remain in place.

## Slice 2 — Formatting and code lint

- [x] Add `bb clj-fmt check|fix [PATHS...]` and project aliases using the shared scope; verify invalid arguments fail with usage guidance.
- [x] Add `bb clj-kondo:lint` with automatic preparation and warning-level failure; verify setup failures propagate.
- [x] Test the existing macro mapping with real clj-kondo: valid fields, method bindings, and both constructors pass; unresolved method-body symbols fail.
- [x] Add disposable-checkout probes for formatting defects, warning-only lint findings, and lint errors; confirm warning probes have zero errors in structured output.
  - Real Git fixtures verify clean checks and all three defect cases with unchanged tracked bytes and index trees, including binary and unstaged CRLF files.
- [x] Verify missing required scope stops project check, fix, and lint with a path diagnostic and unchanged files; explicit-path formatting still works.
- [x] Fix scoped format/lint findings and document narrow intentional-syntax exceptions; review behavior-sensitive diffs and rerun the focused probes.

## Slice 3 — Generated API

- [x] Implement in-memory Quickdoc generation with JVM `.cljc` selection, exact API filtering, required contract prose, notice, commands, and repository/README links.
  - [x] Add in-memory generation, internal-var exclusions, notice, support boundary, and links; verify exact entries and deterministic output with real Quickdoc.
  - [x] Complete contract prose for constructors, options, error data, and literal-key limits.
  - [x] Prove JVM reader-branch selection through owned clj-kondo `:cljc` features and divergent real-process fixtures.
- [x] Add UTF-8 regeneration and non-writing freshness operations behind `bb api-docs`; verify invalid arguments fail.
- [x] Add independent content tests for the exact two-var API, arguments, options, constructors, rejected operations, literal keys, error contracts, JVM-only support, notice, and links.
- [x] Add real-process tests for missing/stale output, repair diagnostics, invalid arguments, unchanged failure bytes, and identical repeated generation.
- [x] Inject a required-content defect into a disposable generator, regenerate, and prove freshness passes while unchanged content tests fail.
- [x] Generate `doc/API.md` and add the README link; verify reverse and source links and run the focused Scry command from the plan.

## Slice 4 — Prose

- [x] Add owned Vale/proselint configuration, pinned package URL, ClosedRecord vocabulary, and narrow code/lambda exclusions.
- [x] Implement `bb vale` with required-scope validation, style sync, explicit project configuration, validated JSON, suggestion reporting, and warning/error failure.
  - [x] Add pure decoded-result validation and severity policy; retain suggestions, reject malformed findings and unknown severities.
  - Real-process probes cover recursive selection, all severities, missing required inputs/styles, invalid arguments, and sync failures with unchanged fixture bytes.
- [x] Complete Git-tracked byte preservation coverage. Real severity, missing styles/files, and sync-failure probes pass; pure malformed-result and unknown-severity tests pass.
  - [x] Verify tracked bytes and index trees for clean prose, all severities, missing styles, sync failure, and missing API input; include binary and unstaged CRLF files.
  - [x] Extend Git-tracked probes to invalid arguments and missing executable failures.
  - [x] Verify a missing Vale executable fails without changing fixture files, using an empty process search path.
- [x] Test owned exclusions in disposable files: fenced code and lambda instructions pass while ordinary prose defects in `AGENTS.md` and nested README Markdown fail.
- [x] Fix scoped prose at its source, regenerate API output, and verify Vale, freshness, and content tests pass without further tracked edits.

## Slice 5 — Staged-content hooks

- [x] Add and test index-only scope selection with real Git: unusual names, untracked exclusions, staged renames, and missing indexed configuration.

- [x] Add indexed lint entry point with the shared warning threshold; verify untracked defects are excluded and missing indexed configuration fails.

- [x] Add pinned pre-commit configuration and thin format/lint hooks using shared scope, index path selection, fail-fast ordering, and no automatic staging.
- [x] Add isolated real-commit fixtures with private Git configuration, sanitized `GIT_*` variables, and the actual installed hooks; verify external repositories remain untouched.
- [x] Prove clean staged content commits despite unrelated unstaged and untracked defects; compare index trees and unrelated file bytes.
- [x] Prove staged warning-only and error cases block commits despite unstaged corrections; verify original working bytes and index trees remain unchanged.
- [x] Prove formatting repairs block commits and remain unstaged; manually stage the repair and verify the next commit passes.
  - Installed-hook fixtures now use private Git configuration and sanitized `GIT_*` variables. A child-process probe now verifies external HEAD, raw index bytes, index tree, and tracked bytes remain unchanged under inherited Git overrides.
- [x] Prove nonconflicting partial staging retains unstaged edits, conflicting repairs are discarded, and unrelated edits remain unchanged in both failure cases.
- [x] Test excluded paths, staged additions/deletions/renames, missing indexed scope/configuration, and tool failures through real commits; verify documented exits and preservation.
  - [x] Verify excluded source, staged additions/deletions/renames, and missing indexed formatter configuration through installed-hook commits, with unchanged tracked bytes and index trees.
  - [x] Verify missing indexed directory scope and formatter tool failure through real commits.
  - [x] Add an installed-hook lint setup-failure probe.

## Slice 6 — Integration and acceptance

- [x] Extend the existing CI quality job with pinned setup, shared format/lint, Vale, API freshness, and focused tooling tests; retain existing jobs and security settings.
- [x] Replace manual release format/lint calls with shared tasks and required Babashka setup; verify publication steps and gates are unchanged.
- [x] Run actionlint on both workflows and controlled failures through each quality command; compare tracked bytes on success and failure.
- [x] Complete development documentation for setup, commands, tests, scopes, generated output, hook installation, repair review, and conflict outcomes.
  - [x] Document shared commands, scopes, generated API, focused tests, pinned hook installation, repair review, and restoration conflicts in a linked quality guide.
  - [x] Complete pinned Java/Clojure CLI, cljfmt, clj-kondo, and Vale installation instructions; verify host versions.
  - [x] Verify the documented installation in a fresh Linux checkout.
- [x] Add a short Ramora artifact description and index link; link to contributor instructions without copying them.
- [x] Follow documented setup in a fresh Linux checkout with empty user configuration and cold imports; record architecture, versions, commands, exits, and hosted-execution limits.
- [x] Run all three runtime suites, focused build/release suites, link fixtures, and `bb lychee:check`; inspect structured Scry results and record failures or limits.
- [x] Verify source-only JAR contents and base runtime dependencies remain unchanged apart from permitted source/docstring edits; confirm tooling is absent from the artifact.
- [x] Complete the acceptance evidence map after fresh Linux verification.
  - [x] Run final shared quality commands and focused tooling tests after documentation changes; verify tracked bytes and index tree are unchanged.
  - [x] Map evidence to all seven acceptance criteria and record remaining limits explicitly.

Final evidence: [acceptance map](acceptance.md). Linux setup and checks pass;
hosted Actions execution remains unverified.
