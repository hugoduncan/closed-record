# Actual store traversal clock check

Run `bash munera/open/019-space-time-performance/evidence/experimental-traversal-clock/run.sh`
from the repository root after the existing Valhalla fixture setup. The command
uses the pinned runtime and actual StoreBatch class, not StoreBatchProbe's old
full-array reset. Source changes are in the commit containing this evidence;
environment.txt records the parent revision and machine. Exit: 0, first attempt.

Each layout/length has 2,000 warmup traversals and 1,000 measured traversals.
Creation and index-zero reset are outside each interval. Each traversal changes
1,024 distinct arrays. A volatile sink consumes the returned roots after the
interval. Final values and untouched slots are checked outside all intervals.
No mocks, overhead subtraction, duration floors, or speed assertions are used.

| Layout | Length | Zero samples | Minimum ns/traversal | Median ns/traversal |
| --- | ---: | ---: | ---: | ---: |
| Flat | 4 | 0 | 208 | 291 |
| Reference | 4 | 0 | 666 | 709 |
| Flat | 1024 | 0 | 10958 | 11041 |
| Reference | 1024 | 0 | 875 | 958 |

The actual traversal avoids the earlier single-store zero-duration problem in
this sample. The smallest duration spans only about five observed 41/42 ns clock
steps. Quantisation and amortised timer cost remain; this does not establish an
error bound or an intrinsic store latency. Different physical working sets are
part of the disclosed contract. This is a single-JVM clock diagnostic, not a
Criterium result, independent-fork evidence, or acceptance evidence.

Next compile StoreBatch in a new identified five-fork timing command and retain
its raw invocation/store divisors and saved report. Assess that run before
closing milestone 2. Do not replace experimental-timing-01 or reopen completed
memory integration.
