# Implementation steps

Closed without completion on 2026-10-23. The user requested the simpler scry
build/deploy approach. This document records the abandoned scope, not current
requirements. Do not continue its checklist or enforce its transport policies.
See [016 Simple release workflow](../../closed/016-simple-release-workflow/design.md).

All checks are local unless stated otherwise. Do not push, configure secrets,
run benchmarks, or publish for acceptance. Record results and limits in
`implementation.md`; commit each completed slice.

## Slice 1 — Notes and local preparation

- [x] Add the `:release` and `:release-test` aliases; verify both aliases load without changing base runtime dependencies.
- [x] Add the `prepare` command entry point specified in `plan.md`.
- [x] Add `CHANGELOG.md` with one Unreleased section and factual current-feature bullets; verify that it has no invented version entries.
- [x] Implement pure notes parsing, stamping, extraction, and readiness selection using the architecture grammar, valid UTC dates, and Git-count versions.
- [x] Add pure notes tests for preserved preamble/older bodies, exact extraction, duplicate or unsupported headings, invalid dates, empty bullets, and missing versus invalid matching sections; verify no invalid-section fallback.
- [x] Implement `prepare` with clean-master/full-history checks, HEAD recheck, changelog-only staging/commit, count/message/path/clean-state postconditions, and partial-state diagnostics without recovery writes.
- [x] Test preparation in isolated temporary Git repositories: success, dirty index/tree/untracked files, wrong branch, shallow history, duplicate proposed version, invalid notes, and a real failing hook; assert unchanged remote refs and no local tags.
- [x] Run the focused release suite and format/lint the new paths; record structured results and commit slice 1.

## Slice 2 — Request, target, and bundle contracts

- [x] Implement typed JSON request validation for manual master dispatch, canonical publication authorization, and blank-or-full-SHA target input; pass values as data, not generated code.
- [x] Test true, false, missing, null, strings, malformed JSON, wrong events/branches, and invalid target syntax; assert only explicit boolean false authorizes publication.
- [x] Implement full-history remote-master target resolution, ancestor checks, and exact verification-checkout assertion; return one fixed SHA and computed version.
- [x] Test targets with a real bare remote and clones: blank tip, ancestor, unknown/non-commit/side-branch SHA, shallow checkout, and master advancement after resolution.
- [x] Add pure strict manifest parsing and transfer validation for trusted SHA/version, same-run artifact identity, fixed inventory, and observed hashes; test invalid schemas and transfer mismatches.
- [x] Add secure standalone/embedded POM and JAR Maven properties identity validation, with real archive tests for mismatches, missing metadata, duplicate POM fields, and forbidden DTDs.
- [x] Validate downloaded bundle files with exact inventory, regular-file checks, SHA-256, independent artifact/run identity, and Maven metadata; test real archives, changed bytes, missing/extra files, directories, and symbolic links.
- [x] Implement bundle creation and validation with fixed payload names, exact notes bytes, minimal manifest, SHA-256 hashes, coordinate/version checks, and expected run artifact ID/SHA/version inputs. Creation precedes upload; artifact/run identity is checked on download.
- [x] Add real fixture packaging tests for valid bundles and wrong coordinate/version/SHA, altered bytes, missing/extra files, and mismatched artifact identity; confirm release sources/dependencies stay out of JAR/POM.
- [x] Run focused release and build suites plus format/lint; record results and commit slice 2.

## Slice 3 — Publication and failure policy

- [x] Compose typed request, prepared notes, and strict boolean check success into a canonical publication output; test preview, missing/invalid evidence, and invalid matching notes without mocks.
- [ ] Connect the authorization gate to trusted fixed-target verification results and workflow outputs; do not accept check results from bundle metadata.

- [x] Add pure fixed-sequence outcome reports for preflight failure, each uncertain write, and success; test ordered completed/pending operations without mocks.
- [ ] Wire outcome reports into the straight-line adapter and verify that no later write runs after failure. Pure reports alone do not enforce execution order.

- [x] Add a bounded single-process runner with explicit environment, closed stdin, separate output streams, no retries, and redacted failures; test real local exit, timeout, and launch failure. Service wiring remains below.
- [x] Connect bounded GitHub reads to response/evidence validation with one explicit token/repository and an isolated environment; test environment policy and real child-process delivery without mocks. Live lookup and CLI review remain open.
- [x] Add the pinned, deployment-only `:deploy` alias; verify loading and isolation from the release-only classpath. Inspect deps-deploy entry-point side effects and session forwarding.
- [x] Identify the transport actually registered by pomegranate and inspect Wagon PUT retry and timeout paths; stock HTTP 429 handling retries independently of HttpClient.
- [x] Prove disabled Wagon request/status retries with a real loopback transport fixture for 429, 503, 500, and a lost response; verify successful payload bytes.
- [x] Characterize stock Wagon redirects with a real loopback server: all five redirect statuses repeat PUT despite retry controls.
- [x] Disable deployment redirects in a deployment-only Wagon factory and prove rejection with the real transport before wiring writes.
- [x] Connect the configured Wagon factory to the deployment repository system; do not use the stock provider. Real Resolver tests cover failed artifact PUTs and redirects.
- [x] Inspect connector checksum upload behavior and characterize successful artifact/checksum writes and ignored checksum failures with real Resolver tests.
- [x] Characterize isolated metadata SHA-1 and MD5 upload failures with real Resolver; confirm false success and no repeated PUTs.
- [x] Add an opt-in deployment-wide file-upload failure guard; prove later file uploads are blocked after HTTP failures or lost responses with real transport tests.
- [x] Cover both public Wagon streaming upload entry points with the shared guard; test success, HTTP failures, redirects, lost responses, and blocked later file/stream uploads.
- [x] Share the guard through the repository provider and check retained failure after Resolver returns; test artifact and metadata checksum failures end to end.
- [x] Enforce fail-fast checksum upload failures in the guarded repository path before deployment wiring; retained failures prevent later PUTs and reject false Resolver success.
- [x] Add an owned deployment operation that pairs one guard, guarded repository system, and serial session; exercise checksum failures through this operation.
- [x] Reject missing or unsafe retry startup properties before the owned deployment operation creates its transport; test exact values and secret-safe errors. Launch-time enforcement remains required.
- [x] Add an existing-file deployment request constructor with fixed Clojars destination, JAR/POM coordinates, and in-memory authentication; test real Resolver values without network access.
- [x] Add the deployment child entry point with environment-only credentials and fixed failure diagnostics; test invalid inputs and real JVM failure exit without publication.
- [x] Add a pure direct-Java launch contract with startup retry controls, absolute classpath entries, and a credential-only environment; test argument boundaries and rejected inputs.
- [x] Connect the owned deployment operation to a bounded child-JVM entry point with startup retry controls; do not expose the unguarded characterization path to publication.
- [x] Add private local repository lifecycle management; test owner-only permissions, empty creation, cleanup on both exits, and symbolic-link safety with real files.
- [ ] Add successful child transport coverage before publication wiring; caller gates remain required.
- [x] Test metadata read failures (401/403/500/503) and write failures (429/500/503) with real Resolver; assert failure and no later metadata writes/checksums.
- [x] Test existing metadata merge through real Resolver; verify retained versions, release version, checksum bytes, and no repeated writes.
- [x] Test lost metadata and metadata-checksum responses through real Resolver; verify unique PUT paths, metadata failure stops, and ignored checksum failures remain explicit.
- [x] Test lost artifact SHA-1 and MD5 responses through real Resolver; verify no repeated writes and characterize continued metadata publication despite uncertain checksums.
- [x] Add a deployment session with one connector transfer thread and explicit connect/request timeouts; exercise it through real Resolver tests.
- [ ] Complete connector review and fail-fast checksum handling before connecting deployment. Serial transfers alone do not stop ignored checksum failures.
- [ ] Finish resolved transport retry/timeout inspection and implement bounded deployment without automatic write retries. Account for username logging and temporary POM writes; the stock entry point does not forward a custom repository session.
- [x] Add pure fixed-coordinate Clojars JAR/POM URL and combined response policy; require both files absent and independent visibility evidence, and reject partial publication. Test without network access or mocks.
- [x] Add bounded credential-free Clojars HEAD transport with redirects disabled; test real local statuses, redirect rejection, and request timeout. Raw responses do not establish absence.
- [x] Add an independent fixed published-POM visibility probe and strict completed-200 policy; verify pure policy and shared HTTP transport tests. Record read-only public endpoint observations without treating them as absence proof.
- [x] Require cache revalidation on Clojars HEAD reads; verify the header on real local HTTP requests without credentials or redirect following. This does not prove endpoint compliance.
- [x] Verify interrupted Clojars reads with a real local HTTP server; require prompt termination, preserved interrupt status, and secret-safe diagnostics.
- [ ] Establish public Clojars HEAD endpoint semantics and independent visibility evidence before connecting lookup responses to preflight.
- [ ] Implement secret-safe credential validation and bounded exact-resource lookups: `gh api` for GitHub tags/Releases, JDK HTTP for Clojars versions. Use `GH_TOKEN`, explicit repository selection, and non-interactive argument-vector commands; do not add a custom GitHub HTTP client. Document endpoints and authoritative absence responses; a nonzero CLI exit alone is not absence.
- [x] Test pure lookup policy for present, absent, authentication failure, timeout, malformed response, rate limit, and server error; assert every uncertain/conflict result rejects the preflight gate.
- [x] Add pure GitHub evidence validation for repository identity/push access, exact resource identity, and completed HTTP-error responses; test with parsed values and preflight policy. Live endpoint and pinned CLI review remain required.
- [ ] Wire the preflight gate before the first write; adapters must establish validated HTTP status, response validity, and authoritative absence (including repository visibility), not infer absence from CLI exit codes.
- [x] Add the Git tag adapter and real bare-remote tests for exact SHA, unchanged master/local tags, invalid objects, and existing identical/different tags.
- [x] Test remote lookup failure and receive-hook rejection against real local repositories; check unchanged refs and secret-safe diagnostics.
- [x] Add bounded timeout cleanup for observed child processes and test a real waiting shell/child without mocks.
- [x] Test timeout cleanup through a real local Git push with a stalled receive hook; verify hook termination and unchanged remote refs/local state.
- [x] Preserve thread cancellation after an interrupted subprocess wait; verify real process cleanup and redacted failure without mocks.
- [x] Reject already-cancelled subprocess requests before launch; verify an unchanged local output file and preserved interrupt status.
- [x] Add coordinated in-flight subprocess cancellation coverage; a real loopback connection establishes launch before interruption, and checks parent/observed-child termination plus preserved cancellation.
- [ ] Verify Git transport child lifecycle before hosted wiring; snapshot cleanup does not contain detached children or children spawned during cleanup.
- [x] Add bounded GitHub Release creation adapter with fixed completion/failure results; test exit policy and invalid inputs without remote writes. Pinned CLI review and caller gates remain open.
- [ ] Implement create-only Git tag publication, existing-file Clojars deployment, and `gh release create --verify-tag --notes-file` with explicit tag/repository and verified asset paths; verify none builds, updates master, replaces artifacts, generates notes, or falls back to tag creation.
- [ ] Implement straight-line publication with bundle/prepared-notes/authorization gates and completed/uncertain operation reporting; stop on the first failure without rollback or resume.
- [x] Parse one `gh api --include` response as HTTP status and a JSON object; test malformed headers, extra responses/data, HTTP errors, and secret-safe diagnostics without mocks. This does not establish resource visibility or absence.
- [x] Test pure `gh` argument construction for exact lookups and create-only Release assets, including invalid identities and unsafe asset paths. No commands execute in these tests.
- [ ] Test lookup classification adapters and sequence outcomes for each failure point, including process timeout and partial asset uploads; test create-only tag conflicts against a real temporary bare remote.
- [ ] Review adapter code and pinned `gh` behaviour for exact file paths, bounded processes/requests, secret redaction, and no write retries; record untested live-service behaviour, run release tests, and commit slice 3.

## Slice 4 — Workflow and CI integration

- [ ] Add manual-only release dispatch with default-true boolean dry run, optional full-SHA target, master guard, two jobs, shared non-cancelling concurrency, and finite job timeouts.
- [ ] Configure CI-consistent pinned tools/actions, install and verify a pinned `gh` version in the publish job, full-history target checkout, fixed workflow-revision control checkout, and non-persistent credentials; grant write permission only to publication.
- [ ] Wire quality checks and sequential runtime suites with immediate per-suite hidden failure uploads and an aggregate failure gate; run build/release checks before the one publication build.
- [ ] Wire bundle upload and identity/gate outputs, exact same-run artifact download, and revalidation; require successful verification plus the canonical explicit-false gate before secret-bearing publication.
- [ ] Add summaries for SHA, version, tag, notes source, readiness, check outcomes, and partial publication; ensure Unreleased previews say preparation is required.
- [ ] Include the separate real packaging suite (`:test:build-test:release:release-build-test`, `--dir test-release-build`) in release verification and ordinary CI.
- [ ] Extend ordinary CI quality paths and add focused release checks with failure-evidence uploads; retain runtime matrix and separate build coverage.
- [ ] Run actionlint and inspect workflow gates, artifact identity, permissions, secret placement, failure collection, and no-build deployment against architecture contracts; record results and commit slice 4.

## Slice 5 — Documentation and acceptance

- [ ] Document exact preparation, inspection, separate push, dry-run dispatch, explicit publication, `gh` version/authentication requirements, and secret setup commands in README; include local/remote partial-failure recovery and artifact retention limits.
- [ ] Update the release architecture artifact map and core embodiment tooling boundaries to match implemented paths; distinguish implemented behaviour from unverified hosted operation.
- [ ] Run Java 21 format/lint, all three runtime commands, focused build/release suites, and actionlint; inspect exit codes and `.scry-results/` data rather than scraping test output.
- [ ] Build once locally and validate a non-publishing bundle/notes preview; confirm runtime dependencies, Git-count versioning, and packaged content remain unchanged.
- [ ] Walk through all six design acceptance criteria against tests, workflow inspection, and documentation; append commands/results and explicit hosted-verification limits to `implementation.md`, then commit slice 5.

## CLI review follow-up

- [x] Review candidate gh v2.102.0 asset retry and draft cleanup paths; compare v2.65.0 and v2.40.0. Record the contract conflict in `github-cli-review.md`.
- [ ] Obtain a maintainer decision on separate `gh api` writes versus allowing internal retries and draft deletion. Do not connect the current Release adapter.
- [ ] After the decision, revise the adapter plan, review the selected transport, and add failure coverage before publication wiring.
