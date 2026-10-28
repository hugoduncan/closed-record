---
title: Benchmark operation boundaries and saved evidence
status: active
category: tooling
tags: [bench, mutation, timing, normalization, provenance, evidence]
related: [mementum/knowledge/benchmarks.md, mementum/knowledge/benchmark-memory-accounting.md, dev/bench/valhalla/reset_measured.clj, dev/bench/valhalla/StoreBatch.java, README/performance.md]
depends-on: [deps.edn]
---

💡 A valid result envelope does not prove that the intended operation was
measured. Task 019 needed real expression checks, clock controls, and saved-data
checks in addition to process and transport tests.

## Changed-value mutation needs a reset contract

A plain Criterium batch repeats the operation without resetting its input.
Resetting once before the batch makes only its first store a changed-value store.
Verify repeated invocations and both normal and warmup state with the actual
measured-expression generator. Criterium returns the first expression result,
not the last; inspect mutated state as well as the returned value.

The reset-aware wrapper resets outside each separately timed invocation. Its
input and timer costs remain included. Do not use this timing wrapper to measure
allocation: its excluded reset and wrapper work has a different allocation boundary.
Use checked operations for diagnostic tests and unchecked operations for timing;
precondition checks must not become part of the reported operation cost.

Single-store timing on the pinned Valhalla runtime produced mostly zero durations,
with 41/42 ns clock steps. Positive aggregate results did not establish resolution.
Inspect raw duration distributions. Never floor raw timings to hide zeros.

A longer invocation can reduce quantisation, but it changes the workload. The
verified store boundary traverses 1,024 distinct arrays and changes index zero
once in each. Slot-only resets occur outside timing. Match logical traversal and
ownership across representations; disclose physical footprint and cache differences.
Traversal and amortised timer/input costs remain in ns/store. Nonzero samples
alone do not make these costs negligible.

## Keep raw quantities and their meanings

- Preserve raw batch time and its actual invocation count before normalization.
  For multi-store invocations, retain both invocation count and operations per
  invocation; their product is the divisor. Do not alter raw nanoseconds.
- Criterium's bootstrap mean batch time is an estimate, not an individual sample.
  Legacy per-call statistics cannot reconstruct discarded raw batches.
- Give each fork equal weight after normalization. Unequal batch sizes do not
  justify weighting the cross-fork mean by operation count.
- Constant allocation batches within a fork do not prove convergence across JVMs.
  Task 019 found constant per-fork counts with different modes aligned to pair
  order. Timing modes also aligned with order and divisors. Preserve all forks;
  correlation does not establish cause or justify longer warmup or favourable reruns.
- Configured timing seconds are not elapsed duration: startup, estimation, warmup,
  analysis, and separate memory collection add work. Record actual duration.

## Revalidate saved evidence before reporting

Keep these checks separate: process exit, envelope validity, runtime/tool
observations, case identity, complete coverage, and measurement validity.
Partial envelopes can preserve failure evidence but cannot prove complete coverage.
Unavailable metrics need a demonstrated measurement limit; missing implementation
and failed processes are not support limits. Never convert unavailable data to zero.

Persist command, streams, raw result, and exit before result validation. Use
exclusive run and child paths so retries cannot overwrite evidence. A failed
launch has no exit; a result-read error has no invented validation result.

Saved readers must validate the manifest and reconcile raw child observations
again, not trust a saved success flag. Changes to runtime declarations exposed
this stale-validation defect. Apply the same principle to budgets and collector
identity. Tests should alter saved files and verify rejection without writes.
These checks establish consistency, not protection against deliberate tampering.

Runtime declarations alone do not prove what a child loaded. Compare observed
runtime build, Java home, and ordered flags with declarations. Compare collector
source/patch or agent JAR/class hashes with independently prepared identities.
Keep resource locations for diagnosis, but exclude them from portable tool-identity
equality. Resource hashes do not detect runtime class transformation or var replacement.

Keep graph instrumentation out of timing JVMs. When combining separate metric
runs, validate each first and retain each source manifest. Require matching
inventory, revision, machine, and runtime while allowing metric-specific flags.
Do not flatten away the provenance of separately measured metrics.

Evidence: task 019 `implementation.md`, entries on saved runtime revalidation,
experimental clock quantisation, saved allocation/read audits, and store traversal
integration. Task-scoped commits `eed28b5`, `3403a89`, and `120bd0c` retain controls
and corrections. Reuse the existing collectors and readers; these lessons do not
require a new generic benchmark framework.
