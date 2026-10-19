# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

## Design review (2026-10-19)

- Architecture sources: `ramora/META.md` and `doc/architecture.md` do not exist. The architecture is
  `ramora/INDEX.md` → `ramora/defclosedrecord.md` and `ramora/defclosedrecord/*` (embodiment, D-13, D-14, D-16, K-01, K-02).
- Fit checked: the layout matches the embodiment artifacts. `core.cljc` agrees with D-13. Clojure 1.11.4 agrees with K-01 and D-14.
  The version aliases supply the D-16 matrix. The `dev/bench/` and bench alias stay with the adjacent tasks.
- no architectural review feedback
- ambiguity review added 3 new design steps
- inconsistency review added 1 new design step
  - psi reads only `<worktree>/.psi/project.edn` (psi `shared_config/project.clj`, `project_nrepl/ops.clj`).
    The root `project.edn` is not read. A move or merge of it is outside the frozen scope.

## Notes for the design-steps fix

- Principles: edit `design.md` in place (munera: no amendment files, no revision markers). Keep the scope frozen.
  Resolve each step by a clarification or a recorded fact, not by a new in-scope or out-of-scope item.
  Prose in simplified technical English (`AGENTS.md`).
- Version aliases: in tools.deps, `:extra-deps` and `:override-deps` can both replace `org.clojure/clojure`.
  Make the version check in AC 2 the same form as AC 3.
- REPL: the start command in README and in `.psi/project.edn` must be the same. `.nrepl-port` must stay in the
  repository root, because psi reads the port there.
- `munera/plan.md`: it lists this task already. Munera removes the entry when the task moves to `closed/`.
- Files: root `project.edn` (do not change), `.gitignore`, `.psi/skills/scry/SKILL.md` (CLI exit and
  `.scry-results/` behaviour), `mementum/state.md`, `munera/plan.md`.
- Tools: `clj-kondo` and `cljfmt` are in `~/.local/bin`. `clojure` is in `/opt/homebrew/bin`.

## Design-steps fix (2026-10-19)

- All 4 design steps resolved in `design.md`. No `SCOPE_QUESTION:` items.
- Verified in a temporary project: `:override-deps` aliases give `1.12.6` and `1.13.0-alpha9` alone and with `:test`;
  base and `:test` give `1.11.4`. `clojure -M:test:nrepl` with `:main-opts ["-m" "nrepl.cmdline"]` prints
  `nREPL server started on port …` and writes `.nrepl-port` in the current directory.
- `plan.md` already uses `:override-deps`. It agrees with `design.md`.

## Design review, pass 2 (2026-10-19)

- no architectural review feedback
- no ambiguity review feedback
- no inconsistency review feedback

## Notes after design review, pass 2

- No open design steps. The design is stable. Next step: plan review (`plan.md` and `steps.md` exist).
- Not filed, do not raise again: Intent says "load the source" with one command. The REPL and test commands load it.
- `.cpcache/` is already ignored by the global `~/.gitignore_global` on this machine. Keep the repository entry
  (Scope 5). Do not use local `git status` alone as proof that the repository ignores it.
