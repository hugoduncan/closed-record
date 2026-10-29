# State

Working memory. Read this file first in each session. It records only the
current project state. Update statements in place; do not append history.

- Tasks and task progress: Munera (`munera/plan.md`).
- Design and implementation limits: Ramora (`ramora/INDEX.md`).
- History: Git.

## Project

closed-record: a `defrecord`-like record that rejects `assoc` of undeclared
keys and `dissoc` of declared keys. Main form: `defclosedrecord`.

## Current state

- Not released. `CHANGELOG.md` has only an Unreleased section; no Git tags.
- Verification limits (hosted CI, publication, benchmark scope) are in each
  design's `embodiment.md`.

## Where to look

- Commands (tests, REPL, benchmarks, build): `doc/development.md`.
- Tool installation and pins: `doc/tool-installation.md`.
- Quality checks: `doc/quality-checks.md`.
- Releases: `doc/releases.md`.
- Performance results: `doc/performance.md`.
- Public API: `doc/API.md` (generated).
- Reusable lessons: `mementum/knowledge/`, `mementum/memories/`.

## Local environment

- Default Java is 25. CI uses Temurin 21. For Java 21 checks, set `JAVA_HOME`
  to `/Users/duncan/.local/share/mise/installs/java/temurin-21` and prepend
  `$JAVA_HOME/bin` to `PATH`.
