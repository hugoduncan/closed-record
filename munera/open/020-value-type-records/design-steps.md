# Design steps

- [x] Architecture: state the dependency boundary of VT-V8 in `dev/bench`.
  `dev/bench` is on the `:test` class path, and the default `:bench` run
  (Clojure 1.11.4, V-11, S-03 pass rule and exit code) has no value-type.
  The Dependencies section names only `closed-record.core`,
  `closed-record.runtime` and the ordinary test suite. Add: the VT-V8 code
  loads value-type only in the VT-V8 run (a separate entry namespace or
  case selection), the default `:bench` run and its V-11 result do not
  change, and the `:test` alias does not load value-type through
  `dev/bench`. This applies O-VT3 and VT-I2 to the bench boundary.
- [x] Architecture: complete the `ramora/defclosedrecord/embodiment.md`
  update so that the embodiment stays current (Ramora document rules).
  The Documentation scope names only the alias, suite, CI job and
  permitted dependencies. After the change these statements are also
  false or incomplete: the Allocation option check ("options other than
  `:no-print` and `:load-ns`"); the Generated type, Method behaviour
  (`hasheq`, `hashCode` "Cached") and State (`__hash`, `__hasheq` mutable
  cache) sections, which describe only the ordinary mode and must point
  to the value mode in `ramora/value-type-records/embodiment.md`; and the
  `dev/bench/` artifact row (VT-V8).
- [x] Ambiguity: state how a value-type check knows the representation that
  it must expect. The same shared checks run on Temurin 21 (identity) and on
  Valhalla (value). The differences-table assertions "for the representation
  under test", and the VT-V4 rule "the check fails if the Valhalla job gives
  identity output", need a source of truth. Detection of the JVM capability
  alone cannot catch silent fallback. Choose and record one: an explicit
  test-side expectation (system property or environment variable that the
  job sets), a separate value-output test namespace that only the Valhalla
  command runs, or `-Dvalue-type.fallback=error` in the Valhalla job. A
  test-side expectation is not a library switch (VT-D1); state this.
- [x] Ambiguity: state how the checks that need their own JVM configuration
  run. VT-V5 needs `-Dvalue-type.fallback=error`, which breaks the identity
  checks of VT-V3 in the same JVM. The Clojure 1.11 case of VT-V1 needs
  value-type on the class path with Clojure 1.11.4, where every other
  value-type check fails. For each case, state if it runs in a subprocess
  JVM from a test, or as a separate test directory or namespace with its own
  command (and CI step), and state the alias combination of each command.
- [x] Ambiguity: define "equal to the current expansion" (VT-I2, acceptance
  criterion 2). The emitter uses `gensym` (`p__`, `this__`, `c#`, `h#`), so
  two expansions are not `=`. State the reference (for example, a fixture
  recorded from the emitter at `ac25942`, or the expansion of no option
  against `:value-type false`) and the comparison (for example, equality
  after gensym normalisation). Also state where the "does not load
  `value-type.core`" check runs: in the ordinary suite value-type is not on
  the class path, so the check there cannot fail; the Temurin value-type job
  has value-type on the class path.
- [x] Ambiguity: name "the existing bench structure" for VT-V8. Two
  structures exist: the criterium fork harness `closed-record.bench`
  (`:bench` alias, 5 forks, S-03 pass rule, exit code) and the Java probes
  in `dev/bench/valhalla/` (task 019). Name the one to use and the alias
  combination of the VT-V8 command (value-type, `:clj-1.12`, Valhalla
  `JAVA_CMD`, `--enable-preview`).
- [x] Inconsistency: the CI section puts the identity part of VT-V7 in the
  Temurin 21 value-type job. The VT-D10 placement paragraph in
  `ramora/value-type-records/invariants-and-verification.md` names only
  VT-V1, VT-V2, VT-V3, VT-V5 and VT-V6 for Temurin 21 jobs, and only the
  value-output part of VT-V7 for the Valhalla job. Add this paragraph to the
  Documentation update list for that page, so that it names the identity
  part of VT-V7 in the Temurin 21 job.
- [x] Architecture: add the public surface of the option to the
  `ramora/defclosedrecord/embodiment.md` update (Ramora document rules,
  Principle 6 explicit boundaries). After the change, the Boundaries
  "Public" row ("the `ex-data` of D-03") and the Public interface section
  ("Error data: see D-03") are incomplete. The `:value-type` option and the
  `ex-data` of `:closed-record/value-type-unavailable` are public, and D-03
  does not include that `:type`. Add to the Documentation list: the Public
  row and the Public interface section name the option and this error, or
  point to the public contract in `ramora/value-type-records.md`.
- [x] Ambiguity: state the value of `:class-name` in the
  `:closed-record/value-type-unavailable` `ex-data`. The design and the
  Ramora public contract give only the key (`:class-name …`). The data is
  public API (as D-03 `:type` values and keys are), and VT-V1 must assert
  it. At macroexpansion no class exists. The emitter has the munged,
  qualified symbol `classname` (`ns.Name`) and its string `cname-str`
  (`src/closed_record/impl/jvm.clj`, `emit`). Choose one (for example, the
  qualified class-name symbol, as `deftype*` receives it), state it in
  design.md, and add it to the public contract in
  `ramora/value-type-records.md`.
- [x] Inconsistency: the Intent says the result is "an identity class on
  other JVMs". The public contract (value-type errors pass through), VT-V5
  (`-Dvalue-type.fallback=error` gives value-type's `Error`) and the
  Summary of `ramora/value-type-records.md` ("value-type's application
  policy selects an identity class or an error") say that on other JVMs the
  result is an identity class or an error. Change the Intent to agree, for
  example: "an identity class or an error, as value-type's fallback policy
  selects, on other JVMs".
- [x] Ambiguity: state the cause of `:closed-record/value-type-unavailable`
  when `value-type.core` loads but has no `defvaluetype*`. The public
  contract says "the resolution failure as cause" for every case, including
  "a value-type version without `defvaluetype*`". In that case
  `requiring-resolve` returns `nil` and throws nothing (observed:
  `(requiring-resolve 'clojure.set/no-such-var)` gives `nil`), so no
  resolution failure exists. Choose one and state it in the Public contract
  of design.md: no cause (`ex-cause` is `nil`) in this case, or a cause that
  the emitter makes (state its class and message). A missing library and a
  load failure throw, and that exception is the cause.
- [x] Inconsistency: the CI section adds the `Value-type tests (Valhalla)`
  job, which runs Clojure on the Valhalla JDK. After this change, two
  statements in `doc/` are false: `doc/development.md` (Continuous
  integration: "Clojure jobs use Temurin Java 21") and
  `doc/tool-installation.md` (Java and Clojure CLI: "CI uses Temurin Java
  21"). The Documentation item for `doc/development.md` names only the
  commands, and no item names `doc/tool-installation.md`, but acceptance
  criterion 7 requires current documentation. Add both statements to the
  Documentation list: the Valhalla job uses the pinned Valhalla build, and
  the other Clojure jobs use Temurin Java 21.
- [x] Architecture: complete the `ramora/defclosedrecord/embodiment/artifacts.md`
  update so that the artifact map stays current (Ramora document rules). The
  Documentation item names only the `deps.edn`, `ci.yml` and `dev/bench/`
  rows and the value-type test directories. After the change these rows are
  also false or incomplete: `src/closed_record/impl/jvm.clj` ("Builds the
  `deftype*` form"; in value mode it builds the `defvaluetype*` form and
  resolves value-type, VT-D4), and the `test/runtime` rows, which do not
  name the VT-V1 selection tests (ordinary suite, absent-var subprocess),
  the expansion fixture file (not a `*_test.clj` file) and the shared check
  definitions of VT-V3 and VT-V4 if they are in `test/runtime`. Add these
  rows to the Documentation item for that page.
- [x] Inconsistency: the CI section does not say if the new scry steps
  upload test failures. Every existing scry step in `ci.yml` uploads
  `.scry-results/` on failure with its own artifact name, the `ci.yml` row
  of `ramora/defclosedrecord/embodiment/artifacts.md` names "structured
  test-failure uploads", and `doc/development.md` (Continuous integration)
  lists each artifact name (`scry-runtime-1.11` … `scry-tooling`) and says
  "The runtime matrix runs the three test commands above". Make the CI
  section agree: the Valhalla suite step, the Temurin 21 suite step and
  the Clojure 1.11 VT-V1 step each upload failures with a unique artifact
  name (state the names). The matrix-entry option must not reuse
  `scry-runtime-${{ matrix.version }}` (version 1.12 exists; duplicate names
  fail in `upload-artifact` v4). Add the new names, and the runtime-matrix
  sentence if a matrix entry is used, to the Documentation item for
  `doc/development.md`.
- [x] Ambiguity: state which ordinary tests the shared VT-V3/VT-V4 checks
  include, and the expectation for serialization on value output. The
  ordinary suite does not map one-to-one to V-01 to V-15.
  `test/runtime/closed_record/defclosedrecord_test.clj` has
  `serializable-test` (labelled D-06, an `ObjectOutputStream` round trip)
  and a section "Generated methods not in the V-01 tables" (`isEmpty`,
  zero fields, `containsValue`, ...). The differences table says
  serialization is "as `defrecord`" on identity fallback and "not
  promised" on value output, and Out of scope excludes serialization of
  value output. The design rule for identity properties (hash cache,
  `locking`, reference `identical?`) does not cover serialization, and "not
  promised" cannot be asserted. State the selection rule (for example: all
  behaviour tests of the ordinary runtime suite except V-11, V-13 and the
  emitter or expansion tests), and state that the serialization check runs
  on identity fallback and does not run on value output (or another stated
  expectation).
- [x] Inconsistency: the Documentation item for `doc/development.md` does
  not name its list of test suites. `doc/development.md` (lines 7-16) says
  "Test suites share the `test/` parent directory", lists the five suites
  (`test/runtime/` … `test/bench/`), and says "Each suite has a separate
  source path. Select the suite directory explicitly". The design adds two
  test directories (the value-type suite and the Clojure 1.11 case of
  VT-V1) with their own commands. After the change, the list is incomplete,
  and acceptance criterion 7 requires current documentation. Add to the
  Documentation item for `doc/development.md`: the suite list names both
  new directories and their value-type dependency requirement. The
  existing format and lint scope (all of `test/`) also assumes that both
  directories are under `test/`; state this as a constraint on the paths
  that the plan selects.
- [x] Plan ambiguity: state how the shared VT-V3/VT-V4 checks get the
  record types that they test. The ordinary tests use types defined at the
  top level (`defclosedrecord R [a b]` in
  `test/runtime/closed_record/defclosedrecord_test.clj` and other files) and
  bind to them by name: `->R`, `map->R`, reader literals with the class name,
  `.a` field interop, `instance?`. Plan "both suites call them" suggests
  functions, which cannot express the literal and interop checks. Choose
  and record in plan.md: for example, a macro that takes the record options
  and expands the type definitions and the `deftest`s in the calling
  namespace, or check functions over a descriptor of factories (state how
  the type-bound checks run then). Also state how a check gets the
  representation (D-06 serialization on identity only), the namespace names
  and paths of the shared check and subprocess-helper namespaces (in
  `test/runtime`, loadable without value-type, names that do not match the
  scry discovery pattern `.*-test$`).
- [x] Plan ambiguity: state the path, format and record procedure of the
  expansion fixture file. Plan and steps say only "a fixture file in
  `test/runtime`". The design requires a re-record in the same change after
  an intended expansion change, and an artifact-map row for the file. State:
  the file path; the content (for example an EDN map of form name to
  normalised printed expansion, or forms and expansions together); where the
  input forms live (test file or fixture file); and how to record it again
  (a function or a command).
- [x] Plan ambiguity: state the `dev/bench/timing/run.sh` argument syntax for
  extra aliases and JVM options. Now `"$@"` are main args after
  `-M:bench:corrected-timing` (`run.sh -m <ns>` selects an entry), and no
  arguments select `-m closed-record.bench`. The script also finds the
  Criterium JAR with `clojure -Srepro -Spath -M:bench`. State how the caller
  gives aliases and `-J` options (for example environment variables such as
  `BENCH_ALIASES` and `BENCH_JVM_OPTS`, or leading arguments before `--`),
  whether the JAR lookup uses the same aliases, and the full VT-V8 command.
- [x] Plan ambiguity: state where VT-V7 runs and where it compiles. Slice 5
  puts VT-V7 in the `test/value-type/` suite. Slice 6 says the Valhalla job
  runs "the Valhalla suite command and the value-output VT-V7", which can
  mean a separate step (that would need its own upload name). State that
  VT-V7 runs inside the suite command (or name its step, command and
  artifact). Also state where the compile happens (test JVM or subprocess)
  and the compile output path (for example a temporary directory on the
  subprocess class path).
- [x] Plan ambiguity: state how slice 3 verifies value mode. Slice 3 asks to
  verify that no value-mode method reads or writes `__hash`/`__hasheq`, but
  the alias arrives in slice 4 and `emit` resolves `defvaluetype*` before it
  builds the form (VT-D4), so the value-mode expansion fails without
  value-type. Choose: code reading only, a local run with an uncommitted
  `-Sdeps` value-type dependency, a check of a form builder that runs after
  resolution, or defer the check to VT-V2 in slice 5.
- [x] Plan inconsistency: steps.md slice 5 VT-V6 names a `:mutable-field`
  pass-through case. `defclosedrecord` cannot reach it:
  `check-field-metadata` in `src/closed_record/impl/jvm.clj` throws
  `IllegalArgumentException` for `:volatile-mutable` and
  `:unsynchronized-mutable` fields before `emit` builds the form
  (`argument-errors-test` asserts this). The Ramora VT-V6 row names only
  `:non-nil` metadata → `:value-type/error :invalid-field`. Make the VT-V6
  cases agree with the Ramora row: use `:invalid-field` cases that the
  emitter can reach (`^:non-nil` or `^:nil-restricted` field; optionally a
  user method that reads `__hash`, which gives `:removed-field-access` on
  value output only). Remove the `:mutable-field` case.
- [x] Plan ambiguity: state how the VT-V8 entry runs the forks and reports
  the result. Plan and steps say only "its own child command" and "via the
  criterium fork harness". The harness parent and transport are fixed to
  S-03: `fork/parent` prints `report/table` of `report/fork-stats` and
  returns `report/exit-code` (the S-03 pass rule); `legacy-transport/encode`
  and `decode` and `report/decode-result` accept only the six `report/ops`
  entries; `fork/run-fork` is private. VT-V8 measures `hash` and `=` only,
  and the S-03 pass rule and exit code do not apply (design). State: the
  parent loop (reuse `fork/parent`, or a VT-V8 loop in the entry namespace);
  the child result format and its validation (including the collector
  identity of `timing-tool/observe`); the output (table columns, ns per
  call, ratio, no pass rule); the exit code; and which `dev/bench`
  namespaces change, if any (for example `run-fork` made public, or `ops`
  as a parameter). A change to `fork`, `report` or `legacy-transport` must
  keep the default `:bench` run and its tests unchanged (VT-I2).
- [x] Plan inconsistency: the `run.sh` change has no test step. The
  plan-review design-steps notes in implementation.md say that
  `test/bench/closed_record/timing_launcher_test.clj` calls
  `run.sh -e …` (arguments as main arguments) and "run `test/bench` after
  the `run.sh` change". Steps slice 7 checks only the default run, and slice
  9 does not name `test/bench`. `test/bench` has no CI job and no command in
  `doc/development.md`. Add to slice 7 a step that runs the `test/bench`
  launcher test (state the command, for example
  `clojure -M:test:bench -m scry.cli --dir test/bench`, after checking the
  aliases that the suite needs) with `BENCH_ALIASES` and `BENCH_JVM_OPTS`
  unset.
- [x] Plan inconsistency: the Clojure CLI version of the new CI jobs.
  `doc/development.md` line 53 says "Clojure jobs use Temurin Java 21 and
  Clojure CLI `1.12.6.1673`". The "Notes after round 6" in
  implementation.md say to keep the Clojure CLI clause when the Temurin
  statement is replaced, but steps slice 8 (`doc/development.md` item) does
  not say this, and steps slice 6 does not say that both new jobs install
  Clojure CLI `1.12.6.1673` with `DeLaGuardo/setup-clojure@13.4`, as the
  other jobs do. Add both: slice 6 pins that CLI version in both new jobs
  (the Valhalla job sets `JAVA_CMD` and needs no Temurin), and slice 8
  keeps the CLI clause for all Clojure jobs.
- [x] Plan ambiguity: state the value-type coordinate of the
  `:value-type-test` alias. Plan and steps say only "value-type at a pinned
  Git SHA". The lib name `org.hugoduncan/value-type` does not let
  tools.deps infer a URL (only `io.github.*` names do), so the alias needs
  an explicit `:git/url`. The local value-type `origin` is the SSH URL
  `git@github.com:hugoduncan/value-type.git`; CI and lint setup have no SSH
  key. `https://github.com/hugoduncan/value-type.git` is public (anonymous
  `git ls-remote` works). State: the lib name, the `:git/url` (HTTPS), and
  the SHA rule at the slice-4 gate (pin `66d5ff7` when it is on GitHub, or
  the commit that the user names; a later commit needs a check that the
  value-type facts of design.md still hold).
- [x] Plan ambiguity: state how slice 6 pushes and confirms CI. Steps say
  only "Push and confirm both jobs pass in CI". Local `master` is 41
  commits ahead of `origin/master`, CI runs on pushes to `master` and on
  pull requests, and earlier tasks say "do not push solely for evidence"
  (tasks 014, 017). Acceptance criterion 3 needs the jobs to pass in CI.
  State: the ref to push (`master`, or a branch with a pull request), if
  the push needs user approval first (stop and ask, as at the slice-4
  gate), how the result is read (for example `gh run watch`), and what to
  record in implementation.md.
- [x] Plan ambiguity: name the local JDKs and the VT-V8 platform. Slice 5
  says "Local runs on Temurin 21 and Valhalla (the Valhalla run requires the
  pinned JDK)", and the VT-V8 command has `<valhalla-jdk>`. The pinned
  archive is Linux x64; this machine is macOS arm64 and cannot run it. The
  machine has Temurin `21.0.10+7` and the macOS aarch64 build of Valhalla
  `27-jep401ea3+1-1` (mise, `~/.local/share/mise/installs/java/`), and the
  default `java` is 25. Slice 7 records the VT-V8 limits as "one Valhalla
  build, Linux x64", which is false for a local run. State: the `JAVA_CMD`
  of each local run (slice 5 and VT-V8), that a local Valhalla run uses the
  same build on another platform (not the pinned archive), and the platform
  that the VT-V8 limits name (the machine of the run, as in
  `doc/performance.md`).
- [x] Plan ambiguity: state if the commits after the slice-6 CI run go to
  CI. Slice 6 pushes local `master` to the pull request ref and reads one
  green run. Slices 7 to 9 then commit more changes: `src` (the
  `defclosedrecord` docstring, slice 8), `dev/bench`, `doc/`, Ramora and
  quality fixes, which can touch `test/value-type` or `ci.yml` (slice 9).
  The plan pushes again only after a failed run. Thus the pull request head
  and the CI evidence of acceptance criterion 3 are for an earlier commit
  than the final one, and the quality job (lint, prose, links, API
  freshness) does not see the new files in CI. Slice 9 says "Verify all
  acceptance criteria 1–8" but does not say which commit the AC3 run must
  be for. Choose and state in plan.md and steps.md: (a) slice 9 pushes the
  final commit to the same ref (the user approved the ref at slice 6, or
  stop and ask again) and records a second green run; or (b) the slice-6 run
  is the AC3 evidence, and slice 9 records the commits after it and why no
  push is necessary. If (b), state which local checks replace the CI jobs
  for the changed files.
- [x] Plan inconsistency: the slice-8 item for
  `ramora/value-type-records/embodiment.md` names only the Status ("Not
  implemented") and the verification limits. The design-review round-5
  note in implementation.md says that the whole page must change, and that
  three more statements become false: the Elements table row
  `.github/workflows/ci.yml` ("The Valhalla job of VT-D10"; the
  `Value-type tests (Temurin 21)` job and the failure uploads are absent),
  the Tests row ("VT-V1 runs in the ordinary `:test` alias"; VT-V1 now has
  three placements: ordinary suite with the absent-var subprocess,
  value-type suite subprocess, Clojure 1.11 directory) and the Verification
  limits text "Until the job exists". Carry this note into the slice-8
  step of steps.md (the active surface), as round 2 did for the earlier
  notes.
- [ ] Plan ambiguity: state how the expansion fixture treats the reader
  position metadata of its input forms. plan.md (Expansion fixture first)
  puts `forms` as quoted forms in
  `test/runtime/closed_record/expansion_fixture.clj` and compares printed
  expansions with `*print-meta*` true. The reader gives each list in a
  source file `:line` and `:column` metadata. The emitter copies user
  method bodies into the expansion, so this metadata is in the printed
  string. Observed (Clojure 1.11.4, current emitter): a quoted
  `(defclosedrecord P [a b] Object (toString [_] (str (inc a))) …)` in a
  loaded file gives `^{:line 2 :column …}` three times in the printed
  `macroexpand-1`. (The round-3 note "a quoted form … gives no `:line`
  metadata" checked a form with no nested list.) Thus an edit that moves a
  form in that file (a new form, or a `cljfmt` fix in slice 9) changes the
  fixture strings with no emitter change. A record again after slice 3 then
  records the changed emitter, not `ac25942`, so VT-I2 has no reference.
  Choose and state in plan.md and steps.md (slice 1): for example, remove
  `:line` and `:column` (and `:file`) from the input forms before
  expansion, or keep each form as a string and read it with `read-string`
  (no position metadata). Also state that `with-option` keeps this rule.
