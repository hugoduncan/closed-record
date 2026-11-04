# 020 Value-type records

## Intent

Implement the [value-type records design](../../../ramora/value-type-records.md).
With `:value-type true`, `defclosedrecord` defines a closed record through
`value-type.core/defvaluetype*`. The result is a JEP 401 value class on a
supported JVM and an identity class on other JVMs. The closed-record contract
does not change. Users who do not set the option get no new dependency, cost or
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
  `{:type :closed-record/value-type-unavailable :class-name …}` and the
  resolution failure as cause. A missing library, a load failure (such as on
  Clojure 1.11) and a value-type version without `defvaluetype*` all give this
  error.
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
ordinary expansion stays equal to the current expansion (VT-I2).

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
- A new test alias adds value-type at the pinned Git SHA (VT-R3) and a separate
  test directory. It is used with `:clj-1.12` (VT-K4). Because the base
  `:deps` select Clojure 1.11.4, the value-type alias alone does not select
  Clojure 1.12.6.
- The quality checks (format, lint, prose, API freshness, links) include the
  new test directory and the new documentation.

### Verification

Implement VT-V1 to VT-V8 as the Ramora verification plan states. Additional
rules:

- VT-V1 runs in the ordinary `test/runtime` suite on Clojure 1.11, 1.12 and
  1.13, without value-type on the class path. It also checks that the ordinary
  expansion does not load `value-type.core`.
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
- Where an ordinary check depends on an identity property (hash cache,
  `locking`, reference `identical?`), the value-type check asserts the
  differences table of the Ramora design, for the representation under test.
- VT-V4 also checks `.isValue`, `identical?` of equal records, and the absence
  of the hash fields. The check fails if the Valhalla job gives identity
  output: the job must not pass through silent fallback.
- VT-V5 runs in a JVM with `-Dvalue-type.fallback=error` on a JVM without value
  classes.
- VT-V7 compiles a value-type record namespace and loads the classes in a
  subprocess JVM of the same kind, once for each representation. It follows the
  value-type AOT rules. Loading value-output classes on another JVM is out of
  scope.
- VT-V8 is a manual, informational Criterium measurement of `hash` and `=`
  on value output against ordinary closed records. Use the existing bench
  structure. Add no new harness and no CI timing. Record the commands,
  runtime and results in `implementation.md`, and the result and its limits in
  `doc/performance.md`.

### CI

Embody VT-D10 in `ci.yml`:

- A required job `Value-type tests (Valhalla)`. It downloads the pinned
  archive from `download.java.net`, checks the SHA-256, keeps the archive in
  `actions/cache` with the SHA-256 as key, and runs the value-type suite with
  `JAVA_CMD` set to that JDK, `:clj-1.12` and `-J--enable-preview`. It also runs
  the value-output part of VT-V7.
- A Temurin 21 job, or a matrix entry, for the value-type suite on identity
  fallback (VT-V2, VT-V3, VT-V5, VT-V6, the identity part of VT-V7, and the
  Clojure 1.11 case of VT-V1).
- No `continue-on-error`. The release workflow runs the new jobs through
  `workflow_call` with no change.

### Documentation

- `README.md` and `doc/`: the option, the differences table, the runtime
  dependency on value-type, the Clojure 1.12.6 and JVM requirements, the hash
  cost, and no performance claim (VT-R4). value-type is unreleased: state how
  to add it.
- `doc/development.md`: the commands for the value-type suite on both JVMs.
- `doc/API.md`: regenerate it from the docstring.
- `ramora/value-type-records/embodiment.md`: replace "Not implemented" with the
  current embodiment and its verification limits (one Valhalla build, Linux
  x64, VT-V8 manual).
- `ramora/value-type-records/invariants-and-verification.md`: update VT-R1 and
  VT-R2 to the current facts (`defvaluetype*` exists; the feasibility gate
  observed `this(…)` constructors on `27-jep401ea3+1-1`), and the VT-V1 row
  (Verification above). Do not add progress history.
- `ramora/defclosedrecord/embodiment.md` and its artifact map: the value-type
  alias, suite and CI job, and the permitted dependencies of the Ramora
  value-type embodiment.
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
   task, and it does not load value-type (VT-V1).
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
