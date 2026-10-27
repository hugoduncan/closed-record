(ns closed-record.bench-timing-budget-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.timing-budget :as budget]))

(deftest timing-budget-test
  ;; Preserve the exact CLI limit independently of operation counts and time samples.
  (testing "valid limits have a portable double representation"
    (doseq [seconds [1 1.5 1/2 Double/MIN_VALUE]]
      (let [declared (budget/declaration seconds)
            row {:raw 40 :operations 20 :timing-budget declared}
            saved (edn/read-string (pr-str row))]
        (is (= {:limit-time-s (double seconds)} declared))
        (is (= row (budget/validate-observation declared saved))))))
  (testing "undefined and unrepresentable declarations fail"
    (doseq [seconds [nil "1" 0 -1 ##NaN ##Inf ##-Inf 1E-1000M]]
      (is (thrown? clojure.lang.ExceptionInfo (budget/declaration seconds)))))
  (testing "missing, changed, and malformed observations fail"
    (doseq [observed [nil {} {:limit-time-s 2.0} {:limit-time-s ##NaN}
                      {:limit-time-s "1"} {:limit-time-s 1.0 :unknown true}]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (budget/validate-observation {:limit-time-s 1.0}
                                                {:timing-budget observed}))))))
