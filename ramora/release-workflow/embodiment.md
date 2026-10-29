# Embodiment

## Artifacts

| Path | Responsibility |
|---|---|
| `build.clj` | Source-only JAR/POM packaging and credential-checked stock deps-deploy call |
| `deps.edn` | Build/deploy dependencies and release aliases, outside runtime dependencies |
| `dev/release/closed_record/notes.clj` | Changelog validation, stamping, and note selection |
| `dev/release/closed_record/release.clj` | Local preparation and notes export commands |
| `test/release/`, `test/build/` | Notes, preparation, credential, and packaging verification |
| `.github/workflows/release.yml` | Manual release: master guard, called CI, and one release job with default dry run and explicit-false publication gates |
| `.github/workflows/ci.yml` | Read-only CI for pushes and pull requests; called by the release workflow through `workflow_call` |
| `CHANGELOG.md` | Maintained release notes |
| `doc/releases.md` | Maintainer commands and failure inspection |

The release job uses one full-history checkout at the dispatch SHA. Publication
steps create the Git tag, deploy the JAR/POM, and pass those deployment outputs
to `gh release create`. Notes remain outside the build output directory.

## Verification limits

Local Java 21 checks cover format/lint, the three Clojure runtime suites,
focused build/release suites, packaging, and workflow syntax. See the retained
[acceptance evidence](../../munera/closed/016-simple-release-workflow/implementation.md).
These checks do not verify hosted permissions or successful publication on
Clojars and GitHub. Hosted publication remains unverified.
