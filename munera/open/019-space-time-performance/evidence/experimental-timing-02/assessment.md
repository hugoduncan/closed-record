# Corrected experimental timing attempt 02

All five independent pinned-Valhalla children exit zero. The saved report checks
90 measured rows, process associations, runtime and corrected collector identity,
one-second site budgets, complete timing coverage, and both operation divisors.
Child durations are 25, 24, 25, 24, and 24 seconds (122 total; excludes fixture
preparation). Source revision was committed before capture. No memory agent runs
in these children. Attempt 01 remains unchanged. This is diagnostic evidence,
not the acceptance run or an S-03 result.

Stores now traverse 1,024 distinct arrays, with slot-only resets outside timing.
Each raw nanosecond estimate is divided by Criterium invocations times 1,024;
other operations retain a multiplier of one. Traversal, timer, and opaque-input
costs remain included. These are changed stores, not repeated same-value stores.
See the [contract](../../measurement-contract.md) and the
[actual traversal clock check](../experimental-traversal-clock/assessment.md).
There is no overhead subtraction, clock flooring, or discarded fork.

## Results and limits

The [table](report.md) retains all ranges and sample standard deviations. Small
flat reads still range from 0.228 to 18.263 ns/op. Small flat stores range from
0.263 to 11.203 ns/store. The batch boundary removes the demonstrated majority-zero
single-store sampling problem; it does not remove execution-mode variation.
Do not describe either mean as a stable, order-independent intrinsic cost.
The earlier read audit does not establish the cause. Clock granularity remains
material for the fastest traversal; its minimum in the independent clock check
was about five clock steps. Timer and traversal costs remain part of this workload.

Large flat construction remains slower (7,430 ns/op mean versus 571 for reference
arrays). Large flat stores average 11.042 ns/store versus 1.113 for reference
arrays. These external comparison types are not closed-record operations.
Different physical working-set sizes and store semantics prevent general speed
claims. Do not rerun to select favourable modes or hide slower results.

Together with the [memory assessment](../experimental-memory-assessment.md),
this completes diagnostic experimental collection and reporting. Acceptance must
retain these disclosed limits, fixed workloads, and independent forks. It still
requires frozen commands/budgets, original S-03 cases, and the complete matrix.

## Reproduction and verification

Run `run.sh` from the repository root only into a fresh evidence identity. It
compiles StoreBatch as well as Operations. Preserve existing outputs. To validate
and render saved evidence without measurement:

```sh
clojure -Srepro -Sdeps '{:paths ["src" "dev/bench" "target/019-timing-overlay"]}' -M munera/open/019-space-time-performance/evidence/experimental-timing-02/report.clj
```

The corrected overlay must be prepared as described in the task's timing tooling.
A second render is byte-identical. Real expression, collection, and store-boundary
Scry tests on pinned Valhalla pass: 6 tests, 300 assertions, exit zero, no failure
files. No mocks or speed thresholds. Focused formatting, lint, and shell syntax
checks pass. Saved report assertions also verify the two normalization divisors.
