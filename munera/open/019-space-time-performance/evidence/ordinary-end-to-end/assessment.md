# Ordinary end-to-end attempt 01

Not acceptance evidence. All commands in `run.sh` exited zero on pinned Temurin
21.0.10+7-LTS. The harness revision is in `revision.txt`. Each metric has five
independent forks, both widths, and both representations. `report.stdout` is the
unmodified combined report. Do not publish its timing values as valid performance.

## Duration and budgets

Wall-clock seconds (whole-second clock): timing 40, allocation 4, agent preparation
1, graph 4, reporting 0. These are observed durations, not configured budgets.
Timing used a five-second per-site limit; this is a limit, not elapsed duration.
Allocation used 10,000 operations and 20 warmup batches.

## Memory observations

Both representations allocate 40 bytes/op at width 3 and 96 at width 16 in every
fork. The final 19 warmup batches match the final assoc sample. Final boundary
counts are zero; initial boundary counts sometimes include 56 bytes. No overhead
was subtracted. These budgets are suitable for this observed allocation workload;
stable allocation does not prove timing/JIT convergence.

Graph counts agree in every fork and representation:

| Width | Result bytes | Source/result union bytes | Union minus source bytes |
| --- | --- | --- | --- |
| 3 | 112 | 176 | 64 |
| 16 | 480 | 600 | 120 |

The extra 24 bytes in graph increments versus allocated record bytes are the
prebuilt replacement Long, reachable from the result but not the source. It is
not an allocation within assoc. Shared payloads are counted once in the union.
Agent preparation and runtime hashes are checked by the existing saved reader.
The agent is absent from timing flags.

## Timing audit failure

The report gives 0.029–0.064 ns/op across individual forks. For example, timing
child 0 width-3 closed-record has a raw estimate of 320.1179487179487 ns and
10,000 operations. Positive numbers and valid transport do not establish valid
measurement. This result is suspect and cannot close milestone 1.

Next inspect the actual compiled measurement and Criterium batch data to locate
folding, expression factoring, or unit/divisor errors. A direct symbolic check of
`measured/expr` for qualified assoc shows three factored arguments and an assoc
call; that alone does not validate the nested benchmark macro or generated loop.
No cause is established yet. Add a contract regression for the demonstrated cause,
then use a new run identity after the fix. Preserve this attempt unchanged.

No collector code changed in this pass. Existing real-file report tests verify
readers and rendering; they cannot detect an invalid measured expression merely
from positive raw numbers. Experimental collection and full acceptance remain open.
