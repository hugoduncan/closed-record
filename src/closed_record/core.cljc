(ns closed-record.core
  "Public API of closed-record: a record type that does not permit assoc of
  undeclared keys or dissoc of declared keys."
  #?(:clj (:require [closed-record.impl.jvm :as jvm])))

(defprotocol ClosedRecord
  "Internal. Implemented only by types that defclosedrecord generates."
  (-extra-keys [r] "The extra keys of a map literal, or nil."))

#?(:clj
   (defn- validate-fields
     [fields]
     (let [reserved '#{__meta __extmap __hash __hasheq}]
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
              (recur (conj impls [(first s) (take-while seq? (next s))])
                     (drop-while seq? (next s)))
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
  map literal #ns.Name{...} are hidden; use check to find them.

  Supported options: :no-print and :load-ns. Defines ->Name and map->Name."
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
  (if (satisfies? ClosedRecord x)
    (if-let [extra (seq (-extra-keys x))]
      (let [ks (set (map key extra))
            c (value-class x)]
        (throw (ex-info (str "Extra keys " ks " in closed record " c)
                        {:type :closed-record/extra-keys
                         :record c
                         :keys ks})))
      x)
    (let [c (value-class x)]
      (throw (ex-info (str "Not a closed record: " c)
                      {:type :closed-record/not-closed-record
                       :value-class c})))))
