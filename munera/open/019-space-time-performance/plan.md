# Plan

## Goal and current position

Finish the measurements and linked performance page, not a general benchmark
platform. The design remains the acceptance contract. `steps.md` is the active
checklist; `implementation.md` preserves completed work and discoveries.

The capability gate is complete for Temurin 21.0.10+7-LTS and Valhalla
27-jep401ea3+1-1 with Clojure 1.11.4. Internal atomic arrays and
`@NullRestricted` fields are approved. Ordinary assoc collectors, saved evidence,
reports, graph tooling, and the isolated timing correction already exist.
The complete matrix, public report, and final repository checks are complete.
See the final verification entry in implementation.md for acceptance and limits.

Use `features.md` for commands and capability evidence, `measurement-contract.md`
for metric semantics, and `dev/bench/cases.edn` for the fixed inventory. Earlier
open-gate statements in discovery notes are historical, not new requirements.

## Execution rule

Each implementation pass targets a complete milestone below: collection through
saved evidence and a usable report, or a documented concrete obstacle. Continue
through helper functions, process wiring, and tests within that milestone.
A new helper, validator, CLI, or report renderer alone is not a planned milestone.
If a pass ends early, record the exact remaining integration work; do not create
another chain of framework follow-ups.

Freeze framework expansion. Reuse existing collectors, readers, manifests,
validation, and report commands. Add only the connections required by these
measurements. A short explicit command sequence is acceptable; a universal
matrix runner is not required. Do not add new diagnostic products, schema
versions, generic provenance systems, or duplicate validation layers. Fix a
framework defect only when it blocks a required command or invalidates evidence;
record the concrete failure. Do not remove existing checks or undertake a broad
cleanup to make progress appear faster.

## 1. Complete ordinary assoc end to end

Use the existing timing, allocation, and graph parent commands and combined
report for widths 3 and 16. Keep timing free of the memory agent. Use the
verified timing overlay and prepared graph agent with their existing provenance.
Select the parent JVM explicitly.

Run the commands on pinned Temurin 21, save actual five-fork outcomes, and render
the combined report. Check shared payload accounting and raw time/allocation
divisors. Start allocation budget evaluation from the existing 10,000-operation,
20-warmup-batch diagnostic; do not restart control research without a failure.
Record observed duration and budget suitability. This milestone must produce
real time and memory results, not only collector or report fixtures.

Attempt 01 remains invalid timing evidence. Attempt 02 completes this milestone:
the nested macro now constructs a list that Criterium factors correctly instead
of timing nil. Real measured-result regression and five-fork collection pass.
See `evidence/ordinary-end-to-end-02/assessment.md`. Width-16 timing variation
and its slower closed-record mean remain visible; no parity claim is made.
Next execute experimental coverage, using these ordinary budgets as diagnostics.

## 2. Complete experimental coverage

Connect the existing Valhalla fixtures and controls to measurement and saved
reporting for the fixed array and field cases. Preserve construction boundaries,
changed-value stores, source-preserving holder replacement, and scalar reads.
Verify opaque inputs, result consumption, reflection, and reset boundaries in
bounded smoke runs before full measurements.

Decide memory support per metric and representation using retained controls.
The JDK allocation counter and verified graph agent are the selected tools;
no replacement-tool search is required without a demonstrated defect. Physical
box identity for reference containers is a known limit. Record affected graph
outcomes as unavailable with the existing probe evidence unless a small verified
solution exists. Continue unaffected timing, allocation, and flat-layout cases.
Do not publish a total-space ratio when one side is unavailable.

Unavailable means a demonstrated measurement limit, not unfinished wiring,
missing time, a failed process, or an inconvenient result. Preserve failed states.
All topics still need measured time and memory evidence where supported.

## 3. Freeze and run acceptance

Keep the 17 inventory comparisons, five forks, alternating pair order, and
original S-03 rule. Fix commands and budgets after smoke checks; record observed
wall-clock duration rather than treating 850 configured timing seconds as an
elapsed-time estimate. Commit the harness and inventory before acceptance.

Run the original six cases and field-declaration test, then the new matrix using
separate metric commands if simpler. A bounded evidence index can associate
those runs; reuse existing validation rather than require one transport envelope.
Verify every inventory slot has a measured, evidenced unavailable, or failed
outcome. Failed or missing supported measurements block completion.

Retain the first complete run, raw results, commands, revision, runtimes, flags,
tool identities, machine, budgets, exits, and variation. Preserve incomplete
attempts. A correctness fix needs a new identified run; an unfavourable result
is not a reason to rerun. Parity failure is reported, not optimised away.

## 4. Publish and verify

Create `README/performance.md` from the evidence, covering all three topics,
comparison semantics, uncertainty, unavailable metrics, and reproduction commands.
Link it beside the qualified README claim and from the development guide.
Do not imply that external Valhalla fixtures are closed-record features.

Run the three ordinary runtime test suites, affected benchmark tests, shared
quality checks, API freshness, and tracked Markdown links. Record failures and
resolve relevant defects; do not silently waive checks. Audit the design criteria,
then update the checklist and implementation log. No further framework work is
needed when those criteria pass.

Experimental boundary check: the real generated Criterium batch does not reset
arrays between stores. See `evidence/experimental-store-boundary/assessment.md`.
The custom `valhalla.reset-measured/reset-measured` boundary now passes real
expression checks with reset outside each timed operation. Connect it to
experimental collection; retain per-operation timer and input costs. Keep the existing changed-store contract; do not
label repeated same-value stores as its measurement.

Full Criterium collection now also passes the reset boundary smoke for both
layouts and lengths (`valhalla.reset-collection-test`). The checked store throws
if any operation fails to change 17 to 23. Use separate unchecked acceptance
expressions; diagnostic precondition costs are not performance evidence.

Unchecked Java operation entry points now exist in `valhalla/Operations.java`.
Real generated-expression tests cover array construction/read/store and holder
construction/read/replacement. Use these operations in collection; do not time
`StoreBoundaryProbe` checks. Experimental collection, separate allocation/graph
outcomes, five-fork persistence, and reporting remain unfinished.

`valhalla.timing/collect!` now collects the 18 experimental timing outcomes with
alternating pair order. Exact expressions and full collection pass pinned-runtime
smoke checks. Next persist timing rows with existing child/tool/budget metadata,
connect separate memory outcomes, and finish five-fork saved reports. Do not
interpret this collector-only smoke as completion of milestone 2.

Experimental timing now has `valhalla.timing-child` publication with the existing
transport, observed runtime/tool identity, and per-site budget. A fresh pinned
Valhalla test saves all 18 rows. Next run five independent children and connect
separate memory collection and saved reporting; one child is not milestone 2.

Five independent experimental timing children and a saved timing table now exist
in `evidence/experimental-timing-01`. All 90 rows pass existing validation.
Before acceptance, inspect store timer resolution and the large flat-array
read/store variation recorded in its assessment. Do not treat successful exits
as measurement approval. Separate allocation/graph collection remains next.

The clock audit now demonstrates zero durations in 54–70% of reset-aware store
samples and 41/42 ns smallest positive durations. See
`evidence/experimental-clock-audit/assessment.md`. Do not floor raw timings or
subtract overhead. Next verify a multi-store boundary with untimed resets and
matched ownership/cache semantics, or establish error bounds; read variation
remains a separate question. Attempt 01 is still not acceptance evidence.

The multi-array store probe now passes result checks for both layouts/lengths.
At 1,024 stores no sampled batch is zero, but multiple arrays change the working
set. See `evidence/experimental-store-batch/assessment.md`. Do not adopt its
numbers or boundary without matching and disclosing cache/ownership semantics.
The acceptance collector and separate memory integration remain unchanged.

The separate Java `Allocation.sample` boundary now exercises all experimental
operations with prebuilt payloads and escaping results. It uses distinct sources
for changed stores and primitive read sinks, outside-counter setup and checks.
See `evidence/experimental-allocation-boundary`. Next connect inventory rows and
existing persistence, verify controls, and retain five forks; the single-JVM
smoke does not complete memory integration or resolve store timing.

Allocation boundary controls now reject malformed sites before setup and confirm
both array allocation length deltas against runtime stride. See
`evidence/experimental-allocation-controls`. Next retain holder controls and
connect the existing boundary to inventory/persistence and five-fork reporting;
these single-JVM checks do not complete milestone 2.

Holder construction/replacement allocation now matches independent shallow sizes
for both representations in the real boundary. See
`evidence/experimental-holder-allocation`. Use the prebuilt-payload contract;
these 16-byte allocations do not include reference payload graph ownership.
Next connect inventory/persistence and five-fork memory reporting; timing
quantisation and graph integration remain unresolved.

Experimental allocation now has five independent captures and a saved table in
`evidence/experimental-allocation-01`. All 90 rows pass validation. Retain the
restricted-holder read range (0–24 bytes/op) and flat-array read allocations;
inspect raw warmup/final batches before freezing budgets. No favourable rerun.
Graph outcomes and the timing audits still prevent milestone 2 completion.

Saved allocation batch review is complete. Keep the fixed diagnostic budget:
restricted-holder reads are constant within forks but differ with pair order;
longer warmup is not supported as a remedy. Disclose the range and order
association, not a converged intrinsic cost. See experimental-allocation-01's
assessment and reproducible audit. Next connect graph outcomes and resolve the
separate timing limits; milestone 2 remains incomplete.

Saved read timing review now identifies a small flat-array fast/slow split aligned
with pair order and batch divisor, absent from large flat-array and holder reads.
See experimental-timing-01/read-audit.md and its assessment. Preserve every fork
and disclose the unknown cause; saved means cannot establish convergence. Do
not conflate this with single-store clock quantisation. Next connect graph
outcomes and resolve the store boundary; no favourable timing rerun is justified.

Experimental graph operation controls now pass on pinned Valhalla; see
`evidence/experimental-graph-operations`. Reads retain input roots; holder read
source/result roots alias, unlike construction/replacement. Java Object[] roots
need an Object cast at the varargs graph boundary. Next connect saved graph
outcomes and five forks with verified agent identity; this control is not
milestone 2 completion or acceptance.

The experimental graph Java boundary now returns source/result/union counts for
all supported flat sites; see evidence/experimental-graph-boundary. Integrate
these counts rather than repeat operation setup in the inventory adapter. Reads
and stores alias roots. Reference outcomes require explicit evidenced unavailable
rows; five-fork capture, provenance, and reporting remain unfinished.

`valhalla.graph/collect!` now adapts all experimental graph sites to 30 inventory
rows per fork. Array roots count the result array; field roots expose result,
union, and increment. Reference rows are explicitly unavailable due to physical
value-box identity, not failed collection. Real-agent tests pass. Next attach
verified experimental agent provenance, persist five children, and render saved
graph results. The ordinary RecordGraphProbe identity is not the FlatGraphProbe
identity; do not label these counts with the ordinary agent's hashes.

The flat agent now exposes active JAR/class resource hashes and location, with
real-process checks for active, inactive, and loose-class loading. Put the agent
JAR before loose classes when collecting identity. Next attach this observation
to experimental child rows and compare with the prepared artifact; resource hashes
do not detect class transformation. Five-fork graph capture/reporting remains open.

Experimental graph child publication and the bounded five-fork command/report
are connected. Execute `evidence/experimental-graph-01/run.sh` after committing
its source; reconcile independently declared flat-agent hashes before reporting.
This completes graph integration only after actual saved evidence passes.

Experimental graph attempt 01 now completes five-fork graph capture and saved
reporting: 150 outcomes, stable flat counts, evidenced unavailable reference
roots. See its assessment. The first report failed on an unquoted require;
corrected reporting reused all original children. Next resolve store timing
quantisation and freeze disclosed limits/budgets before acceptance; no further
graph infrastructure is required.

Experimental diagnostic memory coverage is now assessed end to end. See
`evidence/experimental-memory-assessment.md` for all 240 allocation/graph outcomes,
ownership limits, and reproduction. Both saved reports reproduce byte for byte.
No memory integration remains for milestone 2; do not reopen it because older
notes above describe pending wiring. Store timing remains the next concrete work.

The changed-store replacement now has a concrete matched working-set contract
in measurement-contract.md and Java StoreBatch. Slot-only reset avoids the
old probe's full-array scan. Real pinned-runtime state checks pass. Next connect
this batch to the existing reset-aware Criterium wrapper, retain the 1,024-store
divisor, verify actual batch durations, and capture a new five-fork timing run.
Do not reuse attempt 01 or the exploratory batch probe as corrected evidence.

StoreBatch is now connected to real Criterium collection. Saved timing rows keep
`:criterium-invocations` and `:operations-per-invocation`; `:operations` is their
product. Raw nanoseconds remain unchanged. Real collection and generated normal/
warmup states pass. Next compile StoreBatch in the new capture command, inspect
raw traversal duration resolution, and retain corrected five-fork timing and its
report. Attempt 01 remains unchanged; this smoke does not complete milestone 2.

The actual StoreBatch clock check now passes result checks with no zero-duration
samples at any site. See evidence/experimental-traversal-clock/assessment.md.
The minimum small-flat traversal is only about five clock steps; retain timer
and quantisation limits. Next run corrected five-fork Criterium capture with
StoreBatch compiled, then assess its report. This diagnostic is not acceptance
or a reason to change budgets for favourable results.

Corrected five-fork timing attempt 02 completes milestone 2 diagnostic coverage.
Its report validates both divisors and all 90 timing rows. Memory coverage is
already complete. Small-flat read/store variation and traversal clock limits
remain disclosed; do not rerun to select favourable modes. Next execute milestone
3: freeze acceptance commands/budgets and run original S-03 plus the full matrix.

Acceptance attempt 01 now has frozen metric scripts, budgets, report validators,
and preflight classpaths in `evidence/acceptance-01`. Base and ordinary tests
exclude measurement dependencies. No acceptance measurements have run. Next
commit/use these commands, retain original S-03 child envelopes (zero-based forks),
and execute the complete matrix. Do not use the legacy parent's temporary files
as the only S-03 evidence. Preserve all failures and the first complete outcome.

The first acceptance matrix captures now exist in `evidence/acceptance-01`. All
six metric commands and saved reports pass without retries. Do not repeat these
measurements: next retain original S-03 children and field-test results, audit
490 inventory outcomes, and assess acceptance. See its assessment for durations
and unfavourable results. Design remains unchanged.

Original S-03 and field-declaration verification now pass on pinned Temurin 21;
see evidence/acceptance-01/s03. All five original children are retained. Next
audit the 490 matrix outcomes without recollection, then publish the performance
page. The design remains unchanged.

Acceptance inventory audit is complete; see evidence/acceptance-01/audit.md.
There are 430 raw outcomes and 60 legacy statistics, not 490 raw batches.
Next publish the performance page and run final checks; do not recollect.

The public performance page now links the first acceptance evidence, all three
topics, unavailable reference graphs, and reproduction commands. README claims
are limited to measured cases. Vale and tracked Markdown links pass. Next run
the three runtime suites, affected benchmark tests, remaining quality checks,
and final acceptance review; no measurement repetition is needed.

Final verification is complete on pinned Temurin 21. All three Clojure suites
and shared quality checks pass. The saved-controls test fixture now declares its
budget; no collector, dependency, acceptance capture, or library semantics changed.
The design acceptance criteria are satisfied with the published measurement limits.
