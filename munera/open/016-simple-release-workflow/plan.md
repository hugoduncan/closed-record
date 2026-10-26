# Simple release implementation plan

## Approach

Follow [design.md](design.md), not task 015's transport contracts. Implement
three coherent slices. Do not restart dependency-source audits or add a general
release framework. Tests must prove our logic, not stronger guarantees than
stock deps-deploy or gh provides.

## 1. Simplify and add standard deployment

Start from the pre-015 implementation baseline (`c9066bd`). All task-015 source,
tests, changelog, release/deploy aliases, and packaging-doc additions are removed.
Keep the closed task history, not its implementation. Do not restore its modules
wholesale or replace its frameworks with equivalent abstractions.

Create small notes/preparation helpers under `dev/release` and focused tests under
`test-release`. Add composable `:release` and `:release-test` aliases as needed,
without data.json unless actual code requires it. Add `:deploy` with stock
deps-deploy 0.2.2. Seed factual Unreleased notes, not historical releases.
Implement `clojure -M:release -m closed-record.release prepare` with the design's
clean-master, count/version, notes-only commit, and no-remote-write requirements.
Use scry's small changelog approach rather than the old task's adapter hierarchy.
Retain existing pre-015 build/runtime test boundaries.

Extend `build.clj` with scry-style credential validation and `deploy`:
validate environment, call `jar`, call stock
`deps-deploy.deps-deploy/deploy` with `{:installer :remote :artifact jar-path
:pom-file pom-path}`, return artifact details. Have `jar` return its POM path
as well as version/JAR path. Command: `clojure -T:build:deploy deploy`.
Lazy-load deployment dependencies. Do not copy scry's second-artifact machinery.

Use existing real build fixtures to verify artifact paths, coordinate, version,
and runtime dependency isolation. Test missing credentials before any build or
network work. Inspect the short stock-library call; no mock service or real
Clojars publication is needed. Do not add dependency injection solely to count
network calls. Use a small notes extraction command if the workflow needs one.

## 2. Add the manual workflow

Create `.github/workflows/release.yml` with one job, master-only manual dispatch,
boolean default-true dry run, full-history checkout at `github.sha`, non-persistent
credentials, finite timeout, and non-cancelling concurrency. No target input.
Use CI's tool/action pins and Java 21. Use runner-provided gh and record
`gh --version`; a separately installed CLI pin is not required.

Use GitHub's typed boolean input for the publication condition; missing/invalid
input must not authorize writes. Pass values through environment variables or
arguments, not generated Clojure/shell source. Run quality, runtime, build, and
release tests before writes. Normal fail-fast workflow steps suffice;
collect failure evidence with `if: failure()` as in CI. Do not require all runtime
suites to continue after failure. Extend ordinary CI to cover new tooling.

Resolve version from the one checkout and select notes. Build a dry-run preview
with `clojure -T:build jar`. For publication, validate credentials, create the
local tag and push it without force, call `clojure -T:build:deploy deploy`, then
create the GitHub Release. Use workflow-token authentication for Git push without
persisting credentials. A failed step prevents subsequent publication steps.

The deploy command builds before upload. Derive its conventional output paths
from the checked version and assert they exist before passing them to gh. No
build or cleanup may occur between deployment and GitHub upload. Use
`gh release create` with explicit tag/repository/title, `--verify-tag`,
`--notes-file`, JAR and POM. Set `GH_TOKEN` only where needed. Do not use `--clobber`.

Do not add service absence checks, gh API publication writes, outer retries, or
rollback. Accept the tools' internal behaviour. GitHub step logs and a short
summary identify completed/failed steps; no structured outcome engine is needed.

## 3. Document and verify

Document prepare, inspect, separate push, manual dry run, manual publication,
Clojars credentials, and partial-failure inspection. Explain that deploy rebuilds
from the same checkout, internal retries/cleanup are allowed, and rerunning a
partial release is not automatic recovery. Update architecture paths to match
what remains. No claims of hosted success without evidence.

Run Java 21 quality checks, all three runtime suites, focused build/release
suites, and actionlint. Use scry exit codes and structured failure files. Check
that removed namespaces/aliases have no live references and runtime packaging is
unchanged. Record results in implementation.md and mark steps continuously.
Commit each completed slice. Do not publish for acceptance.

## Risks and limits

Standard tools can leave partial remote state or return before every remote file
has been independently checked. Maintainers inspect services after publication
or failure. Git/service conflicts are handled by ordinary command failure, not
an atomic preflight. The single job trusts master code and has write permission;
credentials are limited to publishing steps. These are accepted trade-offs, not
reasons to reconstruct task 015.
