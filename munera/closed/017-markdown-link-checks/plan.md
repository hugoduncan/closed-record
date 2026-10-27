# Plan: Markdown link checks

## Approach

### Task and policy

Add `bb.edn` with one public task, `lychee:check`, and a small helper in
`dev/link_check.clj`. Follow scry's named-task pattern, not its mise setup or
unrelated tasks. Keep all tooling outside runtime dependencies.

Use Git from the repository root to obtain tracked Markdown paths with
`git ls-files -z -- '*.md'`. Split on NUL, not whitespace. Pass explicit file
arguments to lychee after `--`; do not use shell expansion, directory crawling,
or a newline-separated input list. Use the working-tree content of these
tracked files. Include hidden paths and historical documents. Fail on Git
errors or missing tracked inputs rather than silently reduce coverage.

Add `lychee.toml` for common policy: `scheme = ["http", "https", "file"]`,
`hidden = true`, `no_ignore = true`, TLS verification, GET requests, no cache,
no cookie output, and detailed diagnostics without a progress bar. Use a
20-second request timeout, two retries, one-second retry wait, at most ten
redirects, and eight concurrent requests. Accept only successful HTTP status
codes (`200..=299`); do not accept timeouts. Start with no exceptions.

The task supplies the absolute checkout path as `--root-dir` and explicitly
loads `lychee.toml`. Do not use `--base-url`: it changes source-relative link
resolution. Keep lychee's default directory handling: an existing directory
is valid without an index file. Do not scan code blocks or email addresses.

Run two lychee passes over the same input list:

1. Local: `--offline --include-fragments=anchor-only` checks files,
   directories, and same-document and cross-document headings.
2. Online: `--include-fragments=none` checks HTTP and HTTPS destinations,
   redirects, and TLS without requiring external heading validation. This
   also repeats inexpensive local existence checks.

These switches are repository-owned policy in the task, not CI options.
Run both passes and return nonzero if either fails. Missing executables,
invalid configuration, or process-launch failures must also fail. Preserve
lychee's source-document and failed-link diagnostics. Do not expose arbitrary
CLI overrides that can turn the default task into an offline-only success.
The task must not write reports, caches, or document changes.

### Versions and installation

Pin Babashka `1.13.225` and lychee `0.24.2`. Both are installed locally; release
metadata confirms platform archives and SHA-256 sidecars for these versions.
Document installation from the exact upstream release archives, checksum
verification, extraction to a directory on `PATH`, and version checks. Cover
Linux x86-64 and macOS arm64/x86-64 without requiring mise or Homebrew's
current version.

Add a separate `links` job named `Markdown links` to `.github/workflows/ci.yml`,
on `ubuntu-latest`, outside every matrix, with a 15-minute timeout. Use
`actions/checkout@v4.2.2` with `persist-credentials: false`, then
`DeLaGuardo/setup-clojure@13.4` with `bb: '1.13.225'`. Install lychee from the
`lychee-v0.24.2` release's `lychee-x86_64-unknown-linux-gnu.tar.gz` and matching
SHA-256 sidecar; fail if download or checksum verification fails. Extract in
runner temporary storage and add the binary directory to `GITHUB_PATH`.

Run the fixture suite, then `bb lychee:check` exactly once in this job. Do not
add a lychee action, token, secrets, caches, write permissions, or error
suppression. Preserve existing jobs and triggers. Extend existing format/lint
coverage to `bb.edn`; `dev` already covers the helper and fixture runner.

### Verification and repairs

Add a repeatable Babashka fixture runner, `dev/link_check_test.clj`, invoked
with `bb dev/link_check_test.clj`. Use temporary Git repositories containing
copies of the real task/helper/configuration and real lychee processes. Use
Babashka's HTTP server support on an assigned loopback port for deterministic
HTTP success, redirect, and failure responses. Do not mock subprocesses or
use live websites for fixture assertions. Isolate Git configuration and
inherited Git overrides. Always stop the server and remove temporary files.

Test document selection, path/fragment behaviour, exit propagation, useful
diagnostics, and absence of checkout changes. Keep deliberately broken
Markdown in temporary repositories, not tracked fixture files. Test the
live repository separately. Repair only links with clear intended targets;
record reasons beside exact, anchored URL exceptions in `lychee.toml`.

Update `README/development.md` with pinned installation commands, both check
commands, coverage, network requirements, failure behaviour, and exception
policy. Adjust its CI environment description: the link job needs neither
Java nor Clojure CLI. Record commands, exit codes, fixture results, repairs,
exceptions, and live external results in `implementation.md`. Hosted CI is
unverified unless existing evidence is available; do not push to obtain it.

## Risks

- Live sites can rate-limit or reject automated requests. Fail after bounded
  retries; do not broaden accepted statuses. An unclear destination or a
  broad repair requires a maintainer decision.
- Historical links can expose larger repair work. No document tree may be
  excluded. Preserve historical meaning and stop on unclear intent.
- Lychee fragments are disabled by default. Planning probes confirmed valid
  paths, spaces, root paths, directories, and duplicate-heading suffixes with
  `0.24.2`; enabled fragment checks returned exit 2 for a missing heading.
  Keep negative fixtures to prevent silent loss of coverage.
- Global ignore rules and hidden paths can reduce coverage unless explicit
  inputs, `hidden`, and `no_ignore` remain in use.
- Hosted tool installation and network behaviour remain unverified until CI
  runs. Local fixtures do not replace the live repository check.

## Slice order

1. Implement the pinned local task and shared policy. Done; temporary real-process
   probes passed.
2. Add and run isolated contract fixtures. Done: seven tests, 131 assertions.
   The complete repository check also passes; no repairs are needed.
3. Check the complete repository and repair clear broken links. Done: 133
   tracked documents, both passes successful, no repairs or exceptions.
4. Add the single CI job and development documentation. Done; actionlint and
   macOS arm64 archive installation pass.
5. Run final acceptance checks and record evidence. Done: quality checks,
   seven fixture tests (131 assertions), actionlint, and both live passes succeed.
   Hosted CI remains unverified; no push was made.
