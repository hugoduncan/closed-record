# Experimental graph operation controls

Pinned Valhalla, real Instrumentation, no mocks. Reproduce from the repository
root with `bash munera/open/019-space-time-performance/evidence/experimental-graph-operations/run.sh`.
The environment file identifies the base revision; this commit adds the test.

All 11 operation/root controls pass, plus the existing flat graph controls.
Flat arrays retain 32 and 4112 bytes at lengths 4 and 1024 across construction,
read, and changed store. Stores return the same container and preserve other
elements. Independent physical base/stride agrees with the agent size.

Flat holders retain 16 bytes. Read roots alias: union is 16 and increment is
zero. Construction and replacement use distinct source/result holders: union
is 32 and increment is 16. Replacement preserves the source value. These counts
include inline primitive payloads, not temporary materialized boxes.
Reference arrays and holders are rejected, not reported as shallow graph totals.

Attempt 1 exposed Java varargs expansion: passing an Object[] directly treated
its elements as roots. The test now casts the array to Object, which preserves
the container root. The failed output and exit remain here. Production collection
must use the same explicit root boundary.

This is single-process operation verification, not five-fork inventory capture
or acceptance. Next connect these root semantics to saved graph rows, use the
verified agent identity path, and retain evidenced unavailable reference roots.
Timing store quantisation remains separate work.
