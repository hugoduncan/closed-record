# Space and time performance

## Intent

Users can assess the time and memory costs of `assoc`, value arrays, and
null-restricted fields from reproducible measurements. The README links directly
to the evidence and states its limits. Benchmark work does not change library
semantics or add runtime dependencies.

## Problem

The README says that reads and `assoc` of declared keys are as fast as
`defrecord`, but links only to a development command. The current benchmark
measures six operations on three-field records. It does not measure allocation
or retained size, and has no value-array or null-restricted-field cases.
Users cannot determine the memory costs or the scope of the timing claim.

The repository does not define value arrays or null-restricted fields as
supported features. Their meaning, required runtime, and applicability to
`defclosedrecord` must be established before benchmark results can support a
public claim. A type hint alone does not prove null restriction or flat storage.

## Context

- [README](../../../README.md): current performance claim.
- [Development guide](../../../README/development.md): benchmark command.
- [Benchmark knowledge](../../../mementum/knowledge/benchmarks.md): measurement
  hazards, fork policy, and prior results.
- [Benchmark entry point](../../../dev/bench/closed_record/bench.clj):
  keyword lookup, `get`, field access, `assoc`, cached `hash`, and equality.
- [Success criteria](../../../ramora/defclosedrecord/purpose.md): S-03 timing rule.

The current harness uses Criterium 0.5.251-ALPHA, five independent JVM forks,
and alternating comparison order. It separates measurement, process control,
and pure reporting. Known issues include array-typed locals and expression
hoisting. Old timings from Criterium 0.5.243 are not valid evidence for cheap
operations. The existing `assoc` case replaces a declared field in a
three-field record; it is not evidence for all record sizes.

## Scope

### Feature and runtime boundary

Establish the exact APIs meant by value arrays and null-restricted fields,
with authoritative references and executable capability checks. Record the
Clojure version, JDK build, flags, and feature status. Explain whether each
feature applies directly to closed records, to their field values, or only to
an external comparison type.

Internal Valhalla APIs with atomic arrays are acceptable for these benchmarks;
public APIs and non-atomic arrays are not prerequisites. Pin the Valhalla build
and document internal APIs, access flags, and atomicity guarantees. Verify the
actual storage layout: atomicity does not prove flattening. Keep this use in
benchmark tooling, not library runtime code, and disclose its experimental status
on the performance page.

Use a supported runtime for ordinary records. Isolate any preview or experimental
runtime needed for the other cases. Do not raise the library's minimum runtime.
If a feature cannot be exercised without library changes, record the evidence
and limit its results to clearly labelled comparison types. Do not silently
omit the topic or call a substitute a closed-record measurement. If the intended
API remains ambiguous, obtain a user decision before fixing the benchmark matrix.

### Measurements

Use matched data and equivalent operations. Define the case matrix before the
acceptance run, including small and larger record widths and array lengths.
Keep the matrix small enough for a documented manual run.

- `assoc`: compare closed records with `defrecord` for a changed declared value.
  Measure time and bytes allocated per operation, and the retained size of
  equivalent results. Separate source/result sharing from newly retained data.
- Value arrays: compare with ordinary reference arrays containing equivalent
  logical values. Measure retained size, construction, indexed read, and the
  applicable element update or replacement operation. State whether updates
  mutate or copy; do not equate an array store with persistent `assoc`. State the
  atomicity and memory-ordering guarantees of each compared operation and explain
  any differences.
- Null-restricted fields: compare with nullable reference fields containing
  equivalent non-null values. Measure retained size, construction, field read,
  and the applicable replacement operation. Separate null restriction from
  flattening; verify the representation rather than infer it from syntax.

Time, allocation, and retained size are separate metrics. Specify units,
ownership of reachable objects, treatment of shared values, and measurement
limits. An unsupported or unavailable measurement is not zero. Avoid counting
fixture setup as operation cost unless construction is the operation.

### Evidence and documentation

Create `README/performance.md` and link to it near the README performance claim.
The page covers all three topics, definitions, supported configurations,
comparison semantics, measured tables, uncertainty, commands, and limitations.
Qualify the README claim to match the evidence. Link from the development guide
without duplicating the report.

Retain machine-readable results and enough run metadata to trace every published
number to a source revision, command, runtime, machine, and case. Keep a bounded
acceptance evidence set in Git; do not commit caches or unrestricted profiler
output. Record failed attempts and unavailable cases, not only favourable runs.

## Design constraints and alternatives

Extend the existing manual benchmark structure rather than introduce a second
unrelated timing harness. Keep reporting testable without long benchmarks.
Use an isolated tool or profile for memory measurement when timing tools cannot
provide a valid metric; document its limitations. A dependency upgrade is allowed
only when needed for correct measurement and after existing cases are checked.

Documentation alone cannot supply missing evidence. A broad runtime survey or
performance optimisation would exceed this task. Prefer a small, verified matrix
over many poorly controlled measurements. Preserve the existing S-03 rule for
its original cases; new exploratory comparisons do not imply parity requirements.

## Out of scope

- Library feature implementation, emitter optimisation, or semantic changes.
- Universal performance guarantees or exhaustive hardware/runtime coverage.
- Timing gates in CI, application benchmarks, and unrelated operations.

## Acceptance

1. Feature definitions, capability checks, and runtime requirements are explicit.
   Each topic has measured cases or a demonstrated support limit, with no silent
   substitution. Supported cases include time and memory evidence.
2. Commands reproduce the fixed matrix. Results cannot be folded away; array
   cases address the known harness hazards. Timing uses independent forks and
   reports variation. Memory measurements verify what they count.
3. The first complete run of the fixed harness is retained with metadata and
   structured results. Failures are reported, not tuned away or relabelled.
4. Tests cover case identity, units, normalisation, reporting, unavailable
   measurements, and process failures. Existing benchmark tests and cases remain
   valid. Ordinary test and runtime dependencies remain isolated from tools.
5. The linked performance page covers all three topics and distinguishes actual
   closed-record results from external comparisons and unmeasured expectations.
   Public claims agree with the evidence.
6. Applicable code-quality, prose, and Markdown-link checks pass. Benchmark
   commands remain manual; implementation notes record verification and limits.
