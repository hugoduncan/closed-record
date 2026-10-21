# Plan

## Approach

Build the bench harness first, then run it, then write the docs from the run result.

1. **Harness slice.** Add the `:bench` alias to `deps.edn` (extra path `dev/bench`, criterium
   0.5.243-ALPHA, blackhole `:jvm-opts`, `:main-opts ["-m" "closed-record.bench"]`). Write
   `dev/bench/closed_record/bench.clj` (ns `closed-record.bench`) with `(defclosedrecord P [a b c])` and
   `(defrecord R [a b c])`, a `-main` that runs `criterium.bench/bench` with `:viewer :none` and
   `:limit-time-s 10` for each of the five operations on `P` then `R` in one JVM, reads
   `criterium.bench/last-bench` for mean `:point-estimate` and `:estimate-quantiles`, divides by that run's
   `:batch-size`, applies the Scope 4 pass rule on unrounded values, prints the Scope 5 table, and exits
   0/1 via `System/exit`.
   - Expressions use local bindings. Field access is `(.a x)` with a type hint. `hash` on one instance
     (cached path). `=` on two distinct equal instances of the same type.
   - Bench helper: a macro, not a function. `(bench-op expr limit-s)` expands to
     `(criterium.bench/bench ~expr :viewer :none :limit-time-s ~limit-s)`, then reads
     `(criterium.bench/last-bench)` into the per-call stats. Each of the ten uses has its own `bench` call
     site with the literal operation expression, and `x` (and `y`) are locals in a `let` around it. A
     function that takes a thunk is not used: `bench` then measures `(f)` at one shared call site, and the
     `IFn` invoke cost (about 1 ns or more) moves `P / R` toward 1 (see Key decisions).
   - Structure: a small pure layer (extract mean/interval from bench data, pass rule, row formatting) so
     the logic can be REPL-checked without a 100 s run; `-main` wires the ten `bench-op` uses.
2. **Run slice.** Run `clojure -M:bench` once on Clojure 1.11.4, record the table, JVM and machine in
   `implementation.md` (Acceptance 3). Verify exit code and no blackhole warning.
3. **Docs slice.** Scope 6 and 7: `purpose.md` S-03 one-sided rule; `README.md` Development bench
   command; `embodiment.md` `dev/bench/` and `deps.edn` rows (exact text in Scope 7); `mementum/state.md`
   alias list, "Bench:" line after the "Tests:" command line, and V-11 status chosen from the recorded run
   (done, or not-done with failed operations and `P / R` ratios).
4. **Verify slice.** `clj-kondo --lint src test dev` clean, `cljfmt check src test dev` passes, existing
   tests still pass.

## Key decisions

- Pass rule and table logic are pure functions over the `last-bench` data; only `-main` has side effects.
- The measured body is the literal operation only: no `IFn` invoke, var deref or shared call site. This is
  why the bench helper is a macro (Slice 1). The keyword lookup is about 0.64 ns, so an extra 1 ns would
  hide a difference between `P` and `R`. `:limit-time-s` is a macro argument, evaluated at run time
  (`bench` puts its options in a runtime map), so the REPL smoke test can use 1 s.
- No emitter changes even if an operation fails (the probe suggests `assoc` may fail at ~1.14). A failure
  does not block task completion: Scope 6/7 still complete, V-11 stays not-done with the ratios.
- `System/exit` only in `-main`, after printing, so the table always appears.
- Docs slice runs after the run slice because the `state.md` V-11 text depends on the run result.

## Risks

- **Run-to-run variance.** A marginal operation (e.g. `assoc` near 1.05) may pass or fail depending on
  machine load. Mitigation: the overlap rule, `:limit-time-s 10`, a quiet machine; record the one
  acceptance run and use it for the docs. Do not cherry-pick re-runs.
- **criterium 0.5.243-ALPHA data shape.** The `last-bench` paths are from the design's probe; if the shape
  differs, fix the extraction paths, not the design. Verify at the REPL with a short `:limit-time-s`
  before the full run.
- **Blackhole warning.** Acceptance 2 requires no warning; the `:jvm-opts` flags must be in the alias, and
  the run must use `-M:bench` (not a REPL without the flags).
- **Open D-16 SCOPE_QUESTION** (design-steps.md): deferred to the user; Scope 7 does not include D-16, so
  implementation proceeds without it.

## Slice order

1. Harness: `deps.edn` `:bench` alias + `dev/bench/closed_record/bench.clj`.
2. Run: acceptance run on 1.11.4, record in `implementation.md`.
3. Docs: `purpose.md`, `README.md`, `embodiment.md`, `mementum/state.md`.
4. Verify: lint, format, tests, acceptance check.
