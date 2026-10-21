# Design steps

- [ ] Architecture review: SCOPE_QUESTION: D-16 is not in the documents that Scope 6 and 7 update.
  - D-16 (`ramora/defclosedrecord/decisions/verification.md`) includes "criterium benchmarks against `defrecord`"
    in the verification plan, with scope "CI matrix Clojure 1.11, 1.12, 1.13".
  - This design decides that the bench is not in CI (Out of scope 1) and that only Clojure 1.11.4 is an
    acceptance run (Out of scope 3). That is an exception to D-16, but the design does not record it in D-16.
  - After this task, D-16 states a verification scope for V-11 that the embodiment does not meet.
  - A human decides: add a D-16 update to Scope 7 (V-11 is a manual run, not in CI, on 1.11), or accept the gap.
- [x] Ambiguity review: say what "stop" means in Out of scope 4, and what the docs record when an operation fails.
  - "record the result in `implementation.md` and stop" can mean: stop all work, or do not fix the emitter but
    complete Scope 6 and 7. The probe (`assoc` about 14% slower) makes this case likely.
  - Scope 7 says `mementum/state.md` records "V-11 done". It does not say what `state.md` (and the README) record
    when `clojure -M:bench` exits 1: V-11 done with a failed operation, or V-11 open.
  - Say which work completes, and the V-11 status text, when one or more operations fail.
- [x] Ambiguity review: define "the ratio" and the columns of the Scope 5 table.
  - The ratio can be `P / R` or `R / P`, as a factor or as a percent. The direction changes how a reader applies the
    5% rule.
  - Give the column order, the ratio formula (for example `P mean / R mean`), and the unit (ns for one call).
- [x] Ambiguity review (pass 2): give the new text of the Scope 7 targets that have no stated content.
  - `embodiment.md` `dev/bench/` row: the row already says "criterium benchmarks against `defrecord`". Scope 7
    says "update", but not to what. It can stay as it is, or name `closed_record/bench.clj` and the five
    operations. Acceptance 5 cannot check it.
  - `mementum/state.md` "the bench command": say the line that gets it (for example a "Bench:" line after the
    "Tests:" line), and whether the skeleton alias list (`:test`, `:nrepl`, `:clj-1.12`, `:clj-1.13`) adds `:bench`.
- [x] Inconsistency review (pass 3): the Scope 7 `deps.edn` row text does not agree with `deps.edn` and `state.md`.
  - Scope 7 gives the `embodiment.md` `deps.edn` row the text "Aliases `:test` and `:bench`." This reads as the
    full alias list.
  - `deps.edn` also has `:nrepl`, `:clj-1.12` and `:clj-1.13`. The Scope 7 `state.md` "Project skeleton" text lists
    all five aliases.
  - Change the Scope 7 row text so that it does not state a partial list as the full list (for example, list all
    five aliases).
