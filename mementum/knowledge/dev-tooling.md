---
title: Development tooling rules (deps aliases, scry, psi REPL)
status: active
category: tooling
tags: [deps.edn, aliases, scry, nrepl, psi, clj-kondo, gitignore]
related: [README.md, mementum/state.md, .psi/project.edn, .psi/skills/scry/SKILL.md]
depends-on: [deps.edn]
---

💡 Rules for changes to `deps.edn`, tests and the project REPL. The commands are in `README.md` "Development".
This page gives the rules that the commands do not show.

## Alias composition

- 🎯 The `:test` alias has no `:main-opts`. Only `:nrepl` has `:main-opts`. Then `-M:test:nrepl` works.
  Do not add `:main-opts` to an alias that must compose with `:nrepl` (for example a bench alias).
- 🎯 Version aliases (`:clj-1.12`, `:clj-1.13`) use `:override-deps`. They compose with `:test` and `:nrepl`.
  The CI matrix (D-16) must use these aliases. Do not add a version parameter.
- Pin all versions. Do not use `RELEASE`.
- A top-level `org.clojure/clojure` has priority over the transitive Clojure of scry. Check the version with
  `clojure -M:<aliases> -e '(print (clojure-version))'`.

## Scry

- ❌ The scry CLI exits non-zero when it finds zero tests.
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
