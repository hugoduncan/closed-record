(ns closed-record.bench.assoc-graph
  "Agent-backed ordinary assoc graphs. Load only with RecordGraphProbe available."
  (:require [closed-record.bench.fixtures :as fixtures]
            [closed-record.bench.graph :as graph]
            [closed-record.bench.metrics :as metrics])
  (:import [java.util Set]
           RecordGraphProbe))

(defn collect!
  "Return raw graph rows for one fixed ordinary assoc representation.
  Count physical fields and shared Long leaves with the validated bounded walker.
  This is reachable size, not allocation or dominator retention. The agent must
  be installed; unsupported reachable objects fail rather than produce estimates."
  [inventory benchmark-case representation fork]
  (when-not (and (= :assoc (:contract benchmark-case))
                 (= :ordinary (:configuration benchmark-case))
                 (some #{benchmark-case} (:cases inventory)))
    (throw (ex-info "Unsupported assoc graph case" {:case benchmark-case})))
  (let [identity {:case-id (:id benchmark-case)
                  :configuration :ordinary :representation representation :fork fork}
        _ (metrics/validate-results
           inventory [(merge identity {:metric :graph :unit :bytes :root :result
                                       :status :unavailable :reason "Preflight"})])
        {:keys [key replacement] :as fixture}
        (fixtures/assoc-pair (get-in inventory [:contracts :assoc]) benchmark-case)
        source (get fixture representation)
        result (assoc source key replacement)
        types (Set/of (class source))
        source-bytes (RecordGraphProbe/graphBytes types (object-array [source]))
        result-bytes (RecordGraphProbe/graphBytes types (object-array [result]))
        union-bytes (RecordGraphProbe/graphBytes types (object-array [source result]))
        rows (mapv #(merge identity %)
                   (graph/outcomes source-bytes result-bytes union-bytes))]
    (metrics/validate-results inventory rows)
    rows))
