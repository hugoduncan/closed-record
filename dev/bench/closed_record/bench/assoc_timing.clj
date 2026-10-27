(ns closed-record.bench.assoc-timing
  "Literal ordinary assoc timing sites. Load only with the benchmark alias."
  (:require [closed-record.bench.fixtures :as fixtures]
            [closed-record.bench.fork :as fork]
            [closed-record.bench.timing :as timing]
            [closed-record.bench.transport :as transport]
            [criterium.bench :as bench]))

(defmacro ^:private measure
  [source key replacement limit-s]
  `(do
     (bench/bench ~(list 'clojure.core/assoc source key replacement)
                  :viewer :none :limit-time-s ~limit-s)
     (timing/outcome (bench/last-bench))))

(defn collect!
  "Measure the fixed width-3/16 pairs once per child JVM.
  Keep setup outside timing and alternate pair order by fork. Return raw time
  rows, not parity decisions. The caller must use a fresh JVM for each call;
  repeated execution of a Criterium call site is not supported."
  [inventory association limit-s]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (let [cases (filterv #(= :assoc (:contract %)) (:cases inventory))]
    (when-not (and (= :ordinary (:configuration association))
                   (= [[:assoc/width-3 3 :ordinary :assoc]
                       [:assoc/width-16 16 :ordinary :assoc]]
                      (mapv (juxt :id :width :configuration :operation) cases))
                   (number? limit-s) (pos? limit-s)
                   (Double/isFinite (double limit-s)))
      (throw (ex-info "Invalid ordinary assoc timing configuration"
                      {:association association :limit-s limit-s :cases cases})))
    (let [contract (get-in inventory [:contracts :assoc])
          small (fixtures/assoc-pair contract (first cases))
          large (fixtures/assoc-pair contract (second cases))
          key (:key small)
          replacement (:replacement small)
          runs [[#(let [x (:closed-record small)] (measure x key replacement limit-s))
                 #(let [x (:defrecord small)] (measure x key replacement limit-s))]
                [#(let [x (:closed-record large)] (measure x key replacement limit-s))
                 #(let [x (:defrecord large)] (measure x key replacement limit-s))]]
          rows (into []
                     (mapcat (fn [benchmark-case [closed-run record-run]]
                               (mapv (fn [representation outcome]
                                       (merge outcome
                                              (select-keys association [:configuration :fork])
                                              {:case-id (:id benchmark-case)
                                               :representation representation}))
                                     [:closed-record :defrecord]
                                     (fork/in-order (:fork association) closed-run record-run)))
                             cases runs))]
      (transport/validate-envelope inventory association
                                   (assoc association :schema-version 1 :results rows))
      rows)))
