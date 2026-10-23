# Plan

## Approach

Change the JVM emitter so that `assoc`, `without`, `containsKey` and `entryAt` have the
`defrecord` shape and size. Move the throw paths of `assoc` and `without` out of line into
one private helper in `core`.

Key decisions (from `design.md`; the gate, the capture, the no-table rule and the
Slice 6 failure rule are plan choices within Scope 5):

- Dispatch: `condp identical?` for `assoc`, `identical?` tests for `without`. No `case`
  in the four methods. `valAt` keeps its `case` (out of scope).
- `containsKey` and `entryAt` call `(.valAt this k this)` with `this` as the not-found
  sentinel, as `defrecord` does. The closed `valAt` reads only declared fields, so I-02
  holds.
- Helper: `(key-error t record k)`, a private `defn-` in `closed-record.core` (`.cljc`),
  after `class-name` (~line 63). It returns the `ex-info`; the call site has the `throw`.
  Message and data are unchanged (D-03). The emitter emits `#'closed-record.core/key-error`
  only as a symbol (as D-17), so `impl.*` does not require `core`.
- Lint: `#_{:clj-kondo/ignore [:unused-private-var]}` before the `defn-`. No change to
  `.clj-kondo/config.edn`.
- `closed-throw` stays for `empty` and `map->Name`.
- `jvm.clj` ns docstring: "It emits the ClosedRecord protocol interface only as a
  symbol" becomes "It emits the ClosedRecord protocol interface name and the key-error
  var name only as symbols", as the new `embodiment.md` sentence (Scope 6). It is a
  `src` change, so it is in Slice 2, before the acceptance run.
- Size table (Scope 4, acceptance 1): the 8 rows of the design Observed facts table
  (`without`, `assoc`, `getLookupThunk`, `entryAt`, `containsKey`, `valAt(k, nf)`,
  `cons`, `equiv`), for `P` and `R`. The four changed methods are the acceptance 1
  rows; the other four are unchanged references. The `javap` `grep` names all 8
  methods. Ignore the `valAt(Object)` row and the 9-byte `Associative assoc` bridge.
- Clojure version of the evidence (Scope 4: Clojure 1.11.4): the Slice 3 commands run
  in a scratch directory outside the repository, so the repository `deps.edn` does not
  apply. Without a pin, the CLI uses the Clojure version of the root deps (1.12.6 with
  CLI 1.12.6.1673). Each scratch `-Sdeps` map has
  `:deps {org.clojure/clojure {:mvn/version "1.11.4"}}`, and each `-e` prints
  `(clojure-version)`. The output must be `1.11.4`; else the evidence is not valid.
  `implementation.md` records this output with the evidence. The commands are in
  "Measurement (Slice 3)" below. The bench and test runs are in the repository, so
  `deps.edn` gives them 1.11.4 (default alias) with no pin.
- Gate before the acceptance run. Slice 2 ends with a commit of the `src`, `test` and
  `dev` changes (the gate commit), after the tests pass on the default alias,
  `:clj-1.12` and `:clj-1.13`, and clj-kondo and cljfmt pass. Slice 3 measures the
  classes of the gate commit. Slice 4 confirms that `src`, `test` and `dev` did not
  change after the gate commit (else repeat Slice 2 checks and Slice 3 on the new
  commit), and `implementation.md` records the gate commit sha. After the gate,
  `src`, `test` and `dev` do not change in this task.
- Evidence first, acceptance run last: the first full bench run (5 forks) after the
  gate that prints the table is the acceptance run (see the table check below).
  Shorter runs (one fork, REPL criterium) are permitted before it and do not count
  for V-11.
- Capture of each full run (`mementum/knowledge/benchmarks.md`, 🎯 acceptance run), in
  bash: `clojure -M:bench 2>&1 | tee /tmp/012-bench-<n>.txt; echo "exit ${PIPESTATUS[0]}"`.
  `implementation.md` records the complete file content and the exit code.
- Run classification: the table in the captured output classifies a full run, not
  the exit code. The exit code does not prove a table: `clojure.main` exits 1 on an
  uncaught exception (start-up, dependencies, `report/fork-stats`, `report/table`)
  with no table, and a kill or an interrupt gives another code (for example 137 or
  130). Bench exit codes (task 011): 0 all rows pass, 1 a row fails, 2 a fork fails.
  - Table check: `grep -E '^\| (Operation|\()' /tmp/012-bench-<n>.txt` prints 7
    lines: the header line (`| Operation ...`) and one row for each of the six
    operations (`report/header`, `report/ops`, `report/table` in
    `dev/bench/closed_record/bench/report.clj`).
  - Table present: the run is the acceptance run, whatever its exit code. The
    Result column of the table gives the row results (exit 0 or 1 agrees with it).
    No later run replaces it.
  - No table (fewer than 7 lines), with any exit code (for example 2, 1, 137, 130):
    the run has no timed result and gives V-11 no result. It is not the acceptance
    run. Record its output and exit code in `implementation.md`, do not change `src`,
    `test` or `dev`, and do one more full run. The 🎯 rule ("do not choose a later
    run that passes") stays true: a run with no table gives no result to choose.
  - Limit: count the runs with no table together, whatever their exit codes. After
    the second run with no table, stop and ask the user (a harness or environment
    fault is out of scope).
- Slice 6 is a re-check of the gate commit. First,
  `git diff --stat <gate-sha> HEAD -- src test dev` and `git status --short src test dev`
  must be empty. If a Slice 6 check
  fails, do not change `src`: a `src` fix needs a new acceptance run, which Scope 5
  does not permit. Record the failure in `implementation.md`, keep the task open and
  ask the user.
- Documents: new D-21 in `decisions/type-structure.md` plus the Scope 6 edits
  (K-09, K-10, R-05, D-02, embodiment, principles, state, benchmarks).

## Measurement (Slice 3)

These commands replace the "Measurement commands" of `implementation.md` (design
steps, 2026-10-23) for the Slice 3 evidence. They add the 1.11.4 pin, the
`(clojure-version)` print and the 8-method `grep`. Verified at `479f463` (current
emitter): both print `1.11.4`, and the table is equal to the design Observed facts
table.

Scratch directory (outside the repository), `src/user012/n.clj`:

```clojure
(ns user012.n
  (:require [closed-record.core :refer [defclosedrecord]]))
(defclosedrecord P [a b c])
(defrecord R [a b c])
(defn loop-p [n] (loop [i 0 r (->P 1 2 3)] (if (< i n) (recur (inc i) (.assoc ^clojure.lang.IPersistentMap r :a (num i))) r)))
(defn loop-r [n] (loop [i 0 r (->R 1 2 3)] (if (< i n) (recur (inc i) (.assoc ^clojure.lang.IPersistentMap r :a (num i))) r)))
```

In bash, in the scratch directory. `REPO` is the repository `src` directory, with the
working tree at the gate commit (clean `src`):

```sh
REPO=<repo>/src; mkdir -p classes
CLJ='org.clojure/clojure {:mvn/version "1.11.4"}'
clojure -Sdeps "{:paths [\"$REPO\" \"src\" \"classes\"] :deps {$CLJ}}" -M -e "(println (clojure-version)) (binding [*compile-path* \"classes\"] (compile 'user012.n))"
for c in P R; do javap -c -cp classes user012.n.$c | awk '/^  public .*\(/{m=$0} /^ *[0-9]+: /{split($1,a,":"); last=a[1]} /^$/{if(m!=""){print last+1, m; m=""}}' | grep -E " (assoc|without|getLookupThunk|entryAt|containsKey|valAt|cons|equiv)\("; done
for t in p r; do clojure -Sdeps "{:paths [\"$REPO\" \"src\"] :deps {$CLJ}}" -J-XX:+UnlockDiagnosticVMOptions -J-XX:CompileCommand=quiet "-J-XX:CompileCommand=PrintInlining,user012.n\$loop_$t::invokeStatic" -M -e "(println (clojure-version)) (require 'user012.n) (dotimes [_ 20] (user012.n/loop-$t 1000000))" 2>&1 | grep -E "^1\.|\.[PR]::assoc"; done
java -version
```

- `last+1` is the code length (the last instruction is `areturn` or `ireturn`, 1 byte).
- `-Sdeps` warns about an external path; ignore the warning.

## Slice order

1. Helper: `key-error` in `core.cljc`, with tests.
2. Emitter: the four method bodies and the ns docstring in `impl/jvm.clj`. Tests on
   three Clojure versions, clj-kondo, cljfmt; commit (the gate commit).
3. Evidence: size table and `PrintInlining` on Clojure 1.11.4 in `implementation.md`
   (commands in "Measurement (Slice 3)").
4. Gate (no `src`, `test` or `dev` change after the gate commit of Slice 2), then the
   bench acceptance run (Scope 5); record output, exit code and V-11 status.
5. Documents (Scope 6). No `src`, `test` or `dev` change.
6. Final re-check of the gate commit: tests on three Clojure versions, clj-kondo,
   cljfmt.

## Risks

- A method is still at or above 325 bytes (code length). The design prototype gives 214
  bytes for the `assoc` shape with three fields, so the margin is wide. If not, the
  acceptance 1 target rules; re-shape before the acceptance run.
- The acceptance run can fail on `assoc` or on a row that passed in task 011. The task
  still completes: `state.md` records the ratio, V-11 stays not done, a follow-up is a
  new task (Scope 5).
- Measurement pitfalls (recorded in `implementation.md`): hint
  `^clojure.lang.IPersistentMap` and box the value in the hot loop, or `.assoc` reflects;
  use the per-method `CompileCommand`, not global `PrintInlining`; read the
  `IPersistentMap assoc` row, not the 9-byte bridge.
- The emitter must not change after the acceptance run in this task. The gate puts each
  check and edit that can touch `src` before the run (Slice 2): the three-version tests,
  clj-kondo, cljfmt and the `jvm.clj` docstring. Slice 5 edits only documents. A Slice 6
  failure stops the task for a user decision (see Key decisions).
- A full run prints no table (a fork fails with exit 2, an uncaught exception with
  exit 1, or a kill or an interrupt). The table check, not the exit code, finds it.
  One more full run; after the second run with no table, stop and ask the user (see
  Key decisions, Run classification).
