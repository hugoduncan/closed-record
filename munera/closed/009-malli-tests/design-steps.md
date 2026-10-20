# Design steps

- [x] Architecture review: align the README fix in Scope 3 with D-08 and the behaviour table for `map->Name`.
  - D-08 makes `map->Name` the explicit construction path that enforces the key set: an extra key throws
    `:closed-record/extra-keys`. The advice "call `map->Name` only if it has no undeclared keys" duplicates this
    check. Say that `map->Name` rejects undeclared keys with `ex-data`, as the error path the user can see.
  - `map->Name` gives nil for a missing declared key (D-19). Thus after `strip-extra-keys-transformer`, `map->Name`
    silently adds the removed declared keys back as nil. The fix must say this: keep the plain map, or rebuild only
    when the result has all the declared keys.
- [x] Ambiguity review: state how the Explanation tests in Scope 2 check `me/humanize` output.
  - "names the key and the error" allows a check of the key only (`contains?`), or an `=` check of the full map.
  - The table gives the full observed maps (`{:a ["should be an integer"]}`, `{:d ["missing required key"]}`), and the
    decision "Test the observed malli results" with malli pinned to 0.20.3 supports `=` on the full map. Write the
    chosen form in Scope 2.
- [x] Inconsistency review: make the Intent and Problem quotes match the `qualities.md` trade-off.
  - `qualities.md` "Closed maps in an open-map ecosystem": the docs must name malli default-value transformers
    and code that adds keys such as `:db/id`. The Problem section quotes only "the malli transformers that throw",
    and the Intent says the README section is what the trade-offs require.
  - Scope 3 stays as it is (malli only). Say that this task covers the malli part of the trade-off, and that the
    `:db/id` case is not part of it.

## Plan review

- [x] Ambiguity review: state the test `ns` requires in steps.md Slice 2 without "scry/test ns per project conventions".
  - No test namespace requires a scry namespace. scry is only the runner (`-m scry.cli`). All test namespaces use
    `[clojure.test :refer [deftest is testing]]` and `[closed-record.core :refer [defclosedrecord]]`, and have a
    docstring that names the V-ids (here V-07, S-04, D-11).
  - Write: require `clojure.test`, `closed-record.core`, `malli.core :as m`, `malli.error :as me`,
    `malli.transform :as mt`; no scry require; ns docstring names V-07.
- [x] Ambiguity review: state what the `mementum/state.md` update in Slice 4 changes.
  - "V-07 done" allows only the edit of the "Verification plan done" line. The earlier tasks also added the test
    file to the "Tests" list, and the property task named its dependency in the `:test` alias.
  - Write: move V-07 to the done list (not done: V-11 only); add `test/closed_record/malli_test.clj`
    (`closed-record.malli-test`, V-07) to the "Tests" list with the note that `metosin/malli` 0.20.3 is in the
    `:test` alias.
