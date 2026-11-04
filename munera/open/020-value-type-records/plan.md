# Plan

## Approach

Implement the `:value-type` option in slices that keep VT-I2 (unchanged
ordinary expansion) verifiable at every point. Record the expansion fixture
from the unmodified emitter first, then change the emitter, then add tests,
dependencies, CI and documentation.

Key decisions:

- **Emitter shape**: a small representation description, selected once from
  the option in `emit`. It holds the four differences (type-form head, hidden
  fields, hash forms, `withMeta` form). The affected emitter functions receive
  the description as explicit data. This keeps the differences in one place
  and leaves the ordinary path a constant description, so the ordinary
  expansion stays unchanged (VT-I2). No dynamic var, no global state (VT-D1).
- **Expansion fixture first**: record the fixture from the emitter at the task
  start (same as `ac25942`), before any emitter change. The fixture comparison
  uses printed forms with `*print-meta*` true after gensym normalisation, as
  the design specifies. Fixture forms cover `:no-print`, `:load-ns`, field
  tags, user interfaces and methods, and destructured method parameters.
- **Test directory layout**: two new directories, both direct children of
  `test/`:
  - `test/value-type/` — the value-type suite (VT-V2 to VT-V7).
  - `test/value-type-clj-1.11/` — the Clojure 1.11 case of VT-V1.
  Neither is under `test/runtime/`, so ordinary `--dir test/runtime` commands
  and the value-type suite command do not scan the other.
- **Shared checks**: extract the ordinary runtime tests that check run-time
  behaviour of a defined record type (per the design's shared check selection
  rule) into shared check definition namespaces under `test/runtime/`
  (loadable without value-type). The ordinary suite and the value-type suite
  both call them. Representation-dependent checks (hash cache, `locking`,
  reference `identical?`) branch on the expectation property
  `closed-record.test.representation` in the value-type suite only.
- **Alias**: one new alias `:value-type-test` in `deps.edn` with value-type at
  a pinned Git SHA and the two test directory paths. Base `:deps` unchanged.
  Add the alias to `closed-record.lint-dependencies/dependency-bases` so lint
  setup fetches the pin.
- **Error contract**: in value mode, `emit` calls
  `(requiring-resolve 'value-type.core/defvaluetype*)` inside try/catch.
  Throw → `ex-info :closed-record/value-type-unavailable` with the thrown
  exception as cause. `nil` → same `ex-info` with no cause. `:class-name` is
  the qualified class-name symbol (`classname` in `emit`).
- **VT-V8 bench**: a separate entry namespace in `dev/bench` that no other
  namespace requires, with its own child command measuring both record kinds
  in the same child JVM. Extend `dev/bench/timing/run.sh` to accept aliases
  and JVM options as arguments, default run unchanged. Use the criterium fork
  harness, not `dev/bench/valhalla/` probes. `default-child-cmd` already
  copies `java.home` and JVM input arguments to forks, so `--enable-preview`
  propagates.
- **CI**: two new required jobs per VT-D10, with the pinned Valhalla archive
  (SHA-256 check, `actions/cache` keyed on the SHA-256) and per-step scry
  failure uploads with unique names.

## Risks

- **Prerequisite unmet (blocker for slice 4)**: value-type `66d5ff7`
  (contains `defvaluetype*`) is not on GitHub; origin/master is at `5d2656b`.
  Slices 1–3 can proceed (no committed value-type dependency). At slice 4
  (test alias), stop and ask the user per the design, unless the user has
  pushed by then.
- **Valhalla download flakiness**: `download.java.net` availability; mitigated
  by `actions/cache` keyed on the SHA-256.
- **Shared check extraction churn**: refactoring the ordinary runtime tests
  risks accidental behaviour change; mitigated by running the ordinary suite
  on Clojure 1.11/1.12/1.13 after the extraction, before any value-type use.
- **Gensym normalisation fragility**: the fixture comparison depends on the
  printed-form normalisation rule; verify it is stable across two expansions
  of the same form before recording the fixture.
- **value-type defect**: if found, record facts in implementation.md and stop
  for a user decision (out of scope to fix).
- **Subprocess tests** (absent-var stub, does-not-load check, VT-V5, VT-V7):
  subprocess JVM construction (java binary, input args, class path) must be
  shared helper code to avoid divergence.

## Slice order

1. **Expansion fixture** — record the fixture from the unchanged emitter;
   add the normalisation and comparison code and the VT-V1 fixture test in
   `test/runtime`.
2. **Option and error contract** — `check-options` accepts `:value-type`;
   value checking; `:closed-record/value-type-unavailable` with correct cause
   semantics; ordinary-suite VT-V1 tests (option values, errors,
   missing-library cause, absent-var subprocess stub case).
3. **Emitter value mode** — representation description; VT-D3 to VT-D8
   differences; fixture test still passes for no-option and
   `:value-type false`.
4. **Test alias and shared checks** — `:value-type-test` alias with the pinned
   GitHub SHA (**gate: requires user push**); lint-dependencies entry;
   shared check extraction in `test/runtime`; ordinary suite green on 1.11,
   1.12, 1.13 without value-type.
5. **Value-type suite** — `test/value-type/`: representation expectation
   property handling; VT-V2, VT-V3, VT-V4, VT-V6; subprocess checks VT-V5 and
   does-not-load; VT-V7 AOT check; `test/value-type-clj-1.11/` with the
   Clojure 1.11 VT-V1 case. Local runs on Temurin 21 and Valhalla.
6. **CI** — the two required jobs, archive pin and cache, failure uploads,
   release workflow verified unchanged (`workflow_call` picks up new jobs).
7. **VT-V8 bench** — entry namespace, `run.sh` extension, manual Valhalla
   run; results in `implementation.md` and `doc/performance.md`; default
   `:bench` run verified unchanged.
8. **Documentation** — docstring, README, `doc/development.md`,
   `doc/tool-installation.md`, `doc/API.md` regeneration, Ramora value-type
   pages, defclosedrecord embodiment and artifact map, `mementum/state.md`.
9. **Quality gate** — format, lint, prose, API freshness, link, build and
   release checks.
