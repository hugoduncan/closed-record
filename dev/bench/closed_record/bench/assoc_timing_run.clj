(ns closed-record.bench.assoc-timing-run
  "Saved partial ordinary timing evidence, independent of measurement tools."
  (:require [closed-record.bench.assoc-timing-command :as command]
            [closed-record.bench.assoc-timing-result :as result]
            [closed-record.bench.fork :as fork]
            [closed-record.bench.metric-table :as table]
            [closed-record.bench.timing-budget :as budget]
            [closed-record.bench.timing-tool :as tool]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport]))

(defn read-results
  "Read five saved ordinary timing forks without changing evidence.
  Require process success, matching associations, raw runtime observations,
  complete partial timing coverage, and declared tool and per-site budgets.
  This diagnostic does not establish full matrix coverage or acceptance."
  [directory]
  (let [{:keys [manifest captures]} (evidence/read-run directory)
        {:keys [inventory children metadata]} manifest
        measurement (:measurement metadata)]
    (when-not (and (= [1 2 3 4 5] (mapv :fork children))
                   (every? #(= :ordinary (:configuration %)) children)
                   (= :ordinary-assoc-timing (:scope measurement))
                   (false? (:acceptance? measurement)))
      (throw (ex-info "Invalid timing diagnostic manifest" {:manifest manifest})))
    (into []
          (mapcat
           (fn [[association {:keys [outcome raw-result]}]]
             (when-not (and (= association (:child outcome))
                            (evidence/successful-child? outcome))
               (throw (ex-info "Unsuccessful or misassociated timing child"
                               {:expected association :outcome outcome})))
             (let [envelope (transport/decode inventory association raw-result)]
               (runtime/validate-observation
                (get-in metadata [:runtimes (:configuration association)])
                (:runtime envelope))
               (result/validate-results inventory association
                                        (:timing-tool metadata) measurement
                                        (:results envelope)))))
          (mapv vector children captures))))

(defn collect!
  "Save declarations, then collect five ordinary timing children in order.
  Require explicit tool identity and per-site seconds before creating evidence.
  Validate saved rows before success. Return exit 2 for unsuccessful children;
  validation and IO exceptions propagate with evidence retained. Never retry.
  Exit 0 establishes only a valid partial diagnostic, not acceptance."
  [directory inventory metadata run-id revision limit-s]
  (let [measurement (assoc (budget/declaration limit-s)
                           :scope :ordinary-assoc-timing :acceptance? false)
        declared-tool (tool/declaration (:timing-tool metadata))
        children (mapv (fn [number]
                         {:run-id run-id :revision revision
                          :configuration :ordinary :fork number})
                       (range 1 6))
        child-command (fn [association path]
                        (command/command inventory association path limit-s))]
    (child-command (first children) "result.edn")
    (evidence/create-run!
     directory inventory
     (assoc metadata :runtimes {:ordinary (runtime/observe)}
            :timing-tool declared-tool :measurement measurement)
     children)
    (let [outcomes (mapv #(fork/capture-process! directory % child-command)
                         (range (count children)))
          success? (every? evidence/successful-child? outcomes)]
      (when success?
        (read-results directory))
      {:exit (if success? 0 2) :outcomes outcomes})))

(defn render-saved
  "Render validated partial timing evidence without changing saved files.
  Summarise only ordinary assoc time rows; do not imply matrix acceptance or parity."
  [directory]
  (let [rows (read-results directory)
        inventory (get-in (evidence/read-run directory) [:manifest :inventory])
        partial-inventory (-> inventory
                              (update :cases #(filterv (fn [benchmark-case]
                                                         (and (= :assoc (:contract benchmark-case))
                                                              (= :ordinary (:configuration benchmark-case))))
                                                       %))
                              (assoc-in [:contracts :assoc :metrics] [:time]))]
    (str "# Partial ordinary assoc timing diagnostic\n\n"
         "This report covers timing only, not full-matrix acceptance or S-03 parity. "
         "Means weight the five forks equally. Range and sample SD are not confidence bounds.\n\n"
         (table/render partial-inventory rows))))
