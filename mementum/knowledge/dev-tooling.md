---
title: Development tooling rules (deps aliases, scry, psi REPL)
status: active
category: tooling
tags: [deps.edn, aliases, bench, scry, nrepl, psi, clj-kondo, gitignore]
related: [README.md, mementum/state.md, mementum/knowledge/benchmarks.md, .psi/project.edn, .psi/skills/scry/SKILL.md, mementum/knowledge/testing-generated-types.md, mementum/knowledge/artifact-verification.md]
depends-on: [deps.edn]
---

💡 Rules for changes to `deps.edn`, tests and the project REPL. The commands are in `README.md` "Development".
This page gives the rules that the commands do not show.

## Alias composition

- 🎯 The `:test` alias has no `:main-opts`. Then `-M:test:nrepl` works.
  Do not add `:main-opts` to an alias that must compose with `:nrepl`.
- `:bench` has `:main-opts` (`clojure -M:bench` runs the bench). It is a standalone alias, not for composition.
  To use its classpath with `-e` or a REPL, clear `:main-opts` with an extra alias. See
  `mementum/knowledge/benchmarks.md`.
- `:test` has the extra path `dev/bench`, so the suite tests the bench pure layer and fork layer. criterium is not
  in `:test`.
- 🎯 Version aliases (`:clj-1.12`, `:clj-1.13`) use `:override-deps`. They compose with `:test` and `:nrepl`.
  CI uses these aliases. Do not add a separate workflow version override.
- Pin all versions. Do not use `RELEASE`.
- A top-level `org.clojure/clojure` has priority over the transitive Clojure of scry. Check the version with
  `clojure -M:<aliases> -e '(print (clojure-version))'`.
- ❌ A command in a scratch directory outside the repository does not read the repository `deps.edn`.
  `clojure -Sdeps "{:paths [...]}"` then uses the Clojure version of the root deps (`~/.clojure/deps.edn` or the
  CLI default, 1.12.6 here), not 1.11.4. Add `:deps {org.clojure/clojure {:mvn/version "1.11.4"}}` to the
  `-Sdeps` map and print `(clojure-version)` with the evidence (task 012, found in plan review).

## Build tooling runtime

- ❌ tools.build 0.10.10 `b/process` requires `clojure.java.process`, which is
  absent from Clojure 1.11. Use `clojure.java.shell/sh` for test subprocesses
  that must run on the project's base runtime. A successful `-T:build` command
  does not prove that the same process API works inside `-M:test:build-test`:
  `-T:build` has a separate tooling runtime (task 013, slice 2a).
- Keep artifact verification separate from the default runtime suite. The
  pinned scry supports `--dir test-build`; its default discovery is under
  `test`. See `mementum/knowledge/artifact-verification.md` for fixture and
  consumer isolation rules.

## Continuous integration

- `.github/workflows/ci.yml` owns setup and orchestration for pushes to
  `master` and all pull-request targets. It uses Ubuntu, Temurin 21,
  read-only contents access, and no stored checkout credentials.
- Pinned tools: Clojure CLI 1.12.6.1673, cljfmt 0.16.4, clj-kondo 2026.04.15.
  Quality commands: `cljfmt check src test dev test-build build.clj` and
  `clj-kondo --lint src test dev test-build build.clj --fail-level warning`.
- The runtime matrix uses the three README test commands, with fail-fast
  disabled. `deps.edn` owns runtime versions, not the CLI setup version.
- Build verification runs once outside the matrix: focused `test-build`
  tests, then `clojure -T:build jar`, with full history. Keep real temporary
  project and isolated-consumer checks. Performance benchmarks remain manual.
- Failed test steps upload hidden `.scry-results/` files under distinct
  `scry-runtime-<version>` and `scry-build` names. Missing files warn without
  clearing the test failure. A JAR-only failure does not upload test results.
- Local validation is not proof of hosted Ubuntu or fork-event execution.

## Scry

- ❌ The scry CLI exits non-zero when it finds zero tests.
- ❌ A test with no assertions counts as "unknown". The CLI then exits 1. Every test needs an assertion.
- In a CLI test, `*ns*` is `user` and `Compiler/LOADER` is not bound. A REPL run can hide this. See
  `mementum/knowledge/testing-generated-types.md`.
- On a pass, `.scry-results/` is empty. On a failure, read `.scry-results/*.edn`, not the terminal output.

## psi project REPL

- 💡 psi reads only `<worktree>/.psi/project.edn` (and `.psi/project.local.edn`). It does not read a root
  `project.edn`. The start command is at `:agent-session :project-nrepl :start-command`.
- The start command in `.psi/project.edn` and in `README.md` must be the same: `clojure -M:test:nrepl`.
- psi reads the port from `.nrepl-port` in the repository root. Start the REPL from the root.
- Keep `:session-profiles` and the `#_` forms in `.psi/project.edn`. The task-lifecycle workflows use them.
- ❌ `psi-tool` is an LLM-callable tool of the psi harness. It is not a shell command. Do not look for it on PATH.
  Verify the REPL with the tool: `psi-tool` action `project-repl`, op `start`, then op `eval` with
  `(require '[scry.core :as scry]) (:pass? (scry/run))`. Op `stop` stops the REPL.

## Git ignore checks

- ❌ On this machine `~/.gitignore_global` ignores `.cpcache/`. A clean local `git status` does not prove that the
  repository ignores a file. Read `.gitignore`. Entries are root-anchored (`/.cpcache/`).
