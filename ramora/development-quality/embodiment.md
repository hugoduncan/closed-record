# Embodiment

## Artifacts

| Path | Responsibility |
|---|---|
| `bb.edn` | Shared quality commands and internal staged-content tasks |
| `dev/quality/closed_record/` | Scope validation, tool setup, format/lint, prose checks, API generation, and hook helpers |
| `test/tooling/` | Scope contracts and real-tool, real-Git, and API tests |
| `.cljfmt.edn`, `.clj-kondo/config.edn` | Owned format and analysis configuration |
| `.vale.ini`, `styles/config/vocabularies/ClosedRecord/` | Prose policy and project vocabulary |
| `doc/API.md` | Generated API reference with separate freshness and content checks |
| `.pre-commit-config.yaml` | Serial formatting followed by whole-index lint under staged-content isolation |
| `.github/workflows/ci.yml` | Shared quality commands and focused tooling tests |
| `.github/workflows/release.yml` | Shared format/lint checks before publication |

See the [contributor guide](../../README/quality-checks.md) for commands and the
[library artifact map](../defclosedrecord/embodiment/artifacts.md) for dependency
and packaging boundaries.

## Verification limits

Local checks and real-tool fixtures cover the quality boundaries. Fresh Linux
installation evidence uses Ubuntu 24.04 under QEMU/Podman on macOS arm64; it is
not hosted GitHub Actions evidence. Hosted execution remains unverified. See
[retained acceptance and review evidence](../../munera/closed/018-development-quality-checks/implementation.md).
