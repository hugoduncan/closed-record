(require '[clojure.edn :as edn])

;; Inspect saved evidence, not new measurements. Run report.clj first for validation.
(let [directory "munera/open/019-space-time-performance/evidence/experimental-timing-01"
      rows (into []
                 (mapcat (fn [fork]
                           (:results
                            (edn/read-string
                             (slurp (str directory "/fork-" fork ".edn"))))))
                 (range 1 6))
      reads (filterv #(contains? #{:array/read-4 :array/read-1024 :field/read}
                                 (:case-id %)) rows)
      small-flat (filterv #(and (= :array/read-4 (:case-id %))
                                (= :value-array (:representation %))) reads)]
  (assert (= 90 (count rows)))
  (assert (= 30 (count reads)))
  (assert (= [1 2 3 4 5] (mapv :fork small-flat)))
  (assert (every? #(and (= :measured (:status %)) (= :ns/op (:unit %))
                        (pos? (:raw %)) (pos-int? (:operations %))) reads))
  (println "# Saved experimental read timing audit\n")
  (println "Raw is a bootstrap mean batch estimate in ns, not a single sample.")
  (println "Pair position follows the fixed alternating execution order, not row order.\n")
  (println "| Case | Representation | Fork | Pair position | Raw ns | Divisor | ns/op |")
  (println "| --- | --- | --- | --- | --- | --- | --- |")
  (doseq [{:keys [case-id representation fork raw operations]} reads]
    (let [left? (contains? #{:value-array :restricted-holder} representation)
          first? (= left? (odd? fork))]
      (println (format "| %s | %s | %d | %s | %.6f | %d | %.6f |"
                       case-id representation fork (if first? "first" "second")
                       (double raw) operations (double (/ raw operations)))))))
