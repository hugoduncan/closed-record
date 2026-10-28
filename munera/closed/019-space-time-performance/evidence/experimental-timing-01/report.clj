(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[closed-record.bench.transport :as transport]
         '[closed-record.bench.runtime :as runtime]
         '[closed-record.bench.timing-tool :as tool]
         '[closed-record.bench.timing-budget :as budget]
         '[closed-record.bench.metric-table :as table])

(let [directory "munera/open/019-space-time-performance/evidence/experimental-timing-01"
      inventory (edn/read-string (slurp "dev/bench/cases.edn"))
      revision (str/trim (slurp (str directory "/revision.txt")))
      envelopes (mapv (fn [fork]
                        (assert (= "0" (str/trim (slurp (str directory "/fork-" fork ".exit"))))
                                "Unsuccessful timing child")
                        (transport/decode
                         inventory
                         {:run-id "experimental-timing-01" :revision revision
                          :configuration :valhalla :fork fork}
                         (slurp (str directory "/fork-" fork ".edn"))))
                      (range 1 6))
      declared-runtime (:runtime (first envelopes))
      declared-tool (tool/observe)
      projection (-> inventory
                     (update :cases #(filterv (fn [c] (= :valhalla (:configuration c))) %))
                     (assoc-in [:contracts :array :metrics] [:time])
                     (assoc-in [:contracts :field :metrics] [:time]))
      rows (into [] (mapcat :results) envelopes)]
  (assert (= "27-jep401ea3+1-1" (:version declared-runtime)))
  (assert (not-any? #(str/starts-with? % "-javaagent:") (:flags declared-runtime)))
  (assert (= :local-correction (:distribution declared-tool)))
  (doseq [envelope envelopes]
    (runtime/validate-observation declared-runtime (:runtime envelope)))
  (doseq [row rows]
    (assert (= :measured (:status row)))
    (tool/validate-observation declared-tool (:timing-tool row))
    (budget/validate-observation {:limit-time-s 1.0} row))
  (println "# Experimental timing diagnostic\n")
  (println "Five independent forks; one-second site budgets. Not acceptance or S-03 parity.")
  (println "Stores include per-operation timer and opaque-input costs; reset is outside timing.\n")
  (print (table/render projection rows)))
