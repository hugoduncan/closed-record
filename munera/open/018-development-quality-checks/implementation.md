# Implementation notes

## 2026-10-27 — Architecture review

- No architectural review feedback. Scope unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture sources:
  `AGENTS.md`, `ramora/defclosedrecord.md`, its principles and embodiment,
  and `ramora/release-workflow/` design and contracts. Use these sources in
  later review turns.
- The release architecture keeps publication in one job with step-scoped
  credentials. The read-only CI contract does not replace that release model;
  this task only shares its format/lint policy with release checks.

## 2026-10-27 — Ambiguity review

- Ambiguity review added 1 new design step. Scope unchanged.
- An unchanged index proves that a hook did not stage edits; it does not prove
  that the hook checked the content being committed. This distinction makes
  the hook acceptance clarification actionable, not an implementation choice.

## 2026-10-27 — Inconsistency review

- No new inconsistency review feedback. Scope unchanged. The existing hook
  ambiguity remains open in `design-steps.md`.

## 2026-10-27 — Handoff for hook clarification

- For later hook acceptance probes, use the Git isolation guidance in
  `mementum/knowledge/artifact-verification.md` and the fixture setup in
  `test-build/build_test.clj`. Remove inherited `GIT_*` overrides and isolate
  user Git configuration so probes cannot alter the real index. That fixture
  uses an empty hooks directory; hook probes must instead enable the hooks
  under test inside the disposable repository.

## 2026-10-27 — Hook contract follow-up

- At the reference commit, hook probes are in `test/tooling/hooks.sh`, not
  `test-tooling/hooks.sh` as some reference task notes state. They invoke
  `pre-commit run`; formatter scripts alone do not detect edits or protect
  partial staging. Test the complete hook execution path. The reference probes
  do not cover a staged lint defect hidden by an unstaged correction.

## 2026-10-27 — Architecture review after hook clarification

- No new architectural review feedback or design steps. The clarified index-based
  hook contract fits the maintainer-tooling boundary; it does not require a new
  runtime abstraction. Keep the mechanism choice in planning. Scope unchanged.

## 2026-10-27 — Ambiguity review after hook clarification

- No new ambiguity review feedback or design steps. The revised hook contract
  resolves the earlier staged-content finding; the open-finding statement in
  the earlier inconsistency review is historical. Scope unchanged.

## 2026-10-27 — Inconsistency review after hook clarification

- No new inconsistency review feedback or design steps. Checked the revised
  hook contract and acceptance criteria against the pinned reference guidance,
  current API, dependency and artifact boundaries, and workflow permissions.
  Scope unchanged.

## 2026-10-27 — Plan ambiguity review

- No new actionable ambiguity feedback or design steps. Reviewed the plan and
  read-only checklist against the task contract, current code/tests/workflows,
  and pinned reference. Mechanism risks have explicit verification steps.

## 2026-10-27 — Plan inconsistency review

- No new actionable inconsistency feedback or design steps. The plan and
  read-only checklist agree with the design and resolved review findings.

## 2026-10-27 — Plan-review handoff

- For API prose and content tests, distinguish runtime-read literals from
  compiled constants: compilation and print/read round trips drop hidden extra
  keys. Use `test/closed_record/literal_test.clj` and
  `test/closed_record/hidden_key_test.clj` as evidence; do not imply that `check`
  can recover keys already lost through these paths. This is existing behavior,
  not a new design follow-up.
