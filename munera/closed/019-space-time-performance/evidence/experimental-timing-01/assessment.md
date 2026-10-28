# Experimental timing attempt 01

All five independent children exited zero on pinned Valhalla. The saved envelopes
contain 90 measured timing rows for all nine experimental pairs. Child elapsed
seconds were 76, 67, 83, 68, and 80 (374 total, excluding fixture preparation).
Each site used a one-second diagnostic budget. This is not acceptance evidence.

`run.sh` records the command, source revision, machine, runtime, fixture checks,
raw EDN, streams, process exits, and durations. The existing checksum-verified
`target/019-timing-overlay` was used; each row retains its observed tool identity.
No memory agent was loaded. The source harness was committed before collection.

Reproduce the saved table from the repository root, without measurement:

```sh
clojure -Srepro -Sdeps '{:paths ["src" "dev/bench" "target/019-timing-overlay"]}' -M munera/open/019-space-time-performance/evidence/experimental-timing-01/report.clj
```

The report checks process exits, expected associations, matching runtime
observations, known corrected collector identity, budgets, and complete timing
coverage through the existing metric table. It does not change raw evidence.
Means weight forks equally; ranges and sample SD are not confidence intervals.

## Audit and remaining work

Large flat-array construction is substantially slower than reference-array
construction in this attempt. Preserve this result. Small flat-array reads vary
from 0.234 to 18.436 ns/op; small flat stores vary from 1.429 to 21.039 ns/op.
Reference stores are also highly variable and below a typical timer-call cost.
The reset wrapper sums single-operation durations; the actual clock resolution
and zero-duration contributions need inspection before these timings can support
an acceptance claim. Successful validation alone does not establish measurement
quality. Do not rerun to select favourable values or assert a cause without evidence.

Mock-free generated-expression and store-boundary Scry tests pass on pinned
Valhalla: 4 tests, 250 assertions, exit 0, empty failure directory. The first
verification command selected a nonexistent reset-measured-test namespace and
exited 1 before tests; the corrected selector uses store-boundary-test. No tests
or production code changed. Report script formatting and lint pass.

Milestone 2 remains open: audit timing resolution/variation, connect separate
allocation and graph outcomes, and finish the experimental report. Reference
value-box graph limits remain distinct from missing collection. No human decision
blocks that work.

## Saved read timing assessment

Reproduce [the read audit](read-audit.md) with `bb
munera/open/019-space-time-performance/evidence/experimental-timing-01/read-audit.clj`
after the report command above. It checks 90 total rows and all 30 read rows,
then prints raw batch estimates, divisors, and actual pair positions. No new
measurement was made and no fork was excluded.

Small flat-array reads are 18.189, 0.234, 18.436, 0.239, and 18.170 ns/op in
fork order. The two fast forks run the reference representation first. Their
batch divisor is 10,000; the other three divisors are 625, 588, and 625.
The difference is therefore present in the saved batch estimates and divisors,
not introduced by report rounding or unequal fork weighting. Large flat-array
reads remain 18.068–18.456 ns/op in both pair positions. Holder reads also lack
the small-array split. Pair position alone does not explain all read sites.

This is an order association, not proof of a compiler cause. These envelopes
retain bootstrap mean estimates and batch sizes, not warmup trajectories, raw
time samples, or compiled code. They cannot establish convergence or show why
the small-array fast mode occurs. Allocation was measured in separate processes;
its box counts cannot establish boxing in these timing children. The read
boundary uses ordinary Criterium batches, not the single-store reset timer, so
the store clock audit does not establish a read measurement limit.

Keep all five forks and the full range. Do not publish 11.054 ns/op as a stable,
order-independent small flat-array read cost, infer parity, increase budgets
without evidence, or rerun to select one mode. The saved-read audit is complete;
the cause remains unknown and must be disclosed with any later evidence. This
is not an unavailable metric or an acceptance approval. Store timing and graph
integration remain required before milestone 2 can close.
