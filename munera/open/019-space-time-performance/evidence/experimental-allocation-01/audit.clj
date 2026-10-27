(require '[clojure.edn :as edn])

;; Audit saved counts only. Run report.clj first for transport and runtime checks.
(let [directory "munera/open/019-space-time-performance/evidence/experimental-allocation-01"
      rows (into []
                 (mapcat (fn [fork]
                           (:results
                            (edn/read-string
                             (slurp (str directory "/fork-" fork ".edn"))))))
                 (range 1 6))
      varying (filterv (fn [row]
                         (let [{:keys [warmup batches]} (:allocation-samples row)]
                           (not (apply = (into warmup batches)))))
                       rows)
      reads (filterv #(and (= :field/read (:case-id %))
                           (= :restricted-holder (:representation %))) rows)]
  (assert (= 90 (count rows)))
  (assert (= 5 (count varying)))
  (assert (= [0 24 0 24 0] (mapv #(/ (:raw %) (:operations %)) reads)))
  (assert (every? (fn [row]
                    (let [{:keys [warmup batches]} (:allocation-samples row)]
                      (apply = (into warmup batches)))) reads))
  (assert (= 1 (count (filterv #(not (apply = (get-in % [:allocation-samples :batches]))) rows))))
  (println "# Saved allocation batch audit\n")
  (println "Counts below are bytes per 1,024-operation batch, without subtraction.\n")
  (doseq [row (into varying reads)]
    (println (str "## Fork " (:fork row) " — " (:case-id row) " / " (:representation row)))
    (println "\nWarmup:" (pr-str (get-in row [:allocation-samples :warmup])))
    (println "\nFinal:" (pr-str (get-in row [:allocation-samples :batches])) "\n")))
