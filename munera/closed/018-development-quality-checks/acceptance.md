# Acceptance evidence

All seven criteria are satisfied. Detailed commands and results are in
[implementation.md](implementation.md). Hosted GitHub Actions execution is not
verified. Linux results use Ubuntu 24.04 x86-64 under QEMU in Podman on macOS
arm64, not a native x86-64 host.

| Criterion | Evidence |
| --- | --- |
| 1. Reproducible setup | Fresh Linux verification entry: documented archive installations, isolated Python runner, empty initial user configuration and caches, cold dependency imports, shared checks, and focused tests pass. Earlier installation entry covers macOS arm64. |
| 2. Format and lint | Scope, command, and macro tests use real tools. They cover defects, warning-only findings, errors, explicit repair, missing scope, valid bindings, and unresolved method symbols. Final local and Linux tooling suites pass. |
| 3. Vale | Pure result tests reject malformed findings and unknown severity. Real process and owned-policy tests cover suggestions, warnings, errors, missing inputs/tools/styles, setup failure, and narrow code/lambda exclusions. Both final suites pass. |
| 4. API | Real Quickdoc and process tests cover JVM selection, exact API content, deterministic bytes, missing/stale output, invalid arguments, and repair. The controlled content-defect probe proves freshness does not replace content checks. Both final suites pass. |
| 5. Hooks and automation | Installed-hook Git fixtures cover clean commits, hidden staged defects, repairs, manual staging, partial staging, conflict rollback, and unrelated-file preservation. Check-only fixtures compare tracked bytes and indexes. Actionlint accepts both workflows; final Linux checks also preserve tracked bytes and index trees. |
| 6. Regression boundaries | Runtime, packaging, and link acceptance entry records all three runtime suites, build/release suites, link fixtures, live links, source-only JAR inspection, and unchanged base dependencies. No release behavior or publication gate changed. |
| 7. Documentation | Development links to pinned installation and quality guides. Guides cover commands, scopes, generated output, tests, and hook recovery. Ramora describes ownership and links to contributor instructions. Vale and API freshness pass. |

No new production behavior was added in final verification. Existing real-tool
and state-based tests cover the task; no mocks were added. Publication and
hosted workflow execution remain outside this local acceptance evidence.
