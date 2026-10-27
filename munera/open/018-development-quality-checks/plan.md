# Development quality checks: implementation plan

## Approach

Extend existing tasks and workflows. Use value-type commit
`d117bf540cdaa4d4e80994bb4500c370fda029df` as research only, through
`git show`, not its newer checkout HEAD. Preserve runtime dependencies,
Clojure 1.11+ JVM support, var visibility, packaging, and release gates.

### Setup and ownership

Reuse Babashka 1.13.225, Clojure CLI 1.12.6.1673, cljfmt 0.16.4,
clj-kondo 2026.04.15, lychee 0.24.2, and the existing Temurin 21 CI setup.
Add Vale 3.17.1, pre-commit 4.6.0, proselint styles v0.1.0, and Quickdoc
v0.2.6 at `ce8678000afa5f835639dd7267dc191fd90495a2`.
These are selected pins, not installation evidence for this project.

Keep setup-clojure in Actions; add its pinned Babashka input. Install Vale
from its versioned release archive, with a verified checksum. Document
Linux x86-64 and macOS archive installation beside existing tool instructions.
Install pre-commit in an isolated Python 3.12 virtual environment with
`pip install pre-commit==4.6.0`. Pin the proselint package URL in `.vale.ini`.
Do not introduce mise or a second Java installer. Installation errors stop checks.

Put helpers in `dev/quality/closed_record/` and tests in `test-tooling/`.
The development-only `:quickdoc` and `:tooling-test` aliases are in place; the latter
supplies scry 0.1.61. The focused command is
`clojure -Srepro -M:quickdoc:tooling-test -m scry.cli --dir test-tooling`.
Use explicit project configuration and reproducible dependency resolution;
verify with empty user configuration. Keep `bb lychee:check` unchanged.

### Shared format and lint policy

Own one scope list: `src`, `test`, `dev`, `test-build`, `test-release`,
`test-tooling`, `build.clj`, `bb.edn`, `deps.edn`, and owned lint/formatter
configuration and hooks. Enumerate owned lint files separately from imports. The initial policy is in
`closed-record.quality-scope`; extend its required paths when formatter
configuration or owned hooks are added.
The formatter now uses required `.cljfmt.edn` and an explicit CLI file pattern
that includes EDN. Explicit paths still require the owned formatter configuration.
Validate required paths before setup or execution; never filter missing paths
away. Explicit formatting validates only its supplied paths. Both project
format aliases and argument-free `bb clj-fmt check|fix` use this policy.

Prepare dependency imports from separately resolved base and alias bases:
`:test`, `:build`, `:build-test`, `:bench`, `:nrepl`, `:deploy`, `:quickdoc`,
and `:tooling-test`; include version overrides without losing other bases.
Path-only release aliases add no dependencies. Combine distinct classpaths
before clj-kondo import preparation. A merged `:build` basis alone is unsafe.
Dependency preparation now resolves these bases separately through `clojure -Srepro`
and combines distinct classpath entries for one clj-kondo import pass. The tracked
Malli import is removed;
ignore generated imports and caches, but retain owned configuration.

The shared lint command now prepares dependencies automatically and returns structured
EDN findings with warning-level failure. Real-tool probes verify setup failures
and the existing mapping. Git fixtures now verify clean checks, formatting defects,
warning-only lint, and lint errors with unchanged tracked bytes and index trees.
No baseline fixes or new suppressions were needed.
Keep the `defrecord` lint mapping unless real binding, constructor, and
method-body tests show a gap. Fail on warnings and errors. Use explained,
form-local exceptions for intentional invalid syntax. Review baseline fixes
for behavior changes.

### API and prose

Use Quickdoc in-memory output from `src/closed_record/core.cljc`, selecting
JVM reader features. Exclude private vars and override `ClosedRecord` and
`-extra-keys` documentation without changing visibility. Document exactly
`defclosedrecord` and `check`; explain generated constructors within that API.
Use docstrings and a short owned introduction for arguments, options,
rejected operations, literal keys, error data, JVM support, notice, commands,
and links. Source links target this repository on `master`.

The in-memory generator is now `closed-record.api-docs/generate`. Quickdoc
v0.2.6 has no documented reader-feature option; the owned clj-kondo configuration
selects `:cljc {:features #{:clj}}` for both analysis and generation. A real-process
fixture proves JVM docstring/argument selection and excludes a browser-only var.
Current output contains exactly the two API vars. Contract docstrings and independent
content assertions now cover constructors, option forwarding, error data, and
the loss of extra literal keys through compilation and print/read round trips.

`closed-record.api-docs-cli` now separates generation from UTF-8 writing and
byte comparison behind `bb api-docs`. Only regeneration writes `doc/API.md`;
freshness and invalid-argument failures never write. Real task processes in
temporary projects verify these operations.
Independent content tests reject a controlled generator defect even
after regenerating a fresh reference. A disposable real-process probe removes
the unsupported-host notice, verifies freshness passes, and checks the Scry
failure artifact from the unchanged content tests. `doc/API.md` is committed
and linked from README; later prose fixes must regenerate it.

Vale selects `README.md`, recursive `README/**/*.md`, `AGENTS.md`, and
`doc/API.md`; validate required roots/files. Use owned Vale/proselint
configuration and ClosedRecord vocabulary. Protect fenced code and lambda
lines narrowly. The owned `.vale.ini` and ClosedRecord vocabulary are in place; downloaded
`styles/proselint/` is ignored. Real Vale fixtures prove fenced/inline code and
lambda exclusions while adjacent ordinary prose and misspellings still fail.
Focused tests currently require `vale sync --config=.vale.ini` before execution;
`bb vale` now provides command/setup integration.
`closed-record.vale-results/outcome` now validates decoded JSON with string keys
and retains all findings with file paths. Invalid results throw; warnings and
errors return exit 1. The Babashka command decodes JSON with built-in Cheshire and preserves nonzero process exits. No runtime dependency is added.
Sync styles and request JSON with suggestions visible.
A real process with an empty executable search path verifies missing Vale fails
without changing fixture files. Git-tracked probes now cover clean prose, all
severities, missing styles, sync failure, and missing API input. Invalid arguments
and missing executables also have Git-tracked preservation probes, with exact
byte and index-tree comparisons.
The scoped prose baseline now passes. Technical vocabulary is explicit; API literal
syntax uses inline code in its source docstring. No rule was disabled.
Validate results: suggestions pass; warnings, errors, unknown severities,
invalid results, and setup failures fail. Fix generated prose at its source.

### Hooks and automation

Use local `pre-commit` hooks installed with `pre-commit install`.
Its staged-files context temporarily removes tracked unstaged changes.
Run formatting on staged maintained Clojure/EDN files, selected through the
shared scope policy. Run project lint with `always_run: true` and
`pass_filenames: false`, so all indexed scope is checked on every commit.
For hook lint, enumerate index paths within the shared scope; ignore untracked
files and require indexed configuration. `closed-record.index-scope/paths!` now
provides this selection with NUL-delimited Git output. `closed-record.lint/lint-index!`
now applies the shared dependency setup and warning threshold to those paths.
Real Git/tool tests prove untracked defects are excluded and indexed warnings fail. It validates indexed
paths, not content isolation; the hook integration must still provide the
pre-commit staged-files context. Set `fail_fast: true`: formatter
repairs must stop before lint can inspect repaired rather than staged content.
Do not substitute working-tree-only direct hook invocation for commit tests.
The local configuration and `quality:format-staged`/`quality:lint-staged` tasks
are now in place. The formatter intersects supplied filenames with indexed owned
paths and prefixes `./` to support names that start with a dash. Installed-hook
commit tests pass with pre-commit 4.6.0 for clean content, hidden staged defects,
repair rejection, and manual staging. Partial-stage restoration and conflicting-repair rollback now pass real-commit
probes with unchanged index and HEAD. Installed-hook probes also pass for additions, namespace-correct renames, deletions,
excluded source, and indexed formatter configuration removed while its working file
remains. Missing indexed directory scope and formatter configuration failure now stop real
commits without changing HEAD, index trees, or tracked bytes. Installed-hook lint
setup failure now also stops commits with exit 1 and preserves HEAD, index, and
tracked bytes. The probe uses valid EDN with a missing local dependency, so it
reaches dependency setup without network access. A child-process installed-hook probe now verifies external repository
preservation under inherited GIT_DIR, GIT_WORK_TREE, and GIT_INDEX_FILE overrides.
It compares HEAD, raw index bytes, index tree, and tracked bytes. The configuration sets a minimum runner version; installation
instructions must still pin the exact supported version.

Use pre-commit's change detection and restoration: repairs stop the commit
and remain unstaged; nonconflicting unstaged edits are restored; conflicting
repairs are rolled back before restoration. Never stage edits. Verify these
properties with pinned pre-commit and real commits before accepting this
mechanism. Isolate fixture Git configuration and inherited `GIT_*` variables.

Extend the existing CI quality job with shared commands and focused tests.
Retain all other jobs, read-only permissions, credential-free checkout, and
fork support. Replace only manual release format/lint commands and required
setup; preserve publication behavior. The release workflow now installs pinned
Babashka and calls the shared format/lint tasks. Actionlint accepts both workflows;
CI quality now installs pinned Vale with an embedded verified Linux archive
checksum and pre-commit in a Python 3.12 virtual environment. It runs shared
format/lint, prose, API freshness, and focused tooling tests, with Scry failure
artifacts. Both workflows pass local actionlint; hosted execution remains unverified.

## Risks

- Downloads, Python prerequisites, or Linux execution may be unavailable.
  Record blockers; reference-project evidence is not project verification.
- Quickdoc `.cljc` filtering and pre-commit restoration need real-tool proof.
  If the mechanism fails the contract, revise this plan before proceeding.
- Wider scope can expose intentional test syntax and tracked-import assumptions.
  Do not weaken analysis or change behavior to obtain a pass.
- Vale style synchronization may write ignored caches, never tracked files.
  Freshness cannot substitute for API-content tests.

## Slice order

1. Pinned setup, shared scope, and cold dependency imports.
2. Format/lint commands, negative tests, and baseline fixes.
3. API generation, independent content tests, and committed reference.
4. Vale policy, failure tests, and prose fixes.
5. Staged-content hooks and disposable commit tests.
6. CI/release integration, documentation, and complete acceptance evidence.

Contributor commands and hook behavior are now in `README/quality-checks.md`,
linked from development instructions. This keeps each document below 1000 words.
The Ramora artifact describes ownership without copying instructions. Pinned tool installation is now in `README/tool-installation.md`, linked from
development instructions. Its Java, CLI, and native quality-tool blocks passed
on macOS arm64 with a temporary home. cljfmt has no macOS x86-64 native archive
for this pin, so the guide supports macOS arm64 and Linux x86-64. Fresh Linux
verification remains open; design is unchanged.

Each slice records commands, versions, exits, and limits in `implementation.md`.
Commit each verified action. Do not mark checks complete without evidence.

## Planning review

No blocking design ambiguity or inconsistency was found. The shared read-only
CI requirement does not change the release job's publication permissions.
Check-only file preservation permits ignored dependency/style caches; it does
not permit tracked edits. Formatting repair is intentionally a writing command.
The exactly-two-var API rule includes constructor documentation, not separate
constructor entries. Linux installation and hook safety remain implementation
verification obligations, not unresolved design decisions.

The checklist was reviewed as read-only context against all seven acceptance
criteria. Remaining mechanism risks are listed above; no design change is needed.

Local acceptance now confirms all three runtime suites, build/release suites,
link fixtures, and online links. The built JAR contains only runtime sources,
license, and standard metadata. Diff review against pre-task commit `b90c66f`
confirms unchanged base dependencies and build code; source changes are docstrings
only. Final local quality verification now passes after documentation changes.
Actionlint 1.7.7 accepts both workflows. Fresh Linux setup and the final
acceptance evidence map remain open; hosted execution remains unverified.

Fresh Linux verification is complete on Ubuntu 24.04 x86-64 under QEMU/Podman.
Documented installation, cold shared checks, and focused tests pass with no
tracked-byte or index changes. See [acceptance.md](acceptance.md) for all seven
criteria. No design change was needed; hosted Actions remains unverified.
