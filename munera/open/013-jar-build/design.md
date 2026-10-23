# 013 JAR build

## Intent

Produce a library JAR that consumers can use without a checkout of
closed-record. Keep the build small and separate from runtime dependencies.

## Context

At task design, the project had no JAR build. `deps.edn` has `src` as its runtime path and
Clojure 1.11.4 as its only runtime dependency. Test, benchmark and REPL tools
are in development aliases. The README gives a git dependency for installation.

Runtime source consists of:

- `src/closed_record/core.cljc`: the public macros and functions, the internal
  protocol, and runtime helpers.
- `src/closed_record/impl/jvm.clj`: the JVM emitter, needed when a consumer
  expands `defclosedrecord`.

The reference is `~/projects/hugoduncan/scry/scry-master/build.clj` and its
`:build` alias. Use its single-artifact build approach, not its adapter or
deployment machinery.

The user confirmed:

- Maven coordinate: `org.hugoduncan/closed-record`.
- Version: `0.1.<git commit count>`, with no fallback if Git metadata is unavailable.
- Build only: no local Maven installation or deployment.
- License: EPL-2.0, as in the reference project, with a license file.

## Scope and contracts

### Build interface

Use `clojure.tools.build` through a maintainer-only `:build` alias and the
`build` namespace. From the repository root:

- `clojure -T:build clean` removes build output under `target/`. It succeeds
  when that directory is absent and does not require Git metadata.
- `clojure -T:build jar` produces `target/closed-record-<version>.jar` and a
  generated POM under `target/`. It clears stale build output before packaging.

Build output is ignored by Git. Build tooling must not become a runtime
dependency. The build does not require tests, benchmarks, or REPL tooling on
its classpath.

### Version and metadata

The version suffix is the output of `git rev-list --count HEAD` in the project
repository. Compute the version once per JAR build and use it in the artifact
name and all Maven metadata. A failed Git command or a non-numeric count must
stop the build with a clear error. Do not invent a fallback version.

The POM and embedded Maven metadata identify `org.hugoduncan/closed-record`
and the computed version. The POM declares only the runtime dependencies from
`deps.edn`; it does not include dependencies from development or build aliases.

Public metadata includes the name `closed-record`, the description
"A Clojure record type with a fixed key set.", the project URL
`https://github.com/hugoduncan/closed-record`, the source repository, and the
EPL-2.0 license. No release tag is created or assumed by this task.

### Artifact contents

Produce one source-only library JAR, not an uberjar or an executable JAR.
Include both runtime source files at their namespace paths, embedded Maven
metadata, and the EPL-2.0 license text. Add the license text as a repository
license file as well.

Do not include AOT classes, bundled dependencies, tests, benchmarks, build
code, development configuration, task files, or project working memory.

The project retains its current JVM and Clojure support. Packaging must not
change the public API or record behaviour.

### Documentation and verification

Document the build and clean commands, output location, version rule, and
Git requirement in the README. Keep the git installation instructions: a
local build does not mean that a Clojars release exists.

Verify the artifact itself, not only the source checkout. A consumer process
must load the library from the built JAR with its runtime dependencies and
without the project source or development paths.

## Design choices

Use the standard tools.build facilities for one source JAR and its POM.
A custom archive writer or a copy of scry's multi-artifact machinery adds no
value here. AOT compilation is not needed: consumers must be able to expand
the macro from the packaged source.

`deps.edn` owns runtime dependency versions. The build owns the artifact
coordinate and version rule. Build output stays under `target/`.

Commit-count versions require Git history. A shallow checkout can give a
smaller count; use a full checkout for a release-quality build. Release
version policy beyond the confirmed rule is outside this task.

## Out of scope

- Local Maven installation, Clojars deployment, credentials, and signing.
- Release tags, release automation, and CI setup.
- Additional artifacts, an uberjar, AOT packaging, or other host support.
- Changes to the runtime API, behaviour, or benchmarks.

## Acceptance criteria

1. From a full Git checkout, `clojure -T:build jar` succeeds and produces the
   named JAR and POM with version `0.1.<git rev-list --count HEAD>`.
2. JAR inspection confirms the required source, license, and Maven metadata;
   excluded files and bundled dependencies are absent. POM dependencies match
   the base runtime dependencies in `deps.edn`.
3. A separate JVM process, with the JAR and runtime dependencies but no
   checkout source paths, requires `closed-record.core`, defines a record,
   constructs it, reads a field, updates a declared key, and observes the
   expected exceptions for an undeclared `assoc` and a declared-key `dissoc`.
4. A second build excludes stale staging files. `clean` removes build output,
   is safe to repeat, and leaves source files unchanged. Git ignores generated
   output.
5. Verification covers failed Git version lookup and non-numeric output;
   neither can produce an artifact with a fallback version.
6. The existing test suite passes. Build verification commands and results are
   recorded in `implementation.md` during execution.
7. The repository and JAR contain the EPL-2.0 license text. README build
   instructions match the commands and do not claim a published release.
