# JAR build plan

## Approach

Implement the contracts in [design.md](design.md) without changing runtime
source or behaviour. Use the scry reference only for the tools.build alias
and single-JAR sequence. Do not copy its adapter, deployment, custom XML,
or release-tag machinery.

### Build and metadata

- Add a maintainer-only `:build` alias with `:ns-default build` and a pinned
  tools.build dependency at the reference version, `0.10.10`.
  Keep the base paths, dependencies, and existing aliases unchanged.
- Add root `build.clj`. `clean` deletes only `target/` and does not resolve a
  version or create a dependency basis. Loading the namespace must not run
  Git. Repeated clean calls must succeed.
- In `jar`, run `git rev-list --count HEAD` from the project root once.
  Separate validation of the process result from process execution. Trim
  output and accept only a successful exit with decimal digits. Report a
  clear error for launch failure, nonzero exit, or invalid output. Validate
  before writing an artifact; never supply a fallback.
- Use the resulting `0.1.<count>` throughout the build. Clear `target/` before
  staging. Create a basis from project `deps.edn`, with no aliases and with
  user dependency configuration excluded. Generate the POM with `b/write-pom`
  and the JAR with `b/jar`; do not compile or bundle dependencies.
- Stage under `target/classes/`. Copy `src` at its namespace paths and copy
  repository `LICENSE` to the JAR root. Copy the complete EPL-2.0 text from
  the verified reference `~/projects/hugoduncan/scry/scry-master/LICENSE`.
- Emit `target/closed-record-<version>.jar`. Keep the generated POM at
  `target/classes/META-INF/maven/org.hugoduncan/closed-record/pom.xml`.
  Use the same Maven path in the archive, including `pom.properties`.
- Set coordinate, name, description, project URL, source repository, and
  license metadata exactly as specified by the design. Omit an SCM release
  tag. The direct POM dependencies must match base `:deps`, not the build
  basis used to launch tools.build. Add `/target/` to `.gitignore`.

### Verification

Keep build tests separate from the default runtime suite. Add
`test-build/build_test.clj` and a `:build-test` alias that adds root and
`test-build` paths and tools.build. Use the existing `:test` alias for scry:
`clojure -M:test:build-test -m scry.cli --dir test-build`.
Neither test alias is required by `clojure -T:build jar` or `clean`.
For isolated CLI runs, set `CLJ_CONFIG` to a temporary directory with an
empty `deps.edn` map; pass this environment to child CLI processes. Resolve
consumer dependencies from the base project basis, including transitive
runtime JARs. Remove temporary fixtures after each check.

Use `clojure.java.shell/sh` for test subprocesses so the test harness runs
on Clojure 1.11. tools.build 0.10.10 process execution needs Clojure 1.12;
the isolated `-T:build` process supplies its own tooling runtime.

Use real processes and temporary filesystem fixtures for build and clean
checks. Use pure process-result validation tests for non-numeric output,
blank output, and failed exit status; do not mock Git or add a test-only
version override. Test an actual failed Git lookup in a temporary project
copy without Git metadata, outside any Git work tree. Confirm failure leaves
no new JAR or POM. Keep version validation before all artifact writes.

Inspect archive entries and parse POM XML and Maven properties. Compare the
packaged source and license bytes with repository files. Permit standard
JAR metadata, but reject classes, dependency content, and all excluded
project files. Verify the version against Git at build time, before any
verification commit changes the count.

Launch a separate JVM from a temporary consumer directory. Its explicit
classpath must contain only the built JAR and resolved base runtime
dependency JARs. Check that the core and emitter resources resolve inside
the built JAR. Exercise the record operations and exception types in
acceptance criterion 3. Do not use the test runner's classpath for this JVM.

Record execution commands, exit status, structured test results, artifact
paths, computed version, and discoveries in `implementation.md`. Keep
fixtures outside `target/` when they must survive a clean operation.

## Risks

- Shallow history gives a smaller count. Require full history for a
  release-quality build; no release policy or automation is added here.
- User CLI configuration can contaminate dependency resolution. Use an
  isolated CLI configuration for verification and exclude user dependencies
  from the runtime basis. Check the POM and consumer classpath explicitly.
- A source-checkout classpath can hide a missing emitter. The isolated JVM
  and resource-origin assertions guard against this error.
- The reference assumes a release tag and omits the license from its core
  JAR. Follow this task's explicit contracts instead.
- Dependency downloads require repository access or a populated cache.
  Report environmental failures separately from assertion failures.
- No known blocker. The design leaves staging layout, license filename,
  tools.build version, and verification layout to implementation; the choices
  above resolve these details without changing scope.

## Slice order

1. **Build and metadata**: add the alias, license, ignore rule, clean task,
   version validation, POM generation, and source JAR. Covers criteria 1, 2,
   4, 5, and the artifact part of 7.
2. **Artifact verification**: add focused build tests, inspect the archive
   and metadata, exercise the isolated consumer, and test clean, stale output,
   and version failure paths. Covers criteria 1–5 and 7.
3. **Documentation and final checks**: document the interface, run build
   verification and all supported Clojure test configurations, and record
   results. Covers criteria 6 and 7 and confirms the complete contract.

Commit each completed implementation slice with its verification evidence.
Artifact verification was completed in smaller commits. All three slices are
complete, including documentation and final version-matrix verification.
Commands and results are recorded in `implementation.md`.

## Planning review — 2026-10-23

- Reviewed the plan for ambiguity with `steps.md` as read-only context.
  The checklist covers all seven acceptance criteria in implementation order.
  No blocking ambiguity remains.
- Checked design consistency against `deps.edn`, README, both runtime source
  paths, and the reference build, alias, and license. No internal design
  conflict or unresolved conflict with these artifacts was found.
- The README git coordinate differs from the Maven coordinate by design;
  retain it. The reference's tag and license-packaging differences are
  explicit exclusions, not requirements to copy.
- Non-blocking details resolved here: tools.build version, staging and POM
  paths, license filename, test layout, and CLI isolation procedure. No user
  decision is needed. `design.md` and `steps.md` were unchanged by review.
