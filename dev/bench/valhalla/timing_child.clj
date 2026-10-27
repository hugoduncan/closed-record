(ns valhalla.timing-child
  "Partial experimental timing collection and child evidence persistence."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [valhalla.timing :as timing]
            [closed-record.bench.child :as child]
            [closed-record.bench.transport :as transport]
            [closed-record.bench.timing-tool :as tool]
            [closed-record.bench.timing-budget :as budget]))

(defn write-results!
  "Measure the experimental pairs and write a validated child envelope.
  Use a fresh JVM and an absent result path for each call. The explicit time
  limit is a measurement budget, not an acceptance threshold. This partial
  envelope does not satisfy full-matrix coverage. Runtime metadata comes from
  the executing JVM; raw batch estimates and operation divisors are preserved."
  [path inventory association limit-s]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (when-not (= :valhalla (:configuration association))
    (throw (ex-info "Experimental timing requires Valhalla association" {})))
  (let [timing-budget (budget/declaration limit-s)
        provenance (tool/observe)
        rows (timing/collect! inventory (:fork association) (:limit-time-s timing-budget))]
    (child/write-results! path inventory association
                          (mapv #(assoc % :timing-tool provenance
                                        :timing-budget timing-budget) rows))))

(defn -main
  "Collect partial timing evidence with an explicit seconds budget per site.
  Arguments: result path, run ID, revision, fork, seconds. Exit 2 for invalid
  arguments or IO failure. Exit zero is not full-matrix acceptance."
  [& args]
  (let [failure
        (try
          (when-not (and (= 5 (count args))
                         (re-matches #"[1-9][0-9]*" (nth args 3)))
            (throw (ex-info "Usage: valhalla.timing-child RESULT RUN REVISION FORK SECONDS" {})))
          (let [[path run-id revision fork-text limit-text] args
                fork (Integer/parseInt fork-text)
                limit-s (Double/parseDouble limit-text)
                inventory (edn/read-string (slurp (io/resource "cases.edn")))]
            (write-results! path inventory
                            {:run-id run-id :revision revision
                             :configuration :valhalla :fork fork}
                            limit-s))
          nil
          (catch clojure.lang.ExceptionInfo error (.getMessage error))
          (catch NumberFormatException error (.getMessage error))
          (catch java.io.IOException error (.getMessage error)))]
    (when failure
      (binding [*out* *err*] (println failure))
      (System/exit 2))))
