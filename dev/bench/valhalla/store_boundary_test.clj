(ns valhalla.store-boundary-test
  (:require [clojure.test :refer [deftest is testing]]
            [criterium.measured :as measured]
            [valhalla.reset-measured :as reset-measured])
  (:import StoreBoundaryProbe))

(deftest store-boundary-test
  ;; A reset in args-fn is per invocation, not per operation in its batch.
  (doseq [flat? [true false]
          length [4 1024]]
    (testing (str "store boundary " flat? " length " length)
      (let [array (StoreBoundaryProbe/array flat? length)
            replacement (StoreBoundaryProbe/replacement)
            operation (measured/expr (StoreBoundaryProbe/store array replacement))]
        (is (= 17 (second (measured/invoke operation (measured/args operation) 1))))
        (is (= 23 (StoreBoundaryProbe/read array))))
      (let [array (StoreBoundaryProbe/array flat? length)
            replacement (StoreBoundaryProbe/replacement)
            operation (measured/expr (StoreBoundaryProbe/changedStore array replacement))]
        (is (thrown-with-msg? IllegalStateException #"did not change"
                              (measured/invoke operation (measured/args operation) 2)))
        (is (= 23 (StoreBoundaryProbe/read array)))))))

(deftest reset-measured-test
  ;; Real generated expressions enforce the precondition on every store.
  (doseq [flat? [true false]
          length [4 1024]]
    (testing (str "reset outside timed store " flat? " length " length)
      (let [array (StoreBoundaryProbe/array flat? length)
            initial (aget array 0)
            replacement (StoreBoundaryProbe/replacement)
            expression (measured/expr
                        (StoreBoundaryProbe/changedStore array replacement))
            operation (reset-measured/reset-measured
                       expression (fn [_] (aset array 0 initial)))]
        (doseq [state [(measured/args operation)
                       (measured/warmup-args operation)]
                count [1 2 100]]
          (let [[elapsed value] (measured/invoke operation state count)]
            (is (and (integer? elapsed) (<= 0 elapsed)))
            (is (= 17 value))
            (is (= 23 (StoreBoundaryProbe/read array)))))
        (doseq [count [0 -1 1.5 nil]]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"positive evaluation count"
                                (measured/invoke operation
                                                 (measured/args operation)
                                                 count))))))))
