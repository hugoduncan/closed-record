(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[closed-record.bench.transport :as transport]
         '[closed-record.bench.runtime :as runtime]
         '[closed-record.bench.timing-tool :as tool]
         '[closed-record.bench.timing-budget :as budget]
         '[closed-record.bench.metric-table :as table])

(let [directory "munera/open/019-space-time-performance/evidence/acceptance-01/experimental-timing"
      inventory (edn/read-string (slurp "dev/bench/cases.edn"))
      revision (str/trim (slurp (str directory "/revision.txt")))
      envelopes (mapv (fn [fork]
                        (assert (= "0" (str/trim (slurp (str directory "/fork-" fork ".exit"))))
                                "Unsuccessful timing child")
                        (transport/decode
                         inventory
                         {:run-id "acceptance-01-experimental-timing" :revision revision
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
    (budget/validate-observation {:limit-time-s 1.0} row)
    (let [benchmark-case (first (filterv #(= (:id %) (:case-id row)) (:cases inventory)))
          expected (if (= :store (:operation benchmark-case)) 1024 1)]
      (assert (= expected (:operations-per-invocation row)))
      (assert (pos-int? (:criterium-invocations row)))
      (assert (= (:operations row) (* expected (:criterium-invocations row))))))
  (println "# Experimental timing acceptance capture\n")
  (println "Five independent forks; one-second site budgets. No S-03 parity requirement applies to these external types.")
  (println "Stores traverse 1,024 distinct arrays; timer/input and traversal costs are included; slot resets are outside timing.\n")
  (print (table/render projection rows)))
