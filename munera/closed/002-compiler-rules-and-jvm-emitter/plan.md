# Plan

## Approach

Implement in strict slice order, compiler rule tests first. A rule failure on any supported Clojure version
(1.11.4, 1.12.6, 1.13.0-alpha9) stops the task: record in `implementation.md` and ask the user. Do not work
around a failed rule.

Key decisions (fixed by design.md; the K-06 lookup, K-08 constants and `:cljs` content are plan-level test and
layout details inside the design):

- Compiler rule tests (`test/closed_record/compiler_rules_test.clj`) use only `defrecord`, `deftype`, `deftype*`,
  the reader and the compiler. No closed-record namespace. `deftype*` forms are quoted data given to `eval`, so
  clj-kondo does not lint them.
- K-06 class lookup: the design's `(Class/forName class-name true (clojure.lang.RT/baseLoader))` runs inside
  `(with-bindings {clojure.lang.Compiler/LOADER (clojure.lang.DynamicClassLoader.)} ...)`. Then `RT/baseLoader`
  is a `DynamicClassLoader` in the scry CLI and in the REPL, and it finds the `eval`ed class. Without the binding,
  the CLI gives `ClassNotFoundException`, not the K-06 result.
- K-08 constants: `((eval (list 'fn [] v)))` for an instance `v`. The `IRecord` path uses the K-06 layout without
  `IType` (`create` from the `entrySet` view). The `IType` order uses a `[a]`-only `deftype*` (no `create`) with and
  without `IType`. `steps.md` Slice 1 gives the forms and assertions.
- Emitter emits `deftype*` directly (D-01). One public function in `closed-record.impl.jvm` takes the type name,
  field vector with metadata, option map and specs, and returns the full expansion form ("Expansion form"
  decision). The function name and argument shape are internal.
- `closed-record.core` (cljc) holds the public API: `ClosedRecord` protocol with `-extra-keys` (D-17),
  `defclosedrecord`, `check`. It requires `impl.jvm` only in a `:clj` reader conditional. `impl.jvm` never
  requires `core`; it emits the protocol only as a symbol.
- `:cljs` content of `core.cljc`: `ClosedRecord` and `check` only. Both are portable; the class of a value in
  `check` is `#?(:clj (class x) :cljs (type x))`. The whole `defclosedrecord` macro is inside `#?(:clj ...)`.
  ClojureScript behaviour stays unspecified; this only keeps clj-kondo clean for `:cljs`.
- Argument processing allocation: macro checks reserved names and duplicates and splits options/specs; emitter
  checks mutable field metadata and option keys, resolves protocol symbols, destructures method args, builds
  factories and the expansion form. No private `clojure.core` vars at expansion time or in generated code —
  reimplement `validate-fields`, `parse-opts+specs`, `maybe-destructured`, `build-positional-factory` with
  public functions.
- Error data: exactly the `ex-data` maps of the "Error data" table (7 paths, 5 `:type` values). Argument errors
  are host exceptions (`AssertionError`, `IllegalArgumentException`) with no `ex-data`; tests check `ex-cause`
  class, not the `CompilerException` wrapper.
- Emitter tests use the public macro and the generated type, never form comparison.
- Quoted test forms: each `macroexpand-1` or `eval` of a quoted form runs inside `(binding [*ns* (the-ns '<test-ns>)]
  ...)`. In the scry CLI `*ns*` is `user`, so without the binding an unqualified macro or var does not resolve. The
  binding keeps the acceptance 5 form (`'(defclosedrecord T [__meta])`) unchanged. Wrong-arity `->Name` calls (V-10)
  use `apply`, because clj-kondo (`:lint-as` `defrecord`) reports direct wrong-arity calls as errors.
- Verification per slice: scry in the REPL during work; Slice 1 gate with the three CLI runs; clj-kondo at the end
  of Slices 1, 4 and 5; final check with the three CLI runs (default, 1.12, 1.13), clj-kondo and cljfmt.

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
- clj-kondo zero-warning criterion depends on quoted forms, the `:cljs` branch of `core.cljc` and `:lint-as`
  config. Run `clj-kondo --lint src test` at the end of Slices 1, 4 and 5, not only in Slice 7.

## Slice order

1. Compiler rule tests (V-13) — Scope 1. Gate: all three CLI runs pass before any emitter work.
2. Public API skeleton: `ClosedRecord` protocol in `core.cljc` (D-17); `.clj-kondo/config.edn` with `:lint-as`.
3. JVM emitter in `src/closed_record/impl/jvm.clj`: argument checks, expansion form, generated type (fields,
   interfaces, methods, factories).
4. `defclosedrecord` macro and `check` in `closed-record.core`, with docstrings.
5. Tests: V-01 behaviour tables, V-10 (>20 fields), V-15 (`check`), expansion-time argument errors, `ex-data`
   table (7 paths). Record the V-01 unit-to-test mapping and the test of each `ex-data` path in one
   `implementation.md` section.
6. Cleanup: remove the smoke test; update `mementum/state.md` and `mementum/knowledge/dev-tooling.md`. Do not
   change `mementum/memories/deftype-star-hidden-fields.md` or `ramora/` without user approval.
7. Final verification: three scry CLI runs, REPL scry, clj-kondo, cljfmt. Then show the open architecture findings
   of `implementation.md` to the user together and ask.
