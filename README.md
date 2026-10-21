# closed-record

Provides adefrecord like record, that does not permit assoc'ing
additional keys, or dissoc'ing declared keys.

## Rational

This is intended to provide a type that can be used as an implementation
detail, and catches issues with incorrect assoc, dissoc, etc.

It is intend as a complement to clojure's new required :keys!, :select,
:all destructing directives. It is not intended as an antagonist to
these.

## malli

A malli `[:map ...]` schema validates and explains a closed record as a
map. Some transformers add an undeclared key or remove a declared key.
On a closed record, these throw:

- `default-value-transformer` with a default for an undeclared key
  throws `:closed-record/undeclared-key`.
- `strip-extra-keys-transformer` with a schema that does not have all
  the declared keys throws `:closed-record/declared-key-dissoc`.
- `key-transformer` throws `:closed-record/empty`.

Fix: decode a plain map, `(m/decode schema (into {} r) transformer)`,
and keep the result as a plain map.

`map->Name` on the result:

- An undeclared key throws `:closed-record/extra-keys`. The `:keys` of
  the `ex-data` names the keys.
- A missing declared key gives nil. Thus `map->Name` on the result of
  `strip-extra-keys-transformer` adds the removed declared keys again,
  with nil values. Call `map->Name` only when the result has all the
  declared keys.

## Development

Use the Clojure CLI. Run all commands from the repository root.

Run the tests on Clojure 1.11 (the minimum version):

    clojure -M:test -m scry.cli

Run the tests on Clojure 1.12 and on Clojure 1.13:

    clojure -M:test:clj-1.12 -m scry.cli
    clojure -M:test:clj-1.13 -m scry.cli

Start a REPL. The nREPL server writes the port to `.nrepl-port`:

    clojure -M:test:nrepl

Run the tests in the REPL:

    (require '[scry.core :as scry])
    (scry/run)

Run the benchmarks against `defrecord`:

    clojure -M:bench
