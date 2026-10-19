# Design steps

- [x] Ambiguity: the version aliases and AC 2. The design says `:clj-1.12` and `:clj-1.13` "replace the Clojure
  version", but it does not give the mechanism (`:extra-deps` or `:override-deps`). AC 2 checks only exit 0, so an
  alias that does not change the version also passes. Give the mechanism, and add a check as in AC 3, for example
  `clojure -M:clj-1.12 -e '(print (clojure-version))'` prints `1.12.6` (and with `-M:test:clj-1.12`).
- [x] Ambiguity: the REPL start command. Scope 6 and AC 8 require a README command to start a REPL, but Decisions
  gives only the in-REPL test command. State the command (for example `clojure -M:test:nrepl`) and how the `:nrepl`
  alias starts the server (for example `:main-opts ["-m" "nrepl.cmdline"]`, which writes `.nrepl-port`).
- [x] Ambiguity: the `munera/plan.md` update. Scope 7 requires an update, but `munera/plan.md` already lists this
  task, and AC 9 only requires that it lists the task. State the change, or state that no change is necessary.
- [x] Inconsistency: the root `project.edn`. Commit `4b09778` (after the design) added a root `project.edn` with
  `:agent-session :project-nrepl :start-command ["clojure" "-M:run:dev:test-paths" "--nrepl"]` and
  `:session-profiles`. This conflicts with the Problem ("documents only"), with Scope 4 (`.psi/project.edn`,
  `-M:test:nrepl`) and with the Decision "no `dev` path": the aliases `:run`, `:dev` and `:test-paths` do not exist.
  psi reads only `.psi/project.edn`. Record this file in Context, and state that this task does not change it.
- [x] Ambiguity: the content of `.psi/project.edn`. Commit `27e1b9d` moved the root `project.edn` to
  `.psi/project.edn`. The file exists now, with `:version 1`, the start command `["clojure" "-M:run:dev:test-paths"
  "--nrepl"]` and `:session-profiles`. Scope 4 and the Decision "`.psi/project.edn` is committed" read as "create the
  file". They do not say if the task replaces the file or changes only the start command. State that the task changes
  only `:agent-session :project-nrepl :start-command` to `["clojure" "-M:test:nrepl"]` and keeps `:session-profiles`.
- [x] Inconsistency: the Context paragraph "Root `project.edn`" and the repository. Commit `27e1b9d` moved the root
  `project.edn` to `.psi/project.edn`. The root file does not exist now. The paragraph says that the root file exists,
  that psi does not read it, and that "this task does not change the root `project.edn`". Change the paragraph to
  describe `.psi/project.edn` as it is now: it exists, it has the start command
  `["clojure" "-M:run:dev:test-paths" "--nrepl"]` with the aliases `:run`, `:dev` and `:test-paths` that do not exist,
  and it has `:session-profiles`. Remove the statement about the root file.
- [x] Ambiguity (plan review): the `git status` step in Slice 4 (criterion 7). The plan does not say if slices 1 to
  3 are committed before the check. If they are not committed, `git status` shows `deps.edn`, `src/` and `test/` as
  untracked, and the step "no new untracked files" fails. State one rule in `plan.md` or `steps.md`: commit slices 1
  to 3 before the check, or "no untracked files other than the files that slices 1 to 3 add" (only tool output, for
  example `.cpcache/`, `.scry-results/`, `.nrepl-port`, `.clj-kondo/.cache/`, must not show).
- [x] Inconsistency (plan review): `steps.md` Slice 4, criterion 1. The step lists "exits 0, one or more tests, zero
  failures", but design AC 1 also requires "`.scry-results/` is empty". Add the check to the step.
- [x] Inconsistency (plan review): design AC 10 has no step and no plan item. `plan.md` Verification covers AC 1 to 7.
  Slice 5 covers AC 8 and 9. Add a check for AC 10 (no other source namespaces, no `defclosedrecord`), for example
  in Slice 4: the only files under `src/` and `test/` are `closed_record/core.cljc` and `closed_record/core_test.clj`.
- [x] Inconsistency (plan review): `steps.md` Slice 5, README step. The step lists the test commands, the
  version-alias test commands and the REPL start command. Design AC 8 requires all the commands in Decisions, which
  include the in-REPL test command `(require '[scry.core :as scry]) (scry/run)`. Add the in-REPL command to the step.
