# API reference

Generated file. Do not edit. Regenerate with `bb api-docs`; check with `bb api-docs --check`.

See the [README](../README.md) for examples. This library supports Clojure 1.11+ on the JVM only. ClojureScript and Babashka are not supported.

# Table of contents
-  [`closed-record.core`](#closed-record.core)  - Public API of closed-record: a record type that does not permit assoc of undeclared keys or dissoc of declared keys.
    -  [`check`](#closed-record.core/check) - Return x if x is a closed record with no extra literal keys.
    -  [`defclosedrecord`](#closed-record.core/defclosedrecord) - (defclosedrecord Name [fields*] options* specs*) Same syntax as defrecord.

-----
# <a name="closed-record.core">closed-record.core</a>


Public API of closed-record: a record type that does not permit assoc of
  undeclared keys or dissoc of declared keys.




## <a name="closed-record.core/check">`check`</a>
``` clojure
(check x)
```
Function.

Return x if x is a closed record with no extra literal keys.

  Throw ex-info with :type :closed-record/extra-keys (and :record, :keys)
  if x is a closed record read from a literal with undeclared keys. Throw
  ex-info with :type :closed-record/not-closed-record (and :value-class)
  for any other value, including nil.
<p><sub><a href="https://github.com/hugoduncan/closed-record/blob/master/src/closed_record/core.cljc#L103-L122">Source</a></sub></p>

## <a name="closed-record.core/defclosedrecord">`defclosedrecord`</a>
``` clojure
(defclosedrecord name [& fields] & opts+specs)
```
Macro.

(defclosedrecord Name [fields*] options* specs*)

  Same syntax as defrecord. Defines a record type with closed keys:

  - assoc (also conj, merge, into, update) of an undeclared key throws
    ex-info with :type :closed-record/undeclared-key.
  - dissoc of a declared key throws :closed-record/declared-key-dissoc.
  - empty throws :closed-record/empty.
  - map->Name with an undeclared key throws :closed-record/extra-keys.

  Lookup of an undeclared key gives the not-found value. Extra keys of a
  map literal `#ns.Name{...}` are hidden; use check to find them.

  Name is the type name symbol. fields is a vector of field symbols;
  each field declares its corresponding keyword key. specs supplies protocol
  or interface names and method implementations, as in defrecord.
  Options are keyword/value pairs before specs. Supported options are
  :no-print and :load-ns, passed to the JVM type compiler as in defrecord.

  Defines ->Name, which takes one value per field in declaration order,
  and map->Name, which takes a map of keyword keys to field values.
  Missing declared keys in map->Name have nil values.

  Rejected operations throw ex-info. Its ex-data contains :type and
  :record (the record class). Rejected assoc and dissoc also include :key;
  map->Name includes :keys (the set of undeclared keys).
  dissoc of an undeclared key returns the same record.

  Extra literal keys do not affect lookup, entries, equality, hash, or print.
  Compilation of a literal as a code constant and print/read round trips
  discard extra keys. Call check on a runtime-read literal before those
  operations; check cannot find keys that have already been discarded.
<p><sub><a href="https://github.com/hugoduncan/closed-record/blob/master/src/closed_record/core.cljc#L39-L76">Source</a></sub></p>
