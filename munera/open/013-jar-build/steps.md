# JAR build steps

Record commands, results, and decisions in `implementation.md` as each
slice proceeds. Mark an item complete only after its stated check passes.

## Slice 1 — Build and metadata

- [x] Add the pinned tools.build `:build` alias with `:ns-default build` to `deps.edn`; confirm base runtime paths, dependencies, and existing aliases are unchanged.
- [x] Add root `LICENSE` from the reference project's EPL-2.0 file; verify the complete text matches the reference.
- [x] Add `/target/` to `.gitignore`; confirm `git check-ignore target/probe` identifies the rule.
- [x] Implement `build/clean` in root `build.clj` with no namespace-load Git lookup; confirm `clojure -T:build clean` succeeds twice when `target/` is absent.
- [x] Implement Git version lookup and pure process-result validation; verify a successful lookup returns `0.1.<count>` for the current HEAD and invalid results throw clear errors.
- [x] Implement `build/jar` with one version lookup, fresh staging, an alias-free runtime basis, standard POM generation, source and license copying, and source-only JAR generation; confirm `clojure -T:build jar` produces the JAR and POM at the paths in `plan.md` without test, benchmark, or REPL aliases.
- [x] Inspect the first JAR and POM for coordinate, version, public metadata, both runtime sources, root `LICENSE`, and base runtime dependencies; record the command results and commit the build slice.

## Slice 2 — Artifact verification

- [x] Add `:build-test` and `test-build/build_test.clj` as specified in `plan.md`; confirm scry discovers a passing build test with `clojure -M:test:build-test -m scry.cli --dir test-build` and the default runtime suite does not discover this directory.
- [x] Add process-result validation tests for numeric output with a trailing newline, nonzero exit, blank output, and non-numeric output; assert the version or clear error data without replacing Git or production vars.
- [x] Add a real-process failure check in a temporary project copy outside any Git work tree; assert `jar` fails clearly without creating a JAR or POM when Git metadata is absent.
- [x] Add a build-success check that compares the artifact filename, generated POM, embedded POM, and `pom.properties` with the same Git-derived version and `org.hugoduncan/closed-record` coordinate.
- [x] Add parsed-POM checks for the exact public name, description, project URL, source repository, EPL-2.0 metadata, no assumed release tag, and direct dependencies equal to base `deps.edn` dependencies.
- [x] Add archive checks for source and license byte equality, required Maven metadata, and absence of AOT classes, bundled dependencies, tests, benchmarks, build code, development configuration, task files, and working memory.
- [x] Add an isolated consumer JVM check with only the built JAR and base runtime dependency JARs; assert core and emitter resources come from the artifact, then require the library, define and construct a record, read a field, update a declared key, and check the undeclared-assoc and declared-dissoc exception types.
- [x] Add a stale-output check that inserts a sentinel into staging and an obsolete artifact under `target/`, then builds again; assert neither stale file survives and the sentinel is absent from the new JAR.
- [x] Add a clean check in the no-Git project fixture; assert build output is removed, a repeated clean succeeds, and source file bytes are unchanged.
- [x] Run the focused build suite with isolated CLI configuration; verify its exit code and `.scry-results`, record the artifact version and all acceptance evidence, and commit the verification slice.

## Slice 3 — Documentation and final checks

- [x] Update README development instructions with `clojure -T:build jar`, `clojure -T:build clean`, output paths, the commit-count version rule, Git and full-history requirements, and the focused build-test command; preserve git installation instructions and the no-Clojars-release status.
- [x] Run the documented build, build-test, and clean commands from the repository root with isolated CLI configuration; verify output locations, repeatable clean, and ignored generated output, then record the results in `implementation.md`.
- [x] Run `clojure -M:test -m scry.cli`; verify the exit code and `.scry-results` and record the Clojure 1.11 result.
- [x] Run `clojure -M:test:clj-1.12 -m scry.cli`; verify the exit code and `.scry-results` and record the Clojure 1.12 result.
- [x] Run `clojure -M:test:clj-1.13 -m scry.cli`; verify the exit code and `.scry-results` and record the Clojure 1.13 result.
- [x] Review the final diff against all seven design acceptance criteria; confirm runtime source and API are unchanged, all evidence is in `implementation.md`, and no install, deploy, release-tag, or CI machinery was added, then commit the documentation and final checks.
