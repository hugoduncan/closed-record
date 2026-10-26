# Implementation notes

## 2026-10-23 — Architectural review

- No architectural review feedback. Scope unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture context:
  `AGENTS.md`, `ramora/defclosedrecord/{principles,embodiment}.md`, and the three
  documents linked from `ramora/release-workflow.md`. The release architecture
  is intended, not evidence of implementation or hosted publication.

## 2026-10-23 — Ambiguity review

- No ambiguity review feedback. Scope unchanged.

## 2026-10-23 — Inconsistency review

- No inconsistency review feedback. Scope unchanged.
- Reference checks used `build.clj` for the coordinate, Git-count version, and
  POM path; `deps.edn` for alias composition; and `.github/workflows/ci.yml`
  for tool pins, test commands, and failure uploads. These were source checks,
  not test runs.

## 2026-10-23 — Planning and review

- Created `plan.md` and `steps.md` from the stable design. Selected JVM Clojure,
  `dev/release` and `test-release`, a local `prepare` command, and five slices.
  No implementation or remote operation was performed.
- Plan ambiguity review treated `steps.md` as read-only. Clarified in the plan
  that the publication output requires both typed explicit-false authorization
  and prepared notes, and that the job requires the exact output string `true`.
  No blocking ambiguity remains. All six acceptance criteria map to slices.
- Design consistency review compared the task with all three release architecture
  documents, `build.clj`, `deps.edn`, CI, README, core embodiment, and the named
  scry reference files. No internal contradiction or reference conflict found.
  Scry's tag triggers, permissive notes fallback, and rebuild-on-deploy are
  examples to exclude, not requirements to copy.
- Command spelling, language, paths, and dependency pins were explicit planning
  choices, not unresolved design questions. Remote absence semantics and
  deployment-library retry behaviour remain implementation checks in slice 3;
  credential authorization and hosted behaviour remain evidence limits.
- Validation: checklist order and coverage review, local link checks, and
  `git diff --check`. No code tests or actionlint were run: this pass changes
  planning documents only.

## 2026-10-23 — Shared plan-review, ambiguity turn

- No new actionable ambiguity feedback; existing planning clarifications suffice.

## 2026-10-23 — Shared plan-review, inconsistency turn

- No new actionable inconsistency feedback across the task files.


## 2026-10-23 — GitHub CLI requirement

- Updated task design, plan, checklist, and release architecture to require
  `gh` for GitHub API and Release operations, not a custom HTTP client.
  Git ref operations remain in Git; Clojars tooling is unchanged.
- Required a pinned CLI, `GH_TOKEN`, explicit repository selection, bounded
  non-interactive commands, authoritative absence checks, and existing-tag
  Release creation. Retained no-overwrite and no-write-retry constraints.
- Added command-construction checks and CLI behaviour review to acceptance
  work. Preserved untracked `dev/release/` and `test-deploy/` files.
- Validation: local Markdown links, document word limits, and
  `git diff --check` passed. No code tests, hosted checks, or publication:
  this change updates task and architecture documents only.

## 2026-10-23 — Slice 1a: pure notes policy

- Added notes parsing, stamping, count versions, and readiness selection under
  `closed-record.release.notes`. Extraction preserves the body after the heading
  newline, including boundary blank lines. Stamping alone normalizes boundaries.
- Canonical decimal version counts prevent alternate spellings of a release.
  Invalid matching sections fail without Unreleased fallback. Calendar dates
  are checked with `LocalDate`; the future prepare adapter must supply UTC today.
- Added release aliases and current-feature changelog notes. Runtime dependencies
  are unchanged. No command entry point or Git preparation adapter exists yet;
  these are the next slice, with real temporary Git fixtures and failing hooks.
- Java 21: focused scry CLI exited 0 (4 tests, 30 assertions); `.scry-results/`
  was empty. cljfmt and clj-kondo passed on both new paths (zero warnings).
  No remote writes or hosted checks ran. This commit is a verified part of slice 1,
  not completion of local preparation or the release workflow.

## 2026-10-23 — Slice 1b: local preparation

- Added the prepare entry point and Git adapter. Validation precedes writes;
  postconditions check exact committed notes, parent, count, message, paths,
  and clean state. Errors report a phase and retain partial state for inspection.
- Real isolated repositories and a bare remote cover success, dirty tree/index,
  untracked files, wrong branch, shallow history, empty/malformed notes,
  duplicate proposed version, and a failing pre-commit hook. Tests compare
  remote refs and local tags; no external remote is contacted.
- Java 21: `clojure -M:test:release:release-test -m scry.cli --dir test-release`
  exited 0: 7 tests, 94 assertions; `.scry-results/` contains no failure files.
  `cljfmt check dev/release test-release` and clj-kondo on those paths passed
  with zero warnings. No hosted verification or publication ran.
- Next: slice 2 request/target/bundle contracts. The local Git adapter is
  preparation-specific; bounded remote subprocess execution remains slice 3
  work. Runtime dependencies and build behaviour are unchanged.

## 2026-10-23 — Slice 2a: typed request validation

- Added pure request parsing without command execution. JSON is data; trailing
  non-whitespace data is rejected. Missing dry-run permits preview only; null
  and other non-boolean values fail. SHA normalization does not resolve Git refs.
- Authorization stays separate from notes readiness and verification success.
  The future workflow must combine these before emitting its publication gate.
- Java 21 focused release suite exited 0: 10 tests, 131 assertions; no failure
  files in `.scry-results/`. cljfmt and clj-kondo passed on release source/test
  paths with zero warnings. Initial parser API and test hash-length errors were
  corrected before the passing run. No dependencies changed or remote writes ran.
- Next: real Git target resolution and bundle validation. Build checks were not
  rerun because this slice changes only request policy, not packaging. Hosted
  workflow behaviour remains unverified.


## 2026-10-23 — Slice 2b: fixed target

- Added `target/resolve!` and `target/assert-checkout!`. Resolution fetches only
  origin master without tags, checks commit object type and ancestry, and fixes
  SHA/version. Checkout assertion requires clean full-history HEAD and matching
  count; it does not refetch. Design scope is unchanged.
- Each Git process has a 120-second limit and non-interactive authentication.
  Output uses temporary files to avoid pipe deadlocks; failure diagnostics omit
  remote output to avoid exposing credential-bearing URLs. Replacement objects
  are disabled, and repository-location environment overrides are removed.
- Real bare-remote/clone tests cover tip and ancestor selection, non-commit and
  side-history objects, unknown/invalid SHA, shallow history, remote advancement,
  dirty checkout, and identity mismatch. No mocks or external service writes.
- Java 21 release suite exited 0: 12 tests, 151 assertions; no failure files in
  `.scry-results/`. cljfmt and clj-kondo passed on both release paths with zero
  warnings. A test delimiter error was repaired before the passing run.
- Next: bundle contracts. Build tests were not rerun because packaging did not
  change. Workflow wiring, hosted execution, and publication remain unverified.

## 2026-10-23 — Slice 2c: pure bundle contracts

- Added strict single-form EDN manifest validation and independent transfer
  validation. Artifact/run IDs are canonical positive decimal strings from
  workflow/download context, never manifest fields. Extra manifest keys fail.
- These functions validate supplied observations only. The next slice must
  compute file hashes, reject unsafe filesystem entries, check POM/JAR metadata,
  and create/test real bundles. No publication readiness is inferred here.
- Design scope is unchanged. Java 21 focused release CLI exited 0: 14 tests,
  183 assertions; no failure files in `.scry-results/`. cljfmt, clj-kondo
  (zero warnings), and `git diff --check` passed. Build tests were not rerun:
  this slice changes only pure transfer policy, not packaging. No remote writes
  or hosted checks ran. Dependencies are unchanged.

## 2026-10-23 — Slice 2d: archive metadata

- Added `metadata/validate!`: checks standalone POM, embedded POM, and Maven
  properties against the constant coordinate and supplied version. XML DTDs,
  external access, and duplicate direct identity fields are rejected. No new
  dependencies. Task design and acceptance scope are unchanged.
- Tests use real temporary ZIP/POM files, without mocks. Initial Java 21 DOM
  reflection failures were fixed with public DOM interface type hints.
- Java 21 release CLI passed (16 tests, 192 assertions); build CLI passed
  (3 tests, 71 assertions). Both left no scry failure files. cljfmt and
  clj-kondo passed with zero warnings. Expected DTD rejection emits parser
  diagnostics during tests. No hosted checks or remote writes ran.
- Next: bundle filesystem inventory, safe file handling, SHA-256, exact notes
  copying, and creation/transfer integration. Add a tools.build fixture check;
  these tests construct archives directly and do not yet prove build integration.

## 2026-10-23 — Slice 2e: downloaded bundle files

- Added `bundle-files/validate!`. Filesystem observations override any supplied
  inventory/hash claims; artifact/run identity remains independent download data.
  Exact inventory and regular-file checks precede reading. Symbolic links and
  directories fail. Hashing streams payload bytes; notes are not rewritten.
- The directory must remain private and unchanged until publication finishes.
  Validation cannot prevent concurrent replacement. Added this caller constraint
  to the design. No dependency changes or publication operations.
- Real temporary archives/files cover valid transfer, target and artifact identity
  mismatches, changed bytes, missing/extra files, directories, links, and invalid
  Maven metadata even with updated hashes. No mocks.
- Java 21 release CLI exited 0 (18 tests, 205 assertions); no scry failure files.
  cljfmt and clj-kondo passed with zero warnings. Build tests were not rerun:
  packaging is unchanged. Hosted operations remain unverified.
- Next: bundle creation and real tools.build fixtures. This pass implements the
  download-validation part of slice 2, not the complete bundle lifecycle.

## 2026-10-23 — Slice 2f: bundle creation

- Added `bundle-files/create!`: copies existing build outputs, writes selected
  notes as UTF-8 without newline changes, and returns/writes the minimal manifest.
  It checks source file types, target identity, and Maven metadata before output;
  copied metadata is checked again. Existing destinations are refused. Partial
  failures retain output for inspection. No build or readiness decision occurs.
- Artifact identity is intentionally absent at creation: upload assigns it later.
  Caller supplies validated notes and keeps sources/destination private. Scope
  and dependencies are unchanged; real tools.build bundle fixtures remain next.
- Real filesystem/archive tests cover transfer round-trip, exact notes/hashes,
  no-overwrite, invalid identity/version, missing files, and invalid input types.
- Java 21 release CLI passed: 20 tests, 220 assertions. Build CLI passed:
  3 tests, 71 assertions. Both exited 0 with no scry failure files. cljfmt and
  clj-kondo passed (zero warnings). No hosted checks or remote writes ran.

## 2026-10-23 — Slice 2g: real build bundle integration

- Added a separate slow integration test alias/path. A real one-commit Git
  fixture builds once, then copies and validates the bundle without rebuilding.
  Tests cover exact artifact/Unicode CRLF notes bytes, wrong coordinate/version/
  SHA, altered bytes, missing/extra files, and artifact identity mismatch.
- Reused private build test helpers and artifact contract checks. Real release
  sources are copied into the fixture; JAR inventory and POM base-dependency
  equality prove tooling exclusion. No runtime dependencies or design changes.
- Java 21: integration suite passed (1 test, 53 assertions), release suite passed
  (20 tests, 220 assertions), build suite passed (3 tests, 71 assertions). All CLI
  exits were 0; no failure files remained. cljfmt and clj-kondo passed with zero
  warnings. No hosted checks or publication ran.
- Next slice starts publication policy. Workflow integration must run the separate
  slow suite as well as the release suite; its command is in plan.md.

## 2026-10-23 — Slice 3a: publication outcome reports

- Added pure outcome reports for preflight failure, each failed write, and full
  success. Failed writes always require inspection: command failure cannot prove
  absence of remote effects. Reports exclude exception/output data to avoid
  credential leaks. No dependencies or design scope changed.
- These are reporting contracts, not a sequence executor. The next adapter must
  stop at the first failure and call the report for that stage only. Tests check
  values without mocks; they do not establish actual remote execution order.
- Java 21 release CLI exited 0 (22 tests, 230 assertions); no scry failure files.
  cljfmt and clj-kondo passed with zero warnings. Packaging did not change, so
  build tests were not rerun. No network writes or hosted verification ran.


## 2026-10-23 — Slice 3b: pure preflight policy

- Added lookup classification, credential-presence checks, and an all-resources
  preflight gate. Only boolean evidence flags and numeric HTTP statuses qualify;
  missing observations, conflicts, and uncertain responses fail closed. Errors
  contain no credentials, response bodies, or causes.
- Adapters must establish authoritative absence independently, including resource
  visibility. Pure observations do not prove live endpoint semantics or prevent
  races. No remote adapter, execution gate wiring, or dependency change in this
  slice. Design clarifies the existing fail-closed requirement.
- Java 21 release CLI exited 0: 24 tests, 338 assertions; no scry failure files.
  cljfmt and clj-kondo passed with zero warnings. No hosted checks or remote writes
  ran. Build tests were not rerun because packaging is unchanged.
- Next: review deployment dependencies and implement bounded service adapters;
  connect preflight and outcome policy to the straight-line publication path.


## 2026-10-23 — Slice 3c: GitHub command values

- Added pure argument vectors for repository visibility, exact tag/Release
  lookups, and Release creation. API GET includes headers for future status
  classification. Explicit github.com selection avoids ambient host selection.
  Creation requires an existing tag and fixed bundle assets; paths with gh
  label syntax are rejected. No shell, execution, or dependency changes.
- Tests assert command contracts and invalid inputs without mocks. Design scope
  is unchanged. These tests do not prove CLI behaviour, HTTP absence semantics,
  timeouts, or publication sequencing; adapters and pinned CLI review remain.
- Java 21: focused release CLI exited 0 (27 tests, 398 assertions), with no
  `.scry-results/` failure files. `cljfmt check dev/release test-release` and
  `clj-kondo --lint dev/release test-release` passed with zero warnings.
  No remote writes or hosted checks ran; packaging was unchanged.

## 2026-10-23 — Slice 3d: GitHub response parsing

- Added a pure single-response parser for `gh api --include` stdout. HTTP error
  responses remain data; they do not clear preflight. Malformed headers, extra
  responses, non-object JSON, and trailing data fail with fixed diagnostics and
  no input or parser cause. No dependencies or design scope changed.
- Next adapter must separate stderr, bound execution, check completion, validate
  resource identity/visibility, and review pinned CLI output and retry behaviour.
  These value tests do not establish live-service semantics or CLI compatibility.
- Java 21 release CLI exited 0: 29 tests, 495 assertions; no scry failure files.
  cljfmt and clj-kondo passed with zero warnings. Packaging is unchanged; build
  tests were not rerun. No hosted checks, external writes, or publication ran.

## 2026-10-23 — Slice 3e: bounded process execution

- Added a single-process runner with an explicit (not inherited) environment,
  closed stdin, file-backed separate streams, timeout, forced termination, and
  a bounded termination wait. No retries. Failures omit command, output, and cause.
- Callers must not log returned streams or use commands that spawn descendants.
  gh integration and service-specific non-interactive environment remain next;
  this runner alone establishes no remote absence or publication authorization.
- Real local shell processes test exit status, streams, stdin, timeout, and launch
  failure without mocks. Java 21 release CLI exited 0 (31 tests, 505 assertions);
  no scry failure files. cljfmt and clj-kondo passed with zero warnings.
  Design scope and dependencies are unchanged. No hosted checks or remote writes.

## 2026-10-23 — Slice 3f: GitHub evidence values

- Added pure repository and exact-resource evidence validation. Repository push
  permission is required to avoid treating hidden draft Releases as absent.
  Invalid identity/access fails with fixed diagnostics; inconsistent exit/status/
  body values stay uncertain. No dependency or design scope changes.
- Tests compose the actual response parser, evidence validation, and preflight
  classifier without mocks. Java 21 release CLI exited 0 (33 tests, 557 assertions);
  `.scry-results/` was empty. cljfmt and clj-kondo passed with zero warnings.
- Next adapter must bind evidence to one token/repository and completed processes.
  HTTP 404 envelope checks are not proof of live endpoint semantics. Pinned gh
  review and service wiring remain required. No network operations or hosted
  checks ran; build tests were not rerun because packaging is unchanged.


## 2026-10-23 — Slice 3g: GitHub lookup adapter

- Connected process, response, and evidence contracts for repository access
  followed by exact tag/Release reads. Each process has a 120-second limit.
  One allowlisted environment binds the selected token and host; ambient debug,
  proxy, alternate token, and deployment-secret values are excluded.
- Caller must provide a private empty gh config directory and pinned executable
  on PATH. These prerequisites and live endpoint/CLI semantics are not verified
  by this pass. No write path or publication gate uses this adapter yet.
- Pure environment tests and real local child-process tests use no mocks.
  Java 21 release CLI exited 0 (35 tests, 571 assertions); `.scry-results/`
  was empty. cljfmt and clj-kondo passed with zero warnings. An initial run also
  passed on Java 25 before the required Java 21 run. No network operations,
  dependency changes, or design scope changes. Packaging was unchanged.
- Next: pinned CLI behavior review, Clojars/deployment adapters, and publication
  gate wiring. Do not treat these local tests as live-service evidence.

## 2026-10-23 — Slice 3h: publication authorization gate

- Added pure `authorization/decide` using real request/notes validation. Only
  explicit publication, matching prepared notes, and boolean check success emit
  the exact `true` output string. Invalid context fails; preview preserves notes
  readiness without authorizing writes. No dependencies or design scope changed.
- Caller must bind version, changelog, and check result to the fixed target.
  This gate does not validate bundles or replace service preflight. Workflow and
  straight-line publication wiring remain open. Tests use no mocks.
- Java 21 release CLI passed: 37 tests, 595 assertions, exit 0, empty
  `.scry-results/`. cljfmt and clj-kondo passed with zero warnings. First test
  launch selected system Java 8 and failed before tests; reran with mise's
  Temurin 21. Formatting was corrected before the passing run. No remote writes
  or hosted checks ran; build tests were not rerun because packaging is unchanged.

## 2026-10-23 — Slice 3i: deployment dependency boundary

- Added deployment-only deps-deploy 0.2.2. Java 21 load assertions confirm the
  namespace loads with `:release:deploy`, is absent with `:release` alone, and
  leaves Clojure at 1.11.4. No runtime dependencies or design scope changed.
- Inspected the cached library source and resolved pomegranate 1.2.23 source.
  `deploy` copies POM text to a versioned file in the working directory, deletes
  it in finally, and prints the username. `deploy*` forwards neither a custom
  session nor timeout options. Exceptions propagate; there is no outer retry.
  Use scratch space outside the immutable bundle and suppress captured output.
- Resolved graph includes Resolver HTTP and Wagon transports (Resolver 1.9.4,
  Wagon 3.5.3, HttpClient 4.5.14). `javap` inspection of HttpTransporter shows
  default connect/request timeouts of 10 seconds/30 minutes. Transport selection
  and write retries still need inspection; do not claim the stock call meets
  no-retry requirements. A bounded child JVM is necessary but not sufficient.
- Java 21 release CLI exited 0: 37 tests, 595 assertions; `.scry-results/` empty.
  `cljfmt check deps.edn` and clj-kondo passed (zero warnings). No deployment,
  hosted checks, or remote writes. No tests were added: this slice changes only
  dependency composition, checked through actual classpath/load assertions.


## 2026-10-23 — Slice 3j: create-only Git tag adapter

- Added direct SHA-to-remote-tag publication, with a fresh exact-ref lookup and
  empty expected-value lease. No local tag or master update; no retries. Existing
  identical and different tags fail. Concurrent external writers remain a race;
  an identical concurrent creation can yield Git's up-to-date result.
- Real bare-remote tests cover target SHA, conflict preservation, unchanged master
  and local tags, invalid input and tree objects. No mocks or external writes.
- Java 21 release CLI exited 0: 38 tests, 605 assertions; `.scry-results/` empty.
  cljfmt and clj-kondo passed with zero warnings. Packaging is unchanged.
- Before hosted wiring, extend bounded execution to clean up Git transport
  descendants and test unavailable remotes/receive rejection. Caller must supply
  trusted private repository configuration and an explicit secret-safe environment.
  Authorization/preflight and outcome sequencing are not connected. Dependency
  transport review remains open; no hosted verification or publication ran.


## 2026-10-23 — Slice 3k: tag failure coverage

- Real unavailable-origin and rejecting pre-receive-hook tests confirm unchanged
  refs/local state and fixed diagnostics without remote output or causes. No mocks,
  dependencies, API changes, or design deviations.
- Java 21: `clojure -M:test:release:release-test -m scry.cli --dir test-release`
  exited 0 (39 tests, 617 assertions); `.scry-results/` was empty.
  `cljfmt check dev/release test-release` and clj-kondo on these paths passed
  with zero warnings. No external writes or hosted checks ran.
- Next slice still needs process-tree timeout cleanup before hosted Git wiring;
  these completed local failures do not prove cleanup of stalled transports.
  Deployment transport review and publication sequencing also remain open.


## 2026-10-23 — Slice 3l: observed child timeout cleanup

- Timeout cleanup now kills observed descendants before the parent, allowing
  five seconds for parent reaping and five seconds for forced parent exit.
  A real waiting shell and sleep child test confirms the child is no longer
  alive when the redacted timeout error returns. No mocks or dependencies.
- Snapshot cleanup is not process-group containment. Detached children and
  concurrent spawning remain outside the runner contract. Check actual Git
  transport lifecycle before hosted wiring; do not mark that review complete.
- Java 21 release CLI exited 0: 40 tests, 620 assertions; `.scry-results/`
  empty. cljfmt and clj-kondo passed with zero warnings. Initial compilation
  required a fully qualified ProcessHandle type; corrected before verification.
  No external writes or hosted checks; packaging is unchanged.

## 2026-10-23 — Slice 3m: GitHub Release write adapter

- Added `github-publish/publish!` using existing create-only arguments and the
  isolated gh environment. One invocation has a 120-second limit; output is
  discarded. Nonzero or missing exit evidence fails with fixed diagnostics.
- Caller gates and pinned CLI retry review remain required before hosted wiring.
  No sequence executor, dependency changes, or design scope changes. Pure exit
  policy and invalid-input tests use no mocks; no Release was created.
- Java 21 release CLI exited 0: 42 tests, 638 assertions; `.scry-results/` empty.
  cljfmt and clj-kondo passed with zero warnings after formatting correction.
  Packaging was unchanged; build tests and hosted checks were not run.

## 2026-10-23 — Slice 3n: selected deployment transport review

- Inspected cached pomegranate 1.2.23 source and Java 21 `javap -c -p`
  output for Wagon 3.5.3 AbstractHttpClientWagon, Resolver WagonTransporter,
  DefaultDeployer, and HttpClient StandardHttpRequestRetryHandler. Pomegranate
  explicitly selects Wagon; the earlier HTTP-transporter inspection does not
  describe this deployment path. PUT has independent 429 recursion and is
  considered idempotent by the default request handler. Plan records controls
  and remaining proof requirements; no adapter is authorized by this review.
- Java 21 `clojure -M:release:deploy -e` load/inspection assertions passed:
  repository system exists; a zero-count real retry handler rejects attempt 1.
  Initial exploratory calls used a private var directly and omitted required
  session options; corrected the inspection without changing production code.
- Documentation-only slice; no dependencies, APIs, design scope, or tests changed.
  `git diff --check` passed. No service requests, remote writes, hosted checks,
  or deployment ran. Next: real local transport proof plus connector/checksum
  and redirect inspection, then bounded deployment wiring.


## 2026-10-23 — Slice 3o: real Wagon retry checks

- Added isolated `:deploy-test` alias with startup retry/backoff properties and
  a real loopback HTTP transport test. Success preserves payload bytes; 429,
  503, 500, and response loss each fail after one PUT. No mocks or new dependencies.
- This tests HttpWagon only. Resolver connector/checksum behavior, redirects,
  and production bounded child-JVM setup remain next. Design scope is unchanged;
  no deployment adapter is authorized by these tests.
- Java 21 deployment CLI passed (1 test, 5 assertions); release CLI passed
  (42 tests, 638 assertions). Both exited 0 with empty `.scry-results/`.
  cljfmt and clj-kondo passed with zero warnings; `git diff --check` passed.
  No external requests, publication, hosted checks, or packaging changes.


## 2026-10-23 — Slice 3p: redirect transport evidence

- Real loopback tests show Wagon follows 301/302/303/307/308 with another PUT
  carrying identical artifact bytes. Startup retry controls do not stop this.
  Java 21 bytecode inspection confirms a custom WagonRedirectStrategy.
- Initial rejection assertions failed for all five statuses; structured scry
  results established the repeated writes. Tests now characterize that unsafe
  dependency behavior. Production must disable redirects before deployment;
  connector/checksum review remains separate. No design scope change.
- Java 21 deployment CLI exited 0 (2 tests, 10 assertions), with empty
  `.scry-results/`. cljfmt, clj-kondo (zero warnings), and `git diff --check`
  passed. No production code or dependencies changed. No external requests,
  hosted checks, publication, or packaging tests ran.


## 2026-10-23 — Slice 3q: reject deployment redirects

- Added deployment-only Wagon factory. Bytecode inspection of ConfigurationUtils
  confirms `http.protocol.handle-redirects=false` maps to RequestConfig redirect
  rejection. Apply it to all methods. No new dependency or design scope change.
- Real loopback tests prove 301/302/303/307/308 fail after one PUT. Existing
  success/status/lost-response tests now use the factory; stock behavior remains
  separately characterized. Startup retry controls remain necessary.
- Next: register this factory in the deployment repository system, inspect
  connector/checksum behavior, then implement bounded child-JVM deployment.
  The stock pomegranate provider does not use this factory automatically.
- Java 21 deployment CLI passed (3 tests, 15 assertions); release CLI passed
  (42 tests, 638 assertions). Both exited 0 with empty `.scry-results/`.
  cljfmt and clj-kondo passed with zero warnings; `git diff --check` passed.
  No external requests, remote writes, hosted checks, or packaging changes.


## 2026-10-23 — Slice 3r: isolated deployment repository system

- Added `repository/create` with per-instance configured HTTP(S) Wagon provider
  and BasicRepositoryConnector. No global registration or dependency changes.
  Pomegranate's deployment function creates its own private system, so this
  factory is separate; the future adapter must use it explicitly.
- Real loopback Resolver tests prove one failed artifact PUT for five redirect
  statuses and 429/500/503. Checksum and metadata paths remain next; these tests
  do not establish complete deployment safety. No design scope change.
- Java 21 deployment CLI exited 0 (4 tests, 23 assertions); release CLI exited 0
  (42 tests, 638 assertions). Both had empty `.scry-results/`. cljfmt and clj-kondo
  passed with zero warnings; `git diff --check` passed. No external requests,
  publication, hosted checks, or packaging changes.


## 2026-10-23 — Slice 3s: connector checksum failure evidence

- Java 21 bytecode review of BasicRepositoryConnector 1.9.4 PutTaskRunner
  shows checksum upload exceptions are logged and ignored. Real loopback
  Resolver tests confirm success for checksum responses 429/500/503 and
  continued metadata writes. No PUT repeats; artifact SHA-1/MD5 bytes match.
- This characterizes unsafe library behavior, not an accepted publication
  policy. Next: fail-fast checksum handling and metadata merge/failure tests.
  Design scope, production code, and dependencies are unchanged.
- Initial inventory assertions omitted generated metadata; structured scry
  evidence identified the three extra writes. Corrected the inventory checks.
  Java 21 deployment CLI passed (5 tests, 47 assertions), exit 0, empty
  `.scry-results/`. cljfmt, clj-kondo (zero warnings), and `git diff --check`
  passed. Only deployment tests changed; release/build suites were not rerun.
  No external requests, publication, or hosted checks ran.


## 2026-10-23 — Slice 3t: metadata failure boundaries

- Real Resolver tests cover metadata GET 401/403/500/503 and PUT 429/500/503.
  Reads fail after the three artifact/checksum writes; failed metadata PUTs
  add one write and stop before metadata checksums. No write repeats.
- Existing metadata merge coverage and fail-fast checksum handling remain next.
  No production code, dependencies, APIs, or design scope changed.
- Java 21 `clojure -M:test:deploy:deploy-test -m scry.cli --dir test-deploy`
  exited 0: 6 tests, 68 assertions, empty `.scry-results/`. `cljfmt check
  test-deploy`, `clj-kondo --lint test-deploy --fail-level warning`, and
  `git diff --check` passed. No external requests or publication ran.
  Release, runtime, and packaging suites were not rerun for this test-only change.


## 2026-10-23 — Slice 3u: existing metadata merge

- Real loopback Resolver coverage now serves existing metadata and checks retained
  versions, the new release version, exact metadata checksum bytes, and unique
  PUT paths. No mocks, dependencies, production APIs, or design changes.
- Connector checksum failures still need fail-fast handling before publication
  wiring. This merge test does not establish Clojars service behavior.
- Java 21 deployment CLI exited 0 (7 tests, 77 assertions); `.scry-results/`
  was empty. cljfmt, clj-kondo (zero warnings), and `git diff --check` passed.
  No external requests or publication ran. Release/runtime/build suites were
  not rerun for this deployment-test-only change.


## 2026-10-23 — Slice 3v: metadata checksum failure evidence

- Real loopback tests isolate metadata SHA-1 and MD5 failures for 429/500/503.
  Resolver reports success and sends all six PUTs once. The fixture now accepts
  per-path checksum responses; existing tests retain their behavior. No mocks.
- Java bytecode inspection of pinned BasicRepositoryConnector 1.9.4 confirms
  uploadChecksum catches Exception and logs it without propagating failure.
  Fail-fast handling must cover metadata as well as artifact checksums; this
  test-only slice does not implement that handling or authorize deployment.
- Java 21 deployment CLI exited 0 (8 tests, 107 assertions); `.scry-results/`
  was empty. cljfmt and clj-kondo passed with zero warnings; `git diff --check`
  passed. No dependencies, production APIs, or design scope changed. Release,
  runtime, and packaging suites were not rerun. No external requests or
  publication ran.


## 2026-10-23 — Slice 3w: explicit deployment session

- Added `repository/session`: one connector transfer thread and 30-second
  connect/request limits. Java 21 bytecode inspection confirms the pinned
  connector defaults to five threads. The caller supplies its private local
  repository manager; outer process bounds and checksum failure handling remain
  required. Serial transfers do not prevent ignored checksum failures.
- Real Resolver tests now use this session; a value test checks its settings.
  No mocks, dependencies, or design scope changes. Design remains synchronized
  without edits because its bounded/no-retry contract is unchanged.
- Java 21 deployment CLI exited 0 (9 tests, 111 assertions); `.scry-results/`
  was empty. cljfmt and clj-kondo passed with zero warnings; `git diff --check`
  passed. No external requests or publication. Runtime, release, and packaging
  suites were not rerun because only deployment session configuration changed.

## 2026-10-23 — Slice 3x: lost metadata response boundaries

- Real loopback Resolver tests now close connections after receiving metadata
  or checksum payloads, without sending response headers. Metadata PUT fails
  after one attempt and stops later writes. Either metadata checksum can lose
  its response while Resolver reports success; all six PUT paths occur once.
- This extends transport evidence, not deployment authorization. Strict checksum
  failure handling remains required. No production API, dependency, or design
  scope changed; no mocks or external requests were used.
- Java 21 deployment CLI exited 0 (10 tests, 123 assertions); `.scry-results/`
  was empty. cljfmt, clj-kondo, and `git diff --check` passed. Release, runtime,
  and build suites were not rerun for this deployment-test-only change.


## 2026-10-23 — Slice 3y: lost artifact checksum responses

- Real loopback Resolver tests now close the connection after an artifact SHA-1
  or MD5 payload. Resolver reports success and continues metadata publication;
  all six PUT paths occur once. Strict checksum handling must cover response
  loss as well as HTTP errors. No mocks or production changes.
- Java 21 `clojure -M:test:deploy:deploy-test -m scry.cli --dir test-deploy`
  exited 0 (11 tests, 133 assertions); `.scry-results/` was empty. `cljfmt check
  test-deploy`, `clj-kondo --lint test-deploy --fail-level warning`, and
  `git diff --check` passed. No dependencies, APIs, or design scope changed.
  Release, runtime, and build suites were not rerun for this test-only change.
  No external requests or publication ran. Strict checksum handling remains next.

## 2026-10-23 — Slice 3z: retained upload failure guard

- Added `upload-guard` and opt-in `wagon/guarded`. One shared failure atom
  blocks later file PUTs and retains a secret-free failure for a mandatory
  post-Resolver check. Successful transfers leave the guard clear. Exceptions
  discard transport messages and causes; no retry or rollback occurs.
- Real loopback tests cover 429/500/503, redirect, and lost-response failures,
  including a later upload through a separate Wagon. No mocks or remote writes.
- Not wired to the repository provider. Next cover streaming upload entry points
  and real Resolver checksum failures, then require the retained-failure check.
  The current file-only factory is not sufficient for publication. Design scope
  remains unchanged; the plan and checklist record this implementation boundary.
- Java 21 deployment suite passed (12 tests, 151 assertions), exit 0; failure
  directory empty. cljfmt, clj-kondo, and diff whitespace checks passed.
  Runtime, release, and packaging suites were not rerun for this isolated change.

## 2026-10-23 — Slice 3aa: streaming upload guard

- Guarded Wagon now covers both public `putFromStream` overloads. Real HTTP
  tests verify exact bytes, failures (429/500/503), all five redirects, and lost
  responses. A retained failure blocks later file and stream uploads through
  separate Wagons. No mocks, dependencies, or design scope changes.
- Next connect the shared guard to the repository provider and require the
  post-Resolver check. This pass does not fix the repository's ignored checksum
  failures or authorize deployment. Design acceptance contracts remain unchanged.
- Java 21 deployment CLI passed: 13 tests, 247 assertions, exit 0; no files in
  `.scry-results/`. cljfmt, clj-kondo (zero warnings), and `git diff --check`
  passed. No external requests or publication. Release, runtime, and packaging
  suites were not rerun for this isolated deployment transport change.


## 2026-10-23 — Slice 3ab: guarded Resolver deployment

- Repository providers can now share one upload guard. `repository/deploy!`
  rejects retained failure even when Resolver ignores a checksum exception.
  Real loopback tests cover artifact/metadata SHA-1 and MD5, 429/500/503,
  lost responses, no later PUTs, no retries, and successful completion.
- Next: bounded deployment entry point must own matching guard/system/serial
  session and startup controls. Unguarded creation remains for characterization,
  not publication. No dependencies or design scope changed.
- Java 21 deployment CLI passed (14 tests, 297 assertions), exit 0 and empty
  `.scry-results/`. cljfmt, clj-kondo (zero warnings), and `git diff --check`
  passed. Initial form damage was repaired with the repair tool and structural
  editing before verification. No external requests or publication. Release,
  runtime, and packaging suites were not rerun for this deployment-only change.


## 2026-10-23 — Slice 3ac: owned deployment operation

- Added `deployment/deploy!` to own guard/system/session pairing and local
  repository manager setup. Returns `{:status :complete}` only after the
  retained-failure check; exceptions omit underlying messages and causes.
- Existing real Resolver checksum tests now exercise this operation, including
  artifact/metadata failures and lost responses. No mocks or dependencies added.
  Next: bounded child JVM, startup controls, and existing-file request creation.
  The operation does not establish publication gates or an outer deadline.
- Java 21 deployment CLI passed (14 tests, 297 assertions), exit 0, empty
  `.scry-results/`. cljfmt and clj-kondo passed with zero warnings; diff whitespace
  checks passed. No external requests or publication. Release, runtime, and build
  suites were not rerun for this isolated deployment change. Design is unchanged.


## 2026-10-23 — Slice 3ad: deployment startup validation

- Added exact retry-property validation before owned deployment transport creation.
  Failures omit property values and causes. This does not prove initialization
  order: the next child-JVM launcher must set controls at JVM startup.
- Tests validate real JVM settings and each missing/unsafe value without mocks
  or global property mutation. No dependencies or design scope changed.
- Java 21 deployment CLI passed (15 tests, 359 assertions), exit 0, empty
  `.scry-results/`. cljfmt and clj-kondo passed with zero warnings after a
  formatting correction. No external requests or publication. Runtime, release,
  and build suites were not rerun for this deployment-only change.


## 2026-10-23 — Slice 3ae: existing-file deployment request

- Added `deployment-request/create`: selects only release.jar and pom.xml,
  fixes the coordinate and Clojars HTTPS destination, and attaches in-memory
  Resolver authentication. No build, file mutation, or dependency change.
- Tests inspect real Resolver artifacts and authentication values without mocks
  or network access. Invalid versions and blank credentials have fixed errors.
  Bundle validation and protection remain caller obligations. Next: child-JVM
  launcher and entry point with startup controls and a bounded process.
- Java 21 deployment CLI passed (17 tests, 383 assertions), exit 0, empty
  `.scry-results/`. cljfmt, clj-kondo (zero warnings), and diff checks passed.
  No external requests or publication. Release/runtime/build suites were not
  rerun for this isolated deployment change. Design contracts remain unchanged.


## 2026-10-23 — Slice 3af: deployment child entry point

- Added `deployment-child` with three non-secret arguments and environment-only
  credentials. It connects the existing request to the owned deployment operation;
  invalid input has fixed diagnostics and exit 1. Parent must capture dependency
  output, enforce startup controls/deadline, and validate/protect bundle paths.
- Pure rejection checks and a real child JVM test use no mocks or network writes.
  Java 21 deployment CLI passed (19 tests, 402 assertions), exit 0, empty
  `.scry-results/`. cljfmt, clj-kondo, and diff checks passed. A test delimiter
  error was repaired with clj-paren-repair before verification.
- No dependencies or design contracts changed. Launcher and successful child
  transport coverage remain next. Release/runtime/build suites and hosted checks
  were not run for this deployment-only change; no publication occurred.


## 2026-10-23 — Slice 3ag: deployment launch values

- Added pure direct-Java command construction and credential-only environment.
  Absolute classpath entries exclude implicit current-directory loading; startup
  retry properties precede clojure.main. Ambient Java injection variables and
  unrelated tokens are excluded. Caller must supply trusted control classpath.
- Next connect to bounded process execution and private repository setup; capture
  and discard all child output. This slice does not execute or authorize writes.
- Java 21 deployment CLI passed (22 tests, 437 assertions), exit 0, empty
  `.scry-results/`. cljfmt and clj-kondo passed with zero warnings. Initial CLI
  load failure from a hyphenated filename was corrected before the passing run.
  No dependencies or design contracts changed. No external publication; runtime,
  release, build, and hosted checks were not run for this isolated change.


## 2026-10-23 — Slice 3ah: bounded deployment execution

- Connected launch values to the shared process runner with a five-minute limit.
  Output is discarded; failure has fixed diagnostics without causes. No build,
  retry, or publication gate is added. Caller still owns private repository setup.
- A real child JVM rejects a missing bundle before transport creation. Tests also
  cover parent validation redaction. Successful child transport coverage remains
  open; existing in-process transport tests do not prove that path. No mocks.
- Deployment test alias now includes the release source path for process/run!.
  Java 21 deployment CLI passed (24 tests, 444 assertions), exit 0, empty
  `.scry-results/`. cljfmt, clj-kondo (zero warnings), and diff checks passed.
  No runtime dependencies or design changes. No external requests/publication.
  Release, runtime, packaging, and hosted checks were not run.


## 2026-10-23 — Slice 3ai: private deployment repository lifecycle

- Launcher now creates and removes its own owner-only POSIX repository. Its fifth
  argument is a trusted scratch parent. Cleanup does not follow symbolic links;
  any cleanup failure uses the existing uncertain outcome. Bundle stays separate.
- Real filesystem tests cover empty creation, permissions, nested cleanup on both
  exits, and preservation of linked external content. Existing real child failure
  test also verifies scratch cleanup. No mocks or dependencies added.
- Java 21 deployment CLI passed (25 tests, 454 assertions), exit 0, empty
  `.scry-results/`. cljfmt, clj-kondo, and diff checks passed. No external requests
  or publication. Runtime, release, build, and hosted checks were not rerun.
- Next: successful child transport coverage and remaining publication gates.
  Design contracts remain unchanged.


## 2026-10-23 — Slice 3aj: Clojars lookup values

- Added fixed-coordinate JAR/POM URLs and combined response evidence. Either
  existing file is a conflict, including partial publication. Two completed
  404s require independent visibility evidence; redirects and errors fail closed.
- Next adapter must bind responses to these URLs and establish public repository
  visibility and absence semantics. Pure values alone do not prove either.
  No dependencies or design scope changed; no mocks or network requests.
- Java 21 release CLI passed (44 tests, 698 assertions), exit 0, empty
  `.scry-results/`. cljfmt and clj-kondo passed with zero warnings. An initial
  test delimiter error was repaired with clj-paren-repair before verification.
  Deployment, runtime, build, and hosted checks were not rerun for this pure
  release-policy change. No publication occurred.


## 2026-10-23 — Slice 3ak: bounded Clojars reads

- Added `clojars-lookup/responses!` for the fixed JAR/POM HEAD URLs. It sends
  no credentials, bypasses ambient proxies, rejects redirect following, and
  uses 30-second connect/request limits. Transport failures return uncertain
  values without causes; interruption is preserved and stops the lookup.
- Real loopback tests cover success, redirects, access/server errors, and
  withheld headers. No mocks or external requests. HEAD semantics and
  independent public visibility still need proof before preflight wiring.
- Java 21 release CLI passed (47 tests, 723 assertions), exit 0; no scry
  failure files. cljfmt and clj-kondo passed with zero warnings; diff checks
  passed. No dependencies or design contracts changed. Deployment, runtime,
  build, and hosted checks were not rerun. No publication occurred.

## 2026-10-23 — Slice 3al: independent Clojars visibility

- Added a credential-free HEAD probe for the already pinned scry 0.1.61 POM.
  `clojars-evidence/visible?` accepts only a completed numeric 200. It cannot
  clear absence by itself. Uses the existing bounded, non-redirecting transport.
- Read-only `curl -I --max-time 20` observed 200 for that POM and 404 for
  closed-record 0.1.0's POM at repo.clojars.org. These are observations, not
  proof of cache/absence semantics. Endpoint review and preflight wiring remain
  open. No publication, credentials, dependencies, or design changes.
- Java 21 release CLI passed: 48 tests, 734 assertions, exit 0; no scry failure
  files. cljfmt and clj-kondo passed (zero warnings). Deployment, runtime,
  packaging, and hosted checks were not rerun. Tests use pure values and the
  existing real local HTTP transport tests, without mocks.

## 2026-10-23 — Slice 3am: Clojars cache revalidation

- Shared HEAD transport now requests `Cache-Control: no-cache` to revalidate
  cached responses, including negative responses. Real loopback tests check
  the header across success/error/redirect statuses; no mocks or external reads.
- This is a request, not proof of CDN compliance or authoritative absence.
  Endpoint review and preflight wiring remain open. Design scope and
  dependencies are unchanged.
- Java 21 release CLI passed (48 tests, 734 assertions), exit 0; no files in
  `.scry-results/`. cljfmt and clj-kondo passed with zero warnings; diff checks
  passed. Deployment/runtime/build and hosted checks were not rerun for this
  isolated read-transport change. No publication occurred.


## 2026-10-23 — Slice 3an: interrupted Clojars reads

- Added real loopback cancellation coverage, coordinated with latches rather
  than sleeps. It verifies worker termination, preserved interrupt status, and
  fixed exception data without a cause or request path. No mocks or remote reads.
- Java 21 release CLI passed (49 tests, 737 assertions), exit 0; no scry
  failure files. cljfmt and clj-kondo passed with zero warnings; diff check passed.
- No production code, dependency, or design contract changed. Endpoint absence
  semantics and preflight wiring remain next. Deployment, runtime, packaging,
  and hosted checks were not rerun; no publication occurred.


## 2026-10-23 — Slice 3ao: local Git transport timeout

- Added a real bare-remote push with a stalled pre-receive hook. The shared
  runner times out, terminates the hook, and preserves remote refs/local state.
  No mocks or external writes. Hosted HTTPS lifecycle review remains open.
- Java 21 release CLI passed (50 tests, 746 assertions), exit 0; no scry
  failure files. cljfmt and clj-kondo passed with zero warnings. Initial test
  delimiter damage was fixed with clj-paren-repair before verification.
- No production code, dependencies, or design contracts changed. Deployment,
  runtime, build, and hosted checks were not rerun. Next slices still need
  endpoint/CLI review and publication wiring; this test does not authorize it.


## 2026-10-23 — Slice 3ap: preserve process cancellation

- The shared runner now restores interrupt status after cleanup for an interrupted
  wait. Previously its broad exception handler discarded cancellation. A real
  pre-interrupted worker exercises process creation, waiting, and cleanup without
  mocks. Fixed diagnostics still omit commands, output, and causes.
- Java 21 release CLI (`clojure -M:test:release:release-test -m scry.cli --dir
  test-release`) exited 0: 51 tests, 748 assertions; no scry failure files.
  cljfmt and clj-kondo passed for both changed Clojure files (zero warnings).
  Design contracts and dependencies are unchanged. Hosted lifecycle review and
  publication wiring remain open. No external requests or publication ran.


## 2026-10-23 — Slice 3aq: cancellation before launch

- Reject known cancellation before starting a subprocess, so an already-cancelled
  publication caller cannot start a write. Interrupt status and fixed diagnostics
  are preserved. The real shell test now checks that its output file stays empty.
- The earlier pre-interrupted test no longer reaches wait cleanup. Added an
  explicit follow-up for coordinated in-flight cancellation coverage. Cancellation
  after the initial check remains a race; no broader containment claim is made.
- Java 21 release CLI passed (51 tests, 749 assertions), exit 0; no scry failure
  files. cljfmt and clj-kondo passed with zero warnings. No dependency or design
  changes. Runtime, deployment, build, and hosted checks were not rerun. No
  external requests or publication occurred.


## 2026-10-23 — Slice 3ar: in-flight process cancellation

- Added real shell/TCP coordination before interrupting the process worker.
  Tests check parent/observed-child termination, preserved interrupt status,
  and fixed diagnostics. No sleeps, mocks, production changes, or external writes.
- The fixture requires `/usr/bin/nc` and loopback networking. Snapshot cleanup
  limitations remain unchanged; hosted transport review and publication wiring
  remain open. No dependency or design contract changed.
- Java 21 release CLI passed (52 tests, 754 assertions), exit 0; `.scry-results/`
  was empty. cljfmt and clj-kondo passed with zero warnings; diff check passed.
  Deployment, runtime, build, and hosted checks were not rerun for this test-only
  slice. Next work retains the endpoint/CLI review and publication gates.

## 2026-10-23 — Slice 3as: GitHub CLI write review

- Reviewed candidate gh v2.102.0 source; internal asset retries and automatic
  draft deletion violate the task contract. Two older versions share the paths.
  Source links, archive digest, and decision options are in `github-cli-review.md`.
- No production code, dependency, or accepted design contract changed. No tests
  were required or rerun for this documentation-only review. Read-only source
  retrieval succeeded; no credentials, remote writes, or publication were used.
- Next slice must resolve the command contract before connecting the existing
  Release adapter; one bounded invocation does not prevent internal retries.

Historical blocker: the old contract prohibited internal gh retries and draft
cleanup. This is no longer an active decision request; task 015 is closed.
The replacement task accepts standard tool behaviour. The source findings above
remain evidence, not requirements for the replacement implementation.


## 2026-10-23 — Scope correction and closure

- User clarified that the intended reference is scry's `build.clj` deployment,
  then requested scope correction. Closed this task without completion under
  the Munera scope-change rule; created task 016 with its own design and plan.
- The replacement accepts standard deps-deploy and gh behaviour, including
  internal retries, redirects, checksum handling, and temporary draft cleanup.
  No transport replacement or separate gh API write layer is required.
- Existing code and test evidence remain in Git. Task 016 will retain useful
  notes/preparation code and remove unused bundle, remote-policy, process,
  and custom deployment machinery. This documentation pass changes no code.
- Updated release architecture, task order, and working memory. Validation:
  relative Markdown links and `git diff --check`; no code tests, service writes,
  hosted verification, or real release.
