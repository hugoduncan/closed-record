# Development quality checks

The development boundary owns shared format/lint scope, prose policy, generated
API documentation, and staged-content hooks. Thin Babashka tasks call helpers
under `dev/quality/closed_record/`; real-tool tests live in `test/tooling/`.
Tool dependencies do not enter the runtime basis or source-only JAR.

Local checks and CI use the same commands. Release reuses format/lint without
changing publication gates. Check commands preserve tracked files; only explicit
format repair and API regeneration write owned output. Missing scope and failed
setup stop checks rather than reducing coverage.

Pre-commit supplies staged-content isolation and restoration. Formatting runs
first and stops commits after repairs; lint checks indexed project scope. No hook
stages changes. Disposable real commits verify this boundary, including partial
staging and restoration conflicts.

The API reference has separate freshness and content contracts. Vale checks
selected current prose, while lychee retains the broader tracked-Markdown scope.

See [contributor quality instructions](../doc/quality-checks.md) for commands,
scopes, generated output, tests, and hook recovery. See
[embodiment](development-quality/embodiment.md) for current artifacts and
verification limits. See
[development](../doc/development.md) for runtime and artifact verification.
