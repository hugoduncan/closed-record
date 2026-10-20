# Steps: 008 differential tests

## Slice 1: differential test namespace

- [ ] Create `test/closed_record/differential_test.clj` with ns `closed-record.differential-test`, a docstring
      naming V-02, requires for `clojure.test`, `test.check` (`quick-check`, generators, properties, results),
      and `closed-record.core`; define `(defclosedrecord P [a b c])` and `(defrecord R [a b c])`.
- [ ] Add private outcome-model helpers (`failure`, `attempt`, `judge`, `run-check`) with the same model as
      `test/closed_record/property_test.clj`: `attempt` catches `Throwable`; `failure` builds a
      `clojure.test.check.results/Result` with `pass?` false and `result-data`.
- [ ] Add generators: value (nil or small integer), start `[a b c]`, declared key (`:a :b :c`), undeclared key
      (`:x` or `"a"`), meta (nil or `{:n v}`), step (one of `[:assoc k v]` `[:conj k v]` `[:merge k v]`
      `[:into k v]` `[:update k v]` `[:dissoc k]` `[:with-meta m]`), steps `gen/vector` 0–100.
- [ ] Implement `apply-step`: `[:assoc k v]` → `(assoc x k v)`; `[:conj k v]` → `(conj x [k v])`;
      `[:merge k v]` → `(merge x {k v})`; `[:into k v]` → `(into x {k v})`; `[:update k v]` →
      `(update x k (constantly v))`; `[:dissoc k]` → `(dissoc x k)`; `[:with-meta m]` → `(with-meta x m)`.
- [ ] Implement the 21 checks in design order with candidate keys `[:a :b :c :x "a"]` (`:keyword` uses
      `[:a :b :c :x]`): `:type` `:entries` `:seq` `:count` `:get` `:keyword` `:contains` `:find` `:size`
      `:is-empty` `:map-get` `:contains-key` `:key-set` `:values` `:entry-set` `:contains-value` `:meta`
      `:hash-code` `:equals` `:start-equal` `:start-hash`. Each operation runs once per side in `attempt`; a
      throw on either side fails the check; per-key/per-value checks stop at the first failing key or value.
- [ ] Build `result-data` per design: `:check` always; `:step` (the step) for step failures; `:side`
      (`:closed`/`:record`) and `:ex` when an operation threw (both threw → `x` side); `:closed`/`:record`
      values otherwise (`:type` and `:equals` give only `:closed`); `:key` or `:value` where applicable.
- [ ] Implement the property: `x0` = `(->P a b c)`, `y0` = `(->R a b c)` outside `attempt`; run all checks on
      the start, then for each step apply it to both sides in `attempt` (throw → `:step` failure) and run all
      checks; stop at the first failure.
- [ ] Add the `deftest`: `(quick-check 200 property)`, one `is` on `:pass?`, failure message with `:seed`,
      `[:shrunk :smallest]` and `[:shrunk :result-data]`.
- [ ] Run `clojure -M:test -m scry.cli`: exit 0, zero failures, zero errors.
- [ ] Run `clojure -M:test:clj-1.12 -m scry.cli` and `clojure -M:test:clj-1.13 -m scry.cli`: exit 0, zero
      failures, zero errors.
- [ ] If a check fails on unchanged `src`: if it is an emitter fault, fix `src/closed_record/impl/jvm.clj`,
      record the cause in `implementation.md`, and rerun the three aliases. If the difference can be intended,
      stop and ask the user (Scope 5); do not decide.
- [ ] Commit Slice 1.

## Slice 2: mutation verification (acceptance 3)

- [ ] Mutation 1: in `src/closed_record/impl/jvm.clj`, make `seq` give the entries reversed. Run the test;
      confirm it fails with `[:shrunk :result-data :check]` = `:seq`. Record the mutation and failure report in
      `implementation.md`. Revert; confirm `git diff src` is empty.
- [ ] Mutation 2: make `valAt k nf` return nil in the default branch in place of `nf`, with `valAt k`
      unchanged. Run the test; confirm it fails with `:check` `:get` and `:key :x`. Record the mutation and
      failure report in `implementation.md`. Revert; confirm `git diff src` is empty.
- [ ] Run `clojure -M:test -m scry.cli` on clean `src`: exit 0, zero failures, zero errors.
- [ ] Commit Slice 2 (implementation.md only; no `src` change).

## Slice 3: lint, format and documents

- [ ] Run `clj-kondo --lint src test`: no errors, no warnings. Fix the test file if needed.
- [ ] Run `cljfmt check src test`: passes. Fix formatting if needed.
- [ ] Update `ramora/defclosedrecord/embodiment.md`: Artifacts row for
      `test/closed_record/differential_test.clj` (`closed-record.differential-test`, V-02).
- [ ] Update `mementum/state.md`: add `differential_test.clj` (V-02) to the first "Tests:" list; move V-02 from
      "Not done" to the "Verification plan done" list.
- [ ] Rerun the three test aliases: exit 0, zero failures, zero errors.
- [ ] Commit Slice 3.
