# Value-type records invariants and verification

## Invariants

Assertion records in the shape of the defclosedrecord
[invariants](../defclosedrecord/invariants.md). `T` is a type defined with
`:value-type true`. `r` is an instance of `T`.

```text
id: VT-I1
subject: I-01 to I-09
predicate: hold for
object: T, on value output and on identity fallback
scope: all instances
basis: VT-D3, VT-D5, VT-D8
enforced-by: the shared method emitter; compiler-generated create and constructors
verified-by: VT-V3, VT-V4
confidence: medium (value output depends on VT-R1 and VT-R2)
```

```text
id: VT-I2
subject: the expansion of defclosedrecord without :value-type true
predicate: equals
object: the expansion of the ordinary design, and does not load value-type
scope: all types
basis: O-VT3, VT-D4
enforced-by: the emitter resolves value-type only in value mode
verified-by: VT-V1
confidence: high
```

```text
id: VT-I3
subject: the expansion with :value-type true
predicate: contains no deftype* outside
object: value-type.core/defvaluetype*
scope: all types
basis: VT-D3
enforced-by: the JVM emitter
verified-by: VT-V2
confidence: high
```

```text
id: VT-I4
subject: generated methods and factories of T
predicate: do not read or write
object: __hash, __hasheq
scope: all methods
basis: VT-D6, VT-D7
enforced-by: the emitter; value-type :removed-field-access check
verified-by: VT-V2, VT-V4
confidence: high
```

```text
id: VT-I5
subject: :value-type true with value-type not resolvable
predicate: fails at macroexpansion with
object: :closed-record/value-type-unavailable
scope: all hosts
basis: VT-D2, VT-D9
enforced-by: the emitter
verified-by: VT-V1
confidence: high
```

```text
id: VT-I6
subject: :non-nil field metadata in the expansion with :value-type true
predicate: occurs unchanged only in
object: the defvaluetype* field vector; the rest of the expansion equals the expansion without :non-nil
scope: all types with :value-type true
basis: VT-D11
enforced-by: the JVM emitter puts field metadata only in the defvaluetype* field vector
verified-by: VT-V2
confidence: high
```

## Verification plan

| Id | Item | Check |
|---|---|---|
| VT-V1 | Selection | Ordinary test JVM, without value-type: no option and `:value-type false` give an expansion equal to the recorded expansion fixture (printed with `*print-meta*` true, gensyms normalised); `:value-type true` fails with `:closed-record/value-type-unavailable`, `:class-name` and the resolution exception as cause; other values give `IllegalArgumentException`. Absent-var case, in a subprocess JVM with a stub `value-type.core`: the error has no cause. Value-type suite, in a subprocess JVM: an ordinary record does not load value-type. Clojure 1.11 with value-type: `:value-type true` fails with the load failure as cause. |
| VT-V2 | Expansion | With value-type: `defvaluetype*` replaces `deftype*`; immutable `^int` hash fields; no generated reference to `__hash` or `__hasheq`; the option is not passed to `defvaluetype*`. VT-I6: expand a form with `:non-nil` fields and the same form without the metadata (`macroexpand-1` in the fixture namespace, position metadata removed). (a) Each `:non-nil` field symbol in the `defvaluetype*` field vector carries `:non-nil true` unchanged, and no other place in the expansion carries `:non-nil`. (b) With `:non-nil` removed from those symbols, the printed expansion (`*print-meta*` true, normalised) equals the printed expansion of the form without `:non-nil`. |
| VT-V3 | Identity fallback | Run V-01 to V-10, V-12, V-14 and V-15 on value-type records, on Temurin 21 with `value-type.fallback=silent` (the default). The shared checks are the ordinary behaviour tests, except tests of options, of argument errors, of class reflection and of the compiler rules. The serialization round trip runs on identity fallback only. |
| VT-V4 | Value output | Run VT-V3 on Valhalla `27-jep401ea3+1-1` with `--enable-preview`. Also check `.isValue`, `identical?` of equal records, and that the hash fields are absent. |
| VT-V5 | Policy | With `value-type.fallback=error` on a JVM without value classes, definition throws value-type's `Error`. |
| VT-V6 | Errors | value-type errors pass through unchanged: a `:non-nil` field with no tag gives `:value-type/error :invalid-field` with `:reason :missing-tag`, as the cause of the macroexpansion failure. |
| VT-V7 | AOT | Compile a value-type record namespace, then load it in a subprocess JVM, for each representation. |
| VT-V9 | Non-nil fields | Fixture types: a value record `Inner`, and a record with a `^Inner ^:non-nil` field, a `^String ^:non-nil` field and a field with no metadata. Field storage types are the tag classes; the field with no metadata is `Object`. On value output, the record class is a value class. Valid values construct through `->Name`, `map->Name`, `assoc` of each declared key, the map literal and the positional literal; the fields read back by keyword at a call site (the lookup thunk), by `get` and by field access; `with-meta` keeps the fields. For each non-nil field, nil and a wrong type give `NullPointerException` and `ClassCastException` in the cause chain, through `->Name`, `map->Name` (nil value and missing key), `assoc`, the map literal and the positional literal. The field with no metadata accepts nil on each path. Key-set errors come first: `assoc` of an undeclared key, `dissoc` of a non-nil field, and `map->Name` with an extra key and a missing non-nil key give the closed-record `ex-info` types. `check` returns a valid record. The lookup thunk read detects the `c366f15` stack map defect (value-type), which failed the definition of such a record on Valhalla. |
| VT-V10 | Ordinary mode | Ordinary suite (Clojure 1.11, 1.12, 1.13): a closed record with a `^String ^:non-nil` field and no option accepts nil on `->Name`, `map->Name` and `assoc`. The expansion fixture (VT-V1, VT-I2) has an ordinary form with a `^String ^:non-nil` field and no option. |
| VT-V8 | Performance | criterium: `hash` and `=` of value-type records against ordinary closed records. Informational. It is not a pass criterion. |

## VT-D10 Value output on CI

- Subject: where VT-V4 runs.
- Choice: a CI job, `Value-type tests (Valhalla)`, in the `ci.yml` workflow. Thus the
  release workflow also runs it, through `workflow_call`.
  - Download `openjdk-27-jep401ea3+1-1_linux-x64_bin.tar.gz` from
    `download.java.net` and check its SHA-256
    (`b8bdd7b1…ffb7d`, the same pin as the value-type release workflow).
  - Keep the archive in `actions/cache`, with the SHA-256 as the key.
  - Run the value-type tests with `JAVA_CMD` set to that JDK, the
    `:clj-1.12` alias (Clojure 1.12.6, VT-K4) and `-J--enable-preview`.
  - The job is required, as the other test jobs are.
- Rejected:
  - Manual runs only. Value output then has no regression signal, and a
    release can break it with no failure.
  - A job that can fail with no effect (`continue-on-error`). The failure is
    then easy to miss.
  - A Docker image or a copy of the JDK in the repository. More maintenance,
    and the JDK licence terms then apply to the copy.
- Rationale: VT-I1 for value output depends on VT-R1 and VT-R2. Only a test
  on the Valhalla JVM finds those failures. The value-type release workflow
  shows that the download works on GitHub runners.
- Assumptions: the pinned build stays available, or in the cache (VT-R5).
- Qualities: + verifiability. − CI downloads about 250 MB when the cache is
  empty. A closed-record release depends on an early-access download.
- Scope: CI.
- Commitment: medium. Change the pin together with value-type.

Both value-type jobs (`Value-type tests (Temurin 21)` and
`Value-type tests (Valhalla)`) run all of `test/value-type`:

- VT-V2, VT-V6 and VT-V9 run in both value-type jobs.
- VT-V3 runs in the Temurin 21 value-type job. VT-V4 (VT-V3 on value
  output) runs in the Valhalla job.
- VT-V5 runs only in the Temurin 21 value-type job.
- VT-V1: the ordinary part runs in the runtime jobs, the value-type suite
  part in both value-type jobs, and the Clojure 1.11 part in the Temurin 21
  value-type job.
- VT-V10 runs in the runtime jobs (Clojure 1.11, 1.12 and 1.13).
- The identity part of VT-V7 runs in the Temurin 21 value-type job, and the
  value-output part in the Valhalla job. VT-V8 runs manually.

## Risks

- VT-R1 The `defvaluetype*` contract can change before the value-type
  release. Likelihood: medium. Effect: VT-D3, VT-D5, VT-D11 and VT-I6
  change. Signals: VT-V2, VT-V6, VT-V9.
- VT-R2 JEP 401 rejects value-class constructors that call `this(…)`. The
  compiler-generated record constructors do this. Likelihood: low. Effect:
  value output is not possible with the record shape. Signal: VT-V4. The pinned
  build gives value output with the record shape.
- VT-R3 The value-type dependency in tests is not reproducible, because
  value-type has no release. Likelihood: medium. Effect: CI differs from local
  tests. Mitigation: pin a Git SHA in the test alias.
- VT-R4 Users expect better performance. Likelihood: medium. Effect: wrong
  adoption. Mitigation: the user documentation states the hash cost and the
  value-type size results, and gives no performance claim.
- VT-R5 `download.java.net` removes the pinned early-access build.
  Likelihood: medium. Effect: the Valhalla job fails when the cache is empty,
  and releases stop. Signal: the download step fails. Response: change the
  pin to the build that value-type tests, in the same change for both projects.
