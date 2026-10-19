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

## Design review, pass 3 (2026-10-19)

- no architectural review feedback
  - `design.md` has not changed since pass 2. The fit to `ramora/defclosedrecord/` (embodiment, D-13, D-14, D-16, K-01) is the same.
  - Context for the later turns: commit `27e1b9d` moved the root `project.edn` to `.psi/project.edn`. The root file
    does not exist now. `.psi/project.edn` has the start command `-M:run:dev:test-paths --nrepl` and `:session-profiles`
    (the lifecycle model profiles). The Context paragraph about the root `project.edn` is stale. Scope 4 reads as
    "create the file". This is a fact or wording mismatch, not an architectural misfit, so it is left to the
    inconsistency turn. A fix must keep `:session-profiles` and change only the start command.
- ambiguity review added 1 new design step
  - Classified as ambiguity: Scope 4 does not tell the implementer what to do with the existing keys. The stale
    Context paragraph about the root `project.edn` is a separate inconsistency for the next turn.
- inconsistency review added 1 new design step
  - Fix this step together with the `.psi/project.edn` ambiguity step. Both edit the same facts in `design.md`.

## Notes for the pass 3 design-steps fix

- Read `.psi/project.edn` before you edit `design.md`. It is the only psi project configuration now. Do not
  re-create a root `project.edn`.
- Keep `:session-profiles` and the `#_` commented-out profile blocks. The task-lifecycle workflows use the profiles.
  The only change in that file is the value of `:agent-session :project-nrepl :start-command`.
- `.gitignore` already ignores `/.psi/project.local.edn` and `/.psi/metrics.edn`. Scope 5 adds entries only.
- After the design fix: `plan.md` item 5 and `steps.md` ("Write `.psi/project.edn`") say "write". The plan review
  must make them say "edit the start command".

## Design-steps fix, pass 3 (2026-10-19)

- Both pass 3 design steps resolved in `design.md` (Context, Scope 4, Decisions). No `SCOPE_QUESTION:` items.
- `design.md` no longer refers to a root `project.edn` or to commit `4b09778`. The root file does not exist.
- Still open for the plan review: `plan.md` item 5 and `steps.md` ("Write `.psi/project.edn`") say "write". Change
  them to "edit the start command only". Verify with `git diff .psi/project.edn`: one changed line.

## Design review, pass 4 (2026-10-19)

- no architectural review feedback
  - Since pass 3, only the `.psi/project.edn` facts changed (Context, Scope 4, Decisions). An edit of the start
    command only is minimal and agrees with the principles. Fit to the embodiment, D-13, D-14, D-16 and K-01 is unchanged.
- no ambiguity review feedback
  - AC 1 ("`.scry-results/` is empty") is exact: the scry CLI clears and recreates the directory at run start and
    leaves it empty on a passing run (`.psi/skills/scry/SKILL.md`).
- no inconsistency review feedback

## Notes after design review, pass 4

- Pass 4 added no design steps. `design-steps.md` has no open items. There is no design-steps fix to do.
- Next step: plan review. The pass 3 carry-over is still open: `plan.md` item 5 (`` `.psi/project.edn`. ``) and
  `steps.md` ("Write `.psi/project.edn`.") do not yet say "change only the start command" (design Scope 4).
