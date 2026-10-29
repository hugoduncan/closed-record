(ns closed-record.runtime
  "Internal runtime support for closed records. Does not depend on the public
  API or the macro emitter.")

(defprotocol ClosedRecord
  "Internal. Implemented only by types that defclosedrecord generates."
  (-extra-keys [r] "The map of the extra entries of a map literal, or nil."))

(defn class-name
  "The name of class c without a \"class \" prefix, or \"nil\"."
  [c]
  (if (nil? c)
    "nil"
    #?(:clj (.getName ^Class c) :cljs (pr-str c))))

(defn key-error
  "Return the ex-info of a rejected key operation on a closed record.
  Internal: the generated assoc and without call it on the
  rejected path only (D-21). `t` is :closed-record/undeclared-key or
  :closed-record/declared-key-dissoc. `c` is the record class."
  [t c k]
  (ex-info (case t
             :closed-record/undeclared-key
             (str "Undeclared key " k " for closed record " (class-name c))
             :closed-record/declared-key-dissoc
             (str "Cannot dissoc declared key " k " from closed record "
                  (class-name c)))
           {:type t :record c :key k}))
