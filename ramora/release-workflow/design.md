# Design

## Purpose and context

Publish one source JAR/POM with matching changelog notes on Clojars and GitHub.
Validate credentials, build, call stock deps-deploy, and return artifact details.
Publish one artifact only. Use manual dispatch, not tag-triggered publication.

## Ownership

| Component | Responsibility |
|---|---|
| Maintainer | Notes, local preparation, separate push, dispatch, remote inspection |
| Local prepare command | Stamp notes and commit only the changelog; no remote writes |
| build.clj | Version, source JAR/POM, credential validation, stock Clojars deployment |
| CI workflow | All checks at the dispatch SHA, called by the release workflow |
| Release job | Check out dispatch SHA after CI passes, gate writes, run normal commands |
| Git | Create and push a tag without force |
| gh | Create Release and upload the deployed JAR/POM with prepared notes |
| External services | Own published state and enforce their normal conflict rules |

Keep notes logic separate from build tooling. Use small functions and ordinary
workflow steps. No cross-job bundle, custom client, process framework, remote
state classifier, or alternative Maven transport is needed.

## Accepted trade-offs

- One full-history checkout at the master dispatch SHA. There is no alternate
  target input or separately trusted control checkout.
- The release workflow calls CI instead of keeping a second check list. A
  separate check list can drift from CI and let a release pass when CI fails.
- Only the release job has contents-write permission. CI jobs are read-only
  and get no secrets. Publication conditions and step-scoped credentials
  protect dry runs.
- Deploy builds from the checked source, as scry does. Tests need not establish
  byte identity with an earlier fixture or preview build. GitHub receives the
  actual deployment outputs, without another rebuild.
- Use stock deps-deploy and runner-provided gh. Internal retries, redirects,
  checksum handling, and draft cleanup are accepted. Record gh version; source
  audits and transport overrides are not release acceptance work.
- Ordinary command failures stop later steps. No atomicity, exactly-once, or
  cross-service absence guarantee. Recovery and independent remote inspection
  belong to the maintainer.
