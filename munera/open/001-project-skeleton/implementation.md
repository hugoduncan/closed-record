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

## Plan review, pass 1 (2026-10-19)

- ambiguity review added 1 new design step
  - Context for the inconsistency turn (not filed as ambiguity): `steps.md` criterion 1 omits "`.scry-results/` is
    empty". No step and no plan item checks AC 10. The README step says "test commands"; AC 8 also needs the in-REPL
    command from Decisions. The pass 3 carry-over (`plan.md` item 5, "write") is resolved by commit `d8ae2bf`.
- inconsistency review added 3 new design steps
  - Not filed: `plan.md` Approach says "verify each slice before the next slice", but all checks are in Slice 4.
    AC 1 needs Slice 2, so early checks are partial only. The order in `steps.md` is sufficient.

## Notes for the plan-review design-steps fix

- The 4 open plan-review steps change `plan.md` and `steps.md` only. Do not change `design.md`: the design is stable
  after pass 4, and each step is a missing check or rule, not a design change. Use the principles in "Notes for the
  design-steps fix" (edit in place, frozen scope, simplified technical English).
- `steps.md` items stay unchecked. Add the new checks to the slice that the step names.
- `git status` rule: the "commit slices 1 to 3 first" option agrees with `.psi/skills/work-independently/SKILL.md`
  (commit after every step). If you select it, write the commit step in `steps.md` before the `git status` step.
- AC 10 check: `find src test -type f` works before the commit, and `git ls-files src test` works after it.

## Plan-review design-steps fix (2026-10-19)

- All 4 plan-review steps resolved in `plan.md` and `steps.md`. `design.md` is not changed. `steps.md` items stay
  unchecked (no implementation yet).
- Selected `git status` rule: commit slices 1 to 3 (new last step of Slice 3) before the Slice 4 checks. Edits after
  that commit (fixes, `README.md`, `mementum/state.md`, `implementation.md`) are modified files, not untracked files,
  so criterion 7 is not affected.
- AC 10 check uses `find src test -type f` (it also finds untracked files), not `git grep defclosedrecord`: the
  `core.cljc` namespace docstring can name `defclosedrecord` and give a false failure.
- Repository now: no `src/`, no `test/`, no `deps.edn`. `.gitignore` has only `/.psi/project.local.edn` and
  `/.psi/metrics.edn`.

## Plan review, pass 2 (2026-10-19)

- no ambiguity review feedback
  - Checked for the smoke test (criterion 4): with clj-kondo, `[closed-record.core :as core]` gives the warning
    "required but never used". A bare `[closed-record.core]` with `(is (find-ns 'closed-record.core))` gives no
    errors and no warnings. cljfmt passes.
  - Checked for criterion 6: psi reads the start command when `project-repl start` runs (merge: user < project).
    `.psi/project.local.edn` has no `:project-nrepl` key, so the Slice 3 edit is the command that psi uses.
- no inconsistency review feedback

## Notes after plan review, pass 2

- Plan review pass 2 added no design steps. `design-steps.md` has no open items. There is no design-steps fix to do.
  Next step: implementation (`steps.md`, Slice 1).
- `README.md`: add the "Development" section only. Do not change the existing text (its typos are out of scope).
- `mementum/state.md` "Status" has the line "No source code yet." Replace it. Obey the rule in that file: no task
  progress there.
- Paths for the implementer: `.psi/skills/scry/SKILL.md` (CLI contract), `.psi/skills/clojure-coding-standards/SKILL.md`
  (test style), psi config merge in `~/projects/hugoduncan/psi/*/components/project-nrepl/src/psi/project_nrepl/config.clj`.

## Implementation (2026-10-19)

- All slices done. All acceptance criteria verified.
- Smoke test: bare require `[closed-record.core]` and `(is (find-ns 'closed-record.core))`. clj-kondo: 0 warnings.
- `.gitignore` entries are root-anchored (`/.cpcache/` etc.), as the existing entries.
- Criterion 6: `psi-tool` is not on PATH in this harness. Verified with the same start command
  `clojure -M:test:nrepl` and an nREPL client: `(:pass? (scry/run))` gave `true`. nREPL deletes `.nrepl-port` at
  stop. A psi-managed start is still worth a check at review.
- 1.13.0-alpha9 works with scry 0.1.61 and nREPL 1.8.0.
