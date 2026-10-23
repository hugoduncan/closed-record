# State

Working memory. Read this file first in each session. Update it after each significant change.

## Project

closed-record: a `defrecord`-like record. It does not permit assoc of
undeclared keys. It does not permit dissoc of declared keys.
Main form: `defclosedrecord`.

## Status

- Repository initialised: README, AGENTS, psi extension config.
- Protocol directories initialised: ramora, mementum, munera.
- Design document: `ramora/defclosedrecord/` (status intended).
- Project skeleton: `deps.edn` (Clojure 1.11.4, aliases `:test`, `:bench`, `:nrepl`, `:clj-1.12`, `:clj-1.13`),
  `src/closed_record/core.cljc` (`defclosedrecord`, `check`, `ClosedRecord`),
  `src/closed_record/impl/jvm.clj` (JVM emitter). Test runner: scry.
- Tests: `test/closed_record/compiler_rules_test.clj` (K-06..K-08 compiler rules),
  `test/closed_record/defclosedrecord_test.clj` (behaviour, errors, factories),
  `test/closed_record/literal_test.clj` (V-04, V-05: print, read and code constants),
  `test/closed_record/field_value_test.clj` (V-06, V-08, V-09: field values; V-11: field declarations),
  `test/closed_record/hidden_key_test.clj` (V-14: hidden extra keys of a literal, and their retention),
  `test/closed_record/property_test.clj` (`closed-record.property-test`, V-03: test.check property of random
  `assoc`/`dissoc` sequences; `org.clojure/test.check` 1.1.3 is in the `:test` alias),
  `test/closed_record/differential_test.clj` (`closed-record.differential-test`, V-02: test.check property that
  compares a closed record with a `defrecord` after random permitted steps),
  `test/closed_record/malli_test.clj` (`closed-record.malli-test`, V-07: malli map schemas and transformers;
  `metosin/malli` 0.20.3 is in the `:test` alias).
- Verification plan done: V-01, V-02, V-03, V-04, V-05, V-06, V-07, V-08, V-09, V-10, V-12, V-13, V-14, V-15.
  Not done: V-11. Task 011 acceptance run (5 forks, criterium 0.5.251): `(assoc x :a 2)` fails, `P / R` 1.193
  (7.93 against 6.65 ns). The other five rows pass (`get` 1.049, `(.a x)` 0.995). The field declaration test
  (`field_value_test.clj`) passes. The probable cause of the `assoc` gap is the emitter dispatch (`case` against
  `condp identical?`); no measurement isolates it. A new task.
- Tests: `clojure -M:test -m scry.cli`. Other versions: add `:clj-1.12` or `:clj-1.13`.
- Bench: `clojure -M:bench` (V-11), 5 child JVM forks, about 6 min. It exits 1 if an operation fails, 2 if a fork
  fails. The pure layer (`closed-record.bench.report`) and the fork layer (`closed-record.bench.fork`) have tests
  in the suite (`bench_report_test.clj`, `bench_fork_test.clj`).
- REPL: `clojure -M:test:nrepl`. In the REPL: `(require '[scry.core :as scry]) (scry/run)`.

## Tasks

Munera tracks the tasks. Do not record tasks or task progress in this file.

- Task order: `munera/plan.md`.
- Open tasks: `munera/open/`. Progress: the `steps.md` file of each task.

## Key design facts

- The type uses `deftype*` with the `defrecord` layout. The `deftype` macro forbids the `__meta __extmap __hash __hasheq` fields.
- The compiler makes `create` and the n-argument constructor only with exactly those 4 trailing fields.
- Map literals with extra keys put them in `__extmap`. No observable result depends on it. `check` finds them.
- The type must not implement `IType`: `Compiler.emitValue` checks `IType` before `IRecord` (K-08).
- Equality and hash mirror `defrecord`: `.equals` = `mapEquals`, `.hashCode` = `mapHash`,
  `hasheq` = type-hash XOR `mapHasheq`, `=` requires the same class (D-07).
- `check` uses the internal `ClosedRecord` protocol (D-17). It throws for a value that is not a closed record (D-18).
- Design review done (2026-10-19). Decisions split into `ramora/defclosedrecord/decisions/`. Evidence E-01..E-07 in context.md.

## Related

- `README.md`
- `munera/plan.md`
- `ramora/INDEX.md`
