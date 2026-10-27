# Acceptance attempt 01: frozen commands

This directory retains the first acceptance matrix captures. All six metric
commands completed successfully. Original S-03 and field verification also pass;
the [complete coverage audit](audit.md) also passes. See [S-03 assessment](s03/assessment.md). See [capture assessment](assessment.md).
Run commands from the repository root. Never overwrite a result or retry under
this identity. Preserve failed processes and reports. A correctness fix requires
a new identity; an unfavourable result does not justify a rerun.

## Fixed workload and budgets

Use `dev/bench/cases.edn` unchanged: 17 comparisons and five independent forks,
with alternating pair order. Use Temurin 21.0.10+7-LTS for ordinary cases and
Valhalla 27-jep401ea3+1-1 for experimental cases, with Clojure 1.11.4.

- Original S-03: six operations, five seconds per timing site, five forks.
  Keep the original ratio-or-overlapping-95%-interval rule and field test.
- Ordinary assoc: five seconds per timing site; allocation uses 10,000 operations
  and 20 warmup batches. Graph measurements use the verified ordinary agent.
- Experimental timing: one second per site, as in corrected diagnostic attempt
  02. Stores traverse 1,024 distinct arrays with untimed slot resets.
- Experimental allocation: 20 warmup and five final batches of 1,024 operations.
- Experimental graph: flat roots measured; reference roots explicitly unavailable
  because physical value-box identity is not verified. This is not zero bytes.

These budgets preserve the tested workloads, not a convergence guarantee. Keep
small-flat timing mode variation, clock granularity, traversal cost, allocation
order effects, and unavailable reference graphs in the published assessment.
Measure elapsed duration; configured timing limits are not wall-clock estimates.

## Commands

First prepare the corrected overlay with the pinned benchmark JAR (resolve it
with `clojure -Srepro -Spath -M:bench`):

```sh
sh dev/bench/timing/prepare.sh "$CRITERIUM_JAR" target/019-acceptance-overlay
```

Execute each committed `run.sh` in these subdirectories, in order:

1. `ordinary`
2. `experimental-timing`
3. `experimental-allocation`
4. `experimental-graph`

The scripts select their JVMs explicitly and retain revision, runtime, machine,
outputs, exits, and durations. Ordinary reporting is included. Experimental
allocation and graph reporting are included. Render experimental timing with:

```sh
clojure -Srepro -Sdeps '{:paths ["src" "dev/bench" "target/019-acceptance-overlay"]}' -M munera/open/019-space-time-performance/evidence/acceptance-01/experimental-timing/report.clj > munera/open/019-space-time-performance/evidence/acceptance-01/experimental-timing/report.md
```

Original S-03 capture must also retain each child's raw envelope: invoke the
existing `closed-record.bench` child with fork numbers **0 through 4**, not the
new matrix's 1 through 5. Use `sh dev/bench/timing/run.sh -m closed-record.bench
FORK RESULT_FILE` on pinned Temurin 21. Decode through `legacy-transport` and
aggregate with the existing `report/fork-stats`, table, and exit-code functions.
The default parent uses temporary result files; do not rely on its table alone.
Record each command, runtime, machine, revision, exit, and elapsed time. Run:

```sh
clojure -Srepro -M:test -m scry.cli --var closed-record.field-value-test/field-declaration-test
```

## Preflight and completion boundary

Saved base and ordinary-test classpaths were resolved on pinned Temurin 21.
Both exclude Criterium, Valhalla classes, agents, and timing overlays. No dependency
or library runtime change was required. Shell syntax checks pass for all four
capture scripts. Reports reuse the validated diagnostic readers and checks.

Before completion, associate all captures here, validate all 490 required matrix
outcomes, and retain S-03 status separately. Unavailable reference roots need
published probe links. Missing or failed supported outcomes block completion.
This reservation and command freeze do not constitute acceptance evidence.
