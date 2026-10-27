(ns closed-record.bench.assoc-allocation-run
  "Sequential parent orchestration for partial allocation evidence."
  (:require [closed-record.bench.assoc-allocation-child :as child]
            [closed-record.bench.assoc-allocation-command :as command]
            [closed-record.bench.transport :as transport]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.fork :as fork]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.metric-table :as metric-table]))

(defn read-results
  "Read and validate a saved five-fork ordinary allocation diagnostic.
  Require matching child associations, successful processes and runtimes, complete
  allocation rows, and declared budgets. Return raw rows without changing files.
  This partial evidence does not establish complete matrix acceptance."
  [directory]
  (let [{:keys [manifest captures]} (evidence/read-run directory)
        {:keys [inventory children metadata]} manifest
        measurement (:measurement metadata)]
    (when-not (and (= 5 (count children))
                   (= [1 2 3 4 5] (mapv :fork children))
                   (every? #(= :ordinary (:configuration %)) children)
                   (apply = (mapv :run-id children))
                   (apply = (mapv :revision children))
                   (= :ordinary-assoc-allocation (:scope measurement))
                   (false? (:acceptance? measurement)))
      (throw (ex-info "Invalid allocation diagnostic manifest" {:manifest manifest})))
    (into []
          (mapcat
           (fn [[association {:keys [outcome raw-result]}]]
             (when-not (and (= association (:child outcome))
                            (evidence/successful-child? outcome))
               (throw (ex-info "Unsuccessful or misassociated allocation child"
                               {:expected association :outcome outcome})))
             (let [envelope (transport/decode inventory association raw-result)]
               (runtime/validate-observation
                (get-in metadata [:runtimes (:configuration association)])
                (:runtime envelope))
               (child/validate-budget
                (child/validate-results inventory association (:results envelope))
                measurement))))
          (mapv vector children captures))))

(defn collect!
  "Save a manifest, then collect five ordinary allocation children in order.
  Budgets and invocation metadata are explicit. Return all process outcomes and
  exit 0 only when every child passes transport/runtime checks; otherwise exit 2.
  Neither exit 0 nor these partial rows establish complete matrix acceptance.
  Exceptions propagate with existing evidence retained. Never overwrite a run."
  [directory inventory metadata run-id revision operations warmup-batches]
  (let [children (mapv (fn [number]
                         {:run-id run-id :revision revision
                          :configuration :ordinary :fork number})
                       (range 1 6))
        child-command (fn [association path]
                        (command/command inventory association path operations warmup-batches))]
    ;; Validate budgets before creating a manifest or consuming an evidence slot.
    (child-command (first children) "result.edn")
    (evidence/create-run!
     directory inventory
     (assoc metadata
            :runtimes {:ordinary (runtime/observe)}
            :measurement {:scope :ordinary-assoc-allocation
                          :acceptance? false :operations operations
                          :warmup-batches warmup-batches})
     children)
    (let [outcomes (mapv #(fork/capture-process! directory % child-command)
                         (range (count children)))]
      (when (every? evidence/successful-child? outcomes)
        (read-results directory))
      {:exit (if (every? evidence/successful-child? outcomes) 0 2)
       :outcomes outcomes})))

(defn render-saved
  "Validate a saved allocation diagnostic and return its raw control table.
  This read-only report does not require or claim complete matrix coverage.
  Invalid process, runtime, coverage, or budget evidence throws before rendering."
  [directory]
  (let [results (read-results directory)]
    (str "# Partial ordinary assoc allocation diagnostic\n\n"
         "This report is not complete matrix or acceptance evidence.\n"
         (metric-table/render-allocation-controls results))))
