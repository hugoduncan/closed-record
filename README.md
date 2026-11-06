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

Released on Clojars. The API is small: `defclosedrecord` and `check`.
The API changes only by addition. See the [changelog](CHANGELOG.md).

- Host: Clojure on the JVM. ClojureScript and Babashka are not
  supported.
- Clojure versions: 1.11 and later. The tests run on 1.11, 1.12 and
  1.13.

## Installation

The library is on [Clojars](https://clojars.org/org.hugoduncan/closed-record).

Clojure CLI (`deps.edn`):

```clojure
{:deps {org.hugoduncan/closed-record {:mvn/version "0.1.190"}}}
```

Leiningen (`project.clj`):

```clojure
[org.hugoduncan/closed-record "0.1.190"]
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

### Value-type records

The `:value-type true` option defines the record through
[value-type](https://github.com/hugoduncan/value-type):

```clojure
(defclosedrecord Point [x y] :value-type true)
```

On a JVM with JEP 401 value classes (a Valhalla early-access JDK with
`--enable-preview`), the type is a value class. On other JVMs,
value-type's fallback policy (system property `value-type.fallback`)
selects an identity class, a warning, or an error.

Requirements:

- Clojure 1.12.6. value-type does not load on Clojure 1.11.
- value-type on the class path at macro expansion and at run time, also
  for AOT classes. value-type has no release yet. Add it as a Git
  dependency:

  ```clojure
  {:deps {org.hugoduncan/value-type
          {:git/url "https://github.com/hugoduncan/value-type.git"
           :git/sha "edc3f79de77bcf044d6234df361e090edc46c813"}}}
  ```

Without value-type, macro expansion throws ex-info with
`:type :closed-record/value-type-unavailable` and `:class-name` (the
class name symbol, for example `user.Point`). Without the option,
closed-record does not load value-type.

The record contract does not change, with these differences:

| Behaviour | Ordinary | Value output | Identity fallback |
|---|---|---|---|
| Hash values | as `defrecord` | same | same |
| Hash caching | yes | no | no |
| `identical?` | reference | field values | reference |
| `locking`, weak references | work | throw `IdentityException` | work |
| Serialization | as `defrecord` | not promised | as `defrecord` |
| Non-nil enforcement | none: `:non-nil` has no effect | native null restriction with a value-class tag on a JVM that supports native fields; otherwise constructor checks | constructor checks |
| Field storage type | primitive tag: the primitive; other fields: `Object` | `:non-nil` field: the tag class; other fields: as Ordinary | `:non-nil` field: the tag class; other fields: as Ordinary |

Each `hash` call computes the hash again. The option makes no
performance claim; see [performance](doc/performance.md#value-type-records).

#### Non-nil fields

With `:value-type true`, field metadata `^:non-nil` makes a field
non-nil:

```clojure
(defclosedrecord Point [^long x ^long y] :value-type true)
(defclosedrecord Segment [^Point ^:non-nil start
                          ^Point ^:non-nil end
                          label]
  :value-type true)
```

A non-nil field must have a tag that names a defined concrete,
non-primitive class. value-type owns these rules; see its
[field contract](https://github.com/hugoduncan/value-type/blob/edc3f79de77bcf044d6234df361e090edc46c813/doc/usage.md#portable-non-nil-fields).
closed-record passes the metadata to value-type with no change.

- Nil gives `NullPointerException`, and a value of the wrong type gives
  `ClassCastException`, on `->Name`, `map->Name`, `assoc` and record
  literals. On `->Name`, `map->Name` and `assoc`, closed-record does not
  wrap these host exceptions, and they have no `ex-data`. On record
  literals, the reader can wrap them in an exception with `ex-data`; the
  host exception is in the cause chain.
- `map->Name` without a key for a non-nil field gives
  `NullPointerException`.
- Key-set errors come first and do not change: `assoc` of an undeclared
  key, `dissoc` of a declared key and `map->Name` with extra keys throw
  ex-info as before.
- Without `:value-type true`, `:non-nil` has no effect, as in
  `defrecord`. The field accepts nil.

## Development

See [development instructions](doc/development.md) for tests, CI,
benchmarks, and source JAR builds.

## Releases

See [release instructions](doc/releases.md) for local preparation,
inspection, a separate push, manual preview and publication, credentials,
and recovery after partial failure. No release has been published yet.
