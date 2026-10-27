(ns closed-record.timing-estimator-test
  (:require [clojure.test :refer [deftest is testing]]
            [criterium.collect :as collect]))

(deftest zero-estimate-contract-test
  ;; Characterize the pinned tool failure without relying on clock resolution.
  ;; This is a tool diagnostic, not approval of the timing harness.
  (testing "a zero initial estimate fails before a batch can be collected"
    (is (thrown? ArithmeticException (collect/batch-size 0 1000000))))
  (testing "positive estimates produce bounded positive batch counts"
    (is (= 1000000 (collect/batch-size 1 1000000)))
    (is (= 100 (collect/batch-size 10000 1000000)))
    (is (= 1 (collect/batch-size 2000000 1000000)))))
