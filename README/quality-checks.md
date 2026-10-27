# Quality checks

Run commands from the repository root. These tools are for development, not
runtime dependencies. See [development](development.md) for runtime tests,
builds, and Markdown link checks.

## Commands and scope

```bash
bb clj-fmt:check
bb clj-kondo:lint
bb vale
bb api-docs --check
```

These checks do not repair tracked files. Failed setup stops the check.
Dependency imports, tool caches, and downloaded prose styles are ignored files.

Format and lint share the required scope in
[`quality_scope.clj`](../dev/quality/closed_record/quality_scope.clj): `src/`,
`test/`, `dev/`, `test-build/`, `test-release/`, `test-tooling/`, `build.clj`,
`bb.edn`, `deps.edn`, `.cljfmt.edn`, and `.clj-kondo/config.edn`.
Generated output, dependency imports, and caches are excluded. Missing required
paths fail; they do not reduce coverage.

Lint prepares dependency configuration before analysis. To prepare it separately:

```bash
bb clj-kondo:copy-config
```

Preparation resolves dependency aliases separately, including aliases that
replace dependencies. Do not copy imports from another checkout. Lint fails on
warnings and errors. Keep exceptions local and explain why they are needed.

Review formatting repairs before staging them:

```bash
bb clj-fmt:fix
bb clj-fmt check src/closed_record/core.cljc
bb clj-fmt fix src/closed_record/core.cljc
```

`bb clj-fmt check|fix` without paths uses project scope. Explicit paths require
only those paths and the owned formatter configuration, not the full scope.

Vale synchronizes the pinned proselint styles before checking `README.md`,
recursive Markdown under `README/`, `AGENTS.md`, and `doc/API.md`. Suggestions
are reported but pass. Warnings, errors, invalid results, and setup failures
fail. Fenced and inline code and lambda instruction lines are excluded; adjacent
ordinary prose is checked. Historical tasks and Ramora files are outside this
prose scope, but remain subject to Markdown link checks.

## Generated API

[`doc/API.md`](../doc/API.md) is committed output. Edit source docstrings or
[`api_docs.clj`](../dev/quality/closed_record/api_docs.clj), not the output.
Then regenerate and verify:

```bash
bb api-docs
bb api-docs --check
```

The check compares UTF-8 bytes and does not write missing or stale output.
Quickdoc is pinned in `deps.edn`. It documents the two user API vars with JVM
reader features. Independent content tests check the API contract; freshness
alone does not prove that the content is correct.

## Commit hooks

Install Python 3.12 and the pinned runner in a separate environment:

```bash
python3.12 -m venv "$HOME/.local/share/closed-record-pre-commit"
"$HOME/.local/share/closed-record-pre-commit/bin/python" -m pip install pre-commit==4.6.0
export PATH="$HOME/.local/share/closed-record-pre-commit/bin:$PATH"
pre-commit --version
pre-commit install
```

The hooks also need Babashka, Clojure CLI, Java, cljfmt, and clj-kondo on PATH.
An explicit Git `core.hooksPath` prevents pre-commit installation. Resolve that
local setting before installation; do not overwrite another hook manager.

During a commit, pre-commit temporarily removes tracked unstaged changes.
Formatting selects staged owned files; lint checks the full indexed scope on
every commit. Untracked defects are excluded. An unstaged correction cannot
hide a staged defect. Use real commits to check this behavior, not direct hook
task calls against working files.

A formatting repair stops the commit before lint. Repairs remain unstaged:
review the diff, stage the intended changes manually, and commit again. Hooks
never stage files. Nonconflicting unstaged edits are restored. If restoration
conflicts with a repair, pre-commit discards the repair, restores the original
edits, and stops the commit. Separate the edits and retry; do not discard work
to bypass the failure.

## Focused tests

Install the quality tools and synchronize styles, then run:

```bash
vale sync --config=.vale.ini
clojure -Srepro -M:quickdoc:tooling-test -m scry.cli --dir test-tooling
```

Tests use real tools, temporary files, and isolated Git repositories, including
installed-hook commits. They do not install hooks in the working repository.
Inspect the exit code and structured `.scry-results/` failure files. CI uploads
failures as `scry-tooling`. Runtime, build, release, and link suites remain
separate checks.
