# Release workflow implementation plan

Closed without completion on 2026-10-23. The user requested the simpler scry
build/deploy approach. This document records the abandoned scope, not current
requirements. Do not continue its checklist or enforce its transport policies.
See [016 Simple release workflow](../../open/016-simple-release-workflow/design.md).

## Approach

Implement the [task design](design.md) and
[release architecture](../../../ramora/release-workflow.md). Keep packaging in
`build.clj`, runtime dependencies, versioning, and public API unchanged.
Do not publish for acceptance.

### Tooling and boundaries

Use JVM Clojure with the existing Clojure CLI.
Put helpers under `dev/release/closed_record/`, with `closed-record.release`
as the command entry point and small domain namespaces as needed. Keep pure
notes, request, bundle, and failure policy separate from filesystem, Git,
and remote operations. No general release framework.

Add composable aliases without `:main-opts`:

- `:release`: release source path and `org.clojure/data.json` 2.5.2 for typed
  workflow input and structured CLI/API output.
- `:release-test`: `test-release` path; compose with `:test:release`.
- `:deploy`: `slipset/deps-deploy` 0.2.2, as used by scry. Only publication
  loads the deployment namespace; preparation and verification do not need it.

Local preparation is `clojure -M:release -m closed-record.release prepare`.
Release tests are
`clojure -M:test:release:release-test -m scry.cli --dir test-release`.
Use Java 21; retain existing runtime suite aliases.
Use `gh` subprocesses for GitHub API and Release operations; do not implement
a GitHub HTTP client. Pin and verify the CLI version in the publish job. Use
JDK HTTP support only for bounded Clojars version lookups. Pin any additional
dependency before use. Review deps-deploy's retry and error behaviour before
wiring it into publication. Deployment accepts existing paths and never
invokes build.

### Preparation and validation

Apply the architecture's exact notes grammar, UTC date, full-history checks,
clean master requirement, HEAD recheck, and notes-only commit postconditions.
Preserve exact selected notes bytes during extraction; only stamping may
normalize boundary blank lines. Seed factual Unreleased notes, not historical
versions. On partial failure, report state and leave user work intact.

Parse typed `inputs` JSON as data. Only a present boolean false authorizes
publication; missing input remains non-publishing and invalid types fail.
Accept a blank target or a full commit SHA, not Git ref expressions. Fetch
remote master and fix one ancestor SHA before checks. Use isolated temporary
Git repositories and bare remotes for local effects, including failing hooks.

### Verification and transfer

Use exactly two jobs in `.github/workflows/release.yml`. Keep a checkout of
the dispatch revision (`github.workflow_sha`) for control tooling, and a
separate full-history target checkout for verification and packaging. Run
publication tooling and its dependency configuration only from the fixed
workflow revision, never from the target or downloaded bundle.

The read-only verify job checks quality, runs all three runtime suites
sequentially, then focused build/release tests. Preserve each failed suite's
hidden scry evidence before the next suite. Aggregate runtime outcomes before
allowing packaging. Build publication files once after all checks succeed.

Validate JAR/POM coordinate and version, then copy them with exact notes to
`release.jar`, `pom.xml`, and `notes.md`. Add `manifest.edn` containing only
SHA, version, and payload SHA-256 hashes. Keep the bundle outside the build
cleanup path. Pass the upload artifact ID, SHA, version, and canonical
publication gate as job outputs. Download that exact artifact from this run;
revalidate identity, metadata, file inventory, and hashes before publication.
The publication output is the literal string `true` only when the typed request
explicitly authorizes publication and matching prepared notes passed validation;
all other cases omit it or emit `false`. The job gate requires success and exact
string `true`. Notes readiness comes from verification, not bundle instructions.

### Publication and workflow controls

Pass GitHub's token to `gh` through `GH_TOKEN`; do not run `gh auth login` or
store credentials. Set the repository explicitly for each GitHub command.
Pass command arguments as data, without shell interpolation. Bound subprocess
execution, disable prompts, and keep secrets out of command lines and logs.
Expose the two named Clojars secrets only in publishing steps that need them.
Require prepared notes and successful verification. Preflight credential
presence and exact tag, Release, and Clojars version absence; uncertain lookup
results fail closed. Use `gh api` for exact GitHub resource lookups. A nonzero
CLI exit alone does not prove absence; document and classify the service
status and response, including authentication and repository-access failures.

Use create-only operations in order: Git tag, Clojars deployment, GitHub
Release and its assets. Keep create-only tag publication in Git. Use
`gh release create --verify-tag --notes-file` with the verified notes and
JAR/POM paths, an explicit tag, and `--repo`. Do not generate notes or allow
fallback tag creation. Do not use asset replacement (`--clobber`), release
edits, or automatic write retries. Inspect the pinned CLI's retry behaviour.
Document the chosen lookup endpoints and authoritative absence responses.
Keep a straight-line sequence with structured completed/uncertain operation
results; a failed `gh release create` can leave a Release or partial assets.
Test command construction and failure policy as pure data, and create-only
tags against a real temporary remote. Review adapters without publication or mocks.

Match CI's Ubuntu, Java, CLI, quality-tool, and action pins. Pin the added
artifact download action. Use 60-minute verification and 15-minute publication
timeouts, bounded remote requests, and one release-wide concurrency group
with cancellation disabled. Disable checkout credential persistence. Report
identity, notes source, readiness, and check outcomes even when checks fail.

## Risks

- Clojars and GitHub writes are not atomic. Timeouts, partial uploads, and
  external writers can leave uncertain state. Stop; never retry writes,
  overwrite, roll back, or resume automatically.
- Credential presence does not prove artifact authorization. Endpoint absence
  semantics and deployment-library behaviour need implementation evidence.
- Hooks and concurrent local edits can break preparation postconditions.
  Report partial state without reset, amendment, or deletion.
- Git-count collisions and history rewrites can make a version unusable.
  Artifact retention is finite; recovery must preserve the verified bundle.
- Local tests and actionlint do not prove hosted permissions or service writes.
  Record hosted limits; do not push or configure secrets for acceptance.

## Slice order

1. Notes policy and local preparation, with real Git fixture tests (criteria 1, 4).
2. Typed request, immutable target, and verified bundle contracts (2, 4, 5).
3. Publication preflight, adapters, and failure reporting (4, 5).
4. Two-job workflow and ordinary CI coverage (2, 3, 5).
5. Maintainer documentation and complete local verification (1–6).

Implement and verify each slice before the next. Update `steps.md`, append
commands/results to `implementation.md`, and commit each completed slice.

## Current implementation boundary

Slice 1 is implemented and locally verified. The preparation command uses real
Git operations and reports the failed phase without rollback. It checks exact
committed notes as well as parent, count, message, changed paths, and clean state.
Slice 2a implements pure typed request validation. It returns `:publish?` and
`:target` (nil for the remote tip, otherwise a lowercase full SHA-1). This flag
is authorization only, not notes readiness or the final workflow output.
Slice 2b implements fixed-target resolution from an existing full clone, with a
120-second limit per Git command. It rejects shallow clones rather than silently
repairing them; workflow checkout must supply full history. Checkout assertion
checks clean HEAD and version without fetching again. Bundle contracts are next;
CLI/workflow wiring remains in slice 4.

Slice 2c adds pure bundle contracts. Manifest keys are `:sha`, `:version`,
and `:hashes` (fixed filename to lowercase SHA-256). Transfer validation takes
trusted job outputs and separate download observations, with canonical decimal
string artifact/run IDs. It does not read files or establish notes readiness.
Next: filesystem hashing/copying, secure POM/JAR metadata checks, and real
packaging fixtures. The download adapter must obtain identity from the workflow,
not from bundle content.

Slice 2d adds `metadata/validate!` for standalone and embedded Maven identity.
It rejects DTDs and missing or duplicate metadata entries. Bundle file handling,
hashes, copying, and integration with real build fixtures remain next.

Slice 2e adds `bundle-files/validate!` for downloaded files. It computes inventory
and hashes itself and accepts artifact/run observations separately. The caller
must keep the directory private and unchanged through publication. Bundle
creation, exact notes copying, and tools.build integration fixtures remain next.

Slice 2f adds `bundle-files/create!` for existing JAR/POM outputs and selected
notes text. It refuses an existing destination and leaves partial output on
failure. Creation has no artifact ID: upload assigns that later. Download
validation remains responsible for artifact/run identity. Real tools.build
bundle integration tests remain next; creation tests currently use real ZIP files.

Slice 2g completes real tools.build bundle coverage. The slow integration suite
is separate from pure release tests: `clojure -M:test:build-test:release:release-build-test -m scry.cli --dir test-release-build`. It reuses private build-fixture helpers
and artifact contract assertions. Include this command in CI and release checks.
Next: slice 3 publication preflight and failure policy.

Slice 3a adds pure publication outcome reports in `release.outcome`. Reports
contain only fixed operation names, not exceptions or subprocess output. Every
failed attempted write is uncertain, even if the command reports failure.
The future straight-line adapter must invoke each report only at its matching
stage; these functions do not execute or enforce sequencing. Remote lookup
classification and deployment dependency review remain next.


Slice 3b adds pure preflight policy in `release.preflight`. Only validated,
authoritative HTTP 404 observations for all three resources clear its gate.
Credential presence is checked without diagnostic values. Adapters must still
establish endpoint semantics, repository visibility, and response validity;
these are not proven by the pure tests. Wire this gate before any write together
with authorization, notes, and bundle checks. Dependency review and remote
adapters remain next.


Slice 3c adds pure `github/lookup-command` and `github/create-command` argument
vectors. They select github.com explicitly and use fixed bundle filenames.
Asset directories must be absolute POSIX paths without `#` (gh label syntax),
NUL, or line breaks. Command construction does not validate files or execute
preflight. Next: bounded execution, response classification, pinned CLI review,
and deployment dependency review before connecting publication writes.

Slice 3d adds strict parsing of a single `gh api --include` response in
`github-response/parse`. It returns HTTP status and a JSON object, without
classifying visibility or absence. The future process adapter must separate
stdout/stderr and establish process completion. Pinned CLI review and endpoint
validation remain required before using the parsed response for preflight.

Slice 3e adds `process/run!` for one bounded subprocess with an explicit environment,
closed stdin, and separate stdout/stderr. It never retries. Callers must treat
returned output as sensitive and use commands that do not spawn descendants.
This is not yet wired to gh; service-specific environment, response classification,
CLI pin review, and publication gates remain required.

Slice 3f adds pure `github-evidence` validation. Repository lookup requires
matching full name, positive numeric ID, and push permission, since read access
alone does not establish draft Release visibility. Exact-resource observations
require consistent CLI exit/status/body values. The future adapter must bind all
lookups to the same token/repository and only pass completed process results.
Pinned CLI behaviour and live endpoint absence semantics still need review;
these value checks alone do not authorize publication.


Slice 3g adds `github-lookup/observations!`, connecting bounded reads to parsed
repository/resource evidence with one explicit environment. The caller must
supply a private empty gh configuration directory and pinned CLI on PATH.
Only PATH, HOME, and TMPDIR are retained with the selected GH_TOKEN and fixed
non-interactive settings; deployment secrets and ambient CLI overrides are not
inherited. Tests cover environment policy and real child delivery, not live gh
behavior. CLI review, Clojars lookups, and publication gates remain open.

Slice 3h adds `authorization/decide`, composing actual typed request and notes
validation with a strict boolean check result. It returns selected notes and
`:publication-output` as `true` or `false` strings. The caller must provide the
fixed target's version/changelog and trusted aggregate check result. Workflow
wiring, bundle validation, preflight, and write sequencing remain separate gates.

Slice 3i adds the isolated `:deploy` alias and starts dependency inspection.
Do not wire `deps-deploy/deploy` directly yet: it prints the username, writes
and deletes a versioned POM in the working directory, and does not pass a
custom repository session to pomegranate. Its outer call has no retry loop,
but this does not establish transport-level no-retry behaviour. Finish transport
selection/retry inspection before choosing the adapter. Use a private scratch
working directory outside the bundle and a bounded child JVM with captured,
unlogged output. A process timeout alone cannot prevent transport retries.


Slice 3j adds `tag/publish!`: direct SHA-to-tag push, existing-ref check, then
an empty expected-value lease to prevent replacing a concurrently created tag.
It disables hooks and follow-tags and never creates a local tag. The caller
supplies a trusted private Git repository and explicit environment. Git network
transports can spawn descendants: resolve process-tree timeout cleanup before
hosted wiring. An identical concurrent creation can be reported by Git as up to
date; the preflight cannot provide global exclusion against external writers.
Deployment transport review remains open. No design scope change.


Slice 3k adds real tag failure tests: unavailable origin and rejecting remote
pre-receive hook. Both preserve remote refs, local HEAD, tags, and clean state;
errors omit remote diagnostics and causes. Process-tree timeout cleanup remains
required before hosted wiring. No design scope or adapter API change.


Slice 3l extends timeout cleanup to observed descendants, then the parent, with
at most ten seconds of cleanup waiting. A real shell/child test passes. This
uses a process-tree snapshot, not OS process-group containment: detached children
and new children during cleanup are outside its contract. Verify actual Git
transport lifecycle before hosted wiring; that checklist item remains open.

Slice 3m adds `github-publish/publish!`, a bounded single invocation using the
existing command and environment contracts. It discards CLI output and returns
only fixed completion evidence. All authorization, bundle, preflight and prior
write gates remain caller obligations. Pinned gh retry review and sequence
wiring remain open; local policy tests do not establish service behavior.

Slice 3n establishes the selected deployment transport: pomegranate 1.2.23
registers WagonTransporterFactory and FileTransporterFactory, not the HTTP
transporter merely present on the classpath. Its HTTPS provider creates Wagon
HttpWagon 3.5.3. Stock Wagon retries PUT on HTTP 429 through its own recursive
backoff path, independently of HttpClient's retry handler. The standard
HttpClient handler also treats PUT as idempotent. Do not wire stock deployment.
Before class initialization in the isolated child JVM, disable the request retry
count and service-unavailable strategy. Set both initial and maximum backoff to
zero so the first 429 throws before recursive PUT. Prove these controls with a
local transport fixture before accepting them. WagonTransporter sets timeout to
the maximum of session connect/request values (defaults 10 seconds/30 minutes);
use explicit bounded session settings or reviewed Wagon configuration as well
as the outer process limit. This review does not yet prove connector/checksum
behavior, redirects, or absence of every write retry.


Slice 3o adds a separate deployment transport suite:
`clojure -M:test:deploy:deploy-test -m scry.cli --dir test-deploy`.
The `:deploy-test` alias supplies the four JVM startup properties identified
above, before Wagon class initialization. A real loopback HTTP server verifies
one PUT for success, 429, 503, 500, and connection closure without a response.
These tests exercise HttpWagon directly, not the Resolver connector or complete
deployment. Connector/checksum retries, redirects, and production child-JVM
configuration remain open. Include this suite in subsequent CI wiring.


Slice 3p characterizes stock Wagon redirects. HTTP 301, 302, 303, 307, and
308 each send the artifact again with PUT to Location, even with all four
retry controls. Do not wire this configuration into publication. Select and
prove redirect rejection in addition to retry controls; connector/checksum
inspection remains open. Tests record unsafe library behavior, not acceptance
of that behavior for release publication.


Slice 3q adds `dev/deploy/closed_record/release/wagon.clj`, available only through
`:deploy`. Its disconnected factory disables redirects for all methods through
Wagon's supported HTTP configuration. Real loopback tests prove all five redirect
statuses fail after one PUT; success and retry-failure tests also use this factory.
Startup retry controls are still required. The repository system must use this
factory rather than pomegranate's stock provider. Connector/checksum review and
bounded child-JVM wiring remain open. No full deployment safety claim is made.


Slice 3r adds an isolated repository system with a per-instance Wagon provider.
It uses the configured factory for HTTP(S), without global provider registration
or pomegranate private API calls. Real Resolver deployment tests show one artifact
PUT on redirects and 429/500/503 failures. Successful artifact/checksum transfers,
checksum failure policy, and metadata writes still need inspection and tests.
No production deployment command is connected; startup retry controls and bounded
child-JVM execution remain required. Design scope is unchanged.


Slice 3s confirms BasicRepositoryConnector 1.9.4 catches checksum upload
exceptions and returns success. Real Resolver tests show metadata writes continue
after failed artifact checksums. Do not connect this behavior to publication.
Next: enforce fail-fast checksum failure handling without retries, then test
metadata merge/failure paths. The local success test covers a new repository
(404 metadata read), not existing repository metadata or Clojars behavior.


Slice 3t adds real metadata failure tests. Failed metadata reads stop after
artifact/checksum uploads; failed metadata PUTs stop before metadata checksums.
Existing metadata merge tests and fail-fast checksum handling remain open.
No deployment adapter is connected. Design scope is unchanged.


Slice 3u verifies existing metadata merge through real Resolver: earlier versions
survive, the release version advances, and uploaded metadata checksums match
the merged bytes. Connector review and fail-fast checksum handling remain open.
No production deployment adapter is connected; design scope is unchanged.


Slice 3v covers isolated metadata checksum failures. Either SHA-1 or MD5 can
return 429/500/503 while Resolver still reports success. A fail-fast solution
must cover artifact and metadata checksums, not only the artifact transfer.
Bytecode review confirms each checksum upload catches Exception independently;
there is no failure-policy branch in that method. Production wiring remains
open. Design scope is unchanged.


Slice 3w adds `repository/session` with one BasicRepositoryConnector transfer
thread and 30-second connect/request limits. Pinned connector bytecode shows a
five-thread default and no strict checksum-upload failure switch. All real
Resolver tests now use this session. Serial transfers are a prerequisite for
ordered failure handling, not an implementation of it. The future adapter must
add its private local repository manager, preserve these settings, and apply
an outer process deadline. Strict checksum handling remains open.

Slice 3x adds lost-response coverage through real Resolver. A metadata PUT with
no response stops deployment before metadata checksums and is not repeated.
A lost metadata checksum response is still ignored, with no repeated PUT.
Strict checksum failure handling remains the next deployment requirement;
these tests do not authorize the current connector for publication.


Slice 3y covers lost artifact checksum responses through real Resolver. Either
SHA-1 or MD5 response can be lost while deployment reports success and writes
metadata. All six PUT paths occur once. Strict checksum handling must stop later
writes after uncertain responses as well as explicit HTTP failures. This remains
an open production requirement; no publication adapter is authorized.

Slice 3z adds a deployment-wide upload failure guard and an opt-in guarded
Wagon file-PUT factory. Real HTTP tests prove that explicit failures, redirects,
and lost responses prevent subsequent file uploads, including on another Wagon.
The guard retains a secret-free failure for a mandatory post-Resolver check.
It is not connected to the repository system: first cover streaming PUT entry
points used by WagonTransporter, then prove checksum fail-fast behavior through
real Resolver. Keep serial transfers and share one guard across all providers.

Slice 3aa extends the guarded Wagon to both public streaming PUT overloads.
Real loopback tests cover exact bytes and retained failures across file/stream
uploads and separate Wagons. Next connect one guard to the repository provider
and require its check after Resolver returns, including ignored checksum errors.
The factory remains opt-in; no publication adapter is connected.


Slice 3ab connects optional shared failure state through `repository/create`
and adds `repository/deploy!` with a mandatory retained-failure check after
Resolver returns. Real loopback tests prove artifact and metadata checksum
HTTP failures and lost responses stop subsequent PUTs and fail deployment.
The no-argument factory remains for dependency characterization only. The future
bounded deployment entry point must own the matching guard/system/serial session;
these low-level functions do not enforce that pairing or authorize publication.
Design scope and acceptance contracts remain unchanged.


Slice 3ac adds `deployment/deploy!`, which owns the shared guard, matching
repository system, serial session, and local repository manager for one request.
Real loopback checksum tests now use this operation. It returns fixed completion
evidence and discards exception details. The caller supplies a private local
repository directory. Next add the bounded child-JVM entry point and startup
retry controls; this in-process operation alone is not a publication adapter.
Design scope and acceptance contracts remain unchanged.


The owned deployment operation now rejects missing or unsafe retry properties
before transport creation. This is a defensive check, not proof of class-loading
order. The child-JVM launcher must still set all four properties at startup;
do not set them after loading Wagon. Design scope is unchanged.


Slice 3ae adds `deployment-request/create` for the two verified bundle paths,
fixed release coordinate, fixed Clojars HTTPS destination, and in-memory
authentication. It performs no file or network I/O. The caller must validate
and protect the bundle; request construction is not publication authorization.
The bounded child JVM must connect this request to the owned deployment operation.


Slice 3af adds `deployment-child` connecting environment credentials and existing
file requests to the owned operation. Invalid input exits with fixed diagnostics.
The parent launcher remains required: use a direct Java process, startup retry
properties, an explicit credential environment, captured unlogged output, and an
outer deadline. Bundle validation and publication gates remain caller obligations.


Slice 3ag adds pure `deployment-launch` command and environment contracts. The
command requires trusted absolute Java/classpath paths and sets retry controls
before class loading. The environment contains only Clojars credentials, excluding
ambient JVM option injection. Next connect these values to bounded execution with
captured, discarded output and a private local repository. No publication gate
or process execution is implemented by this contract. Design scope is unchanged.


Slice 3ah adds `deployment-publish/publish!`: direct child JVM, five-minute
deadline, startup controls, credential-only environment, and discarded output.
It requires both release and deploy paths. The deployment test alias includes
the release source path for the shared process runner. Caller-owned private
empty repository setup, successful child transport coverage, and publication
gates remain open. Design contracts are unchanged.


Slice 3ai makes the deployment launcher own an empty owner-only POSIX local
repository under a trusted scratch parent. The fifth publish argument now names
that parent, not the repository itself. Cleanup runs on both exits and does not
follow symbolic links. Cleanup failure is an uncertain deployment failure, not
permission to retry. Successful child transport coverage and publication gates
remain open. Design contracts are unchanged.


Slice 3aj adds pure Clojars exact JAR/POM URL and combined evidence policy.
Either existing file is a conflict; only two completed 404 responses plus
independent visibility evidence can clear it. The bounded JDK HTTP adapter
must reject redirects and establish repository visibility and 404 semantics
before using this policy. No network lookup or publication gate is wired.
Design contracts remain unchanged.


Slice 3ak adds Clojars HEAD transport with 30-second connect/request limits,
no credentials or ambient proxy, and no redirects. Real loopback tests cover
statuses and withheld response headers. It returns raw responses only: public
HEAD endpoint semantics and independent visibility evidence remain required
before preflight wiring. JDK read retries are allowed; no writes occur.
The client lifecycle uses Java 21, as required by the release design.

Slice 3al adds an independent public visibility probe using the pinned scry
0.1.61 POM and a strict completed-200 policy. Read-only curl probes observed
200 for that POM and 404 for closed-record 0.1.0's POM. These observations
are not a guarantee of CDN absence semantics. Keep preflight wiring open
until endpoint/cache behavior is reviewed; never use candidate 404s as their
own visibility evidence. The probe is public and carries no credentials.

Clojars HEAD reads now send `Cache-Control: no-cache` for candidate files and
the independent visibility probe. This requests intermediary revalidation rather
than accepting a fresh cached negative response. It does not bypass caches or
prove server compliance; endpoint semantics review remains required before
preflight wiring. Real local HTTP tests verify the request header.


Real local HTTP coverage now verifies that interrupted Clojars reads throw fixed
diagnostics, preserve interrupt status, and terminate the worker. No response
is returned as absence evidence. Endpoint semantics review remains open.


Real local Git push timeout coverage now verifies termination of a stalled
receive hook and preserved remote refs/local state. This tests the shared runner
with actual Git descendants, not hosted HTTPS transport. Keep hosted lifecycle
review open; snapshot cleanup is not process-group containment.


The shared process runner now restores thread interruption after cleanup when
a subprocess wait is cancelled. A real pre-interrupted worker test covers the
wait/cleanup path and fixed diagnostics. This does not extend descendant
containment or establish hosted transport behavior.


The process runner now rejects an already-interrupted caller before temporary
file creation or process launch. The pre-interrupted test checks an unchanged
local file, not wait cleanup. Coordinated in-flight cancellation coverage now uses a real shell and local
TCP connection before interruption. It verifies parent and observed-child
termination, preserved interrupt status, and fixed diagnostics. The local test
requires `/usr/bin/nc`; it does not contact an external service. This guard does not
eliminate cancellation races after the check. Design contracts are unchanged.

## CLI review boundary

The v2.102.0 candidate is not approved for publication. The
[CLI review](github-cli-review.md) identifies internal upload retries and draft
deletion; v2.65.0 and v2.40.0 have the same paths. Do not wire the current
Release adapter. A maintainer must approve a command-contract change or the
CLI failure behavior before this part of the plan can proceed.
