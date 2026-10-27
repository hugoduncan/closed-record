---
title: Markdown link coverage and network verification
status: active
category: tooling
tags: [markdown, lychee, babashka, ci, contract-tests, tls]
related: [mementum/knowledge/dev-tooling.md, mementum/knowledge/artifact-verification.md, README/development.md, dev/link_check_test.clj]
depends-on: [bb.edn, dev/link_check.clj, lychee.toml]
---

💡 Use these rules when changing documentation checks, their policy, or tool
versions. Evidence is from Babashka 1.13.225 and lychee 0.24.2. Recheck
version-specific behavior on upgrades.

## Prevent silent loss of coverage

- Select tracked Markdown with `git ls-files -z -- '*.md'`. Split on NUL
  and pass each path as a separate process argument after `--`. This retains
  paths with spaces and excludes untracked output without directory crawling.
  Keep `hidden = true` and `no_ignore = true` so lychee does not omit selected
  inputs because of hidden paths or ignore rules.
- Use the TOML scheme array `["http", "https", "file"]`. In planning probes,
  `--scheme http,https,file` silently excluded all links. Repeated scheme
  flags worked. A successful exit alone does not prove coverage; keep negative
  fixtures for each required link class and document location.
- Fragment checking is off by default. The local pass uses
  `--offline --include-fragments=anchor-only`; the online pass uses
  `--include-fragments=none`. This checks local headings without requiring
  remote heading checks. Use `--root-dir` for repository-root links, not
  `--base-url`, which changes source-relative resolution.
- Execute both passes eagerly before selecting the first nonzero exit.
  A fixture with both a missing local heading and an HTTP 404 must report
  both failures. Separate failure fixtures cannot detect an early return
  that omits the online pass.

## Do not infer network guarantees from configuration

- A permanent HTTP 503 failure does not prove retries work. With the current
  success-only status policy and `max_retries = 2`, a real endpoint returning
  503 once, then 200, failed with `Rejected status code: 503`. Lychee 0.24.2
  did not retry that rejected response. Test recovery within the retry budget
  separately from permanent rejection; do not accept error statuses to make
  a recovery test pass. This observation does not establish behavior for
  other failures or tool versions.
- HTTP fixtures do not prove HTTPS coverage or certificate verification.
  A macOS loopback TLS probe could not establish trust through the child
  process's `SSL_CERT_FILE`; lychee reported certificate error -67843.
  The untrusted case also failed. This is not a portable trust mechanism or
  proof that HTTPS is broken. Require both trusted-success and
  untrusted-failure cases before claiming TLS fixture coverage. Do not alter
  system trust or disable verification to obtain a pass.
- Bound real network fixtures independently of the request timeout. The
  contract runner uses a 90-second child-process deadline and process-tree
  cleanup. Use loopback responses and observable diagnostics, not exact
  request counts or live websites, for deterministic assertions.

Git fixture isolation is already documented in
`mementum/knowledge/artifact-verification.md`; reuse it rather than relying
on a temporary working directory alone.

## Evidence

Task 017 `implementation.md`: planning probes and review follow-up.
Task-scoped commits: `072539f` (selection and policy), `f2c881d`
(mixed failures), `5d2aa94` (TLS probe), `3c7f620` (timeouts and retry probe),
and `8dec9c8` (eager process execution).
