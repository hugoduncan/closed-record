# Ordinary end-to-end attempt 02

Milestone 1 evidence, not full-matrix acceptance. All five commands exited zero
on Temurin 21.0.10+7-LTS at the committed revision in revision.txt. The original
attempt remains unchanged. Commands, raw rows, runtime/tool declarations, streams,
and the combined report are retained here.

## Corrected cause

The nested `measure` macro constructed assoc with syntax quote, yielding a
`clojure.lang.Cons`. Pinned Criterium accepts that as an expression but its
factoring returned `{:expr nil :arg-vals nil}` for this generated form. It timed
nil, not assoc. Constructing the call with `list` yields a PersistentList and
correct factoring of all three arguments. No unit or divisor change was needed.
The regression feeds the actual macro-generated expression to real Criterium,
checks its arguments, and checks changed-value results for 1, 7, and 100 calls.
It does not assert a timing threshold or mock the tool. The original six-case
macro passes through its caller's expression; it does not construct this assoc.

## Results and budgets

Five independent forks, alternating order, five-second limit per timing site.
Observed wall-clock seconds: timing 46, allocation 4, graph 4, preparation 1,
reporting 0. Limits are not duration predictions. Timing has no memory agent.

| Width | Closed record ns/op mean (range) | defrecord ns/op mean (range) |
| --- | --- | --- |
| 3 | 8.817 (7.955–9.667) | 9.090 (7.988–10.190) |
| 16 | 18.029 (12.841–24.906) | 16.081 (13.797–23.553) |

These are equal-fork means, not parity decisions. Width-16 variation is material;
do not tune or repeat away its slower closed-record mean. Raw batch estimates
are divided once by their recorded operation counts. Opaque argument reads and
result consumption are supplied by the now correctly factored measured form.

Allocation remains 40/96 bytes per operation at widths 3/16 for both types.
The 10,000-operation, 20-warmup-batch controls have 19 final matching assoc
warmups and zero final boundary bytes. No overhead is subtracted. These budgets
are suitable for this ordinary diagnostic, not proof of JIT convergence.
Graph result/union/increment bytes remain 112/176/64 and 480/600/120 across
all forks and both types. The 24-byte replacement payload is prebuilt: graph
increments include it, allocation does not. This confirms ordinary shared-root
accounting, not physical value-box identity on Valhalla.

## Verification

Temurin 21 real Criterium tests: 2 tests, 33 assertions pass. Pure timing and
saved-suite tests: 4 tests, 91 assertions pass. Scry exits zero; no failure files.
Focused formatting and lint pass. Initial verification commands omitted Scry
from the benchmark classpath and used a wrong suite namespace; corrected commands
passed. Full acceptance and experimental measurement remain next.
