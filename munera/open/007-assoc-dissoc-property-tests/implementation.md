# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

## Design review (architecture pass)

- Architecture sources: `ramora/META.md` and `doc/architecture.md` do not exist. The architecture is
  `ramora/INDEX.md` → `ramora/defclosedrecord.md` → `ramora/defclosedrecord/*.md`.
- Fit confirmed: test-only dependency in `:test` (embodiment `deps.edn` row: "Test and bench aliases"); no src
  dependency change, so the permitted and forbidden dependencies of embodiment do not change; `.clj` test is
  consistent with the other JVM-only tests (K-02 concerns the public API, not tests).
- Emitter fact: `size`, `keySet`, `values` and `entrySet` derive from `count`, `keys`, `vals` and `seq`
  (`jvm.clj`). Thus they agree now, but a mutation in one view is not found by a check of another view.
- architectural review added 2 new design steps
- ambiguity review added 5 new design steps
- inconsistency review added 1 new design step

## Notes for addressing design-steps (design review, 2026-10-20)

- Edit `design.md` in place (munera: no amendment markers). Keep scope frozen. Write in simplified technical English.
- Resolve these steps together: architecture step 1 (more `java.util.Map` views) changes the sub-check list of
  ambiguity step 5 (acceptance 4). Ambiguity step 4(a) (the `contains?` candidate keys) depends on the undeclared keys
  of the step generator in Scope 3.
- D-03 error data also has `:record` (`ramora/defclosedrecord/decisions/failure-and-validation.md`). Scope 4 checks
  only `:type` and `:key`. If you keep this subset, state that it is deliberate.
- Patterns to reuse: the view table in `test/closed_record/hidden_key_test.clj` (`.size`, `.keySet`, `.values`, ...);
  `java-map-views-test` and the `data-of` / `undeclared` helpers in `test/closed_record/defclosedrecord_test.clj`.
- Files that the steps change after implementation: `ramora/defclosedrecord/embodiment.md` (Artifacts table and
  `deps.edn` row), `mementum/state.md`.

## Design steps addressed (2026-10-20)

- All 8 design steps done in `design.md`. Extra choices, not asked by a step: `ex-data` checks compare the full D-03
  map with `:record` (as `undeclared` in `defclosedrecord_test.clj`); the undeclared key `"a"`; the I-01 checks also
  run on the start value.
- test.check 1.1.3 is in `~/.m2` (offline use works). Verified API: `gen/small-integer`, `(gen/vector g min max)`,
  `quick-check` result keys `:pass? :seed :shrunk`; `:shrunk` has `:smallest :result-data`.
- A failure value: `(reify clojure.test.check.results/Result (pass? [_] false) (result-data [_] {:check id}))`. Its
  data is in `[:shrunk :result-data]`. A throw in the body gives `{:clojure.test.check.properties/error e}` there.
- Shrinking keeps any failure, so `[:shrunk :result-data]` can name a different check than the first failing trial.
  In a mutation run, read the name from the shrunk result (acceptance 4).
- Candidate mutations (not run yet): `:size` → `size` body; `:key-set`, `:values`, `:entry-set` → their own method
  bodies (each calls `seq`); `:contains` → `containsKey`; `:check` → the `assoc` clause passes `(assoc __extmap k v)`
  in place of `__extmap` (breaks `check` only for `assoc` results, so it counts).

## Design review, pass 2 (architecture, 2026-10-20)

- no architectural review feedback
- ambiguity review added 1 new design step
  - Why actionable: `check` (`core.cljc`) only returns `x` or throws, so the only valid `:check` mutation throws.
    With the current text, that throw is reported as `{:clojure.test.check.properties/error e}`, which names no
    check, so acceptance 4 cannot be met for `:check`.
- inconsistency review added 1 new design step

## Notes for addressing design-steps (design review pass 2, 2026-10-20)

- Two open steps. Resolve them together: the throw rule (ambiguity) decides how the `:check` mutation is reported,
  and the Context list (inconsistency) names the code of that mutation.
- Throw rule: keep Scope 4, Scope 5 and acceptance 4 consistent. If a failure also carries the exception (for
  example `{:check <name> :ex e}`), change the `result-data` shape in Scope 5 and the `msg` text too. Keep "unexpected
  exception" (`:clojure.test.check.properties/error`) only for throws outside a check.
- Method locations at `db2082e`, `src/closed_record/impl/jvm.clj`: `containsKey`, `seq`, `iterator`, `assoc`,
  `without` in the `deftype*` method list; `size` → `(.count this)`, `keySet` → `(set (keys this))`, `values` →
  `(vals this)`, `entrySet` → `(set this)`. `check` is in `src/closed_record/core.cljc`. It calls `-extra-keys` of the
  `ClosedRecord` protocol, which returns `__extmap`.

## Design steps addressed, pass 2 (2026-10-20)

- Both open steps done in `design.md` (throw rule in Scope 4, `:ex` in Scope 5 `result-data`, Decisions bullet;
  Context method list). No `SCOPE_QUESTION:` items.
- Verified: `(into {} x)` reads through `iterator`, not `seq` (a `reify` with different `seq` and `iterator` gives the
  `iterator` entries). Thus a `seq`-only mutation does not break `:assoc-entries`; an `iterator` mutation does.
- Implementation hint: wrap each check in its own `try`/`catch Throwable`, so the throw names the check. Do not wrap
  `->P` or the model update; their throws must stay `:clojure.test.check.properties/error`.

## Design review, pass 3 (architecture, 2026-10-20)

- no architectural review feedback
- no ambiguity review feedback
  - Verified (Clojure 1.11.4): `(assoc p "a" 1)` throws `undeclared-key` with `:key "a"` and `:record` the class
    `P`; `(dissoc p "a")` is `identical?` to `p`. `:unknown` is a scry test status.
- inconsistency review added 1 new design step

## Notes for addressing design-steps (design review pass 3, 2026-10-20)

- One open step: reword one Decisions bullet (the I-01 views). Edit only that text. Do not change the check list,
  the check order or acceptance 4. Keep the scope frozen. Use simplified technical English.
- Source lines at `0c53ddd`, `src/closed_record/impl/jvm.clj`: `containsKey` 171, `seq` 178, `iterator` 180,
  `size` 201, `keySet`/`values`/`entrySet` 209–211. The candidate mutations in "Design steps addressed" (pass 1)
  already change the view method bodies, so they agree with the new sentence.
- Call paths from Clojure functions to the emitter methods: `mementum/knowledge/testing-generated-types.md`, section
  "Hidden keys and call paths".

## Design steps addressed, pass 3 (2026-10-20)

- The one open step is done: the Decisions bullet on the I-01 views now gives the call paths (`size` → `count`;
  `keySet`, `values`, `entrySet` → `seq`). The check list, check order and acceptance 4 did not change. No
  `SCOPE_QUESTION:` items.
- "First in `:keys` or `:count`" holds because the I-01 checks run on the start value before any step, and `:keys`
  and `:count` come before the view checks. `:assoc-entries` reads through `iterator`, so a `seq` fault does not show
  there. Call paths: `mementum/knowledge/testing-generated-types.md`, "Hidden keys and call paths".

## Design review, pass 4 (architecture, 2026-10-20)

- no architectural review feedback
- no ambiguity review feedback
- no inconsistency review feedback

## Notes after design review pass 4 (2026-10-20)

- No design steps were added, and none are open. All three review passes found nothing, so `design.md` is stable
  at `560288f`. The next lifecycle step is the plan (`plan.md`, `steps.md`), not a design-step pass.
- If a later review adds a design step, edit `design.md` in place. Keep the check list, the check order and
  acceptance 4 unchanged unless the step names them. These three parts depend on each other (the first failed check
  stops the property, and the shrunk `result-data` names that check).
