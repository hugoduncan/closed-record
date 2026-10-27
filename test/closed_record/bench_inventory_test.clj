(ns closed-record.bench-inventory-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.report :as report]))

(defn- inventory []
  (edn/read-string (slurp (io/resource "cases.edn"))))

(deftest fixed-inventory-test
  ;; Lock the bounded matrix and its references before measurement code is added.
  (let [{:keys [cases contracts configurations execution]} (inventory)
        grouped (group-by :contract cases)]
    (testing "case identities and contract references are complete"
      (is (= 17 (count cases) (count (set (mapv :id cases)))))
      (is (every? keyword? (mapv :id cases)))
      (is (every? #(contains? contracts (:contract %)) cases))
      (is (every? #(contains? configurations (:configuration %)) cases)))
    (testing "all original cases keep their order, expressions and parity scope"
      (is (= report/ops (mapv :expression (:legacy grouped))))
      (is (= :s-03 (get-in contracts [:legacy :parity-rule])))
      (is (every? #(= :none (:parity-rule (contracts %)))
                  [:assoc :array :field])))
    (testing "new topics have fixed dimensions and separate memory metrics"
      (is (= [3 16] (mapv :width (:assoc grouped))))
      (is (= {4 #{:construct :read :store}
              1024 #{:construct :read :store}}
             (into {} (map (fn [[length rows]]
                             [length (set (mapv :operation rows))]))
                   (group-by :length (:array grouped)))))
      (is (= #{:construct :read :replace} (set (mapv :operation (:field grouped)))))
      (is (every? #(= [:time :allocation :graph] (:metrics (contracts %)))
                  [:assoc :array :field])))
    (testing "the inventory is not yet an acceptance-ready harness"
      (is (= report/fork-count (:forks execution)))
      (is (false? (:acceptance-ready? execution))))))

(deftest measurement-contract-test
  ;; Check data contracts, not source text or timing outcomes.
  (let [{:keys [contracts metrics]} (inventory)
        assoc-contract (:assoc contracts)]
    (testing "units do not mix time, allocation and reachable size"
      (is (= {:time :ns/op :allocation :bytes/op :graph :bytes}
             (into {} (map (fn [[metric spec]] [metric (:unit spec)])) metrics))))
    (testing "assoc always changes a declared value with matched field counts"
      (is (= 16 (count (:fields assoc-contract)) (count (:values assoc-contract))))
      (is (= :a (:key assoc-contract)))
      (is (not= (first (:values assoc-contract)) (:replacement assoc-contract))))
    (testing "experimental comparisons use matched non-null changed payloads"
      (doseq [topic [:array :field]]
        (is (= [17 23] ((juxt :initial-x :replacement-x) (contracts topic))))))
    (testing "array mutation is plain and separate from immutable replacement"
      (is (= :plain (get-in contracts [:array :ordering])))
      (is (= :outside-measurement (get-in contracts [:array :reset])))
      (is (= :in-place-element-store (get-in contracts [:array :update])))
      (is (= :new-holder-source-unchanged (get-in contracts [:field :update]))))))
