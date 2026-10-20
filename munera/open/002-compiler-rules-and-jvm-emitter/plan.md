# Plan

## Approach

Implement in strict slice order, compiler rule tests first. A rule failure on any supported Clojure version
(1.11.4, 1.12.6, 1.13.0-alpha9) stops the task: record in `implementation.md` and ask the user. Do not work
around a failed rule.

Key decisions (all fixed by design.md):

- Compiler rule tests (`test/closed_record/compiler_rules_test.clj`) use only `defrecord`, `deftype`, `deftype*`,
  the reader and the compiler. No closed-record namespace. `deftype*` forms are quoted data given to `eval`, so
  clj-kondo does not lint them.
- Emitter emits `deftype*` directly (D-01). One public function in `closed-record.impl.jvm` takes the type name,
  field vector with metadata, option map and specs, and returns the full expansion form ("Expansion form"
  decision). The function name and argument shape are internal.
- `closed-record.core` (cljc) holds the public API: `ClosedRecord` protocol with `-extra-keys` (D-17),
  `defclosedrecord`, `check`. It requires `impl.jvm` only in a `:clj` reader conditional. `impl.jvm` never
  requires `core`; it emits the protocol only as a symbol.
- Argument processing allocation: macro checks reserved names and duplicates and splits options/specs; emitter
  checks mutable field metadata and option keys, resolves protocol symbols, destructures method args, builds
  factories and the expansion form. No private `clojure.core` vars at expansion time or in generated code —
  reimplement `validate-fields`, `parse-opts+specs`, `maybe-destructured`, `build-positional-factory` with
  public functions.
- Error data: exactly the `ex-data` maps of the "Error data" table (7 paths, 5 `:type` values). Argument errors
  are host exceptions (`AssertionError`, `IllegalArgumentException`) with no `ex-data`; tests check `ex-cause`
  class, not the `CompilerException` wrapper.
- Emitter tests use the public macro and the generated type, never form comparison.
- Verification per slice: scry in the REPL during work; final check with the three CLI runs (default, 1.12,
  1.13), clj-kondo and cljfmt.

## Risks

- A compiler rule (K-06/K-07/K-08) may not hold on a version — by design this is a hard stop, not a blocker to
  route around.
- `deftype*` quoted-form tests are sensitive to exact field metadata (`^int ^:unsynchronized-mutable` on
  `__hash`/`__hasheq`); a metadata omission turns a rule test into a false failure. Follow the design's
  metadata requirements exactly.
- Reimplementing `parse-opts+specs`/`maybe-destructured`/`build-positional-factory` with public functions may
  drift from `defrecord` semantics. Mitigation: V-01/V-10 tests and the design's observed-fact anchors.
- `VerifyError` behaviour (no cause, thrown at `Class/forName` with initialization) could differ in a future
  Clojure; the three-version test matrix catches this early.
- clj-kondo zero-warning criterion depends on `:lint-as` config and quoted forms; verify early, not at the end.

## Slice order

1. Compiler rule tests (V-13) — Scope 1. Gate: all three CLI runs pass before any emitter work.
2. Public API skeleton: `ClosedRecord` protocol in `core.cljc` (D-17); `.clj-kondo/config.edn` with `:lint-as`.
3. JVM emitter in `src/closed_record/impl/jvm.clj`: argument checks, expansion form, generated type (fields,
   interfaces, methods, factories).
4. `defclosedrecord` macro and `check` in `closed-record.core`, with docstrings.
5. Tests: V-01 behaviour tables, V-10 (>20 fields), V-15 (`check`), expansion-time argument errors, `ex-data`
   table (7 paths). Record the V-01 unit-to-test mapping in `implementation.md`.
6. Cleanup: remove the smoke test; update `mementum/state.md` and `mementum/knowledge/dev-tooling.md`.
7. Final verification: three scry CLI runs, REPL scry, clj-kondo, cljfmt.
