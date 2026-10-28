(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[closed-record.bench.transport :as transport]
         '[closed-record.bench.runtime :as runtime]
         '[closed-record.bench.metric-table :as table])

(let [directory "munera/open/019-space-time-performance/evidence/experimental-allocation-01"
      inventory (edn/read-string (slurp "dev/bench/cases.edn"))
      revision (str/trim (slurp (str directory "/revision.txt")))
      envelopes (mapv (fn [fork]
                        (assert (= "0" (str/trim (slurp (str directory "/fork-" fork ".exit"))))
                                "Unsuccessful allocation child")
                        (transport/decode
                         inventory
                         {:run-id "experimental-allocation-01" :revision revision
                          :configuration :valhalla :fork fork}
                         (slurp (str directory "/fork-" fork ".edn"))))
                      (range 1 6))
      declared-runtime (:runtime (first envelopes))
      projection (-> inventory
                     (update :cases #(filterv (fn [c] (= :valhalla (:configuration c))) %))
                     (assoc-in [:contracts :array :metrics] [:allocation])
                     (assoc-in [:contracts :field :metrics] [:allocation]))
      rows (into [] (mapcat :results) envelopes)]
  (assert (= "27-jep401ea3+1-1" (:version declared-runtime)))
  (assert (not-any? #(str/starts-with? % "-javaagent:") (:flags declared-runtime)))
  (doseq [envelope envelopes]
    (runtime/validate-observation declared-runtime (:runtime envelope)))
  (doseq [row rows]
    (let [{:keys [operations warmup batches warmup-batches]} (:allocation-samples row)]
      (assert (= :measured (:status row)))
      (assert (= 1024 operations))
      (assert (= 20 warmup-batches (count warmup)))
      (assert (= 5 (count batches)))
      (assert (every? #(and (integer? %) (not (neg? %))) (into warmup batches)))
      (assert (= (:raw row) (reduce + batches)))
      (assert (= (:operations row) (* operations (count batches))))))
  (println "# Experimental allocation diagnostic\n")
  (println "Five independent forks; 1,024 operations per batch, 20 warmup and five final batches.")
  (println "Raw counts include boundary costs. No subtraction. Not acceptance or graph ownership.\n")
  (print (table/render projection rows)))
