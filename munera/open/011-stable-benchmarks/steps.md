# Steps

## Slice 1: pure layer

- [x] Add `ops` (the six Scope 3 operation strings, in order) and `fork-count` (5) to
      `closed-record.bench.report`.
- [x] Add `encode-result` (`pr-str` of the vector of six `[op p r]`, `p`/`r` as `{:mean :low :high}`) and
      `decode-result` (string → `{:result v}` or `{:error msg}`, no throw) to `closed-record.bench.report`.
      Decode catches the EDN reader exception, and validates: a vector of 6 entries, each a 3-element vector,
      operation strings equal to `ops` in order, `p`/`r` with exactly the keys `:mean :low :high`, number values.
- [x] Add `fork-stats` to `closed-record.bench.report`: `:pre` `fork-count` results; for each operation and type
      the mean of the fork `:mean` values and the t interval mean ± 2.776 · s / √5 (s with divisor 4); return
      `[op p r n]` with `{:mean :low :high}` and `n` the number of fork results.
- [x] Add the `Forks` column after `Result`: `report/header` gets `"Forks"`; `report/row` becomes `[op p r n]`
      with a final cell `(str n)`.
- [x] Update the `closed-record.bench.report` ns docstring: add the encode and decode of the child result and the
      statistics over forks.
- [x] Update and extend `test/closed_record/bench_report_test.clj`:
  - encode/decode round trip;
  - decode errors (an `:error` value, no exception): empty string, truncated EDN, 5 entries, wrong operation
    order, a missing key, a non-number value;
  - fork-stats mean and interval against hand-computed values, `n` = 5, and the `:pre` failure with 4 results;
  - Forks column in header/row/table (`row` calls get the `n` argument);
  - pass rule and exit code unchanged (0 / 1), also for `[op p r n]` entries;
  - ns docstring: the tests also inject child results.
- [x] Run `clojure -M:test -m scry.cli` (default alias): zero failures and errors.

## Slice 2: child mode

- [x] Rework `closed-record.bench/run` to the six Scope 3 operations in order (`(:a x)`, `(get x :a)`, `(.a x)`,
      `(assoc x :a 2)`, `(hash x)`, `(= x y)`), with per-operation order by fork parity (even: P then R; odd: R
      then P), each `bench` call site executed at most once per JVM. `run` docstring: "the six operations".
- [x] Change `-main` so that with two args `fork result-file` it runs `run` with a 5 s limit, `spit`s
      `(report/encode-result ...)` to the result file, and exits 0.
- [x] Manually verify one child run (about 60 s): `clojure -M:bench 0 /tmp/fork0.edn; echo "exit $?"` (the CLI
      gives `0 /tmp/fork0.edn` to `-main` after the `:main-opts`) → exit 0. Then
      `clojure -Sdeps '{:aliases {:e {:main-opts []}}}' -M:bench:e -e "(require '[closed-record.bench.report :as r]) (prn (r/decode-result (slurp \"/tmp/fork0.edn\")))"`
      → `{:result [...]}` with the six operations.

## Slice 3: parent mode

- [x] Add `default-child-cmd` (`[fork file]` → command vector): `java.home`/`bin/java`, all
      `RuntimeMXBean/getInputArguments` (no filter), `-cp` `java.class.path`, `clojure.main -m closed-record.bench
      <fork> <file>`.
- [x] Add `parent` (`child-cmd` → exit code, no `System/exit`): for forks 0 to `fork-count` - 1, one after the
      other, make a temp result file, start the child with inherited I/O, wait for its exit, then check exit
      value, file exists, `report/decode-result` (in this order).
- [x] Fork failure handling in `parent`: on the first failure print `fork <n> failed: <cause>` to stderr, start no
      more forks, print no table, return 2. Do not delete the result files.
- [x] On 5 good forks: `report/fork-stats` of the 5 results decoded after each fork, print the table with the
      Forks column, return `report/exit-code`.
- [x] `-main` with no args: `(System/exit (parent default-child-cmd))`.
- [x] Update the `closed-record.bench` ns docstring: parent and child modes of `-main`; exit 0 when all
      operations pass, 1 if not, 2 if a fork fails.
- [x] Verify `parent` with short test commands, no child JVM, in
      `clojure -Sdeps '{:aliases {:e {:main-opts []}}}' -M:bench:e -e ...`:
  - `(fn [_ _] ["sh" "-c" "exit 3"])` → `fork 0 failed: exit 3`, no table, returns 2;
  - `(fn [_ f] ["rm" f])` → missing result file, fork 0, returns 2;
  - `(fn [_ _] ["true"])` → empty file, decode error, fork 0, returns 2;
  - `(fn [_ f] ["sh" "-c" (str "printf '[1' > " f)])` → invalid EDN, fork 0, returns 2;
  - `(fn [_ f] ["cp" <valid fixture> f])` (fixture written with `report/encode-result`) → table with `Forks` 5,
    returns `report/exit-code`.

Note: no full parent run (`clojure -M:bench` with no args) in this slice. The first full run is the acceptance
run (Slice 5).

## Slice 4: structural field declaration test

- [x] Add a test to `test/closed_record/field_value_test.clj`: a closed type and a `defrecord` with one untyped
      field and one `^long` field have non-static declared fields equal in name, order, type and modifiers; ns
      docstring names V-11 for this test.
- [x] Run the test on the default alias, `:clj-1.12` and `:clj-1.13`: passes on all three.

## Slice 5: acceptance run

- [x] Run `clojure -M:bench 2>&1 | tee <file>; echo "exit ${PIPESTATUS[0]}"` (the first full run of the finished
      harness); record its complete output, exit code, JVM and machine in `implementation.md`. Do not replace it
      with a later run.
- [x] Check the recorded output: a `CompileCommand: blackhole ...` line for the parent and for each child (6
      lines), and the table with `Forks` 5.
- [x] If the run exits 2, or a child has no blackhole line: it is a harness fault, not the acceptance run. Record
      its output and cause in `implementation.md`, fix the fault, and run again; the next full run is the
      acceptance run.

## Slice 6: documents

- [x] Update `ramora/defclosedrecord/purpose.md` S-03: add `get`; means and intervals over 5 JVM forks; field
      access has the timed `(.a x)` row and the field declaration test, and passes only if both pass.
- [x] Update `ramora/defclosedrecord/decisions/verification.md` D-16 Scope: suite on Clojure 1.11, 1.12, 1.13
      (aliases); bench on 1.11; CI deferred, all runs manual.
- [x] Update `mementum/state.md`: V-11 status from the acceptance run (done only if all six timed rows pass and
      the declaration test passes); replace the task 010 ratios.
- [x] Update `mementum/knowledge/benchmarks.md`: add the "Forks" section (Scope 1 and 2 rules); in "Known limits"
      replace "A stable bench needs more (for example several JVM forks)." with a reference to "Forks"; in "Code
      structure" replace "Only `-main` has side effects (`System/exit`)." with "only `closed-record.bench` has
      side effects (process start, file I/O, `System/exit`)" and add encode/decode and fork statistics to the
      pure layer; replace "`:main-opts` in `:bench` runs `-main` (a 100 s run)" with a run of 5 forks, about
      300 s plus JVM start. In "Known limits" replace the capture command `clojure -M:bench | tee <file>` with the
      Slice 5 command (`2>&1`, `${PIPESTATUS[0]}`).
- [x] Update `ramora/defclosedrecord/embodiment.md` `dev/bench/` row with the Scope 7 text (six operations, 5
      forks, child entry point, pure-layer list).
- [x] (no decision: rows left unchanged) If the user has decided the SCOPE_QUESTION by then: update the `qualities.md` V-11 row and the
      `embodiment.md` `bench_report_test.clj` row accordingly; otherwise leave them.

## Slice 7: final verification

- [x] `clojure -M:test -m scry.cli` on the default alias, `:clj-1.12` and `:clj-1.13`: zero failures and errors.
- [x] `clj-kondo --lint src test dev`: no errors, no warnings.
- [x] `cljfmt check src test dev`: passes.
