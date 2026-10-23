# Implementation notes

## 2026-10-23 — Architecture review

- No architectural review feedback. The scope is unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture context
  came from `AGENTS.md` and `ramora/defclosedrecord.md`, with its linked
  principles, embodiment, invariants, qualities, and platform decisions.
  Use these sources for later design-review turns.

## 2026-10-23 — Ambiguity review

- No ambiguity review feedback. The scope is unchanged.

## 2026-10-23 — Inconsistency review

- No inconsistency review feedback. The scope is unchanged.
- The scry reference generates an SCM tag `v<version>` and its core JAR
  builder does not copy the license. Do not copy these details: this task's
  explicit metadata and license contracts take precedence over the example.

## 2026-10-23 — Review handoff

- Verified license source: `~/projects/hugoduncan/scry/scry-master/LICENSE`
  contains EPL-2.0 text. Use this file, not the reference build's POM metadata,
  as the source for the license file.

## 2026-10-23 — Plan ambiguity review

- No new ambiguity review feedback on `plan.md` and `steps.md`.

## 2026-10-23 — Plan inconsistency review

- No new inconsistency review feedback across the task files.

## 2026-10-23 — Plan review handoff

- Checked the pinned scry `0.1.61` JAR, not only the reference checkout:
  `scry/cli.clj` supports `--dir`; `scry/core.clj` documents default discovery
  under `test`. The planned build-test directory needs no runner upgrade.
- For consumer error assertions, use the `:type` values in
  `src/closed_record/core.cljc` and the assoc/dissoc checks in
  `test/closed_record/defclosedrecord_test.clj`; both rejected operations
  throw `ExceptionInfo`, so the class alone does not distinguish them.
