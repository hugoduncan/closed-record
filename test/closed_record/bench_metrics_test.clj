(ns closed-record.bench-metrics-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.metrics :as metrics]))

(deftest normalisation-test
  ;; Each batch uses its own divisor; graph bytes are never divided.
  (doseq [[metric unit] [[:time :ns/op] [:allocation :bytes/op]]]
    (let [batch {:metric metric :unit unit :status :measured
                 :raw 120 :operations 3}]
      (is (= (assoc batch :value 40) (metrics/normalise batch)))
      (is (= 24 (:value (metrics/normalise (assoc batch :operations 5)))))
      (is (= 0 (:value (metrics/normalise (assoc batch :raw 0)))))))
  (is (= 123 (:value (metrics/normalise {:metric :graph :unit :bytes :status :measured :raw 123})))))

(deftest unavailable-test
  ;; Missing evidence must survive transport without becoming a zero.
  (doseq [status [:unavailable :failed]]
    (let [outcome {:metric :graph :unit :bytes :status status
                   :reason "Physical box identity is unverified"}]
      (is (= outcome (metrics/normalise outcome)))
      (is (not (contains? (metrics/normalise outcome) :value))))))

(deftest invalid-outcomes-test
  ;; Reject bad units, non-finite totals, invalid divisors and mixed states.
  (let [batch {:metric :allocation :unit :bytes/op :status :measured
               :raw 120 :operations 3}]
    (testing "measured outcomes"
      (doseq [changes [{:metric :unknown} {:unit :bytes} {:status :unknown}
                       {:raw -1} {:raw ##NaN} {:raw ##Inf} {:raw 1.5}
                       {:operations 0} {:operations -1} {:operations 1.5}
                       {:operations nil} {:raw nil} {:value 40} {:reason "error"}]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (metrics/normalise (merge batch changes))) (pr-str changes))))
    (testing "graph bytes do not accept a divisor"
      (is (thrown? clojure.lang.ExceptionInfo
                   (metrics/normalise (assoc batch :metric :graph :unit :bytes)))))
    (testing "non-measured outcomes require a reason and no totals"
      (doseq [outcome [nil {} {:metric :graph :unit :bytes :status :unavailable}
                       {:metric :graph :unit :bytes :status :failed :reason " "}
                       (assoc batch :status :failed :reason "error")]]
        (is (thrown? clojure.lang.ExceptionInfo (metrics/normalise outcome)))))))

(deftest result-identity-test
  ;; Use the actual inventory to reject misattributed and duplicate evidence.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        row {:case-id :assoc/width-3 :configuration :ordinary
             :representation :closed-record :fork 1
             :metric :time :unit :ns/op :status :measured :raw 120 :operations 3}]
    (testing "partial results preserve order and raw inputs"
      (is (= [(assoc row :value 40)] (metrics/validate-results inventory [row])))
      (is (= [] (metrics/validate-results inventory [])))
      (is (= 5 (count (metrics/validate-results inventory
                                                (mapv #(assoc row :fork %) [1 2 3 4 5]))))))
    (testing "invalid associations fail"
      (doseq [changes [{:case-id :unknown} {:configuration :valhalla}
                       {:representation :value-array} {:fork 0} {:fork 6}
                       {:fork 1.5} {:fork nil} {:root :result}
                       {:case-id :legacy/get :metric :allocation :unit :bytes/op}]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (metrics/validate-results inventory [(merge row changes)]))
            (pr-str changes))))
    (testing "duplicates cannot be hidden by outcome status"
      (is (thrown? clojure.lang.ExceptionInfo
                   (metrics/validate-results inventory [row row])))
      (is (thrown? clojure.lang.ExceptionInfo
                   (metrics/validate-results
                    inventory [row (-> row (dissoc :raw :operations)
                                       (assoc :status :failed :reason "child failed"))]))))
    (testing "graph roots distinguish separate retained graphs"
      (let [graph (-> row (dissoc :operations)
                      (assoc :metric :graph :unit :bytes :root :result))]
        (is (= 2 (count (metrics/validate-results
                         inventory [graph (assoc graph :root :source-and-result)]))))
        (doseq [invalid [(dissoc graph :root) (assoc graph :root :unknown)]]
          (is (thrown? clojure.lang.ExceptionInfo
                       (metrics/validate-results inventory [invalid]))))))))

(deftest complete-coverage-test
  ;; Complete evidence includes explicit limits, but never silently missing forks.
  (let [inventory {:execution {:forks 5}
                   :configurations {:ordinary {:representations [:closed-record :defrecord]}}
                   :contracts {:sample {:metrics [:time :graph] :graph-roots [:result :source-and-result]}}
                   :cases [{:id :sample :configuration :ordinary :contract :sample}]}
        rows (vec (for [representation [:closed-record :defrecord]
                        fork [1 2 3 4 5]
                        root [nil :result :source-and-result]]
                    (cond-> {:case-id :sample :configuration :ordinary
                             :representation representation :fork fork
                             :metric (if root :graph :time)
                             :unit (if root :bytes :ns/op)
                             :status :unavailable :reason "Control not supported"}
                      root (assoc :root root))))]
    (testing "all roots and forks are required, independent of result order"
      (is (= rows (metrics/validate-complete-results inventory rows)))
      (is (= (vec (reverse rows))
             (metrics/validate-complete-results inventory (vec (reverse rows)))))
      (doseq [row rows]
        (let [partial (filterv #(not= row %) rows)
              error (try (metrics/validate-complete-results inventory partial)
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))]
          (is (= #{(select-keys row [:case-id :configuration :representation :fork :metric :root])}
                 (:missing error))))))
    (testing "complete coverage does not bypass metric or duplicate validation"
      (is (thrown? clojure.lang.ExceptionInfo
                   (metrics/validate-complete-results inventory (conj rows (first rows)))))
      (is (thrown? clojure.lang.ExceptionInfo
                   (metrics/validate-complete-results inventory (assoc-in rows [0 :unit] :bytes)))))
    (testing "explicit failures remain visible without a numeric result"
      (let [failed (assoc-in rows [0 :status] :failed)]
        (is (= failed (metrics/validate-complete-results inventory failed)))))))

(deftest fork-aggregation-test
  ;; Independent forks have equal weight, even when operation counts differ.
  (let [inventory {:execution {:forks 5}
                   :configurations {:ordinary {:representations [:closed-record]}}
                   :contracts {:sample {:metrics [:allocation]}}
                   :cases [{:id :sample :configuration :ordinary :contract :sample}]}
        rows (mapv (fn [fork operations]
                     {:case-id :sample :configuration :ordinary
                      :representation :closed-record :fork fork
                      :metric :allocation :unit :bytes/op :status :measured
                      :raw (* fork operations) :operations operations})
                   [1 2 3 4 5] [2 3 5 7 11])
        summary (first (metrics/aggregate-results inventory rows))]
    (testing "normalise before aggregation and report sample variation"
      (is (= 3 (:mean summary)))
      (is (= [1 5] [(:min summary) (:max summary)]))
      (is (= (Math/sqrt 2.5) (:sample-standard-deviation summary)))
      (is (= (metrics/validate-results inventory rows) (:forks summary)))
      (is (= [summary] (metrics/aggregate-results inventory (vec (reverse rows))))))
    (testing "missing measurements are not zero or a reduced sample"
      (doseq [status [:unavailable :failed]]
        (let [missing (-> (first rows) (dissoc :raw :operations)
                          (assoc :status status :reason "Tool unavailable"))
              result (first (metrics/aggregate-results inventory (assoc rows 0 missing)))]
          (is (= :unavailable (:status result)))
          (is (= missing (first (:forks result))))
          (is (not-any? #(contains? result %)
                        [:mean :min :max :sample-standard-deviation])))))
    (testing "partial evidence cannot be aggregated"
      (is (thrown? clojure.lang.ExceptionInfo
                   (metrics/aggregate-results inventory (pop rows)))))))
