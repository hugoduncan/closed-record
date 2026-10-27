# 018 Development quality checks and API documentation

## Intent

Unify local and CI formatting, code lint, prose lint, and API documentation.
Detect defects and stale references before merge. Adapt value-type task 010;
do not copy its runtime requirements or internal APIs.

## Context and reference

Reference checkout: `~/projects/hugoduncan/value-type/value-type-master`,
inspected at commit `d117bf540cdaa4d4e80994bb4500c370fda029df`.
Read `munera/closed/010-development-quality-checks/` and
`mementum/knowledge/verifying-development-quality-checks.md` there.
The checkout is research input, not a dependency.

Existing checks include `defclosedrecord` lint configuration, pinned format/lint
CI commands, three Clojure versions, build/release-tooling tests, and Markdown
links (`bb lychee:check`). Manual release repeats format/lint commands.
Extend these boundaries, not competing policies.

The user API is `defclosedrecord` and `check`; publicly visible `ClosedRecord`
and `-extra-keys` are internal. Preserve Clojure 1.11+ JVM support, excluding
ClojureScript and Babashka.

## Scope and contracts

### Commands and setup

Reference commands:

- `bb clj-fmt check|fix [PATHS...]`: explicit paths, or project scope when omitted.
- `bb clj-fmt:check` and `bb clj-fmt:fix`: project scope.
- `bb clj-kondo:copy-config`: prepare dependency lint configuration.
- `bb clj-kondo:lint`: prepare configuration and fail on warnings or errors.
- `bb vale`: sync styles and check selected prose.
- `bb api-docs`: regenerate committed `doc/API.md` with Quickdoc.
- `bb api-docs --check`: check freshness without writing.

Preserve `bb lychee:check`. Pin tools and styles for reproducible local and Linux CI
installation. Plan installation details; reuse suitable pins. Keep tooling outside
runtime dependencies and the source-only library artifact. Commands must not require user-global
configuration, warm caches, copied imports, or another local project.

### Formatting and code lint

Share one maintained scope: `src/`, `test/`, `dev/`, `test-build/`,
`test-release/`, `build.clj`, `bb.edn`, `deps.edn`, owned lint hooks/configuration,
and any new tooling or tooling-test paths. Exclude generated output,
dependency imports, and caches. Required missing paths must fail with a
useful diagnostic, not silently reduce coverage.

Retain useful analysis of `defclosedrecord` fields, methods, and constructors.
Prefer the existing `defrecord` mapping unless tests show it is insufficient.
Use narrow, explained exceptions for intentional invalid test syntax, not
broad lint suppression. Resolve relevant dependency configurations without
letting an alias that replaces dependencies omit another alias's imports.
Fix scoped findings without changing library or test behavior.

### Prose and API reference

Vale covers `README.md`, Markdown under `README/`, prose in `AGENTS.md`, and
`doc/API.md`. Adapt the reference Vale/proselint baseline with closed-record
vocabulary. Preserve fenced code and lambda instructions through narrow
exclusions. Report suggestions; fail on warnings, errors, or setup failures.
Keep `CHANGELOG.md`, `munera/`, `mementum/`, `ramora/`, and historical evidence
outside this initial prose gate. This does not reduce Markdown link coverage.

Generate deterministic UTF-8 Markdown from owned configuration and docstrings.
Document exactly `closed-record.core/defclosedrecord` and
`closed-record.core/check`, their arguments, options, constructors, rejected
operations, literal-key behavior, and error contracts. Exclude private vars,
`ClosedRecord`, `-extra-keys`, and implementation namespaces without changing
visibility. State the JVM-only support boundary; `.cljc` source must not imply
ClojureScript support.

Include a generated-file notice, regeneration/check commands, a README link,
and source links to `https://github.com/hugoduncan/closed-record` on `master`.
Link the README back to the API reference. Fix generated prose at its source;
avoid duplicate long examples. Missing or stale output must fail with a
repair command and remain unchanged. Invalid arguments must fail.

### Hooks and automation

Provide pre-commit formatting-fix and lint hooks. Check staged content, not
unstaged working-tree edits: format selected staged files within the maintained
scope; lint the maintained project scope as represented by the index.
An unstaged correction must not hide a staged lint warning or error.

Formatting repairs must stop the commit for review and manual staging, even
if the repaired content passes. Never stage edits. Preserve unstaged and
unrelated edits. If a repair conflicts with unstaged edits, discard the repair,
restore those edits, and stop the commit. Document these outcomes. Planning
selects the mechanism.

CI must run format, lint, Vale, API freshness, and focused tooling tests on
pull requests and pushes to `master`, using the local commands. Checks must
not repair tracked files or skip failed setup. Retain read-only permissions,
credential-free checkout, fork support, and existing verification jobs.
Reuse the shared format/lint policy in manual release checks; do not add new
publication behavior or change release gates. Keep link validation with lychee.

## Design choices and limits

Prefer thin Babashka tasks and standard configuration. Add wrappers or analysis
only for contract needs. Use focused real-tool/file/process tests, not a framework.
Document setup and scopes in `README/development.md`; add a short Ramora
artifact description and index link without duplicating instructions.

Runtime features, API redesign, benchmark changes, consumer lint exports,
documentation hosting, publication, branch protection, and historical prose
cleanup are out of scope.

## Acceptance criteria

1. Documented setup and commands pass in a fresh checkout with empty user
   configuration and cold project imports. Record Linux installation evidence;
   distinguish local command validation from hosted Actions execution.
2. Real formatting defects, lint warnings without errors, and lint errors
   fail checks. Explicit format repair succeeds. Missing required scope fails.
   Valid macro bindings pass while unresolved method-body symbols fail.
3. Real Vale suggestions pass; warnings and errors fail. Tests use the owned
   exclusions to protect code/lambda text while detecting ordinary prose defects.
   Missing tools, styles, files, or invalid results cannot pass.
4. Missing/stale API output fails without writes; regeneration restores success
   and repeated generation gives identical bytes. Independent content tests
   enforce the selected API, contract, notice, and links, and reject a controlled
   generator-content defect even when freshness would pass.
5. Disposable Git repositories prove these hook outcomes, with unchanged indexes:
   - Clean staged content passes despite unrelated unstaged edits.
   - Staged lint warnings or errors block commits despite unstaged corrections.
   - Formatting repairs block commits and remain unstaged for review; after
     manual staging, clean content passes.
   - Partial staging preserves unstaged edits; conflicting repairs are discarded
     and commits stop. Unrelated edits remain unchanged in every case.
   All check-only commands preserve tracked files on success and failure.
   CI shares scopes and thresholds; actionlint accepts changed workflows.
6. Existing runtime, build, release-tooling, and link checks pass after baseline
   fixes. Runtime dependencies, artifact contents, and release behavior remain
   unchanged. Record commands, versions, exits, and verification limits.
7. Contributor and architecture documentation describe setup, commands, scopes,
   generated files, and hook behavior without requiring absolute local paths.
