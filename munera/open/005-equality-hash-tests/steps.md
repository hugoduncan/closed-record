# Steps

## Slice 1: namespace and helper

- [ ] Create `test/closed_record/equality_test.clj` with ns `closed-record.equality-test`, requiring
      `clojure.test` and `closed-record.core`.
- [ ] Define `(defclosedrecord E [a b])` and `(defrecord DE [a b])`.
- [ ] Add a differential helper (`is-diff` style macro or function): calls `f` with the closed factory and with
      the `defrecord` factory, each returning a vector of booleans; asserts the vectors are `=`; failure shows
      both vectors.
- [ ] Run `clojure -M:test -m scry.cli` to confirm the namespace loads.

## Slice 2: field value pairs (Scope 3)

- [ ] Add a function that, given a factory and a pair `[a1 a2]`, builds `x = (f a1 2)` and `y = (f a2 2)` and
      returns `[(= x y) (= (hash x) (hash y)) (.equals x y) (= (.hashCode x) (.hashCode y))]`.
- [ ] Differential test for pair `1`, `1N`.
- [ ] Differential test for pair `(int 1)`, `1`.
- [ ] Differential test for pair `0.0`, `-0.0`.
- [ ] Differential test for pair `1`, `1.0`.
- [ ] Run the tests; confirm pass.

## Slice 3: plain maps (Scope 4)

With `r = (f 1 2)` and `m = {:a 1 :b 2}`:

- [ ] Differential test: `(= r m)` and `(= m r)`.
- [ ] Differential test: `(.equals r m)` and `(.equals m r)`.
- [ ] Differential test: `(.equals r (java.util.HashMap. m))` and the reverse.
- [ ] Differential test: `.hashCode` of `r` equal to `.hashCode` of `m`.
- [ ] Differential test: `hash` of `r` compared with `hash` of `m` (within each type, not across types).
- [ ] Differential test: `(contains? #{m} r)`.
- [ ] Differential test: `(.contains (java.util.HashSet. [m]) r)`.
- [ ] Run the tests; confirm pass.

## Slice 4: metadata (Scope 5)

Each case returns the four results of the first-table function. Build both values before calling `hash` or
`.hashCode`.

- [ ] Differential test: `r = (f 1 2)` vs `(with-meta r {:m 1})`.
- [ ] Differential test: `(with-meta (f 1 2) {:m 1})` vs `(with-meta (f 1 2) {:m 2})` from two separate factory
      calls.
- [ ] Run the tests; confirm pass.

## Slice 5: cross type (Scope 6)

- [ ] Test with fixed expected values, closed value first only: `(= (->E 1 2) (->DE 1 2))` is false,
      `(.equals (->E 1 2) (->DE 1 2))` is true, `.hashCode` of both are equal.
- [ ] Run the tests; confirm pass.

## Slice 6: matrix, lint, format

- [ ] `clojure -M:test -m scry.cli` exits 0, zero failures, zero errors.
- [ ] `clojure -M:test:clj-1.12 -m scry.cli` exits 0, zero failures, zero errors.
- [ ] `clojure -M:test:clj-1.13 -m scry.cli` exits 0, zero failures, zero errors.
- [ ] `clj-kondo --lint src test` reports no errors and no warnings.
- [ ] `cljfmt check src test` passes.
- [ ] Commit the test namespace.

## Slice 7: mutation checks (Acceptance 3)

Mutations M1–M12, rules and the expected coverage are in plan.md, "Mutation checks". For each mutation: change
`src/closed_record/impl/jvm.clj` from an unchanged `src`, run `clojure -M:test -m scry.cli`, confirm that the
`equality-test` assertions of the listed items fail, revert, and record in `implementation.md` the change and the
items that failed. A change that fails at namespace load does not count.

- [ ] `equiv`: M1 (Scope 3 rows `1N`, `(int 1)`, `-0.0`), M2 (Scope 3 row `1.0`), M8 (Scope 5 both cases).
- [ ] `equals`: M3 (Scope 3 rows `1N`, `(int 1)`, `-0.0`; Scope 4 `.equals`, `HashMap`, `HashSet`; Scope 6),
      M9 (Scope 5 both cases).
- [ ] `hasheq`: M4 (Scope 3 row `-0.0`), M5 (Scope 4 `hash`), M10 (Scope 5 both cases).
- [ ] `hashCode`: M6 (Scope 3 row `-0.0`; Scope 4 `.hashCode`, `HashSet`; Scope 6), M11 (Scope 5 both cases).
- [ ] `base-interfaces`: M7 (Scope 4 `=`).
- [ ] `hasheq` and `equiv`: M12 (Scope 4 `contains?`). Also run the `equiv` part alone and confirm that Scope 4
      `contains?` does not fail (the `hasheq` part alone is M5). Record both parts and both results.
- [ ] Compare the results with the plan.md coverage list. For each item that no mutation made fail, find a
      covering mutation or record the reason that no `src` change breaks it.
- [ ] Commit `implementation.md` entries.

## Slice 8: state

- [ ] In `mementum/state.md`, move V-12 from "Not done" to "done" in the verification-plan line.
- [ ] Commit.
