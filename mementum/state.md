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
- No source code yet.
- No tasks yet.

## Next

- Create munera tasks from `ramora/defclosedrecord/` (skeleton, JVM emitter, tests, benchmarks).
- Create the project skeleton (deps.edn, src, test).

## Key design facts

- The type uses `deftype*` with the `defrecord` layout. The `deftype` macro forbids the `__meta __extmap __hash __hasheq` fields.
- The compiler makes `create` and the n-argument constructor only with exactly those 4 trailing fields.
- Map literals with extra keys put them in `__extmap`. No operation reads it. `check` finds them.

## Related

- `README.md`
- `munera/plan.md`
- `ramora/INDEX.md`
