(ns closed-record.core
  "Public API of closed-record: a record type that does not permit assoc of
  undeclared keys or dissoc of declared keys."
  (:require [closed-record.runtime :as runtime]
            #?(:clj [closed-record.impl.jvm :as jvm])))

#?(:clj
   (defn- validate-fields
     [fields]
     (let [reserved (set jvm/hidden-fields)]
       (when (some reserved fields)
         (throw (AssertionError.
                 (str "The names in " reserved
                      " cannot be used as field names for closed records.")))))
     (when-let [dups (seq (for [[f n] (frequencies fields) :when (< 1 n)] f))]
       (throw (AssertionError.
               (str "Duplicate field names: " (vec dups)))))))

#?(:clj
   (defn- split-opts+specs
     "Return [option-map impls]. impls is a vector of [symbol method-forms]."
     [opts+specs]
     (loop [opts {}
            s opts+specs]
       (if (keyword? (first s))
         (recur (assoc opts (first s) (second s)) (nnext s))
         [opts
          (loop [impls []
                 s s]
            (if (seq s)
              (let [[methods more] (split-with seq? (next s))]
                (recur (conj impls [(first s) methods]) more))
              impls))]))))

#?(:clj
   (defmacro defclosedrecord
     "(defclosedrecord Name [fields*] options* specs*)

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
  operations; check cannot find keys that have already been discarded."
     {:arglists '([name [& fields] & opts+specs])}
     [name fields & opts+specs]
     (validate-fields fields)
     (let [[opts impls] (split-opts+specs opts+specs)]
       (jvm/emit name fields opts impls))))

(defn- value-class [x]
  #?(:clj (class x) :cljs (type x)))

(defn check
  "Return x if x is a closed record with no extra literal keys.

  Throw ex-info with :type :closed-record/extra-keys (and :record, :keys)
  if x is a closed record read from a literal with undeclared keys. Throw
  ex-info with :type :closed-record/not-closed-record (and :value-class)
  for any other value, including nil."
  [x]
  (let [c (value-class x)]
    (if (satisfies? runtime/ClosedRecord x)
      (if-let [extra (seq (runtime/-extra-keys x))]
        (let [ks (set (map key extra))]
          (throw (ex-info (str "Extra keys " ks " for closed record " (runtime/class-name c))
                          {:type :closed-record/extra-keys
                           :record c
                           :keys ks})))
        x)
      (throw (ex-info (str "Not a closed record: " (runtime/class-name c))
                      {:type :closed-record/not-closed-record
                       :value-class c})))))
