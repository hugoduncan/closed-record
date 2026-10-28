(ns closed-record.bench-timing-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.metrics :as metrics]
   [closed-record.bench.timing :as timing]))

(defn- estimate [batch mean]
  {:data {:bootstrap-stats
          {:batch-size batch
           :bootstrap {:elapsed-time {:mean {:point-estimate mean}}}}}})

(deftest raw-timing-outcome-test
  ;; Preserve the batch estimate and its own divisor, not a rounded ns/op value.
  (testing "unequal batches retain their raw estimates and normalize independently"
    (doseq [[batch mean expected] [[100 250.0 2.5] [7 21.0 3.0] [1 0.0 0.0]]]
      (let [result (timing/outcome (estimate batch mean))]
        (is (= {:metric :time :unit :ns/op :status :measured
                :raw mean :operations batch}
               result))
        (is (= expected (:value (metrics/normalise result)))))))
  (testing "missing and invalid estimates are failures, not zero measurements"
    (doseq [data [nil {} (estimate nil 10) (estimate 0 10)
                  (estimate -1 10) (estimate 1.5 10) (estimate 1 nil)
                  (estimate 1 -1) (estimate 1 ##NaN) (estimate 1 ##Inf)]]
      (is (thrown? clojure.lang.ExceptionInfo (timing/outcome data))
          (pr-str data)))))
