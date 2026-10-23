---
title: Benchmarks against defrecord (criterium 0.5, V-11)
status: active
category: tooling
tags: [bench, criterium, jit, blackhole, variance, locale, format, macro, call-site, V-11, S-03]
related: [dev/bench/closed_record/bench.clj, dev/bench/closed_record/bench/report.clj, test/closed_record/bench_report_test.clj, mementum/knowledge/dev-tooling.md, ramora/defclosedrecord/purpose.md]
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
  the loop) and `assoc` gave `P / R` about 1.01. With 0.5.251: `(.a x)` gives 0.30 / 0.36 ns, and `assoc` gives
  `P / R` 1.44–1.64 (8.6–11.0 ns against 6.0–6.7 ns). It stays about 1.44 when a macro keeps `:a` literal. The
  `assoc` gap is a real emitter cost that the folding hid.
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

## Known limits

- ✅ (0.5.251) `(.a ^P x)` is now measured: the opaque barrier stops the lift. With 0.5.243 it gave about
  0.06 ns, because the JIT lifted the read out of the loop.
- ❌ Results near the 1.05 ratio change from run to run on one machine (`=`: 1.086 fail, then 0.957 pass;
  `assoc`: 1.111 in a 1 s run, 1.011 in a 10 s run). One run does not prove S-03. A stable bench needs more
  (for example several JVM forks).
- 🎯 The acceptance run is the first full run of the fixed harness. Do not choose a later run that passes.
  ❌ Capture the full output of each run to a file (`clojure -M:bench | tee <file>`). In task 010 the acceptance
  run output was only partly captured, so its table is lost.

## Code structure

- 🔁 Keep the pure layer (`closed-record.bench.report`: stats, pass rule, table, exit code) in a namespace with
  no criterium dependency. The `:test` alias adds `dev/bench` to the paths and tests it with injected
  `last-bench`-shaped maps. criterium stays out of `:test`. Only `-main` has side effects (`System/exit`).
- ❌ `format` uses the JVM default locale. With `de_DE`, `1.23` prints as `1,23`, and an interval
  `[1,20, 1,30]` is ambiguous. Format numbers with `(String/format java.util.Locale/ROOT ...)`. To test, run the
  suite with `-Duser.language=de -Duser.country=DE`.
- To use the `:bench` classpath in a REPL or with `-e`: `:main-opts` in `:bench` runs `-main` (a 100 s run). Add an
  alias that clears it: `clojure -Sdeps '{:aliases {:e {:main-opts []}}}' -M:bench:e -e ...`.
