# Plan

## Approach

Implement the `:value-type` option in slices that keep VT-I2 (unchanged
ordinary expansion) verifiable at every point. Record the expansion fixture
from the unmodified emitter first, then change the emitter, then add tests,
dependencies, CI and documentation.

Key decisions:

- **Emitter shape**: a small representation description, selected once from
  the option in `emit`. It holds the four differences (type-form head, hidden
  fields, hash forms, `withMeta` form). The affected emitter functions receive
  the description as explicit data. This keeps the differences in one place
  and leaves the ordinary path a constant description, so the ordinary
  expansion stays unchanged (VT-I2). No dynamic var, no global state (VT-D1).
- **Expansion fixture first**: record the fixture from the emitter at the task
  start (same as `ac25942`), before any emitter change. The fixture comparison
  uses printed forms with `*print-meta*` true after gensym normalisation, as
  the design specifies. Fixture forms cover `:no-print`, `:load-ns`, field
  tags, user interfaces and methods, and destructured method parameters.
  - `test/runtime/closed_record/expansion_fixture.clj`, namespace
    `closed-record.expansion-fixture`: the input forms, the fixed namespace
    and the code. `forms` is a map of form name keyword to quoted
    `(closed-record.core/defclosedrecord …)` form. The forms use Java
    interfaces only, so `macroexpand-1` needs no other var. `with-option`
    inserts an option pair after the field vector of a form. `expansion`
    gives the normalised printed `macroexpand-1` of a form, with `*ns*`
    bound to `closed-record.expansion-fixture` (the fixed namespace).
    `record!` writes the fixture file.
  - `test/runtime/closed_record/expansion_fixture.edn`: the fixture file. It
    is an EDN sorted map of form name keyword to normalised printed
    expansion string. Tests read it with
    `(io/resource "closed_record/expansion_fixture.edn")`.
  - Record command, from the repository root (base `:deps`, Clojure 1.11.4):
    `clojure -M:test -e "(require 'closed-record.expansion-fixture) (closed-record.expansion-fixture/record!)"`.
    A change that intends to change the ordinary expansion runs this command
    again and commits the new file in the same change.
  - The VT-V1 tests of the ordinary suite are in
    `test/runtime/closed_record/value_type_option_test.clj`
    (`closed-record.value-type-option-test`). The fixture test compares
    `(expansion f)` and `(expansion (with-option f :value-type false))` with
    the entry of each form.
- **Test directory layout**: two new directories, both direct children of
  `test/`:
  - `test/value-type/` — the value-type suite (VT-V2 to VT-V7).
  - `test/value-type-clj-1.11/` — the Clojure 1.11 case of VT-V1.
  Neither is under `test/runtime/`, so ordinary `--dir test/runtime` commands
  and the value-type suite command do not scan the other.
- **Shared checks**: extract the ordinary runtime tests that check run-time
  behaviour of a defined record type (per the design's shared check selection
  rule) into shared check namespaces under `test/runtime/` (loadable without
  value-type). The ordinary suite and the value-type suite both use them.
  - Mechanism: macros, not functions. Each shared check namespace has one
    macro `define-checks`. It takes an option map
    `{:record-opts [k v …] :representation form}` and expands, in the
    calling namespace, the type definitions (with `record-opts` inserted
    after each field vector), the protocols, `defrecord` comparison types and
    values that the checks use, and the `deftest`s. Thus the type-bound
    checks (`->R`, `map->R`, `.a` interop, type hints, `instance?`) bind by
    name in the calling namespace. The expansion uses unqualified symbols for
    these names (quote or `~'`), not syntax-quote qualified symbols.
    Class-name strings and symbols (reader literal strings, the `pr-str`
    result, `(hash 'ns.R)`) are built at macroexpansion from
    `(namespace-munge *ns*)`. A form that a check builds and evaluates at run
    time also gets `record-opts`.
  - Representation: `:representation` is a form that the tests evaluate at
    run time. It gives `:identity` or `:value`. Only the D-06 serialization
    check uses it: it asserts when the value is `:identity`, and makes no
    assertion when it is `:value`. No other shared check branches (round-5
    and round-6 notes in implementation.md).
  - Paths: `test/runtime/closed_record/checks/<topic>.clj`, namespace
    `closed-record.checks.<topic>`, one for each ordinary test file that has
    selected tests. `<topic>` is the test namespace name without `-test`
    (for example `closed-record.checks.defclosedrecord`). Helpers that more
    than one check namespace uses (for example `thrown`, `data-of`,
    `round-trip`) are in `closed-record.checks.support`
    (`test/runtime/closed_record/checks/support.clj`).
  - Callers: each ordinary test namespace calls
    `(define-checks {:record-opts [] :representation :identity})` and keeps
    its tests that are not shared. The value-type suite has one test
    namespace for each shared check namespace,
    `closed-record.value-type.<topic>-test`
    (`test/value-type/closed_record/value_type/<topic>_test.clj`). It calls
    `(define-checks {:record-opts [:value-type true] :representation
    (expectation/representation)})`. One namespace for each is necessary,
    because two shared check namespaces can define types with the same name.
  - Expectation: `closed-record.value-type.expectation`
    (`test/value-type/closed_record/value_type/expectation.clj`).
    `representation` reads `closed-record.test.representation` and gives
    `:value` or `:identity`. It throws `ex-info` if the property is absent
    or has another value. Each value-type suite test namespace calls it when
    it loads, so a wrong property fails the suite.
  - Subprocess helper: `closed-record.subprocess`
    (`test/runtime/closed_record/subprocess.clj`). It runs `clojure.main`
    with given arguments in a child JVM: the `java` binary of `java.home`,
    the input arguments of the test JVM, extra JVM options, and the class
    path of the test JVM after extra class path entries. It gives
    `{:exit :out :err}`. The absent-var, does-not-load, VT-V5 and VT-V7
    tests use it. Because the child gets the test JVM input arguments, it
    also gets `--enable-preview` and `-Dclosed-record.test.representation`.
  - None of these namespace names ends in `-test`, so scry (default pattern
    `.*-test$`) does not run them as tests.
- **Alias**: one new alias `:value-type-test` in `deps.edn` with value-type at
  a pinned Git SHA and the two test directory paths. Base `:deps` unchanged.
  Add the alias to `closed-record.lint-dependencies/dependency-bases` so lint
  setup fetches the pin.
- **Error contract**: in value mode, `emit` calls
  `(requiring-resolve 'value-type.core/defvaluetype*)` inside try/catch.
  Throw → `ex-info :closed-record/value-type-unavailable` with the thrown
  exception as cause. `nil` → same `ex-info` with no cause. `:class-name` is
  the qualified class-name symbol (`classname` in `emit`).
- **Value-mode check in slice 3**: the alias arrives in slice 4, and `emit`
  resolves `defvaluetype*` before it builds the form (VT-D4). Slice 3 thus
  inspects the value-mode expansion in a local run with an uncommitted
  dependency:
  `clojure -Sdeps '{:deps {org.hugoduncan/value-type {:local/root "/Users/duncan/projects/hugoduncan/value-type/value-type-master"}}}' -M:test:clj-1.12 -e <form>`,
  where `<form>` prints the `macroexpand-1` of a `:value-type true`
  definition. First verify that the HEAD of that repository is `66d5ff7`. Record the
  command and the result in implementation.md. The committed check is VT-V2
  (slice 5).
- **VT-V7**: a test in the value-type suite,
  `closed-record.value-type.aot-test`. It runs in the suite command of each
  job, and its failures go to the upload of that suite step. It has no CI
  step of its own. The test writes the source of a namespace with a
  `:value-type true` record to `<tmp>/src` (`<tmp>` from
  `java.nio.file.Files/createTempDirectory`). Subprocess 1 compiles it with
  `(binding [*compile-path* "<tmp>/classes"] (compile '<ns>))`, with
  `<tmp>/src` and `<tmp>/classes` as extra class path entries. Subprocess 2
  has only `<tmp>/classes` as extra entry (no source). It requires the
  namespace from its `__init` class, and checks the record behaviour and
  the representation against the expectation. The compile is not in the
  test JVM, because `compile` needs `*compile-path*` on the class path and
  the load must not see the source.
- **VT-V8 bench**: a separate entry namespace `closed-record.bench.value-type`
  (`dev/bench/closed_record/bench/value_type.clj`) that no other namespace
  requires, with its own child command (as `default-child-cmd`, with
  `-m closed-record.bench.value-type <fork> <result-file>`), measuring both record kinds in the same child JVM. Use the criterium fork
  harness, not `dev/bench/valhalla/` probes. `default-child-cmd` already
  copies `java.home` and JVM input arguments to forks, so `--enable-preview`
  propagates.
  - `dev/bench/timing/run.sh` gets two environment variables. The arguments
    do not change: `"$@"` are main arguments, and no arguments give
    `-m closed-record.bench`.
    - `BENCH_ALIASES`: alias keywords that `run.sh` appends to `:bench`, for
      example `:value-type-test:clj-1.12`. Default empty. The Criterium JAR
      lookup (`clojure -Srepro -Spath -M:bench$BENCH_ALIASES`) and the run
      use the same aliases, so the JAR comes from the same class path.
    - `BENCH_JVM_OPTS`: Clojure CLI `-J` options, separated by spaces, for
      example `-J--enable-preview`. Default empty. Only the run gets them.
      An option that contains a space is not supported.
    - `JAVA_CMD` (Clojure CLI) selects the `java` binary. The child JVMs
      use the same binary through `java.home`.
  - VT-V8 command:
    `JAVA_CMD=<valhalla-jdk>/bin/java BENCH_ALIASES=:value-type-test:clj-1.12 BENCH_JVM_OPTS=-J--enable-preview sh dev/bench/timing/run.sh -m closed-record.bench.value-type`.
- **CI**: two new required jobs per VT-D10, with the pinned Valhalla archive
  (SHA-256 check, `actions/cache` keyed on the SHA-256) and per-step scry
  failure uploads with unique names.

## Risks

- **Prerequisite unmet (blocker for slice 4)**: value-type `66d5ff7`
  (contains `defvaluetype*`) is not on GitHub; origin/master is at `5d2656b`.
  Slices 1–3 can proceed (no committed value-type dependency). At slice 4
  (test alias), stop and ask the user per the design, unless the user has
  pushed by then.
- **Valhalla download flakiness**: `download.java.net` availability; mitigated
  by `actions/cache` keyed on the SHA-256.
- **Shared check extraction churn**: refactoring the ordinary runtime tests
  risks accidental behaviour change; mitigated by running the ordinary suite
  on Clojure 1.11/1.12/1.13 after the extraction, before any value-type use.
- **Gensym normalisation fragility**: the fixture comparison depends on the
  printed-form normalisation rule; verify it is stable across two expansions
  of the same form before recording the fixture.
- **value-type defect**: if found, record facts in implementation.md and stop
  for a user decision (out of scope to fix).
- **Subprocess tests** (absent-var stub, does-not-load check, VT-V5, VT-V7):
  subprocess JVM construction (java binary, input args, class path) must be
  shared helper code to avoid divergence (`closed-record.subprocess`).
- **Lint of macro-defined names**: clj-kondo does not see the vars that
  `define-checks` defines. Ordinary tests that are not shared and use these
  names can give unresolved-symbol findings. Resolve them in slice 4 with
  `.clj-kondo/config.edn` (for example a hook or `:lint-as`), not by copies.
- **scry load errors**: the expectation guard fails the suite through a
  namespace load error. Verify in slice 5 that scry 0.1.61 then exits
  non-zero.

## Slice order

1. **Expansion fixture** — `closed-record.expansion-fixture` (forms,
   normalisation, `record!`); record `expansion_fixture.edn` from the
   unchanged emitter; the VT-V1 fixture test in
   `closed-record.value-type-option-test`.
2. **Option and error contract** — `check-options` accepts `:value-type`;
   value checking; `:closed-record/value-type-unavailable` with correct cause
   semantics; ordinary-suite VT-V1 tests (option values, errors,
   missing-library cause, absent-var subprocess stub case).
3. **Emitter value mode** — representation description; VT-D3 to VT-D8
   differences; local value-mode inspection with an uncommitted `-Sdeps`
   dependency; fixture test still passes for no-option and
   `:value-type false`.
4. **Test alias and shared checks** — `:value-type-test` alias with the pinned
   GitHub SHA (**gate: requires user push**); lint-dependencies entry;
   shared check macros in `test/runtime/closed_record/checks/`; ordinary
   suite green on 1.11, 1.12, 1.13 without value-type.
5. **Value-type suite** — `test/value-type/`: representation expectation
   property handling; VT-V2, VT-V3, VT-V4, VT-V6; subprocess checks VT-V5 and
   does-not-load; VT-V7 AOT test; `test/value-type-clj-1.11/` with the
   Clojure 1.11 VT-V1 case. Local runs on Temurin 21 and Valhalla.
6. **CI** — the two required jobs, archive pin and cache, failure uploads,
   release workflow verified unchanged (`workflow_call` picks up new jobs).
7. **VT-V8 bench** — entry namespace, `run.sh` environment variables,
   manual Valhalla run; results in `implementation.md` and `doc/performance.md`; default
   `:bench` run verified unchanged.
8. **Documentation** — docstring, README, `doc/development.md`,
   `doc/tool-installation.md`, `doc/API.md` regeneration, Ramora value-type
   pages, defclosedrecord embodiment and artifact map, `mementum/state.md`.
9. **Quality gate** — format, lint, prose, API freshness, link, build and
   release checks.
