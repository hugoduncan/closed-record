---
title: Benchmark timing and harness rules
status: active
category: tooling
tags: [bench, criterium, jit, macro, call-site, variance, S-03]
related: [mementum/knowledge/benchmark-measurement-boundaries.md, mementum/knowledge/benchmark-memory-accounting.md, mementum/knowledge/dev-tooling.md, README/performance.md, ramora/defclosedrecord/purpose.md]
depends-on: [deps.edn]
---

💡 Use these rules when changing performance measurements. They combine tasks
010–012 with task 019's real-tool failures and controls. Public results and
reproduction commands belong in `README/performance.md`, not this page.

## Select the corrected collector

Use `sh dev/bench/timing/run.sh` for timing. It prepares a fresh, checksum-verified
source overlay for pinned Criterium 0.5.251-ALPHA. Direct `:bench` invocation still
loads the uncorrected dependency. Keep the overlay until all children finish.
The coordinate is `org.hugoduncan/criterium`; `criterium/criterium` stops at
0.5.243. The overlay does not change ordinary dependencies or cached JARs.

The initial single-evaluation estimate can be zero even on a fresh call site.
Both batch sizing and sample budgeting divide by it. A fresh JVM, a longer
budget, or a fix to only `batch-size` does not solve this fault. The correction
floors the planning estimate before both divisions; it does not floor raw samples.
Criterium 0.5.253-ALPHA and inspected upstream commit `4e91647` had identical
collection sources and the same fault. Do not upgrade or retry without evidence.

## Verify the measured expression

- Use a macro with a literal operation at each call site, not a measured thunk.
  An `IFn` call can hide differences between cheap operations.
- Runtime ordering can call thunks that contain whole benchmark forms. The
  measured expression must remain literal inside each form.
- Bind inputs outside measurement. Put field hints on the expression symbol,
  such as `(.a ^P x)`, to prevent reflection and timed var dereferences.
- Criterium 0.5.251 stores arguments in atomic cells and reads them with
  `getOpaque` each iteration. This prevents earlier expression lifting, but adds
  input-barrier cost. Do not subtract that cost.
- Array-typed locals and hoisting of class symbols or nested forms remain known
  hazards in the pin. Test the actual generated expression and returned value.
- A nested syntax-quoted assoc form produced a `Cons` that Criterium factored to
  `nil`. Explicit `(list 'clojure.core/assoc source key replacement)` preserved
  the operation. Successful collection and positive timings missed the defect.
  Regressions must check arguments and changed results, not speed thresholds.
- Cheap-operation results from 0.5.243 are invalid evidence: operations could
  be lifted out of the loop. Do not compare them with corrected measurements.

## Statistics and forks

`last-bench` bootstrap statistics describe a batch. Divide its elapsed-time mean
by `[:data :bootstrap-stats :batch-size]`, which can differ by representation.
The mean is at `[:data :bootstrap-stats :bootstrap :elapsed-time :mean]`.
Select interval quantiles by `:alpha` (0.025/0.975), not vector order. Preserve raw
estimates and divisors before per-call reduction; see the measurement-boundary page.

The original harness runs five independent children, forks 0–4, with alternating
pair order. Each literal site runs once per child. This policy limits shared
history but does not cure the zero-estimate fault. Select the parent JVM explicitly:
children inherit `java.home`, classpath, and all ordered JVM input flags.

Original S-03 uses the mean of five fork means and the Student t interval
`mean ± 2.776 * s / sqrt(5)`, with sample SD divisor 4. Child intervals do not
supply that cross-fork interval. Preserve the rule in `bench.report`; exploratory
comparisons do not inherit parity requirements. Equal-weight fork summaries,
ranges, and sample SD are not confidence bounds or proof of convergence.

Original child EDN now uses `bench.legacy-transport`, including collector identity
and unchanged per-call statistics. `fork/parent` requires a declared tool identity.
Do not restore the old bare-vector transport. Use files, not stdout, which also
contains HotSpot blackhole notices. Missing, invalid, or nonzero-exit children
prevent reporting. Process success alone does not establish measurement validity.

## Interpretation and acceptance

C2 method size can explain an assoc gap. A keyword `case` can generate a large
`tableswitch`; above `FreqInlineSize` (325 bytes), hot methods may not inline.
AOT-compile a scratch namespace and inspect `javap -c`; ignore the small bridge
method. Use per-method `CompileCommand=PrintInlining` rather than interleaved
global output. An `IPersistentMap`-hinted direct `.assoc` with boxed values exposes
inlining; concrete `P` hints or primitive-long values can reflect. Pin Clojure in
the scratch project. Task 012 reduced assoc code from 995 to 217 bytes and removed
the measured gap; this is not a universal performance guarantee.

Retain the first complete run of the fixed harness and all failed attempts.
A correctness fix requires a new identified run; an unfavourable result does not.
Capture stderr and the actual command exit when using `tee`. Exit 1 can mean a
parity failure or an uncaught exception: inspect the saved table and evidence.
A report-only fix should reuse existing captures, not repeat measurements.

## Dependency and test boundaries

Keep statistics, validation, reporting, and process control independent of
Criterium. `:test` includes `dev/bench` but excludes measurement dependencies.
Real collector smoke tests stay in `test-bench` or isolated runtime commands.
Use real processes/files for failure tests; numerical fixtures are not measurements.

Use `Locale/ROOT` for numeric Markdown. `:viewer :none` suppresses Criterium
output; `:limit-time-s` is evaluated at runtime. Preserve blackhole JVM options.
To use the benchmark classpath with `-e` or a REPL, clear alias main options:
`clojure -Sdeps '{:aliases {:e {:main-opts []}}}' -M:bench:e -e ...`.
This selects upstream source unless the corrected launcher supplies the overlay.

Evidence: task 019 `implementation.md`, especially “Timing estimator diagnostic”,
“Default corrected timing launcher”, and “Ordinary milestone completed”; commits
`576d0d5` and `4cd35ba`. Earlier inlining evidence is in task 012.
