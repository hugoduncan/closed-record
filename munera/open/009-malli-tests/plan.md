# Plan

## Approach

Add malli 0.20.3 to the `:test` alias only. Write one example-test namespace,
`test/closed_record/malli_test.clj`, with `(defclosedrecord P [a b c])` and one `deftest` per group
from Scope 2: validation, explanation, nil fields, permitted transformers, throwing transformers.
Assertions follow the design's observed-facts table exactly: humanized explain results compared with
`=` to the full map; throwing cases compared on full `ex-data` (`:type`, `:record`, `:key` when
present), as the V-01 tests do. Then write the README `malli` section (names the three throwing
cases, the plain-map fix, and `map->Name` behavior per D-08), and update `embodiment.md` and
`mementum/state.md`.

Key decisions (from design.md):

- Example tests, not properties.
- Test observed malli results, not malli internals (`key-transformer` case checks only `ex-data`).
- No `src` changes. If a test exposes an emitter fault, stop and record in `implementation.md`.

## Risks

- Observed facts are pinned to malli 0.20.3, Clojure 1.11.4, commit `ef24b1a`. Behavior may differ
  under `:clj-1.12` / `:clj-1.13` aliases; acceptance requires all three to pass.
- The exact shape of `ex-data` `:record` for the throwing cases must match what V-01 tests use;
  read those tests first to copy the comparison style.
- malli transitive deps in the `:test` alias could trigger clj-kondo warnings; acceptance requires
  a clean lint of `src` and `test`.

## Slice order

1. Dependency: add `metosin/malli` 0.20.3 to the `:test` alias in `deps.edn`.
2. Tests: `test/closed_record/malli_test.clj` with the five deftests; verify with scry under all
   three Clojure aliases; lint and format.
3. Docs: README `malli` section.
4. Architecture bookkeeping: `embodiment.md` artifacts/deps rows; `mementum/state.md` V-07 done and
   `malli_test.clj` in the "Tests" list.
