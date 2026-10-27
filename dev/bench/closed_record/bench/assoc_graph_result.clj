(ns closed-record.bench.assoc-graph-result
  "Pure validation of partial ordinary assoc graph evidence."
  (:require [closed-record.bench.graph :as graph]
            [closed-record.bench.graph-tool :as tool]
            [closed-record.bench.transport :as transport]))

(defn validate-results
  "Require complete measured ordinary assoc graphs and matching agent hashes.
  Check raw counts and shared source/result/union counts across roots. Return
  rows unchanged. The caller must obtain declared hashes from a verified build;
  this partial gate does not approve the tool or establish matrix acceptance."
  [inventory association declared-tool rows]
  (tool/declaration declared-tool)
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results rows))
  (let [expected (into #{}
                       (mapcat (fn [benchmark-case]
                                 (into [] (mapcat (fn [representation] (mapv (fn [root] [(:id benchmark-case) representation :graph root]) (get-in inventory [:contracts :assoc :graph-roots])))) (get-in inventory [:configurations :ordinary :representations]))))
                       (filterv #(and (= :assoc (:contract %))
                                      (= :ordinary (:configuration %)))
                                (:cases inventory)))
        actual (into #{} (map (juxt :case-id :representation :metric :root)) rows)]
    (when-not (and (= :ordinary (:configuration association))
                   (seq expected) (= expected actual)
                   (every? #(= :measured (:status %)) rows))
      (throw (ex-info "Incomplete ordinary assoc graph results"
                      {:expected expected :actual actual})))
    (doseq [row rows]
      (tool/validate-observation declared-tool (:graph-tool row)))
    (doseq [[identity group] (group-by (juxt :case-id :representation) rows)]
      (let [{:keys [source-bytes result-bytes union-bytes]} (first group)
            derived (graph/outcomes source-bytes result-bytes union-bytes)
            fields [:root :raw :source-bytes :result-bytes :union-bytes]]
        (when-not (= (into #{} (map #(select-keys % fields)) derived)
                     (into #{} (map #(select-keys % fields)) group))
          (throw (ex-info "Inconsistent ordinary assoc graph counts"
                          {:identity identity})))))
    rows))
