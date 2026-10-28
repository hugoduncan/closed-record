(ns planning-estimate-test
  (:require [clojure.test :refer [deftest is testing]]
            [criterium.collect :as collect]))

(deftest planning-estimate-boundaries
  ;; Exercise the candidate's real planning function, without replacing clocks.
  (testing "zero and truncated sub-nanosecond estimates have a positive floor"
    (doseq [[elapsed evaluations expected] [[0 1 1] [0 100 1] [1 100 1]
                                          [100 100 1] [250 100 2]
                                          [2000 1 2000]]]
      (let [estimate (collect/planning-time-estimate elapsed evaluations)
            batch (collect/batch-size estimate 1000000)]
        (is (= expected estimate))
        (is (pos? batch))
        (is (pos? (/ 1000000000 (* estimate batch)))))))
  (testing "raw batch sizing still rejects a zero estimate"
    (is (thrown? ArithmeticException (collect/batch-size 0 1000000)))))
