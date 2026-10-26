# Implementation steps

This is a new acceptance checklist. Start from the pre-015 implementation
baseline; do not reuse its module hierarchy. Do not resume task 015. No real publication for acceptance.

## 1. Simple tooling

- [x] Restore pre-015 code/dependencies and packaging docs (`c9066bd`); remove all task-015 release/deployment source, tests, and changelog. Preserve closed task history and the 016 design.
- [ ] Add scry-style credential validation and stock deps-deploy deployment in build.clj; return JAR/POM/version details and keep runtime dependencies unchanged.
- [ ] Implement small local preparation and notes extraction with a maintained changelog; test invalid notes and credential checks without publication.
- [ ] Run new release and existing build tests, format/lint, and removed-reference checks; record results and commit.

## 2. Workflow

- [ ] Add one-job manual master-only workflow with default dry run, explicit-false publication, full-history dispatch-SHA checkout, timeout, and non-cancelling concurrency.
- [ ] Run all required checks before publication; preserve scry failure evidence and add new release tooling to ordinary CI coverage.
- [ ] Add dry-run build and notes summary with no Clojars secrets or publication writes.
- [ ] Connect credential validation, ordinary Git tag push, stock build/deploy, and gh Release creation; stop on command failure, with no custom retry/rollback or preflight layer.
- [ ] Check token scope, non-persistent checkout credentials, explicit repository, existing-tag requirement, and exact post-deploy asset paths. Record gh version at runtime.
- [ ] Run actionlint and inspect gates/sequence; record local evidence and hosted limits; commit.

## 3. Documentation and acceptance

- [ ] Document preparation, separate push, preview, publication, credential setup, accepted stock-tool behaviour, and manual partial-failure inspection.
- [ ] Align architecture and artifact paths with the simplified implementation; remove stale active requirements and commands.
- [ ] Run Java 21 quality, three runtime suites, build/release tests, and actionlint; inspect scry results and record commands.
- [ ] Check all six acceptance criteria, runtime artifact isolation, and absence of unused task-015 infrastructure. Commit; do not claim hosted success or publish for acceptance.
