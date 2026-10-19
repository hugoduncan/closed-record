# 001 Project skeleton

## Intent

Make a Clojure project that later tasks can build on. A developer or an agent must be able to load the source,
run the tests and start a REPL with one command each. Scry is the test runner.

## Problem

The repository has documents only. It has no `deps.edn`, no source and no tests. The emitter, the tests (V-01 to
V-15) and the benchmarks need a classpath, a test runner and a REPL first. If each later task adds a part of the
skeleton, the tasks change the same files and use different conventions.

## Context

- Design: `ramora/defclosedrecord/`. The file layout is in
  [embodiment](../../../ramora/defclosedrecord/embodiment.md#artifacts).
- K-01 and D-14: Clojure 1.11 is the minimum version on the JVM.
- D-13: the public namespace `closed-record.core` is in a `.cljc` file.
- D-16: the verification plan runs on Clojure 1.11, 1.12 and 1.13.
- Scry: `org.hugoduncan/scry` on Clojars. Source: `~/projects/hugoduncan/scry`. The project skill is
  `.psi/skills/scry/SKILL.md`.
- psi starts the managed project REPL with the command in
  `.psi/project.edn` → `:agent-session :project-nrepl :start-command`. Psi reads the port from `.nrepl-port`.

### Observed facts (2026-10-19)

- Scry 0.1.61 is the latest release on Clojars.
- Scry 0.1.61 runs on Clojure 1.11.4. Test: a temporary project with Clojure 1.11.4 as a top-level dependency and
  scry in a `:test` alias. `clojure -M:test -m scry.cli` exited 0. `(clojure-version)` was `"1.11.4"`.
  Scry depends on Clojure 1.12.1, but the top-level dependency has priority.
- The scry CLI exits non-zero when it finds zero tests. Thus the skeleton must contain one test.
- Latest versions: Clojure 1.11.4, 1.12.6 and 1.13.0-alpha9. nREPL 1.8.0.

## Scope

In scope:

1. `deps.edn`:
   - `:paths ["src"]`.
   - Dependency `org.clojure/clojure` 1.11.4 (K-01).
   - Alias `:test`: adds the path `test` and the dependency `org.hugoduncan/scry` 0.1.61. Pin the version.
     Do not use `RELEASE`.
   - Alias `:nrepl`: adds `nrepl/nrepl` 1.8.0 and starts an nREPL server.
   - Aliases `:clj-1.12` and `:clj-1.13`: replace the Clojure version with 1.12.6 and 1.13.0-alpha9.
2. Source namespace `closed-record.core` in `src/closed_record/core.cljc`. It has a namespace docstring and no vars.
3. Test namespace `closed-record.core-test` in `test/closed_record/core_test.clj`. It has one smoke test.
   The smoke test requires `closed-record.core` and checks that the namespace is loaded.
4. `.psi/project.edn` with `:version 1` and the project REPL start command `["clojure" "-M:test:nrepl"]`.
5. `.gitignore` entries for the files that the tools make: `.cpcache/`, `.scry-results/`, `.nrepl-port`,
   `.clj-kondo/.cache/`.
6. A "Development" section in `README.md`. It gives the commands to run the tests, to run them on each Clojure
   version and to start a REPL.
7. An update of `mementum/state.md` and `munera/plan.md`.

## Out of scope

- `defclosedrecord`, `check`, the `ClosedRecord` protocol and `closed-record.impl.jvm`.
- The tests of the verification plan, V-01 to V-15.
- Benchmarks, criterium, `dev/bench/`.
- CI configuration, for example GitHub Actions.
- Build, jar, deployment, license, version number and changelog.
- clj-kondo configuration and hooks for `defclosedrecord`.
- ClojureScript and babashka.
- A `:main-opts` in the `:test` alias. The `:test` alias must also work with `:nrepl`.

## Adjacent work

Later tasks own this work. This task must not block it:

- JVM emitter and public API (`core.cljc`, `impl/jvm.clj`).
- Compiler rule tests (V-13).
- Behaviour, differential, equality, literal and constant tests.
- Benchmark alias and `dev/bench/` (V-11).
- CI matrix on Clojure 1.11, 1.12 and 1.13 (D-16). It uses the aliases from this task.
- clj-kondo hook for `defclosedrecord`.

## Decisions

- Test runner: scry, for the REPL and the CLI. Do not add `clojure.test` runner scripts or Kaocha.
- Test commands:
  - Clojure 1.11: `clojure -M:test -m scry.cli`.
  - Other versions: `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`.
  - REPL: `(require '[scry.core :as scry]) (scry/run)`.
- Version aliases: one alias for each version, not a parameter. A version alias composes with `:test` and `:nrepl`.
- The skeleton has no `dev` path and no `user` namespace. Add them when a task needs them.
- The smoke test is necessary because the scry CLI fails with zero tests. A later task can remove it when other
  tests exist.
- `.psi/project.edn` is committed. `.psi/project.local.edn` stays ignored.

## Acceptance criteria

1. `clojure -M:test -m scry.cli` exits 0. The summary shows one or more tests and zero failures. `.scry-results/` is
   empty.
2. `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli` exit 0.
3. `clojure -M -e '(print (clojure-version))'` prints `1.11.4`. With `-M:test`, it also prints `1.11.4`.
4. `clj-kondo --lint src test` reports no errors and no warnings.
5. `cljfmt check src test` passes.
6. `psi-tool` `project-repl` `start` in the worktree succeeds. In that REPL, `(:pass? (scry/run))` is `true`.
7. After the commands in 1 to 6, `git status` shows no new untracked files.
8. `README.md` has a "Development" section with the commands in Decisions. The prose obeys the project style
   (simplified technical English).
9. `mementum/state.md` shows the skeleton and the test commands. `munera/plan.md` lists this task.
10. The repository has no other source namespaces and no implementation of `defclosedrecord`.
