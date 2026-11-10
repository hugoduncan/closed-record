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

- Released: `org.hugoduncan/closed-record` `0.1.190` on Clojars, Git tag
  `v0.1.190`, and a GitHub Release. This is the first release.
- The release workflow calls CI (`workflow_call`), so a CI failure stops a
  release. `doc/releases.md` has the steps.
- Unreleased: the `:value-type true` option (task 020, branch
  `020-value-type-records`, pull request #1). Design:
  `ramora/value-type-records.md`. Tests need the `:value-type-test` alias;
  CI has a Temurin 21 and a Valhalla value-type job. `CHANGELOG.md`
  `## [Unreleased]` has a note for the option.
- Unreleased: non-nil fields for value-type records (task 021, same
  branch). `:non-nil` field metadata passes to `defvaluetype*` unchanged
  (VT-D11); ordinary mode ignores it (VT-D12). The `:value-type-test` pin
  is value-type `edc3f79`.
- Closed-record value records are not flat in `value-type.array` on
  Valhalla `27-jep401ea3+1-1` (hidden `__meta`/`__extmap` fields). The
  README and `doc/value-type-records.md` say this.
- Designed, not implemented: closed shape for value records (VT-D13 to
  VT-D15): no hidden fields on value output, no metadata, map literals with
  undeclared keys throw. Task 022, ready for a plan. value-type `4ed8d0a3`
  provides the closed shape (`:value-type/closed true`); the task design was
  checked against it, and the current suites pass with that pin. The Ramora
  embodiment lists the current differences.
- Verification limits (hosted CI, publication, benchmark scope) are in each
  design's `embodiment.md`.

## Where to look

- Commands (tests, REPL, benchmarks, build): `doc/development.md`.
- Tool installation and pins: `doc/tool-installation.md`.
- Quality checks: `doc/quality-checks.md`.
- Releases: `doc/releases.md`.
- Performance results: `doc/performance.md`.
- Public API: `doc/API.md` (generated).
- Value-type record details: `doc/value-type-records.md`. The README has an
  introduction only.
- Reusable lessons: `mementum/knowledge/`, `mementum/memories/`.

## Local environment

- Default Java is 25. CI uses Temurin 21. For Java 21 checks, set `JAVA_HOME`
  to `/Users/duncan/.local/share/mise/installs/java/temurin-21` and prepend
  `$JAVA_HOME/bin` to `PATH`.
