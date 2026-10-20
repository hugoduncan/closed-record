# Steps

## Slice 1: dependency

- [x] Add `metosin/malli {:mvn/version "0.20.3"}` to `:extra-deps` of the `:test` alias in `deps.edn`; confirm no other dependency changes.
- [x] Verify the dep resolves: `clojure -M:test -e "(require 'malli.core)"` exits 0.

## Slice 2: tests

- [x] Read the V-01 tests to copy the full-`ex-data` comparison style (`:type`, `:record`, `:key`).
- [x] Create `test/closed_record/malli_test.clj` (ns `closed-record.malli-test`) and `(defclosedrecord P [a b c])`.
  - ns docstring: names V-07 (also S-04, D-11).
  - ns requires: `[clojure.test :refer [deftest is testing]]`, `[closed-record.core :refer [defclosedrecord]]`,
    `[malli.core :as m]`, `[malli.error :as me]`, `[malli.transform :as mt]`.
  - No scry require: scry is only the runner (`-m scry.cli`).
- [x] Add `deftest` validation: open and closed `[:map ...]` schemas of declared keys validate true; a schema with an undeclared key (`[:map [:a :int] [:d :int]]`) and a closed schema with fewer keys (`[:map {:closed true} [:a :int]]`) validate false.
- [x] Add `deftest` explanation: `m/explain` nil for a valid record; `me/humanize` of `(m/explain s (->P "x" nil 3))` `=` `{:a ["should be an integer"]}`; `me/humanize` of `(m/explain [:map [:a :int] [:d :int]] p)` `=` `{:d ["missing required key"]}`.
- [x] Add `deftest` nil fields: nil field valid for `[:maybe :int]`, invalid for `:int`.
- [x] Add `deftest` permitted transformers: `string-transformer` decode of `(->P "1" nil "3")` gives a `P` with `{:a 1 :b nil :c 3}`; `default-value-transformer` with `[:map [:b {:default 5} :int]]` gives a `P` with `{:a 1 :b 5 :c 3}`; `json-transformer` encode and `m/coerce` return `p`.
- [x] Add `deftest` throwing transformers, comparing full `ex-data`: default for undeclared key throws `:closed-record/undeclared-key` with `:key :d`; `strip-extra-keys-transformer` with `[:map [:a :int]]` throws `:closed-record/declared-key-dissoc` with `:key :b`; `key-transformer` decode throws `:closed-record/empty`.
- [x] Run `clojure -M:test -m scry.cli` (no extra alias): exit 0, zero failures, zero errors.
- [x] Run `clojure -M:test:clj-1.12 -m scry.cli`: exit 0, zero failures, zero errors.
- [x] Run `clojure -M:test:clj-1.13 -m scry.cli`: exit 0, zero failures, zero errors.
- [x] Run `clj-kondo --lint src test`: no errors, no warnings.
- [x] Run `cljfmt check src test`: passes (fix with `cljfmt fix` if needed).
- [x] Commit slice 1+2.

## Slice 3: README

- [x] Add a `malli` section to `README.md`: map schemas validate a closed record; name the three throwing cases (default for an undeclared key, `strip-extra-keys-transformer` with a schema missing declared keys, `key-transformer`); give the fix (decode `(into {} r)`, keep the result a plain map); state `map->Name` behavior per D-08 (undeclared key throws `:closed-record/extra-keys` with keys in `ex-data`; missing declared key becomes nil, so `map->Name` after `strip-extra-keys-transformer` re-adds removed keys as nil — call it only when all declared keys are present).
- [x] Commit slice 3.

## Slice 4: architecture bookkeeping

- [x] Update `ramora/defclosedrecord/embodiment.md`: add an Artifacts row for `malli_test.clj` (V-07); the `deps.edn` row names malli in the `:test` alias.
- [x] Update `mementum/state.md` "Status" section:
  - "Verification plan done" line: add V-07 to the done list; the not-done list is V-11 only.
  - "Tests:" list: add `test/closed_record/malli_test.clj` (`closed-record.malli-test`, V-07), with the note that
    `metosin/malli` 0.20.3 is in the `:test` alias (same form as the `property_test.clj` entry).
- [x] Commit slice 4.
