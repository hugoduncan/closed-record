# Steps

## Slice 1: pure layer

- [ ] Add `encode-result` and `decode-result` to `closed-record.bench.report`: EDN of a vector of six `[op p r]`
      entries in the Scope 3 order, `p`/`r` as `{:mean :low :high}`; decode validates the shape and the six
      operation strings and returns the data or an error value.
- [ ] Add `fork-stats` to `closed-record.bench.report`: given 5 decoded fork results, return per operation and
      type the mean of the 5 fork `:mean` values and the t interval mean ± 2.776 · s / √5 (s with divisor 4), as
      `[op p r]` with `{:mean :low :high}`.
- [ ] Add the `Forks` column (value 5) after `Result` in `report/header` and `report/row`.
- [ ] Update and extend `test/closed_record/bench_report_test.clj`: encode/decode round trip, decode rejection of
      bad shapes and wrong operations, fork-stats mean and interval against hand-computed values, Forks column in
      header/row/table, pass rule and exit code unchanged (0 / 1).
- [ ] Run `clojure -M:test -m scry.cli` (default alias): zero failures and errors.

## Slice 2: child mode

- [ ] Rework `closed-record.bench/run` to the six Scope 3 operations in order (`(:a x)`, `(get x :a)`, `(.a x)`,
      `(assoc x :a 2)`, `(hash x)`, `(= x y)`), with per-operation order by fork parity (even: P then R; odd: R
      then P), each `bench` call site executed at most once per JVM.
- [ ] Change `-main` so that with two args `fork result-file` it runs `run` with a 5 s limit, writes
      `(report/encode-result ...)` to the result file, and exits 0.
- [ ] Manually verify one child run: `clojure -M:bench:<no-main> ...` or direct `java` invocation writes a valid
      EDN result file and exits 0.

## Slice 3: parent mode

- [ ] Implement the parent path of `-main` (no args): compute the java binary (`java.home`), classpath
      (`java.class.path`) and JVM options (`RuntimeMXBean/getInputArguments`, keep the blackhole opts); start
      forks 0..4 one after the other, each with its own temp result file, child output inherited.
- [ ] Implement fork failure handling: on non-zero child exit, missing result file, or invalid decode, print an
      error naming the fork and the cause, start no more forks, print no table, exit 2.
- [ ] On 5 good forks: decode the 5 files, compute `report/fork-stats`, print the table with the Forks column,
      exit with `report/exit-code` over the timed rows.
- [ ] Verify the blackhole `CompileCommand` notice appears for each child in the parent output.
- [ ] Verify the failure path: force a child failure (for example a bad result file path) and confirm error
      message, no table, exit 2.

## Slice 4: structural field declaration test

- [ ] Add a test to `test/closed_record/field_value_test.clj`: a closed type and a `defrecord` with one untyped
      field and one `^long` field have non-static declared fields equal in name, order, type and modifiers; ns
      docstring names V-11 for this test.
- [ ] Run the test on the default alias, `:clj-1.12` and `:clj-1.13`: passes on all three.

## Slice 5: acceptance run

- [ ] Run `clojure -M:bench` (the first full run of the finished harness); record its complete output, exit code,
      JVM and machine in `implementation.md`. Do not replace it with a later run.

## Slice 6: documents

- [ ] Update `ramora/defclosedrecord/purpose.md` S-03: add `get`; means and intervals over 5 JVM forks; field
      access has the timed `(.a x)` row and the field declaration test, and passes only if both pass.
- [ ] Update `ramora/defclosedrecord/decisions/verification.md` D-16 Scope: suite on Clojure 1.11, 1.12, 1.13
      (aliases); bench on 1.11; CI deferred, all runs manual.
- [ ] Update `mementum/state.md`: V-11 status from the acceptance run (done only if all six timed rows pass and
      the declaration test passes); replace the task 010 ratios.
- [ ] Update `mementum/knowledge/benchmarks.md`: add the "Forks" section (Scope 1 and 2 rules); in "Known limits"
      replace "A stable bench needs more (for example several JVM forks)." with a reference to "Forks"; in "Code
      structure" replace "Only `-main` has side effects (`System/exit`)." with "only `closed-record.bench` has
      side effects (process start, file I/O, `System/exit`)" and add encode/decode and fork statistics to the
      pure layer.
- [ ] Update `ramora/defclosedrecord/embodiment.md` `dev/bench/` row with the Scope 7 text (six operations, 5
      forks, child entry point, pure-layer list).
- [ ] If the user has decided the SCOPE_QUESTION by then: update the `qualities.md` V-11 row and the
      `embodiment.md` `bench_report_test.clj` row accordingly; otherwise leave them.

## Slice 7: final verification

- [ ] `clojure -M:test -m scry.cli` on the default alias, `:clj-1.12` and `:clj-1.13`: zero failures and errors.
- [ ] `clj-kondo --lint src test dev`: no errors, no warnings.
- [ ] `cljfmt check src test dev`: passes.
