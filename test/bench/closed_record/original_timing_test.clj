(ns closed-record.original-timing-test
  (:require [clojure.test :refer [deftest is]]
            [closed-record.bench :as bench]
            [closed-record.bench.report :as report]))

(deftest original-cases-smoke-test
  ;; Run each original literal site once in a fresh JVM; do not test parity.
  (let [rows (bench/run 1 1.0)]
    (prn {:scope :smoke-only :fork 1 :limit-time-s 1.0 :rows rows})
    (is (= report/ops (mapv first rows)))
    (is (= {:result rows} (report/decode-result (report/encode-result rows))))
    (doseq [[operation closed ordinary] rows
            stats [closed ordinary]]
      (is (every? #(and (number? %) (Double/isFinite (double %)) (pos? %))
                  (vals stats))
          operation)
      (is (<= (:low stats) (:mean stats) (:high stats)) operation))))
