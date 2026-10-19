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
