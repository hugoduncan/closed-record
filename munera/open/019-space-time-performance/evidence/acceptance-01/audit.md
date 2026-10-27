# Acceptance inventory audit

All 490 inventory slots have evidence: 430 raw metric outcomes and 60 original
S-03 per-call statistics. These are different evidence formats. Do not convert
legacy means into invented raw batches or claim that 490 raw samples exist.

[Machine-readable audit](audit.edn) records 355 measured raw outcomes, 75
unavailable graph outcomes, and no missing or failed supported outcomes. The
60 original statistics cover six expressions, two representations, and five
zero-based forks. All original S-03 comparisons pass. See the
[S-03 assessment](s03/assessment.md) for its field test and transport limits.

## Checks and reproduction

The bounded [audit script](audit.clj) reuses the ordinary saved suite reader and
the three experimental report scripts. These check process exits, runtime and
tool observations, fixed budgets, and raw divisors. The existing complete-result
validator checks every non-legacy inventory identity, duplicate, unit, graph root,
and fork. The legacy decoder checks collector identity and original statistics;
the audit also checks exact inventory expression order. No measurements run.

From the repository root, with the verified overlay prepared as described in
[the command index](README.md):

```sh
clojure -Srepro -Sdeps '{:paths ["src" "dev/bench" "target/019-acceptance-overlay"]}' -M munera/open/019-space-time-performance/evidence/acceptance-01/audit.clj
```

Two successful executions on Temurin 21 produced identical EDN. Existing metric
and table tests pass: 8 tests, 127 assertions; Scry exit zero and no failure files.
Focused formatting and lint pass. The first audit execution encountered a report
namespace alias conflict; each existing report now loads in a separate namespace.
No capture or report was changed, and no measurement was repeated.

## Interpretation and ownership

Time is ns/op; allocation is current-thread bytes/op; graph is bytes per named
root, without a divisor. Experimental stores divide by Criterium invocations
multiplied by 1,024 changed stores. They include traversal and timer costs, not
slot resets. Allocation sums five final batches of 1,024 operations; its 20
warmup batches remain separate. Ordinary allocation keeps raw boundary controls
without subtraction. Equal controls do not prove compilation convergence.

Graph outcomes preserve result, source/result union, and source-relative increment
where the inventory requires them. Shared preboxed ordinary payloads are not
newly owned by each result. Flat payloads belong to container storage. Reference
experimental graphs remain unavailable because physical value-box identity is
not established; see [identity evidence](../value-identity/) and
[root controls](../experimental-graph-operations/). All 75 unavailable outcomes
are confined to those reference graph roots. No total-space ratio is supported.

Saved tables weight forks equally and retain ranges and sample standard deviation,
not confidence bounds. Preserve wider-record slowdown, flat construction cost,
read allocation, small-flat timing variation, and timer limits from the
[capture assessment](assessment.md) and diagnostic assessments. Coverage does not
establish convergence or a universal performance claim.

The public page and final repository checks remain open. The inventory's
`acceptance-ready? false` flag and partial-report disclaimers are unchanged;
this audit associates existing evidence, not a new universal acceptance runner.
