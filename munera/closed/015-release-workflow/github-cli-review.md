# GitHub CLI publication review

Closed without completion on 2026-10-23. The user requested the simpler scry
build/deploy approach. This document records the abandoned scope, not current
requirements. Do not continue its checklist or enforce its transport policies.
See [016 Simple release workflow](../../closed/016-simple-release-workflow/design.md).

2026-10-23. Read-only source review; no credentials or publication used.

## Reviewed version and source

Candidate pin: `gh` v2.102.0, also the locally installed version. This is not
an accepted publication pin.

Source archive:
`https://codeload.github.com/cli/cli/tar.gz/refs/tags/v2.102.0`

Downloaded archive SHA-256:
`08bf0ef8b4409893889175e0f5279d30d6edd96465816042c0d7c72eac598158`

Source links:

- [Release creation](https://github.com/cli/cli/blob/v2.102.0/pkg/cmd/release/create/create.go), lines 497–563.
- [Asset upload](https://github.com/cli/cli/blob/v2.102.0/pkg/cmd/release/shared/upload.go), lines 111–155.
- [Release HTTP operations](https://github.com/cli/cli/blob/v2.102.0/pkg/cmd/release/create/http.go), lines 182–212.

## Findings

With assets and without `--draft`, creation first makes a draft Release.
`ConcurrentUpload` calls `uploadWithDelete` for each asset. The upload uses a
200 ms constant backoff with up to three retries after the first attempt.
Transport errors and HTTP statuses of 500 or greater qualify for retry.
This happens within one CLI invocation; the parent process deadline and
single-invocation policy do not prevent it.

After uploads, `publishRelease` sends PATCH to publish the draft. An upload
failure or final publication failure calls `cleanupDraftRelease`, which sends
DELETE for that Release. Thus a failed invocation can delete its draft and
assets, not merely leave partial state for manual recovery.

The retry count and cleanup path in these functions are not controlled by a
command flag. `--draft` avoids this cleanup path but still retries uploads and
does not produce the required published Release. Omitting assets does not meet
the required command contract. The source for v2.65.0 and v2.40.0 has the same
retry and draft-cleanup paths; choosing either older pin does not resolve this.
This review does not claim to cover every historical CLI version.

## Consequence

Do not connect the current `github-publish/publish!` adapter to publication.
Its lack of a parent retry loop is not a no-write-retry guarantee. The required
`gh release create` asset command conflicts with the no-retry/no-deletion
contract. Keep the design contract unchanged until the maintainer decides.

Preferred decision: permit separate `gh api` create/upload commands instead of
requiring `gh release create` with asset arguments. That remains GitHub CLI use,
not a custom HTTP client, but needs a new bounded request/response adapter,
transport review, and failure tests before acceptance. Alternatively, explicitly
permit the CLI's internal retries and draft cleanup. Do not infer that permission.

Verification: downloaded and inspected the tagged source and compared the two
older tags through read-only HTTPS requests. No live GitHub API behavior,
permissions, upload, or failure handling was tested. No executable source changed,
so Clojure suites were not rerun for this review-only slice.
