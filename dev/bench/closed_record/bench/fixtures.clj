(ns closed-record.bench.fixtures
  "Ordinary benchmark fixtures. Construct these outside measured expressions."
  (:require
   [closed-record.core :refer [defclosedrecord]]))

(defclosedrecord Closed3 [a b c])
(defrecord Record3 [a b c])
(defclosedrecord Closed16 [a b c d e f g h i j k l m n o p])
(defrecord Record16 [a b c d e f g h i j k l m n o p])

(defn assoc-pair
  "Build matched sources from an assoc inventory contract and case.

  Return sources, the shared replacement, and the declared key. Call assoc
  directly at each measurement call site; this function does not measure it.
  Reject widths without a fixed comparison type."
  [contract benchmark-case]
  (let [width (:width benchmark-case)
        constructors (case width
                       3 [map->Closed3 map->Record3]
                       16 [map->Closed16 map->Record16]
                       (throw (ex-info "Unsupported assoc width" {:width width})))
        fields (subvec (:fields contract) 0 width)
        values (subvec (:values contract) 0 width)
        data (zipmap fields values)]
    {:closed-record ((first constructors) data)
     :defrecord ((second constructors) data)
     :key (:key contract)
     :replacement (:replacement contract)}))
