# 020 Value-type records

## Intent

Implement the [value-type records design](../../../ramora/value-type-records.md).
With `:value-type true`, `defclosedrecord` defines a closed record through
`value-type.core/defvaluetype*`. On a supported JVM the result is a JEP 401
value class. On other JVMs, value-type's fallback policy (the application
property `value-type.fallback`) selects an identity class or an error. The
closed-record contract does not change. Users who do not set the option get no new dependency, cost or
behaviour.

The Ramora design is the authority for the contract and the decisions
(VT-D1 to VT-D10). This task does not change those decisions. It makes the
embodiment, the verification (VT-V1 to VT-V8) and the documentation agree with
them.

## Problem

The value-type records design was written before value-type had
`defvaluetype*`. Its embodiment page says "Not implemented", and risks VT-R1
and VT-R2 say that `defvaluetype*` does not exist and that value-class record
constructors are not verified.

value-type now has `defvaluetype*` (value-type task 019). Its contract agrees
with the facts that the closed-record design uses (see Context). closed-record
has no option, no value-type tests and no Valhalla CI job. Thus no user can
define a value-type closed record, and no test shows that the closed-record
contract holds for one.

## Context

### Architecture

Use the architecture model (`~/projects/hugoduncan/architecture/ramora/INDEX.md`):
Purpose + Context → Architecture → Embodiment → Observed qualities.

- [Value-type records design](../../../ramora/value-type-records.md): purpose
  (O-VT1 to O-VT3), context (VT-K1 to VT-K6), public contract, differences table.
- [Decisions](../../../ramora/value-type-records/decisions.md): VT-D1 to VT-D9.
- [Invariants and verification](../../../ramora/value-type-records/invariants-and-verification.md):
  VT-I1 to VT-I5, VT-V1 to VT-V8, VT-D10, VT-R1 to VT-R5.
- [Embodiment](../../../ramora/value-type-records/embodiment.md): the elements
  that change and the permitted dependencies.
- [defclosedrecord design](../../../ramora/defclosedrecord.md): K-06 to K-08,
  D-01, D-07, D-17, D-20, D-21, I-01 to I-09, V-01 to V-15, the boundaries in
  [embodiment](../../../ramora/defclosedrecord/embodiment.md).

### value-type facts

From `~/projects/hugoduncan/value-type/value-type-master` at `66d5ff7`
(`ramora/deftype-star-design.md`, the `defvaluetype*` docstring):

- `(defvaluetype* tagname classname [fields*] opts+specs*)` takes the
  `deftype*` arguments and evaluates to the class.
- Record shape: the fields end with `__meta __extmap ^int __hash ^int __hasheq`,
  with no tag on `__meta` and `__extmap`. Mutable fields give
  `:mutable-field`. Other shapes give `:invalid-field :reason :record-shape`.
- Value output removes `__hash` and `__hasheq`. A captured class that reads or
  writes them gives `:invalid-field :reason :removed-field-access`. Identity
  output keeps both fields and does not check access.
- `:non-nil` and `:nil-restricted` field metadata give `:invalid-field`.
- The system property `value-type.fallback` (`silent`, `warn`, `error`)
  selects the fallback policy. The policy runs at macroexpansion.
- value-type tests Clojure 1.12.6, Java 8 and Valhalla `27-jep401ea3+1-1`.
  Its `:deps` select Clojure 1.12.6.
- The value-type release workflow pins the Valhalla archive
  `openjdk-27-jep401ea3+1-1_linux-x64_bin.tar.gz` with SHA-256
  `b8bdd7b181c6a5ea2dd9959255e222cd9d9a9f42cca4f2400991b9b2ff7ffb7d`.
- Local `master` is 119 commits ahead of `origin/master`. The commits that add
  `defvaluetype*` are not on GitHub (see Prerequisite).

The value-type feasibility gate observed that Valhalla `27-jep401ea3+1-1`
accepts a value-class constructor that calls `this(…)`, and that `create`,
both record literal forms and record code constants work in both
representations. This is the evidence for VT-R2.

### Probe observations

On 2026-11-03, a throwaway probe patched a copy of the closed-record emitter
(`ac25942`) outside the repository. In value mode it used `defvaluetype*`, the
VT-D5 layout, hash methods with no cache and `withMeta` with no hash fields.
value-type was a `:local/root` dependency at `66d5ff7`. The probe is evidence
only. It is not the implementation.

| Check | Valhalla `27-jep401ea3+1-1`, `--enable-preview`, Clojure 1.12.6 | Temurin `21.0.10+7`, Clojure 1.12.6 |
|---|---|---|
| `.isValue` | true | method absent |
| Instance fields | declared, `__meta`, `__extmap` | declared and all four hidden fields |
| `identical?` of equal records | true | false |
| `=`, `hash`, `assoc`, `with-meta`, keyword lookup, primitive field | as ordinary | as ordinary |
| Map and positional literals, code constant | work | work |
| `check` of a literal with extra keys, `assoc` of an undeclared key | `ex-info` `:type` as D-03 | same |
| `locking` on an instance | `IdentityException` | not run |
| `^:non-nil` field | `:invalid-field :reason :non-nil-unsupported` | same |
| `-Dvalue-type.fallback=error` | not run | `Error` at macroexpansion |

More observations:

- Without the `requiring-resolve` of VT-D4, the expansion fails with
  `ClassNotFoundException: value-type.core`. The emitter must load
  value-type before the compiler expands the emitted form.
- On Clojure 1.11.4, loading `value-type.core` fails (`No matching method
  maybeArrayClass … Compiler$HostExpr`), on both JVMs. Under VT-D4, every
  expansion with `:value-type true` on Clojure 1.11 must then give
  `:closed-record/value-type-unavailable`, with the load failure as cause.
  This agrees with VT-K4 and K-01: the option needs a Clojure version that
  value-type supports.
- The `getLookupThunk` `reify` (a companion class) and the `(let [] …)` wrapper
  of the expansion work with `defvaluetype*` with no change.

### Current embodiment

- `closed-record.core`: `split-opts+specs` gives an option map to
  `closed-record.impl.jvm/emit`.
- `closed-record.impl.jvm`: `check-options` accepts only `:no-print` and
  `:load-ns`. All options go to `deftype*`. The hidden fields have
  `^:unsynchronized-mutable` hash caches. `hasheq` and `hashCode` cache the
  hash. `withMeta` passes `__hash` and `__hasheq` to the constructor. All other
  generated methods are the same in both modes (VT-D8).
- `deps.edn`: Clojure 1.11.4. The `:test` alias runs `test/runtime` with scry,
  test.check and malli. `:clj-1.12` selects Clojure 1.12.6.
- CI (`ci.yml`): runtime tests on Clojure 1.11, 1.12 and 1.13 with Temurin 21,
  build, release, quality and link jobs. The release workflow calls CI.

## Prerequisite

VT-R3 requires a test dependency on value-type at a pinned Git SHA. CI can
fetch only a commit that is on GitHub. Before the implementation adds the test
alias, the user must push a value-type commit that contains `defvaluetype*`
(for example `66d5ff7`), or choose another reproducible source. The
implementation must not use a `:local/root` dependency in a committed alias.
If no commit is on GitHub, the implementation stops at the test alias and asks
the user.

## Scope

### Public contract

Implement the public contract of the Ramora design:

- `:value-type true` selects value-type. `:value-type false` and no option give
  the ordinary closed record. Other values give an `IllegalArgumentException`
  at macroexpansion.
- The option works together with `:no-print` and `:load-ns`. The option is not
  passed to `deftype*` or `defvaluetype*`.
- When `value-type.core/defvaluetype*` cannot be resolved, for any reason,
  macroexpansion fails with `ex-info`
  `{:type :closed-record/value-type-unavailable :class-name c}` and the
  resolution failure as cause. A missing library, a load failure (such as on
  Clojure 1.11) and a value-type version without `defvaluetype*` all give this
  error.
- `c` is the qualified class-name symbol that `deftype*` receives: the munged
  namespace name, a dot and the type name (`classname` in `emit`). For example,
  `Point` in the namespace `my-app.geo` gives `my_app.geo.Point`. It is a
  symbol, not a class and not a string: at macroexpansion no class exists.
  (The D-03 `:record` value is a class, because it is computed at run time.)
  VT-V1 asserts this value.
- value-type errors pass through with no change.
- The `defclosedrecord` docstring describes the option, the runtime dependency
  and the differences table, in brief.

### Emitter

Embody VT-D3 to VT-D8 in `closed-record.impl.jvm`. The value mode differs from
the ordinary mode only in:

1. the head of the type form: `value-type.core/defvaluetype*` in place of
   `deftype*`, with the same arguments (VT-D3);
2. the hidden field layout: `__meta __extmap ^int __hash ^int __hasheq`, with
   no mutable metadata (VT-D5);
3. `hasheq` and `hashCode`: the D-07 values, computed on each call (VT-D6);
4. `withMeta`: the constructor call with the declared fields, `__meta` and
   `__extmap`, as in `assoc` (VT-D7).

All other generated methods and both factories stay the same in both modes
(VT-D8). The emitter calls `requiring-resolve` only in value mode (VT-D4). The
ordinary expansion stays equal to the current expansion (VT-I2), as the VT-V1
expansion fixture defines (Verification).

The emitter receives the mode as explicit data. It must not use a dynamic var
or other global state: VT-D1 rejects a global selection. Choose between these
shapes in the plan:

- a mode flag that each affected emitter function receives;
- a small representation description (type-form head, hidden fields, hash
  forms, `withMeta` form) that the emitter selects once from the option.

Prefer the shape that keeps the four differences in one place and leaves the
ordinary expansion unchanged.

### Dependencies

- The base `:deps` do not change. No runtime or ordinary test alias adds
  value-type. `closed-record.core`, `closed-record.runtime` and the ordinary
  test suite do not load value-type.
- A new test alias adds value-type at the pinned Git SHA (VT-R3) and two
  separate test directories: the value-type suite and the Clojure 1.11 case of
  VT-V1 (Verification). It is used with `:clj-1.12` (VT-K4), except for the
  Clojure 1.11 case. Because the base `:deps` select Clojure 1.11.4, the
  value-type alias alone does not select Clojure 1.12.6.
- The VT-V8 code in `dev/bench` is in a separate entry namespace. It is the
  only `dev/bench` namespace that defines value-type records. No other
  namespace requires it: not `closed-record.bench`, not its fork and report
  layers, and no namespace that a test in `test/runtime` or `test/bench`
  loads. Only the VT-V8 command loads it.
- The default `:bench` run (`sh dev/bench/timing/run.sh` with no arguments:
  Clojure 1.11.4, V-11, the S-03 pass rule and exit code) and its V-11 result
  do not change. The `:test` alias, which has `dev/bench` on its class path,
  does not load value-type through `dev/bench`. This applies O-VT3 and VT-I2
  to the bench boundary.
- The quality checks (format, lint, prose, API freshness, links) include the
  new test directories and the new documentation.

### Verification

Implement VT-V1 to VT-V8 as the Ramora verification plan states. Additional
rules:

- VT-V1 runs in the ordinary `test/runtime` suite on Clojure 1.11, 1.12 and
  1.13, without value-type on the class path. There it checks the expansion
  with no option and with `:value-type false` against the expansion fixture
  (below), and the errors of `:value-type true` and of other values.
- Expansion fixture (VT-I2, acceptance criterion 2). The emitter uses
  `gensym`, so two expansions are not `=`. Thus:
  - Reference: a fixture file in `test/runtime`, recorded from the emitter at
    `ac25942` before any emitter change. (`src/` at the task start is the same
    as at `ac25942`.) It holds the expansions of a fixed set of
    `defclosedrecord` forms in a fixed namespace. The plan selects forms that
    include `:no-print`, `:load-ns`, field tags, user interfaces and methods,
    and destructured method parameters.
  - Comparison: the printed expansion with `*print-meta*` true, so that field
    tags and mutable metadata count, after gensym normalisation. The
    normalisation replaces each symbol with a name `prefix__N` or
    `prefix__N__auto__` (`N` digits) with `prefix__G<i>` or
    `prefix__G<i>__auto__`, where `<i>` is the order of first appearance in the
    printed form. The check passes if the strings are equal. The expansions
    with no option and with `:value-type false` must both equal the fixture.
  - A later intended change of the ordinary expansion records the fixture
    again in the same change.
- VT-V1 "does not load value-type" check. In the ordinary suite value-type is
  not on the class path, so a check there cannot fail. The check runs in the
  value-type suite, in a subprocess JVM that has not loaded value-type. The
  subprocess has the `java` binary, the JVM input arguments and the class path
  of the test JVM. The subprocess expands and evaluates a
  definition with no option and one with `:value-type false`, then checks
  that `value-type.core` is not in `(loaded-libs)`. Add this placement to the
  Ramora VT-V1 row.
- VT-V1 also has one check with value-type on the class path and Clojure 1.11:
  each expansion with `:value-type true`, not only the first, gives
  `:closed-record/value-type-unavailable` with a cause. Add this case to the
  Ramora VT-V1 row.
- VT-V2 inspects the expansion. Because identity output does not check access
  to the hash fields, VT-V2 is the check of VT-I4 on Temurin 21.
- VT-V3 and VT-V4 run the behaviour checks of V-01 to V-10, V-12, V-14 and V-15
  on types defined with the option. Use shared check definitions, not copies of
  the ordinary tests. The ordinary suite must stay runnable without value-type.
  No library switch selects the representation for a whole test run (VT-D1).
- Representation expectation. Each value-type suite command sets the JVM
  system property `closed-record.test.representation` to `value` (Valhalla
  job) or `identity` (Temurin 21 job). The value-type suite fails if the
  property is absent or has another value. The checks compare the observed
  representation with this expectation. JVM capability detection alone cannot
  find silent fallback, so the checks do not use it as the expectation. The
  property is test data only: closed-record and value-type do not read it,
  and it does not select the representation. Thus it is not a library switch
  (VT-D1).
- Where an ordinary check depends on an identity property (hash cache,
  `locking`, reference `identical?`), the value-type check asserts the
  differences table of the Ramora design, for the expected representation.
- VT-V4 also checks `.isValue`, `identical?` of equal records, and the absence
  of the hash fields. These checks compare with the expectation, so the suite
  fails if the Valhalla job gives identity output: the job must not pass
  through silent fallback.
- Checks that need their own JVM configuration:
  - VT-V5. The fallback policy is read once, when value-type loads, so
    `-Dvalue-type.fallback=error` cannot apply to one test in the suite JVM.
    A test in the value-type suite starts a subprocess JVM with the `java`
    binary, the JVM input arguments and the class path of the test JVM, and
    `-Dvalue-type.fallback=error`. The subprocess defines a value-type record.
    The test checks that the definition throws value-type's `Error`. The test
    starts the subprocess only when the expectation is `identity`, so VT-V5
    runs in the Temurin 21 job, with no separate command.
  - Clojure 1.11 case of VT-V1. It needs Clojure 1.11.4, so it cannot run in
    the value-type suite JVM. It is in its own test directory, with its own
    command and CI step in the Temurin 21 job. The value-type suite command
    does not scan that directory.
- Commands and aliases. A command sets the expectation with
  `-J-Dclosed-record.test.representation=…`.
  - Value-type suite, Temurin 21: `:test`, the value-type alias and
    `:clj-1.12`, with `closed-record.test.representation=identity`.
  - Value-type suite, Valhalla: the same aliases, with `JAVA_CMD` set to the
    pinned JDK, `-J--enable-preview` and
    `closed-record.test.representation=value`.
  - Clojure 1.11 case of VT-V1, Temurin 21: `:test` and the value-type alias,
    without `:clj-1.12`.
- VT-V7 compiles a value-type record namespace and loads the classes in a
  subprocess JVM of the same kind, once for each representation. It follows the
  value-type AOT rules. Each job runs VT-V7 for its expected representation,
  so the two jobs together cover both representations. Loading value-output classes on another JVM is out of scope.
- VT-V8 is a manual, informational Criterium measurement of `hash` and `=`
  on value output against ordinary closed records. Add no new harness and no
  CI timing. Record the commands, runtime and results in `implementation.md`,
  and the result and its limits in `doc/performance.md`.
  - Bench structure: the criterium fork harness of `closed-record.bench`
    (`dev/bench/closed_record/bench.clj`, `bench/fork.clj`,
    `bench/report.clj`: 5 child JVM forks, alternating run order, per-call
    statistics), started through `dev/bench/timing/run.sh`, which supplies
    the Criterium correction. VT-V8 does not use the Java probes in
    `dev/bench/valhalla/` (task 019).
  - The VT-V8 entry namespace (Dependencies) has its own child command and
    measures both record kinds in the same child JVM. The S-03 pass rule and
    its exit code do not apply to VT-V8.
  - Command: the aliases `:bench`, the value-type alias and `:clj-1.12`, with
    `JAVA_CMD` set to the Valhalla `27-jep401ea3+1-1` `java` and the JVM
    option `--enable-preview`. The child JVMs use the same `java` binary and
    JVM options. `run.sh` now fixes the alias `:bench`. The plan adds a way to
    give it these aliases and the JVM option, with no change to the default
    run.

### CI

Embody VT-D10 in `ci.yml`:

- A required job `Value-type tests (Valhalla)`. It downloads the pinned
  archive from `download.java.net`, checks the SHA-256, keeps the archive in
  `actions/cache` with the SHA-256 as key, and runs the value-type suite with
  the Valhalla command of Verification (`JAVA_CMD` set to that JDK,
  `:clj-1.12`, `-J--enable-preview`, expectation `value`). It also runs the
  value-output part of VT-V7.
- A Temurin 21 job, or a matrix entry, for the value-type suite on identity
  fallback (VT-V2, VT-V3, VT-V5, VT-V6, the identity part of VT-V7), with the
  Temurin 21 command of Verification (expectation `identity`). The Clojure 1.11
  case of VT-V1 is a separate step in this job, with its own command.
- No `continue-on-error`. The release workflow runs the new jobs through
  `workflow_call` with no change.

### Documentation

- `README.md` and `doc/`: the option, the differences table, the runtime
  dependency on value-type, the Clojure 1.12.6 and JVM requirements, the hash
  cost, and no performance claim (VT-R4). value-type is unreleased: state how
  to add it.
- `doc/development.md`: the commands for the value-type suite on both JVMs.
- `doc/API.md`: regenerate it from the docstring.
- `ramora/value-type-records.md`, Public contract: state the value of
  `:class-name` (the qualified class-name symbol, as Scope, Public contract
  states). This adds precision and does not change a decision.
- `ramora/value-type-records/embodiment.md`: replace "Not implemented" with the
  current embodiment and its verification limits (one Valhalla build, Linux
  x64, VT-V8 manual).
- `ramora/value-type-records/invariants-and-verification.md`: update VT-R1 and
  VT-R2 to the current facts (`defvaluetype*` exists; the feasibility gate
  observed `this(…)` constructors on `27-jep401ea3+1-1`), the VT-V1 row
  (Verification above), and the VT-D10 placement paragraph after VT-D10. That
  paragraph must name the identity part of VT-V7 for the Temurin 21 job, as
  the CI section does. Do not add progress history.
- `ramora/defclosedrecord/embodiment.md` must stay current (Ramora document
  rules). Update:
  - Allocation: the option check accepts `:no-print`, `:load-ns` and
    `:value-type`, and checks the value of `:value-type`.
  - Generated type, Method behaviour (`hasheq` and `hashCode` "Cached") and
    State (`__hash`, `__hasheq` mutable cache): state that these describe
    the ordinary mode, and point to the value mode in
    `ramora/value-type-records/embodiment.md`.
  - Boundaries: the permitted dependencies of the Ramora value-type
    embodiment.
  - Public surface: the Boundaries "Public" row (now "the `ex-data` of
    D-03") and the Public interface section (now "Error data: see D-03")
    also name the `:value-type` option and the `ex-data` of
    `:closed-record/value-type-unavailable`, or point to the Public contract
    in `ramora/value-type-records.md`. Do not add this `:type` to D-03: D-03
    is about key-set failures.
- `ramora/defclosedrecord/embodiment/artifacts.md`: the value-type alias
  (`deps.edn` row), the value-type test directories, the CI jobs (`ci.yml`
  row), and the VT-V8 entry namespace (`dev/bench/` row).
- `mementum/state.md`: current state after the task.

## Invariants and ownership

- value-type owns the representation, the fallback policy and the
  removed-field check. closed-record adds no capability check and no
  fallback (VT-D2, VT-D3).
- closed-record owns the option, the `:closed-record/value-type-unavailable`
  error, and the record methods. One method emitter serves both modes.
- VT-I1 to VT-I5 hold. VT-I2 (no change and no value-type load without the
  option) has the highest priority after the record contract.
- The hidden fields stay internal. No generated method reads or writes
  `__hash` or `__hasheq` in value mode.

## Out of scope

- A release of closed-record or value-type, and a change of the closed-record
  version. The option is an accretion (VT-K6). A release is a separate task.
- Changes to value-type. If the implementation finds a value-type defect, it
  records the facts and stops for a user decision.
- `:non-nil` fields, serialization of value output, and value-equality under
  fallback.
- ClojureScript and Babashka emitters (VT-D9 applies when they exist).
- Other Valhalla builds, other platforms, and Java 8 or Java 25 CI jobs.
- A change of the minimum Clojure version (K-01).
- Performance work or performance claims.

## Acceptance criteria

1. `:value-type true` defines a value class on Valhalla `27-jep401ea3+1-1` with
   `--enable-preview`, and an identity class on Temurin 21 with the default
   policy. Other option values and the unavailable cases give the errors of
   the public contract.
2. Without the option, the expansion is equal to the expansion before this
   task (the VT-V1 expansion fixture and comparison), and it does not load
   value-type (VT-V1).
3. VT-V1 to VT-V7 pass in CI. The ordinary runtime suite passes on Clojure
   1.11, 1.12 and 1.13 without value-type. The Valhalla job fails if it gets
   identity output.
4. `deps.edn` base `:deps` do not change. The value-type test alias pins a
   value-type Git SHA that is on GitHub.
5. The `Value-type tests (Valhalla)` job and the Temurin 21 value-type job are
   required jobs in `ci.yml`, with the pinned archive SHA-256 and the cache.
6. VT-V8 results are in `implementation.md` and `doc/performance.md`, with
   their limits.
7. The documentation of the Documentation scope is current. `doc/API.md` is
   fresh. The Ramora pages contain no progress history.
8. Format, lint, prose, API freshness, link, build and release checks pass.
