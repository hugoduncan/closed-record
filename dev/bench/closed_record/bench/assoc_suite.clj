(ns closed-record.bench.assoc-suite
  "Compatibility checks for separately collected ordinary assoc diagnostics."
  (:require [closed-record.bench.assoc-allocation-run :as allocation]
            [closed-record.bench.assoc-graph-run :as graph]
            [closed-record.bench.assoc-timing-run :as timing]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.metric-table :as table]))

(def ^:private scopes
  {:time :ordinary-assoc-timing
   :allocation :ordinary-assoc-allocation
   :graph :ordinary-assoc-graph})

(defn validate-manifests
  "Check compatibility of three already validated diagnostic manifests.
  Require matching inventory, revision, machine, and Java runtime identity.
  Keep metric-specific flags and run IDs separate. Reject agents in timing.
  Return the manifests unchanged; this does not validate captures or acceptance."
  [manifests]
  (when-not (= (set (keys scopes)) (set (keys manifests)))
    (throw (ex-info "Require time, allocation, and graph manifests" {})))
  (doseq [[metric scope] scopes]
    (let [{:keys [children metadata]} (get manifests metric)]
      (when-not (and (= [1 2 3 4 5] (mapv :fork children))
                     (every? #(= :ordinary (:configuration %)) children)
                     (= scope (get-in metadata [:measurement :scope]))
                     (false? (get-in metadata [:measurement :acceptance?]))
                     (seq (:machine metadata)))
        (throw (ex-info "Invalid ordinary diagnostic suite member" {:metric metric})))))
  (let [identities (mapv (fn [metric]
                           (let [{:keys [inventory children metadata]} (get manifests metric)]
                             {:inventory inventory
                              :revision (:revision (first children))
                              :machine (:machine metadata)
                              :runtime (select-keys (get-in metadata [:runtimes :ordinary])
                                                    [:version :java-home])}))
                         [:time :allocation :graph])
        flags (get-in manifests [:time :metadata :runtimes :ordinary :flags])]
    (when-not (apply = identities)
      (throw (ex-info "Incompatible ordinary diagnostic runs" {:identities identities})))
    (when (some #(re-find #"^-((javaagent|agentlib|agentpath):|Xrun)" %) flags)
      (throw (ex-info "Timing diagnostics must run without agents" {:flags flags}))))
  manifests)

(defn read-results
  "Read compatible saved ordinary diagnostics without changing evidence.
  Require exactly :time, :allocation, and :graph directories. Preserve each
  source manifest and its validated raw rows. This is not acceptance evidence.
  Read only completed runs; concurrent changes to evidence are not supported."
  [directories]
  (when-not (= (set (keys scopes)) (set (keys directories)))
    (throw (ex-info "Require time, allocation, and graph directories" {})))
  (let [readers {:time timing/read-results
                 :allocation allocation/read-results
                 :graph graph/read-results}
        sources (into {}
                      (map (fn [[metric reader]]
                             (let [directory (get directories metric)
                                   rows (reader directory)
                                   manifest (:manifest (evidence/read-run directory))]
                               [metric {:manifest manifest :results rows}])))
                      readers)]
    (validate-manifests (update-vals sources :manifest))
    sources))

(defn render-saved
  "Render compatible ordinary diagnostics without changing saved evidence.
  Preserve source manifests in labelled EDN blocks. This report does not establish
  experimental coverage, timing parity, memory-tool approval, or acceptance."
  [directories]
  (let [sources (read-results directories)
        metrics [:time :allocation :graph]
        inventory (-> (get-in sources [:time :manifest :inventory])
                      (update :cases #(filterv (fn [benchmark-case]
                                                 (and (= :assoc (:contract benchmark-case))
                                                      (= :ordinary (:configuration benchmark-case))))
                                               %)))
        rows (into [] (mapcat #(get-in sources [% :results])) metrics)]
    (str "# Partial ordinary assoc diagnostics\n\n"
         "Time, allocation, and graph were collected in separate runs. "
         "This report does not establish experimental coverage, timing parity, "
         "memory-tool approval, or full-matrix acceptance. "
         "Means weight the five forks equally. Range and sample SD are not confidence bounds.\n\n"
         (table/render inventory rows)
         (table/render-allocation-controls (get-in sources [:allocation :results]))
         "\n## Source manifests\n\n"
         (apply str (mapv (fn [metric]
                            (str "### " (name metric) "\n\n```edn\n"
                                 (pr-str (get-in sources [metric :manifest])) "\n```\n\n"))
                          metrics)))))
