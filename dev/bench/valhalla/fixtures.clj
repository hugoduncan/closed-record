(ns valhalla.fixtures
  (:require [clojure.edn :as edn])
  (:import FixtureProbe))

;; Use the committed inventory rather than a second set of dimensions and values.
(let [{:keys [contracts cases]} (edn/read-string (slurp "dev/bench/cases.edn"))
      experimental (filterv #(= :valhalla (:configuration %)) cases)
      array-contract (:array contracts)
      field-contract (:field contracts)]
  (assert (= "1.11.4" (clojure-version)))
  (doseq [length (into #{} (comp (filter #(= :array (:contract %)))
                                 (map :length)) experimental)]
    (FixtureProbe/arrays length (:initial-x array-contract)
                         (:replacement-x array-contract) (:index array-contract)))
  (FixtureProbe/fields (:initial-x field-contract) (:replacement-x field-contract))
  (prn {:status :verified
        :kind :fixture-equivalence
        :clojure (clojure-version)
        :jdk (System/getProperty "java.runtime.version")
        :case-ids (mapv :id experimental)
        :checks [:construction :read :changed-store :unchanged-other-elements
                 :reset :distinct-holders :unchanged-sources :replacement
                 :flat-array :non-flat-reference-array]
        :timing false :memory false}))
