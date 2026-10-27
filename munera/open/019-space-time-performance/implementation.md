# Implementation notes

## 2026-10-27 — Architecture design review

- No architectural review feedback. Scope is unchanged.
- `ramora/META.md` and `doc/architecture.md` are absent. Architecture context
  came from `AGENTS.md` and `ramora/defclosedrecord.md`: purpose (including
  S-03), principles, invariants, qualities, embodiment, and its artifact map.
  `mementum/knowledge/benchmarks.md` supplies the existing measurement and
  harness constraints for later review turns.

## 2026-10-27 — Ambiguity design review

- No ambiguity review feedback. Scope is unchanged.

## 2026-10-27 — Inconsistency design review

- No inconsistency review feedback. Scope is unchanged.

## 2026-10-27 — Review handoff

- Benchmark rule ownership: `dev/bench/closed_record/bench/report.clj` owns
  S-03 timing statistics and pass decisions; `dev/bench/closed_record/bench/fork.clj`
  owns comparison order and child processes. Keep both independent of Criterium.
  Their regression tests are `test/closed_record/bench_report_test.clj` and
  `test/closed_record/bench_fork_test.clj`.

## 2026-10-27 — Ambiguity plan review

- No new ambiguity review feedback. Reviewed `plan.md` and read-only `steps.md`
  against the design, prior notes, benchmark code/tests, and referenced docs.

## 2026-10-27 — Inconsistency plan review

- No new inconsistency review feedback. The plan and read-only steps agree
  with the design and prior review notes.

## 2026-10-27 — Plan review handoff

- The S-03 field-layout check is `field-declaration-test` in
  `test/closed_record/field_value_test.clj`. In `deps.edn`, `:test` includes
  `dev/bench` without Criterium; preserve this dependency boundary when adding
  capability probes and experimental fixtures.

## 2026-10-27 — Feature API discovery

- Read all task artifacts and benchmark knowledge. No actor-step path or PR
  metadata was supplied; used the requested task in the current worktree.
- Upstream documentation distinguishes nullable JEP 401 value arrays, draft
  `!` types, and internal Valhalla storage APIs. `features.md` records URLs,
  source revision, available EA build, applicability limits, and the decision.
- Verification: retrieved the OpenJDK project, JEP 401, draft 8316779, and EA
  download pages with curl; inspected internal API source and resolved the
  lworld revision with git ls-remote. No runtime probes or benchmarks ran.
  This is discovery evidence, not proof of executable support or layout.
- No code or dependency changes; no tests are applicable to this prose-only
  slice. Next slice must obtain API intent before installing an experimental
  runtime. Design remains unchanged; plan and steps record the gate.

<!-- IMPLEMENTATION_BLOCKER: START -->
- blocker: Intended value-array and null-restricted-field APIs remain ambiguous between JEP 401, draft ! types, and internal Valhalla storage APIs.
- required-human-action: Select the intended API; confirm whether internal Valhalla APIs with atomic arrays and external comparison types are acceptable.
<!-- IMPLEMENTATION_BLOCKER: END -->

## 2026-10-27 — Plan ambiguity review after API permission

- Ambiguity review added 1 new design step; `steps.md` is unchanged.
- Commit `adfa652` changed only the design. The actionable issue is the
  proceed/stop decision after that permission, not a request to repeat API
  discovery. The later inconsistency review should use this same follow-up
  for the stale gate statements and blocker, rather than add a duplicate.

## 2026-10-27 — Plan inconsistency review after API permission

- No new inconsistency review feedback. The existing design step covers the
  stale feature-gate statements across task files; `steps.md` is unchanged.

## 2026-10-27 — Gate-resolution handoff

- Runtime setup must select the parent JVM explicitly: `:bench` in `deps.edn`
  does not select Java 21. `dev/bench/closed_record/bench/fork.clj` uses the
  parent's `java.home`, classpath, and JVM flags for children. Keep ordinary
  and experimental launch environments separate when resolving the gate.
  See `README/tool-installation.md` for the ordinary Java setup.

## 2026-10-27 — Plan-review API gate resolution

- The earlier `IMPLEMENTATION_BLOCKER` and its required human action are
  resolved by design commit `adfa652`. They remain above as historical notes,
  not active instructions. No human decision is pending before runtime setup.
- Runtime setup may proceed on the internal atomic-array/`@NullRestricted`
  path. Build-specific API verification and executable capability/layout checks
  remain prerequisites for the matrix; all six implementation slices remain open.
- Verification: reviewed the updated plan, features, and checklists against
  `adfa652` and the read-only design; `git diff --check` passes. No code,
  dependencies, runtime installation, or probe results changed. Runtime tests
  and benchmarks were not run for this plan-only follow-up.

## 2026-10-27 — Plan ambiguity review after gate resolution

- No new ambiguity review feedback. Reviewed the resolved API gate and remaining
  plan against task artifacts, benchmark code/tests, and referenced docs;
  `steps.md` is unchanged. The completed design follow-up needs no new item.

## 2026-10-27 — Plan inconsistency review after gate resolution

- No new inconsistency review feedback. The plan, read-only steps, feature gate,
  and completed design follow-up agree. The earlier blocker is explicitly
  resolved in the append-only notes; it is not an active stop condition.

## 2026-10-27 — Evidence transport handoff

- `bench-op` in `dev/bench/closed_record/bench.clj` reduces `last-bench` to
  per-call statistics before transport. The planned raw counts and batch divisors
  must be captured before that reduction; the current child EDN cannot recover
  them. Preserve this evidence when extending transport, not only its schema.
