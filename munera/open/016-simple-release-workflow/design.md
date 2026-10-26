# 016 Simple release workflow

## Intent and reference

Publish one `org.hugoduncan/closed-record` release with the small build/deploy
pattern in `~/projects/hugoduncan/scry/scry-master/build.clj`: check credentials,
build the JAR/POM, call stock `deps-deploy/deploy`, and return artifact details.
Use ordinary `gh release create` for GitHub publication.

[015](../../closed/015-release-workflow/design.md) is closed without completion.
Its history is evidence, not inherited requirements. The
[release architecture](../../../ramora/release-workflow.md) describes this
replacement. No release has been published by these tasks.

## Scope

### Build and deploy

Build tooling owns packaging and Clojars deployment. Keep the existing source-only
artifact, EPL license, runtime dependencies, and `0.1.<Git commit count>` version.
Use stock deps-deploy 0.2.2 outside runtime dependencies. Deployment checks
`CLOJARS_USERNAME` and `CLOJARS_PASSWORD` before building, builds from HEAD,
then deploys that JAR/POM. This deliberately permits rebuilding after verification,
as scry does. Use the deploy result paths for GitHub assets.

### Notes and preparation

Create `CHANGELOG.md` and a small local preparation command from the clean
pre-015 baseline. Do not retain the task-015 implementation.
Preparation requires clean master and complete history. It moves nonempty
Unreleased notes into `0.1.<HEAD count + 1>` with the current UTC date, leaves
empty Unreleased, preserves prior entries, and commits only the changelog as
`Release vVERSION`. It does not tag, push, dispatch, or publish. Reject invalid
or duplicate notes before modification; leave partial local failures for
inspection without automatic reset.

The same small, line-based contract governs preparation and note selection:

- A section heading starts in column one with `##`, but not `###`. Only exact
  `## [Unreleased]` and `## [VERSION] - YYYY-MM-DD` lines are valid. VERSION is
  `0.1.N`, where N is a positive decimal integer without leading zeros; the date
  must be a valid calendar date. No trailing spaces or alternate heading forms.
- Require exactly one Unreleased section, before all version sections. Zero
  version sections are allowed. Preamble before the first section is ignored.
  A body extends to the next section heading or end of file. No Markdown parsing
  is required: fences and comments do not hide section headings.
- A nonempty body contains a non-whitespace character. Body Markdown is opaque;
  comments and subheadings count as content. Duplicate means repeated Unreleased
  or version headings, regardless of date, not repeated body text.
- Validate all section headings and bodies before preparation or selection. Every dated
  section, including prior versions, must be nonempty. Unreleased may be empty
  unless selected for preparation or preview. No date/version ordering is required.
  Preparation also rejects an existing section for the next version.
- Publication selects only the section matching the checked-out commit's version.
  Dry run may select nonempty Unreleased only if that section is absent, and must
  report preparation required. Invalid or duplicate sections fail validation;
  an empty or malformed matching section must never cause fallback.

### Manual workflow

Use one job with one full-history checkout of the dispatch SHA on master.
There is no separate target input, remote-tip resolution, control checkout,
artifact transfer protocol, or second publication job. Later branch advancement
does not change the checkout. Reject dispatch from another branch.

Use only `workflow_dispatch`, with boolean `dry-run` defaulting to true.
Only explicit boolean false permits publishing. Run format/lint, all three
runtime suites, focused build tests, and release tests before publication.
Use Ubuntu, Temurin 21, CI-consistent tool/action pins, a finite job timeout,
and one concurrency group with cancellation disabled. Preserve scry failure
artifacts. Do not run benchmarks.

Dry run builds locally, reports SHA/version/tag/notes source, and performs no
release writes. It does not receive Clojars secrets. Publication steps alone
receive Clojars credentials or `GH_TOKEN`; never print secret values. The one
job has contents-write permission for publication; step gating, not separate
job permissions, is the chosen boundary. Disable checkout credential persistence.
The workflow trusts maintainer-controlled master code.

### Publication and failure handling

After checks and matching notes, validate credential presence, create and push
`vVERSION` for HEAD with ordinary Git (no force), run the build/deploy command,
and run `gh release create --verify-tag --notes-file` with explicit repository,
release title, and the resulting JAR/POM. Do not update master, generate notes,
edit an existing Release, or use asset clobbering.

Use stock deps-deploy and gh behaviour. Internal retries, redirects, checksum
handling, and gh's temporary draft cleanup are accepted. These are not blockers
and do not require source/bytecode audits or custom transport controls.

Stop subsequent workflow steps when a command fails. Add no outer retry loop,
rollback, or automatic resume. Normal Git/service conflict responses suffice;
do not build a cross-service preflight or prove remote absence. Publication is
not atomic or guaranteed exactly once. Report the failed step and instruct the
maintainer to inspect GitHub and Clojars before another attempt. Never describe
command success alone as independent verification of remote files.

## Cleanup and boundaries

Revert all task-015 implementation, including preparation and notes code, tests,
changelog, aliases, dependencies, and implementation-specific documentation. In particular, do not retain
custom Wagon/Resolver deployment, bundle/manifest transfer, response/evidence
classification, or a publication process framework as dormant alternatives.
Implement only the small notes/preparation support needed here. Do not merge the old task's
checklist or implementation log into this task.

Out of scope: automatic releases, additional artifacts, signing, dependency
upgrades, transport hardening, cross-service transactions, secret configuration,
branch protection, and a real publication for acceptance.

## Acceptance

1. Build deployment follows scry's credential-check → build → stock deploy path;
   only one artifact is deployed, and runtime packaging/dependencies are unchanged.
2. Local preparation and notes tests pass, including real temporary Git fixtures.
3. Workflow inspection/actionlint confirms manual master-only dispatch, default
   dry run, explicit-false writes, one fixed checkout, all checks before writes,
   secret placement, finite timeout, and serialized runs.
4. GitHub publication uses normal gh with the prepared notes, existing tag, and
   deployed build outputs. Dry run performs no publication writes.
5. All task-015 implementation has been reverted before replacement. Focused tests cover our own logic;
   acceptance does not depend on reproducing dependency transport internals.
6. Java 21 quality checks, three runtime suites, focused build/release tests,
   and actionlint pass. README covers preparation, separate push, preview,
   publish, credentials, and manual failure inspection. Record local results and
   hosted limits; do not push, configure secrets, or publish for acceptance.
