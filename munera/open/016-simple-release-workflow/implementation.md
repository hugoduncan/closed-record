# Implementation notes

## 2026-10-23 — Scope and plan

- User requested scope correction to the scry build/deploy pattern. Created this
  replacement task under the Munera scope-change rule. Task 015 is closed without
  completion; its code and historical evidence remain available in Git.
- Chose stock deps-deploy, ordinary gh Release creation, one job/checkout, and
  step-scoped secrets. Accepted tool-internal retries, redirects, checksum
  handling, and temporary draft cleanup. Deployment may rebuild, as scry does.
- Removed target selection, cross-job bundles, remote-absence proof, custom
  transport controls, and dependency audits from acceptance. The plan requires
  removal of their unused implementation rather than keeping dormant alternatives.
- Retained manual master dispatch, default dry run, local changelog preparation,
  runtime/build/quality checks, Git-count versioning, and manual failure recovery.
- Source reference: scry-master/build.clj and .github/workflows/release.yml.
  The reference has tag-triggered publication and two artifacts; neither is copied.
- This pass changes task/architecture documents and working memory only. Validation:
  local Markdown links and git diff --check. No code tests or hosted checks ran;
  no credentials were configured and no release was published.

## 2026-10-23 — Full task-015 implementation revert

- User requested reverting all existing 015 work and replacing it with 016.
  Restored deps.edn and packaging embodiment from pre-015 commit `c9066bd`.
  Removed all 015 release/deploy source, tests, and CHANGELOG.md, including notes
  and preparation. Kept closed task records and Git history, not its code.
- Updated this design, plan, checklist, architecture, and working memory to start
  from that baseline. Do not retain or restore the old module hierarchy.
- Baseline comparison: deps.edn, build.clj, src, test, test-build, .github, README,
  and packaging embodiment match `c9066bd`. Only replacement/historical task
  documentation and release architecture remain different outside those paths.
- Java 21: `clojure -M:test -m scry.cli` passed (97 tests, 379 assertions);
  `clojure -M:test:build-test -m scry.cli --dir test-build` passed (3 tests,
  71 assertions). Both exited 0 with no scry failure files. Markdown link and
  whitespace checks passed. No remote writes or publication.
- Next: implement the small 016 build/deploy, notes, workflow, and docs from this
  baseline. The revert is complete; the replacement implementation is not.

## 2026-10-23 — Architecture review

- No architectural review feedback. Scope unchanged; no new design steps.
- `ramora/META.md` and `doc/architecture.md` are absent. Review used `AGENTS.md`,
  the release architecture linked from `ramora/INDEX.md`, and the library's
  principles and embodiment. The release architecture is intended; the packaging
  embodiment describes the pre-release baseline, not a ban on deployment.

## 2026-10-23 — Ambiguity review

- Ambiguity review added 1 new design step: define the changelog contract.
  Scope unchanged.

## 2026-10-23 — Inconsistency review

- No inconsistency review feedback. Scope unchanged; no new design steps.

## 2026-10-23 — Design follow-up handoff

- `ramora/release-workflow/design.md` assigns notes logic outside build tooling.
  Keep changelog validation in notes support, not in the packaging/deployment
  interface.
- `ramora/release-workflow/contracts-and-flow.md` describes both preparation and
  note selection. Check it against the resolved contract so both entry points
  use the same rules; do not restore task-015 parser rules by default.

## 2026-10-23 — Changelog contract follow-up

- Checked the contract against release architecture preparation and selection;
  no architecture conflict found. No scope-question items were present.
- Release Git fixtures must select `master` explicitly. Existing
  `test-build/build_test.clj` uses plain `git init`; its private fixture config
  disables hooks/signing and isolates Git settings but sets no default branch.
- Validation: Markdown links and `git diff --check`. Design-only change; no
  runtime tests, hosted checks, or publication.

## 2026-10-23 — Ambiguity review after changelog contract

- No new ambiguity review feedback. Scope unchanged; no new design steps.

## 2026-10-23 — Review reference check

- In `~/projects/hugoduncan/scry/scry-master/build.clj`, `deploy` is the
  single-artifact reference: it returns the artifact map from `jar` after
  deployment. Use that entry point as the reference, not `deploy-all`, which
  builds and deploys both artifacts.
