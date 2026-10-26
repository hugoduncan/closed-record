# Releases

Releases are manual. Use Java 21, the Clojure CLI, Git with full history,
and GitHub CLI (`gh`) with access to `hugoduncan/closed-record`.
Run local commands from the repository root. Do not edit concurrently with
preparation. See [development](development.md) for local checks.

## Prepare and inspect

Commit the intended code changes and nonempty notes under the exact
`## [Unreleased]` heading in `CHANGELOG.md`. Start on clean `master` with
complete history:

    git switch master
    git status --short
    git rev-parse --is-shallow-repository
    clojure -M:release -m closed-record.release prepare

The shallow-history check must print `false`. Preparation rejects a dirty
worktree, another branch, or invalid notes. It moves Unreleased notes into
`## [0.1.N] - YYYY-MM-DD` with the UTC date, leaves empty Unreleased, and
commits only the changelog as `Release v0.1.N`. `N` is the resulting HEAD
commit count. It does not tag, push, dispatch, or publish.

Inspect the result before a separate push:

    git show --stat HEAD
    git show HEAD -- CHANGELOG.md
    git rev-list --count HEAD
    git status --short
    git push origin master

Confirm that the commit contains only the intended notes and its version
matches the count. Do not add commits between preparation and dispatch:
new commits change the version. If master advances, prepare notes for the
new version before publication.

Dated sections must have unique versions and nonempty bodies. The full
changelog is validated, including earlier entries. See the
[changelog contract](../munera/open/016-simple-release-workflow/design.md#notes-and-preparation)
for exact heading rules.

## Preview

Dispatch the [Release workflow](../.github/workflows/release.yml) on master:

    gh workflow run release.yml --repo hugoduncan/closed-record --ref master -f dry-run=true
    gh run list --repo hugoduncan/closed-record --workflow release.yml --limit 5

Open the selected run with `gh run view RUN_ID --repo hugoduncan/closed-record --web`.
Inspect checks and the summary's SHA, version, tag, notes source, and
preparation-required flag. Dry run is the default in the Actions interface.
It builds locally without release writes or Clojars secrets. If matching
notes are absent, preview can use nonempty Unreleased notes; the summary
then reports that preparation is required. Invalid notes never cause fallback.

## Credentials and publication

Before publication, a maintainer must set these repository Actions secrets:

- `CLOJARS_USERNAME`: the authorized Clojars account name.
- `CLOJARS_PASSWORD`: a Clojars deploy token authorized for
  `org.hugoduncan/closed-record`.

Do not put values in source, command logs, or notes. GitHub writes use the
workflow's `github.token`, passed as `GH_TOKEN` only where needed. The job
has contents-write permission; checkout does not retain credentials.
Dry run does not need Clojars credentials.

After a successful preview of the prepared commit, explicitly authorize writes:

    gh workflow run release.yml --repo hugoduncan/closed-record --ref master -f dry-run=false

Each dispatch fixes its own master SHA. A preview of an earlier SHA does
not verify a later dispatch. Publication repeats all checks and requires
notes matching its version. Runs are serialized without cancellation.

After format/lint and runtime/build/release tests, the workflow validates
credentials, creates and pushes `vVERSION` without force, then runs
`clojure -T:build:deploy deploy`. That command checks credentials, rebuilds
one JAR/POM, and calls stock deps-deploy 0.2.2. It returns `:version`,
`:jar-file`, and `:pom-file`. The workflow uses those paths for
`gh release create --verify-tag --notes-file`, not a later build.
Deployment tools are not runtime dependencies.

Inspect the remote tag and commit, Clojars coordinate/version/files, and
GitHub Release notes and JAR/POM assets before declaring completion.
A successful command is not independent verification of remote files.
Hosted permissions, credentials, and publication have not been verified
by local acceptance checks.

## Failures

For preparation failure, inspect `git status`, HEAD, and CHANGELOG.md.
A failed commit can leave local changes. There is no automatic reset.

For workflow failure, preserve the run URL, SHA/version, logs, and available
failure artifacts. Inspect both GitHub and Clojars before another attempt.
The tag or some files can already exist. Stock tools can retry internally;
gh can remove its temporary draft. Publication is not atomic.

Later steps stop on failure. There is no outer retry, rollback, or partial
resume. Do not blindly rerun, force tags, or overwrite assets. Manual repair
can require service support or a new version.
