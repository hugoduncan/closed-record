(ns valhalla.allocation-child
  "Experimental allocation capture using the verified Java counter boundary."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [closed-record.bench.child :as child]
            [closed-record.bench.transport :as transport])
  (:import Allocation))

(defn collect!
  "Collect inventory allocation rows with fixed operation and warmup budgets.
  Retain all warmup counts and five final batches. Setup and result checks are
  outside each counter boundary; no costs are subtracted. Pair order alternates."
  [inventory fork operations warmup-batches]
  (when-not (and (integer? fork) (<= 1 fork 5)
                 (pos-int? operations) (pos-int? warmup-batches))
    (throw (ex-info "Invalid allocation budgets or fork" {})))
  (into []
        (mapcat
         (fn [{:keys [id contract length operation]}]
           (let [representations (get-in inventory [:contracts contract :representations])
                 ordered (if (odd? fork) representations (vec (reverse representations)))]
             (mapv
              (fn [representation]
                (let [flat? (contains? #{:value-array :restricted-holder} representation)
                      sample #(Allocation/sample flat? (= :array contract)
                                                 (name operation) (int (or length 0))
                                                 (int operations))
                      warmup (mapv (fn [_] (sample)) (range warmup-batches))
                      batches (mapv (fn [_] (sample)) (range 5))]
                  {:case-id id :configuration :valhalla :representation representation
                   :fork fork :metric :allocation :unit :bytes/op :status :measured
                   :raw (reduce + batches) :operations (* 5 operations)
                   :allocation-samples {:operations operations :warmup warmup
                                        :batches batches :warmup-batches warmup-batches}}))
              ordered))))
        (filterv #(= :valhalla (:configuration %)) (:cases inventory))))

(defn -main
  "Write one fresh experimental child. Arguments: RESULT RUN REVISION FORK.
  Fixed diagnostic budgets: 1,024 operations, 20 warmup and five final batches."
  [& args]
  (when-not (= 4 (count args))
    (throw (ex-info "Usage: valhalla.allocation-child RESULT RUN REVISION FORK" {})))
  (let [[path run-id revision fork-text] args
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        association {:run-id run-id :revision revision :configuration :valhalla
                     :fork (Integer/parseInt fork-text)}]
    (transport/validate-envelope inventory association
                                 (assoc association :schema-version 1 :results []))
    (child/write-results! path inventory association
                          (collect! inventory (:fork association) 1024 20))))
