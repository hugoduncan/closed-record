# Steps

## Slice 1: Expansion fixture

- [x] Create `test/runtime/closed_record/expansion_fixture.clj`
      (`closed-record.expansion-fixture`, the fixed namespace) with `forms`:
      the fixture `defclosedrecord` forms (cover `:no-print`, `:load-ns`,
      field tags, Java interfaces and methods, destructured method
      parameters), and `with-option`; list the form names in
      implementation.md.
- [x] Write the gensym normalisation function (replace `prefix__N` /
      `prefix__N__auto__` with `prefix__G<i>` / `prefix__G<i>__auto__` by order
      of first appearance) and the printed-form comparison (`*print-meta*`
      true) as `expansion` in the same namespace; verify two expansions of
      the same form normalise to equal strings.
- [x] Add the private `strip-positions` helper (plan.md, Position
      metadata): remove `:line`, `:column` and `:file` from every list,
      vector, map and set of the input form; keep other metadata; no
      `clojure.walk`. `expansion` applies it before `macroexpand-1`, so
      `record!`, the fixture test and `with-option` forms all get it.
      Verify: no `(expansion f)` string contains `:line`, `:column` or
      `:file`; field tags and list type hints stay; a form and the same
      text read with `read-string` (no position metadata) give the same
      string.
- [x] Add `record!`; verify `src/` matches `ac25942`; record
      `test/runtime/closed_record/expansion_fixture.edn` (EDN sorted map of
      form name to normalised printed expansion) from the unchanged emitter
      with `clojure -M:test -e "(require 'closed-record.expansion-fixture)
      (closed-record.expansion-fixture/record!)"`.
- [x] Add the VT-V1 fixture test in
      `test/runtime/closed_record/value_type_option_test.clj`: no-option and
      `:value-type false` expansions equal the fixture (the
      `:value-type false` assertion stays pending until slice 2 accepts the
      option; note this in the test or add it in slice 2).
- [x] Run the ordinary runtime suite; commit.

## Slice 2: Option and error contract

- [x] Extend `check-options` in `closed-record.impl.jvm`: accept
      `:value-type`; `true`/`false`/absent valid; other values throw
      `IllegalArgumentException` at macroexpansion; option not passed to
      `deftype*`.
- [x] Implement value-mode resolution: `requiring-resolve` of
      `value-type.core/defvaluetype*` only when `:value-type true` (VT-D4);
      throw → `ex-info {:type :closed-record/value-type-unavailable
      :class-name classname}` with the thrown exception as cause; `nil` →
      same `ex-info` with no cause; `classname` is the qualified class-name
      symbol.
- [x] Add ordinary-suite VT-V1 tests: invalid option values throw
      `IllegalArgumentException`; `:value-type true` without value-type throws
      `:closed-record/value-type-unavailable` with the `requiring-resolve`
      exception as cause; `:class-name` is the expected symbol.
- [x] Add the subprocess-JVM test helper `closed-record.subprocess`
      (`test/runtime/closed_record/subprocess.clj`): `clojure.main` with
      given arguments; java binary of `java.home`, JVM input arguments of
      the test JVM, extra JVM options, extra class path entries before the
      test JVM class path; gives `{:exit :out :err}`.
- [x] Add the absent-var subprocess test: temporary directory with a stub
      `value_type/core.clj` containing only `(ns value-type.core)`; assert
      the error has no cause (`ex-cause` nil).
- [x] Complete the fixture test's `:value-type false` assertion if deferred.
- [x] Run the ordinary runtime suite on Clojure 1.11, 1.12 and 1.13; commit.

## Slice 3: Emitter value mode

- [x] Add the representation description (type-form head, hidden fields,
      hash forms, `withMeta` form), selected once from the option in `emit`;
      pass it as explicit data to the affected emitter functions.
- [x] Value mode: hidden fields `__meta __extmap ^int __hash ^int __hasheq`
      with no mutable metadata (VT-D5); `hasheq`/`hashCode` compute D-07
      values on each call (VT-D6); `withMeta` constructor call with declared
      fields, `__meta` and `__extmap` as in `assoc` (VT-D7); type-form head
      `value-type.core/defvaluetype*` with the same arguments (VT-D3).
- [x] Verify no generated method reads or writes `__hash`/`__hasheq` in value
      mode; all other methods and both factories identical in both modes
      (VT-D8). Check: a local run with an uncommitted `-Sdeps` `:local/root`
      value-type dependency at `66d5ff7` and `:clj-1.12` that prints the
      value-mode expansion (plan.md, Value-mode check in slice 3); record the
      command and result in implementation.md. VT-V2 (slice 5) is the
      committed check.
- [x] Run the fixture test: no-option and `:value-type false` expansions
      still equal the fixture (VT-I2).
- [x] Run the ordinary runtime suite on Clojure 1.11, 1.12 and 1.13; commit.

## Slice 4: Test alias and shared checks

- [x] Apply the SHA rule (plan.md, Alias): verify that `8bb7c4f`
      (`8bb7c4f7a21d72c8eae553df89e17b71f34aeda3`) is on GitHub with
      `git fetch https://github.com/hugoduncan/value-type.git master` (or
      the branch that the user names) and
      `git merge-base --is-ancestor <sha> FETCH_HEAD` in the local
      value-type repository. **If not, stop and ask the user** (design
      Prerequisite); a commit that the user names gets the same check and,
      if it is not `66d5ff7`, the value-type facts check. Record the pinned
      SHA and the checks in implementation.md. Do not commit a
      `:local/root` dependency.
      Done 2026-11-03: `8bb7c4f` on GitHub master; tree equal to `66d5ff7`;
      pin `8bb7c4f`. See implementation.md.
- [x] Add the `:value-type-test` alias to `deps.edn`: lib
      `org.hugoduncan/value-type`, `:git/url
      "https://github.com/hugoduncan/value-type.git"`, full `:git/sha` of
      the pinned commit (no `:git/tag`), extra paths `test/value-type` and
      `test/value-type-clj-1.11`; base `:deps` unchanged. Verify that
      `clojure -Srepro -Spath -M:value-type-test` resolves it over HTTPS.
- [x] Add the alias to `closed-record.lint-dependencies/dependency-bases`.
- [x] Map the shared check selection rule to the ordinary runtime tests
      (run-time behaviour of a defined type: methods, factories, literals,
      printing, equality, hash, metadata, call-site errors, D-06 round trip,
      D-15 user implementations; exclude V-11, V-13, expansion-time tests,
      bench support tests); record the mapping in implementation.md.
- [x] Extract the shared checks into `define-checks` macros in
      `test/runtime/closed_record/checks/<topic>.clj`
      (`closed-record.checks.<topic>`; helpers in
      `closed-record.checks.support`), loadable without value-type: the macro
      expands the types, values and `deftest`s in the calling namespace with
      `:record-opts` after each field vector, unqualified type-bound
      symbols, class names from `(namespace-munge *ns*)`, and the D-06
      serialization assertion only when `:representation` gives
      `:identity`. Each ordinary test namespace calls
      `(define-checks {:record-opts [] :representation :identity})` (no
      copies).
- [x] Resolve clj-kondo findings for macro-defined names in
      `.clj-kondo/config.edn`, if any.
- [x] Run the ordinary runtime suite on Clojure 1.11, 1.12 and 1.13 without
      value-type; commit.
- [x] Update `ramora/defclosedrecord/embodiment/artifacts.md` rows for the
      shared check files and changed ordinary test files; commit.

## Slice 5: Value-type suite

- [x] Create `test/value-type/` with the expectation guard
      `closed-record.value-type.expectation`: `representation` reads
      `closed-record.test.representation`, gives `:value`/`:identity`, and
      throws if absent or another value; each suite test namespace calls it
      at load. Verify that scry 0.1.61 exits non-zero on that load error.
- [x] VT-V2: expansion inspection for value mode (hidden field layout, hash
      forms, `withMeta` form, `defvaluetype*` head) — the VT-I4 check on
      Temurin 21.
- [x] VT-V3/VT-V4: one test namespace
      `closed-record.value-type.<topic>-test` for each shared check
      namespace, calling `(define-checks {:record-opts [:value-type true]
      :representation (expectation/representation)})`; serialization round
      trip on identity fallback only, no serialization assertion for value
      output.
- [x] VT-V4 representation checks: `.isValue`, `identical?` of equal records,
      absence of the hash fields; compare with the expectation so the
      Valhalla job fails on silent identity fallback.
- [x] VT-V6: value-type errors pass through unchanged, as the Ramora VT-V6
      row. Both expectations (value-type checks these at macroexpansion,
      before the policy): a `^:non-nil` field gives `{:value-type/error
      :invalid-field :reason :non-nil-unsupported}`; a `^:nil-restricted`
      field gives `{:value-type/error :invalid-field :reason
      :removed-nil-restricted}`. Expectation `value` only (when the class is
      defined): a user method that reads `__hash` gives `{:value-type/error
      :invalid-field :reason :removed-field-access}`. No `:mutable-field`
      case: closed-record `check-field-metadata` rejects mutable fields
      before `emit` builds the form.
- [x] Does-not-load subprocess check: subprocess expands and evaluates a
      no-option and a `:value-type false` definition, asserts
      `value-type.core` not in `(loaded-libs)`.
- [x] VT-V5 subprocess check: `-Dvalue-type.fallback=error`, definition
      throws value-type's `Error`; runs only when the expectation is
      `identity`.
- [x] VT-V7: test `closed-record.value-type.aot-test` in the suite (no CI
      step of its own): write a `:value-type true` record namespace to
      `<tmp>/src`; subprocess 1 compiles it to `<tmp>/classes` (both on its
      class path); subprocess 2 (only `<tmp>/classes` added) loads the
      classes and checks behaviour and representation against the
      expectation; follows the value-type AOT rules.
- [x] Create `test/value-type-clj-1.11/` with the Clojure 1.11 VT-V1 case:
      every `:value-type true` expansion (not only the first) throws
      `:closed-record/value-type-unavailable` with a cause.
- [x] Record the three commands in implementation.md:
      Temurin 21 suite (`:test` + value-type alias + `:clj-1.12`,
      `-J-Dclosed-record.test.representation=identity`); Valhalla suite (same
      aliases, `JAVA_CMD`=Valhalla JDK, `-J--enable-preview`,
      `…representation=value`); Clojure 1.11 case (`:test` + value-type
      alias, no `:clj-1.12`).
- [x] Run all three locally with the `JAVA_CMD` of plan.md, Local JDKs:
      Temurin `21.0.10+7` for the Temurin 21 suite and the Clojure 1.11
      case; the macOS aarch64 Valhalla `27-jep401ea3+1-1` for the Valhalla
      suite (same build as CI, not the pinned Linux x64 archive). Record the
      `JAVA_CMD` and `java -version` of each run; commit.

## Slice 6: CI

- [x] Add required job `Value-type tests (Temurin 21)` to `ci.yml`:
      `actions/setup-java@v4.7.1` (Temurin `'21'`) and
      `DeLaGuardo/setup-clojure@13.4` with `cli: '1.12.6.1673'`, as the
      runtime matrix; suite step with expectation `identity`; upload step `if: failure()` on its
      own outcome, name `scry-value-type-temurin-21`, `path: .scry-results/`,
      `include-hidden-files: true`, `if-no-files-found: warn`,
      `actions/upload-artifact@v4.6.2`; then the Clojure 1.11 VT-V1 step with
      `if: ${{ !cancelled() }}` and its upload `scry-value-type-clj-1.11`
      (suite upload before the 1.11 step).
- [x] Add required job `Value-type tests (Valhalla)`: download
      `openjdk-27-jep401ea3+1-1_linux-x64_bin.tar.gz` from
      `download.java.net`, verify SHA-256
      `b8bdd7b181c6a5ea2dd9959255e222cd9d9a9f42cca4f2400991b9b2ff7ffb7d`,
      cache with the SHA-256 as key; `DeLaGuardo/setup-clojure@13.4` with
      `cli: '1.12.6.1673'`; no `setup-java` step: set `JAVA_CMD` to the
      `bin/java` of the extracted JDK (used for dependency resolution and
      the test JVM); run the Valhalla suite command (it
      includes VT-V7 for value output); upload `scry-value-type-valhalla`.
- [x] Verify: no `continue-on-error`; artifact names unique in the workflow;
      runtime matrix unchanged; release workflow runs the new jobs through
      `workflow_call` with no change.
- [x] **Stop and ask the user before the first push** (plan.md, CI,
      Push). Done 2026-11-03: the user approved free pushes of the branch
      `020-value-type-records` (worktree
      `/Users/duncan/projects/hugoduncan/closed-record/020-value-type-records`),
      including the slice-9 push. Not `master`.
- [x] Push and open the pull request: `git push -u origin
      020-value-type-records`, then `gh pr create --base master --head
      020-value-type-records` (once).
- [x] Read the run of the pushed commit (`gh run list --workflow ci.yml
      --branch <ref> --commit <sha>`, `gh run watch <id> --exit-status`,
      `gh run view <id> --json jobs`); every job, including both new jobs,
      has conclusion `success`. After a failure, fix, commit and push to the
      same ref again. Record the date, ref, SHA, pull request URL, and each
      run URL with its job conclusions in implementation.md; commit.

## Slice 7: VT-V8 bench

- [x] Add the VT-V8 entry namespace `closed-record.bench.value-type`
      (`dev/bench/closed_record/bench/value_type.clj`) as plan.md, VT-V8
      bench, states: records `V` (`:value-type true`) and `P`; `ops`
      `(hash x)` and `(= x y)`; own child command
      (`-m closed-record.bench.value-type <fork> <result-file>`); child
      throws if `V` is not a value class, then writes
      `{:schema-version 1 :timing-tool … :results [[op v p] …]}`; own
      parent loop over `report/fork-count` forks with the child result
      validation (including `timing-tool/validate-observation`); table
      `Operation`, `V mean [min, max] ns`, `P mean [min, max] ns`, `V / P`,
      `Forks`, no pass rule; exit 0, 2 (child not valid) or 64 (usage).
      Verify that no other namespace requires it, and that `fork`,
      `report`, `legacy-transport`, `timing-tool` and `closed-record.bench`
      do not change (`git diff`).
- [x] Extend `dev/bench/timing/run.sh` with `BENCH_ALIASES` (appended to
      `:bench` in the JAR lookup and the run) and `BENCH_JVM_OPTS` (`-J`
      options, run only), both default empty; `:corrected-timing` stays the
      last alias; arguments unchanged; verify the default run (no
      arguments: Clojure 1.11.4, V-11, S-03 pass rule and exit code) is
      unchanged.
- [x] Run the `test/bench` launcher test with both variables unset:
      `env -u BENCH_ALIASES -u BENCH_JVM_OPTS clojure -Sdeps '{:aliases
      {:bench-test {:extra-paths ["test/bench"]}}}' -M:test:bench-test -m
      scry.cli --dir test/bench --ns closed-record.timing-launcher-test`
      (1 test, 4 assertions before the change); it must pass.
- [x] Verify the `:test` alias does not load value-type through `dev/bench`.
- [x] Run VT-V8 manually:
      `JAVA_CMD=$HOME/.local/share/mise/installs/java/openjdk-27.0.0-jep401ea3+1-1/bin/java
      BENCH_ALIASES=:value-type-test:clj-1.12
      BENCH_JVM_OPTS=-J--enable-preview sh dev/bench/timing/run.sh -m
      closed-record.bench.value-type`; record commands, runtime (`java
      -version`), platform (`uname -sm`, machine model) and results in
      implementation.md.
- [x] Record the result and its limits (plan.md, VT-V8 limits: the machine
      of the run, the macOS aarch64 Valhalla `27-jep401ea3+1-1` build,
      Clojure 1.12.6, one manual, informational run) in
      `doc/performance.md`, in the style of its "Evidence and runtime"
      section; commit.

## Slice 8: Documentation

- [x] Update the `defclosedrecord` docstring: the option, the runtime
      dependency and the differences table, in brief.
- [x] Regenerate `doc/API.md`.
- [x] Update `README.md` and `doc/`: the option, differences table,
      value-type dependency (unreleased: how to add it, with the
      `:git/url` and `:git/sha` coordinate of the `:value-type-test` alias),
      Clojure 1.12.6 and JVM requirements, hash cost, no performance claim
      (VT-R4).
- [x] Update `doc/development.md`: suite commands for both JVMs and the
      Clojure 1.11 case; test suite list with both new directories and their
      requirements; CI section (Valhalla job JDK, other jobs Temurin 21,
      which jobs run which commands; keep the clause that all Clojure jobs
      use Clojure CLI `1.12.6.1673`); the three new failure artifact names.
- [x] Update `doc/tool-installation.md`: replace "CI uses Temurin Java 21"
      with the Valhalla/Temurin statement; keep the pinned local Temurin
      archive.
- [x] Update `ramora/value-type-records.md` Public contract: the
      `:class-name` value and the cause of each case; replace "the resolution
      failure as cause".
- [x] Update all of `ramora/value-type-records/embodiment.md` to state the
      current embodiment (no progress history):
      - Status: replace "Not implemented".
      - Elements table, `.github/workflows/ci.yml` row: both jobs
        (`Value-type tests (Temurin 21)` and `Value-type tests (Valhalla)`)
        and their failure uploads.
      - Elements table, Tests row: replace "VT-V1 runs in the ordinary
        `:test` alias" with the three VT-V1 placements (ordinary suite with
        the absent-var subprocess, value-type suite does-not-load
        subprocess, `test/value-type-clj-1.11/`).
      - Verification limits: replace "Until the job exists" with the
        current limits (one Valhalla build; CI on Linux x64; VT-V8 manual,
        on the machine of the run).
      - Check the other rows and sections against the implementation.
- [x] Update `ramora/value-type-records/invariants-and-verification.md`:
      VT-R1, VT-R2, the VT-V1 row (fixture, absent-var case, does-not-load
      placement, Clojure 1.11 case), the VT-V3 row (shared check selection,
      serialization on identity fallback only), the VT-D10 placement
      paragraph (names the identity part of VT-V7 for the Temurin 21 job).
      No progress history.
- [x] Update `ramora/defclosedrecord/embodiment.md`: Allocation (option
      check), Generated type / Method behaviour / State (ordinary mode,
      pointer to value mode), Boundaries (permitted dependencies), Public
      surface (`:value-type` option and the
      `:closed-record/value-type-unavailable` ex-data; do not add the
      `:type` to D-03).
- [x] Update `ramora/defclosedrecord/embodiment/artifacts.md` remaining
      rows: `deps.edn`, `src/closed_record/impl/jvm.clj`,
      `test/runtime/closed_record/*_test.clj`, the fixture rows
      (`expansion_fixture.clj` and `expansion_fixture.edn`), the
      `subprocess.clj` row, the new test directory rows, `ci.yml`,
      `dev/bench/`.
- [x] Update `mementum/state.md`; commit.

## Slice 9: Quality gate

- [x] Run format, lint, prose, API freshness and link checks; fix findings.
- [x] Run the build and release checks.
- [x] Final push (plan.md, CI, Final push): approved at slice 6. Push the
      branch `020-value-type-records`; read
      the run of the pushed commit with the slice-6 `gh run` commands; every
      job has conclusion `success`. After a failure, fix, commit and push to
      the same ref again. Record the date, ref, SHA, pull request URL and
      each run URL with its job conclusions in implementation.md.
- [x] Verify all acceptance criteria 1–8 against the design (acceptance
      criterion 3: the green run of the last commit that changes files
      outside `munera/open/020-value-type-records/`); record the
      verification in implementation.md; commit (task files only; not
      pushed).

## Implementation review follow-ups

- [x] Restore clj-kondo coverage of the shared checks. Each
      `closed-record.checks.<topic>` holds its tests as a quoted `body`
      vector (about 1500 lines), so clj-kondo does not lint them, and each
      check ns turns off `unused-namespace` and `unused-referred-var`. The
      ordinary test files now hold only a `define-checks` call, so the
      ordinary suite lost lint coverage too. Make the bodies real code that
      clj-kondo lints. For example: keep the check forms as plain top-level
      forms in a source file, and let `define-checks` read them from the
      resource and apply `support/expand`; then remove the quoted vectors
      and the clj-kondo `:off` metadata. Keep test and assertion counts
      (186/2793 ordinary; 81 value-type).
- [x] Make VT-V6 check "pass through unchanged". `errors-test/root-ex-data`
      searches the whole cause chain, so the test also passes if
      closed-record wraps the value-type error. Assert that the first cause
      that is not a `Compiler$CompilerException` is the value-type
      `ex-info` (same `ex-data` and message).
- [x] Add a timeout to `closed-record.subprocess/run` (for example
      `.waitFor` with a limit, then `destroyForcibly` and a failed result
      that holds the output so far). Now a child JVM that does not exit
      blocks the suite and the CI job until the GitHub job limit.

## Implementation review follow-ups, second round

- [x] Update the `test/runtime/closed_record/checks/support.clj` row of
      `ramora/defclosedrecord/embodiment/artifacts.md`. It names only
      `expand` and says it replaces `$ns`, `$P` and `$representation`.
      After the first follow-ups, `$ns` and `$representation` do not exist:
      the entry point is the macro `define-checks` (it reads the check
      source and evaluates only its `ns` form), and the placeholder var
      `closed-record.checks.support/representation` gives the
      representation.
- [x] Remove the temporary files that the tests and VT-V8 make. The
      `absent-var-test` stub directory (`value_type_option_test.clj`), the
      `aot-test` source and class directory, and the VT-V8 fork result
      files (`run-fork` in `closed-record.bench.value-type`) stay in the
      temporary directory after each run. Delete them in a `finally`
      (after the child exits or the result is read).

## Implementation review follow-ups, third round

- [x] Update the `test/runtime/closed_record/subprocess.clj` and
      `test/runtime/closed_record/*_test.clj` rows of
      `ramora/defclosedrecord/embodiment/artifacts.md`. The `subprocess.clj`
      row names only `run`; it does not name `temp-dir` and `delete-tree`
      (no symbolic links followed), which the absent-var, AOT tests use.
      The `*_test.clj` row says `subprocess_test.clj` checks only the time
      limit; it also has `delete-tree-test`.

## Implementation review follow-ups, fourth round

- [x] Push the branch and get a green CI run of the current code. The last
      CI run (`d689459`) comes before the three follow-up rounds, which
      changed `test/runtime`, `test/value-type` and `dev/bench`
      (`define-checks` rewrite, VT-V6 pass-through check, subprocess time
      limit and temporary-file deletion). Branch `020-value-type-records` is
      7 commits ahead of `origin`. Acceptance criterion 3 needs the green run
      of the last commit that changes files outside the task directory, so
      the slice-9 verification is stale. Push (approved at slice 6), read the
      run with the slice-6 `gh run` commands, and record the SHA, run URL
      and job conclusions in implementation.md as a new acceptance note.
      Done 2026-11-04: `acd056f`, run 41880383270, all 9 jobs `success`.
- [x] Rename the two shared-check `testing` labels in
      `test/runtime/closed_record/checks/defclosedrecord.clj` that name the
      ordinary-mode hash cache: "declared key gives hash caches 0 (no copy
      of cached hashes)" (`assoc-test`) and "with-meta copies the hash
      caches" (`metadata-test`). The value-type suite also runs them, and
      value mode has no hash cache (VT-D6). Name the behaviour that both
      modes share (for example "the hash after assoc equals the hash of a
      new record", "with-meta keeps the hash").
      Done: "the hash after assoc of a declared key equals the hash of a
      new record", "with-meta keeps the hash".

## Implementation review follow-ups, fifth round

- [x] Make shared-check failures report the correct source location.
      `define-checks` keeps the `:line` metadata of the forms that it reads
      from `closed-record.checks.<topic>`, but the compiler uses the file of
      the caller. Thus a failure gives the file of the wrapper test
      namespace and a line of the check file. Observed: a failing `is` on
      line 7 of a check file gives `FAIL in (zz-test) (zz_test.clj:7)`, and
      the wrapper file has 3 lines. Make the file and the line agree (for
      example, compile the expanded forms with the check source path, as
      `Compiler/load` with the check file name does, or report the check
      file in the failure). Keep test and assertion counts. Verify with a
      failing assertion in a temporary check namespace (not committed).
      Done: `define-checks` expands to `load-checks`, which evaluates each
      form with `Compiler/SOURCE` and `SOURCE_PATH` of the check file.
      Temporary check: `FAIL in (zz-test) (zz.clj:7)`.

## Implementation review follow-ups, sixth round

- [x] Push the branch and get a green CI run of `9a026d6`. The fifth-round
      follow-up changed `test/runtime/closed_record/checks/support.clj`
      (`load-checks`), which the ordinary and both value-type suites use.
      The last green run (`acd056f`, run 41880383270) comes before it, and
      the branch is 3 commits ahead of `origin`. Acceptance criterion 3
      needs the green run of the last commit that changes files outside the
      task directory. Push (approved at slice 6), read the run with the
      slice-6 `gh run` commands, and record the SHA, run URL and job
      conclusions in implementation.md as a new acceptance note.
      Done 2026-11-04: pushed `290f2a1` (contains `9a026d6`; later commits
      change task files only), run 41883488159, all 9 jobs `success`.

## Test review follow-ups

- [x] Remove `test/runtime/closed_record/subprocess_test.clj`. It tests
      `closed-record.subprocess`, which is test support code (a test of a
      test). The absent-var, does-not-load, VT-V5 and AOT tests use the
      helper; a time-out or a left directory shows there. Update the
      `*_test.clj` row of `ramora/defclosedrecord/embodiment/artifacts.md`
      (it names `subprocess_test.clj`).
- [x] Cover "the option works together with `:no-print` and `:load-ns`"
      (design, Public contract) for `:value-type true`. No test defines a
      value-type record with these options: `accepted-options-test` and
      `load-ns-option-test` are not shared, and the ordinary-suite
      `value-type-unavailable-test` checks only the error. Add a value-type
      suite test: a record with `:value-type true :no-print true` and one
      with `:value-type true :load-ns true` are working types (factories,
      `=`) with the expected representation; `:load-ns true` makes class
      initialisation require the namespace, as `load-ns-option-test` checks.
      Done: `representation-test/options-test`.
- [x] Remove the position check of the `require` preamble in
      `closed-record.value-type.representation-test/value-expansion-test`
      ("the expansion loads value-type.core first", `(nth expansion 2)`).
      The preamble is an implementation decision of slice 5, not a VT-V2
      item (Ramora VT-V2 row); `aot-test` covers its behaviour (classes
      load without the source). A test that fixes its place in the `let`
      prevents a change of that decision.

## Test review follow-ups, second round

- [x] Remove the two method-body shape checks of
      `closed-record.value-type.representation-test/value-expansion-test`:
      "hash forms compute on each call" (`hashCode` body ends in a
      `APersistentMap/mapHash` call, `hasheq` names `mapHasheq`) and
      "withMeta calls the constructor …" (body `=` `(new Q a b m __extmap)`).
      They are not VT-V2 items (Ramora VT-V2 row) and fix the form of the
      emitted code, so a change of that form (for example a helper call or
      `Q.`) fails them. The behaviour has other tests: "no generated
      reference to `__hash` or `__hasheq`" (no cache, VT-D6, VT-D7) and the
      shared `hash`, `assoc` and `metadata` checks (values). Keep test
      counts in implementation.md current.
      Done: value-type suite 82/312 (Temurin 21), 82/313 (Valhalla).

## Test review follow-ups, third round

- [x] Push the branch and get a green CI run of `f99ce54`. The test-review
      follow-ups (`fa5d2d6`, `f99ce54`) changed
      `test/value-type/closed_record/value_type/representation_test.clj`
      (new `options-test`) and removed `subprocess_test.clj`; the new
      `options-test` has run only locally. The last green run (`290f2a1`,
      run 41883488159) comes before these commits. Push (approved at
      slice 6), read the run with the slice-6 `gh run` commands, and record
      the SHA, run URL and job conclusions in implementation.md as a new
      acceptance note (criterion 3).
      Done 2026-11-04: pushed `14a3e33` (contains `f99ce54`; later commit
      changes task files only), run 41885774931, all 9 jobs `success`.
