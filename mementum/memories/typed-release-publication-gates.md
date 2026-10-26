🎯 Require explicit boolean false before release writes

The manual release workflow defaults to dry run. Each publication step uses
`toJSON(inputs['dry-run']) == 'false'`, not a loose boolean comparison or
negation. Serialization distinguishes boolean false from missing/null values
and the string "false"; only the boolean permits writes.

Keep normal success gating, so an earlier failed check stops publication.
Scope credentials to the publication steps that need them and disable checkout
credential persistence. The single job has contents-write permission: step
gates and secret placement are the chosen boundary, not job-level read-only
isolation. The workflow trusts maintainer-controlled master code.

Use this rule when adding or changing release steps. Do not infer hosted
permission or publication success from local workflow inspection.

Evidence: task 016 design and implementation log, slice 3; commit `c1112e4`.
Related: `.github/workflows/release.yml`, `ramora/release-workflow.md`.
