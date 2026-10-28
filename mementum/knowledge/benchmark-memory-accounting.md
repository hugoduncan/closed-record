---
title: Allocation and reachable-graph accounting
status: active
category: tooling
tags: [bench, allocation, graph, valhalla, boxing, identity, layout]
related: [mementum/knowledge/benchmarks.md, mementum/knowledge/benchmark-measurement-boundaries.md, dev/bench/memory/RecordGraphProbe.java, dev/bench/valhalla/FlatGraphProbe.java, README/performance.md]
depends-on: [deps.edn]
---

💡 Allocation, shallow size, and reachable graph size answer different questions.
Task 019 verified their boundaries with real JVM counters, instrumentation, and
layout probes. Do not infer one metric from another.

## Allocation includes argument adaptation

Specify whether boxing and argument adaptation occur inside the measured interval.
On Valhalla 27-jep401ea3+1-1, passing a typed Point added a 24-byte box to flat-array
construction. A preboxed Object argument prepared outside the counter removed
that allocation. The same control reduced reference-holder construction from
40 to 16 bytes. This isolates these call boundaries, not a general VM guarantee.

Use preallocated result sinks and escaping results. Put setup, counter enabling,
and correctness checks outside the interval. Record same-thread before/after
readings, raw delta, and the actual operation count. Keep loop/store/counter
controls separate; do not subtract their counts. A zero-operation control has
no bytes/op value, even when its raw delta is zero.

Validate counts with independent controls: known escaping arrays, unequal batch
sizes, worker-thread exclusion, physical stride deltas, and shallow holder sizes.
Scalar read results do not prove zero allocation; value payloads can materialize
internal boxes. Keep raw warmup and final counts instead of asserting a universal
zero-byte boundary or convergence.

## Reachability is not allocation or exclusive retention

For source S and result R, record source size, result size, and union size.
Incremental reachable size is `size(S union R) - size(S)`, not
`size(R) - size(S)`. Shared objects count once. Prebuilt replacement payloads can
add reachable bytes without being allocated by the operation. Cached Long payloads
are reachable data, not exclusive retention.

Traverse physical instance reference fields, including metadata, rather than
map entries. Exclude static caches. A bounded walker must reject unknown reachable
types instead of returning plausible partial totals. Verify sharing, distinct
equal objects, nulls, duplicate roots, and cycles with independent shallow sums.

At a Java `Object...` root boundary, cast an Object array to Object to count the
array container. Otherwise its elements become the roots and the container is
omitted. This error recurred when operation-specific graph collection was added.

Reads and in-place stores can alias source and result roots. Fresh construction
or replacement need not. Derive graph roots from the actual operation's ownership,
not from a common result shape.

## Flat values require physical layout evidence

Null restriction, flattening, and atomicity are distinct properties. On the pinned
EA, a nullable Point field also flattened. The non-flat control needed an
Object-declared field; disclose the declared-type and read-cast difference.
Atomic array metadata does not imply volatile memory ordering.

Clojure 1.11.4 closed-record fields remained Object fields on this EA even with
a Point hint. They accepted null and were neither restricted nor flat. Records
remained identity classes. External flat arrays can be payloads, but that does
not make the record itself flat or make mutable payloads immutable. Do not
present external Valhalla comparisons as implemented closed-record features.

Value arrays of one class can have different physical representations. Use
instance-specific layout queries, not class-only Unsafe queries. Count inline
primitive payloads inside the container's shallow size. Reading them during graph
traversal can materialize boxes that are not retained graph nodes.

On this EA, separately constructed equal Points collapsed in IdentityHashMap.
That does not prove physical box sharing. Semantic value identity cannot establish
physical reference-payload deduplication. Mark affected reference graphs unavailable
with probe evidence; continue supported allocation and flat-graph measurements.
Do not publish a total-space ratio when one side is unavailable. Revalidate these
build-specific limits before making claims for a different Valhalla build.

Evidence: task 019 `implementation.md`, especially argument allocation controls,
flat-container accounting, value-payload identity, and experimental operation roots.
Commits `21b4ca1`, `99d5de5`, and `73001a3` retain the controls and failed assumptions.
