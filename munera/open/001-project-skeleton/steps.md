# Steps

## Slice 1: classpath

- [ ] Write `deps.edn`: `:paths ["src"]`, dependency `org.clojure/clojure` 1.11.4.
- [ ] Add alias `:test`: extra path `test`, dependency `org.hugoduncan/scry` 0.1.61, no `:main-opts`.
- [ ] Add alias `:nrepl`: `nrepl/nrepl` 1.8.0, `:main-opts ["-m" "nrepl.cmdline"]`.
- [ ] Add aliases `:clj-1.12` and `:clj-1.13` with `:override-deps` for Clojure 1.12.6 and 1.13.0-alpha9.

## Slice 2: code

- [ ] Write `src/closed_record/core.cljc`: namespace `closed-record.core` with a docstring, no vars.
- [ ] Write `test/closed_record/core_test.clj`: namespace `closed-record.core-test`, one smoke test that
      requires `closed-record.core` and checks that the namespace is loaded. Intention comment first, no
      docstring on `deftest`.

## Slice 3: tooling

- [ ] Add `.gitignore` entries: `.cpcache/`, `.scry-results/`, `.nrepl-port`, `.clj-kondo/.cache/`.
- [ ] In `.psi/project.edn`, change only `:agent-session :project-nrepl :start-command` to
      `["clojure" "-M:test:nrepl"]`. Keep all other content.

## Slice 4: verification

- [ ] Run `clojure -M:test -m scry.cli`: exits 0, one or more tests, zero failures (criterion 1).
- [ ] Run `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`: both exit 0
      (criterion 2).
- [ ] Check versions: `clojure -M -e '(print (clojure-version))'` prints `1.11.4`; with `:clj-1.12` prints
      `1.12.6`; with `:clj-1.13` prints `1.13.0-alpha9`; also with `:test` composed (criteria 2, 3).
- [ ] Run `clj-kondo --lint src test`: no errors, no warnings (criterion 4).
- [ ] Run `cljfmt check src test`: passes (criterion 5).
- [ ] Start the psi project REPL (`psi-tool project-repl start`); in the REPL `(:pass? (scry/run))` is `true`
      (criterion 6).
- [ ] Run `git status`: no new untracked files (criterion 7).

## Slice 5: documentation

- [ ] Add a "Development" section to `README.md` with the test commands, the version-alias test commands and
      the REPL start command, in simplified technical English (criterion 8).
- [ ] Update `mementum/state.md` with the skeleton and the test commands (criterion 9). `munera/plan.md` needs
      no change.
- [ ] Record decisions and discoveries in `implementation.md`.
