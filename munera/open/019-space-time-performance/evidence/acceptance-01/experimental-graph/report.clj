(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[closed-record.bench.transport :as transport]
         '[closed-record.bench.runtime :as runtime]
         '[closed-record.bench.graph-tool :as tool]
         '[closed-record.bench.metric-table :as table])

(let [directory "munera/open/019-space-time-performance/evidence/acceptance-01/experimental-graph"
      inventory (edn/read-string (slurp "dev/bench/cases.edn"))
      revision (str/trim (slurp (str directory "/revision.txt")))
      envelopes (mapv (fn [fork]
                        (assert (= "0" (str/trim (slurp (str directory "/fork-" fork ".exit"))))
                                "Unsuccessful graph child")
                        (transport/decode
                         inventory
                         {:run-id "acceptance-01-experimental-graph" :revision revision
                          :configuration :valhalla :fork fork}
                         (slurp (str directory "/fork-" fork ".edn"))))
                      (range 1 6))
      declared-runtime (:runtime (first envelopes))
      projection (-> inventory
                     (update :cases #(filterv (fn [c] (= :valhalla (:configuration c))) %))
                     (assoc-in [:contracts :array :metrics] [:graph])
                     (assoc-in [:contracts :field :metrics] [:graph]))
      rows (into [] (mapcat :results) envelopes)]
  (assert (= "27-jep401ea3+1-1" (:version declared-runtime)))
  (assert (some #(str/starts-with? % "-javaagent:") (:flags declared-runtime)))
  (doseq [envelope envelopes]
    (runtime/validate-observation declared-runtime (:runtime envelope)))
  (let [declared-tool (edn/read-string (slurp (str directory "/graph-tool.edn")))]
    (doseq [row rows]
      (tool/validate-observation declared-tool (:graph-tool row))
      (if (contains? #{:reference-array :reference-holder} (:representation row))
        (assert (= :unavailable (:status row)))
        (assert (= :measured (:status row))))))
  (println "# Experimental graph acceptance capture\n")
  (println "Five independent forks. Flat payloads belong to container layout.")
  (println "Reference graphs are unavailable: physical value-box identity cannot be established.")
  (println "See ../value-identity and ../experimental-graph-operations. No space ratio is available.\n")
  (print (table/render projection rows)))
