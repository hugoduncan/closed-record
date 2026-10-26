# Simple release implementation plan

## Approach

[design.md](design.md) owns scope and acceptance. Follow scry's single-artifact
`deploy`, not `deploy-all` or task 015's abandoned contracts. The pre-015 revert
is complete (commit `6e66d98`); do not repeat it or restore its module hierarchy.
Keep runtime source, dependencies, source-only packaging, license, and Git-count
version unchanged. No real publication is required for acceptance.

### Build and deployment

Add stock `slipset/deps-deploy` 0.2.2 under `:deploy`, outside runtime dependencies.
Keep `clojure -T:build jar` usable without deployment dependencies or credentials.
Extend its result with `:pom-file`, retaining `:jar-file` and `:version`.
`clojure -T:build:deploy deploy` checks nonblank Clojars credentials before
building, calls `jar`, lazy-loads stock `deps-deploy.deps-deploy/deploy`, calls it
with `{:installer :remote :artifact jar-path :pom-file pom-path}`, and returns
the artifact map. Do not add an injected transport or a second artifact.

Pass the successful deploy result paths to subsequent workflow steps, rather
than reconstructing filenames. A small optional `:github-output` argument to
`deploy` will append version/JAR/POM step outputs after success when supplied; it must not
replace the normal return map or print credentials. The output keys are `version`, `jar-file`, and `pom-file`. Test output formatting as
local data handling. No bundle, manifest protocol, or publication framework.

### Notes and local preparation

Use small functions under `dev/release` and focused tests under `test-release`.
Add composable `:release` and `:release-test` aliases without `:main-opts`.
Use `clojure -M:release -m closed-record.release prepare` from the repository
root. Seed `CHANGELOG.md` with factual Unreleased notes, not invented releases.

One shared line-based validator implements every changelog rule in the design.
Keep it separate from build tooling. Pure functions handle validation,
selection, and stamping with an explicit UTC date. Preparation checks clean
master and full history, validates before writing, commits only CHANGELOG.md
as `Release vVERSION`, then checks the resulting commit-count version. Report
partial failures without reset. Run without concurrent local edits.

Add a small `notes` CLI entry point for the workflow. It derives SHA/version/tag
from HEAD, validates the full changelog, writes the selected body to a supplied
file, and reports notes source and preparation status as step outputs. Only an
explicit preview option permits Unreleased fallback. Store notes under
`RUNNER_TEMP`, not `target/`, because packaging deletes `target/`.

CLI contract: `notes NOTES_FILE GITHUB_OUTPUT [--preview]`. Outputs are `sha`,
`version`, `tag`, `source`, and `preparation-required`. Selection requires full
history and validates before writing either file.

Use pure state-based tests and real isolated Git fixtures, explicitly initialized
on master. Disable inherited Git overrides, hooks, and signing in fixtures.
Use Clojure-1.11-compatible subprocess APIs. Test credential rejection with
absent/blank environment values before build effects; never exercise a real
upload. Inspect the short stock deployment call instead of mocking services or
auditing dependency internals.

### Workflow and operation

Use one Ubuntu/Temurin 21 job in `.github/workflows/release.yml`, only
`workflow_dispatch`, default-true boolean `dry-run`, and a master-ref guard before
checkout. One full-history checkout uses `github.sha` with credential persistence
disabled. Set a finite timeout and one constant release concurrency group with
cancellation disabled. Use the action/tool pins in CI; record runner `gh --version`.

Require explicit typed false for every publication step. Guard against missing
input coercion as well as true; use a serialized typed-value comparison such as
`toJSON(inputs['dry-run']) == 'false'`. Retain normal success gating. Pass values
as environment data or arguments, not generated shell/Clojure source.

Run format/lint, three runtime suites, focused build tests, and release tests
before publication. Preserve hidden scry failure files before another invocation
can clear them; normal fail-fast sequencing suffices. Extend read-only ordinary
CI to cover release code/tests. No benchmarks.

Dry run selects notes, builds locally, and reports SHA/version/tag/source and
preparation status without Clojars secrets or release writes. Publication
requires matching notes, validates Clojars and GitHub credential presence before
tagging, creates/pushes `vVERSION` at HEAD without force, deploys, then invokes
`gh release create --verify-tag --notes-file` with explicit repository/title and
the deploy step's JAR/POM outputs. Check those files exist; do not rebuild or
clean between deploy and upload. Authenticate Git with the workflow token only
for that command, without storing credentials. Scope Clojars secrets to validation
and deployment; scope `GH_TOKEN` to publication steps that need it.

Use named steps and a failure summary for manual inspection. Add no remote
absence checks, outer retries, rollback, clobbering, release edits, or resume.

## Risks

- The single job has contents-write permission and trusts maintainer-controlled
  master. Step gates and secret placement are the chosen boundary.
- Git-count versions change with commits; later master advancement does not
  retarget a dispatch. A new dispatch checks its own SHA and notes again.
- Preparation can leave local changes after commit failure. Publication can
  leave partial remote state. Maintainers must inspect before retrying.
- Stock tools can retry internally or clean up drafts. Command success is not
  independent remote-file verification. These are accepted limits.
- Local tests/actionlint cannot establish hosted permissions, credentials, or
  publication success. No known blocker requires a design decision.

## Slice order

1. Deployment contract and build tests (acceptance 1, 5).
2. Shared notes validation, preparation, selection, and Git tests (acceptance 2).
   Commit pure notes support as 2a, then Git preparation as 2b and notes CLI integration as 2c.
3. Manual workflow and ordinary CI coverage (acceptance 3, 4), implemented and locally checked.
4. Maintainer documentation and Java 21 acceptance checks (acceptance 5, 6).

Follow [steps.md](steps.md). Record commands, results, decisions, and hosted limits
in implementation.md; commit each completed slice. Do not push, configure
secrets, or publish. README now links to development and release instructions; architecture docs
describe the implemented path. All four slices passed local acceptance. Task 015 remains historical only.

## Plan and design review

Reviewed plan ambiguities with steps.md as read-only context. The deploy-output
handoff is now explicit; no blocking plan ambiguity remains. Reviewed design.md
for internal conflicts and against all three release architecture pages, scry's
single-artifact deploy, the baseline build/CI, and the historical task-015 link.
No inconsistency requires a design change. The design leaves CLI/output details
to implementation; this plan selects them. No unresolved scope question remains.
Local Markdown links and `git diff --check` pass. This is planning evidence only;
no runtime tests, hosted checks, or publication ran.
