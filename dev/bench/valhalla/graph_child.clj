(ns valhalla.graph-child
  "Experimental graph publication with active flat-agent provenance."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [closed-record.bench.child :as child]
            [closed-record.bench.transport :as transport]
            [valhalla.graph :as graph])
  (:import FlatGraphProbe))

(defn write-results!
  "Write exclusive graph evidence, including unavailable reference roots.
  Observe the active agent before collection; callers must reconcile its hashes
  with the prepared artifact before using measured counts."
  [path inventory association]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (when-not (= :valhalla (:configuration association))
    (throw (ex-info "Experimental graphs require Valhalla" {:association association})))
  (let [identity (into {} (map (fn [[k v]] [(keyword k) v]))
                       (FlatGraphProbe/identity))]
    (child/write-results!
     path inventory association
     (mapv #(assoc % :graph-tool identity)
           (graph/collect! inventory (:fork association))))))

(defn -main
  "Write one child. Arguments: RESULT RUN REVISION FORK."
  [& args]
  (when-not (= 4 (count args))
    (throw (ex-info "Usage: valhalla.graph-child RESULT RUN REVISION FORK" {})))
  (let [[path run-id revision fork-text] args]
    (write-results! path (edn/read-string (slurp (io/resource "cases.edn")))
                    {:run-id run-id :revision revision :configuration :valhalla
                     :fork (Integer/parseInt fork-text)})))
