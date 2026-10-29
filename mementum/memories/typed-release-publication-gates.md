🎯 Require explicit boolean false before release writes

The manual release workflow defaults to dry run. Each publication step uses
`toJSON(inputs['dry-run']) == 'false'`, not a loose boolean comparison or
negation. Serialization distinguishes boolean false from missing/null values
and the string "false"; only the boolean permits writes.

Keep normal success gating, so a failed CI job stops publication.
Scope credentials to the publication steps that need them and disable checkout
credential persistence. Only the release job has contents-write permission;
the called CI jobs are read-only. Within the release job, step gates and
secret placement are the boundary. The workflow trusts maintainer-controlled master code.

Use this rule when adding or changing release steps. Do not infer hosted
permission or publication success from local workflow inspection.

Evidence: task 016 design and implementation log, slice 3; commit `c1112e4`.
Related: `.github/workflows/release.yml`, `ramora/release-workflow.md`.
