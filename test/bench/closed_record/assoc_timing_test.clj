(ns closed-record.assoc-timing-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-timing :as timing]
            [closed-record.bench.assoc-timing-child :as child]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport]
            [closed-record.bench.metrics :as metrics]
            [criterium.measured :as measured]))

(deftest generated-assoc-expression-test
  ;; Exercise the actual macro output: Cons forms previously became timed nil.
  (let [expansion (#'timing/measure nil {} {:a 1} :a 2 1)
        expression (second (second expansion))
        subject (eval (list 'criterium.measured/expr expression))
        inputs (measured/args subject)]
    (testing "the generated measurement executes changed-value assoc"
      (is (= [{:a 1} :a 2] inputs))
      (doseq [operations [1 7 100]]
        (is (= {:a 2} (second (measured/invoke subject inputs operations))))))))

(deftest literal-assoc-timing-test
  ;; Exercise real Criterium sites once; short budgets test plumbing, not speed.
  (let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        association {:run-id "timing-test" :revision "test"
                     :configuration :ordinary :fork 1}]
    (testing "invalid inputs fail before any call site runs"
      (doseq [limit [0 -1 ##Inf ##NaN nil]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (timing/collect! inventory association limit))))
      (is (thrown? clojure.lang.ExceptionInfo
                   (timing/collect! (update inventory :cases pop)
                                    (assoc association :configuration :unknown) 0.1))))
    (testing "both widths retain raw time and the actual divisor"
      (let [directory (.toFile (java.nio.file.Files/createTempDirectory
                                "timing-child-" (make-array java.nio.file.attribute.FileAttribute 0)))
            path (io/file directory "results.edn")
            [envelope saved] (try
                               (let [written (child/write-results! path inventory association 0.1)]
                                 [written (transport/decode inventory association (slurp path))])
                               (finally
                                 (io/delete-file path true)
                                 (io/delete-file directory true)))
            rows (:results saved)]
        (is (= envelope saved))
        (is (= (runtime/observe) (:runtime saved)))
        (is (= [[:assoc/width-3 :closed-record]
                [:assoc/width-3 :defrecord]
                [:assoc/width-16 :closed-record]
                [:assoc/width-16 :defrecord]]
               (mapv (juxt :case-id :representation) rows)))
        (doseq [row rows]
          (is (= :measured (:status row)))
          (is (= :ns/op (:unit row)))
          (is (pos? (:operations row)))
          (is (pos? (:raw row)))
          (is (= (/ (:raw row) (:operations row))
                 (:value (metrics/normalise row)))))))))
