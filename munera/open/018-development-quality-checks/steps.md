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

## Implementation review follow-up

- [x] Use the same owned-file selection for project formatting, project lint,
  and indexed hooks. `format/format!` and `lint/lint!` pass whole directories
  to the tools, so they do not apply `quality-scope/owned-source?` exclusions.
  A disposable project with `dev/.cache/generated.clj` reproduces the gap:
  `(def x 1 )` makes formatting exit 1; `(ns generated)` followed by
  `(missing)` makes lint exit 3 with one unresolved-symbol error. The index
  selector excludes that path. Project format repair can therefore also
  change excluded cache files. Preserve required-path validation and explicit
  formatting semantics. Add real-command regression tests for nested caches,
  imports, and generated output, including unchanged bytes after project fix.
  Align supported extensions too: the formatter includes `.bb`, but the index
  selector rejects `dev/probe.bb`. Verify project and hook scope agreement.

- [x] Validate the expected type of each required quality path in working-tree
  and index scope. `quality-scope/validate-paths!` checks only existence, and
  `index-scope/select-paths!` accepts either an exact path or any descendant
  for every requirement. A disposable fixture that replaces `src/` with a
  regular file containing `(missing 1 )` passes project formatting (exit 0):
  the selector silently omits `src`. The index selector also accepts an exact
  `src` file instead of a directory. Require directories for source roots and
  regular files for required configuration and root files; report invalid
  types before tool setup. Add real-command and installed-hook regression
  tests for wrong-type roots/configuration, with unchanged tracked bytes and
  index trees. Keep explicit formatting valid for both files and directories.

- [x] Validate required prose path types before Vale synchronization. In
  `dev/quality/closed_record/prose.clj`, `prose-paths!` checks only existence
  for required files. A real Vale 3.17.1 fixture with an empty `README.md/`
  directory instead of the required file returns exit 0 and `{}`; the required
  document is not checked. Require regular files for `README.md`, `AGENTS.md`,
  `doc/API.md`, `.vale.ini`, and the vocabulary file, and a directory for
  `README/`. Reuse `quality-scope/validate-paths!` with explicit expected types.
  Add real-command wrong-type regression tests with path diagnostics and
  unchanged tracked bytes and index trees. This extends the completed
  format/lint type checks to the separate prose boundary.

- [x] Isolate clj-kondo from user-global configuration in shared lint and
  dependency-import commands. `lint/lint-paths!` and
  `lint-dependencies/copy-config!` specify `--config-dir` but omit `--repro`;
  clj-kondo still merges user configuration. A disposable project with
  `(ns probe)` followed by `(missing)` returns exit 0 and zero findings from
  `lint/lint!` when `XDG_CONFIG_HOME/clj-kondo/config.edn` disables
  `:unresolved-symbol`. The same native command with `--repro` returns exit 3
  with one error. Add `--repro` at both command boundaries and real-process
  regression tests with isolated global configuration that disables warning
  and error rules. Verify project lint and installed hooks still reject staged
  defects, and preserve tracked bytes and index trees. Do not change the
  contributor's actual global configuration.

- [x] Isolate API generation from user-global clj-kondo configuration.
  `dev/quality/closed_record/api_docs.clj` calls Quickdoc v0.2.6, whose
  embedded `clj-kondo/run!` call reads global configuration without reproducible
  isolation. With isolated `XDG_CONFIG_HOME/clj-kondo/config.edn` containing
  `{:lint-as {clojure.core/defn clojure.core/comment clojure.core/defmacro clojure.core/comment}}`,
  the unchanged checkout's `bb api-docs --check` fails as stale. In a disposable
  project, `bb api-docs` then `bb api-docs --check` both exit 0, but the output
  has neither API entry. The completed native lint `--repro` fix does not cover
  this embedded analysis. Use owned configuration without user-global influence
  and retain JVM reader selection. Add real-process regression tests that compare
  generated bytes and exact API entries with and without isolated global settings;
  verify freshness preserves tracked bytes and the index. Do not change the
  contributor's actual global configuration.

- [x] Prevent project scope and staged hooks from following source symlinks
  outside their owned content. `quality-scope/project-source-paths!` uses
  `file-seq` and `.isFile`, which follow symlinks; `index-scope/select-paths!`
  checks Git modes only for required root files, not selected source files.
  A disposable project with `src/external.clj` linked to an external file
  containing `(def x 1 )` reproduces an unrelated-file write: project
  `format/format!` with `["fix"]` returns 0 and changes the target to
  `(def x 1)`. Required directory symlinks are also accepted by working-tree
  validation. Define safe symlink handling for maintained scope, reject or
  exclude linked source entries before invoking tools, and do not descend
  through directory symlinks. Keep project and indexed selection consistent;
  a hook must not read external target bytes as staged source content. Add
  real-file and installed-hook tests for file and directory symlinks, with
  unchanged external bytes, link targets, and index trees. Document the policy
  and preserve the separate explicit-path formatting contract.

- [x] Support absolute file and directory paths in explicit formatting.
  `quality-scope/validate-paths!` constructs every input with `(io/file root path)`,
  which throws `IllegalArgumentException` for absolute paths. A temporary
  `probe.clj` containing `(def x 1 )` is checked by native cljfmt, but both
  `bb clj-fmt check /absolute/path/probe.clj` and `bb clj-fmt fix` with that
  path exit 1 before tool execution with “is not a relative path”. Resolve
  relative inputs against root and retain absolute explicit inputs. Keep owned
  configuration validation and maintained project scope unchanged. Add real-tool
  tests for absolute files and directories, check-only byte preservation,
  successful repair, and missing absolute-path diagnostics.

## Test review follow-up

- [x] Remove tests of tests from the normal tooling suite. In
  `api_docs_cli_test.clj`, `fresh-reference-still-needs-content-tests` edits
  generator source, launches another test suite, and asserts on its failures.
  Keep the required controlled generator-defect check as a separate acceptance
  probe, with recorded evidence; retain independent API-content tests in the
  normal suite. In `hooks_test.clj`, `external-repository-preservation-test`
  launches `hook-lint-setup-failure-test` to test fixture isolation. Replace
  that nested test run with a plain fixture operation in a child process and
  assert directly on repository state. Neither probe should depend on another
  test var, assertion count, or Scry failure-file layout.

- [x] Add a real dependency-import regression test for independent alias bases.
  `lint_dependencies_test.clj` tests string union and missing scope, but does
  not prove that `copy-config!` resolves and imports each basis. The other
  tooling fixtures use empty dependency maps or a failing local dependency.
  Use disposable local dependencies with real exported clj-kondo configuration,
  split across an additive alias and a dependency-replacing build alias. Start
  without imports, run the shared setup, and verify both configurations are
  usable by real lint. Check tracked-byte and index preservation. The test
  must fail if setup uses only a combined build basis or omits one alias;
  do not assert a particular subprocess sequence or classpath order.

- [x] Prove whole-index lint coverage through an installed-hook commit with no
  staged Clojure changes. Existing installed-hook defect cases stage the
  defective source, so a changed-files-only lint hook could still pass them.
  Put a warning-only defect, then an error, in committed source before hook
  installation. Stage only an unrelated prose change and attempt a real commit.
  Require a lint diagnostic for the unchanged source and a blocked commit,
  with unchanged HEAD, index tree, and working bytes. Include a clean control.
  This must also detect a hook that skips lint when no code paths are supplied.

- [x] Prove that hook formatting repairs only selected staged files, not all
  indexed source. The installed-hook tests in `hooks_test.clj` have no
  unchanged committed source with a formatting defect, so a formatter that
  repairs the whole index could pass them. Before hook installation, commit
  a lint-clean source file with a real formatting defect. Install the hooks,
  stage only prose, and require a successful commit with unchanged source
  bytes and index tree. Also stage a separate source file that needs repair:
  require the commit to stop and repair only that selected file, preserving
  the unchanged defective source, unrelated working bytes, HEAD, and index
  tree. Use real tools and commits, not assertions on hook command structure.

- [x] Let the child fixture finish cleanup in
  `hooks_test.clj`'s `external-repository-preservation-test`. The child calls
  `System/exit` inside the callback passed to `with-hooks`. This skips the
  enclosing `with-project` cleanup in `finally`, leaving a temporary Git
  repository and pre-commit cache after each run. Return the commit result
  through the fixture, then exit outside it. Verify that the child removes
  its temporary directory on both successful and failed commit outcomes,
  while retaining the external-repository preservation assertions.

- [x] Validate the reason for rejection in
  `dev/acceptance/api_content_defect.clj`. The probe prints selected Scry
  results but accepts any child exit code of 1. A namespace-load error or an
  unrelated content-test failure can therefore satisfy the negative control.
  Read a structured result and require the intended missing-support-notice
  assertion to fail, with no test errors or unrelated failures. Treat child
  launch/load failures and absent or invalid results as probe failures. Keep
  this probe separate from the normal suite and independent of failure-file
  layout or a fixed total assertion count.

- [x] Isolate the owned Vale policy tests from user-global configuration.
  `test-tooling/closed_record/vale_policy_test.clj`'s `check-prose` supplies
  `--config` but omits `--no-global`, unlike the production prose command.
  A disposable HOME with a malformed global `.vale.ini` makes this command
  exit 2 before checking clean prose; adding `--no-global` returns 0 for
  the same input. Thus contributor settings can fail the tests without a
  project policy defect. Use the production configuration-isolation flag
  and verify the owned exclusion and vocabulary cases under isolated hostile
  global settings. Preserve the real Vale checks and do not change the
  contributor's actual configuration.

- [x] Test the exact-byte API freshness boundary with a line-ending-only
  change. In `api_docs_cli_test.clj`, stale-output cases replace the reference
  with unrelated text. They would still pass if freshness compared normalized
  text instead of exact UTF-8 bytes. Generate a valid reference, change only
  LF to CRLF, and require `bb api-docs --check` to fail with the repair command
  while preserving the changed bytes and index. Then regenerate and require
  the original bytes and a passing check. Keep this in the real CLI lifecycle
  coverage; do not assert on implementation code or add a test of another test.

- [x] Strengthen the staged-defect assertions in `hooks_test.clj`'s
  `installed-hooks-test`. The warning/error cases with unstaged corrections
  currently accept any nonzero commit exit and compare only working bytes and
  the index tree. An unrelated hook failure can satisfy these assertions;
  they also do not prove that HEAD stayed unchanged. Require the indexed
  source path and the expected lint finding (`:unused-binding` or
  `:unresolved-symbol`) in the commit diagnostics, and compare HEAD before
  and after each rejected commit. Retain the real installed hooks, unstaged
  correction, clean control, and byte/index preservation checks. This covers
  the staged-content isolation cases, not the separate prose-only whole-index
  tests that already check diagnostics and HEAD.

- [x] Prove that the conflicting-hunk case in `hooks_test.clj`'s
  `partial-staging-test` reaches formatting repair and rollback. Its expected
  working bytes equal the initial bytes, and it checks only a nonzero exit
  plus unchanged HEAD, index, and unrelated files. A failure before formatting
  therefore satisfies every assertion in this case without testing rollback.
  Require the real pre-commit diagnostics for modified files and conflicting
  unstaged-change rollback, or equivalent direct evidence of those events.
  Retain the final byte, HEAD, and index checks and the separate-hunk case.
  An unrelated setup failure must not count as a successful rollback probe.

- [x] Verify that a successful formatting repair stops installed hooks before
  lint runs. In `hooks_test.clj`, the repair cases check rejection and repaired
  bytes, but do not check whether lint ran on those repaired bytes. The
  existing no-lint assertion covers a formatter configuration error, not a
  successful repair detected by pre-commit. Extend an existing real repair
  case to require the modified-files diagnostic and absence of the lint-hook
  diagnostic. Retain the repaired bytes, unchanged HEAD/index, unrelated-file
  preservation, and successful manual-restaging checks. A hook chain that runs
  lint after a repair must fail this test; do not inspect YAML text or mock tools.

- [x] Add an independent test of the maintained format/lint scope in
  `quality_scope_test.clj`. `required-scope-test` builds its fixture and expected
  result from `scope/project-paths`; indexed type tests and preservation fixtures
  also derive their paths from production scope. `owned-source-test` does not
  name `test-build/` or `test-release/`. Removing either required root can thus
  reduce coverage without failing these tests. Define the required roots and
  files from the task contract in the test, independently of production data.
  Check that representative files in each root are selected and that removing
  each required input reports that path. Cover both project and indexed scope,
  using real temporary files and Git where needed. Keep expectations insensitive
  to path order and permit additional owned scope; do not duplicate the existing
  real-tool defect matrix for every root.

- [x] Add independent real-command coverage of Vale's prose selection in
  `prose_test.clj`. All current command severity defects are in
  `README/nested/guide.md`; the other required documents contain clean prose.
  Required-path checks prove presence and type, not that Vale receives each
  document. The owned-policy tests invoke Vale directly and do not cover the
  wrapper's selection. Removing `README.md`, `AGENTS.md`, or `doc/API.md` from
  `prose-paths!`'s output can therefore leave these tests passing. Use one
  representative warning in each required document and nested README Markdown,
  one file at a time, and require the file and rule in the findings plus a
  failing exit. Add an excluded-document control for the documented historical
  prose boundary. Retain a clean control and tracked-byte/index preservation;
  do not repeat the full severity matrix for every path or inspect command text.

- [x] Verify untracked-file preservation in `hooks_test.clj`'s
  `installed-hooks-test`. The fixture creates `src/untracked.clj` with a lint
  defect, but `state` delegates to `tracked-state`, which reads only `git ls-files`.
  A hook could change or delete that untracked file while all current byte and
  index assertions still pass. Capture its exact bytes separately and require
  the file to remain present, unchanged, and untracked after the clean commit,
  staged-defect rejections, formatting repair, and manual-restaging commit.
  Extend the existing cases rather than adding another process matrix. Retain
  the tracked-byte, HEAD, and index checks; together these enforce preservation
  of unrelated files, not only exclusion of untracked defects from lint.

- [x] Verify UTF-8 API generation with non-ASCII source documentation in
  `api_docs_cli_test.clj`. Current generated-content fixtures and the owned
  API reference contain only ASCII. The lambda character in the stale-output
  case tests rejection, not generation, so an ASCII encoder could pass the
  existing suite while corrupting valid docstrings. Add non-ASCII text to a
  disposable source docstring, run the real API command, and assert that the
  output contains its exact UTF-8 bytes. Require freshness and repeated
  generation to pass with identical bytes, and retain check-only preservation.
  Extend a current CLI fixture rather than adding another test framework or
  changing the library docstrings solely for this test.

## API input validation review follow-up

- [x] Reject missing API source before generation or freshness comparison.
  `api_docs.clj` takes only `:analysis` from `clj-kondo/run!` and does not
  validate its required source input. In a disposable API CLI fixture, removing
  `src/closed_record/core.cljc` makes `bb api-docs` exit 0 and write only the
  introduction and an empty table of contents. `bb api-docs --check` then also
  exits 0. Validate the required source before rendering, with a path diagnostic;
  do not overwrite a valid reference when the input is absent. Add real CLI
  regression coverage for generation and check with missing source, both with
  an existing reference and with no output. Require nonzero exits, unchanged
  existing bytes and index, and no newly created reference on failure. Keep
  independent content tests separate from this input/setup failure check.

- [x] Reject API source analysis failures before rendering or writing output.
  `api_docs.clj` keeps only `:analysis` from `clj-kondo/run!` and discards
  error findings. In a disposable CLI fixture, source containing
  `(ns closed-record.core)` followed by an unclosed `(defn check [x]`
  makes `bb api-docs` exit 0 and replace an existing reference with the
  introduction and an empty table of contents. `bb api-docs --check` then
  also exits 0. The completed missing-source validation does not cover this
  existing-but-invalid input. Detect analysis failure and report the source
  diagnostic before output writes. Add real CLI tests for malformed source,
  both with an existing reference and without output: generation and freshness
  must fail, preserve existing bytes and the index, and create no reference.
  Keep this input-failure check separate from independent API content tests.

- [x] Validate the required owned analysis configuration before API generation.
  `api_docs.clj` validates source but only passes `.clj-kondo` as a config
  directory; clj-kondo accepts a missing `.clj-kondo/config.edn`. In a
  disposable CLI fixture with divergent JVM/browser definitions and no owned
  configuration, generation and freshness both exit 0, but the reference uses
  the browser docstring and `(check x y)` arguments and includes `browser-only`.
  Thus missing setup silently removes the JVM reader policy. Require the owned
  configuration to be a regular file, using the shared validator, before analysis
  or output writes. Add real CLI regression tests for missing configuration,
  with and without an existing reference: generation and check must fail with
  the configuration path, preserve existing bytes and the index, and create no
  reference. Retain the existing divergent-reader success test and global
  configuration isolation.

## API output boundary review follow-up

- [x] Prevent API regeneration from writing through symbolic links in its output
  path. `api_docs_cli.clj` opens `doc/API.md` with `io/output-stream`, which
  follows links. In a disposable project, linking that path to an unrelated
  external Markdown file makes `bb api-docs` exit 0 and replace the external
  file with the API reference, while retaining the link. This escapes the
  owned-output boundary; the completed source/configuration link validation
  does not protect output. Reject linked output components before writes,
  including a linked `doc/` directory, with a useful path diagnostic. Reuse
  the shared link check where suitable, while permitting an absent ordinary
  output directory/file. Add real CLI regression tests for file and parent
  links, requiring failure and unchanged external bytes, link targets, and
  index. Retain successful generation of missing ordinary output and exact-byte
  freshness checks. Document the output-path policy.

## Public command test review follow-up

- [x] Cover failure propagation and argument forwarding through the public
  Babashka tasks. `format_test.clj` calls `format/format!` directly;
  `code_preservation_test.clj` and `prose_test.clj` launch custom `bb -e`
  expressions that supply their own exit handling instead of using `bb.edn`.
  Thus those tests still pass if `clj-fmt:check`, `clj-kondo:lint`, or `vale`
  stops propagating a failed helper result, or `clj-fmt` stops forwarding paths.
  Add narrow real-task probes in disposable projects using the owned `bb.edn`:
  require a format defect, warning-only lint, and prose warning to produce
  nonzero public command exits with the expected diagnostics. Include clean
  controls, explicit format path forwarding and repair, and tracked-byte/index
  preservation for checks. Reuse existing fixtures and move suitable cases to
  this boundary rather than duplicate the full helper test matrix. Do not
  inspect task source text, mock tools, or add tests of tests. API and installed
  hook tests already exercise their actual task entry points.

## Shared test fixture review follow-up

- [x] Separate reusable fixtures from test namespaces. `hooks_test.clj`,
  `lint_dependencies_test.clj`, and `symlink_scope_test.clj` access private
  helpers in `code_preservation_test.clj` through `#'`; the symlink suite also
  accesses private hook fixtures. The child isolation probe loads
  `closed-record.hooks-test` for setup, and `dev/acceptance/api_content_defect.clj`
  loads `api_docs_cli_test.clj` for its private project fixture. Thus a change
  to one test namespace can prevent unrelated suites or acceptance probes from
  loading. Move only the shared project, Git, command, and installed-hook setup
  into small test-support namespaces with explicit public helper functions.
  Keep assertions and case-specific data in their test namespaces; do not add
  a fixture framework or merge the distinct project setups. Update the child
  probe and acceptance script to load support code rather than test namespaces.
  Verify the affected namespaces separately, the complete tooling suite, and
  the separate API defect probe, retaining cleanup and preservation checks.

## Documentation review follow-up

- [x] Add task 018's user-visible changes to `CHANGELOG.md` under Unreleased.
  The changelog records link checks but none of the new shared format/lint
  commands, Vale checks, generated API reference and freshness command, or
  staged-content hooks. Summarize these additions and the expanded CI quality
  checks; keep release publication behavior unchanged. Check command names
  against `bb.edn` and link to the contributor guide where useful.

- [x] Update the artifact map in `ramora/defclosedrecord/embodiment.md` for
  task 018. Its `bb.edn` row describes only `lychee:check`, its CI row omits
  Vale, API freshness, and tooling tests, and its dependency description omits
  `:quickdoc` and `:tooling-test`. Describe the current responsibilities and
  link to `ramora/development-quality.md` for quality-tool ownership rather
  than copying installation instructions. Verify the descriptions against
  `bb.edn`, `deps.edn`, and `.github/workflows/ci.yml`.

## Code-shaper review follow-up

- [x] Keep recursive prose selection inside owned files. In
  `dev/quality/closed_record/prose.clj`, `prose-paths!` uses `file-seq` and
  `.isFile`, which follow nested symbolic links under `README/`. Required-root
  validation does not protect these descendants. A disposable public-command
  probe with `README/external` linked to an external directory reports
  `Probe.Warning` for `README/external/external.md` and exits 1 because of
  external prose. This makes the check depend on files outside the checkout;
  directory cycles can also cause repeated traversal. Exclude linked Markdown
  files and do not descend through linked directories, consistent with maintained
  source selection. Reuse a small link-safe traversal helper if useful; keep
  prose and code ownership predicates separate. Add real-command tests for
  nested file and directory links, a directory cycle, and an ordinary nested
  Markdown warning. Require bounded completion, unchanged external bytes and
  link targets, and unchanged tracked bytes and index. Document the prose link
  policy. This is separate from the completed required-path and source-link checks.
