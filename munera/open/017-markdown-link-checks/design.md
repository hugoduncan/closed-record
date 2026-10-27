# 017 Markdown link checks

## Intent

Find broken links in repository Markdown before changes merge. Provide one
Babashka command for maintainers and CI. Use lychee to check both local
references and external web links.

## Context

The repository has no `bb.edn` and no link check. Its clj-kondo check runs
directly in `.github/workflows/ci.yml`; `README/development.md` gives the same
local command. CI runs on pushes to `master` and all pull requests, with
read-only permissions and no stored checkout credentials.

The task-runner reference is `clj-kondo:lint` in
`/Users/duncan/projects/hugoduncan/scry/scry-master/bb.edn`, called by that
project's `.github/workflows/ci.yml`. Follow its small named-task pattern and
local/CI command parity. Do not copy its unrelated tasks or require mise.
Adding Babashka here is an explicit part of this task, unlike task 014.

## Scope and contracts

### Local command and ownership

Expose `bb lychee:check`, run from the repository root. The task invokes the
lychee CLI; it does not implement Markdown parsing or link validation.
Repository-owned configuration defines shared link policy. CI owns tool
installation and orchestration, not a second set of checking rules.

Pin the supported lychee and Babashka versions. Document their installation
and the local command. Missing tools, invalid configuration, and failed
checks produce a nonzero exit. Diagnostics identify the source document and
failed link. The check must not rewrite documents.

### Documents and links

Check all Git-tracked `*.md` files, including root documents, `README/`,
`ramora/`, `mementum/`, `munera/`, and tracked hidden directories. Do not
silently omit closed tasks or other historical documents. Generated output,
untracked local files, and files outside the checkout are not input documents.
File selection must work with spaces in paths and must not depend on a
maintainer's global ignore rules.

Check relative file and directory references against their source document.
Resolve repository-root paths against the checkout. Check same-document and
cross-document Markdown heading fragments using lychee's supported GitHub
heading rules. Local links can target non-Markdown files.

Check external HTTP and HTTPS links with normal redirect handling and TLS
verification. An offline-only run does not satisfy the default command.
External fragment validation is not required. Email addresses and non-web
protocols are outside scope.

Use bounded request timeouts and retries. Failed requests, including exhausted
transient failures, fail the command. Do not globally accept authentication
errors, rate limits, or broken-link status codes. If a legitimate destination
cannot be checked, use a narrow, repository-owned exception with a reason.
Do not exclude whole document trees to make the initial check pass.

Correct existing broken links where the intended destination is clear.
Preserve historical meaning. Unclear destinations or broad document rewrites
need a maintainer decision, not a silent deletion or broad exclusion.

### Continuous integration

Add a clearly named link check to the existing CI workflow, once per run and
outside the Clojure runtime matrix. CI invokes `bb lychee:check` with the same
policy as the local command. Use explicit tool and action versions and a
finite job timeout.

Retain the current push and pull-request triggers, including fork support,
read-only permissions, and credential-free checkout. No repository secrets,
write token, privileged pull-request event, or error suppression is needed.
The check does not publish, open issues, or modify the checkout. Existing
format, lint, test, build, and release-tooling checks remain intact.

## Design choices

Prefer a thin Babashka task plus standard lychee configuration. A direct CI
command alone would not meet the requested local task interface. A separate
link-checking service or a custom crawler adds no required capability.

Keep Babashka and lychee as development tools, outside library runtime
dependencies. Select exact versions, installation steps, and job placement
during planning after checking lychee's fragment and path behaviour.

## Out of scope

- HTML-site crawling, prose lint, spelling, and documentation generation.
- Replacing existing commands with Babashka tasks.
- Release-workflow integration or scheduled link-monitoring runs.
- Branch-protection settings, issue automation, and publication.
- Runtime behaviour, library dependencies, or unrelated documentation edits.

## Acceptance criteria

1. `bb lychee:check` checks the complete document set above, with both local
   and external checks enabled, and passes on the corrected repository.
2. Real temporary fixtures prove success for valid relative paths, a path
   with spaces, repository-root paths, heading fragments, HTTP responses,
   and redirects. A local HTTP server provides repeatable web responses.
3. Missing local targets, missing local heading fragments, and an HTTP 404
   each cause a nonzero task exit with useful diagnostics. Missing tools
   and invalid configuration cannot be reported as success. Fixtures do
   not leave broken documents in the repository.
4. CI invokes the same task once, propagates failure, and retains the
   existing event and permission contracts. actionlint accepts the workflow.
5. Development documentation states tool versions, installation, command,
   document scope, network requirements, and exception policy. Any initial
   exceptions are narrow and explain why the link cannot be checked.
6. The implementation log records versions, commands, exit codes, fixture
   evidence, and the live external-link result. Record
   hosted CI evidence if available; otherwise mark it unverified. Do not
   push solely to obtain that evidence.
