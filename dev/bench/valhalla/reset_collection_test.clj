(ns valhalla.reset-collection-test
  (:require [clojure.test :refer [deftest is testing]]
            [closed-record.bench.timing :as timing]
            [criterium.bench :as bench]
            [criterium.measured :as measured]
            [valhalla.reset-measured :as reset-measured])
  (:import StoreBoundaryProbe))

(deftest reset-collection-test
  ;; Exercise estimation, warmup, collection and analysis, not just invoke.
  (doseq [flat? [true false]
          length [4 1024]]
    (testing (str "changed stores through collection " flat? " " length)
      (let [array (StoreBoundaryProbe/array flat? length)
            initial (aget array 0)
            replacement (StoreBoundaryProbe/replacement)
            expression (measured/expr
                        (StoreBoundaryProbe/changedStore array replacement))
            operation (reset-measured/reset-measured
                       expression (fn [_] (aset array 0 initial)))]
        (bench/bench-measured
         (bench/options->bench-plan {:viewer :none :limit-time-s 1.0})
         operation)
        (let [outcome (timing/outcome (bench/last-bench))]
          (is (= :measured (:status outcome)))
          (is (pos-int? (:operations outcome)))
          (is (and (Double/isFinite (double (:raw outcome)))
                   (pos? (:raw outcome))))
          (is (= 23 (StoreBoundaryProbe/read array))))))))
