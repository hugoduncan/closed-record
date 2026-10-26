# 015 Release workflow

Closed without completion on 2026-10-23. The user requested the simpler scry
build/deploy approach. This document records the abandoned scope, not current
requirements. Do not continue its checklist or enforce its transport policies.
See [016 Simple release workflow](../../closed/016-simple-release-workflow/design.md).

## Intent

Prepare notes locally; verify and publish one release to GitHub and Clojars.
Default to dry run. Require explicit publication authorization.

## Architecture

The [full release workflow design](../../../ramora/release-workflow.md)
covers architecture, contracts, recovery, and verification. This task owns
scope and acceptance.

## Context and decisions

Task 013 provides a source JAR and POM for `org.hugoduncan/closed-record`,
versioned `0.1.<Git commit count>`. Task 014 provides format, lint, runtime
matrix, and build checks. No release exists.

References: scry's `.github/workflows/release.yml`, `bb/scry/release.clj`,
`bb.edn`, and `CHANGELOG.md` under `~/projects/hugoduncan/scry/scry-master`.
Unlike scry, publication is manual.

Use Java 21 and secret names `CLOJARS_USERNAME` and `CLOJARS_PASSWORD`.

## Scope and contracts

### Local preparation

Add `CHANGELOG.md` with a maintained `## Unreleased` section. Add a documented
local command that requires a clean working tree on `master`, full Git
history, and nonempty unreleased notes. The command:

- Computes `0.1.<current commit count + 1>`.
- Moves the notes under `## [VERSION] - YYYY-MM-DD`, keeping a new empty
  `Unreleased` section and all earlier entries.
- Commits only that change as `Release vVERSION` and verifies that the new
  commit count gives the expected version.

The local command is `clojure -M:release -m closed-record.release prepare`,
run from the repository root without concurrent edits.
It does not create a tag, push, dispatch a workflow, or publish. The maintainer
pushes separately. Stop clearly on failure; document partial-change recovery
without automatic reset or deletion. Version counts use canonical decimal spelling, without leading zeroes.
Reject duplicate version entries and empty or
malformed notes before modification. Do not invent historical releases.

### Manual workflow and immutable target

Use `workflow_dispatch` with a boolean `dry-run` input defaulting to true and
a target commit input (blank or a full 40-digit hexadecimal SHA-1). An omitted target selects the current remote `master`
commit. Resolve the target once to an exact SHA, require it to be on remote
`master` history, and use that SHA for all verification, packaging, and tagging.
Run the workflow definition from `master`; reject other dispatch branches.
Do not publish automatically on branch pushes, tags, or pull requests.

Use Ubuntu, Temurin 21, CI-consistent tool pins, full history, and timeouts. Serialize release runs without cancelling a publication
in progress. Missing or invalid dry-run input must never authorize publishing;
only an explicit boolean false does so.

### Verification and dry run

Run format, lint, all three runtime aliases, build checks, and release checks.
Include new release code in code-quality and CI coverage. Do not run benchmarks.
Keep runtime dependencies and the Git-count version rule unchanged.

Build the JAR and POM once for publication. Verify their coordinate and version
against the selected SHA. Preserve scry failure evidence as in CI.

A prepared release requires nonempty changelog notes for the commit's computed
version. Use that exact section as release notes. A
dry run of an unprepared commit may use nonempty `Unreleased` notes, as in
scry; its summary must say that preparation is still required. It must not
claim that this commit is ready to publish.

Dry runs do not create commits, tags, releases, or Clojars uploads and do not
require or expose Clojars credentials. They may upload the JAR, POM, and notes preview as workflow artifacts. Report SHA, version, proposed tag,
notes source, and readiness in the run summary.

### Publication

With `dry-run: false`, require matching prepared notes and all checks to pass.
Create `vVERSION` at the verified SHA, deploy the verified JAR and POM to
Clojars, then create a GitHub Release with the same files and changelog notes.
Do not rebuild in the deploy step. Keep the validated bundle directory private
and unchanged through publication; reject symbolic links and non-file entries. Do not update `master` from the workflow.

Before the first remote write, validate required credentials and check for
an existing tag, GitHub Release, or Clojars version. Fail on conflicts or
existing publication; never overwrite an artifact or move a tag. A failed
lookup is not evidence that a version is absent. A 404 response requires
validated resource visibility and endpoint semantics before it can prove absence;
a hidden or inaccessible repository must not clear preflight.

Publication across GitHub and Clojars is not atomic. Stop on failure, report
which operations completed, and document manual recovery. No automatic
rollback, deletion, redeployment, or partial-release resume is required.

Keep verification read-only. Grant write permission and deployment secrets
only to publication after verification. Do not log
secrets. Use the GitHub CLI (`gh`), not a custom GitHub HTTP client, for
GitHub API lookups and Release creation/uploads. Authenticate `gh` with the
GitHub token through `GH_TOKEN`. Keep Git ref operations in Git. Require
bounded, non-interactive commands with child-process cleanup, explicit repository selection, and no
automatic write retries. Reuse scry's secret names,
not its stored values: GitHub cannot export repository secrets. Document maintainer-supplied artifact authorization; credential setup is out
of scope.

## Approach and boundaries

Adapt scry's small changelog functions and standard deployment approach, not
its multi-artifact machinery or tag-trigger policy. Keep local preparation,
pure validation, and remote publication separate. Planning selects the command and language. Do not build a release framework. Build tooling owns packaging; release tooling owns notes and publication.
Keep deployment dependencies outside runtime dependencies.

Document preparation, pushing, dry-run dispatch, explicit publication, secret
setup, and partial-failure recovery. Do not claim a release exists until one
is actually published. Update the architecture artifact map and tooling docs.

## Out of scope

Automatic releases, extra artifacts, benchmarks, dependency upgrades,
signing, branch-protection changes, automatic recovery, and publishing a real
release as part of task acceptance.

## Acceptance criteria

1. Local preparation in a temporary Git repository produces one changelog
   commit with matching count/version, preserves earlier notes, and creates
   no tag or remote write. Invalid input fails safely.
2. Workflow validation confirms manual-only events, default-true dry run,
   explicit-false publication, immutable master-history target, full history,
   bounded jobs, serialized runs, and separated permissions and secrets.
3. Verification passes on all three Clojure aliases; format, lint, build, and
   release checks pass. actionlint accepts the workflow.
4. Tests cover changelog stamping/extraction, commit-count agreement, invalid
   targets and notes, dry-run gating, conflicts, and failure sequencing without
   real publication. Use real temporary Git fixtures and pure validation tests.
5. Publication uses the verified JAR/POM without rebuilding and the matching
   notes. GitHub API and Release operations use `gh`, not a custom HTTP client;
   Release creation requires the existing tag and cannot replace assets.
   Dry runs have no publication effects and need no Clojars secrets.
6. Documentation covers operation and recovery.
   `implementation.md` records commands, results, and any hosted verification
   limits. Do not push, configure secrets, or publish solely for acceptance.

## Publication command decision required

[CLI source review](github-cli-review.md) found that the required asset-bearing
`gh release create` retries uploads and deletes its draft on failure. This
conflicts with the no-retry/no-deletion contracts above. Those contracts remain
unchanged; publication wiring must wait for a maintainer decision.
