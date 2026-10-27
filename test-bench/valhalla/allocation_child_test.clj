(ns valhalla.allocation-child-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.metrics :as metrics]
            [valhalla.allocation-child :as allocation]))

(deftest allocation-collection-test
  ;; Exercise real counters and post-counter checks, never speed thresholds.
  (let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))]
    (testing "invalid budgets reject before collection"
      (doseq [args [[0 16 2] [6 16 2] [1 0 2] [1 16 0]]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (apply allocation/collect! inventory args)))))
    (testing "all experimental sites retain raw batches and alternating order"
      (let [odd-rows (allocation/collect! inventory 1 16 2)
            even-rows (allocation/collect! inventory 2 16 2)]
        (is (= 18 (count (metrics/validate-results inventory odd-rows))))
        (is (= 18 (count (metrics/validate-results inventory even-rows))))
        (is (= (mapv :representation odd-rows)
               (into [] (mapcat reverse) (partition 2 (mapv :representation even-rows)))))
        (is (= odd-rows (edn/read-string (pr-str odd-rows))))
        (doseq [row (into odd-rows even-rows)]
          (let [{:keys [warmup batches]} (:allocation-samples row)]
            (is (= 2 (count warmup)))
            (is (= 5 (count batches)))
            (is (= 80 (:operations row)))
            (is (= (reduce + batches) (:raw row)))
            (is (every? #(and (integer? %) (<= 0 %)) (into warmup batches)))))))))
