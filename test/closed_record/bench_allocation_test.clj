(ns closed-record.bench-allocation-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.allocation :as allocation]
   [closed-record.bench.metrics :as metrics]))

(deftest raw-allocation-outcome-test
  ;; Preserve counter evidence without subtracting boundary overhead.
  (testing "each interval retains its counters and its own operation count"
    (doseq [[before after operations expected]
            [[1000 21560 257 80] [19 327699 4096 80]
             [42 42 7 0] [100 107 2 7/2]
             [(- Long/MAX_VALUE 8) Long/MAX_VALUE 2 4]]]
      (let [result (allocation/outcome before after operations)]
        (is (= {:metric :allocation :unit :bytes/op :status :measured
                :raw (- after before) :operations operations
                :counter-before before :counter-after after}
               result))
        (is (= expected (:value (metrics/normalise result)))))))
  (testing "unavailable or invalid counters cannot become zero measurements"
    (doseq [[before after] [[-1 -1] [-1 10] [10 9] [nil 10] [0 nil]
                            [0 1.5] [0.0 1] [0 ##NaN] [0 ##Inf]
                            [0 (inc (bigint Long/MAX_VALUE))]]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (allocation/outcome before after 1)))))
  (testing "empty controls and invalid divisors have no bytes/op outcome"
    (doseq [operations [0 -1 nil 1.5]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (allocation/outcome 10 10 operations))))))
