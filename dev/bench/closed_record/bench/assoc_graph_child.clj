(ns closed-record.bench.assoc-graph-child
  "Partial ordinary graph persistence. Requires the isolated graph agent."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [closed-record.bench.assoc-graph :as graph]
            [closed-record.bench.child :as child]
            [closed-record.bench.transport :as transport])
  (:import RecordGraphProbe))

(defn write-results!
  "Collect both ordinary assoc widths and write an exclusive child envelope.
  Validate association before graph collection. Alternate representation order
  by fork. Retain active agent resource identity on each row. Declared build
  reconciliation and full-matrix coverage remain required before acceptance."
  [path inventory association]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (when-not (= :ordinary (:configuration association))
    (throw (ex-info "Assoc graphs require the ordinary configuration"
                    {:association association})))
  (let [agent-identity (into {} (map (fn [[k v]] [(keyword k) v]))
                             (RecordGraphProbe/identity))
        cases (filterv #(and (= :assoc (:contract %))
                             (= :ordinary (:configuration %)))
                       (:cases inventory))
        representations (if (odd? (:fork association))
                          [:closed-record :defrecord]
                          [:defrecord :closed-record])]
    (when (empty? cases)
      (throw (ex-info "Missing ordinary assoc graph cases" {})))
    (child/write-results!
     path inventory association
     (into []
           (mapcat (fn [benchmark-case]
                     (into []
                           (mapcat #(mapv (fn [row] (assoc row :graph-tool agent-identity))
                                          (graph/collect! inventory benchmark-case %
                                                          (:fork association))))
                           representations)))
           cases))))

(defn -main
  "Write partial graph evidence in an instrumented JVM.
  Arguments: result path, run ID, revision, fork. Exit 2 for invalid arguments
  or IO failure. Exit zero does not establish agent provenance or acceptance."
  [& args]
  (let [failure
        (try
          (when-not (and (= 4 (count args))
                         (re-matches #"[1-9][0-9]*" (nth args 3)))
            (throw (ex-info "Usage: assoc-graph-child RESULT RUN REVISION FORK" {})))
          (let [[path run-id revision fork-text] args
                inventory (edn/read-string (slurp (io/resource "cases.edn")))]
            (write-results! path inventory
                            {:run-id run-id :revision revision
                             :configuration :ordinary
                             :fork (Integer/parseInt fork-text)}))
          nil
          (catch clojure.lang.ExceptionInfo error (.getMessage error))
          (catch NumberFormatException error (.getMessage error))
          (catch java.io.IOException error (.getMessage error)))]
    (when failure
      (binding [*out* *err*] (println failure))
      (System/exit 2))))
