# Plan

## Approach

Write the files in the order of their dependencies. Verify each part with its acceptance criterion before you
start the next part.

1. `deps.edn` with the base dependency and the aliases `:test`, `:nrepl`, `:clj-1.12` and `:clj-1.13`.
   Use `:override-deps` for the Clojure version in the version aliases, so the aliases compose.
2. `src/closed_record/core.cljc` with the namespace docstring.
3. `test/closed_record/core_test.clj` with the smoke test. Obey the test standards in
   `.psi/skills/clojure-coding-standards/SKILL.md`: no docstring on `deftest`, and an intention comment first.
4. `.gitignore` entries.
5. `.psi/project.edn`.
6. `README.md` "Development" section.
7. `mementum/state.md` and `munera/plan.md`.

## Verification

Run the commands in design acceptance criteria 1 to 7. For test failures, read `.scry-results/*.edn`. Do not read
the terminal output for details.

## Risks

- Clojure 1.13.0-alpha9 can be incompatible with scry or nREPL. Then record the error in `implementation.md` and
  ask the user. Do not change the minimum version.
- The psi project REPL can need more time to start than its default readiness timeout. Then record it in
  `implementation.md`.
