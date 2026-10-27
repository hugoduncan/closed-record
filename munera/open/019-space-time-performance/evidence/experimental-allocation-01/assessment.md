# Experimental allocation attempt 01

Five independent pinned Valhalla children completed with exit 0. Their elapsed
seconds are 2, 3, 2, 2, and 2. The saved report validates 90 inventory rows,
associations, runtime agreement, batch counts, and raw divisors. The harness was
committed before collection; see revision.txt. No timing agent or Criterium is
loaded. Fixtures pass before collection.

Each site uses 1,024 operations per batch, 20 retained warmup counts, and five
retained final counts. Pair order alternates. Counts are summed before division;
fork means have equal weight. Setup, changed-store sources, result sinks, and
post-counter checks are outside counters. No count is subtracted. Reproduce from
the repository root with run.sh; its overwrite guard preserves this attempt.
Run report.clj with dev/bench on the Clojure classpath to read saved evidence.

Construction matches the existing physical controls: arrays allocate 32 or 4,112
bytes per operation; holders allocate 16. Stores allocate zero in these batches.
Flat array reads allocate about 24 bytes per operation. Restricted-holder reads
vary from 0 to 24 across forks (mean 9.6). Preserve this variation: prebuilt value
inputs and scalar sinks do not guarantee that intermediate boxes disappear.
This observation does not establish the cause or JIT convergence. Inspect the
retained warmup/final sequences before freezing acceptance budgets. Do not rerun
merely to obtain zero allocations.

These are diagnostic allocation results, not graph ownership, timing, or full
matrix acceptance. Graph integration, store timing quantisation, and read timing
variation remain open. The design is unchanged.

Verification: real-counter Scry test on pinned Valhalla passes 1 test and 188
assertions, exit 0, no failure files. It covers all 18 sites, alternating order,
raw divisors, EDN round trips, and invalid budgets, without mocks or speed limits.
Focused formatting and clj-kondo pass. Initial test/report delimiter errors were
repaired with clj-paren-repair before verification and collection.

## Saved batch assessment

Run `bb munera/open/019-space-time-performance/evidence/experimental-allocation-01/audit.clj`
after the JVM report command to reproduce [the batch audit](audit.md). Its
assertions check all 90 rows. No measurement was repeated for this assessment.

Restricted-holder reads are constant across all 25 batches within each fork:
0, 24, 0, 24, and 0 bytes/op in fork order. The difference follows alternating
pair order in this attempt. This is association, not proof of causation: process
compilation history and pair order are not independently varied here. More
warmup is not justified by a declining sequence; none is present. Do not describe
the equal-fork mean of 9.6 as a stable allocation cost or select only odd forks.

Only five rows change across their combined warmup/final sequences. Four have
warmup-only 48-byte excursions. The fifth is fork 3, flat-array read length 4:
final batches are 24,576 then four counts of 24,624 bytes. The extra 48 bytes per
batch are 0.046875 bytes/op; their cause is not established. All other final
sequences are constant. Keep these raw costs, without subtraction.

Retain the fixed 20 warmup / five final / 1,024 operation budget for bounded
allocation evidence, with all forks and this limitation. It supports observed
boundary counts, not JIT convergence or an order-independent intrinsic cost.
No budget increase or favourable rerun is warranted by these sequences alone.
Graph integration and timing audits still block milestone 2; this assessment
closes only the allocation variation review.
