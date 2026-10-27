# Performance

These measurements describe one machine and fixed workloads, not a speed
guarantee. Closed records and `defrecord` have similar costs in the original
three-field cases. Wider records and experimental storage have different costs.

## Evidence and runtime

[Acceptance attempt 01](../munera/open/019-space-time-performance/evidence/acceptance-01/README.md)
retains commands, revisions, JVM flags, tool identities, raw results, and exits.
The machine is a macOS arm64 MacBook Pro. Clojure is 1.11.4. Ordinary records use
Temurin 21.0.10+7-LTS; external Java value types use Valhalla
27-jep401ea3+1-1. These tools do not change library runtime requirements.

Each comparison uses five independent JVMs and alternates pair order. Tables
below show equal-weight fork means; brackets show the minimum and maximum,
not confidence intervals. Linked full tables also report sample standard deviation.
Time, current-thread allocation, and reachable graph size are separate metrics.
No boundary cost is subtracted. Graph size is not process memory or GC retention.

## Closed-record assoc

A changed declared value produces a new record and preserves its source.
Matched widths use shared preboxed payloads. Allocation excludes payload setup;
graph sizes include reachable payloads once per physical object.

| Width | Type | Time ns/op [range] | Allocation bytes/op | Result bytes | Source/result union bytes | Increment bytes |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 3 | closed record | 7.283 [7.134–7.385] | 40 | 112 | 176 | 64 |
| 3 | defrecord | 7.485 [7.103–7.701] | 40 | 112 | 176 | 64 |
| 16 | closed record | 19.040 [11.389–22.242] | 96 | 480 | 600 | 120 |
| 16 | defrecord | 17.419 [11.683–21.902] | 96 | 480 | 600 | 120 |

Memory counts were constant across forks. Increment means union minus source,
not bytes allocated. See [full results and allocation controls](../munera/open/019-space-time-performance/evidence/acceptance-01/ordinary/report.stdout).
The wider closed-record mean is slower; these results do not establish parity
for arbitrary record widths.

The [original six-case report](../munera/open/019-space-time-performance/evidence/acceptance-01/s03/report.md)
passes the existing S-03 ratio-or-overlapping-95%-interval rule for lookup, get,
field access, assoc, cached hash, and equality. That rule applies only to those
original three-field cases. Their retained per-call statistics are not raw batches.

## Experimental arrays and fields

These are **external Java Point fixtures, not closed records**. A Clojure type
hint does not request flat or null-restricted storage. The internal
`ValueClass.newNullRestrictedAtomicArray` API and `@NullRestricted` annotation
require the pinned EA, preview support, and module exports. Capability probes
verify null rejection and physical layout separately. See
[APIs, flags, and executable probes](../munera/open/019-space-time-performance/features.md)
and [operation contracts](../munera/open/019-space-time-performance/measurement-contract.md).
These internal APIs are not supported library features.

Both array layouts have atomic element access with plain ordering, not volatile
ordering or atomic read-modify-write. Stores mutate; they are not persistent
assoc. Timing traverses 1,024 distinct arrays at index zero, with changed values
and untimed slot resets. Traversal, input, and timer costs remain included.
Reads return scalar values. Construction uses prebuilt payloads.

| Array length | Operation | Reference ns/op [range] | Flat ns/op [range] | Reference / flat allocated bytes/op |
| --- | --- | --- | --- | --- |
| 4 | construct | 27.244 [26.905–27.887] | 54.066 [53.582–54.659] | 32 / 32 |
| 1024 | construct | 610.585 [592.124–616.700] | 7511.500 [7423.593–7714.463] | 4112 / 4112 |
| 4 | read | 0.392 [0.386–0.400] | 11.494 [0.227–19.263] | 0 / 24.015 |
| 1024 | read | 0.390 [0.387–0.399] | 18.416 [18.078–18.960] | 0 / 24 |
| 4 | store | 0.694 [0.661–0.744] | 6.858 [0.263–11.438] | 0 / 0 |
| 1024 | store | 1.123 [1.024–1.187] | 11.243 [11.138–11.401] | 0 / 0 |

Holder replacement creates a new holder and preserves the source. Nullable
reference and null-restricted flat fields contain equivalent non-null values.
Null restriction alone does not imply flattening.

| Holder operation | Reference ns/op [range] | Restricted ns/op [range] | Reference / restricted allocated bytes/op |
| --- | --- | --- | --- |
| construct | 1.959 [1.945–1.997] | 2.099 [2.082–2.120] | 16 / 16 |
| read | 0.437 [0.433–0.445] | 0.241 [0.239–0.244] | 0 / 14.400 |
| replace | 1.965 [1.948–1.989] | 2.083 [2.068–2.099] | 16 / 16 |

Restricted reads allocate 0–24 bytes/op across forks. Small flat-array reads
allocate 24.000–24.038 bytes/op. Other listed allocation counts are constant.
Pair-order effects and small-flat timing modes prevent convergence claims.
Store batches avoid sampled zero durations, but clock granularity and working-set
costs remain. Sub-nanosecond normalized means are not single-operation clock readings.

Flat array graphs are 32 bytes (length 4) and 4112 bytes (length 1024), including
inline payloads. Flat holders have 16-byte result graphs. Construction and
replacement have 32-byte source/result unions and 16-byte increments; reads alias
the source, with a 16-byte union and zero increment. All are constant across forks.
**Reference graphs are unavailable**, not zero: physical value-box identity cannot
be established. No total-space ratio is supported. See
[identity evidence](../munera/open/019-space-time-performance/evidence/value-identity/).

Full experimental tables: [time](../munera/open/019-space-time-performance/evidence/acceptance-01/experimental-timing/report.md),
[allocation](../munera/open/019-space-time-performance/evidence/acceptance-01/experimental-allocation/report.md),
and [graphs](../munera/open/019-space-time-performance/evidence/acceptance-01/experimental-graph/report.md).

## Reproduction and limits

Follow the [frozen commands and budgets](../munera/open/019-space-time-performance/evidence/acceptance-01/README.md)
from the repository root. Use a new output identity; do not overwrite evidence.
Ordinary timing uses five seconds per site; experimental timing uses one.
Allocation uses fixed warmup budgets, not a convergence test. The verified
Criterium correction changes planning only, not raw samples. Timing runs exclude
memory agents. Saved report and audit commands need no new measurements.

The [coverage audit](../munera/open/019-space-time-performance/evidence/acceptance-01/audit.md)
accounts for 430 raw outcomes and 60 original statistics, including 75 unavailable
reference graph outcomes. No supported slots are missing or failed. Earlier
failed diagnostics remain in the task evidence; they are not substituted for
this first complete attempt. Coverage does not establish universal performance.
