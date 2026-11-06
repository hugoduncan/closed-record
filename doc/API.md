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
<p><sub><a href="https://github.com/hugoduncan/closed-record/blob/master/src/closed_record/core.cljc#L96-L115">Source</a></sub></p>

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
  :no-print and :load-ns, passed to the JVM type compiler as in defrecord,
  and :value-type.

  :value-type true defines the type through value-type
  (org.hugoduncan/value-type, Clojure 1.12.6). On a JVM with JEP 401
  value classes (Valhalla, --enable-preview) the type is a value class;
  otherwise value-type's fallback policy selects an identity class or an
  error. value-type must be on the class path at macro expansion and at run
  time, also for AOT classes. If value-type.core/defvaluetype* cannot be
  resolved, macro expansion throws ex-info with
  :type :closed-record/value-type-unavailable and :class-name (the class
  name symbol). :value-type false or no option gives an ordinary closed
  record and does not load value-type. The record contract stays the same,
  with these differences: hashes are computed on each call (no cache); on
  value output, identical? compares field values, locking and weak
  references throw IdentityException, and serialization is not promised.
  With :value-type true, ^:non-nil field metadata has value-type's meaning
  and passes to value-type unchanged: nil gives NullPointerException and a
  wrong type gives ClassCastException; without the option it has no effect.

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
<p><sub><a href="https://github.com/hugoduncan/closed-record/blob/master/src/closed_record/core.cljc#L36-L91">Source</a></sub></p>
