---
title: Benchmarks against defrecord (criterium 0.5, V-11)
status: active
category: tooling
tags: [bench, criterium, jit, blackhole, variance, locale, format, macro, call-site, V-11, S-03]
related: [dev/bench/closed_record/bench.clj, dev/bench/closed_record/bench/report.clj, dev/bench/closed_record/bench/fork.clj, test/closed_record/bench_report_test.clj, test/closed_record/bench_fork_test.clj, mementum/knowledge/dev-tooling.md, ramora/defclosedrecord/purpose.md]
depends-on: [deps.edn]
---

💡 Rules for the bench (`clojure -M:bench`) and for a change to it. Observed in task 010 (Clojure 1.11.4,
criterium 0.5.243-ALPHA, OpenJDK 25, arm64), updated for 0.5.251-ALPHA (2026-10-23). The pass rule (S-03) is in
`purpose.md`.

## criterium 0.5.251-ALPHA (2026-10-23)

- 🔄 Coordinate: `org.hugoduncan/criterium` (group id changed in 0.5.245). `criterium/criterium` stops at 0.5.243.
- 🔄 Opaque input barrier (criterium #155, PR #160): `measured/expr` stores each argument in an atomic cell and reads
  it with `getOpaque` on each iteration. The JIT no longer lifts or folds the expression. Literal arguments
  (`:a`, `2`) also go through the barrier (`symbolic`: `(let [arg1 x arg2 :a arg3 2] ...)`).
- The reported time includes the barrier, which is not subtracted: about 0.1 ns for a `(do x)` body with one
  reference argument on this machine. The criterium changelog gives 0.2–0.35 ns for each reference argument.
- ❌ Results from 0.5.243 for cheap operations are not valid. In 0.5.243, `(.a x)` gave 0.03–0.06 ns (lifted out of
  the loop) and `assoc` gave `P / R` about 1.01. With 0.5.251, in one JVM (10 s and 3 s runs, before task 011):
  `(.a x)` gives 0.30 / 0.36 ns, and `assoc` gives `P / R` 1.44–1.64 (8.6–11.0 ns against 6.0–6.7 ns). It stays
  about 1.44 when a macro keeps `:a` literal. The `assoc` gap is a real emitter cost that the folding hid.
- The task 011 acceptance run (means over 5 JVM forks) gives `assoc` `P / R` 1.193 (7.93 ns against 6.65 ns), a
  fail. This is the current gap, the start point for the `assoc` task.
- ❌ Still open in 0.5.251: array-typed locals fail (#156), and class symbols and nested forms are hoisted as
  arguments (#157).
- ❌ `ArithmeticException: Divide by zero` in `criterium.collect/batch-size`, also in 0.5.243. It happens when one
  `bench` call site runs a second time in a JVM: the single-eval estimate `t0` is 0 ns. The bench runs each call
  site once, so it does not trigger this.

## Measurement

- 🎯 The bench helper is a macro (`bench-op`), not a function that takes a thunk. `criterium.bench/bench`
  measures the expression at its call site. A thunk adds an `IFn` invoke (about 1 ns or more) at one shared
  site. Keyword lookup is about 0.6 ns, so the invoke hides a `P` / `R` difference and moves `P / R` to 1.
- Bind the record to a local in a `let` around each `bench-op`. A var deref must not be in the measured body.
  criterium (`measured/expr`, uses `&env`) keeps the call head literal and hoists the arguments into state.
- Put the type hint on the symbol in the expression (`(.a ^P x)`), so no reflection occurs.
- 💡 criterium 0.5 `last-bench` values are for one sample (a batch of calls). Divide by
  `[:data :bootstrap-stats :batch-size]`. The batch size differs between runs and between `P` and `R`.
  Mean: `[:data :bootstrap-stats :bootstrap :elapsed-time :mean]` (`:point-estimate`, `:estimate-quantiles`
  with `:alpha` 0.025 / 0.975). Select quantiles by `:alpha`, not by vector order.
- `:viewer :none` prints nothing (not in the `bench` docstring). `:limit-time-s` is evaluated at run time.
- The blackhole `:jvm-opts` must be in the alias. Without them, criterium warns and adds about 3 ns for each
  call. The `CompileCommand: blackhole ...` line at start is a HotSpot notice, not that warning.

## Forks (task 011)

- 🎯 `clojure -M:bench` (no arguments) is the parent. It starts 5 child JVMs (forks 0 to 4), one after the other,
  with its classpath (`java.class.path`), its java binary (`java.home`/`bin/java`) and all its JVM input arguments
  (`RuntimeMXBean/getInputArguments`, no filter). The parent does no measurement.
- A child is `clojure.main -m closed-record.bench <fork> <result-file>`. It runs the six operations in
  `report/ops` order, 5 s for each run. Even forks run `P` then `R` for each operation; odd forks `R` then `P`.
  Each `bench` call site runs once in a child (one thunk for each call site), so the divide by zero fault does
  not occur. The child exits 0 after it writes its result (a fail row is not a fork fault).
- Transport: the child writes EDN (`report/encode-result`) to a temp file; the parent reads it with
  `report/decode-result` (`{:result v}` or `{:error msg}`, no throw). Not stdout: the JVM prints the
  `CompileCommand: blackhole ...` notice to stdout (one line for the parent and each child: 6 in a full run).
- Statistics (`report/fork-stats`): the mean of the 5 fork means, and the Student t interval
  mean ± 2.776 · s / √5 (s with divisor 4). The `:low` / `:high` of a child are not used.
- ❌ A failed fork (exit not 0, missing file, decode error) stops the run: `fork <n> failed: <cause>` on stderr,
  no table, exit 2. The result files stay in the temp dir for inspection.
- `parent` takes the child command function, so the failure paths are tested with `sh` / `rm` / `true` / `cp`
  commands and no JVM.
- First fork run (2026-10-23): fork intervals are wide for `get` and `=` (±0.8 and ±1.4 ns): the variation
  between JVMs is large, as expected.

## Known limits

- ✅ (0.5.251) `(.a ^P x)` is now measured: the opaque barrier stops the lift. With 0.5.243 it gave about
  0.06 ns, because the JIT lifted the read out of the loop.
- ❌ Results near the 1.05 ratio change from run to run on one machine (`=`: 1.086 fail, then 0.957 pass;
  `assoc`: 1.111 in a 1 s run, 1.011 in a 10 s run). One run does not prove S-03. See "Forks".
- 🎯 The acceptance run is the first full run of the fixed harness. Do not choose a later run that passes.
  ❌ Capture the full output of each run to a file (`clojure -M:bench 2>&1 | tee <file>; echo "exit
  ${PIPESTATUS[0]}"`: `2>&1` captures the child stderr and the fork error; `tee` hides the exit code). In task 010
  the acceptance run output was only partly captured, so its table is lost.

## Code structure

- 🔁 Keep the pure layer (`closed-record.bench.report`: stats, encode and decode of the child result, statistics
  over forks, pass rule, table, exit code) in a namespace with no criterium dependency. The `:test` alias adds
  `dev/bench` to the paths and tests it with injected `last-bench`-shaped maps and child results. criterium stays
  out of `:test`. Only `closed-record.bench` and `closed-record.bench.fork` have side effects (process start, file
  I/O, `System/exit`).
- 🔁 The fork layer (`closed-record.bench.fork`: run order `in-order`, `default-child-cmd`, `parent`) has no
  criterium dependency either. The suite tests `parent` with a `child-cmd` of short shell commands (`sh`, `rm`,
  `true`, `cp` of a fixture written with `report/encode-result`; no child JVM), also the fork numbers 0 to 4 of a
  full run; `default-child-cmd` against the state of the test JVM (`java.home`, JVM options, `java.class.path`,
  entry point); and `in-order` with thunks that record their calls. `closed-record.bench` keeps only the criterium part
  (`run`, the child) and `-main`.
- ❌ `format` uses the JVM default locale. With `de_DE`, `1.23` prints as `1,23`, and an interval
  `[1,20, 1,30]` is ambiguous. Format numbers with `(String/format java.util.Locale/ROOT ...)`. To test, run the
  suite with `-Duser.language=de -Duser.country=DE`.
- To use the `:bench` classpath in a REPL or with `-e`: `:main-opts` in `:bench` runs `-main` (a run of 5 forks,
  about 300 s plus JVM start). Add an alias that clears it:
  `clojure -Sdeps '{:aliases {:e {:main-opts []}}}' -M:bench:e -e ...`.
