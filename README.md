# closed-record

A Clojure record type with a fixed key set.

## Problem

A Clojure record is an open map:

- `assoc` of any key succeeds. A key with a typo becomes a new entry.
- `dissoc` of a declared key changes the record into a plain map.

These operations do not fail. Key typos stay hidden until a later
failure, often a nil value far from the cause.

## Approach

`defclosedrecord` defines a record type with a fixed key set. It has
the same syntax as `defrecord`. An operation that changes the key set
throws `ex-info` at the location of the change:

- `assoc` (also `conj`, `merge`, `into`, `update`) of an undeclared key.
- `dissoc` of a declared key.
- `empty`.
- `map->Name` with an undeclared key.

All other operations behave as with `defrecord`: field access,
`assoc` of a declared key, protocols, interfaces, type hints,
metadata, equality, hashing, printing and reader literals. Call sites
that use a record correctly do not change. Reads and declared-key `assoc`
have similar costs in the measured three-field cases. Wider records can differ;
see [performance measurements and limits](doc/performance.md).

Reads are not checked. `(:nmae r)` returns nil, as with `defrecord`.
The Clojure 1.13 destructuring directives `:keys!`, `:select` and
`:all` find read errors. A closed record is a complement to these
directives.

A closed record is for use as an internal type. It does not validate
values. Use malli or spec for that.

## AI usage

AI coding agents wrote most of the code, tests and documentation of
this project, under the direction of a human developer.

## Status

Pre-release. There is no release on Clojars yet. The API is small:
`defclosedrecord` and `check`. After the first release, the API will
change only by addition.

- Host: Clojure on the JVM. ClojureScript and Babashka are not
  supported.
- Clojure versions: 1.11 and later. The tests run on 1.11, 1.12 and
  1.13.

## Installation

Add a git dependency to `deps.edn`:

```clojure
{:deps {io.github.hugoduncan/closed-record
        {:git/url "https://github.com/hugoduncan/closed-record"
         :git/sha "<commit sha>"}}}
```

## Usage

```clojure
(require '[closed-record.core :refer [defclosedrecord check]])

(defclosedrecord Point [x y])

(def p (->Point 1 2))

(:x p)               ;=> 1
(assoc p :x 10)      ;=> #user.Point{:x 10, :y 2}
(dissoc p :z)        ;=> #user.Point{:x 1, :y 2}

(assoc p :z 3)
;; throws: Undeclared key :z for closed record user.Point
;; ex-data: {:type :closed-record/undeclared-key, :record user.Point, :key :z}

(dissoc p :x)
;; throws: Cannot dissoc declared key :x from closed record user.Point
;; ex-data: {:type :closed-record/declared-key-dissoc, ...}

(map->Point {:x 1 :y 2 :z 3})
;; throws: ex-data {:type :closed-record/extra-keys, :keys #{:z}, ...}

(map->Point {:x 1})  ;=> #user.Point{:x 1, :y nil}
```

To get a plain map, use `(into {} p)` or `select-keys`.

### Reader literals

A reader literal with an undeclared key does not throw. The extra key
is hidden: lookup does not find it, and it does not change equality or
hash. Use `check` to find it:

```clojure
(def q #user.Point{:x 1 :y 2 :z 3})

(:z q)     ;=> nil
(check p)  ;=> p
(check q)
;; throws: ex-data {:type :closed-record/extra-keys, :keys #{:z}, ...}
```

`check` also throws (`:closed-record/not-closed-record`) for a value
that is not a closed record.

See the [API reference](doc/API.md) for arguments, options, constructors,
and error contracts.

### Use with malli

A malli `[:map ...]` schema validates and explains a closed record as
a map. Some transformers add an undeclared key or remove a declared
key. On a closed record, these throw:

- `default-value-transformer` with a default for an undeclared key
  throws `:closed-record/undeclared-key`.
- `strip-extra-keys-transformer` with a schema that does not have all
  the declared keys throws `:closed-record/declared-key-dissoc`.
- `key-transformer` throws `:closed-record/empty`.

To use these transformers, decode a plain map and keep the result as a
plain map:

```clojure
(m/decode schema (into {} r) transformer)
```

Call `map->Name` on the result only when the result has all the
declared keys and no other keys. An undeclared key throws. A missing
declared key gives nil.

## Development

See [development instructions](doc/development.md) for tests, CI,
benchmarks, and source JAR builds.

## Releases

See [release instructions](doc/releases.md) for local preparation,
inspection, a separate push, manual preview and publication, credentials,
and recovery after partial failure. No release has been published yet.
