# Development

Use the Clojure CLI. Run all commands from the repository root.

Run the tests on Clojure 1.11:

    clojure -M:test -m scry.cli

Run the tests on Clojure 1.12 and on Clojure 1.13:

    clojure -M:test:clj-1.12 -m scry.cli
    clojure -M:test:clj-1.13 -m scry.cli

Start a REPL. The nREPL server writes the port to `.nrepl-port`:

    clojure -M:test:nrepl

Run the tests in the REPL:

    (require '[scry.core :as scry])
    (scry/run)

Run the benchmarks against `defrecord` (about 6 minutes):

    clojure -M:bench

### Continuous integration

[CI](../.github/workflows/ci.yml) runs on pushes to `master` and pull requests
against any branch, including forks. Jobs use Ubuntu, Temurin Java 21,
and Clojure CLI `1.12.6.1673`, with read-only repository permissions.

The code-quality job uses cljfmt `0.16.4` and clj-kondo `2026.04.15`.
Run the same non-writing checks locally:

    cljfmt check src test dev test-build test-release build.clj
    clj-kondo --lint src test dev test-build test-release build.clj --fail-level warning

Formatting differences, lint warnings, and lint errors fail the job.
The runtime matrix runs the three test commands above; `deps.edn` owns
the Clojure versions. A separate job runs the build tests and JAR command
below once, with full Git history. A separate job runs the release tests. Performance benchmarks remain manual.

Failed tests upload `.scry-results/` as `scry-runtime-1.11`,
`scry-runtime-1.12`, `scry-runtime-1.13`, `scry-build`, or `scry-release`. These artifacts
contain structured failure data. Missing result files produce a warning;
the test failure still fails the job. CI does not publish releases or
configure branch protection.

### Build a library JAR

Build the source-only JAR for `org.hugoduncan/closed-record`:

    clojure -T:build jar

The build writes `target/closed-record-<version>.jar` and generates the POM
at `target/classes/META-INF/maven/org.hugoduncan/closed-record/pom.xml`.
The JAR contains runtime source, Maven metadata, and the EPL-2.0 license.
Build tools are not runtime dependencies.

The version is `0.1.<count>`, where `<count>` is the output of
`git rev-list --count HEAD`. Git and repository history are required;
version lookup failure stops the build. Use a full checkout for a
release-quality build. A shallow checkout can give a smaller count.
Each build removes stale output from `target/` before packaging.

Run the separate build tests, including a consumer JVM that loads the JAR:

    clojure -M:test:build-test -m scry.cli --dir test-build

Remove build output:

    clojure -T:build clean

`clean` is safe to repeat and does not require Git metadata. Generated
output under `target/` is ignored by Git. These build and clean commands do not install or
deploy the artifact; there is no published Clojars release yet.

Run the separate release tooling tests (pure notes and real Git fixtures):

    clojure -M:test:release:release-test -m scry.cli --dir test-release
