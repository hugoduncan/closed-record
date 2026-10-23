# Steps

## Slice 1: helper

- [ ] Add private `key-error` to `src/closed_record/core.cljc`, after `class-name`:
  `(key-error t record k)` returns an `ex-info` with the Scope 2 message (a `case` on `t`,
  using `class-name`) and data `{:type t, :record record, :key k}`. Prefix the `defn-`
  with `#_{:clj-kondo/ignore [:unused-private-var]}`.
- [ ] Add tests for `key-error` (called through `#'closed-record.core/key-error`): message
  text and `ex-data` for both `t` values.

## Slice 2: emitter

- [ ] In `src/closed_record/impl/jvm.clj`, change `assoc` to
  `(condp identical? k kw (new Name ... v ... __meta __extmap) ... default)` with one
  clause per field; default
  `(throw (#'closed-record.core/key-error :closed-record/undeclared-key (class this) k))`.
  With no fields, only the default.
- [ ] Change `without` to `identical?` tests of `k` against each declared keyword; a match
  gives `(throw (#'closed-record.core/key-error :closed-record/declared-key-dissoc (class this) k))`,
  no match gives `this`; with no fields, `this`.
- [ ] Change `containsKey` (both `IPersistentMap` and `java.util.Map`) to
  `(not (identical? this (.valAt this k this)))`.
- [ ] Change `entryAt` to
  `(let [v (.valAt this k this)] (when-not (identical? this v) (clojure.lang.MapEntry/create k v)))`.
- [ ] Remove the now-unused `case`/`closed-throw` use from these four methods only;
  `closed-throw` stays for `empty` and `map->Name`. Remove `key-case` users accordingly
  (`valAt` and `getLookupThunk` keep it).
- [ ] Run `clojure -M:test -m scry.cli` on the default alias; zero failures and errors.
- [ ] Run `clj-kondo --lint src test dev` and `cljfmt check src test dev` now, so that no
  `src` change is needed after the acceptance run.

## Slice 3: evidence (Scope 4)

- [ ] AOT-compile the scratch namespace (outside the repo) with `(defclosedrecord P [a b c])`
  and `(defrecord R [a b c])`, using the recorded commands in `implementation.md`.
- [ ] Record in `implementation.md`: the `javap` size table for `P` and `R`
  (`assoc`, `without`, `containsKey`, `entryAt`, `valAt`; the `IPersistentMap assoc` row),
  the `java -version` output, and the exact commands and scratch namespace.
- [ ] Verify acceptance 1: `assoc`, `without`, `containsKey`, `entryAt` of `P` each below
  325 bytes code length.
- [ ] Run `PrintInlining` (per-method `CompileCommand`, boxed-value hot loop) and record
  the C2 line `P::assoc (<n> bytes) inline (hot)` in `implementation.md` (acceptance 2).

## Slice 4: bench acceptance run (Scope 5)

- [ ] Confirm the emitter is final: tests pass, size table and `PrintInlining` recorded.
- [ ] Run `clojure -M:bench` (5 forks). Record the complete output and exit code in
  `implementation.md`. This first full run is the acceptance run; no later run replaces
  it, and the emitter does not change after it in this task.
- [ ] Determine V-11: done only if all six timed rows pass and
  `test/closed_record/field_value_test.clj` passes (S-03). If a timed row fails, note the
  ratio for the `state.md` update in slice 5.

## Slice 5: documents (Scope 6)

- [ ] Add `D-21 Method size` to `ramora/defclosedrecord/decisions/type-structure.md` with
  the full record shape (subject, choice, rejected, rationale, assumptions, qualities,
  scope, commitment "medium, as D-17") per Scope 6.
- [ ] Add `D-21 method size` to the "Type structure" index entry in
  `ramora/defclosedrecord/decisions.md`.
- [ ] Update D-02 Qualities in `ramora/defclosedrecord/decisions/closed-key-set.md` with
  the new "no cost on a permitted path" text (Scope 6).
- [ ] Update `ramora/defclosedrecord.md`: "D-01 to D-21", "constraints K-01 to K-10", and
  the `observed` paragraph (K-09/K-10 from task 012 measurements; V-11 is their signal).
- [ ] Add K-09 (C2 inline limit) and K-10 (keyword `case` size) to
  `ramora/defclosedrecord/context.md` Constraints; do not change the Evidence table.
- [ ] Update `ramora/defclosedrecord/qualities.md`: the D-21 trade-off and risk R-05.
- [ ] Update `ramora/defclosedrecord/principles.md`: add D-21 to principle 6; principle 28
  "K-06 to K-10" and add D-21.
- [ ] Update `ramora/defclosedrecord/embodiment.md`: `core.cljc` artifact row (`key-error`),
  forbidden-dependency sentence ("protocol name and the `key-error` var name only as
  symbols"), permitted dependencies, Boundaries Internal, and the method-behaviour rows for
  `assoc`, `without`, `containsKey`, `entryAt`.
- [ ] Update `mementum/state.md`: V-11 status from the acceptance run (with ratios of any
  failed rows), and replace the stale cause statement with the measured cause (method size
  stopped C2 inlining; `case` `tableswitch` and inline throw made the size).
- [ ] Update `mementum/knowledge/benchmarks.md`: add method size/inlining as a gap cause
  and how to find it; replace the "current gap / start point" lines with the task 012
  acceptance-run `assoc` result; keep the task 011 result and 0.5.243 lines as history.

## Slice 6: final checks

- [ ] `clojure -M:test -m scry.cli` on the default alias, `:clj-1.12` and `:clj-1.13`:
  zero failures and errors (acceptance 3).
- [ ] `clj-kondo --lint src test dev`: no errors and no warnings (acceptance 5).
- [ ] `cljfmt check src test dev` passes (acceptance 5).
- [ ] Verify all acceptance criteria 1–6 are met and recorded; commit.
