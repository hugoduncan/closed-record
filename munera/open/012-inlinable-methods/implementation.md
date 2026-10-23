# Implementation

## Design review (2026-10-20)

- Architecture context: the helper boundary finding comes from `embodiment.md#boundaries` (permitted and forbidden
  dependencies of generated code) and D-17 (the only current generated-code → `core` dependency is the protocol).
  `ramora/META.md` and `doc/architecture.md` do not exist; the architecture sources are `ramora/INDEX.md` →
  `ramora/defclosedrecord.md` and its subfiles.
- Shape note for later steps: `defrecord` 1.11 `containsKey` and `entryAt` call `valAt` with `this` as the
  not-found sentinel. The closed `valAt` ignores `__extmap`, so this shape keeps I-02 (declared keys only).
- architectural review added 2 new design steps
- ambiguity review added 4 new design steps
- `closed-throw` also builds the throws of `empty` and `map->Name`. Scope 2 moves only the `assoc` and `without`
  throws, so `closed-throw` keeps those two users.
- inconsistency review added 2 new design steps

## For the design-steps task

- Principles: scope is frozen; resolve each step by an edit to `design.md` in place (munera: no amendment
  markers). Prose in STE. Keep D-02/D-12 (no cost on permitted paths) and D-03 (`ex-data` is public API).
- The helper location (arch step 1) drives the helper contract (ambiguity step 2). Decide them together. The
  message parity question: `cname-str` is `(str classname)` with a `namespace-munge`d namespace; `(.getName class)`
  is expected to give the same string, so a helper that gets the class can build the same message. Verify this.
- Relevant files:
  - `ramora/defclosedrecord/embodiment.md` (Boundaries, Method behaviour), `decisions.md` (index, record shape),
    `decisions/type-structure.md` (D-01, D-10), `decisions/platform-and-evolution.md` (D-13, D-17),
    `decisions/failure-and-validation.md` (D-03, D-12), `purpose.md` S-03 (lines ~101–104).
  - `mementum/state.md` (V-11 status and stale cause), `mementum/knowledge/benchmarks.md` (acceptance-run rule 🎯,
    the task 011 `assoc` result).
  - `munera/closed/011-stable-benchmarks/design.md` (acceptance-run and V-11 rules, items 6 and Scope docs).
  - `src/closed_record/impl/jvm.clj` (`closed-throw`, `key-case`, `generated-methods`).

## Design steps done (2026-10-23)

- All 8 design steps resolved in `design.md`. No `SCOPE_QUESTION:` items.
- Choices: helper `#'closed-record.core/key-error` (private, returns the `ex-info`); `condp identical?` for
  `assoc`, `identical?` tests for `without`; `containsKey`/`entryAt` via `valAt` with `this` as sentinel; D-21.
- Discovery: the `case` size is the `tableswitch` over the keyword hash range (`:a :b :c` → 192 entries). Thus the
  "keep `case`" reading of the old decision could not meet acceptance 1.
- Prototype (scratch `deftype`, fields `a b c` + 2): `condp identical?` + `(throw (#'ns/helper ...))` → 214 bytes;
  `without` shape → 86; `containsKey` shape → 46. Acceptance 1 is feasible.
- Verified: `(.getName (class x))` = emitter `cname-str` (ns `foo-bar.baz-q`, types `P_x?`, `Q-r!`).
  `(#'closed-record.core/class-name ...)` works on 1.11 and 1.13.
- Pitfall: `(.assoc ^P r ...)` reflects (bridge `Associative assoc` has the same arity); hint
  `^clojure.lang.IPersistentMap` and box the value (`(num i)`), or `.assoc` reflects on a primitive `long`.
- Pitfall: global `-XX:+PrintInlining` output interleaves between compiler threads; use the per-method
  `CompileCommand`. The `no static binding` lines are C1; the evidence is the line that names `P::assoc`.

### Measurement commands (reproduced the Observed facts table and `PrintInlining` lines)

Scratch dir (outside the repo), `src/user012/n.clj`:

```clojure
(ns user012.n
  (:require [closed-record.core :refer [defclosedrecord]]))
(defclosedrecord P [a b c])
(defrecord R [a b c])
(defn loop-p [n] (loop [i 0 r (->P 1 2 3)] (if (< i n) (recur (inc i) (.assoc ^clojure.lang.IPersistentMap r :a (num i))) r)))
(defn loop-r [n] (loop [i 0 r (->R 1 2 3)] (if (< i n) (recur (inc i) (.assoc ^clojure.lang.IPersistentMap r :a (num i))) r)))
```

```sh
REPO=<repo>/src; mkdir -p classes
clojure -Sdeps "{:paths [\"$REPO\" \"src\" \"classes\"]}" -M -e "(binding [*compile-path* \"classes\"] (compile 'user012.n))"
for c in P R; do javap -c -cp classes user012.n.$c | awk '/^  public .*\(/{m=$0} /^ *[0-9]+: /{split($1,a,":"); last=a[1]} /^$/{if(m!=""){print last+1, m; m=""}}' | grep -E " (assoc|without|containsKey|entryAt|valAt)\("; done
for t in p r; do clojure -Sdeps "{:paths [\"$REPO\" \"src\"]}" -J-XX:+UnlockDiagnosticVMOptions -J-XX:CompileCommand=quiet "-J-XX:CompileCommand=PrintInlining,user012.n\$loop_$t::invokeStatic" -M -e "(require 'user012.n) (dotimes [_ 20] (user012.n/loop-$t 1000000))" 2>&1 | grep -E "\.[PR]::assoc"; done
```

- `last+1` prints the code length (last instruction is 1 byte: `areturn`/`ireturn`). Ignore the 9-byte
  `Associative assoc` bridge line. `-Sdeps` warns about an external path; harmless.

## Design review 2 (architecture)

- Context: `ramora/META.md` and `doc/architecture.md` still do not exist; sources are `ramora/INDEX.md` →
  `defclosedrecord.md` → `context.md`, `principles.md`, `qualities.md`, `embodiment.md`, `decisions/*.md`.
- Pattern used for the findings: D-01 traces assumption → R-01 (signal V-13) and facts → K-06/E-xx. D-21 has no such
  links; D-17 is the precedent for the generated-code → `core` dependency.
- architectural review added 2 new design steps
- ambiguity review 2: clj-kondo check (scratch `/tmp`, `defn-` used only via `#'ns/var` from another ns) gives
  "Unused private var"; a syntax-quoted `#'closed-record.core/key-error` in the emitter gives no warning.
- ambiguity review added 1 new design step
- inconsistency review added 2 new design steps

## For the design-steps task (review 2)

- 5 open steps: 2 architecture, 1 ambiguity, 2 inconsistency. All are edits to `design.md` (mostly Scope 6 and D-21).
  Each changes text that the implementer writes. None changes code scope.
- Next free IDs: K-09, E-08, R-05. The E-xx table has Clojure source locations. A C2 fact has no source line, so
  mark it `observed` (with JDK version), as K-06 to K-08 do.
- D-17 has the same AOT break at "medium". If D-21 becomes "high", say why D-17 stays (do not edit D-17 here).
  The commitment step and the risk/trade-off step both touch D-21 Qualities. Decide them together.
- clj-kondo: `.clj-kondo/config.edn` has only `:lint-as`. A local `#_:clj-kondo/ignore` keeps the exception
  at the helper (minimal). A config entry applies to all files. Verify the fix with `clj-kondo --lint src test dev`.
- Files: `ramora/defclosedrecord/context.md` (Constraints, Evidence), `qualities.md` (Trade-offs, Risks),
  `principles.md` (table), `embodiment.md` (Artifacts, Forbidden dependencies), `decisions.md` (commitment levels),
  `.clj-kondo/config.edn`.

## Design steps 2 done (2026-10-20)

- All 5 review-2 steps resolved in `design.md`. No `SCOPE_QUESTION:` items.
- Choices: K-09 (C2 limit), K-10 (keyword `case` size) in `context.md` Constraints, not in the Evidence table
  (its rows are V-13 test facts; the design has no size test). R-05 (signal V-11) and a D-21 trade-off in
  `qualities.md`. D-21 stays "medium, as D-17", with the reason. Lint: `#_{:clj-kondo/ignore [:unused-private-var]}`
  on the `defn-`.
- Verified: the size table re-measured at `9868723` (unchanged from `66fd109`, code length). `FreqInlineSize` 325
  and `MaxInlineSize` 35 are the OpenJDK 25.0.2 defaults (`java -XX:+PrintFlagsFinal -version`). The clj-kondo
  ignore form removes only the `key-error` warning (scratch `/tmp`, v2026.04.15).
- Discovery: `:a :b :c` hashes have a range of 191 (< 8192), so `case` uses no shift and no mask; the table is the raw
  hash range. Source: `prep-hashes`, `fits-table?` (`core.clj` 6790, 6848 in 1.13.0-alpha6), `CaseExpr.doEmit`
  (`Compiler.java` 9453–9466).
- HotSpot rejects a hot callee when code length > `FreqInlineSize` ("hot method too big"), so 325 is still inlined.
  Acceptance 1 ("below 325") is stricter; this is intended.

## Design review 3 (architecture)

- Sources: `ramora/META.md` and `doc/architecture.md` still do not exist. Checked `embodiment.md`, `principles.md`,
  `decisions/*.md`, `invariants.md` for text that names the replaced mechanism (`case`, inline throw). Only D-02
  Qualities names it (`grep -n case ramora/`). Invariants `enforced-by` and principle 7 stay valid.
- architectural review added 1 new design step
- ambiguity review 3: `benchmarks.md` lines ~22–27 have the stale `assoc` "current gap" text (same kind as the
  `state.md` cause step of review 1).
- ambiguity review added 1 new design step
- inconsistency review 3: Context claims checked against `jvm.clj` (`key-case`/`case`/`closed-throw` users), `core.cljc`
  (`class-name`), `context.md` (K-05), D-03 data, bench `-main` (2 args → child), `state.md` quote: all agree.
  Scratch measure (`deftype`, 5 fields, `condp identical?` `assoc`): inline `ex-info` throw 276 bytes, helper call
  217 bytes. The inline throw adds about 59 bytes; the `case` is most of the size (Problem statement holds).
- no inconsistency review feedback

## For the design-steps task (review 3)

- 2 open steps: D-02 Qualities (architecture), `benchmarks.md` gap text (ambiguity). Both are Scope 6 text edits in
  `design.md`, and each adds a file to the acceptance 6 list if Scope 6 changes it. No code scope change.
- D-02 step: keep the "no cost on permitted paths" claim. D-12 (`decisions/failure-and-validation.md` line ~34)
  cites it and does not name `case`, so D-12 stays. The `identical?` chain is the `defrecord` cost (parity, K-05).
  `without` throws on the declared-key match, not on a default branch; the new D-02 text must cover both methods.
- Benchmarks step: the stale lines are in "criterium 0.5.251-ALPHA" of `mementum/knowledge/benchmarks.md` (~22–27).
  The ❌ 0.5.243 lines are history; only "current gap" / "start point" become stale. Mementum knowledge is
  updated in place (git keeps history). Align with the `state.md` cause replacement already in Scope 6.

## Design steps 3 done (2026-10-20)

- Both review-3 steps resolved in `design.md` Scope 6. No `SCOPE_QUESTION:` items.
- D-02: Qualities text replaced (dispatch is the check; helper only on the rejected path). D-12 and the D-02
  choice stay. `closed-key-set.md` added to acceptance 6 as "D-02".
- `benchmarks.md`: only "current gap / start point" is replaced (by the task 012 `assoc` result); the task 011
  ratio and the 0.5.243 ❌ lines stay as history. The `cons` path calls `clojure.core/assoc`, so it gets the new
  `assoc` with no emitter change.

## Design review 4 (architecture)

- Checked D-21 against D-01/D-13/D-17/D-20 commitments, principles 3/6/7/9/28, `qualities.md` targets and
  trade-offs, and embodiment boundaries, method table and allocation. Scope 6 covers each text that names the
  changed mechanism or dependency set.
- no architectural review feedback
- ambiguity review 4: Scope 1 bodies checked against `jvm.clj` `generated-methods` (the `assoc` `new` form, the
  no-field cases, the shared `containsKey` of `IPersistentMap` and `java.util.Map`). Checked the Scope 2 helper against
  `core.cljc` `class-name` (a top-level `defn-`, line ~63; put `key-error` after it). All are specified.
- no ambiguity review feedback
- inconsistency review 4: the text that Scope 6 replaces matches the current files (D-02 Qualities, `state.md` cause,
  `benchmarks.md` gap lines, `embodiment.md` symbol sentence, principle 28, `defclosedrecord.md` ranges). Scope 6 and
  acceptance 6 list the same files.
- no inconsistency review feedback

## For the next step (review 4)

- Review 4 added no design steps. All steps in `design-steps.md` are ticked. The design is ready for `plan.md`.
- Non-actionable note: the Decisions bullet "Out-of-line throw" says that the inline throw adds bytecode "on every
  call site of the method". The bytecode is in the method body (it reaches call sites only by inlining). The meaning
  stays correct. Edit it only if `design.md` changes for another reason.
