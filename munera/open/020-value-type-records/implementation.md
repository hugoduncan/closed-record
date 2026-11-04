# Implementation

## Design review (2026-11-03)

- architectural review added 2 new design steps
- Context: `ramora/META.md` and `doc/architecture.md` do not exist. The
  architecture sources are `AGENTS.md`, `ramora/INDEX.md`, the defclosedrecord
  pages (principles, embodiment, artifact map) and
  `ramora/development-quality.md`.
- Plan note, not a design finding: lint setup resolves each alias in
  `closed-record.lint-dependencies/dependency-bases` with `-Srepro`. The new
  value-type alias must go in that list, so lint setup (CI quality job and
  pre-commit) fetches the pinned value-type Git SHA.
- Plan note: `default-child-cmd` in `dev/bench/closed_record/bench/fork.clj`
  copies `java.home` and the JVM input arguments to each fork, so a VT-V8 run
  on Valhalla keeps `--enable-preview` in the child JVMs.
- `dev/bench/valhalla/` (task 019) also exists. "The existing bench
  structure" can mean the criterium forks or that directory; left to the
  ambiguity turn.
- ambiguity review added 4 new design steps
- "value-type AOT rules" (VT-V7) resolve to value-type
  `ramora/defvaluetype-design.md` D7 (cited from
  `ramora/deftype-star-design.md`): AOT fixes the representation. Not filed.
- inconsistency review added 1 new design step

## Notes for the design-steps pass

Principles:
- Resolve each step by editing design.md in place. Do not change VT-D1 to
  VT-D10 or the scope. Ramora pages get no progress history.
- VT-I2 comes first: the ordinary expansion, the default `:bench` run and the
  `:test` alias stay the same and do not load value-type.
- A representation expectation in the tests is test data, not a library
  switch (VT-D1). Detection of the JVM capability alone cannot catch silent
  fallback.

Paths:
- Gensym sites (VT-I2 comparison step): `src/closed_record/impl/jvm.clj`
  parameter gensyms `p__` and `this__`, the auto-gensyms of `cached-hash`,
  and `m#` in `withMeta`. Existing expansion test helpers (`in-test-ns`,
  `thrown`): `test/runtime/closed_record/defclosedrecord_test.clj` near
  line 489, `literal_test.clj` line 82.
- Subprocess JVM patterns (VT-V5, VT-V1 Clojure 1.11 case, VT-V7):
  `dev/bench/closed_record/bench/fork.clj`, `test/build/build_test.clj`
  (isolated consumer JVM).
- Bench: `dev/bench/closed_record/bench.clj` (S-03 exit code),
  `bench/report.clj`, `dev/bench/valhalla/*.java`, `test/bench/` (no CI job).
  The `dev/bench/` row in `ramora/defclosedrecord/embodiment/artifacts.md`
  already omits `memory/`, `timing/` and `valhalla/`.
- CI runtime matrix: `.github/workflows/ci.yml`, job "Runtime tests", with
  `matrix` entries that each have a `command`.
- value-type sources: `~/projects/hugoduncan/value-type/value-type-master/ramora/`
  `deftype-star-design.md` and `defvaluetype-design.md`.
