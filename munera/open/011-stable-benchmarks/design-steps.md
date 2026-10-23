# Design steps

- [x] Architecture review: move the Scope 4 field declaration test out of `compiler_rules_test.clj`.
  - `compiler_rules_test.clj` is V-13 (embodiment.md). Its ns docstring says that its tests use only `defrecord`,
    `deftype`, `deftype*`, the reader and the compiler. It does not require `closed-record.core`. It fixes the
    Clojure rules K-06 to K-08, which the library depends on.
  - The Scope 4 test compares a `defclosedrecord` type with a `defrecord`. It checks the emitter output (D-01),
    not a compiler rule. In that file, it breaks the V-13 boundary.
  - Put the test in a test namespace of the closed type, for example `field_value_test.clj` (differential closed
    type / `defrecord` cases, V-09 type hints). Name the verification item that it supports (S-03 field access,
    V-11).
- [x] Architecture review: define the parent / child boundary of the fork harness.
  - Scope 1 says that each child "gives its per-call stats to the parent". The design does not say how: the child
    entry point, or the transport (stdout, file).
  - The bench rule (benchmarks.md, Code structure) keeps the pure layer in `closed-record.bench.report`, with no
    criterium and no side effects. Acceptance 2 tests "statistics over forks" with no child JVM.
  - Say: the child entry point (in `closed-record.bench`), the transport and its data format, and that the encode
    and decode of the child result are in the pure layer, so the suite can test them. Process start and I/O stay
    in `closed-record.bench`.
  - A stdout transport must ignore other output: the JVM prints the `CompileCommand: blackhole ...` notice to
    stdout at start.
- [ ] Architecture review: SCOPE_QUESTION: Scope 7 does not update the verification plan and test artifact rows
  that this task changes.
  - `qualities.md` V-11 says "criterium: lookup, `.field`, `assoc`, `hash`, `=`". After this task, S-03 adds
    `get`, 5 JVM forks, and a structural field declaration test that is not criterium. D-16 points to V-11 as the
    verification plan.
  - `embodiment.md` `bench_report_test.clj` row lists "per-call stats, pass rule, table and exit code". After this
    task, the pure layer also has the statistics over forks (and the child result format).
  - Out of scope excludes only the `qualities.md` rows that name a CI matrix. A human decides: add the V-11 row and
    the `bench_report_test.clj` row to Scope 7, or accept the gap.
- [x] Ambiguity review: say how forks are numbered and what "runs first" covers (Scope 1).
  - "In even forks, `P` runs first; in odd forks, `R` runs first." With forks 0–4, 3 forks start with `P`. With
    forks 1–5, 3 forks start with `R`. The design does not say which.
  - "Runs first" can mean: for each operation, `P` then `R`; or all six `P` runs, then all six `R` runs.
  - Give the fork numbering, and the run sequence in one fork (for example: for each operation in the Scope 3
    order, `P` then `R` in even forks).
- [x] Ambiguity review: say what happens when a child fork fails, and what the forks column shows (Scope 1, 5).
  - A child can exit non-zero or throw (for example the criterium divide by zero fault), or give no result.
  - The design does not say if the parent then stops with an error, or computes the result from fewer forks.
    Scope 2 fixes 5 fork means and 4 degrees of freedom.
  - Scope 5 adds "the number of forks" column. Say if it shows the configured count (always 5) or the number of
    forks that gave a result. Give its header text and its position in the table.
- [x] Ambiguity review: say what the field declaration test adds to the S-03 pass and the V-11 status.
  - Scope 7 S-03: "Field access has the timed row and the field declaration test." Scope 7 `state.md`: "V-11 is
    done only if all six operations pass." The exit code uses the timed rows only (Scope 5).
  - Say if field access passes S-03 when the timed `(.a x)` row fails and the declaration test passes, and if V-11
    can be done when the declaration test fails.
- [x] Ambiguity review: give the new text of the Scope 7 `benchmarks.md` and `embodiment.md` changes.
  - "`benchmarks.md`: the fork rule" does not say the rule. It can be the fork count, the alternate order, the
    t interval, one run of each call site in a JVM, or all of these. The Known limits item "A stable bench needs
    more (for example several JVM forks)" can stay or change.
  - "`embodiment.md`: the `dev/bench/` row" does not say the new text. The row now says "the five S-03
    operations" and does not name forks.
  - Acceptance 6 cannot check these changes without the text or its content items.
- [x] Plan ambiguity review: say where the `Forks` value comes from (plan 1, steps Slice 1).
  - Plan: "extend `header` and `row` with a final `Forks` cell (value 5)". Scope 5: the cell "shows the number of
    fork means in the statistics of the row".
  - The plan does not say if `row` prints a constant 5, or gets the count as data (for example a fourth element
    of each `fork-stats` entry, or a `fork-count` constant that the parent loop, `fork-stats` and `row` share).
    The `row` signature `[op p r]` and the table tests depend on this choice.
- [x] Plan ambiguity review: give the `decode-result` contract (plan 1, Key decisions, steps Slice 1).
  - "an error value" does not give its shape. The parent must print the cause from it (Scope 1), and the tests of
    "decode rejection" must check it.
  - Say if decode catches EDN reader exceptions (truncated or empty file) and returns the error value for them.
    The parent makes the temporary file before the child starts, so an empty file is the likely case when the
    child writes no result; "missing result file" can then occur only if the child deletes it.
  - Say what the validation checks: six entries, the operation strings in the Scope 3 order (or only as a set),
    and the keys and number type of `:mean :low :high`.
- [x] Plan ambiguity review: say which JVM options the child gets (plan 3, Risks, steps Slice 3).
  - Scope 1: the child has "the JVM options of the parent". Steps: "`RuntimeMXBean/getInputArguments`, keep the
    blackhole opts" can mean all input arguments or a filter to the blackhole options. Plan: "drop
    `-Dclojure.basis=...` if it is not re-usable" gives no test for "re-usable".
  - Say: pass all input arguments, and the observed condition (if any) that removes `-Dclojure.basis`.
- [x] Plan ambiguity review: say which run is the acceptance run (Scope 6, steps Slice 3 and 5).
  - Slice 3 "Verify the blackhole `CompileCommand` notice appears for each child" needs a full parent run (5
    forks, about 300 s). Scope 6 makes "the first full run of the finished harness" the acceptance run, and
    Slice 5 is after Slice 4.
  - Say if the Slice 3 run is the acceptance run, or if Slice 3 verifies with a shorter run (and how, because the
    run limit and the fork count are fixed), or which slice makes the harness "finished".
  - Say if a run that exits 2 (a harness fault) counts as the acceptance run, or the fault is fixed and the next
    run is the acceptance run.
- [x] Plan ambiguity review: give the manual verification commands (steps Slice 2 and 3).
  - Slice 2: "`clojure -M:bench:<no-main> ...`": no such alias is in `deps.edn`. The `:e` alias of the design notes is
    defined inline (`benchmarks.md`: `clojure -Sdeps '{:aliases {:e {:main-opts []}}}' -M:bench:e ...`). `clojure -M:bench 0 <file>` gives the args to `-main` after the `:main-opts`.
  - Slice 3: "force a child failure (for example a bad result file path)": the parent makes the path, so the step
    does not say how to force it (a temporary edit that is not committed, a REPL call with a different child
    command, or another hook). Say which of the three causes (exit code, missing file, bad content) to check.
- [x] Plan inconsistency review: decode each result file when its child exits (steps Slice 3).
  - Steps Slice 3, item 3: "On 5 good forks: decode the 5 files". Item 2 and Scope 1: an invalid decode is a fork
    failure, and then the parent "starts no more forks". Scope 1: the parent reads the file "after the child
    exits".
  - If the parent decodes only after the 5 forks, a bad result in fork 0 does not stop forks 1 to 4. Make item 3
    use the 5 results that the parent decoded after each fork (plan 3 has the same gap: it does not say when the
    decode occurs).
- [x] Plan inconsistency review: add the texts that the task makes false and that Scope 7 and the plan do not
  change.
  - `dev/bench/closed_record/bench.clj` ns docstring: "Exits 0 when all operations pass, 1 if not." The task adds
    exit 2 and the child entry point (`-main` with the fork number and the result file).
  - `dev/bench/closed_record/bench/report.clj` ns docstring: lists "per-call stats ..., the pass rule, the table
    and the exit code". The task adds the encode and decode of the child result and the statistics over forks.
  - `mementum/knowledge/benchmarks.md` "Code structure": "`:main-opts` in `:bench` runs `-main` (a 100 s run)".
    After the task, a run is 5 forks × 12 runs × 5 s (about 300 s, plus JVM start).
  - Add these to the plan and steps (Slice 1–3 for the docstrings, Slice 6 for `benchmarks.md`), or to Scope 7.
