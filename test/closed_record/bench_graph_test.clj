(ns closed-record.bench-graph-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.graph :as graph]
   [closed-record.bench.metrics :as metrics]))

(deftest graph-outcomes-test
  ;; Sharing changes incremental size independently from result size.
  (testing "shared, disjoint, identical, and empty graphs retain raw counts"
    (doseq [[source result union incremental]
            [[192 192 304 112] [100 80 180 80] [100 100 100 0]
             [0 80 80 80] [80 0 80 0] [0 0 0 0]
             [Long/MAX_VALUE 1 Long/MAX_VALUE 0]]]
      (let [outcomes (graph/outcomes source result union)]
        (is (= [:result :source-and-result :increment-relative-to-source]
               (mapv :root outcomes)))
        (is (= [result union incremental] (mapv :raw outcomes)))
        (doseq [outcome outcomes]
          (is (= {:source-bytes source :result-bytes result :union-bytes union}
                 (select-keys outcome [:source-bytes :result-bytes :union-bytes])))
          (is (= (:raw outcome) (:value (metrics/normalise outcome))))))))
  (testing "invalid counts and impossible unions fail rather than become sizes"
    (doseq [counts [[100 80 99] [100 80 181] [0 0 1]
                    [-1 1 1] [1 -1 1] [1 1 -1]
                    [nil 0 0] [0 0.0 0] [0 0 ##NaN]
                    [0 0 ##Inf] [0 0 (inc (bigint Long/MAX_VALUE))]]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (apply graph/outcomes counts))))))
