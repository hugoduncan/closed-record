(ns closed-record.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core]))

(deftest core-namespace-test
  ;; The public namespace loads, so later tasks can add vars to it.
  (testing "closed-record.core"
    (testing "is loaded"
      (is (find-ns 'closed-record.core)))))
