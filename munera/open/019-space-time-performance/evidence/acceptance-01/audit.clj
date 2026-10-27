;; Audit saved evidence only. Legacy statistics are not raw matrix samples.
(require '[clojure.edn :as edn]
         '[closed-record.bench.assoc-suite :as suite]
         '[closed-record.bench.legacy-transport :as legacy]
         '[closed-record.bench.metrics :as metrics]
         '[closed-record.bench.report :as report]
         '[closed-record.bench.timing-tool :as tool])

(let [directory "munera/open/019-space-time-performance/evidence/acceptance-01"
      inventory (edn/read-string (slurp "dev/bench/cases.edn"))
      ordinary (suite/read-results
                {:time (str directory "/ordinary/timing")
                 :allocation (str directory "/ordinary/allocation")
                 :graph (str directory "/ordinary/graph")})
      experimental (into []
                         (mapcat (fn [metric]
                                   ;; Existing report scripts check exits, runtime, tool,
                                   ;; budgets and raw divisors before inventory reporting.
                                   (let [path (str directory "/experimental-" metric)]
                                     (binding [*ns* (create-ns (symbol (str "acceptance-audit." metric)))]
                                       (refer 'clojure.core)
                                       (with-out-str (load-file (str path "/report.clj"))))
                                     (into [] (mapcat (fn [fork]
                                                        (:results (edn/read-string
                                                                   (slurp (str path "/fork-" fork ".edn"))))))
                                           (range 1 6)))))
                         ["timing" "allocation" "graph"])
      rows (into (into [] (mapcat :results) (vals ordinary)) experimental)
      projection (update inventory :cases
                         #(filterv (fn [item] (not= :legacy (:contract item))) %))
      validated (metrics/validate-complete-results projection rows)
      legacy-cases (filterv #(= :legacy (:contract %)) (:cases inventory))
      original (mapv (fn [fork]
                       (assert (zero? (:exit (edn/read-string
                                              (slurp (str directory "/s03/fork-" fork ".status.edn"))))))
                       (let [decoded (legacy/decode
                                      (slurp (str directory "/s03/fork-" fork ".edn"))
                                      (tool/observe))]
                         (assert (nil? (:error decoded)) (:error decoded))
                         (assert (= (mapv :expression legacy-cases)
                                    (mapv first (:result decoded))))
                         (:result decoded)))
                     (range 5))
      statuses (frequencies (mapv :status validated))]
  (assert (= 430 (count validated)))
  (assert (= {:measured 355 :unavailable 75} statuses))
  (doseq [row validated :when (= :unavailable (:status row))]
    (assert (= :graph (:metric row)))
    (assert (contains? #{:reference-array :reference-holder} (:representation row))))
  (assert (zero? (report/exit-code (report/fork-stats original))))
  (prn {:matrix-slots 490
        :raw-metric-slots (count validated)
        :raw-statuses statuses
        :legacy-statistic-slots (* 2 (reduce + (mapv count original)))
        :legacy-raw-batches :not-retained
        :s03 :pass
        :missing-supported 0
        :failed 0}))
