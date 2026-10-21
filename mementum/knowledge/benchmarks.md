---
title: Benchmarks against defrecord (criterium 0.5, V-11)
status: active
category: tooling
tags: [bench, criterium, jit, blackhole, variance, locale, format, macro, call-site, V-11, S-03]
related: [dev/bench/closed_record/bench.clj, dev/bench/closed_record/bench/report.clj, test/closed_record/bench_report_test.clj, mementum/knowledge/dev-tooling.md, ramora/defclosedrecord/purpose.md]
depends-on: [deps.edn]
---

💡 Rules for the bench (`clojure -M:bench`) and for a change to it. Observed in task 010 (Clojure 1.11.4,
criterium 0.5.243-ALPHA, OpenJDK 25, arm64). The pass rule (S-03) is in `purpose.md`.

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

- ❌ `(.a ^P x)` gives about 0.06 ns: the JIT hoists the field read. The row passes with no signal. A real field
  measurement needs a body that the JIT cannot hoist. Not solved yet.
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
