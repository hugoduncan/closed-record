# 012 inlinable methods

## Intent

Make `assoc` on a closed record as fast as on `defrecord` (S-03, K-05), so that V-11 passes. Generated methods that
`defrecord` keeps small must also be small in the closed type, so that the JIT can inline them.

## Problem

The task 011 acceptance run (5 forks, criterium 0.5.251) fails only on `assoc`: `P / R` 1.193 (`P` 7.93 ns, `R`
6.65 ns). In one JVM the gap was 1.44–1.64. V-11 is not done.

The cause is the size of the generated method, not the speed of the dispatch. C2 does not inline `P::assoc` (995
bytes of bytecode, "hot method too big"). It inlines `R::assoc` (228 bytes). `get` passes (`P / R` 1.049) because
`valAt` has about the same size in both types (838 and 846 bytes), so neither is inlined. Most of the size comes from
the `case` on the declared keys and from the inline throw form (Observed facts).

The same size gap is in three more methods that `defrecord` keeps small: `without` (`dissoc`), `containsKey`
(`contains?`) and `entryAt` (`find`). No bench row measures them, but they have the same cause.

## Context

- Emitter: `src/closed_record/impl/jvm.clj`. `key-case` makes a `case` on the declared keywords. `assoc`, `valAt`,
  `getLookupThunk` and `entryAt` use it. `without` and `containsKey` use a `case` with one clause for all the
  declared keywords. `closed-throw` makes the `(throw (ex-info (str ...) {...}))` form inline in `assoc` and
  `without`. It also makes the throws of `empty` and `map->Name`, which do not change.
- `closed-record.core` (`.cljc`, D-13) has the private function `class-name`. `check` uses it in its messages. On the
  JVM it gives `(.getName c)`.
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
`(defrecord R [a b c])`). The values are the code length (Scope 4): the offset of the last instruction plus 1 (the
last instruction is `areturn` or `ireturn`, 1 byte). `PrintInlining` prints the same value. The `assoc` row is the
`IPersistentMap assoc(Object, Object)` method, not the `Associative assoc` bridge method (9 bytes).

| Method | `P` bytes | `R` bytes |
|---|---|---|
| `without` | 1108 | 149 |
| `assoc` | 995 | 228 |
| `getLookupThunk` | 876 | 876 |
| `entryAt` | 863 | 40 |
| `containsKey` | 841 | 52 |
| `valAt(k, nf)` | 838 | 846 |
| `cons` | 275 | not in the top 12 |
| `equiv` | 182 | 221 |

`PrintInlining` (C2) on a hot loop of `(.assoc ^clojure.lang.IPersistentMap x :a v)`, one type for each JVM:

- `P::assoc (995 bytes)`: "failed to inline: hot method too big".
- `R::assoc (228 bytes)`: "inline (hot)".

The size of a `case` on keywords (Clojure 1.11.4):

- `case` on keywords dispatches on `Util.hash` with a `tableswitch` over the range of the hash values. If the
  range is 8192 or more, `prep-hashes` (`core.clj`) first applies a shift and a mask to the hashes. The size depends
  on the keyword hashes, not only on the number of keys. For `:a :b :c` the hashes have a range of 191 (no shift and
  no mask), so the table has 192 entries (768 bytes).
- A test `deftype` with the fields `a b c`, a dispatch on `:a :b :c` to a new instance and a throw as default:
  888 bytes with `case`, 162 bytes with `condp identical?` (code length).
- The `condp identical?` dispatch is correct for keywords, because keywords are interned. A `case` on keywords also
  compares by identity after the hash.

## Scope

In scope:

1. Change the emitter so that `assoc`, `without`, `containsKey` and `entryAt` of the closed type are small. For a type
   with three fields, each is below `FreqInlineSize` (325 bytes, code length, see Scope 4). The target is the size
   of the `defrecord` method plus the call of a throw helper. The method bodies (`kw`: each declared keyword; `fld`:
   its field):
   - `assoc`: `(condp identical? k kw (new Name ... v ... __meta __extmap) ... default)`, one clause for each
     declared field, as `defrecord` `assoc`. `default` is `(throw (key-error :closed-record/undeclared-key (class
     this) k))`. With no fields, the `condp` has only the default.
   - `without`: an `identical?` test of `k` against each declared keyword. A match gives
     `(throw (key-error :closed-record/declared-key-dissoc (class this) k))`. No match gives `this`. With no fields,
     `this`.
   - `containsKey`: `(not (identical? this (.valAt this k this)))`, as `defrecord`.
   - `entryAt`: `(let [v (.valAt this k this)] (when-not (identical? this v) (clojure.lang.MapEntry/create k v)))`,
     as `defrecord`.
   - `key-error` is the helper of Scope 2. The generated code calls it through its var.
   - The bodies of these four methods have no `case`. `valAt` keeps its `case` (Out of scope). Because the closed
     `valAt` reads only the declared fields, `containsKey` and `entryAt` stay "declared keys only" (I-02).
2. The throw paths of `assoc` and `without` call one shared helper outside the generated class. The `ex-info` message
   and data do not change.
   - Location: a private function `key-error` in `closed-record.core`. It is not a public var (D-20, principle 9:
     two public vars). The generated code calls it through its var: `(#'closed-record.core/key-error t record k)`.
     The `var` form permits a private var from another namespace. The emitter emits the var name only as a symbol,
     so `impl.*` does not require `core` (as for the protocol, D-17).
   - Contract: `(key-error t record k)` returns an `ex-info`. It does not throw. The call site has the `throw`.
     - `t`: `:closed-record/undeclared-key` or `:closed-record/declared-key-dissoc`. The emitter gives no other
       value. A `case` on `t` selects the message.
     - `record`: the class of the instance (`(class this)`). `k`: the key.
     - Message: `(str "Undeclared key " k " for closed record " (class-name record))` for
       `:closed-record/undeclared-key`, and `(str "Cannot dissoc declared key " k " from closed record "
       (class-name record))` for `:closed-record/declared-key-dissoc`. `class-name` is the existing private
       function of `core`. On the JVM, `(.getName record)` is equal to the `cname-str` of the emitter (observed:
       also for a namespace with `-`, and type names with `_`, `?`, `-` and `!`). So the text is the same as now.
     - Data: `{:type t, :record record, :key k}` (D-03).
     - The helper is portable: `ex-info`, `str` and `class-name` exist on all hosts. It needs no new reader
       conditional.
   - Lint (acceptance 5): `core` does not call `key-error`, so clj-kondo reports "Unused private var" for it. A
     test call through the var does not remove the warning (observed). The `defn-` of `key-error` has the form
     `#_{:clj-kondo/ignore [:unused-private-var]}` before it. Thus only this linter is off, and only for this var.
     `.clj-kondo/config.edn` does not change. D-21 and `embodiment.md` do not name the lint exception.
   - `closed-throw` stays for `empty` and `map->Name`.
3. Behaviour does not change. The existing test suite is the check.
4. Evidence in `implementation.md`: the bytecode size table above for the changed emitter, and the
   `PrintInlining` result for `P::assoc`. Measurement:
   - Clojure 1.11.4 (the default alias). Record the `java -version` output with the evidence.
   - Size: the code length of the method, as `PrintInlining` prints it. In `javap -c` it is the offset of the last
     instruction plus the length of that instruction. For `assoc`, the `IPersistentMap assoc(Object, Object)`
     method.
   - Classes: AOT output of a scratch namespace (outside the repository) with `(defclosedrecord P [a b c])` and
     `(defrecord R [a b c])`.
   - `PrintInlining`: `-XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=quiet
     -XX:CompileCommand=PrintInlining,<loop-class>::invokeStatic`, one JVM for each type. The loop is in a `defn`
     and calls `(.assoc ^clojure.lang.IPersistentMap r :a (num i))`. The evidence is the C2 line for `P::assoc`. A
     loop of `(assoc x :a i)` does not show the size: `RT.assoc` calls `Associative.assoc` at a call site that also
     sees other map types, and C2 gives "virtual call".
   - `implementation.md` records the exact commands and the scratch namespace, so that a reviewer can repeat them.
5. Run the bench acceptance run (task 011 rules).
   - Before it, shorter runs are permitted: one child fork (`clojure -M:bench 0 <result-file>`, which writes EDN
     and prints no table) or a criterium run in a REPL. They do not count for V-11.
   - Do a full run (`clojure -M:bench`, 5 forks) only after the emitter change is final: the tests pass and the
     size table and the `PrintInlining` result are recorded. The first full run is the acceptance run. No later
     run replaces it, and the emitter does not change after it in this task.
   - The bench result does not keep the task open. If a row fails (`assoc`, or a row that passed in task 011), the
     task completes when the acceptance criteria are met: `state.md` records the ratio of each failed row, and V-11
     stays not done. A follow-up is a new task.
6. Documents:
   - A new decision `D-21 Method size` in `ramora/defclosedrecord/decisions/type-structure.md`, with the record shape
     of `decisions.md`:
     - Subject: the size of the generated `assoc`, `without`, `containsKey` and `entryAt`, and their throw paths.
     - Choice: each method has the `defrecord` shape (Scope 1), within the size of the `defrecord` method plus the
       helper call. The throw paths of `assoc` and `without` call the private helper
       `closed-record.core/key-error` through its var (Scope 2). Generated code → this helper is a permitted
       dependency.
     - Rejected: `case` in these methods (the `tableswitch` size depends on the keyword hashes). The inline throw
       form (cold path, but it adds bytecode to the method). A public helper in `core` (D-20, principle 9). A
       helper in `impl.*` (generated code → `impl.*` is forbidden; the emitter is for expansion time only).
     - Rationale: K-05, K-09, K-10. C2 does not inline a hot method above `FreqInlineSize` (325 bytes, K-09). A
       `case` on keywords is large (K-10). Observed: `P::assoc` (995 bytes) is not inlined, `R::assoc` (228 bytes)
       is inlined. The helper location: the earlier boundaries permit no location for it. A private var in `core`
       is the smallest change: the generated code already depends on `core` (D-17), and the public API does not
       change.
     - Assumptions: the generated code can refer to `closed-record.core`, because the macro is in that namespace
       (as D-17). The C2 inline limits stay near their current defaults (R-05).
     - Qualities: + performance, + `defrecord` parity. − The generated code depends on a private var of `core`.
       AOT classes of user code refer to the var by name.
     - Scope: JVM emitter. The helper in `core` is portable.
     - Commitment: medium, as D-17. The helper is an internal mechanism: it is not in the public API of D-20, and
       only the generated code calls it. A change of the helper name or arguments changes the JVM emitter and the
       private helper in `core` together, in one release. User classes that were AOT-compiled with an earlier
       version refer to the helper by name, as they refer to the D-17 protocol interface. Before the first release,
       K-03 applies. After it, a change adds a helper with a new name and keeps the old helper, so that the earlier
       AOT classes still work. The message and the data of D-03 do not change.
   - `ramora/defclosedrecord/decisions.md`: add `D-21 method size` to the "Type structure" index entry.
   - `ramora/defclosedrecord/decisions/closed-key-set.md`, D-02 Qualities: "The check costs nothing on the
     declared-key path, because only the `case` default branch throws." becomes "The check costs nothing on a
     permitted path: the key dispatch that selects the result is the check (on the JVM, the `identical?` tests of
     `defrecord` `assoc`, D-21), and only the rejected path calls the throw helper." The claim stays true, so D-12
     (which cites it) does not change. The D-02 choice and commitment do not change.
   - `ramora/defclosedrecord.md`:
     - "D-01 to D-20" becomes "D-01 to D-21". "constraints K-01 to K-08" becomes "constraints K-01 to K-10".
     - The `observed` paragraph: K-09 and K-10 come from the task 012 measurements (Clojure 1.11.4, OpenJDK
       25.0.2), not from tests. V-13 does not keep them (no size test in the suite). V-11 is their signal (R-05).
   - `ramora/defclosedrecord/context.md`, Constraints:
     - K-09 C2 inline limit (observed, OpenJDK 25.0.2, `PrintInlining`). C2 does not inline a hot method with a
       code length above `FreqInlineSize` (default 325 bytes). It reports "hot method too big".
     - K-10 Keyword `case` size (observed, Clojure 1.11.4; `prep-hashes` in `core.clj`). A `case` on keywords
       dispatches on `Util.hash` with a `tableswitch` over the range of the hashes (after a shift and a mask if the
       range is 8192 or more; a `lookupswitch` if no mask makes the hashes distinct). Its size depends on the
       hashes, not only on the key count (`:a :b :c`: 192 entries).
     - The Evidence table does not change: its rows are Clojure source facts that V-13 keeps as tests.
   - `ramora/defclosedrecord/qualities.md`:
     - Trade-offs: the out-of-line throw helper (D-21) keeps the generated methods small enough for C2 inlining
       (K-09), but the generated code depends on a private var of `core`, and AOT-compiled user classes refer to it
       by name. Mitigation: the D-21 commitment (a new helper name after the first release).
     - Risks: R-05 A future JDK changes the C2 inline limits (K-09), or a JVM uses a different JIT compiler.
       Likelihood: low. Effect: a generated method is not inlined, and a gap to `defrecord` can return. Signal:
       V-11.
   - `ramora/defclosedrecord/principles.md`:
     - 6 Explicit boundaries: add D-21 (one new permitted dependency, generated code → `key-error`).
     - 28 Fit to context: "the compiler rules K-06 to K-08" becomes "the compiler rules K-06 to K-10". Add D-21.
   - `ramora/defclosedrecord/embodiment.md`:
     - Artifacts, the `core.cljc` row: add the internal run-time helper `key-error`, which the generated code calls
       (D-21).
     - Forbidden dependencies, `impl.*` → `core`: "It emits the protocol name only as a symbol" becomes "It emits
       the protocol name and the `key-error` var name only as symbols".
     - Permitted dependencies: generated code → the private helper `closed-record.core/key-error` (through its var,
       D-21).
     - Boundaries, Internal: add `closed-record.core/key-error`.
     - Method behaviour: the `assoc` and `without` rows name the `identical?` dispatch and the helper.
       `containsKey` and `entryAt` call `valAt` with `this` as the not-found value.
   - `mementum/state.md`:
     - The V-11 status from the acceptance run. V-11 is done only if all six timed rows pass in the acceptance run
       and the field declaration test (`field_value_test.clj`) passes (S-03).
     - Replace the stale cause statement ("The probable cause of the `assoc` gap is the emitter dispatch (`case`
       against `condp identical?`); no measurement isolates it. A new task.") with the measured cause: the method
       size stopped C2 inlining (`P::assoc` 995 bytes, `FreqInlineSize` 325). The `case` `tableswitch` and the
       inline throw form made the size.
   - `mementum/knowledge/benchmarks.md`:
     - Add method size and inlining as a cause of a gap, and how to find it (`PrintInlining`, `javap` on AOT
       classes).
     - Section "criterium 0.5.251-ALPHA": "The `assoc` gap is a real emitter cost that the folding hid." stays, with
       the measured cause added: the method size stopped C2 inlining (task 012). The task 011 result (`assoc`
       `P / R` 1.193, a fail) stays as history. "This is the current gap, the start point for the `assoc` task."
       is replaced with the task 012 acceptance-run result for `assoc`: the ratio, and pass or fail.

## Out of scope

- `valAt` and `getLookupThunk`. They have the same size as in `defrecord`.
- `cons`, `equiv` and other methods. No measured gap.
- New bench rows for `dissoc`, `contains?` and `find`.
- A test of bytecode size in the suite.
- Types with many fields (V-10). The size of `assoc` grows with the field count, as in `defrecord`. Parity, not a
  fixed limit, is the goal.
- Other hosts.

## Decisions

- Fix the size. `valAt` uses a `case` in both types and `get` passes. So a `case` is not slow; the method size stops
  inlining. But a `case` on keywords is large (a `tableswitch` over the hash range), so `assoc` and `without` use
  `identical?` tests, as `defrecord` `assoc` does. The size, not the speed of the dispatch, is the rule.
- Follow the `defrecord` shape where it is smaller. This is the D-01 basis (`defrecord` parity) and it keeps the
  design simple.
- Out-of-line throw. The throw path is cold. In the method body it adds bytecode (string build and map literal) on
  every call site of the method, but it never runs on a permitted operation. The helper builds the message and the
  data, so the call site has only the call and the `throw`.
- A private helper in `core`, called through its var. The current boundaries permit no location: generated code →
  `impl.*` is forbidden, and a public var breaks D-20 and principle 9. A private var in `core` adds one permitted
  dependency, to the namespace that the generated code already uses (D-17). A var call on the cold path has no
  cost on permitted paths (D-02, D-12).
- Include `without`, `containsKey` and `entryAt`. The cause is the same, and the change is in the same emitter
  functions. Leaving them would keep a known gap in user-visible operations.
- No size test in the suite. A bytecode size depends on the Clojure version, and a test needs AOT output.
  `implementation.md` records the sizes, and the bench is the check of the effect.

## Acceptance criteria

1. For `(defclosedrecord P [a b c])`, `assoc`, `without`, `containsKey` and `entryAt` each have a code length below
   325 bytes (Scope 4), and `implementation.md` has the size table for `P` and `R`.
2. `PrintInlining` (Scope 4) shows the C2 line `P::assoc (<n> bytes) inline (hot)`. `implementation.md` has the
   line.
3. `clojure -M:test -m scry.cli` on the default alias, `:clj-1.12` and `:clj-1.13`: zero failures and errors. No test
   changes, except new tests for a new helper.
4. `implementation.md` has the complete output and exit code of the bench acceptance run (Scope 5). V-11 is done
   only if all six timed rows pass and the field declaration test passes (S-03). If a timed row fails, `state.md`
   records its ratio and V-11 stays not done.
5. `clj-kondo --lint src test dev`: no errors and no warnings. `cljfmt check src test dev` passes.
6. The documents of Scope 6 are updated: D-21, `decisions.md`, D-02, `ramora/defclosedrecord.md`, `context.md`,
   `qualities.md`, `principles.md`, `embodiment.md`, `state.md` and `benchmarks.md`.
