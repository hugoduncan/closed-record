(ns closed-record.bench.assoc-timing-result
  "Pure validation of partial ordinary assoc timing evidence."
  (:require [closed-record.bench.transport :as transport]
            [closed-record.bench.timing-tool :as tool]
            [closed-record.bench.timing-budget :as budget]))

(defn validate-results
  "Require complete measured ordinary assoc timing rows and matching provenance.
  Return raw rows unchanged. This partial diagnostic is not matrix acceptance.
  Declarations are required even when the supplied result collection is empty."
  [inventory association declared-tool declared-budget rows]
  (tool/declaration declared-tool)
  (budget/declaration (:limit-time-s declared-budget))
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results rows))
  (let [expected (into #{}
                       (mapcat (fn [benchmark-case]
                                 (mapv (fn [representation]
                                         [(:id benchmark-case) representation :time])
                                       (get-in inventory [:configurations :ordinary :representations]))))
                       (filterv #(and (= :assoc (:contract %))
                                      (= :ordinary (:configuration %)))
                                (:cases inventory)))
        actual (into #{} (map (juxt :case-id :representation :metric)) rows)]
    (when-not (and (= :ordinary (:configuration association))
                   (seq expected) (= expected actual)
                   (every? #(= :measured (:status %)) rows))
      (throw (ex-info "Incomplete ordinary assoc timing results"
                      {:expected expected :actual actual})))
    (doseq [row rows]
      (tool/validate-observation declared-tool (:timing-tool row))
      (budget/validate-observation declared-budget row))
    rows))
