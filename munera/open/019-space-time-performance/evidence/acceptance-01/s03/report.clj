(require '[clojure.edn :as edn]
         '[closed-record.bench.legacy-transport :as transport]
         '[closed-record.bench.report :as report]
         '[closed-record.bench.timing-tool :as tool])

(let [directory "munera/open/019-space-time-performance/evidence/acceptance-01/s03"
      declaration (tool/observe)
      results (mapv (fn [fork]
                      (assert (zero? (:exit (edn/read-string
                                             (slurp (str directory "/fork-" fork ".status.edn"))))))
                      (let [decoded (transport/decode
                                     (slurp (str directory "/fork-" fork ".edn"))
                                     declaration)]
                        (assert (nil? (:error decoded)) (:error decoded))
                        (:result decoded)))
                    (range report/fork-count))
      stats (report/fork-stats results)]
  (assert (= :local-correction (:distribution declaration)))
  (println (report/table (mapv #(apply report/row %) stats)))
  (println "S-03 exit code:" (report/exit-code stats)))
