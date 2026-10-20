# 009 malli tests

## Intent

Show with tests that a closed record works with malli map schemas (V-07, S-04, D-11). Validation and explanation
treat the record as a map. A transformer that adds an undeclared key or removes a declared key throws, and the
user can see why. The README names these cases, as the trade-offs in `qualities.md` require.

## Problem

V-07 has no test. D-11 rejects a built-in schema because "tests show that the type works with malli map schemas",
but no test shows this. The trade-off "closed maps in an open-map ecosystem" says that the docs must name the malli
transformers that throw. The README does not name them.

## Context

- Architecture: `ramora/defclosedrecord/`:
  - [qualities](../../../ramora/defclosedrecord/qualities.md): V-07, trade-off "Closed maps in an open-map ecosystem".
  - [purpose](../../../ramora/defclosedrecord/purpose.md): S-04, value model "Costs imposed".
  - [context](../../../ramora/defclosedrecord/context.md#malli), D-11 in `decisions/failure-and-validation.md`.
- `deps.edn`: the `:test` alias has scry and `test.check`. malli is not a dependency.
  `metosin/malli` 0.20.3 is the latest release (Clojars, 2026-10-20).
- Test rules: `mementum/knowledge/testing-generated-types.md`.

### Observed facts (2026-10-20, Clojure 1.11.4, malli 0.20.3, commit `ef24b1a`)

`(defclosedrecord P [a b c])`, `p` is `(->P 1 nil 3)`, `R` is `(defrecord R [a b c])`. `s` is
`[:map [:a :int] [:b [:maybe :int]] [:c :int]]`. `sc` is `s` with `{:closed true}`.

| Expression | Result for `P` | Result for `R` |
|---|---|---|
| `(m/validate s p)`, `(m/validate sc p)` | true, true | |
| `(m/explain s p)` | nil | |
| `(me/humanize (m/explain s (->P "x" nil 3)))` | `{:a ["should be an integer"]}` | |
| `(m/validate [:map [:a :int] [:d :int]] p)` | false, `{:d ["missing required key"]}` | |
| `(m/validate [:map {:closed true} [:a :int]] p)` | false | |
| `(m/decode s (->P "1" nil "3") mt/string-transformer)` | a `P`, `{:a 1 :b nil :c 3}` | |
| `(m/decode [:map [:b {:default 5} :int]] p mt/default-value-transformer)` | a `P`, `{:a 1 :b 5 :c 3}` | |
| `(m/decode [:map [:a :int] [:d {:default 5} :int]] p mt/default-value-transformer)` | throws `:closed-record/undeclared-key`, `:key :d` | an `R` with `:d 5` |
| `(m/decode [:map [:a :int]] p mt/strip-extra-keys-transformer)` | throws `:closed-record/declared-key-dissoc`, `:key :b` | a plain map `{:a 1}` |
| `(m/decode s p (mt/key-transformer {:decode name}))` | throws `:closed-record/empty` | throws `UnsupportedOperationException` |
| `(m/encode s p mt/json-transformer)`, `(m/coerce s p)` | `p` | |

## Scope

In scope:

1. Add `metosin/malli` 0.20.3 to the `:extra-deps` of the `:test` alias.
2. A test namespace `closed-record.malli-test` in `test/closed_record/malli_test.clj`, with
   `(defclosedrecord P [a b c])`. Example tests, one `deftest` for each group:
   - Validation: `m/validate` with an open and a closed `[:map ...]` schema of the declared keys gives true.
     A schema with a key that is not declared, or a closed schema with fewer keys, gives false.
   - Explanation: `m/explain` gives nil for a valid record. For a field value that is not valid, and for a
     missing schema key, `me/humanize` names the key and the error.
   - nil fields: a nil field is valid for `[:maybe :int]` and not valid for `:int`.
   - Permitted transformers: `string-transformer` decode and `default-value-transformer` for a declared nil field
     give a `P` with the expected entries. `json-transformer` encode and `m/coerce` give `p`.
   - Transformers that throw: the three throwing rows of the table above. Each test compares the full `ex-data`
     (`:type`, `:record`, `:key` when present), as the V-01 tests do.
3. A short `malli` section in `README.md`. It says that map schemas validate a closed record, and names the three
   throwing cases: a default for an undeclared key, `strip-extra-keys-transformer` with a schema that does not have
   all the declared keys, and `key-transformer`. It gives the fix: decode a plain map (`(into {} r)`). Then call
   `map->Name` on the result only if it has no undeclared keys.
4. Update `ramora/defclosedrecord/embodiment.md` (Artifacts row for `malli_test.clj` (V-07); the `deps.edn` row
   names malli in the `:test` alias) and `mementum/state.md` (V-07 done).

## Out of scope

- Generated values (`malli.generator`), function schemas, and schemas made from a record type.
- Other schema libraries (spec).
- A malli integration in `src` (a schema for `P`, or a transformer that rebuilds a record). D-11 rejects it.
- Changes to the emitter. If a test finds a fault, stop and record it in `implementation.md`.
- Performance (V-11).

## Decisions

- Example tests, not properties. The subject is the interaction with a small set of malli functions, not random
  sequences.
- malli in the `:test` alias only. The library has no new run-time dependency.
- Test the observed malli results, not malli internals. The `key-transformer` case throws because malli calls
  `empty`; the test checks only the `ex-data`.

## Acceptance criteria

1. `deps.edn` has `metosin/malli` 0.20.3 in the `:test` alias, and no other new dependency.
2. `test/closed_record/malli_test.clj` has the tests in Scope 2.
3. `clojure -M:test -m scry.cli`, with no alias, with `:clj-1.12` and with `:clj-1.13`, exits 0 with zero failures
   and zero errors.
4. `clj-kondo --lint src test` reports no errors and no warnings. `cljfmt check src test` passes.
5. `README.md` has the malli section in Scope 3.
6. `embodiment.md` and `mementum/state.md` are updated as in Scope 4.
