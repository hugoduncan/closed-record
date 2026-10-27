# Acceptance matrix capture

Executed the frozen commands at revision
`f292f62fe5ada2cd56a41a38d39858bbdf97c5ee`, without collector changes or retries.
All metric processes and saved report validators exit zero. The raw evidence is
retained here, including runtime, machine, tool identity, budgets, and each fork.

| Capture | Elapsed seconds | Saved report |
| --- | ---: | --- |
| Ordinary timing | 41 | [Combined](ordinary/report.stdout) |
| Ordinary allocation | 4 | [Combined](ordinary/report.stdout) |
| Ordinary graph | 4 | [Combined](ordinary/report.stdout) |
| Experimental timing | 126 | [Timing](experimental-timing/report.md) |
| Experimental allocation | 12 | [Allocation](experimental-allocation/report.md) |
| Experimental graph | 2 | [Graph](experimental-graph/report.md) |

Experimental durations sum child times, not setup or reporting. Ordinary agent
preparation and reporting each rounded to zero seconds. No timing agent was
added. The experimental overlay was prepared with the pinned checksum command
before capture; the ordinary wrapper prepared its own verified overlay.

These are the first acceptance matrix captures, not final acceptance approval.
The reused partial reports deliberately do not assert full acceptance. Original
S-03, its field-declaration test, and the cross-inventory audit remain required.
Do not recollect these results to improve them.

The ordinary width-16 closed-record mean is 19.040 ns/op, with a range of
11.389–22.242. Experimental construction of 1,024 flat elements is slower than
reference construction (7511.500 versus 610.585 ns/op). Flat reads still allocate
about 24 bytes per operation. Preserve these results and the diagnostic limits.
Reference graphs remain unavailable, not zero. Their support evidence is under
[physical identity](../value-identity/) and
[operation roots](../experimental-graph-operations/); the reused graph report's
plain relative path hints refer to the diagnostic evidence parent, not this
nested acceptance directory.

Next audit all 490 required outcomes using the existing inventory validation,
retain original S-03 raw children and status, and publish the bounded evidence
index. Public documentation and final quality checks remain unfinished.

Original S-03 and field verification now pass. The [inventory audit](audit.md)
accounts for all 490 slots, with no missing or failed supported measurements.
Public documentation and final checks remain open; no captures were repeated.
