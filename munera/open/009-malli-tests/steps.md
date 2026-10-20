# Steps

## Slice 1: dependency

- [ ] Add `metosin/malli {:mvn/version "0.20.3"}` to `:extra-deps` of the `:test` alias in `deps.edn`; confirm no other dependency changes.
- [ ] Verify the dep resolves: `clojure -M:test -e "(require 'malli.core)"` exits 0.

## Slice 2: tests

- [ ] Read the V-01 tests to copy the full-`ex-data` comparison style (`:type`, `:record`, `:key`).
- [ ] Create `test/closed_record/malli_test.clj` with ns requires (`malli.core`, `malli.error`, `malli.transform`, scry/test ns per project conventions) and `(defclosedrecord P [a b c])`.
- [ ] Add `deftest` validation: open and closed `[:map ...]` schemas of declared keys validate true; a schema with an undeclared key (`[:map [:a :int] [:d :int]]`) and a closed schema with fewer keys (`[:map {:closed true} [:a :int]]`) validate false.
- [ ] Add `deftest` explanation: `m/explain` nil for a valid record; `me/humanize` of `(m/explain s (->P "x" nil 3))` `=` `{:a ["should be an integer"]}`; `me/humanize` of `(m/explain [:map [:a :int] [:d :int]] p)` `=` `{:d ["missing required key"]}`.
- [ ] Add `deftest` nil fields: nil field valid for `[:maybe :int]`, invalid for `:int`.
- [ ] Add `deftest` permitted transformers: `string-transformer` decode of `(->P "1" nil "3")` gives a `P` with `{:a 1 :b nil :c 3}`; `default-value-transformer` with `[:map [:b {:default 5} :int]]` gives a `P` with `{:a 1 :b 5 :c 3}`; `json-transformer` encode and `m/coerce` return `p`.
- [ ] Add `deftest` throwing transformers, comparing full `ex-data`: default for undeclared key throws `:closed-record/undeclared-key` with `:key :d`; `strip-extra-keys-transformer` with `[:map [:a :int]]` throws `:closed-record/declared-key-dissoc` with `:key :b`; `key-transformer` decode throws `:closed-record/empty`.
- [ ] Run `clojure -M:test -m scry.cli` (no extra alias): exit 0, zero failures, zero errors.
- [ ] Run `clojure -M:test:clj-1.12 -m scry.cli`: exit 0, zero failures, zero errors.
- [ ] Run `clojure -M:test:clj-1.13 -m scry.cli`: exit 0, zero failures, zero errors.
- [ ] Run `clj-kondo --lint src test`: no errors, no warnings.
- [ ] Run `cljfmt check src test`: passes (fix with `cljfmt fix` if needed).
- [ ] Commit slice 1+2.

## Slice 3: README

- [ ] Add a `malli` section to `README.md`: map schemas validate a closed record; name the three throwing cases (default for an undeclared key, `strip-extra-keys-transformer` with a schema missing declared keys, `key-transformer`); give the fix (decode `(into {} r)`, keep the result a plain map); state `map->Name` behavior per D-08 (undeclared key throws `:closed-record/extra-keys` with keys in `ex-data`; missing declared key becomes nil, so `map->Name` after `strip-extra-keys-transformer` re-adds removed keys as nil — call it only when all declared keys are present).
- [ ] Commit slice 3.

## Slice 4: architecture bookkeeping

- [ ] Update `ramora/defclosedrecord/embodiment.md`: add an Artifacts row for `malli_test.clj` (V-07); the `deps.edn` row names malli in the `:test` alias.
- [ ] Update `mementum/state.md`: V-07 done.
- [ ] Commit slice 4.
