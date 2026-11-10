# closed-record

A Clojure record type with a fixed key set.

## Problem

A Clojure record is an open map:

- `assoc` of any key succeeds. A key with a typo becomes a new entry.
- `dissoc` of a declared key changes the record into a plain map.

These operations do not fail. Key typos stay hidden until a later
failure, often a nil value far from the cause.

A Clojure record is also an identity object. Two records with equal
fields are different objects. The JVM cannot store a record inline in
an array or in another object, and a field cannot reject nil.

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

The option `:value-type true` defines the record through
[value-type](https://github.com/hugoduncan/value-type). On a JVM with
JEP 401 value classes, the record is a value object: it has no
identity, and `^:non-nil` fields reject nil. The closed key set does
not change. A value-type record has no metadata. Records without the
option do not load value-type.

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

`:value-type true` is experimental and is not released. It depends on
value-type, which has no release, and on a Valhalla early-access JDK
for value classes. Its behaviour can change.

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

### Value-type records (optional)

`:value-type true` needs Clojure 1.12.6 and value-type on the class
path, at macro expansion and at run time. value-type has no release.
Add it as a Git dependency:

```clojure
{:deps {org.hugoduncan/value-type
        {:git/url "https://github.com/hugoduncan/value-type.git"
         :git/sha "4ed8d0a3c4ce8ed6a74607da312fd33a8c80dea2"}}}
```

For value classes, start the JVM from a Valhalla early-access JDK with
`--enable-preview`. `value-type.array` also needs
`--add-exports=java.base/jdk.internal.value=ALL-UNNAMED`. On other
JVMs, value-type's fallback policy (system property
`value-type.fallback`) selects an identity class, a warning, or an
error. The system property `value-type.value-classes` (`auto` or
`off`) is next to it: `off` selects identity fallback, also on a
Valhalla JVM.

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

### Value-type records

```clojure
(defclosedrecord Point [^long x ^long y] :value-type true)
(defclosedrecord Segment [^Point ^:non-nil start
                          ^Point ^:non-nil end]
  :value-type true)

(identical? (->Point 1 2) (->Point 1 2))  ;=> true on a value-class JVM

(def s (->Segment (->Point 1 2) (->Point 3 4)))

(assoc s :start nil)  ;; throws NullPointerException
(assoc s :z 1)        ;; throws :closed-record/undeclared-key, as before
```

The record contract does not change, with these differences:

- `identical?` compares field values. `locking` and weak references
  throw `IdentityException`.
- Each `hash` call computes the hash again.
- Serialization is not promised.
- The record has no metadata. `meta` gives nil, and `with-meta` throws
  `ClassCastException`.
- A map literal with an undeclared key throws at read. There is no
  hidden key for `check` to find.
- A user method that reads `__meta` or `__extmap` fails at definition
  on value output. On identity fallback it reads nil.

On identity fallback, `identical?`, `locking` and weak references work
as with `defrecord`.

#### Non-nil fields

- A `^:non-nil` field needs a tag that names a concrete, non-primitive
  class.
- Nil gives `NullPointerException`, and a value of the wrong type gives
  `ClassCastException`. This applies on value output and on identity
  fallback.
- Without `:value-type true`, `:non-nil` has no effect, as in
  `defrecord`.

#### Arrays and flattening

`value-type.array` makes arrays that reject nil elements:

```clojure
(require '[value-type.array :as va])

(def points (va/into-array Point [(->Point 1 2) (->Point 3 4)]))
(aset points 0 nil)   ;; throws NullPointerException
```

The JVM can store value objects inline in an array or in another
object. This is flattening. Flattening is a JVM choice, not a
guarantee. `va/array-info` gives the actual layout in `:flat?`. On
Valhalla `27-jep401ea3+1-1`, a value record has only its declared
fields, and a record with at most 64 bits of fields can be flat:

```clojure
(defclosedrecord Cell [^long v] :value-type true)

(:flat? (va/array-info (va/into-array Cell [(->Cell 1) (->Cell 2)])))
;=> true on Valhalla 27-jep401ea3+1-1
```

`Point [^long x ^long y]` (128 bits) is not flat on that JVM.

See [value-type records](doc/value-type-records.md) for the full
behaviour, field rules, errors and requirements.

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
and recovery after partial failure.
