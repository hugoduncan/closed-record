# Design

## Purpose and context

Publish one source JAR/POM with matching changelog notes on Clojars and GitHub.
Follow `~/projects/hugoduncan/scry/scry-master/build.clj`: credentials, build,
stock deps-deploy, artifact details. Do not copy its second artifact or automatic
tag-triggered publication. [Task 016](../../munera/closed/016-simple-release-workflow/design.md)
is authoritative for scope and acceptance.

## Ownership

| Component | Responsibility |
|---|---|
| Maintainer | Notes, local preparation, separate push, dispatch, remote inspection |
| Local prepare command | Stamp notes and commit only the changelog; no remote writes |
| build.clj | Version, source JAR/POM, credential validation, stock Clojars deployment |
| One workflow job | Check out dispatch SHA, run checks, gate writes, run normal commands |
| Git | Create and push a tag without force |
| gh | Create Release and upload the deployed JAR/POM with prepared notes |
| External services | Own published state and enforce their normal conflict rules |

Keep notes logic separate from build tooling. Use small functions and ordinary
workflow steps. No cross-job bundle, custom client, process framework, remote
state classifier, or alternative Maven transport is needed.

## Accepted trade-offs

- One full-history checkout at the master dispatch SHA. There is no alternate
  target input or separately trusted control checkout.
- One job has contents-write permission. Publication conditions and step-scoped
  credentials protect dry runs; separate read/write jobs are not required.
- Deploy builds from the checked source, as scry does. Tests need not establish
  byte identity with an earlier fixture or preview build. GitHub receives the
  actual deployment outputs, without another rebuild.
- Use stock deps-deploy and runner-provided gh. Internal retries, redirects,
  checksum handling, and draft cleanup are accepted. Record gh version; source
  audits and transport overrides are not release acceptance work.
- Ordinary command failures stop later steps. No atomicity, exactly-once, or
  cross-service absence guarantee. Recovery and independent remote inspection
  belong to the maintainer.

## Artifact map

| Path | Responsibility |
|---|---|
| build.clj | Packaging and simple deploy entry point |
| deps.edn | Pinned build/deploy dependencies outside runtime; release aliases |
| dev/release | Notes, preparation, and only the small helpers they need |
| test-release, test-build | Local policy, preparation, and packaging verification |
| .github/workflows/release.yml | Manual one-job release |
| .github/workflows/ci.yml | Include release tooling in ordinary quality/test coverage |
| CHANGELOG.md | Maintained release notes |
| README.md | Exact maintainer commands and failure inspection |

Task-015 implementation, including preparation, has been reverted to the pre-015
baseline. The replacement uses only notes and release namespaces, without the old module hierarchy.
Keep the closed task history for reference. Runtime source and dependencies
remain unchanged.
