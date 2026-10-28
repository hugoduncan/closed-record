# Release Workflow Design

The release path uses a simple build-and-deploy pattern, with manual dispatch
and normal GitHub CLI publication.

- [Design](release-workflow/design.md): ownership and accepted trade-offs.
- [Contracts and flow](release-workflow/contracts-and-flow.md): preparation,
  verification, dry run, and publication.
- [Verification and operation](release-workflow/verification-and-operation.md):
  acceptance, maintainer procedure, and failure handling.
- [Embodiment](release-workflow/embodiment.md): current artifacts and verification
  limits.

Use one job and one checkout. Build tooling calls stock deps-deploy. Git creates
the tag; gh creates the Release. Do not implement a publication framework or
replace library transports. Tool-internal retries and cleanup are accepted.
