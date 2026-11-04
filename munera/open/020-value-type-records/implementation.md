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
