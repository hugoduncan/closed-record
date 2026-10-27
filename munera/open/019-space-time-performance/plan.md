# Plan

## Approach

Extend the existing manual benchmark path. Do not change library semantics,
emitter code, minimum runtime, or base runtime dependencies. Keep ordinary
tests independent of measurement tools and preview runtimes.

### 1. Establish features before fixing cases

Create `features.md` in this task directory. Identify the APIs for value arrays
and null-restricted fields from authoritative runtime documentation. Record
reference URLs, feature status, exact Clojure and JDK versions, flags, and
applicability: closed-record type, field value, or external comparison type.
Add executable capability probes under `dev/bench` and retain their commands
and structured outcomes in the task's `evidence/` directory.

Use Clojure 1.11.4 with Temurin 21 for ordinary cases. Isolate any required
experimental JDK and compiled comparison fixtures in an explicit benchmark
profile. A type hint is not proof of null restriction or flat storage. Check
null rejection and actual layout separately. If closed records cannot support
a feature without library changes, demonstrate that limit and use clearly
labelled external comparisons where possible. An unavailable runtime is not
proof that a feature is unsupported.

Gate: do not fix the matrix until the probes establish the feature boundary.
If authoritative APIs still leave the intended meaning unclear, stop for a user
decision. Do not choose a substitute to avoid that decision.

### 2. Define measurement contracts

After the gate, create `dev/bench/cases.edn` as the fixed case inventory. Include
all six original S-03 cases, small and larger record widths, and small and larger
array lengths. Choose and record exact dimensions, values, replacement values,
operations, fixture ownership, configurations, and expected capability outcomes.
Keep the matrix bounded and estimate its manual run time before acceptance.

Use matched non-null logical values. Changed-value `assoc` creates a new record;
array stores and persistent replacement are different operations. Specify
mutation or copying for every update and reset mutable fixtures outside the
measured operation. Construction alone includes fixture creation costs.

Define time in ns/op, allocation in bytes/op, and retained size in bytes for
named object roots. For `assoc`, report the result graph, the source/result
union, and the incremental retained graph relative to the source. Deduplicate
shared objects by identity. Describe payload ownership and distinguish total
reachable size from newly retained data. Do not call graph size dominator-based
heap retention without evidence.

Select and pin a memory tool in a benchmark-only profile after checking its
support for each runtime and layout. Validate allocation counting with known
allocating and non-allocating controls; validate graph accounting with shared
and unshared fixtures. Report unsupported metrics with a reason, never zero.
A supported case needs time and memory evidence; a missing metric is an explicit
acceptance gap, not a successful measurement.

### 3. Extend the existing boundaries

Keep literal measurement call sites in `closed-record.bench`. Keep statistics,
case validation, units, normalisation, and rendering pure in the benchmark
reporting layer. Keep child commands, files, and failure handling in the fork
layer. Reuse these boundaries rather than create another timing harness.

Retain five independent JVM forks, alternating comparison order, one execution
per measurement call site per child, and per-batch normalisation. Preserve the
original S-03 rule and field-declaration test. New comparisons are exploratory;
do not apply the S-03 pass threshold to them.

Check opaque inputs, result consumption, array-local compilation, reflection,
and expression hoisting before full measurement. Upgrade Criterium only if
correct measurement requires it, with explicit regression checks of the old
cases. Keep memory measurement separate when instrumentation changes timing.

Extend EDN transport with stable case and configuration identities, metric
units, raw counts and normalisation inputs, per-fork results, and explicit
measured/unavailable/failed outcomes. Persist run manifests and child artifacts
before aggregation; failures must not discard successful earlier results.
Retain the original command and its 0/1/2 success/parity/process exit meanings.
Document new commands and their failure semantics before acceptance.

### 4. Capture evidence, then publish

Freeze and commit the harness, inventory, commands, and tool versions before
acceptance. Store a bounded evidence set in this task's `evidence/`: manifests,
capability results, per-fork metric EDN, summaries, and command output. Record
revision, dirty state, runtime, flags, tool versions, machine/OS/CPU, layout
settings, case IDs, timestamps, and process exits. Do not commit caches or full
profiler dumps.

Retain the first complete run of the fixed harness, including unfavourable
results. Record incomplete attempts and failures separately. A corrected
harness starts a new identified revision; preserve earlier evidence and explain
why it is invalid. Never rerun only to replace an unfavourable complete result.

Create `README/performance.md` from retained evidence. Cover all three topics,
commands, definitions, operation semantics, uncertainty, and limits. Trace each
published number to a case and run. Qualify the main README claim and add a
direct link beside it. Link the development guide to the report without copying
tables.

## Risks

- Experimental APIs or runtime availability may block the feature gate.
- Memory tools may not understand flat layouts; unsupported readings cannot
  establish a space advantage.
- Criterium's known array-local and hoisting faults may require a tool upgrade.
- JIT elimination, sharing, mutation, and instrumentation can invalidate metrics.
- Larger widths and lengths increase run time; bound the inventory before runs.
- S-03 may fail. Report it; performance optimisation is outside this task.

## Slice order

1. Feature definitions, isolated capability probes, and support gate.
2. Fixed inventory, metric contracts, and validated memory tooling.
3. Structured reporting and durable process evidence with fast tests.
4. Matched timing/memory cases and harness validity checks.
5. Frozen acceptance run, bounded evidence, and result audit.
6. Public performance report and final verification.

## Planning review

Design and referenced README, development guide, benchmark knowledge, entry
point, and S-03 were checked on 2026-10-27. No blocking inconsistency was found.
The broad README claim is the documented problem, not a new acceptance rule.
Feature identification is explicit discovery work, not permission to assume
Valhalla or another API. Exact experimental versions, tools, and dimensions
remain non-blocking choices subject to slices 1 and 2.

Plan ambiguity review completed with `steps.md` read-only. No blocking issue
was found. New memory requirements apply to the new topic cases, not all six
legacy operations. Steps for unsupported features end with recorded limits,
not substitute measurements. No library implementation is planned.
