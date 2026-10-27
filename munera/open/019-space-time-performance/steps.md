# Implementation steps

The four remaining milestones below replace the helper-level follow-up list.
Completed work remains in Git and the append-only `implementation.md`.
Do not reopen completed probes because an older discovery note says “next gate”.

## Established foundations

- [x] Verify pinned ordinary and Valhalla runtimes, APIs, operations, layout,
  null rejection, and closed-record applicability.
- [x] Fix the 17-case inventory and matched fixture semantics.
- [x] Validate allocation counters and ordinary/flat graph controls; demonstrate
  the physical value-box identity limit for reference containers.
- [x] Implement raw metric reporting, durable capture, runtime/tool checks,
  ordinary timing/allocation/graph collectors, and combined diagnostic reporting.
- [x] Verify and package the isolated Criterium estimator correction, including
  original-case smoke checks and tool provenance.

These checks are foundations, not full-matrix or acceptance results.

## 1. Ordinary end-to-end result

- [x] Run existing ordinary timing, allocation, and graph collection commands on
  pinned Temurin 21 for both widths and representations; retain five-fork raw
  evidence, boundary/warmup controls, and the combined report. Confirm tool
  suitability and record budgets, duration, and any actual defects. Finish the
  command-to-report path in this milestone, not in separate helper milestones.

- [x] Retain ordinary attempt 01 with all three five-fork captures and combined
  report in `evidence/ordinary-end-to-end`. Allocation and graph are stable.
- [x] Resolve the timing audit failure (0.029–0.064 ns/op), add a regression for
  its cause, and collect a new identified run after correction. See
  `evidence/ordinary-end-to-end/assessment.md`; successful exits are not approval.
  Corrected generated Cons expression; attempt 02 completes this milestone.
  See `evidence/ordinary-end-to-end-02/assessment.md` for results and limits.

## 2. Experimental end-to-end result

- [x] Connect fixed Valhalla array and field cases to collection and saved
  reporting. Verify matched results, changed-store resets, measured expression
  validity, and instrumentation separation with bounded real smoke runs.
- [x] Resolve each memory metric from existing controls and case checks. Record
  physically unverifiable reference-value graph sizes as evidenced unavailable;
  retain other supported measurements. Preserve required coverage and distinguish
  unsupported measurement from unimplemented work or process failure.
  See `evidence/experimental-memory-assessment.md`: saved validators reproduce
  both reports; timing and acceptance remain open.

## 3. Acceptance evidence

- [x] Fix commands, budgets, and evidence locations; confirm base and ordinary
  test dependency isolation; commit the harness and inventory before acceptance.
  See `evidence/acceptance-01/README.md`. Commands are frozen; no acceptance
  measurements have run. Original S-03 raw capture and all matrix runs remain.
- [x] Execute all six frozen matrix metric commands and saved reports. All
  processes pass; see `evidence/acceptance-01/assessment.md`. Original S-03 and
  the cross-inventory audit remain separate unfinished checks.
- [x] Run original S-03 cases and field-declaration verification, plus the complete
  new matrix. Retain the first complete results and all incomplete attempts.
  Associate separate metric runs through a bounded evidence index if needed.
- [x] Audit every case/representation/metric/fork against the inventory. Check
  units, raw divisors, runtime/tool identity, uncertainty, and memory ownership.
  Report unfavourable results; reject missing or failed supported measurements.
  See `evidence/acceptance-01/audit.md`: 430 raw outcomes plus 60 legacy
  statistics; 75 evidenced unavailable graph roots, no missing or failed slots.

## 4. Performance page and final checks

- [x] Create `README/performance.md` with measured tables, evidence links,
  commands, definitions, comparison limits, and explicit unavailable metrics.
  Qualify and link the README claim; link from the development guide.
- [ ] Run ordinary tests on Clojure 1.11, 1.12, and 1.13 plus affected benchmark
  tests. Inspect structured Scry outcomes and failures.
- [ ] Run shared format/lint, Vale, API freshness, and tracked Markdown link checks.
  Record and resolve relevant failures; preserve unrelated user changes.
- [ ] Audit all design acceptance criteria and record final verification and
  limits in `implementation.md`. Mark complete only from actual evidence.

Experimental integration detail:
- [x] Reproduce the plain Criterium batch's missing per-operation store reset on
  both layouts and lengths; retain real-tool regression and fixture evidence.
- [x] Verify a reset-aware measured boundary with real generated expressions
  for both layouts and lengths, including warmup state and repeated invocation.
- [x] Wire the boundary into experimental timing and finish collection and
  reports. Per-operation timer/input costs remain included; allocation must
  use a separate boundary.

- [x] Verify reset-aware stores through real Criterium estimation, warmup,
  collection, and bootstrap analysis for both layouts and lengths. This smoke
  uses checked diagnostic operations, not acceptance expressions.
- [x] Add unchecked experimental operation entry points and verify direct and
  real Criterium-generated results for both array lengths/layouts and holder
  representations. This is boundary verification, not completed collection.
- [x] Connect all nine experimental pairs to real timing collection and verify
  all 18 raw outcomes plus exact expression results on pinned Valhalla.
  This is one-second smoke coverage, not five-fork saved evidence.
- [x] Persist all 18 experimental timing rows through the existing child writer,
  with observed tool/runtime identity and per-site budget; verify real saved EDN.
  Five-fork collection, separate memory outcomes, and reports remain unfinished.
- [x] Retain five independent experimental timing children and a validated saved
  table (90 rows), with commands, exits, runtime/tool identity, and durations.
  See `evidence/experimental-timing-01`; this is diagnostic, not acceptance.
- [x] Audit experimental store timer resolution and flat-array read/store
  variation before acceptance; preserve attempt 01 regardless of later fixes.
- [x] Measure raw clock and reset-aware single-store duration distributions on
  pinned Valhalla. See `evidence/experimental-clock-audit`: most samples are zero,
  with smallest positive durations 41/42 ns. This does not approve attempt 01.
- [x] Resolve quantised store timing with a contract-checked batch boundary or
  justified error bounds; separately audit flat-read fork variation.
- [x] Test distinct pre-reset arrays as a changed-store batch on the pinned VM;
  retain result checks and duration summaries. At 1,024 stores all batches are
  positive; this does not approve changed working-set semantics for acceptance.
- [x] Exercise a separate experimental allocation boundary across all 18 sites
  with real counter reads and post-counter result checks. See
  `evidence/experimental-allocation-boundary`; one JVM is smoke evidence only.
- [x] Connect experimental allocation to inventory/persistence, independent
  five-fork capture, controls, and saved reporting; add invalid-input coverage.
- [x] Check experimental allocation invalid inputs and array allocation deltas
  against physical runtime stride. Nine rejection cases and ten real-counter
  comparisons pass; see `evidence/experimental-allocation-controls`.
  Holder controls and five-fork inventory/persistence integration remain open.
- [x] Verify holder construction/replacement allocation against independent
  shallow sizes using the actual boundary and prebuilt payloads. Twenty controls
  pass; see `evidence/experimental-holder-allocation`. Five-fork collection and
  graph ownership are not established by this check.
- [x] Connect experimental allocation inventory rows to child persistence and
  five independent forks with raw warmup/final counts and saved reporting.
  See `evidence/experimental-allocation-01`; invalid-budget tests pass.
- [x] Assess experimental allocation warmup/final variation before acceptance:
  restricted-holder reads range from 0 to 24 bytes/op. Preserve attempt 01.

  Saved audit finds constant holder-read counts within each fork, with differences
  aligned to pair order. Retain the budget and disclose this limit; see
  `evidence/experimental-allocation-01/assessment.md`. Graph and timing work remain.
- [x] Audit saved experimental read timing variation without recollection. Small
  flat-array timings split by pair order and batch divisor; larger arrays do not.
  See experimental-timing-01/read-audit.md. Cause and convergence are unknown;
  disclose the range. This closes read inspection, not timing approval or the
  remaining store-boundary work.
- [x] Verify flat graph roots after actual experimental construction, read,
  changed store, and holder replacement; reject reference shallow totals.
  Eleven controls pass; see experimental-graph-operations. Failed varargs-root
  attempt is retained. Five-fork graph persistence/reporting remains open.
- [x] Connect verified flat operation roots to a reusable graph collection
  boundary; check all nine sites and invalid inputs with the real agent.
  See experimental-graph-boundary. Inventory rows, provenance, five-fork capture,
  unavailable reference outcomes, and saved graph reporting remain open.
- [x] Adapt experimental graph counts to all inventory roots, with explicit
  unavailable reference outcomes and alternating pair order. Real-agent Scry
  verifies 30 rows per fork. Child provenance, five-fork persistence, and saved
  reporting remain required; this does not complete milestone 2.
- [x] Verify experimental active-agent artifact identity against independent JAR
  and class hashes; reject inactive and loose-class probes in real JVMs.
  Child publication, declared-artifact reconciliation, five-fork capture, and
  graph reporting remain unfinished.
- [x] Connect experimental graph child publication and bounded five-fork command
  with independent artifact declarations and validated saved reporting.
- [x] Execute and assess experimental graph attempt 01; retain all 150 outcomes.
  All five children pass; saved report validates 75 measured and 75 unavailable
  roots. Report-only failure retained; no recollection. Timing remains open.

- [x] Define and verify the replacement changed-store working set: 1,024 distinct
  arrays, index zero, slot-only untimed reset, identical logical traversal.
  See experimental-store-batch-contract; 28 real state checks pass.
- [x] Integrate StoreBatch with real Criterium, retain both normalization divisors,
  verify clock resolution, and collect corrected five-fork timing evidence.
- [x] Connect StoreBatch to real Criterium and preserve both normalization
  divisors. Verify normal/warmup result arrays and full collection on pinned
  Valhalla (4 tests, 196 assertions). No speed thresholds or mocks.
  Clock-resolution verification and corrected five-fork capture remain open.
- [x] Check raw clock resolution using the actual StoreBatch traversal and
  slot-only reset. All four sites have zero zero-duration samples in 1,000
  observations; see experimental-traversal-clock. Timer cost and quantisation
  remain. Corrected five-fork capture and assessment are still required.

Experimental milestone 2 closes with `evidence/experimental-timing-02` and the
existing memory assessment. Retain small-flat timing variation and clock limits;
this is diagnostic coverage, not acceptance approval. Next freeze acceptance
commands and budgets, then run the original cases and complete matrix.
