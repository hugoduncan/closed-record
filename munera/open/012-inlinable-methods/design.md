# 012 inlinable methods

## Intent

Make `assoc` on a closed record as fast as on `defrecord` (S-03, K-05), so that V-11 passes. Generated methods that
`defrecord` keeps small must also be small in the closed type, so that the JIT can inline them.

## Problem

The task 011 acceptance run (5 forks, criterium 0.5.251) fails only on `assoc`: `P / R` 1.193 (`P` 7.93 ns, `R`
6.65 ns). In one JVM the gap was 1.44–1.64. V-11 is not done.

The cause is the size of the generated method, not the dispatch. C2 does not inline `P::assoc` (994 bytes of
bytecode, "hot method too big"). It inlines `R::assoc` (227 bytes). `get` passes (`P / R` 1.049) because `valAt` has
about the same size in both types (837 and 845 bytes), so neither is inlined.

The same size gap is in three more methods that `defrecord` keeps small: `without` (`dissoc`), `containsKey`
(`contains?`) and `entryAt` (`find`). No bench row measures them, but they have the same cause.

## Context

- Emitter: `src/closed_record/impl/jvm.clj`. `key-case` makes a `case` on the declared keywords. `assoc`,
  `without`, `valAt`, `containsKey` and `entryAt` use it. `closed-throw` makes the `(throw (ex-info (str ...) {...}))`
  form inline in `assoc` and `without`.
- `defrecord` (Clojure 1.11.4, `emit-defrecord`):
  - `assoc`: `(condp identical? k :a (new R v b c __meta __extmap) ... (new R a b c __meta (assoc __extmap k v)))`.
  - `without`, `containsKey`, `entryAt`: small bodies, with no `case` on the declared keys.
  - `valAt`: a `case` on the declared keys, as in the closed type.
- Behaviour that must not change: D-03 (`ex-info` `:type` and data keys, public API), I-01 to I-09, the behaviour
  tables in `purpose.md`. The tests check the `ex-data`. No test checks the message text.
- Bench: `clojure -M:bench` (task 011: 5 forks, six operations, exit 0 if all pass). Knowledge:
  `mementum/knowledge/benchmarks.md`.
- HotSpot C2 defaults: `FreqInlineSize` 325 bytes (hot call sites), `MaxInlineSize` 35 bytes.

### Observed facts (2026-10-23, Clojure 1.11.4, OpenJDK 25.0.2, Apple M5 Max (arm64), commit `66fd109`)

Bytecode size of the generated methods (`javap -c` of AOT classes, `(defclosedrecord P [a b c])`,
`(defrecord R [a b c])`):

| Method | `P` bytes | `R` bytes |
|---|---|---|
| `without` | 1107 | 148 |
| `assoc` | 994 | 227 |
| `getLookupThunk` | 875 | 875 |
| `entryAt` | 862 | 39 |
| `containsKey` | 840 | 51 |
| `valAt(k, nf)` | 837 | 845 |
| `cons` | 274 | not in the top 12 |
| `equiv` | 181 | 220 |

`-XX:+PrintInlining` on a hot loop of `(assoc x :a i)`:

- `user.P::assoc (995 bytes)`: "failed to inline: hot method too big".
- `user.R::assoc (228 bytes)`: "inline (hot)".

## Scope

In scope:

1. Change the emitter so that `assoc`, `without`, `containsKey` and `entryAt` of the closed type are small. For a type
   with three fields, each is below `FreqInlineSize` (325 bytes). The target is the size of the `defrecord` method
   plus the call of a throw helper.
2. The throw paths of `assoc` and `without` call one shared helper outside the generated class. The `ex-info` message
   and data do not change.
3. Behaviour does not change. The existing test suite is the check.
4. Evidence in `implementation.md`: the bytecode size table above for the changed emitter, and the
   `-XX:+PrintInlining` result for `P::assoc`.
5. Run the bench acceptance run (task 011 rules: the first full run is the acceptance run, with complete output and
   exit code).
6. Documents:
   - A new decision in `ramora/defclosedrecord/decisions/type-structure.md`: generated methods stay within the size of
     the `defrecord` methods, and throw paths go out of line. Basis: K-05, the observed facts.
   - `mementum/state.md`: the V-11 status from the acceptance run. V-11 is done only if all six operations pass.
   - `mementum/knowledge/benchmarks.md`: method size and inlining as a cause of a gap, and how to find it
     (`PrintInlining`, `javap` on AOT classes).

## Out of scope

- `valAt` and `getLookupThunk`. They have the same size as in `defrecord`.
- `cons`, `equiv` and other methods. No measured gap.
- New bench rows for `dissoc`, `contains?` and `find`.
- A test of bytecode size in the suite.
- Types with many fields (V-10). The size of `assoc` grows with the field count, as in `defrecord`. Parity, not a
  fixed limit, is the goal.
- Other hosts.

## Decisions

- Fix the size, not the dispatch. `valAt` uses a `case` in both types and `get` passes. So the `case` is not slow;
  the method size stops inlining.
- Follow the `defrecord` shape where it is smaller. This is the D-01 basis (`defrecord` parity) and it keeps the
  design simple.
- Out-of-line throw. The throw path is cold. In the method body it adds bytecode (string build and map literal) on
  every call site of the method, but it never runs on a permitted operation.
- Include `without`, `containsKey` and `entryAt`. The cause is the same, and the change is in the same emitter
  functions. Leaving them would keep a known gap in user-visible operations.
- No size test in the suite. A bytecode size depends on the Clojure version, and a test needs AOT output.
  `implementation.md` records the sizes, and the bench is the check of the effect.

## Acceptance criteria

1. For `(defclosedrecord P [a b c])`, `assoc`, `without`, `containsKey` and `entryAt` are each below 325 bytes, and
   `implementation.md` has the size table for `P` and `R`.
2. `-XX:+PrintInlining` shows `P::assoc` as inlined in a hot loop. `implementation.md` has the line.
3. `clojure -M:test -m scry.cli` on the default alias, `:clj-1.12` and `:clj-1.13`: zero failures and errors. No test
   changes, except new tests for a new helper.
4. `implementation.md` has the complete output and exit code of the bench acceptance run. If `assoc` still fails,
   `state.md` records the ratio and V-11 stays not done.
5. `clj-kondo --lint src test dev`: no errors and no warnings. `cljfmt check src test dev` passes.
6. The decision, `state.md` and `benchmarks.md` are updated as in Scope 6.
