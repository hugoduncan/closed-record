# Steps

## Slice 1: namespace and helper

- [x] Create `test/closed_record/equality_test.clj` with ns `closed-record.equality-test`, requiring
      `clojure.test` and `closed-record.core`.
- [x] Define `(defclosedrecord E [a b])` and `(defrecord DE [a b])`.
- [x] Add a differential helper (`is-diff` style macro or function): calls `f` with the closed factory and with
      the `defrecord` factory, each returning a vector of booleans; asserts the vectors are `=`; failure shows
      both vectors.
- [x] Run `clojure -M:test -m scry.cli` to confirm the namespace loads.

## Slice 2: field value pairs (Scope 3)

- [x] Add a function that, given a factory and a pair `[a1 a2]`, builds `x = (f a1 2)` and `y = (f a2 2)` and
      returns `[(= x y) (= (hash x) (hash y)) (.equals x y) (= (.hashCode x) (.hashCode y))]`.
- [x] Differential test for pair `1`, `1N`.
- [x] Differential test for pair `(int 1)`, `1`.
- [x] Differential test for pair `0.0`, `-0.0`.
- [x] Differential test for pair `1`, `1.0`.
- [x] Run the tests; confirm pass.

## Slice 3: plain maps (Scope 4)

With `r = (f 1 2)` and `m = {:a 1 :b 2}`:

- [x] Differential test: `(= r m)` and `(= m r)`.
- [x] Differential test: `(.equals r m)` and `(.equals m r)`.
- [x] Differential test: `(.equals r (java.util.HashMap. m))` and the reverse.
- [x] Differential test: `.hashCode` of `r` equal to `.hashCode` of `m`.
- [x] Differential test: `hash` of `r` compared with `hash` of `m` (within each type, not across types).
- [x] Differential test: `(contains? #{m} r)`.
- [x] Differential test: `(.contains (java.util.HashSet. [m]) r)`.
- [x] Run the tests; confirm pass.

## Slice 4: metadata (Scope 5)

Each case returns the four results of the first-table function. Build both values before calling `hash` or
`.hashCode`.

- [x] Differential test: `r = (f 1 2)` vs `(with-meta r {:m 1})`.
- [x] Differential test: `(with-meta (f 1 2) {:m 1})` vs `(with-meta (f 1 2) {:m 2})` from two separate factory
      calls.
- [x] Run the tests; confirm pass.

## Slice 5: cross type (Scope 6)

- [x] Test with fixed expected values, closed value first only: `(= (->E 1 2) (->DE 1 2))` is false,
      `(.equals (->E 1 2) (->DE 1 2))` is true, `.hashCode` of both are equal.
- [x] Run the tests; confirm pass.

## Slice 6: matrix, lint, format

- [x] `clojure -M:test -m scry.cli` exits 0, zero failures, zero errors.
- [x] `clojure -M:test:clj-1.12 -m scry.cli` exits 0, zero failures, zero errors.
- [x] `clojure -M:test:clj-1.13 -m scry.cli` exits 0, zero failures, zero errors.
- [x] `clj-kondo --lint src test` reports no errors and no warnings.
- [x] `cljfmt check src test` passes.
- [x] Commit the test namespace.

## Slice 7: mutation checks (Acceptance 3)

Mutations M1–M12, rules and the expected coverage are in plan.md, "Mutation checks". For each mutation: change
`src/closed_record/impl/jvm.clj` from an unchanged `src`, run `clojure -M:test -m scry.cli`, confirm that the
`equality-test` assertions of the listed items fail, revert, and record in `implementation.md` the change and the
items that failed. A change that fails at namespace load does not count.

- [x] `equiv`: M1 (Scope 3 rows `1N`, `(int 1)`, `-0.0`), M2 (Scope 3 row `1.0`), M8 (Scope 5 both cases).
- [x] `equals`: M3 (Scope 3 rows `1N`, `(int 1)`, `-0.0`; Scope 4 `.equals`, `HashMap`, `HashSet`; Scope 6),
      M9 (Scope 5 both cases).
- [x] `hasheq`: M4 (Scope 3 row `-0.0`), M5 (Scope 4 `hash`), M10 (Scope 5 both cases).
- [x] `hashCode`: M6 (Scope 3 row `-0.0`; Scope 4 `.hashCode`, `HashSet`; Scope 6), M11 (Scope 5 both cases).
- [x] `base-interfaces`: M7 (Scope 4 `=`).
- [x] `hasheq` and `equiv`: M12 (Scope 4 `contains?`). Also run the `equiv` part alone and confirm that Scope 4
      `contains?` does not fail (the `hasheq` part alone is M5). Record both parts and both results.
- [x] Compare the results with the plan.md coverage list. For each item that no mutation made fail, find a
      covering mutation or record the reason that no `src` change breaks it.
- [x] Commit `implementation.md` entries.

## Slice 8: state

- [x] In `mementum/state.md`, move V-12 from "Not done" to "done" in the verification-plan line.
- [x] Commit.

## Test review: test-shaper (2026-10-19)

- [x] Meaningful failures: label the differential results (`test/closed_record/equality_test.clj`). A failure now
      shows only `(not (= [true true false true] [true true true true]))`, and the `expected:` form shows a gensym
      (`f__391__auto__`). The reader must look in `four` or the `with-r` lambda to find the operation for each
      position. Make `four` and the Scope 4 functions return maps keyed by operation (for example
      `{:= .. :hash= .. :equals .. :hashCode= ..}`, and `{:r-first .. :m-first ..}` for the Scope 4 pairs), so a
      failure names the operation that differs. Keep the differential check `(= (f ->E) (f ->DE))`. Re-run the
      matrix, lint and format.
      Done: `four` and Scope 4 functions return maps keyed by operation; `is-diff` binds `closed` and `record`,
      so `expected:` shows `(= closed record)`. Matrix 254 pass; lint, format clean.

## Code review: code-shaper (2026-10-19)

- [x] Local comprehension: remove the references to task design sections from
      `test/closed_record/equality_test.clj`. The ns docstring ("Scope 3, 4 and 5"), the `four` docstring ("the
      first Context table") and the `deftest` comments (`;; Scope 3` … `;; Scope 6`) point to
      `munera/open/005-equality-hash-tests/design.md`, but the source does not name the task. After the task closes,
      a reader cannot resolve them. Describe each item in the source: `four` returns `=`, `hash` equality,
      `.equals` and `.hashCode` equality of `x` and `y`; the comments name the case (field value pairs, plain map,
      metadata, cross type). Keep the durable references (V-12, I-05, D-07). Re-run the matrix, lint and format.
      Done: docstrings and comments describe each case; no "Scope" or "Context" references remain. Matrix 254
      pass; lint, format clean.

## Code review: code-shaper, pass 2 (2026-10-19)

- [x] Naming and local comprehension of `four` (`test/closed_record/equality_test.clj`). The name gives the count
      of results, not what they are. Rename it to a name that tells the content (for example `equality-results`),
      and update `pair`, `metadata-test` and the docstrings that use it. In its docstring, remove "Both values must
      exist before this call, so that no hash is cached before `with-meta`": arguments always exist before a call,
      and the real rule (no `hash` call on a value before `with-meta`) is a rule for the caller. The
      `metadata-test` comment already states it. Re-run the matrix, lint and format.
      Done: renamed to `equality-results`; docstring keeps only the operations. Matrix 254 pass; lint, format
      clean.

## Code review: code-shaper, pass 3 (2026-10-19)

- [x] Naming of the var `m` (`test/closed_record/equality_test.clj`, `(def ^:private m {:a 1 :b 2})`). A
      one-letter top-level var is a free name in each `plain-map-test` lambda; the reader must find its `def`. In
      `metadata-test`, `{:m 1}` uses the same letter for a different thing. Rename it to a name that tells the
      content (for example `plain-map`), as with `equality-results`. Keep the `:m-first` keys or rename them to
      agree. Re-run the matrix, lint and format.
      Done: renamed to `plain-map`; keys `:m-first` → `:map-first`. Matrix 254 pass; lint, format clean.
