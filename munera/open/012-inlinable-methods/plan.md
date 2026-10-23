# Plan

## Approach

Change the JVM emitter so that `assoc`, `without`, `containsKey` and `entryAt` have the
`defrecord` shape and size. Move the throw paths of `assoc` and `without` out of line into
one private helper in `core`.

Key decisions (all fixed in `design.md`):

- Dispatch: `condp identical?` for `assoc`, `identical?` tests for `without`. No `case`
  in the four methods. `valAt` keeps its `case` (out of scope).
- `containsKey` and `entryAt` call `(.valAt this k this)` with `this` as the not-found
  sentinel, as `defrecord` does. The closed `valAt` reads only declared fields, so I-02
  holds.
- Helper: `(key-error t record k)`, a private `defn-` in `closed-record.core` (`.cljc`),
  after `class-name` (~line 63). It returns the `ex-info`; the call site has the `throw`.
  Message and data are unchanged (D-03). The emitter emits `#'closed-record.core/key-error`
  only as a symbol (as D-17), so `impl.*` does not require `core`.
- Lint: `#_{:clj-kondo/ignore [:unused-private-var]}` before the `defn-`. No change to
  `.clj-kondo/config.edn`.
- `closed-throw` stays for `empty` and `map->Name`.
- Evidence first, acceptance run last: the first full bench run (5 forks) is the
  acceptance run, after the emitter is final and the sizes are recorded. Shorter runs
  (one fork, REPL criterium) are permitted before it and do not count for V-11.
- Documents: new D-21 in `decisions/type-structure.md` plus the Scope 6 edits
  (K-09, K-10, R-05, D-02, embodiment, principles, state, benchmarks).

## Slice order

1. Helper: `key-error` in `core.cljc`, with tests.
2. Emitter: the four method bodies in `impl/jvm.clj`.
3. Evidence: size table and `PrintInlining` in `implementation.md` (Scope 4 commands).
4. Bench acceptance run (Scope 5), record output and V-11 status.
5. Documents (Scope 6).
6. Final checks: tests on three Clojure versions, clj-kondo, cljfmt.

## Risks

- A method is still at or above 325 bytes (code length). The design prototype gives 214
  bytes for the `assoc` shape with three fields, so the margin is wide. If not, the
  acceptance 1 target rules; re-shape before the acceptance run.
- The acceptance run can fail on `assoc` or on a row that passed in task 011. The task
  still completes: `state.md` records the ratio, V-11 stays not done, a follow-up is a
  new task (Scope 5).
- Measurement pitfalls (recorded in `implementation.md`): hint
  `^clojure.lang.IPersistentMap` and box the value in the hot loop, or `.assoc` reflects;
  use the per-method `CompileCommand`, not global `PrintInlining`; read the
  `IPersistentMap assoc` row, not the 9-byte bridge.
- The emitter must not change after the acceptance run in this task. Order the steps so
  that lint, format and document edits that touch `src` happen before the run, or do not
  touch `src` after it (document edits do not).
