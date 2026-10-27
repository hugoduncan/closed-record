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
Add development-only `:quickdoc` and `:tooling-test` aliases; the latter
supplies scry 0.1.61. The focused command is
`clojure -Srepro -M:quickdoc:tooling-test -m scry.cli --dir test-tooling`.
Use explicit project configuration and reproducible dependency resolution;
verify with empty user configuration. Keep `bb lychee:check` unchanged.

### Shared format and lint policy

Own one scope list: `src`, `test`, `dev`, `test-build`, `test-release`,
`test-tooling`, `build.clj`, `bb.edn`, `deps.edn`, and owned lint/formatter
configuration and hooks. Enumerate owned lint files separately from imports.
Validate required paths before setup or execution; never filter missing paths
away. Explicit formatting validates only its supplied paths. Both project
format aliases and argument-free `bb clj-fmt check|fix` use this policy.

Prepare dependency imports from separately resolved base and alias bases:
`:test`, `:build`, `:build-test`, `:bench`, `:nrepl`, `:deploy`, `:quickdoc`,
and `:tooling-test`; include version overrides without losing other bases.
Path-only release aliases add no dependencies. Combine distinct classpaths
before clj-kondo import preparation. A merged `:build` basis alone is unsafe.
Replace the tracked Malli import with reproducible dependency preparation;
ignore generated imports and caches, but retain owned configuration.

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

Separate generation from UTF-8 writing and byte comparison. Only regeneration
writes `doc/API.md`; freshness and invalid-argument failures never write.
Independent content tests must reject a controlled generator defect even
after regenerating a fresh reference.

Vale selects `README.md`, recursive `README/**/*.md`, `AGENTS.md`, and
`doc/API.md`; validate required roots/files. Use owned Vale/proselint
configuration and ClosedRecord vocabulary. Protect fenced code and lambda
lines narrowly. Sync styles, request JSON with suggestions visible, and
validate results: suggestions pass; warnings, errors, unknown severities,
invalid results, and setup failures fail. Fix generated prose at its source.

### Hooks and automation

Use local `pre-commit` hooks installed with `pre-commit install`.
Its staged-files context temporarily removes tracked unstaged changes.
Run formatting on staged maintained Clojure/EDN files, selected through the
shared scope policy. Run project lint with `always_run: true` and
`pass_filenames: false`, so all indexed scope is checked on every commit.
For hook lint, enumerate index paths within the shared scope; ignore untracked
files and require indexed configuration. Set `fail_fast: true`: formatter
repairs must stop before lint can inspect repaired rather than staged content.
Do not substitute working-tree-only direct hook invocation for commit tests.

Use pre-commit's change detection and restoration: repairs stop the commit
and remain unstaged; nonconflicting unstaged edits are restored; conflicting
repairs are rolled back before restoration. Never stage edits. Verify these
properties with pinned pre-commit and real commits before accepting this
mechanism. Isolate fixture Git configuration and inherited `GIT_*` variables.

Extend the existing CI quality job with shared commands and focused tests.
Retain all other jobs, read-only permissions, credential-free checkout, and
fork support. Replace only manual release format/lint commands and required
setup; preserve publication behavior. Validate workflows with actionlint.

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
