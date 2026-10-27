(ns closed-record.bench.run-results
  "Pure complete-run assembly from captured child evidence."
  (:require
   [closed-record.bench.evidence :as evidence]
   [closed-record.bench.assoc-allocation-child :as allocation-child]
   [closed-record.bench.assoc-allocation-result :as allocation-result]
   [closed-record.bench.metrics :as metrics]
   [closed-record.bench.graph-tool :as graph-tool]
   [closed-record.bench.metric-table :as metric-table]
   [closed-record.bench.transport :as transport]
   [closed-record.bench.runtime :as runtime]
   [closed-record.bench.timing-tool :as timing-tool]
   [closed-record.bench.timing-budget :as timing-budget]))

(defn assemble
  "Return complete raw results in planned child order, before aggregation.
  Captures contain :outcome and :raw-result and must match planned associations.
  Reject failed processes, duplicate children, and incomplete metric coverage.
  Explicit failed/unavailable metrics remain evidence, not measurement success."
  [inventory children captures]
  (when-not (and (vector? children) (seq children)
                 (= (count children) (count (distinct children)))
                 (apply = (mapv :run-id children))
                 (apply = (mapv :revision children))
                 (vector? captures) (= (count children) (count captures)))
    (throw (ex-info "Invalid run capture set" {:children children})))
  (let [results
        (into []
              (mapcat (fn [[child {:keys [outcome raw-result]}]]
                        (when-not (and (= child (:child outcome))
                                       (evidence/successful-child? outcome))
                          (throw (ex-info "Unsuccessful or misassociated child"
                                          {:expected child :outcome outcome})))
                        (:results (transport/decode inventory child raw-result))))
              (mapv vector children captures))]
    (metrics/validate-complete-results inventory results)
    results))

(defn- assemble-saved
  [{:keys [manifest captures]}]
  (let [{:keys [inventory children metadata]} manifest
        results (assemble inventory children captures)]
    (doseq [[child capture] (mapv vector children captures)]
      (runtime/validate-observation
       (get-in metadata [:runtimes (:configuration child)])
       (:runtime (transport/decode inventory child (:raw-result capture)))))
    (doseq [row results
            :when (and (= :time (:metric row)) (= :measured (:status row)))]
      (timing-tool/validate-observation (:timing-tool metadata) (:timing-tool row))
      (timing-budget/validate-observation (:measurement metadata) row))
    (doseq [row results
            :when (and (= :graph (:metric row)) (= :measured (:status row)))]
      (graph-tool/validate-observation (:graph-tool metadata) (:graph-tool row)))
    (doseq [row results
            :when (contains? row :allocation-controls)]
      (allocation-result/stability row)
      (allocation-child/validate-budget [row] (:measurement metadata)))
    results))

(defn read-results
  "Read saved captures and return complete raw results, or throw.
  Does not write summaries or alter evidence when a run is incomplete."
  [directory]
  (assemble-saved (evidence/read-run directory)))

(defn render-saved
  "Return a Markdown metric table from a complete saved run, or throw.
  Apply process, association, runtime, timing-tool, timing-budget, and coverage gates before rendering.
  Does not write a report or change saved evidence. Missing metrics remain explicit."
  [directory]
  (let [saved (evidence/read-run directory)
        inventory (get-in saved [:manifest :inventory])
        results (assemble-saved saved)]
    (str (metric-table/render inventory results)
         (metric-table/render-allocation-controls results))))
