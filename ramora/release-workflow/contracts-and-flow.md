# Contracts and Flow

## Preparation

Version is `0.1.<git rev-list --count HEAD>` with complete history. The tag is
`vVERSION`. Maintain an Unreleased section and dated version sections.

Run `clojure -M:release -m closed-record.release prepare` from clean master.
Require complete history and nonempty valid Unreleased notes. Stamp the next
commit-count version and UTC date, preserve earlier entries, leave an empty
Unreleased section, and commit only CHANGELOG.md as `Release vVERSION`.
Check the resulting version. Reject invalid/duplicate notes before changes.
No tag, push, or dispatch occurs; partial local failures need inspection.

## Workflow

Only workflow_dispatch from master is allowed. A boolean dry-run input defaults
to true; only explicit boolean false permits publication. There is no target
input. Check out the dispatch SHA with full history and do not retarget it.

Use one Ubuntu/Temurin 21 job with CI-consistent pins, finite timeout, and
non-cancelling release concurrency. The job has contents-write permission;
checkout credentials are not persisted. Put secrets only on the publishing
steps that need them. Values are arguments/environment data, not generated code.

Before writing remotely, run format/lint, all three runtime aliases, focused
build tests, and release tests. Normal fail-fast execution is allowed.
Upload hidden scry failure evidence as in CI. No benchmark run is required.

Select nonempty prepared notes matching the checked version for publication.
Dry run may use valid Unreleased notes only when that version section is absent;
report preparation required. Malformed or empty matching notes fail.

Dry run builds with `clojure -T:build jar` and reports SHA, version, tag, and notes
source. It does not receive Clojars secrets or create commits/tags/releases/uploads.
Workflow artifact uploads are allowed but not required.

## Publication

After all checks, explicit authorization, and prepared notes:

1. Validate required credential presence without printing secret values.
2. Create and push the Git tag at the checked SHA without force. Do not update master.
3. Run `clojure -T:build:deploy deploy`: check Clojars credentials, build, and call
   stock deps-deploy with that JAR/POM. Return artifact paths and version.
4. Run ordinary `gh release create` with explicit repository/tag/title,
   `--verify-tag`, `--notes-file`, and the deployment JAR/POM. Authenticate via
   GH_TOKEN. Do not rebuild between deploy and asset upload; do not use clobber.

The workflow adds no retry, rollback, release-edit, or resume logic. A failed
command stops subsequent commands. Internal deps-deploy/gh retries, redirects,
checksum handling, and temporary draft deletion are accepted. No separate gh API
write implementation, transport replacement, or cross-service preflight is needed.

Publication is not atomic. A failed step can have remote effects. Ordinary
service conflict responses are sufficient; the workflow does not prove absence
or independently verify every remote byte. Report step outcomes and require
maintainer inspection after failure and before rerunning a partial release.

## Local interfaces

`jar` and `deploy` return `:version`, `:jar-file`, and `:pom-file`.
Deployment optionally appends those fields to `:github-output` after success.
Notes stay outside `target/`, which each build removes.

`clojure -M:release -m closed-record.release notes NOTES_FILE GITHUB_OUTPUT [--preview]`
validates the full changelog, writes the selected body, and appends `sha`,
`version`, `tag`, `source`, and `preparation-required` outputs. Only `--preview`
permits Unreleased fallback. See [maintainer commands](../../README/releases.md).
