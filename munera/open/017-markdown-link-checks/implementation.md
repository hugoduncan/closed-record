# Implementation notes

## 2026-10-27 — Architecture review

- No architectural review feedback.
- `ramora/META.md` and `doc/architecture.md` are absent. Used `AGENTS.md`
  and the architecture linked from `ramora/INDEX.md`, particularly
  `ramora/defclosedrecord/principles.md` and
  `ramora/defclosedrecord/embodiment.md`. Checked current CI and dependency
  boundaries against `.github/workflows/ci.yml` and `deps.edn`.

## 2026-10-27 — Ambiguity review

- No ambiguity review feedback.

## 2026-10-27 — Inconsistency review

- No inconsistency review feedback.

## 2026-10-27 — Review handoff

- Keep link repairs separate from check execution: implementation can correct
  existing links, but `bb lychee:check` must not rewrite documents. These
  requirements were reviewed as complementary, not conflicting.

## 2026-10-27 — Planning evidence and review

- Selected installed Babashka `1.13.225` and lychee `0.24.2`; both version
  commands exited 0. GitHub release metadata confirms exact-version archives
  and SHA-256 sidecars for Linux x86-64 and macOS arm64/x86-64.
- Temporary lychee probes passed relative paths, a target with spaces,
  repository-root paths, directories, same-document headings, and duplicate
  headings (exit 0, five links checked). With
  `include_fragments = "anchor-only"`, a missing cross-document heading
  returned exit 2 and named the source, line, target, and missing fragment.
  A separate offline same-document missing-heading probe also returned 2.
  Temporary directories were removed; these are planning probes, not task
  acceptance evidence.
- Lychee's comma-separated `--scheme http,https,file` silently excluded all
  probe links. Repeated `--scheme` flags worked; use the TOML scheme array.
  Fragment validation is off by default. An offline fragment pass followed
  by an online non-fragment pass avoids imposing external fragment checks.
- Confirmed `DeLaGuardo/setup-clojure@13.4` accepts `bb`, and the installed
  Babashka can load `org.httpkit.server` (exit 0). Archive installation and
  hosted CI execution remain unverified.
- Plan ambiguity review: no actionable findings. Read `steps.md` without
  changing it during review. Exact pins, policy ownership, fixture command,
  CI placement, failure behaviour, and maintainer-decision conditions are set.
- Design inconsistency review: no actionable findings within `design.md` or
  against current CI, development docs, scry's task/CI reference, and task
  014. Task 017 explicitly permits Babashka; scry supplies a named-task
  pattern, not its permissions or mise setup. The stable design is unchanged.

- Plan inconsistency review: no actionable findings across the task files.

## 2026-10-27 — Plan review handoff

- Fixture reference: `test-build/build_test.clj`, `prepare-config` and
  `fixture-env`, shows Git isolation with inherited `GIT_*` overrides removed,
  system configuration disabled, fixture-owned global configuration, and
  empty hooks. Adapt this pattern for Babashka; do not add a tools.build
  dependency to the link-check fixtures.

## 2026-10-27 — Slice 1: local command

- Added `bb.edn`, `dev/link_check.clj`, and `lychee.toml`. No design change.
  The helper rejects non-root invocation, empty inventories, and missing
  tracked files. Both passes run after ordinary lychee check failures; the
  first nonzero exit is returned. Launch errors also fail the task.
- Versions (each exit 0): `git --version`: 2.56.0; `bb --version`:
  1.13.225; `lychee --version`: 0.24.2.
- Real temporary Git repository probes used the actual three new files,
  isolated Git configuration, a document path with spaces, and a loopback
  HTTP server. `python3 /tmp/check-links-slice.py` exited 0: valid heading
  plus HTTP 200 returned 0; missing local heading returned 2; HTTP 404
  returned 2. Each case reported both passes. Negative diagnostics named
  the source and target. Full before/after file-byte inventories, including
  Git files, were unchanged. Temporary repositories and server were cleaned.
- The first probe found that `babashka.process/shell` needs argument spreading,
  not a command vector as one argument. Fixed with `apply`; all probes then
  passed. No mocks or replacement executables were used.
- `cljfmt check bb.edn dev/link_check.clj` and
  `clj-kondo --lint bb.edn dev/link_check.clj --fail-level warning` exited 0
  with no findings. Configuration loaded in both passes without errors.
- Next: replace these temporary smoke probes with the planned persistent
  Babashka contract suite. Full inventory/live external checks, installation
  verification, and hosted CI remain unverified. No exceptions were added.

## 2026-10-27 — Slice 2: persistent contract fixtures

- Added `dev/link_check_test.clj`. It copies the real task and policy into
  temporary Git repositories, isolates Git configuration and hooks, and uses
  real tools. HTTP cases use an assigned loopback port. No dependencies,
  production APIs, design contracts, or policy settings changed.
- `bb dev/link_check_test.clj`: exit 0, seven tests, 131 assertions, no
  failures or errors. Coverage includes tracked historical/hidden/ignored
  sources, untracked/generated/outside exclusions, paths and fragments,
  redirects, external fragment omission, HTTP 404/401/503, invalid TOML,
  missing tracked input, and missing tools through restricted PATHs.
- Each ordinary task run compares full file-byte inventories (including Git
  files) and Git status before and after. Missing-tool cases also compare
  inventories. Cleanup assertions confirm temporary workspace removal;
  the server stops in `finally`. The checkout gained only the intended test
  source before task records were updated.
- Babashka 1.13.225 and lychee 0.24.2 version commands exited 0.
  A separate `/bin/sh` invocation with `PATH=/nonexistent` returned 127 for
  `bb lychee:check`, confirming the missing-runner failure.
- Lychee normalizes a missing directory diagnostic by removing its trailing
  slash. The first run exposed that assertion mismatch; the test now checks
  the normalized target. The second complete run passed.
- `cljfmt check bb.edn dev/link_check.clj dev/link_check_test.clj` and
  `clj-kondo --lint bb.edn dev/link_check.clj dev/link_check_test.clj
  --fail-level warning`: exit 0, no findings, after formatting the test.
- Next slice: check the full tracked inventory and live external links.
  No URL exceptions were added. Live repository and hosted CI results remain
  unverified; installation verification and CI/docs are still pending.

## 2026-10-27 — Slice 3: repository baseline

- `git ls-files '*.md'` selected 133 tracked documents. Two runs of
  `bb lychee:check` exited 0. Each local pass reported 97 links, 47 unique,
  94 successful, and three excluded external links. Each online pass reported
  97 successful links, zero exclusions, and zero errors or timeouts.
  Live external checks passed; no repairs or URL exceptions were needed.
- On the second run, SHA-256 inventories of all tracked file bytes and
  `git status --porcelain=v1 --untracked-files=all` matched before and after.
  The command did not change tracked files or add visible output files.
- `bb dev/link_check_test.clj` exited 0: seven tests, 131 assertions,
  zero failures or errors. Existing real-tool fixtures remain sufficient;
  no implementation, test, dependency, or design changes were needed.
- Next: add CI and maintainer documentation, then run final acceptance.
  Hosted CI and archive installation remain unverified.


## 2026-10-27 — Slice 4: CI and documentation

- Added one non-matrix Markdown links job with pinned tools/actions, checksum
  verification, fixtures before the shared command, and a 15-minute timeout.
  Reviewed the diff: events, read-only permissions, credential-free checkouts,
  and existing jobs remain intact. Added bb.edn to quality checks. No runtime
  dependencies, policy exceptions, or design contracts changed.
- Documented pinned installation, scope, network needs, failures, and exception
  policy. Real macOS arm64 archive installation into a temporary binary directory
  passed checksums and returned Babashka 1.13.225 and lychee 0.24.2 (exit 0).
  Installation exposed lychee's enclosing archive directory; CI now strips it
  and documentation selects its binary explicitly. Linux archive layout was
  also inspected. Linux execution and macOS x86-64 installation are unverified.
- actionlint 1.7.12 on .github/workflows/ci.yml: exit 0. It is not on PATH;
  used /Users/duncan/.local/share/mise/installs/actionlint/1.7.12/actionlint.
- Full planned cljfmt and clj-kondo commands including bb.edn: exit 0, no
  findings. bb dev/link_check_test.clj: exit 0, seven tests, 131 assertions,
  no failures or errors. Existing real-process fixtures need no changes.
- Next: final acceptance, including a live check after documentation changes.
  A preliminary inventory script stopped before running lychee because tracked
  skill entries include directories (Git submodules); filter regular files for
  byte comparison. This is a verification-script issue, not a checker failure.
  Hosted CI remains unverified; no push was made.


## 2026-10-27 — Slice 5: final acceptance

- No code, dependency, policy, or design changes were needed. The stable
  design remains valid. Existing real-process tests cover the required
  contracts without mocks; no additional tests were needed.
- Acceptance 1 and 6: final `bb lychee:check` exited 0 over all 133 tracked
  Markdown documents. Local: 97 links, 94 successful, three external links
  excluded. Online: all 97 successful, no exclusions, errors, or timeouts.
  Live external checks passed. No link repairs or URL exceptions were needed.
- Acceptance 2 and 3: `bb dev/link_check_test.clj` exited 0: seven tests,
  131 assertions, zero failures or errors. This includes negative diagnostics,
  missing tools/configuration, before/after inventories, and workspace cleanup.
- Acceptance 4: actionlint 1.7.12 at the path recorded in slice 4 exited 0
  for `.github/workflows/ci.yml`. Final diff review confirms one shared task
  invocation outside the matrix, unchanged events and read-only permissions,
  credential-free checkouts, and no failure suppression. Hosted CI remains
  unverified; no push was made.
- Acceptance 5: reviewed the development guide against the pinned versions,
  installation, commands, scope, network requirements, and exception policy.
  Archive installation evidence and platform limits remain as recorded in slice 4.
- `cljfmt check src test dev test-build test-release build.clj bb.edn` and
  `clj-kondo --lint src test dev test-build test-release build.clj bb.edn
  --fail-level warning` both exited 0, with no findings. Version checks
  confirmed cljfmt 0.16.4, clj-kondo 2026.04.15, Babashka 1.13.225, and
  lychee 0.24.2 (all exit 0).
- Before the live check, Git reported no untracked Markdown. SHA-256 maps
  of all tracked regular files and full Git status matched before/after;
  submodule directories were not read as files. Final implementation diff
  has no runtime source/dependency changes, unrelated rewrites, broad
  exclusions, or leftover temporary files. All implementation steps are done;
  the task stays open for review.

## 2026-10-27 — Implementation review

- No new actionable findings. Contract tests passed (7 tests, 131 assertions),
  as did both live link passes, focused format/lint checks, and actionlint.
  Hosted CI remains unverified.

## 2026-10-27 — Test review

- Added 4 steps to be addressed.

## 2026-10-27 — Review follow-up

- Addressed mixed-failure review step. Both fragment and HTTP 404 diagnostics
  are required from one run. `bb dev/link_check_test.clj`: exit 0,
  8 tests, 139 assertions.
- HTTPS review step blocked locally: a real Python loopback TLS server with an
  OpenSSL-generated certificate failed the trusted case on macOS with lychee
  0.24.2. Child `SSL_CERT_FILE` did not establish trust; the verifier returned
  macOS error -67843 (certificate not trusted). The untrusted case failed as
  expected. Removed the failing probe. Need an environment-only trust mechanism
  supported by this verifier, or verified platform-specific fixture support.
  No system trust, production policy, or dependencies were changed.
- Network review step partly addressed: real unanswered HTTP requests and HTTP
  429 now require failure with source/link diagnostics. Each check has a
  90-second deadline and process-tree cleanup. Suite: exit 0, 9 tests,
  153 assertions; focused format/lint pass.
- Recovery remains blocked: an endpoint returning HTTP 503 once, then HTTP 200,
  failed with `Rejected status code: 503` under the unchanged policy. Lychee
  0.24.2 does not retry that rejected response. Removed the failing recovery
  probe; kept the step unchecked. Need a policy/tool decision for HTTP-response
  retries without accepting error statuses. No exact request counts are asserted.
- Addressed cleanup review step: removed the helper cleanup assertion; retained
  unconditional deletion and all command non-writing assertions. Final suite:
  `bb dev/link_check_test.clj`, exit 0, 9 tests, 125 assertions. Focused cljfmt
  and clj-kondo pass. Correction to the preceding entry: its lint run found an
  unresolved binding in the HTTP macro. Using the server's `as-channel` API
  resolved it; no lint suppression was added.
- Addressed 2 review steps; 2 remain unchecked with blocking evidence above.
  The network step has partial coverage. Design and production policy unchanged.

## 2026-10-27 — Test re-review

- No new actionable test findings.

## 2026-10-27 — Documentation review

- Added 2 steps to be addressed.

## 2026-10-27 — Documentation follow-up execution

- Addressed the changelog review step: added the command and CI check under
  Unreleased. `git diff --check` passed.
- Addressed the embodiment review step: documented four link-tool artifacts,
  the separate CI job, and the installation/command link. `bb lychee:check`
  passed both passes (98 links; online: 98 successful).
- Addressed 2 review steps. HTTPS remains blocked by macOS certificate trust
  ignoring child `SSL_CERT_FILE` in the recorded probe. Transient-response
  recovery remains blocked by lychee 0.24.2 rejecting HTTP 503 without retry
  under the current policy. Both test steps remain unchecked; prior probe
  evidence still applies. No production policy or design changes were made.

## 2026-10-27 — Documentation re-review

- No new actionable documentation findings.

## 2026-10-27 — Code-shaper review

- Added 1 step to be addressed.

## 2026-10-27 — Code-shaper follow-up execution

- Addressed 1 review step: construct both argument vectors before process
  execution. Retained eager passes, `--`, and first-nonzero exit selection.
- `cljfmt check dev/link_check.clj`, focused clj-kondo with warning failure,
  and `bb dev/link_check_test.clj` exited 0. All 9 tests and 125 assertions
  passed, including paths with spaces and mixed failures.
- Two test steps remain unchecked. Prior probe evidence still applies:
  macOS TLS trust ignores child `SSL_CERT_FILE`; lychee 0.24.2 rejects HTTP
  503 without retry under current policy. Completion needs a supported
  environment-only trust mechanism and a retry policy/tool decision.
  No production policy or design changes were made.

## 2026-10-27 — Code-shaper re-review

- No new actionable findings. Focused format/lint checks passed. Contract
  tests passed on rerun (9 tests, 125 assertions); the first run exited 137
  without a test summary.
