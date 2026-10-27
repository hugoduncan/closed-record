(ns valhalla.applicability
  (:require [closed-record.core :refer [defclosedrecord]])
  (:import CapabilityProbe$Point
           [jdk.internal.misc Unsafe]
           [jdk.internal.value ValueClass]))

(set! *warn-on-reflection* true)

(defclosedrecord Holder [payload])
(defclosedrecord HintedHolder [^CapabilityProbe$Point payload])

;; Exercise real generated records and VM layout queries, not inferred hints.
(let [initial (CapabilityProbe$Point. 17)
      replacement (CapabilityProbe$Point. 23)
      array (ValueClass/newNullRestrictedAtomicArray CapabilityProbe$Point 4 initial)
      source (->Holder initial)
      result (assoc source :payload replacement)
      array-holder (->Holder array)
      hinted (->HintedHolder initial)
      field (.getDeclaredField HintedHolder "payload")
      reference-array (into-array Holder [source result])]
  (assert (= "1.11.4" (clojure-version)))
  (assert (not (.isValue Holder)))
  (assert (not (.isValue HintedHolder)))
  (assert (= 17 (.-x ^CapabilityProbe$Point (:payload source))))
  (assert (= 23 (.-x ^CapabilityProbe$Point (:payload result))))
  (assert (not (identical? source result)))
  (assert (identical? array (:payload array-holder)))
  (assert (ValueClass/isFlatArray (:payload array-holder)))
  (aset ^objects array 0 replacement)
  (assert (= 23 (.-x ^CapabilityProbe$Point (aget ^objects (:payload array-holder) 0))))
  (assert (= initial (:payload hinted)))
  (assert (nil? (:payload (assoc hinted :payload nil))))
  (assert (= Object (.getType field)))
  (assert (not (ValueClass/isNullRestrictedField field)))
  (assert (not (.isFlatField (Unsafe/getUnsafe) field)))
  (assert (not (ValueClass/isFlatArray reference-array)))
  (prn {:status :verified
        :clojure (clojure-version)
        :jdk (System/getProperty "java.runtime.version")
        :closed-record {:value-class false :changed-assoc true
                        :value-payload true :flat-array-payload true
                        :array-mutation-visible true :own-array-flat false}
        :hinted-field {:declared-type :object :null-restricted false
                       :flat false :accepts-null true}}))
