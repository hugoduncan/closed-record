# Steps

## Slice 1: helper

- [x] Add private `key-error` to `src/closed_record/core.cljc`, after `class-name`:
  `(key-error t record k)` returns an `ex-info` with the Scope 2 message (a `case` on `t`,
  using `class-name`) and data `{:type t, :record record, :key k}`. Prefix the `defn-`
  with `#_{:clj-kondo/ignore [:unused-private-var]}`.
- [x] Add tests for `key-error` (called through `#'closed-record.core/key-error`): message
  text and `ex-data` for both `t` values.

## Slice 2: emitter

- [x] In `src/closed_record/impl/jvm.clj`, change `assoc` to
  `(condp identical? k kw (new Name ... v ... __meta __extmap) ... default)` with one
  clause per field; default
  `(throw (#'closed-record.core/key-error :closed-record/undeclared-key (class this) k))`.
  With no fields, only the default.
- [x] Change `without` to `identical?` tests of `k` against each declared keyword; a match
  gives `(throw (#'closed-record.core/key-error :closed-record/declared-key-dissoc (class this) k))`,
  no match gives `this`; with no fields, `this`.
- [x] Change `containsKey` (both `IPersistentMap` and `java.util.Map`) to
  `(not (identical? this (.valAt this k this)))`.
- [x] Change `entryAt` to
  `(let [v (.valAt this k this)] (when-not (identical? this v) (clojure.lang.MapEntry/create k v)))`.
- [x] Remove the now-unused `case`/`closed-throw` use from these four methods only;
  `closed-throw` stays for `empty` and `map->Name`. Remove `key-case` users accordingly
  (`valAt` and `getLookupThunk` keep it).
- [x] Change the ns docstring of `src/closed_record/impl/jvm.clj` (lines 5–6): "It emits
  the ClosedRecord protocol interface only as a symbol." becomes "It emits the
  ClosedRecord protocol interface name and the key-error var name only as symbols."
- [x] Run `clojure -M:test -m scry.cli`, `clojure -M:test:clj-1.12 -m scry.cli` and
  `clojure -M:test:clj-1.13 -m scry.cli`; each exits 0 with zero failures and errors.
- [x] Run `clj-kondo --lint src test dev` (no errors, no warnings) and
  `cljfmt check src test dev` (passes).
- [x] Commit the `src`, `test` and `dev` changes of Slices 1 and 2. This is the gate
  commit (plan, Key decisions). If a later step needs a `src`, `test` or `dev` change
  before the acceptance run, commit it, then repeat the checks above and Slice 3 on the
  new commit (the new gate commit).

## Slice 3: evidence (Scope 4)

- [x] AOT-compile the scratch namespace (outside the repo) with `(defclosedrecord P [a b c])`
  and `(defrecord R [a b c])` from the gate commit, on Clojure 1.11.4. Use the commands of
  `plan.md` "Measurement (Slice 3)": each `-Sdeps` has
  `:deps {org.clojure/clojure {:mvn/version "1.11.4"}}`, each `-e` prints
  `(clojure-version)`, and the `javap` `grep` names the 8 methods
  (`grep -E " (assoc|without|getLookupThunk|entryAt|containsKey|valAt|cons|equiv)\("`).
  Each `(clojure-version)` output must be `1.11.4`; else fix the command and measure again.
- [x] Record in `implementation.md`: the `javap` size table for `P` and `R` with the 8
  rows of the design Observed facts table (`without`, `assoc`, `getLookupThunk`,
  `entryAt`, `containsKey`, `valAt(k, nf)`, `cons`, `equiv`; the `IPersistentMap assoc`
  row, not the 9-byte bridge; not `valAt(Object)`), the `(clojure-version)` output
  (`1.11.4`), the `java -version` output, the gate commit sha, and the exact commands and
  scratch namespace.
- [x] Verify acceptance 1: `assoc`, `without`, `containsKey`, `entryAt` of `P` each below
  325 bytes code length.
- [x] Run `PrintInlining` (per-method `CompileCommand`, boxed-value hot loop, Clojure
  1.11.4 pinned as above) and record the C2 line `P::assoc (<n> bytes) inline (hot)` and
  the `(clojure-version)` output in `implementation.md` (acceptance 2).

## Slice 4: bench acceptance run (Scope 5)

Gate commit: `96f229e`.

- [x] Gate: `git diff --stat <gate-sha> HEAD -- src test dev` and `git status --short src
  test dev` are empty; the size table and the `PrintInlining` line of the gate commit
  (Clojure 1.11.4) are in `implementation.md`, with the gate commit sha.
- [x] In bash, run `clojure -M:bench 2>&1 | tee /tmp/012-bench-1.txt; echo "exit
  ${PIPESTATUS[0]}"` (5 forks). Record the complete file content and the exit code in
  `implementation.md`.
  - Classify the run by the table, not by the exit code (plan, Key decisions, Run
    classification). Table check: `grep -E '^\| (Operation|\()' /tmp/012-bench-1.txt`
    prints 7 lines (the header line and the six operation rows).
  - Table present (any exit code): this is the acceptance run. No later run replaces it.
    `src`, `test` and `dev` do not change after it in this task.
  - No table (fewer than 7 lines, any exit code, for example 2, 1, 137 or 130): not the
    acceptance run. Record it, do not change `src`, `test` or `dev`, and do one more full
    run (`/tmp/012-bench-2.txt`), with the same rules. If that run also has no table,
    stop and ask the user.
- [x] Determine V-11 from the Result column of the acceptance-run table: done only if all
  six timed rows pass and
  `test/closed_record/field_value_test.clj` passes (S-03). If a timed row fails, note the
  ratio for the `state.md` update in slice 5.

## Slice 5: documents (Scope 6)

No `src`, `test` or `dev` change in this slice.

- [x] Add `D-21 Method size` to `ramora/defclosedrecord/decisions/type-structure.md` with
  the full record shape (subject, choice, rejected, rationale, assumptions, qualities,
  scope, commitment "medium, as D-17") per Scope 6.
- [x] Add `D-21 method size` to the "Type structure" index entry in
  `ramora/defclosedrecord/decisions.md`.
- [x] Update D-02 Qualities in `ramora/defclosedrecord/decisions/closed-key-set.md` with
  the new "no cost on a permitted path" text (Scope 6).
- [x] Update `ramora/defclosedrecord.md`: "D-01 to D-21", "constraints K-01 to K-10", and
  the `observed` paragraph (K-09/K-10 from task 012 measurements; V-11 is their signal).
- [x] Add K-09 (C2 inline limit) and K-10 (keyword `case` size) to
  `ramora/defclosedrecord/context.md` Constraints; do not change the Evidence table.
- [x] Update `ramora/defclosedrecord/qualities.md`: the D-21 trade-off and risk R-05.
- [x] Update `ramora/defclosedrecord/principles.md`: add D-21 to principle 6; principle 28
  "K-06 to K-10" and add D-21.
- [x] Update `ramora/defclosedrecord/embodiment.md`: `core.cljc` artifact row (`key-error`),
  forbidden-dependency sentence ("protocol name and the `key-error` var name only as
  symbols"), permitted dependencies, Boundaries Internal, and the method-behaviour rows for
  `assoc`, `without`, `containsKey`, `entryAt`.
- [x] Update `mementum/state.md`: V-11 status from the acceptance run (with ratios of any
  failed rows), and replace the stale cause statement with the measured cause (method size
  stopped C2 inlining; `case` `tableswitch` and inline throw made the size).
- [x] Update `mementum/knowledge/benchmarks.md`: add method size/inlining as a gap cause
  and how to find it; replace the "current gap / start point" lines with the task 012
  acceptance-run `assoc` result; keep the task 011 result and 0.5.243 lines as history.

## Slice 6: final checks (re-check of the gate commit)

- [x] `git diff --stat <gate-sha> HEAD -- src test dev` and `git status --short src test
  dev` are empty. If a check below fails, do not change `src`; record the failure in
  `implementation.md`, keep the task open and ask the user (plan, Key decisions).
- [x] `clojure -M:test -m scry.cli` on the default alias, `:clj-1.12` and `:clj-1.13`:
  zero failures and errors (acceptance 3).
- [x] `clj-kondo --lint src test dev`: no errors and no warnings (acceptance 5).
- [x] `cljfmt check src test dev` passes (acceptance 5).
- [x] Verify all acceptance criteria 1–6 are met and recorded; commit.

## Implementation review

- [x] Move `## D-21 Method size` in `ramora/defclosedrecord/decisions/type-structure.md` after `## D-15 Syntax`.
  Each decisions file lists its records in ID order; D-21 is now between D-10 and D-15.

## Test review

- [x] Rewrite `test/closed_record/key_error_test.clj` to test only observable behaviour.
  - Now it calls the private `#'closed-record.core/key-error` directly, and it compares the `assoc`/`dissoc`
    message with the helper result. These tests fix an implementation decision (helper name, arguments, "returns,
    does not throw") and the comparison passes if both are wrong.
  - Remove the direct helper calls. Assert the literal message of `(assoc (->K 1) :z 2)` and
    `(dissoc (->K 1) :a)` (the Scope 2 text, with the munged class name). The `ex-data` is already covered in
    `defclosedrecord_test.clj`, `property_test.clj` and `malli_test.clj`; do not repeat it.
  - Change the ns docstring: D-03 makes the `ex-data` public, not the message. Say that the test keeps the message
    text unchanged (Scope 2). Rename the file and ns if the helper name no longer describes it.
  - Then run the three-version tests, clj-kondo and cljfmt. This is a `test` change after the acceptance run; record
    it in `implementation.md` (no `src` change, so the gate result stays valid).

## Docs review

- [x] Add `test/closed_record/rejected_key_message_test.clj` to the Tests list in `mementum/state.md`
  (`closed-record.rejected-key-message-test`: the `ex-info` message of a rejected `assoc` or `dissoc`, Scope 2).
  The list names each test file; this task added the file (test review follow-up) and did not list it.

## Code-shaper review

- [x] Rename the `record` parameter of `key-error` in `src/closed_record/core.cljc` to `c`, as in `class-name`.
  - The call sites pass `(class this)`, not a record. `(class-name record)` reads as a bug, and the docstring does
    not say that the argument is a class. Keep the `:record` key of the `ex-data` (D-03): `{:type t :record c :key k}`.
  - Say in the docstring that `c` is the record class. Then run the three-version tests, clj-kondo and cljfmt. This
    is a `src` change after the acceptance run; record it in `implementation.md` (the bytecode of the generated
    methods does not change, so the gate result stays valid).
