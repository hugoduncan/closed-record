(ns closed-record.bench.assoc-graph-run
  "Saved partial ordinary graph evidence, independent of measurement tools."
  (:require [closed-record.bench.assoc-graph-command :as command]
            [closed-record.bench.fork :as fork]
            [closed-record.bench.metric-table :as table]
            [closed-record.bench.graph-build :as build]
            [closed-record.bench.assoc-graph-result :as result]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport]))

(defn read-results
  "Read five saved ordinary graph forks without changing evidence.
  Require process success, matching associations, raw runtime observations,
  complete partial graph coverage, and declared agent hashes.
  This diagnostic does not establish full matrix coverage or acceptance."
  [directory]
  (let [{:keys [manifest captures]} (evidence/read-run directory)
        {:keys [inventory children metadata]} manifest
        measurement (:measurement metadata)]
    (when-not (and (= [1 2 3 4 5] (mapv :fork children))
                   (every? #(= :ordinary (:configuration %)) children)
                   (= :ordinary-assoc-graph (:scope measurement))
                   (false? (:acceptance? measurement)))
      (throw (ex-info "Invalid graph diagnostic manifest" {:manifest manifest})))
    (into []
          (mapcat
           (fn [[association {:keys [outcome raw-result]}]]
             (when-not (and (= association (:child outcome))
                            (evidence/successful-child? outcome))
               (throw (ex-info "Unsuccessful or misassociated graph child"
                               {:expected association :outcome outcome})))
             (let [envelope (transport/decode inventory association raw-result)]
               (runtime/validate-observation
                (get-in metadata [:runtimes (:configuration association)])
                (:runtime envelope))
               (result/validate-results inventory association
                                        (:graph-tool metadata)
                                        (:results envelope)))))
          (mapv vector children captures))))

(defn collect!
  "Verify the prepared build, then save and collect five ordinary graph forks.
  Children inherit this JVM's agent flags and classpath. Saved observations must
  match the prepared build before success. Never retry or replace evidence.
  Return exit 2 for unsuccessful children; validation and IO exceptions propagate.
  Exit 0 means partial diagnostic success, not memory-tool approval or acceptance."
  [directory inventory metadata run-id revision build-directory]
  (let [declared-tool (build/read-declaration build-directory)
        children (mapv (fn [number]
                         {:run-id run-id :revision revision
                          :configuration :ordinary :fork number})
                       (range 1 6))
        child-command (fn [association path]
                        (command/command inventory association path))]
    (child-command (first children) "result.edn")
    (evidence/create-run!
     directory inventory
     (assoc metadata :runtimes {:ordinary (runtime/observe)}
            :graph-tool declared-tool
            :measurement {:scope :ordinary-assoc-graph :acceptance? false})
     children)
    (let [outcomes (mapv #(fork/capture-process! directory % child-command)
                         (range (count children)))
          success? (every? evidence/successful-child? outcomes)]
      (when success?
        (read-results directory))
      {:exit (if success? 0 2) :outcomes outcomes})))

(defn render-saved
  "Render validated partial graph evidence without changing saved files.
  Summarise only ordinary assoc graph rows; do not imply matrix acceptance or memory-tool approval."
  [directory]
  (let [rows (read-results directory)
        inventory (get-in (evidence/read-run directory) [:manifest :inventory])
        partial-inventory (-> inventory
                              (update :cases #(filterv (fn [benchmark-case]
                                                         (and (= :assoc (:contract benchmark-case))
                                                              (= :ordinary (:configuration benchmark-case))))
                                                       %))
                              (assoc-in [:contracts :assoc :metrics] [:graph]))]
    (str "# Partial ordinary assoc graph diagnostic\n\n"
         "This report covers graph only, not full-matrix acceptance or memory-tool approval. "
         "Means weight the five forks equally. Range and sample SD are not confidence bounds.\n\n"
         (table/render partial-inventory rows))))
