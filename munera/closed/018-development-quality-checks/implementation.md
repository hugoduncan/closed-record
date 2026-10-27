# Implementation notes

## 2026-10-27 — Architecture review

- No architectural review feedback. Scope unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture sources:
  `AGENTS.md`, `ramora/defclosedrecord.md`, its principles and embodiment,
  and `ramora/release-workflow/` design and contracts. Use these sources in
  later review turns.
- The release architecture keeps publication in one job with step-scoped
  credentials. The read-only CI contract does not replace that release model;
  this task only shares its format/lint policy with release checks.

## 2026-10-27 — Ambiguity review

- Ambiguity review added 1 new design step. Scope unchanged.
- An unchanged index proves that a hook did not stage edits; it does not prove
  that the hook checked the content being committed. This distinction makes
  the hook acceptance clarification actionable, not an implementation choice.

## 2026-10-27 — Inconsistency review

- No new inconsistency review feedback. Scope unchanged. The existing hook
  ambiguity remains open in `design-steps.md`.

## 2026-10-27 — Handoff for hook clarification

- For later hook acceptance probes, use the Git isolation guidance in
  `mementum/knowledge/artifact-verification.md` and the fixture setup in
  `test-build/build_test.clj`. Remove inherited `GIT_*` overrides and isolate
  user Git configuration so probes cannot alter the real index. That fixture
  uses an empty hooks directory; hook probes must instead enable the hooks
  under test inside the disposable repository.

## 2026-10-27 — Hook contract follow-up

- At the reference commit, hook probes are in `test/tooling/hooks.sh`, not
  `test-tooling/hooks.sh` as some reference task notes state. They invoke
  `pre-commit run`; formatter scripts alone do not detect edits or protect
  partial staging. Test the complete hook execution path. The reference probes
  do not cover a staged lint defect hidden by an unstaged correction.

## 2026-10-27 — Architecture review after hook clarification

- No new architectural review feedback or design steps. The clarified index-based
  hook contract fits the maintainer-tooling boundary; it does not require a new
  runtime abstraction. Keep the mechanism choice in planning. Scope unchanged.

## 2026-10-27 — Ambiguity review after hook clarification

- No new ambiguity review feedback or design steps. The revised hook contract
  resolves the earlier staged-content finding; the open-finding statement in
  the earlier inconsistency review is historical. Scope unchanged.

## 2026-10-27 — Inconsistency review after hook clarification

- No new inconsistency review feedback or design steps. Checked the revised
  hook contract and acceptance criteria against the pinned reference guidance,
  current API, dependency and artifact boundaries, and workflow permissions.
  Scope unchanged.

## 2026-10-27 — Plan ambiguity review

- No new actionable ambiguity feedback or design steps. Reviewed the plan and
  read-only checklist against the task contract, current code/tests/workflows,
  and pinned reference. Mechanism risks have explicit verification steps.

## 2026-10-27 — Plan inconsistency review

- No new actionable inconsistency feedback or design steps. The plan and
  read-only checklist agree with the design and resolved review findings.

## 2026-10-27 — Plan-review handoff

- For API prose and content tests, distinguish runtime-read literals from
  compiled constants: compilation and print/read round trips drop hidden extra
  keys. Use `test/closed_record/literal_test.clj` and
  `test/closed_record/hidden_key_test.clj` as evidence; do not imply that `check`
  can recover keys already lost through these paths. This is existing behavior,
  not a new design follow-up.

## 2026-10-27 — Shared scope foundation

- Added `closed-record.quality-scope`: required-path validation reports all
  missing inputs before tool setup. Explicit paths validate independently.
  Git-style source selection excludes imports, caches, output, and traversal.
  Future formatter configuration and owned hooks must join this single list.
- Added only `:tooling-test` in this pass (scry 0.1.61). Quickdoc resolution,
  installation documentation, and cold lint imports remain unimplemented.
  Design is unchanged; this is a smaller first implementation slice.
- Verification: cljfmt 0.16.4 check on new helper/tests and deps.edn exited 0;
  clj-kondo 2026.04.15 on helper/tests exited 0 (no warnings or errors).
  `clojure -Srepro -M:tooling-test -m scry.cli --dir test-tooling` exited 0:
  2 tests, 19 assertions; `.scry-results` has no failure files. Tests use real
  temporary directories and cover missing files/directories and scope selection.
- Base `clojure -Srepro -Spath` before and after is byte-identical (cmp exit 0).
  No runtime source changed. These are local foundation checks, not acceptance
  evidence for tool commands, hook behavior, Linux installation, or hosted CI.

## 2026-10-27 — Quickdoc dependency setup

- Added the planned Quickdoc v0.2.6 Git pin to `:quickdoc`, with the owned
  `dev/quality` helper path. `quickdoc.api` loads and exposes `quickdoc`.
  No generator or API content contract is implemented yet. Design unchanged.
- `clojure -Srepro -P -M:quickdoc:tooling-test` exited 0. Base
  `clojure -Srepro -Spath` before and after is byte-identical (`cmp` exit 0).
  This verifies dependency isolation, not cold-cache or Linux installation.
- `clojure -Srepro -M:quickdoc:tooling-test -m scry.cli --dir test-tooling`
  exited 0: 2 tests, 19 assertions, no failure files in `.scry-results`.
  `cljfmt check deps.edn` and `clj-kondo --lint deps.edn` exited 0;
  lint reports zero warnings and errors. No new behavior needs a new test
  in this dependency-only slice.
- Next: implement separate lint dependency-basis resolution. Include the new
  alias without combining it into the dependency-replacing `:build` basis.

## 2026-10-27 — Independent lint dependency imports

- `bb clj-kondo:copy-config` validates shared scope before resolving each
  dependency alias separately. Resolution/import failures throw with the command,
  exit, and process output. No combined build basis can hide test dependencies.
- Removed the tracked Malli import; imports are now ignored. Cold project
  imports/cache preparation restored Malli and Criterium configuration.
  Style-download ignore rules remain with the Vale slice. Design unchanged.
- Local verification: cold preparation and repeated preparation exited 0;
  tracked diff before/after repeat was byte-identical. Real Malli test lint
  exited 0 with no warnings/errors. cljfmt 0.16.4 checks and clj-kondo
  2026.04.15 checks on helpers/tests/bb.edn passed. Focused Scry CLI exited 0:
  4 tests, 22 assertions, empty `.scry-results`. Tests cover stable classpath
  union and missing-scope failure without filesystem writes; no mocks.
- First preparation process exited 137 without diagnostics; two subsequent
  runs and a cold project-import run passed. Cause is unknown. Maven/Git
  dependency caches were warm; empty-user and Linux checks remain unverified.
- Next: shared formatter/linter commands and real negative probes. Runtime
  source/dependencies and release behavior are unchanged.

## 2026-10-27 — Shared formatting command

- Added formatter tasks and required `.cljfmt.edn`. Explicit inputs are independent
  of project scope but still require owned configuration. Options in path arguments
  are rejected to prevent configuration overrides. Design scope is unchanged.
- cljfmt's EDN configuration cannot hold a regex literal or a string file pattern
  for directory traversal. Pass the EDN-inclusive pattern through its CLI instead.
  Real directory tests caught this distinction; explicit-file tests alone did not.
- Local cljfmt 0.16.4 project check and focused check exited 0. clj-kondo
  2026.04.15 on helpers/tests/bb.edn exited 0 with no warnings/errors.
  Focused Scry CLI exited 0: 6 tests, 45 assertions; no failure artifacts.
  Real temporary-file probes verify defects, repair, non-writing checks, invalid
  arguments, missing project inputs, and missing owned configuration. No mocks.
- Next: lint command and negative lint/macro probes. The combined missing-scope
  and baseline-fix checklist items remain open until lint is covered. No runtime,
  release, or dependency changes. Linux and empty-user setup remain unverified.

## 2026-10-27 — Shared lint command

- Added automatic preparation and warning-level failure with EDN findings.
  Setup failures throw before lint. Flush task output before System/exit so
  redirected diagnostics are retained. Design unchanged; no baseline fixes or
  additional lint suppressions were required.
- Real temporary-project tests cover valid fields/method bindings/constructors,
  one warning with zero errors, unresolved method-body symbols, malformed
  dependency setup, and missing scope. Probe source bytes remain unchanged.
  Full Git checkout preservation tests remain open; these are not hook tests.
- Local verification: focused Scry CLI exited 0 (8 tests, 61 assertions), with
  no failure artifacts. `bb clj-kondo:lint` exited 0 with clj-kondo 2026.04.15
  (37 files, zero warnings/errors). `bb clj-fmt:check` exited 0. No runtime or
  release changes. Linux, empty-user setup, and hosted CI remain unverified.
- Next: API generation and independent content tests; retain the remaining
  installation and disposable-checkout acceptance items.

## 2026-10-27 — In-memory API generation foundation

- Added real Quickdoc generation without output writes. Explicit source scope
  and overrides omit internal vars without changing runtime visibility.
  Independent output tests check exact entries, existing contracts, links,
  support boundary, and repeated string equality. Design unchanged.
- Quickdoc v0.2.6 has no documented reader-feature option. Exact current
  entries pass, but this does not prove JVM branch selection for shared vars.
  Resolve that before marking generation complete. Expand option/constructor,
  error-data, and literal-round-trip prose next; CLI, byte checks, committed
  output, and controlled generator-defect tests remain unimplemented.
- Local verification: combined Quickdoc/tooling Scry command exited 0:
  9 tests, 83 assertions; no failure artifacts. `bb clj-fmt:check` and
  `bb clj-kondo:lint` exited 0 (39 files, zero warnings/errors).
  No runtime source, dependencies, release behavior, or tracked API file changed.

## 2026-10-27 — API file boundary

- Added `bb api-docs` and `--check` through a separate JVM CLI namespace.
  Check compares exact UTF-8 bytes, including line endings; absent/stale output
  returns 1 with a repair command. Invalid arguments return 2 before generation.
  Only regeneration creates the output directory and writes the reference.
- Real Babashka task processes in temporary projects verify missing/stale
  checks, unchanged bytes on failure and success, invalid arguments, repair,
  and identical repeated generation. No mocks. Design unchanged; the committed
  reference remains deferred until contract prose and JVM selection are proved.
- Local verification: combined Quickdoc/tooling Scry CLI exited 0 (10 tests,
  102 assertions); no failure artifacts. Shared format and lint commands exited
  0; lint checked 41 files with zero warnings/errors. A test delimiter error
  was repaired with clj-paren-repair before verification.
- Next: complete contract prose and JVM branch proof, independent content-defect
  acceptance, then commit the generated reference and README link. No runtime,
  dependency, release, Linux-installation, or hosted-CI changes in this slice.

## 2026-10-27 — API contract prose

- Expanded only the macro docstring, not runtime behavior. Options are documented
  as forwarded JVM compiler options; constructor arguments, missing-map-field
  values, error-data fields, and literal-key loss are explicit. Design unchanged.
- Independent generated-content assertions now cover these contracts. JVM branch
  proof and the controlled fresh-but-incomplete generator probe remain next;
  do not treat content assertions as evidence for either. The committed API
  reference and README link remain deferred.
- Local verification: shared format and lint exited 0 (41 lint files, zero
  warnings/errors). Focused Quickdoc/tooling Scry exited 0: 10 tests, 122
  assertions. Runtime Scry exited 0: 97 tests, 379 assertions. Both successful
  runs left no failure artifacts. The first focused run exited 137 without
  diagnostics; rerun with JAVA_TOOL_OPTIONS=-Xmx768m passed. The same heap limit
  was used for lint and runtime verification. No Linux or hosted CI evidence.

## 2026-10-27 — JVM API reader selection

- Quickdoc consumes the project clj-kondo configuration. Set `:cljc` features
  to `#{:clj}` there; this also aligns lint with the JVM-only support boundary.
  No generator adapter or dependency change is needed. Design unchanged.
- A disposable real-task fixture uses different JVM/browser docstrings and
  arguments plus a browser-only var. Removing the setting makes four assertions
  fail: Quickdoc selects browser content. Restoring it passes all five assertions.
  The structured Scry failure artifact confirmed the negative control.
- Local verification with JAVA_TOOL_OPTIONS=-Xmx768m: shared format and lint
  exited 0 (41 files, zero warnings/errors); focused Quickdoc/tooling Scry exited
  0 (11 tests, 127 assertions), with no failure files. No hosted/Linux evidence.
- Next: controlled fresh-but-incomplete generator probe, then committed API
  reference and README link. Remaining Vale, hooks, and integration work is
  unchanged. The API process fixtures now copy the owned lint configuration.

## 2026-10-27 — Committed API reference and negative control

- Added a disposable generator defect that removes the unsupported-host notice.
  Regeneration and freshness pass, but unchanged content tests fail. The probe
  reads the Scry EDN artifact to confirm the missing contract, not terminal text.
  The reference is now committed and linked from README. Design unchanged.
- Local verification with JAVA_TOOL_OPTIONS=-Xmx768m: generation and freshness,
  shared format, and shared lint exited 0 (41 files, zero warnings/errors).
  Focused Quickdoc/tooling Scry exited 0: 12 tests, 133 assertions, no failure
  artifacts. Content tests verify reverse/source links; the README target exists.
  Test delimiter damage was repaired with clj-paren-repair before verification.
- Next: Vale policy and prose checks. Fix generated prose in docstrings or the
  generator, then regenerate. Linux installation, hosted CI, hook safety, and
  final full-suite/link acceptance remain unverified in this slice.

## 2026-10-27 — Owned Vale policy

- Added the pinned proselint package and ClosedRecord vocabulary. Vale's Markdown
  parser excludes fenced and inline code; only lambda instruction lines need
  an explicit block exclusion. Downloaded styles are ignored, not committed.
  Design unchanged. The wrapper, result validation, and baseline prose fixes
  remain next; direct Vale calls are not the completed command contract.
- Real Vale fixtures cover AGENTS and nested README paths, adjacent prose
  defects, spelling vocabulary, and unchanged input bytes. No mocks. Focused
  tooling tests now require `vale sync --config=.vale.ini` before execution;
  integrate this setup in CI and contributor instructions in later slices.
- Local Vale 3.17.1 sync and AGENTS check exited 0. Targeted Scry exited 0
  (2 tests, 13 assertions); full Quickdoc/tooling Scry exited 0 (14 tests,
  146 assertions), with no failure artifacts. Shared formatting and lint
  exited 0 (42 lint files, zero warnings/errors). JVM commands used
  JAVA_TOOL_OPTIONS=-Xmx768m. No Linux or hosted execution evidence.

## 2026-10-27 — Vale result policy

- Added a pure validator for decoded Vale JSON with string keys. It retains
  suggestions and all diagnostic fields, attaches file paths, and fails warnings
  and errors. Invalid shapes, incomplete findings, and unknown severities throw.
  No JSON dependency was added; the next Babashka wrapper can use Cheshire.
- Local focused Scry CLI passed: 2 tests, 23 assertions, no failure artifacts.
  Shared format and lint passed. A real Vale 3.17.1 JSON result parsed through
  Babashka/Cheshire validates and returns failure for proselint errors.
- This is the result-policy part of slice 4, not the complete Vale command.
  Next: required prose scope, sync/process failure handling, JSON decoding,
  real severity/setup probes, and baseline fixes. Design is unchanged.

## 2026-10-27 — Vale command integration

- Added `bb vale` with recursive README Markdown selection, required scope and
  vocabulary validation, style sync, explicit configuration, and `--no-global`.
  Built-in Babashka Cheshire decodes successful output; nonzero tool exits stay
  failures. Suggestions remain visible as JSON. No dependency or design change.
- Real Vale process fixtures use local rules, not mocks or network downloads.
  All severities, nested scope, invalid arguments, missing files/styles, and a
  nonexistent package sync failure preserve fixture bytes. Missing-executable
  and full Git-tracked preservation acceptance probes remain open.
- Vale 3.17.1 live command exited 1 with baseline spelling findings (including
  `destructuring`); baseline corrections remain the next prose step. Shared
  format/lint exited 0 (46 files, zero warnings/errors). Focused Scry with
  JAVA_TOOL_OPTIONS=-Xmx768m exited 0: 18 tests, 195 assertions, no failure
  artifacts. No Linux or hosted CI evidence in this slice.


## 2026-10-27 — Prose baseline corrections

- Added valid technical terms to the owned vocabulary; corrected Babashka case
  and marked the `gh` command as code. API literal syntax is now inline code in
  the source docstring; regenerated API output instead of editing it directly.
  No rule suppression, runtime behavior, dependency, or design change.
- Extended the real Vale vocabulary probe with these terms; the existing
  misspelling control still fails. Shared format/lint, Vale 3.17.1, API freshness,
  and focused Quickdoc/tooling Scry all exited 0 (18 tests, 195 assertions;
  no failure artifacts). Lint reports 46 files, zero warnings/errors.
  JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m.
- Repeated Vale and freshness checks left the tracked diff byte-identical
  (cmp exit 0). Full failure-path Git preservation and missing-executable probes
  remain open, followed by hooks and integration. No Linux or hosted CI evidence.

## 2026-10-27 — Missing Vale executable

- Added a real-process failure probe with an empty PATH and an absolute Babashka
  executable. No fake executable or mock is used. The failure names Vale and
  preserves fixture files. Design unchanged; Git-tracked preservation remains
  separate acceptance work.
- Targeted prose Scry CLI exited 0: 3 tests, 29 assertions; no failure artifacts.
  Shared format and lint exited 0 (46 files, zero warnings/errors). JVM checks
  used JAVA_TOOL_OPTIONS=-Xmx768m. No Linux or hosted CI evidence added.
- Next: complete Git-tracked preservation probes, then staged-content hooks.

## 2026-10-27 — Git-tracked prose preservation

- Added a disposable Git fixture with sanitized Git environment and private
  configuration. Exact byte vectors and index trees remain equal for clean
  prose, all severities, missing styles, failed sync, and missing API input.
  Binary files and unstaged CRLF prose are included. No mocks or runtime changes.
- Vale setup failures return 2; warning/error findings return 1. The initial
  test expected 1 for setup failures; Scry artifacts identified that incorrect
  expectation. Delimiter damage was repaired with clj-paren-repair.
- Targeted prose Scry CLI exited 0: 4 tests, 43 assertions; no failure artifacts.
  Shared format and lint exited 0 (46 files, zero warnings/errors), using
  JAVA_TOOL_OPTIONS=-Xmx768m for JVM commands. Design is unchanged.
- Next: add Git-tracked invalid-argument and missing-executable probes, then
  remaining format/lint preservation and hooks. No Linux or hosted evidence.

## 2026-10-27 — Complete Vale preservation probes

- Extended the disposable Git test to invalid arguments and missing Vale. The
  latter uses an empty PATH only for the check process; Git retains its normal
  executable path for index comparisons. Both failures preserve exact tracked
  bytes and the index, including binary files and unstaged CRLF prose. No mocks.
- Shared format and lint exited 0 (46 files, zero warnings/errors). Targeted
  prose Scry exited 0: 4 tests, 49 assertions, no failure artifacts. JVM checks
  used JAVA_TOOL_OPTIONS=-Xmx768m. Removed a redundant do reported by lint before
  the successful verification. No runtime or design change.
- Next: format/lint Git preservation probes and staged-content hooks. Linux
  installation, hosted CI, and full acceptance verification remain open.

## 2026-10-27 — Format/lint Git preservation

- Added real Babashka/tool processes in isolated Git repositories with private
  configuration and sanitized Git variables. Clean checks, formatting defects,
  warning-only lint, and lint errors preserve tracked bytes and index trees,
  including unrelated binary files and unstaged CRLF prose. Design unchanged.
- The first formatting probe used internal extra spaces, which cljfmt permits.
  Scry identified that incorrect expectation; trailing space before a closing
  delimiter supplies a real formatting defect. Warning output confirms one
  warning and zero errors; error output confirms one error and zero warnings.
- Targeted Scry exited 0: 1 test, 16 assertions, no failure artifacts. Shared
  format and lint exited 0 (47 files, zero warnings/errors). JVM commands used
  JAVA_TOOL_OPTIONS=-Xmx768m. No runtime or dependency changes.
- Next: staged-content hooks and real-commit preservation probes. These tests
  cover check commands, not hook safety. Linux setup, automation, documentation,
  and final acceptance remain open.

## 2026-10-27 — Indexed hook scope foundation

- Added index-only scope validation and owned-source selection. NUL-delimited
  Git output preserves spaces and newlines. Untracked files cannot satisfy
  missing indexed configuration. This helper does not isolate file content;
  hook integration still requires the planned pre-commit context. Design unchanged.
- Real isolated Git tests cover selection, unchanged index tree, staged rename,
  and configuration removal with the working file retained. No mocks.
  Shared format and lint exited 0 (49 files, zero warnings/errors). Targeted
  Scry exited 0: 1 test, 10 assertions, no failure artifacts. JVM commands used
  JAVA_TOOL_OPTIONS=-Xmx768m. No runtime or dependency changes.
- Next: connect this selector to thin hooks, install pinned pre-commit, and
  prove staged-content and restoration contracts through real commits. This
  pass does not establish hook safety, Linux installation, or hosted CI behavior.

## 2026-10-27 — Indexed lint boundary

- Added `lint-index!` using index scope and the same dependency setup, output,
  and warning threshold as project lint. It reads working-tree bytes; callers
  still need pre-commit staged-content isolation. No design or runtime change.
- Real isolated Git/tool tests prove untracked defects are excluded, indexed
  warnings fail with zero errors, missing indexed configuration fails, and lint
  leaves index trees unchanged. These are not installed-hook acceptance tests.
- Shared format/lint passed (49 files, zero warnings/errors). Targeted index
  and lint Scry suites passed: 3 tests, 34 assertions, no failure artifacts.
  JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m.
- Next: connect thin hooks and pinned pre-commit, then verify real commits and
  partial-staging restoration. Linux and hosted execution remain unverified.

## 2026-10-27 — Installed pre-commit integration

- Added fail-fast local hooks and thin shared-command tasks. Format selection
  intersects supplied filenames with owned index paths; `./` protects leading
  dash filenames. No hook stages files. Design and dependencies are unchanged.
- Real installed-hook commits with pre-commit 4.6.0 prove clean indexed content
  passes with unrelated unstaged/untracked defects; staged warnings and errors
  fail despite unstaged corrections; repairs stop without index changes; manual
  staging then passes. Fixtures compare tracked bytes and index trees.
- Pre-commit refuses installation when `core.hooksPath` is explicitly set,
  even to `.git/hooks`. Fixtures now rely on default hooks under isolated Git
  configuration. This resolved the initial test failure without weakening checks.
- Targeted Scry passed (1 test, 17 assertions, no failure artifacts); shared
  format and lint exited 0 (51 files, zero warnings/errors). JVM commands used
  JAVA_TOOL_OPTIONS=-Xmx768m. No hooks were installed in the working repository.
- Next: partial-staging merge/conflict, excluded paths and index mutations,
  setup failures, and explicit external-repository preservation tests. Existing
  success does not establish those contracts. CI, setup docs, Linux verification,
  and full acceptance remain open.

## 2026-10-27 — Partial-stage repair safety

- Real installed-hook commits now prove that separate-hunk unstaged edits survive
  formatting repair, while conflicting repairs are discarded. Both commits stop;
  index trees, HEAD, and unrelated tracked bytes remain unchanged. No mocks,
  runtime changes, or design changes.
- The first separate-hunk fixture used repeated blank lines, which cljfmt also
  repairs. Scry exposed the resulting real conflict. Stable comment lines now
  separate the edits without introducing a second formatting defect.
- Targeted hook Scry exited 0: 2 tests, 29 assertions, no failure artifacts.
  Shared format and lint exited 0 (51 files, zero warnings/errors). JVM commands
  used JAVA_TOOL_OPTIONS=-Xmx768m; tools retain the planned pins.
- Next: external-repository preservation and remaining installed-hook edge cases,
  then automation, documentation, Linux setup, and full acceptance verification.

## 2026-10-27 — Staged path changes

- Real installed-hook commits now cover additions, renames, deletions, excluded
  source defects, and removed indexed formatter configuration with its working
  file retained. Commits preserve tracked bytes and index trees; missing config
  stops the commit and preserves HEAD. No production or design change.
- The first rename fixture retained the old namespace. Scry identified the real
  namespace-name-mismatch error; the fixture now stages the matching namespace.
- Targeted hook Scry exited 0: 3 tests, 42 assertions, no failure artifacts.
  Shared format check passed after explicit test-file repair; shared lint passed
  (51 files, zero warnings/errors). JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m.
  Verified pre-commit 4.6.0, cljfmt 0.16.4, and clj-kondo 2026.04.15.
- Next: external-repository preservation, missing directory scope, and tool-failure
  commit probes. Integration, setup documentation, Linux installation, and full
  acceptance remain open. No hosted execution evidence added.

## 2026-10-27 — Hook setup failure preservation

- Added real installed-hook probes for absent indexed `test/` scope (working
  files retained) and invalid cljfmt indentation configuration. Both stop commits
  and preserve HEAD, index trees, and tracked bytes. Formatter failure stops
  before lint. No production, dependency, or design change was needed.
- Git commit captures pre-commit diagnostics on stderr. The first diagnostic
  assertion used stdout and incorrect wording; structured Scry evidence exposed
  this test error, which is corrected.
- Hook Scry namespace passed: 4 tests, 54 assertions, exit 0, no failure artifacts.
  Explicit test-file format check and shared lint passed; lint reported 51 files,
  zero warnings/errors. JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m.
- Next: installed-hook lint setup failure and explicit external-repository
  preservation. Integration, contributor setup, Linux evidence, and full
  acceptance remain open.

## 2026-10-27 — Installed-hook lint setup failure

- A real commit with valid dependency EDN and an absent local root reaches lint
  setup and exits 1. HEAD, index tree, and tracked bytes remain unchanged. This
  avoids network-dependent failure and requires no mocks or production changes.
  Design is unchanged; external repository preservation is the next hook probe.
- Hook Scry namespace passed: 5 tests, 60 assertions, exit 0, no failure artifacts.
  Shared format and lint passed; lint reported 51 files, zero warnings/errors.
  Commands used JAVA_TOOL_OPTIONS=-Xmx768m. No Linux installation or hosted CI
  evidence added; integration and full acceptance remain open.

## 2026-10-27 — External repository preservation

- A child JVM runs the installed-hook lint setup-failure probe with inherited
  GIT_DIR, GIT_WORK_TREE, and GIT_INDEX_FILE pointing at another disposable
  repository. That repository has different staged and unstaged source bytes.
  Its HEAD, raw index bytes, index tree, and tracked bytes remain unchanged.
  No mocks, production changes, or design changes were needed.
- Hook Scry namespace passed: 6 tests, 64 assertions, exit 0, no failure artifacts.
  Shared format and lint passed after explicit test-file formatting; lint reports
  51 files, zero warnings/errors. JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m.
- Slice 5 is complete. Next: CI/release integration, contributor instructions,
  Linux setup evidence, and full acceptance. No hosted or Linux evidence added.

## 2026-10-27 — Release shared quality checks

- Release now installs Babashka 1.13.225 and invokes the shared format/lint
  tasks. Diff review confirms all publication steps, permissions, credentials,
  and gates are unchanged. No runtime or design change; no new helper needs tests.
- Actionlint 1.7.7 accepted both workflows (exit 0). Shared format and lint
  passed with cljfmt 0.16.4 and clj-kondo 2026.04.15 (51 files, zero warnings
  or errors). Release Scry passed: 9 tests, 104 assertions, exit 0, no failure
  artifacts. JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m.
- Next: expand the CI quality job, then contributor setup and final acceptance.
  Local checks do not establish Linux installation or hosted release execution.


## 2026-10-27 — CI shared quality checks

- Expanded only the existing quality job; other jobs, read-only permissions,
  fork triggers, and credential-free checkout are unchanged. Added pinned
  Babashka, Vale, and isolated pre-commit setup, shared checks, focused tests,
  and Scry failure uploads. Increased the job timeout to 30 minutes for real
  process/hook tests. Design unchanged; existing real-tool tests cover commands.
- Verified the Linux Vale archive against the upstream checksum and embedded
  that digest in CI. Download/checksum verification passed locally; the Linux
  binary and Actions setup were not executed on this macOS host.
- Actionlint 1.7.7 accepted both workflows. Shared format, lint, Vale, and API
  freshness exited 0; lint reports 51 files, zero warnings/errors. Focused
  Quickdoc/tooling Scry exited 0: 28 tests, 316 assertions, no failure artifacts.
  JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m and pre-commit 4.6.0. Checks changed
  no tracked files beyond the workflow edit. No new test implementation needed.
- Next: contributor setup and Ramora documentation, fresh Linux/empty-user
  verification, then full acceptance. Do not treat local success as hosted CI
  or cold-installation evidence.

## 2026-10-27 — Contributor command and hook guide

- Replaced stale direct CI commands with shared tasks. Added a linked quality
  guide for scope, API generation, focused tests, pinned pre-commit installation,
  repair review, and restoration conflicts. Split the guide to keep both README
  files below 1000 words. Added a short Ramora description and index entry.
  Design and runtime behavior are unchanged.
- Vale identified three valid technical terms in the new prose: Ramora,
  docstrings, and unstaged. Added explicit vocabulary entries, not suppressions.
  Existing real Vale policy tests still detect ordinary prose defects.
- `bb vale`, `bb api-docs --check`, and `git diff --check` exited 0. Focused
  `closed-record.vale-policy-test` Scry CLI exited 0: 2 tests, 13 assertions,
  no failure artifacts. JVM commands used JAVA_TOOL_OPTIONS=-Xmx768m. No new
  behavior requires new tests in this documentation slice.
- Next: complete pinned tool installation instructions and version checks,
  then fresh Linux setup and full acceptance. The guide is not installation
  evidence. No hosted execution or Linux verification is claimed.

## 2026-10-27 — Pinned installation guide

- Added a linked installation guide instead of expanding the development page
  beyond 1000 words. Supports Linux x86-64 and macOS arm64; cljfmt 0.16.4 has no
  native macOS x86-64 release archive. Java is Temurin 21.0.10+7, matching the
  locally verified Java 21 environment. No design or runtime changes.
- Executed all three new Bash blocks with a temporary home on Darwin arm64.
  Java, cljfmt, clj-kondo, and Vale archive checksum checks passed. Explicit
  installed-binary version checks passed: 21.0.10+7, 0.16.4, 2026.04.15, 3.17.1.
  The installed CLI reports 1.12.6.1673 and its temporary-home installation path.
  The CLI archive uses HTTPS without a separate checksum; the guide states this.
- Host Babashka 1.13.225 and pre-commit 4.6.0 versions also match. `bb vale`,
  `bb api-docs --check`, and `git diff --check` exited 0. No new program behavior
  or test code; installation verification used real downloads and executables.
- Next: run the documented setup on Linux with empty user configuration and cold
  imports, then final acceptance suites. Podman is installed and has a machine;
  Linux execution has not been attempted. Do not treat these macOS results as
  Linux or hosted Actions evidence.

## 2026-10-27 — Runtime, packaging, and link acceptance

- On macOS arm64 with Temurin 21.0.10+7 and JAVA_TOOL_OPTIONS=-Xmx768m,
  `clojure -Srepro -M:test[:clj-1.12|:clj-1.13] -m scry.cli` passed
  each version: 97 tests, 379 assertions, exit 0. Each run left no Scry
  failure artifacts. Build and release commands from the development guide,
  with `-Srepro`, passed: 6 tests/100 assertions and 9 tests/104 assertions;
  both exit 0 with no failure artifacts.
- `bb dev/link_check_test.clj` passed 9 tests/125 assertions. `bb lychee:check`
  passed both passes (117 links, zero errors). Babashka is 1.13.225; lychee
  is 0.24.2. These checks left the tracked checkout clean.
- `clojure -Srepro -T:build jar` exited 0. Archive inspection found only the
  two runtime source files, LICENSE, and standard manifest/Maven metadata.
  Build tests also verify packaged bytes and isolated consumer loading.
  Diff against pre-task `b90c66f` shows unchanged build code and base deps;
  only development aliases and the permitted core docstring were added.
  Base `-Srepro -Spath` contains src, Clojure 1.11.4, and its two spec JARs.
- Actionlint invocation exited 127: it is absent from the current PATH.
  This is a recoverable setup issue, not a human blocker. Provision it for
  final verification. No code or design change, and no new tests were needed.
  Next: fresh Linux/empty-user setup, then final quality and workflow checks.
  No Linux or hosted execution evidence is claimed.

## 2026-10-27 — Final local quality verification

- Provisioned actionlint 1.7.7 in a temporary directory; both workflow checks
  exited 0. Shared format, lint, Vale, and API freshness commands exited 0
  after the documentation changes. Lint checked 51 files with no findings.
- Focused Quickdoc/tooling Scry exited 0: 28 tests, 316 assertions, no failure
  artifacts. This reruns the real command failure, preservation, content-defect,
  and installed-hook probes; no mocks or new tests were needed. Versions:
  Babashka 1.13.225, cljfmt 0.16.4, clj-kondo 2026.04.15, Vale 3.17.1,
  pre-commit 4.6.0, Temurin 21; JAVA_TOOL_OPTIONS=-Xmx768m.
- Before/after SHA-256 inventories of all tracked file bytes and symlink targets
  match; index trees match. The first inventory attempt followed skill-directory
  symlinks and stopped before checks. The corrected inventory hashes link targets.
- No implementation or design change. Next: fresh Linux installation with empty
  user configuration and cold imports, then map evidence to all seven criteria.
  Podman has an existing stopped machine; Linux execution is not yet verified.
  No human blocker or hosted Actions evidence is claimed.

## 2026-10-27 — Fresh Linux verification and acceptance map

- Used a fresh Ubuntu 24.04 linux/amd64 container in the existing Podman machine,
  with QEMU on macOS arm64. Installed Git, curl, tar, unzip, Python 3.12/venv,
  and CA certificates. Imported HEAD through git archive and made a fixture
  baseline commit. No host home, user configuration, dependency cache, or imports
  were mounted. This verifies commands, not full-history packaging or hosted CI.
- Executed the documented Bash installation blocks unchanged. All archive
  checksum checks and version commands exited 0: Temurin 21.0.10+7, CLI
  1.12.6.1673, cljfmt 0.16.4, clj-kondo 2026.04.15, Vale 3.17.1,
  Babashka 1.13.225, lychee 0.24.2. Python 3.12 installed pre-commit 4.6.0
  in the documented virtual environment; hook installation exited 0.
- With JAVA_TOOL_OPTIONS=-Xmx768m, bb clj-fmt:check, bb clj-kondo:lint,
  bb vale, and bb api-docs --check all exited 0. Dependency imports started
  cold. The documented Quickdoc/tooling Scry CLI exited 0: 28 tests and
  316 assertions pass; .scry-results contains no failure files.
- Before/after SHA-256 tracked-byte inventories (including symlink targets)
  and index trees match. An initial inventory followed external skill symlinks
  and failed before checks; corrected inventory hashes link targets instead.
  Emulation took longer than individual shell waits; the original process
  continued and completed successfully, without restarting tests.
- Added acceptance.md to map all seven criteria to evidence. No source,
  dependency, API, design, or test changes were needed. Existing real-process
  tests cover the contracts. All implementation checklist items are complete.
  Hosted Actions execution remains unverified; no publication was attempted.

## 2026-10-27 — Implementation review

- Added 1 step to be addressed.

## 2026-10-27 — Review follow-up

- Addressed 1 review step: project format/lint now enumerate owned files with
  the indexed-hook predicate, including `.bb`; explicit formatting is unchanged.
- Real-command regression tests verify nested cache/import/target exclusions,
  project/index selection agreement, `.bb` defects and repair, and unchanged
  tracked bytes/index trees for checks and excluded files during repair.
- Java 21 focused Quickdoc/tooling Scry passed: 29 tests, 334 assertions, exit 0,
  no failure artifacts. Shared format, lint, Vale, and API freshness passed.
  Updated the scope guide and plan; design unchanged.

## 2026-10-27 — Further implementation review

- Added 1 further step to be addressed.

## 2026-10-27 — Path-type review follow-up

- Addressed 1 review step. Required source roots now require directories;
  root files and configuration require regular files before tool setup.
  Index validation rejects exact root entries, configuration descendants,
  symlinks, and submodules in place of required files. Explicit formatting
  still accepts files and directories. Design is unchanged.
- Real-command tests cover every required path with check, fix, and lint;
  installed-hook tests cover wrong root, configuration, and root-file types.
  Failed commands preserve tracked bytes and index trees; failed commits
  also preserve HEAD.
- Java 21 focused Quickdoc/tooling Scry: exit 0, 32 tests, 503 assertions,
  no failure artifacts. Shared format, lint, Vale, and API freshness pass.

## 2026-10-27 — Prose boundary implementation review

- Added 1 step to be addressed.

## 2026-10-27 — Prose path-type review follow-up

- Addressed 1 review step. Prose paths use shared expected-type validation
  before Vale synchronization. Real-command tests cover all six required paths,
  path diagnostics, and unchanged tracked bytes and index trees.
- Java 21 focused Quickdoc/tooling Scry passed: 33 tests, 533 assertions,
  exit 0, no failure artifacts. Shared format, lint, Vale, and API freshness
  passed. Updated the scope guide and plan; design unchanged.

## 2026-10-27 — Global lint configuration review

- Added 1 step to be addressed.

- 2026-10-27: Addressed 1 review step. Added `--repro` to shared lint and
  dependency imports. Isolated global-rule suppression probes cover project
  lint and installed hooks; malformed global configuration also leaves imports
  unaffected. Tracked bytes and index trees are preserved. Full focused Scry
  suite: exit 0, 35 tests, 540 assertions, no failure artifacts. Shared format,
  lint, and Vale checks pass. No contributor global configuration was changed.

## 2026-10-27 — API configuration implementation review

- Added 1 step to be addressed.

## 2026-10-27 — API configuration review follow-up

- Addressed 1 review step. Embedded analysis now uses `:repro true` and owned
  configuration before the pinned Quickdoc renderer. The public Quickdoc API
  does not expose that option. JVM selection and generated bytes are unchanged;
  no contributor settings or process globals are changed.
- Real-process tests compare exact API entries and bytes under empty,
  suppressing, and malformed global settings. Successful and stale freshness
  checks preserve tracked bytes and index trees, including unrelated edits.
- Java 21 focused Quickdoc/tooling Scry passed: 36 tests, 565 assertions,
  exit 0, no failure artifacts. Shared format, lint, Vale, and API freshness
  passed. Updated plan and command guide; design unchanged.

## 2026-10-27 — Source symlink implementation review

- Added 1 step to be addressed.

## 2026-10-27 — Source symlink review follow-up

- Addressed 1 review step. Maintained scope excludes source links and does not
  descend through directory links. Required paths reject linked components;
  indexed selection requires regular-file modes. Explicit-path formatting is unchanged.
- Real project and installed-hook probes pass: external bytes, link targets, and
  index trees remain unchanged. Required root, file, and configuration-parent
  link probes reject commands and commits.
- Full tooling suite: 39 tests, 621 assertions pass; no Scry failure artifacts.
  Shared format, lint, Vale, and API freshness checks pass.

## 2026-10-27 — Explicit-path implementation review

- Added 1 step to be addressed.

## 2026-10-27 — Absolute-path review follow-up

- Addressed 1 review step. Explicit formatting retains absolute paths and resolves
  relative paths against root. Owned configuration and maintained scope are unchanged.
- Real cljfmt tests cover absolute files and directories outside the project,
  check-only preservation, repair, missing-path diagnostics, and required configuration.
  All 40 tooling tests and 636 assertions pass; no Scry failure artifacts.
  Shared format, lint, and Vale checks pass. Design unchanged.

## 2026-10-27 — Implementation re-review

- No new actionable issues found.

## 2026-10-27 — Test review

- Added 3 steps to be addressed.

## 2026-10-27 — Test review follow-up 1

- Addressed 1 review step. Moved the generator-defect check to a separate
  acceptance script; generation and freshness pass, while structured content
  results report one failure and no errors. No failure-file layout is used.
- API CLI tests pass (3 tests, 49 assertions). The child-process isolation test
  passes (1 test, 4 assertions) using a plain installed-hook commit operation.

## 2026-10-27 — Test review follow-up 2

- Addressed 1 review step. Real local dependencies export separate binding rules
  through additive `:test` and replacing `:build` bases. Cold lint fails; shared
  import setup makes both bindings pass, with no warnings or errors. Tracked
  bytes and index tree remain unchanged. Dependency tests: 3 tests, 9 assertions pass.

## 2026-10-27 — Test review follow-up 3

- Addressed 1 review step. Installed-hook commits stage only prose, with clean,
  warning-only, and error source baselines committed before hook installation.
  Clean passes; defects produce source diagnostics and block the commit. HEAD,
  index tree, and working bytes remain unchanged on rejection.
- Focused probe: 1 test, 21 assertions pass. Full tooling suite: 41 tests,
  642 assertions pass, with no Scry failure artifacts. Shared format and lint
  checks pass. All 3 test-review follow-ups are complete; design is unchanged.

## 2026-10-27 — Test re-review

- Added 1 step to be addressed.

## 2026-10-27 — Selected-formatting review follow-up

- Addressed 1 review step. Real installed-hook commits preserve a committed
  formatting defect during a successful prose-only commit. A separate staged
  source defect is repaired and blocks the commit; all other tracked bytes,
  HEAD, and the index tree remain unchanged.
- Focused probe: 1 test, 12 assertions pass. Full tooling suite: 42 tests,
  654 assertions pass; no Scry failure artifacts. Shared format and lint pass.

## 2026-10-27 — Test review verification

- No new actionable test findings. Java 21 tooling suite: 42 tests and
  654 assertions pass; exit 0, no Scry failure artifacts.

## 2026-10-27 — Test isolation and acceptance-probe review

- Added 2 steps to be addressed.

- 2026-10-27: addressed 1 review step. Child hook fixtures return before exit; successful and rejected commits remove their temporary roots and caches. Targeted Scry test: 1 test, 7 assertions pass; external repository state is unchanged.

- 2026-10-27: addressed 1 review step. API defect probe reads an explicit EDN result and requires only the missing-support-notice assertion, with zero errors. The probe passes; disposable unrelated-failure and namespace-load-error controls both reject (exit 1). Normal content test: 1 test, 42 assertions pass. Changed-file format check and shared lint pass (zero findings).

## 2026-10-27 — Vale test isolation review

- Added 1 step to be addressed.

## 2026-10-27 — Vale policy review follow-up

- Addressed 1 review step. Policy checks use `--no-global` with temporary
  malformed global configuration at macOS and Linux configuration paths.
- Vale 3.17.1 control: exit 2 without isolation, exit 0 with isolation.
  Focused Scry run: 2 tests, 13 assertions pass; no failure artifacts.
  Explicit format check and shared lint pass. Contributor configuration is unchanged.

## 2026-10-27 — API freshness boundary test review

- Added 1 step to be addressed.

## 2026-10-27 — API line-ending review follow-up

- Addressed 1 review step. Added a real CLI lifecycle test: LF-to-CRLF
  changes fail freshness with the repair command and preserve tracked bytes
  and the index tree; regeneration restores the original bytes and passes.
- Verified API CLI namespace: 4 tests, 61 assertions, no failures or errors;
  Scry exit 0 and no failure artifacts. Explicit format check and shared lint
  pass (zero warnings or errors).

## 2026-10-27 — Staged-defect test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Staged warning/error cases now require
  the indexed source path, expected lint finding, and unchanged HEAD.
  Targeted installed-hook test: 1 test, 22 assertions pass; Scry exit 0,
  no failure artifacts. Explicit format check and shared lint pass.

## 2026-10-27 — Conflicting-repair test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Partial-stage probes require the
  modified-files diagnostic; the conflicting-hunk case also requires the
  conflict-and-rollback diagnostic. Byte, HEAD, and index checks remain.
  Pre-commit 4.6.0 targeted Scry run: 1 test, 13 assertions pass, exit 0,
  no failure artifacts. Explicit format check and shared lint pass.

## 2026-10-27 — Repair-order test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Successful formatting repair now requires
  modified-files diagnostics, no lint-hook diagnostic, and unchanged HEAD.
  Existing byte/index preservation and successful manual-restaging checks remain.
  Targeted installed-hook Scry run on Java 25: 1 test, 25 assertions pass,
  exit 0, no failure artifacts. Explicit format check and shared lint pass.

## 2026-10-27 — Independent scope test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Independent scope expectations cover
  every required root and file in project and real Git index selection. Each
  missing input reports its path; additional owned scope and path order are
  permitted. Focused Scry: 3 tests, 44 assertions pass, exit 0, no failure
  artifacts. Format check and shared lint pass (zero findings).

## 2026-10-27 — Prose selection test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Real Vale wrapper tests reject a warning
  in each maintained document separately and exclude historical task and Ramora
  prose. Clean control and all cases preserve tracked bytes and the index tree.
  Prose namespace: 6 tests, 100 assertions pass; Scry exit 0, no failure artifacts.
  Explicit format check and shared lint pass.

## 2026-10-27 — Untracked-file preservation test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Existing installed-hook cases now check
  untracked source presence, exact bytes, and absence from the index after each
  commit attempt. Targeted Scry: 1 test, 40 assertions pass, exit 0, no failure
  artifacts. Explicit format check and shared lint pass (zero findings).

## 2026-10-27 — API encoding test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. The existing API lifecycle fixture now
  generates a non-ASCII docstring and checks its exact UTF-8 bytes. Freshness,
  repeated generation, and check-only byte preservation pass. API CLI Scry:
  4 tests, 63 assertions pass; exit 0, no failure artifacts. Explicit format
  check and shared lint pass (zero findings). Library docstrings are unchanged.

## 2026-10-27 — API input validation implementation review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. API generation validates required source
  before analysis. Real CLI tests reject missing source with and without output
  for generation and freshness; existing bytes and index trees remain unchanged,
  and no reference is created. API CLI tests: 5 tests, 80 assertions pass;
  independent content test: 1 test, 42 assertions pass. Both Scry runs exit 0
  with no failure artifacts. Shared format, lint, Vale, and API freshness pass.
  Updated plan and command guide; design is unchanged.

## 2026-10-27 — API analysis failure review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. API analysis error findings now stop
  rendering and writes with source diagnostics. Real CLI tests reject malformed
  source for generation and freshness, with and without output; tracked bytes
  and index trees remain unchanged, and no reference is created. API CLI suite:
  6 tests, 105 assertions pass. Independent content suite: 1 test, 42 assertions
  pass. Both Scry runs exit 0 with no failure artifacts. Shared format, lint,
  Vale, and API freshness pass. Design is unchanged.

## 2026-10-27 — API configuration presence review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. API generation now validates owned
  analysis configuration as a regular file before analysis. Real CLI missing-config
  probes preserve tracked bytes and the index, with and without output, for both
  commands. API CLI suite: 7 tests, 122 assertions pass, including JVM reader and
  global configuration isolation. Independent content suite: 1 test, 42 assertions
  pass. Both Scry runs exit 0 with no failure artifacts. Shared format, lint,
  Vale, and API freshness pass. Updated plan and guide; design is unchanged.

## 2026-10-27 — API output boundary implementation review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Both API commands reject output-file
  and parent symbolic links before generation. Real CLI tests preserve external
  bytes, link targets, and index trees. API CLI suite: 8 tests, 146 assertions;
  content suite: 1 test, 42 assertions. Both Scry runs exit 0 without failure
  artifacts. Shared format, lint, Vale, and API freshness pass. Updated plan
  and guide; design is unchanged.

## 2026-10-27 — Implementation review verification

- Java 21 tooling suite: 49 tests and 826 assertions pass; exit 0, no Scry
  failure artifacts. The initial run exceeded the 120-second tool timeout;
  the Java 21 run completed within the extended limit.

## 2026-10-27 — Public command test review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Moved code-preservation and prose-selection
  cases to public Babashka tasks with owned `bb.edn`; added explicit format path
  forwarding and repair coverage. Both test namespaces pass: 12 tests, 284
  assertions, exit 0, no Scry failure artifacts. Shared format and lint pass
  (zero warnings/errors). Design and production code are unchanged.

## 2026-10-27 — Public command test re-review

- Java 21 tooling suite: 50 tests and 838 assertions pass; exit 0, no Scry
  failure artifacts. No new actionable findings.

## 2026-10-27 — Shared test fixture review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Extracted shared code-project, hook,
  and API-project fixtures into public test-support helpers. Child and acceptance
  probes no longer load test namespaces for setup. All five affected namespaces
  pass separately; full tooling Scry: 50 tests, 838 assertions, exit 0, no failure
  artifacts. Separate API defect probe exits 0 with only the expected failure.
  Shared format and lint pass on Java 25. Cleanup and preservation checks remain.

## 2026-10-27 — Test-shaper verification at 5deed1c

- Focused tooling suite: 50 tests, 838 assertions pass; exit 0, no Scry
  failure artifacts. No new actionable findings.

## 2026-10-27 — Documentation review

- Added 2 steps to be addressed.

- 2026-10-27: addressed 1 review step. Added shared quality commands, API
  documentation, hooks, and CI checks to Unreleased. Verified command names
  against `bb.edn` and `bb tasks`, local link targets, and `git diff --check`.
  Release publication behavior and design are unchanged.

- 2026-10-27: addressed 1 review step. Updated artifact responsibilities against
  `bb.edn`, `deps.edn`, and CI; linked quality-tool ownership. Split the artifact
  map from embodiment to meet Ramora's 1000-word limit. Both `bb lychee:check`
  passes succeed (124 links, zero errors); diff whitespace checks pass.
  Design and release publication behavior are unchanged.

## 2026-10-27 — Documentation re-review

- No new actionable findings. API freshness and both Markdown link passes
  succeed (124 links, zero errors).

## 2026-10-27 — Code-shaper review

- Added 1 step to be addressed.

- 2026-10-27: addressed 1 review step. Shared link-safe traversal excludes
  nested prose links and directory cycles; code and prose retain separate scope
  predicates. Real public-command probes complete within 30 seconds and preserve
  external bytes, link targets, tracked bytes, and index trees. Ordinary nested
  warnings still fail. Full tooling Scry: 51 tests, 852 assertions pass, exit 0,
  no failure artifacts. Shared format, lint, Vale, and API freshness pass.
  Updated policy docs and plan; design is unchanged.

## 2026-10-27 — Code-shaper re-review at 19dd3bf

- No new actionable findings. Focused tooling suite: 51 tests, 852 assertions
  pass; exit 0, no Scry failure artifacts.

## 2026-10-27 — Closure

- Closed at the user's request after implementation, review follow-ups, and
  knowledge extraction. No unchecked steps remain.
- Final code-shape re-review: `bedaf88`. Knowledge extraction commits:
  `9234d54`, `198c7eb`, and `ac300f8`.
- Moved the task to `munera/closed/018-development-quality-checks` and removed
  it from the task order. `acceptance.md` retains the verification evidence.
- Closure changes records only; tests were not rerun. Hosted Actions execution
  remains unverified. Fresh Linux evidence uses QEMU/Podman, not native x86-64
  hardware. No publication was attempted.
