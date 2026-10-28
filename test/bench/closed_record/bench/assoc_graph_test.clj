(ns closed-record.bench.assoc-graph-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-graph :as assoc-graph]
            [closed-record.bench.fixtures :as fixtures]
            [closed-record.bench.metrics :as metrics])
  (:import RecordGraphProbe))

(deftest ordinary-graph-rows
  ;; Compare real agent counts with independently summed shallow layouts.
  (let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        contract (get-in inventory [:contracts :assoc])
        cases (filterv #(= :assoc (:contract %)) (:cases inventory))]
    (doseq [benchmark-case cases
            representation [:closed-record :defrecord]]
      (testing (str (:id benchmark-case) " " representation)
        (let [fixture (fixtures/assoc-pair contract benchmark-case)
              source (get fixture representation)
              shallow (RecordGraphProbe/shallow source)
              payload (RecordGraphProbe/shallow (:replacement fixture))
              expected (+ shallow (* (:width benchmark-case) payload))
              rows (assoc-graph/collect! inventory benchmark-case representation 1)
              by-root (into {} (map (juxt :root :raw)) rows)]
          (is (= 3 (count rows)))
          (is (= {:result expected
                  :source-and-result (+ expected shallow payload)
                  :increment-relative-to-source (+ shallow payload)} by-root))
          (is (every? #(= expected (:source-bytes %) (:result-bytes %)) rows))
          (is (= rows (edn/read-string (pr-str rows))))
          (is (= 3 (count (metrics/validate-results inventory rows))))
          (is (every? #(and (= :measured (:status %))
                            (= representation (:representation %))
                            (= 1 (:fork %))) rows)))))
    (testing "invalid associations and unsupported payloads fail"
      (doseq [[benchmark-case representation fork]
              [[(first cases) :unknown 1]
               [(first cases) :closed-record 0]
               [(assoc (first cases) :width 7) :closed-record 1]
               [(first (:cases inventory)) :closed-record 1]]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (assoc-graph/collect! inventory benchmark-case representation fork))))
      (is (thrown? IllegalArgumentException
                   (assoc-graph/collect!
                    (assoc-in inventory [:contracts :assoc :replacement] "unsupported")
                    (first cases) :closed-record 1))))))
