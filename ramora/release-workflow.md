# Release Workflow Design

Status: implemented; locally verified. Hosted publication is not verified. [Task 016](../munera/open/016-simple-release-workflow/design.md)
owns scope and acceptance. The release path follows scry's build.clj deployment
pattern, with manual dispatch and normal GitHub CLI publication.

- [Design](release-workflow/design.md): ownership and accepted trade-offs.
- [Contracts and flow](release-workflow/contracts-and-flow.md): preparation,
  verification, dry run, and publication.
- [Verification and operation](release-workflow/verification-and-operation.md):
  acceptance, maintainer procedure, and failure handling.

Use one job and one checkout. Build tooling calls stock deps-deploy. Git creates
the tag; gh creates the Release. Do not implement a publication framework or
replace library transports. Tool-internal retries and cleanup are accepted.

[Task 015](../munera/closed/015-release-workflow/design.md) is closed without
completion. Its strict transport and bundle contracts are historical, not active
architecture. Task-015 implementation has been reverted; task 016 starts from
the pre-015 code baseline. No hosted
release is established by this design.
