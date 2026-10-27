(ns valhalla.graph-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [valhalla.graph :as graph]))

(deftest inventory-graphs
  ;; Exercise physical counts and unavailable outcomes with the real agent.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        odd-rows (graph/collect! inventory 1)
        even-rows (graph/collect! inventory 2)]
    (testing "all roots retain explicit measured or unavailable outcomes"
      (is (= 30 (count odd-rows) (count even-rows)))
      (is (= odd-rows (edn/read-string (pr-str odd-rows))))
      (doseq [row odd-rows]
        (if (= :unavailable (:status row))
          (do (is (seq (:reason row))) (is (not (contains? row :raw))))
          (do
            (is (<= 0 (:raw row)))
            (is (= (:raw row)
                   (case (:root row)
                     (:array :result) (:result-bytes row)
                     :source-and-result (:union-bytes row)
                     :increment-relative-to-source
                     (- (:union-bytes row) (:source-bytes row)))))))))
    (testing "pair order alternates without changing counts"
      (is (= :value-array (:representation (first odd-rows))))
      (is (= :reference-array (:representation (first even-rows))))
      (is (= (set (map #(dissoc % :fork) odd-rows))
             (set (map #(dissoc % :fork) even-rows)))))
    (testing "invalid fork associations reject"
      (doseq [fork [0 6 nil 1.5]]
        (is (thrown? clojure.lang.ExceptionInfo (graph/collect! inventory fork)))))))
