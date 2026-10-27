# Original S-03 acceptance

The first five children on pinned Temurin 21 all exit zero. Their total elapsed
time is 97 seconds (19/19/20/19/20). The original six comparisons all pass the
unchanged ratio-or-overlapping-95%-interval rule. See [table](report.md).
This does not imply parity for wider records or experimental fixtures.

`run.sh` records the exact commands, revision, machine, runtime, process output,
and durations. Each raw envelope retains the observed corrected collector
identity and the original per-call statistics. The legacy transport does not
retain raw Criterium batches; these are original S-03 statistics, not new-matrix
raw timing rows. Forks are zero-based and alternate pair order.

The field-declaration Scry test passes: one test, three assertions, exit zero,
with no failure files. No mocks or speed assertions were added.

Reproduce the saved table from the repository root after preparing the verified
overlay, without repeating measurements:

```sh
clojure -Srepro -Sdeps '{:paths ["src" "dev/bench" "target/019-acceptance-overlay"]}' -M munera/open/019-space-time-performance/evidence/acceptance-01/s03/report.clj
```

The saved reader checks all child exits, decodes each envelope, reconciles tool
identity, and uses the existing S-03 aggregation and decision functions. Repeat
rendering is byte-identical. The complete 490-outcome matrix audit remains open;
this original-case result does not replace it.
