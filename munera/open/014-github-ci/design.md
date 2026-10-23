# 014 GitHub CI

## Intent

Check code quality and behaviour on every push to `master` and every pull
request. Give maintainers a clear failure signal before changes merge.

## Context

The project has no GitHub Actions workflow. Tests, lint, formatting checks,
and JAR verification currently run locally. D-16 in
`ramora/defclosedrecord/decisions/verification.md` still defers CI.

The reference is `~/projects/hugoduncan/scry/scry-master/.github/workflows/ci.yml`,
with its `bb.edn` lint and formatting commands. It uses Ubuntu, Temurin 21,
Clojure CLI, formatting, lint, tests, and a JAR build. Its prose checks,
API documentation, adapter tests, and release workflow do not apply here.

The user confirmed Temurin 21, cljfmt, and focused build tests plus a JAR
build once outside the Clojure-version matrix.

## Scope and contracts

### Events and execution environment

One GitHub Actions CI workflow runs on pushes to `master` and pull requests
against any branch, including PRs from forks. Do not use path filters or
`pull_request_target`. Use the standard pull-request event activity types.

Run on `ubuntu-latest` with Temurin Java 21 and Clojure CLI. Use explicit
versions for installed Clojure CLI, clj-kondo, and cljfmt tools, and explicit
action version references. Select and record these versions during planning.
Do not add mise or Babashka solely to wrap commands that can run directly.

Grant only read access to repository contents. Checks need no repository
secrets, write token, or deployment credentials. Do not enable error suppression
or allow a failing matrix entry to count as a pass. Give jobs finite timeouts.

### Code quality

Check formatting with cljfmt and lint with clj-kondo over all project Clojure
code: `src`, `test`, `dev`, `test-build`, and `build.clj`. A formatting difference,
lint warning, or lint error fails its job. Checks must not rewrite files.
Use the existing clj-kondo macro configuration.

If the checks expose existing issues, include only the formatting or narrow
lint corrections needed to make this baseline pass. Do not suppress lint
categories globally or change runtime behaviour to satisfy a check. A larger
behavioural change needs a separate task.

### Runtime tests

Run the complete default test suite in three matrix entries, all on Java 21:

| Clojure | Command |
| --- | --- |
| 1.11 | `clojure -M:test -m scry.cli` |
| 1.12 | `clojure -M:test:clj-1.12 -m scry.cli` |
| 1.13 | `clojure -M:test:clj-1.13 -m scry.cli` |

`deps.edn` owns Clojure versions. Use its existing aliases rather than a
separate version override in the workflow. Do not change these versions as
part of this task. Disable matrix fail-fast so that one failure does not
cancel evidence from the other versions.

The default suite includes tests of benchmark support code. Do not run the
performance benchmark command `clojure -M:bench` in CI.

### Build verification

Run one build-verification job, separate from the runtime matrix:

- `clojure -M:test:build-test -m scry.cli --dir test-build`.
- `clojure -T:build jar`.

Use a full-history checkout for the actual JAR build because its version uses
the Git commit count. The focused build tests retain their temporary-project
and isolated-consumer checks. Do not replace them with a source-checkout test.

No artifact is installed, published, or deployed. Uploading the JAR is not
required.

### Failure evidence and documentation

Command exit codes determine success. Preserve scry's structured failure
files from `.scry-results/` as CI artifacts when a test command fails, with
names that distinguish the matrix entries and build tests. Missing result
files must not hide the original command failure.

Document triggers, environment, checks, and matching local commands in the
README. Update D-16 and related tooling documentation to remove the stale
claim that all verification is manual. Benchmarks remain manual. Record the
workflow artifact and its responsibility in the architecture artifact map.

## Design choices

Use standard GitHub Actions jobs and the existing CLI commands. Separate
code quality, the runtime matrix, and build verification so that failures
have clear names and build tests are not repeated for each Clojure version.
No general task runner or reusable workflow framework is needed.

The workflow owns orchestration and tool setup. Existing aliases and test
suites own dependency versions and behavioural checks. Repository branch
protection is separate from the workflow and is not changed by this task.

## Out of scope

- Additional JDKs, operating systems, or Clojure versions.
- Performance benchmarks, prose lint, and API documentation generation.
- Releases, deployment, signing, tags, and credentials.
- Branch-protection settings or required-check configuration.
- A new task runner, runtime API changes, or broad cleanup.

## Acceptance criteria

1. Workflow inspection confirms pushes to `master`, all PR targets, fork-safe
   read-only permissions, Java 21 on Ubuntu, finite timeouts, and no path
   filters, privileged PR events, secrets, or publication steps.
2. cljfmt and clj-kondo cover all listed paths. Both pass on the final code;
   formatting differences and lint warnings cause nonzero exits.
3. The runtime matrix runs the three exact alias-based commands, without
   fail-fast or error suppression. All three suites pass.
4. Focused build tests and the JAR command pass once outside the matrix. The
   JAR build has full Git history. No performance benchmark runs.
5. Test failure uploads preserve `.scry-results/` when present and use distinct
   artifact names. A failed test still fails its job.
6. actionlint accepts the workflow. `implementation.md` records tool versions,
   validation commands, exit codes, and structured test results. Record a
   hosted Actions run if available; if publishing the branch is not authorized,
   explicitly record hosted event execution as unverified rather than claim
   it ran. Do not push solely to obtain acceptance evidence.
7. README, D-16, the architecture artifact map, and development-tooling
   documentation describe the implemented CI scope. Runtime behaviour and
   dependency versions are unchanged.
