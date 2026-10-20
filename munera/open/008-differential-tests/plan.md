# Plan: 008 differential tests

## Approach

Implement in three slices, each independently verifiable. `test.check` 1.1.3 is already in the `:test` alias
(task 007), so there is no dependency slice.

1. **Differential test namespace**: create `test/closed_record/differential_test.clj`
   (`closed-record.differential-test`, V-02) with:
   - Two types: `(defclosedrecord P [a b c])` and `(defrecord R [a b c])`.
   - Private helpers with the task 007 outcome model (`failure`, `attempt`, `judge`, `run-check`). The helpers
     in `property_test.clj` are `defn-`, so the new namespace defines its own; it does not require
     `closed-record.property-test`.
   - Generators (Scope 2): value (nil or small integer), start `[a b c]`, step (7 kinds:
     `:assoc`/`:conj`/`:merge`/`:into`/`:update` on a declared key, `:dissoc` on an undeclared key (`:x` or
     `"a"`), `:with-meta` with nil or `{:n v}`), step sequence `gen/vector` 0–100.
   - One property (Scope 3): `x` = `(->P a b c)`, `y` = `(->R a b c)` (setup, not caught). After the start and
     after each step, run the 21 named checks in the listed order (`:type` … `:start-hash`). Candidate keys
     `[:a :b :c :x "a"]`; `:keyword` uses `[:a :b :c :x]` only. Stop at the first failed step or check.
   - Throws: each step and each check operation runs once per side in `attempt`. A throw on either side fails
     (`:side` `:closed` or `:record`, `:ex` the `Throwable`; both throw → report the `x` side). A throw on
     both sides also fails.
   - `result-data` keys (Scope 3): `:check` always; `:step` for step failures; `:closed`/`:record` for a value
     difference (`:type` and `:equals` give only `:closed`); `:key` or `:value` for the first failing key or
     value in the per-key/per-value checks, which stop there.
   - One `deftest` (Scope 4): `(quick-check 200 property)`, one `is`, message shows `:seed`,
     `[:shrunk :smallest]` and `[:shrunk :result-data]`.
   - Run `clojure -M:test -m scry.cli` with no alias, `:clj-1.12` and `:clj-1.13` on unchanged `src`.
   - If a check fails because of the emitter (Scope 5), fix `src/closed_record/impl/jvm.clj` and record the
     cause in `implementation.md`. If the difference can be intended, stop and ask the user; the agent does
     not decide an intended difference (D-20, principle 15). After a human decision, follow Scope 5 (a)–(d).
2. **Mutation verification** (acceptance 3): two mutations in `src/closed_record/impl/jvm.clj`, each applied
   alone, with the test run and the diff reverted after:
   - `seq` returns the entries reversed. Expected `[:shrunk :result-data :check]` = `:seq`.
   - `valAt k nf` returns nil in the default branch in place of `nf`; `valAt k` unchanged (it passes nil, so
     its result does not change, and `:entries` via `iterator` → `valAt k` stays correct). Expected `:get`
     with `:key :x`.
   Record each mutation and its failure report in `implementation.md`. Follow the mutation rules in
   `mementum/knowledge/testing-generated-types.md`. `git diff src` empty after.
3. **Lint, format and documents**: `clj-kondo --lint src test` (no errors, no warnings), `cljfmt check src
   test`. Update `ramora/defclosedrecord/embodiment.md` (Artifacts row for `differential_test.clj`, V-02) and
   `mementum/state.md` (add the file to the first "Tests:" list; move V-02 from "Not done" to done).

Key decisions (from design):

- Compare with a real `defrecord`, not a model map: the subject of V-02 is parity.
- `:start-equal` and `:start-hash` compare within each type, not across types (D-07).
- The property reads hashes before each `:with-meta` step, so metadata-on-hash faults are V-12's job, not this
  property's.
- No keyword call site in the property (call-site thunks keep state across trials); V-06, V-08, V-09 cover
  `getLookupThunk`.
- Observed facts (design, 2026-10-20): the two types agree on every checked operation at commit `820e889`, so
  the property is expected to pass on unchanged `src`.

## Risks

- A latent emitter difference may surface under long sequences. If it is a fault, fix the emitter (Slice 1).
  If it can be intended, this blocks on a human decision (Scope 5): stop and ask.
- Check order sensitivity: the property stops at the first failure, so each acceptance-3 mutation must not be
  seen by an earlier check. The design argues this (`:entries` reads `iterator` → `valAt k`; `seq` reads the
  fields directly, verified in review 2). Verify empirically in Slice 2.
- `.keySet`/`.values`/`.entrySet` view comparisons: probes (implementation.md) show both types return the same
  view classes, so `=` works without conversion. If a version difference appears under `:clj-1.12`/`:clj-1.13`,
  record it and compare as seqs/sets only if both sides change identically — otherwise it is a real difference.
- scry CLI vs REPL differences for generated types: follow `mementum/knowledge/testing-generated-types.md`;
  verify in the CLI on all three versions, not only the REPL.

## Slice order

1. Create `test/closed_record/differential_test.clj` (types, helpers, generators, property, deftest); run the
   three test aliases on unchanged `src`; fix the emitter if a check finds a fault.
2. Mutation verification of the two acceptance-3 mutations; record results in `implementation.md`.
3. Lint and format checks; update `embodiment.md` and `mementum/state.md`.
