# Implementation notes

Add decisions, discoveries and trade-offs here during implementation. Do not change earlier entries.

- Design review (architecture), 2026-10-19: no architectural review feedback. `ramora/META.md` and
  `doc/architecture.md` do not exist; the architecture source is `ramora/INDEX.md` → `ramora/defclosedrecord.md`.
- Design review (ambiguity), 2026-10-19: no ambiguity review feedback. Fact for the inconsistency pass: `map->Name`
  with extra keys throws `:closed-record/extra-keys` (`map-factory` in `jvm.clj`, docstring in `core.cljc`), not
  `undeclared-key` as "Out of scope" says.
- Design review (inconsistency), 2026-10-19: inconsistency review added 2 new design steps.
- Notes for the design steps (2026-10-19):
  - Edit design.md in place (no revision markers). Scope is frozen: no new or removed cases in Scope 3, Scope 4 or
    "Out of scope". Use simplified technical English.
  - `map->Name` step: the V-01 row is in `ramora/defclosedrecord/purpose.md` (behaviour table, `map->Name`:
    "extra key → throw"). Code: `map-factory`, `src/closed_record/impl/jvm.clj`.
  - Retention rule step: paths in the emitter (`cons` and `without`, `src/closed_record/impl/jvm.clj`):
    `(conj lit [:a 5])` → `cons` vector branch → `assoc`. `(merge lit {:b 9})` → `conj` of a map → `cons` `:else`
    branch (loop of `assoc`), so `merge` covers a different path. `update` → `assoc`. `into` of pairs → `cons` vector
    branch, as `conj`. `(dissoc lit :z)` → `without` returns `this`. A rule that fits: "an operation is a case if it
    reaches an emitter path that no other case reaches". No case reaches the `cons` map-entry branch. Do not add one
    (scope).
- Design steps done (2026-10-19): "Out of scope" now says `map->Name` throws `extra-keys`. The `update`/`into` rule
  is replaced by one emitter path per case and a reason per non-case. Checked at the REPL (Clojure 1.11.4):
  `check` throws `extra-keys` `#{:z}` for `(into lit {:b 9})`, `(into lit [[:b 9]])`, `(conj lit (first {:b 9}))`
  and `(update lit :a inc)`. Known gap, not in scope: no Scope 4 case reaches the `cons` map-entry branch. Only a
  human may add a case for it.
- Design review (architecture, pass 2, after design steps), 2026-10-19: no architectural review feedback.
- Design review (ambiguity, pass 2, after design steps), 2026-10-19: no ambiguity review feedback. Checked
  (Clojure 1.11.4): `.values` is an `APersistentMap$ValSeq`, so `=` compares it by value. `=` of two `.iterator`
  results is false; compare them through `iterator-seq`, as the Context table says.
- Design review (inconsistency, pass 2, after design steps), 2026-10-19: no inconsistency review feedback.
- Design review pass 2 (2026-10-19): no new design steps; all items in design-steps.md are done. Next step:
  plan (`create-task-plan`). For the plan: the helpers `read-literal` (`in-test-ns` + `read-string`) and `data-of`
  are private copies in `test/closed_record/defclosedrecord_test.clj` and `literal_test.clj`. The new namespace
  needs its own copy, as the other test namespaces have.
