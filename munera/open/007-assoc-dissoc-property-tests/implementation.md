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

## Plan review, pass 1 (ambiguity, 2026-10-20)

- ambiguity review added 1 new design step
  - Why actionable: the `:check` mutation text ("make `check` in `core.cljc` throw for a valid value") permits an
    unconditional throw, which the mutation rules reject.
  - Verified at `e8ce834`, `jvm.clj`: `iterator` is `RecordIterator` with `(RT/iter nil)`, `seq` lists only the
    fields, and `count` is the constant field count. None of them reads `__extmap`. Thus the `assoc` → `(assoc
    __extmap k v)` change shows first in `:check`, after the first `assoc` of a declared key.
- inconsistency review added 1 new design step

## Notes for addressing design-steps (plan review pass 1, 2026-10-20)

- The two open steps change `plan.md` and `steps.md`, not `design.md`. `design.md` stays stable at `560288f`.
  Edit in place (no amendment markers). Keep the 13 checks, their order and the slice count. Use simplified
  technical English.
- `:check` step: the `assoc` clause is at `src/closed_record/impl/jvm.clj` 182–187 (`e8ce834`). The change
  replaces `~'__extmap` in the `new` form with `(assoc ~'__extmap ~k ~v)` (`__extmap` is nil for `->P` values;
  `(assoc nil k v)` gives a map). It breaks `check` only for `assoc` results, not for all values, so it agrees with
  the rule "breaks `check` for all values does not count". If you keep `core.cljc` as an option, the step must say
  which values throw.
- Emitter-fix step: in Slice 2, the trigger is a failure on unchanged `src`. Keep the Slice 3 revert check
  (`git diff src` empty), but take out its emitter-fix item, so that the fix is in one place only.
- Rules: `mementum/knowledge/testing-generated-types.md` ("Test design": one change per check, new JVM per run,
  `cp -R` to `/tmp`; "Hidden keys and call paths").

## Plan-review steps addressed, pass 1 (2026-10-20)

- Both open steps done in `plan.md` and `steps.md`. `design.md` did not change (stable at `560288f`).
- `:check` mutation is now named: `jvm.clj` `assoc` clause, declared-key branch, `~'__extmap` → `(assoc ~'__extmap
  ~k ~v)` in the `new` form. No mutation is in `core.cljc`.
  - Verified in a `/tmp` copy (new JVM, Clojure 1.11.4): `(check (->P 0 nil 0))` is `identical?`; for
    `r = (assoc p :a 1)`, `:assoc-type`, `:assoc-entries` and the 7 I-01 checks before `:check` pass, and
    `(check r)` throws `{:type :closed-record/extra-keys :record P :keys #{:a}}`. Thus the shrunk failure names
    `:check`, with `:ex`.
- The emitter-fix step is now in Slice 2 only (after the scry runs on unchanged `src`). Slice 3 keeps only the
  revert check (`git diff src` empty).

## Plan review, pass 2 (ambiguity, at `b36f100`)

- no ambiguity review feedback
  - Checked for Slice 3: `count` (`jvm.clj` 143) is the constant `n`. `keys` and `set` read through `seq`, and
    `into` reads through `iterator`, so neither calls `count`. Thus a `count` change shows first in `:count`, and the
    13 named mutations each show first in their own check.
- no inconsistency review feedback
  - Note for Slice 3: the revert check (`git diff src` empty) assumes that a Slice 2 emitter fix, if any, is
    committed before the mutation runs.

## Notes after plan review pass 2

- No design steps were added, and none are open. `plan.md` and `steps.md` are stable at `b36f100`. The next
  lifecycle step is implementation (Slice 1), not a design-step pass.
- If a later review adds a step, keep the 13 checks, their order and the one-change-per-check mutations, unless the
  step names them. Mutation rules: `mementum/knowledge/testing-generated-types.md` ("Test design", "Hidden keys and
  call paths").

## Slices 1–2 implemented (2026-10-20)

- `deps.edn` `:test` has `org.clojure/test.check` 1.1.3. The new test resolves offline.
- `test/closed_record/property_test.clj`: `run-check` (pass if `(ok? (op))`, any `Throwable` fails the id) and
  `throws-check` (pass only for `ex-info` with `=` `ex-data`). Checks return nil on pass, else a failure `Result`;
  `run-step` returns the next `[x model]` vector or a failure, and `run-steps` stops with `reduced`.
- `:assoc-type` keeps the `assoc` result in a `volatile!` for `:assoc-entries`. The model update `(assoc model k v)`
  is outside every check, as Scope 4 says.
- All three aliases: 76 tests, 0 failed, 0 errored; scry counts the property test as passed. No emitter fault.
- kondo clean; cljfmt needed one `fix` (indent of the `prop/for-all` body). Slice 3 (mutations) is next: edit a
  `/tmp` copy, not the worktree.

## Slices 3–4 implemented (2026-10-20)

- Mutations: one change each in a `/tmp` copy of `src/closed_record/impl/jvm.clj`, a new JVM for each run,
  `quick-check 200`. Each shrunk `result-data` named its own check (`:ex` shows the throw message):

| Check | Change in `jvm.clj` | Shrunk report |
|---|---|---|
| `:assoc-type` | `assoc` declared branch → `(assoc (into {} this) k v)` | `[[nil nil nil] [[:assoc :a nil]]]` |
| `:assoc-entries` | `assoc` declared branch drops `v` (`new` with old `fields`) | `[[nil nil nil] [[:assoc :a 0]]]` |
| `:assoc-undeclared-throws` | undeclared-key `ex-data` `:key nil` | `[[nil nil nil] [[:assoc :x nil]]]`, `:ex` |
| `:dissoc-declared-throws` | declared-key-dissoc `ex-data` `:key nil` | `[[nil nil nil] [[:dissoc :a]]]`, `:ex` |
| `:dissoc-undeclared-identical` | `without` default → `(new tname ~@fields __meta __extmap)` | `[[nil nil nil] [[:dissoc :x]]]` |
| `:keys` | `seq` maps over `(butlast kws)` | `[[nil nil nil] []]` |
| `:key-set` | `keySet` → `(disj (set (keys this)) :a)` | `[[nil nil nil] []]` |
| `:entry-set` | `entrySet` → `(set (rest this))` | `[[nil nil nil] []]` |
| `:contains` | `containsKey` default `false` → `true` | `[[nil nil nil] []]` |
| `:count` | `count` → `(inc n)` | `[[nil nil nil] []]` |
| `:size` | `size` → `(inc (.count this))` | `[[nil nil nil] []]` |
| `:values` | `values` → `(rest (vals this))` | `[[nil nil nil] []]` |
| `:check` | `assoc` declared branch passes `(assoc __extmap k v)` | `[[nil nil nil] [[:assoc :a nil]]]`, `:ex` extra keys `#{:a}` |

- Harness (not committed): python exact-string replace in the copy, then `clojure -M:test -e` that calls
  `quick-check` on the private var `closed-key-set-property` and prints `[:shrunk :result-data]`.
- Worktree `git diff src` empty. kondo 0/0, cljfmt clean, three aliases 76 tests passed. Docs updated
  (`embodiment.md` Artifacts row and `deps.edn` row, `mementum/state.md` V-03 done). All steps done.

## Implementation review (2026-10-20)

- no implementation review feedback

## Test review (2026-10-20)

- no test review feedback

## Test-shaper review (2026-10-20)

- no test-shaper review feedback

## Docs review (2026-10-20)

- no docs review feedback
  - Out of scope, earlier than this task: `embodiment.md` names `dev/bench/`, bench aliases and differential and
    malli tests, which do not exist yet (V-02, V-07, V-11 not done).

## Code-shaper review (2026-10-20)

- code-shaper review added 2 steps to be addressed

## Code-shaper follow-up addressed (2026-10-20)

- addressed 2 review steps
- `attempt` runs an operation once (`{:value v}` or `{:ex e}`); `judge` checks an outcome; `run-check` is
  `(judge id (attempt op) ok?)`. `:assoc-type` and `:assoc-entries` judge the same outcome; no `volatile!`.
- `failure?` is `instance?` of the `clojure.test.check.results.Result` interface (the `reify` implements it).
  Do not use `satisfies?`: test.check extends `Result` to `Object` and `nil`.
- Three aliases: 76 tests passed. `/tmp` mutations: `:check` → `{:check :check :ex extra keys #{:a}}`;
  `:assoc-entries` and `:assoc-type` name their own checks. kondo 0/0, cljfmt clean, `git diff src` empty.

## Code-shaper review, pass 2 (2026-10-20)

- code-shaper review added 1 step to be addressed

## Code-shaper follow-up, pass 2 addressed (2026-10-20)

- addressed 1 review step
- `throws-check` uses `attempt`: no `:ex` → `(failure id nil)`; `(= data (ex-data ex))` → pass; else
  `(failure id ex)`. No `try` outside `attempt` and `judge`.
- Three aliases: 76 tests passed. `/tmp` mutations (`:key nil` in `ex-data`): `:assoc-undeclared-throws` →
  `[[nil nil nil] [[:assoc :x nil]]]`; `:dissoc-declared-throws` → `[[nil nil nil] [[:dissoc :a]]]`; each with
  `:ex`. kondo 0/0, cljfmt clean, `git diff src` empty.

## Code-shaper review, pass 3 (2026-10-20)

- code-shaper review added 1 step to be addressed

## Code-shaper follow-up, pass 3 addressed (2026-10-20)

- addressed 1 review step
- `judge` docstring now names `outcome`, not `op`. Docstring only. kondo 0/0, cljfmt clean, default alias 76 tests
  passed.

## Code-shaper review, pass 4 (2026-10-20)

- no code-shaper review feedback
