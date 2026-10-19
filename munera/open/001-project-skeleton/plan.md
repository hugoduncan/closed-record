# Plan

## Approach

Write the files in the order of their dependencies. Verify each slice with its acceptance criteria before the
next slice.

Key decisions (from design.md):

- Pin all dependency versions: Clojure 1.11.4, scry 0.1.61, nREPL 1.8.0. No `RELEASE`.
- Version aliases `:clj-1.12` and `:clj-1.13` use `:override-deps`, so they compose with `:test` and `:nrepl`.
- `:test` alias has no `:main-opts`, so it composes with `:nrepl`.
- Edit only `:agent-session :project-nrepl :start-command` in `.psi/project.edn`. Keep `:version`,
  `:session-profiles` and the `#_` forms.
- The smoke test exists because the scry CLI fails with zero tests.
- Obey the test standards in `.psi/skills/clojure-coding-standards/SKILL.md`: no docstring on `deftest`, an
  intention comment first.

## Slice order

1. Classpath: `deps.edn` with base dependency and aliases `:test`, `:nrepl`, `:clj-1.12`, `:clj-1.13`.
2. Code: `src/closed_record/core.cljc` (docstring, no vars) and `test/closed_record/core_test.clj` (smoke test).
3. Tooling: `.gitignore` entries and `.psi/project.edn` start command.
4. Verification: acceptance criteria 1 to 7 (test runs, version checks, lint, format, psi REPL, clean tree).
5. Documentation: `README.md` "Development" section and `mementum/state.md`. `munera/plan.md` lists this task
   already and needs no change.

## Verification

Run the commands in design acceptance criteria 1 to 7. For test failures, read `.scry-results/*.edn`. Do not read
the terminal output for details.

## Risks

- Clojure 1.13.0-alpha9 can be incompatible with scry or nREPL. Then record the error in `implementation.md` and
  ask the user. Do not change the minimum version.
- The psi project REPL can need more time to start than its default readiness timeout. Then record it in
  `implementation.md`.
