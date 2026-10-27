# Experimental memory coverage decision

The diagnostic memory part of milestone 2 is complete. This is not acceptance
approval. Timing remains unresolved. No collector, budget, or source result was
changed for this assessment.

## Evidence and coverage

- [Allocation report](experimental-allocation-01/report.md): 90 measured outcomes,
  nine cases × two representations × five independent forks. The saved validator
  checks every raw divisor and batch total, 20 warmup batches, and five final
  batches of 1,024 operations. No memory agent enters these children.
- [Graph report](experimental-graph-01/report.md): 150 outcomes across five
  independent forks. The six array cases each have two result-array roots; the
  three field cases each have two representations and three ownership roots.
  There are 75 measured flat roots and 75 unavailable reference roots. Runtime
  observations and independently declared agent hashes reconcile.
- [Allocation assessment](experimental-allocation-01/assessment.md) and its
  [batch audit](experimental-allocation-01/audit.clj) retain variation, including
  restricted-holder reads at 0–24 bytes/op. This is not a stable intrinsic cost.
- [Graph assessment](experimental-graph-01/assessment.md) retains the report-only
  failure and the original successful children. No recollection was needed.

## What the numbers mean

Both array layouts allocate 32 bytes for length 4 and 4,112 bytes for length
1,024 when constructed from prebuilt payloads. Both holder layouts allocate
16 bytes for construction and replacement. These are new allocation counts,
not total ownership: the reference payloads existed before the counter started.
Equal allocation counts therefore do not establish equal retained graphs.

Flat array graphs are 32 and 4,112 bytes. Flat holder result graphs are 16 bytes.
A new holder and its preserved source have a 32-byte union and a 16-byte
increment. A holder read aliases the source: its union is 16 bytes and increment
is zero. These counts agree across forks and include inline flat payloads.

Reference graph totals remain unavailable. The [value-identity probe](value-identity)
and [operation controls](experimental-graph-operations) demonstrate why value
projection cannot establish physical box identity. Shallow reference-container
sizes cannot replace missing reachable totals. Do not calculate a total-space
ratio or treat an unavailable root as zero.

Flat array reads allocate about 24 bytes/op in this boundary despite primitive
result sinks. Restricted-holder reads vary with pair order. Keep these outcomes;
do not infer allocation elimination from the scalar result type. Stores allocate
zero measured bytes here, but that is neither zero retained space nor evidence
about timing costs. Allocation and graph children are separate processes.

## Reproduction and verification

From the repository root:

```sh
base=munera/open/019-space-time-performance/evidence
cp="dev/bench:$(clojure -Srepro -Spath)"
for metric in allocation graph; do
  java -cp "$cp" clojure.main "$base/experimental-$metric-01/report.clj" \
    > "/tmp/019-$metric-report.md"
  cmp "/tmp/019-$metric-report.md" "$base/experimental-$metric-01/report.md"
done
bb "$base/experimental-allocation-01/audit.clj"
```

Both existing saved validators pass and reproduce the committed reports byte for
byte. The allocation audit assertions pass. These are real saved-data checks,
not mocks or new measurements. Retain separate run IDs, source revisions, flags,
and tool declarations; this assessment does not merge them into one acceptance
run. Next resolve store timing, then freeze commands and budgets before acceptance.
