# Fixed inventory and metric contract

`dev/bench/cases.edn` fixes 17 comparison pairs: six original S-03 pairs,
two changed-value assoc widths (3 and 16), six array pairs (lengths 4 and
1024, each with construction, read, and store), and three field pairs.
Only the original six pairs use S-03. Each new pair requires time, allocation,
and reachable-size evidence. The inventory is not an acceptance-ready harness.

Contracts supply shared properties to each case. Width selects a prefix of the
listed fields and values. Both record types receive the same boxed payload
objects. Array construction fills every slot with the prebuilt Point(17);
reference slots share that payload. Flat slots contain its value, not an object
reference. Holder construction uses the same prebuilt payload. Replacement uses
Point(23), or the preboxed number 23 for assoc. Reads return the scalar Point.x,
including the Object-to-Point cast in the reference-holder path.

Construction includes allocation and initialization of the result container,
not payload creation. Other operations exclude fixture setup. Array stores
change index zero from 17 to 23. The harness must reset that slot outside each
measured operation; a batch of repeated same-value stores does not satisfy this
contract. If the timing tool cannot support this boundary, change and document
the harness before acceptance, not the result label. All arrays are thread-local.
Both representations have atomic element accesses with plain ordering; this is
not volatile ordering or atomic read-modify-write. Holder replacement allocates
a new immutable holder; it does not mutate the source.

## Metrics and ownership

- Time: ns/op, normalized by the actual call count of each sample batch. Retain
  raw times and divisors. Report five independent fork means and their variation.
- Allocation: bytes/op on the executing thread, including allocations from the
  operation and measurement boundary. Exclude setup/reset by counter placement.
  Record unadjusted byte deltas, operation counts, and a separately measured
  empty boundary. Do not silently subtract overhead or count other threads.
  A zero result needs a verified counter and an escaping/consumed result.
- Graph: bytes reachable from named roots, not dominator-based retained heap.
  Count each identity object once across the whole root set. Include headers,
  padding, reference slots, and reachable payload objects. A flat payload belongs
  to its enclosing layout; never add a fictitious payload object for that slot.

For records and replaced holders, measure G(source), G(result), and the deduplicated
G(source,result). Newly reachable bytes are G(source,result) minus G(source),
not G(result) minus G(source). This includes a replacement payload if it was not
reachable from the source, even when the harness prebuilt it. Report that payload
ownership separately from operation allocation. Shared boxed values are counted
once per graph, irrespective of JVM caches; external harness roots are excluded.

For arrays, measure the complete array root before and after the store. The
reference array initially shares one Point payload. After a store it reaches two
payload values; a flat array still has no separate element objects. Read cases
reuse the corresponding input graph measurement, labelled as input size, not
new allocation or newly retained data. Field-read cases likewise name the input
holder root. Keep repeated graph observations distinct from operation samples.

A metric outcome is measured, unavailable (with reason), or failed (with error).
An unavailable reading is not zero. Under the design, a demonstrated measurement
limit with retained probe evidence may remain unavailable at completion. Missing
implementation and failed processes do not qualify. Flat-layout support must be
checked independently: reflection-based traversal can box flat fields and invent
object sizes. Use the existing JDK counter and graph agent only for layouts and
operations validated by controls. Do not block supported metrics on unresolved
physical value-box accounting or publish a ratio with an unavailable side.

## Budget and next gate

There are 34 timed representation measurements per fork, or 170 over five forks.
At the existing five-second budget this is 850 seconds of configured measurement
limits, not a wall-clock guarantee. Allow 30–60 minutes initially for startup,
warmup, capability probes, and separate memory runs. This estimate needs a smoke
run. Memory sample counts, overhead controls, and exact counter tooling remain
open; do not start acceptance until those budgets and commands are fixed.

The current execution order is in `plan.md` and `steps.md`. The control findings
below record evidence and its limits; their earlier open-work statements do not
restart completed investigations. Inventory tests alone do not prove executable
fixture equivalence. Validate each remaining measured path during integration.

## Allocation counter controls

Use the JDK-provided `com.sun.management.ThreadMXBean` allocation counter in
separate memory processes. Its version is the selected JDK build; no Maven or
runtime dependency is added. Run `JAVA_HOME=<jdk> bash dev/bench/memory/allocation-probe.sh`
from the repository root. Both pinned JDKs pass the initial controls in
`evidence/allocation-controls/`. The probe fails with a nonzero exit on a failed
assertion or unavailable counter.

The probe warms each path, allocates the root array before sampling, and retains
all 4096 byte arrays until after the counter read. It records raw byte deltas;
it does not subtract overhead. The non-allocating control records zero. Increasing
each payload from 64 to 128 bytes increases the total by exactly 4096 × 64 bytes.
A worker thread independently obtains the same allocating count. These controls
validate basic counter operation and the divisor, not flat-value allocation
or reachable graph size. Validate those boundaries and
actual case operations before approving the complete memory tool path.

Cross-thread controls in `evidence/allocation-thread-scope/` now verify counter
scope on both pinned JDKs. Volatile handshakes place the complete worker
allocation interval inside the parent counter interval. Thread creation, root
setup, joining, and assertions are outside the parent interval. The worker
allocates 327680 bytes while the parent records zero, without overhead
subtraction. Twenty repeated controls precede the recorded sample. This checks
thread exclusion, not flat storage or graph accounting.

Flat-array controls run with the pinned EA through
`JAVA_HOME=<ea> bash dev/bench/valhalla/allocation.sh`. Evidence is in
`evidence/flat-allocation/`. Instance-specific Unsafe queries establish a
16-byte base and four-byte stride. The 4096 escaping arrays allocate 229376
bytes at length 4 and 16941056 bytes at length 1024. The difference matches
4096 × 1020 × 4. Both totals include 24 extra bytes per API call beyond the
array layout. No overhead is subtracted. This validates allocation growth,
not a pure array-size reading or graph traversal. Flat-field controls remain open.


Field controls use `JAVA_HOME=<ea> bash dev/bench/valhalla/field-allocation.sh`
and a separate JDK Instrumentation agent. `evidence/preboxed-field-allocation/`
retains the matched typed and Object-argument controls. Each of 4096 flat and
preboxed reference holders allocates 16 bytes, equal to its shallow size.
Typed reference construction allocates 40 bytes, including a 24-byte payload
box. The Object argument is boxed before sampling; no cost is subtracted.
The original typed constructor remains unchanged. These controls validate
allocation boundaries, not reachable-graph accounting or final case costs.

## Value-payload identity boundary

The real EA control shows that IdentityHashMap merges separately constructed,
equal Point values. It distinguishes changed payloads and ordinary identity
containers. This does not establish whether equal payload boxes share physical
storage. Do not use value equality as proof of physical graph deduplication.
Reference-container graph sizes remain unavailable pending a verified physical
accounting method. Record traversal remains open. Design is unchanged.

## Allocation operation divisor

`evidence/allocation-divisor/` records 20560 bytes for 257 escaping 64-byte
arrays and 327680 bytes for 4096 arrays on each pinned runtime: 80 bytes/op
in both batches. Root arrays are allocated before sampling. The zero-operation
boundary records zero raw bytes but has no defined bytes/op result. No overhead
is subtracted. Existing cross-thread and non-allocating controls also pass.
These controls complete counter validation, not case-specific cost validation
or physical value-box graph accounting.

## Cross-fork summaries

The pure summary gives each fork equal weight after normalization by that
fork's operation count. It reports the mean, minimum, maximum, and sample
standard deviation. These describe variation, not a confidence interval.
All fork outcomes and raw inputs remain attached. If any fork has no measured
value, the summary is unavailable and has no numeric summary fields; failed
forks retain their status and reason. Missing fork identities are errors.
This summary does not apply the S-03 parity rule.

## Raw EDN envelope

`closed-record.bench.transport` encodes one schema-version 1 map with `:run-id`,
`:revision`, `:configuration`, `:fork`, and raw `:results`. The reader requires
an exact expected association supplied by the parent. Result configuration and
fork must also match the envelope. Forks are numbered 1 through 5 here; the
legacy S-03 command keeps its existing numbering and format.

Partial and empty result vectors are valid failure evidence, not coverage proof.
The transport validates metrics but does not store derived normalized values.
It rejects trailing EDN forms, unknown tags, duplicates, and invalid identities.
This pure boundary does not yet write manifests or start child processes.

## Required manifest metadata

`evidence/create-run!` requires a nonempty `:command` vector of nonblank
strings and a `:runtimes` map keyed by exactly the planned child configurations.
Each runtime requires nonblank `:version` and `:java-home` strings and an
explicit `:flags` vector of nonblank strings (empty is valid). Extra provenance
fields are preserved. Validation occurs before directory creation.
These are caller declarations, not runtime verification. The run command
identifies the invocation; each actual child command is saved separately.
Machine, timestamps, tool versions, dirty state, and layout provenance still
need enforcement before acceptance. Generated child commands also need
validation before launch.

## Changed-store timing working set

The replacement timing boundary uses 1,024 distinct arrays per layout and
length. Each invocation changes index zero once per array, from 17 to 23.
Creation and slot-only reset are outside timing. Both layouts use the same
array count and access order, not the same physical byte footprint. The timed
loop includes working-set traversal; results describe this batch workload,
not an isolated hot-array store. Normalize each batch by 1,024 stores in addition
to the Criterium invocation count. Retain both divisors. Do not subtract loop
or timer overhead. Graph sizes remain per array, not per batch.

`StoreBatch` implements this boundary; `StoreBatchCheck` checks all elements,
full array identity uniqueness, repeated reset/store results, and invalid sizes.
Criterium integration and clock-resolution verification remain required before
acceptance. The earlier index-one diagnostic is not this contract.
