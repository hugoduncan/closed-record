# Implementation checklist

Record verification in `implementation.md`. Check an item only after its
result is verified. Keep indexes unchanged in all hook probes.

## Slice 1 — Setup and shared scope

- [ ] Document installation commands for the plan's pinned tools in `README/development.md`; verify host version output.
- [ ] Add `:quickdoc` and `:tooling-test` aliases and helper/test paths; compare the base runtime dependency basis before and after.
- [ ] Add one owned format/lint scope and validator; test missing required files and directories, excluded imports/caches, and explicit-path independence.
- [ ] Implement separate dependency-basis resolution and `bb clj-kondo:copy-config`; verify cold imports include Malli and dependencies omitted by a build-only basis.
- [ ] Remove the tracked generated Malli import and add ignore rules for imports, style downloads, and caches; verify cold preparation restores usable lint configuration without tracked edits.

## Slice 2 — Formatting and code lint

- [ ] Add `bb clj-fmt check|fix [PATHS...]` and project aliases using the shared scope; verify invalid arguments fail with usage guidance.
- [ ] Add `bb clj-kondo:lint` with automatic preparation and warning-level failure; verify setup failures propagate.
- [ ] Test the existing macro mapping with real clj-kondo: valid fields, method bindings, and both constructors pass; unresolved method-body symbols fail.
- [ ] Add disposable-checkout probes for formatting defects, warning-only lint findings, and lint errors; confirm warning probes have zero errors in structured output.
- [ ] Verify missing required scope stops project check, fix, and lint with a path diagnostic and unchanged files; explicit-path formatting still works.
- [ ] Fix scoped format/lint findings and document narrow intentional-syntax exceptions; review behavior-sensitive diffs and rerun the focused probes.

## Slice 3 — Generated API

- [ ] Implement in-memory Quickdoc generation with JVM `.cljc` selection, exact API filtering, required contract prose, notice, commands, and repository/README links.
- [ ] Add UTF-8 regeneration and non-writing freshness operations behind `bb api-docs`; verify invalid arguments fail.
- [ ] Add independent content tests for the exact two-var API, arguments, options, constructors, rejected operations, literal keys, error contracts, JVM-only support, notice, and links.
- [ ] Add real-process tests for missing/stale output, repair diagnostics, invalid arguments, unchanged failure bytes, and identical repeated generation.
- [ ] Inject a required-content defect into a disposable generator, regenerate, and prove freshness passes while unchanged content tests fail.
- [ ] Generate `doc/API.md` and add the README link; verify reverse and source links and run the focused Scry command from the plan.

## Slice 4 — Prose

- [ ] Add owned Vale/proselint configuration, pinned package URL, ClosedRecord vocabulary, and narrow code/lambda exclusions.
- [ ] Implement `bb vale` with required-scope validation, style sync, explicit project configuration, validated JSON, suggestion reporting, and warning/error failure.
- [ ] Test real suggestion, warning, and error cases; test missing tools/styles/files, sync failures, malformed results, and unknown severities fail without tracked edits.
- [ ] Test owned exclusions in disposable files: fenced code and lambda instructions pass while ordinary prose defects in `AGENTS.md` and nested README Markdown fail.
- [ ] Fix scoped prose at its source, regenerate API output, and verify Vale, freshness, and content tests pass without further tracked edits.

## Slice 5 — Staged-content hooks

- [ ] Add pinned pre-commit configuration and thin format/lint hooks using shared scope, index path selection, fail-fast ordering, and no automatic staging.
- [ ] Add isolated real-commit fixtures with private Git configuration, sanitized `GIT_*` variables, and the actual installed hooks; verify external repositories remain untouched.
- [ ] Prove clean staged content commits despite unrelated unstaged and untracked defects; compare index trees and unrelated file bytes.
- [ ] Prove staged warning-only and error cases block commits despite unstaged corrections; verify original working bytes and index trees remain unchanged.
- [ ] Prove formatting repairs block commits and remain unstaged; manually stage the repair and verify the next commit passes.
- [ ] Prove nonconflicting partial staging retains unstaged edits, conflicting repairs are discarded, and unrelated edits remain unchanged in both failure cases.
- [ ] Test excluded paths, staged additions/deletions/renames, missing indexed scope/configuration, and tool failures through real commits; verify documented exits and preservation.

## Slice 6 — Integration and acceptance

- [ ] Extend the existing CI quality job with pinned setup, shared format/lint, Vale, API freshness, and focused tooling tests; retain existing jobs and security settings.
- [ ] Replace manual release format/lint calls with shared tasks and required Babashka setup; verify publication steps and gates are unchanged.
- [ ] Run actionlint on both workflows and controlled failures through each quality command; compare tracked bytes on success and failure.
- [ ] Complete development documentation for setup, commands, tests, scopes, generated output, hook installation, repair review, and conflict outcomes.
- [ ] Add a short Ramora artifact description and index link; link to contributor instructions without copying them.
- [ ] Follow documented setup in a fresh Linux checkout with empty user configuration and cold imports; record architecture, versions, commands, exits, and hosted-execution limits.
- [ ] Run all three runtime suites, focused build/release suites, link fixtures, and `bb lychee:check`; inspect structured Scry results and record failures or limits.
- [ ] Verify source-only JAR contents and base runtime dependencies remain unchanged apart from permitted source/docstring edits; confirm tooling is absent from the artifact.
- [ ] Run final shared quality commands and focused tooling tests after documentation changes; map evidence to all seven acceptance criteria and record remaining blockers explicitly.
