(ns valhalla.timing
  "Experimental timing collection; requires the pinned Valhalla fixtures."
  (:require [closed-record.bench.fork :as fork]
            [closed-record.bench.timing :as timing]
            [criterium.bench :as bench]
            [criterium.measured :as measured]
            [valhalla.reset-measured :as reset])
  (:import Operations StoreBatch))

(def stores-per-invocation 1024)

(defn operation
  "Prepare an opaque-input expression and untimed fixtures for one timing site.
  Stores reset before every timed 1,024-store traversal. Replacement leaves its source intact."
  [benchmark-case representation]
  (let [initial (Operations/point 17)
        replacement (Operations/point 23)
        length (:length benchmark-case)
        flat? (= representation :value-array)
        restricted? (= representation :restricted-holder)]
    (case (:contract benchmark-case)
      :array
      (let [array (if flat?
                    (Operations/valueArray length initial)
                    (Operations/referenceArray length initial))]
        (case (:operation benchmark-case)
          :construct (if flat?
                       (measured/expr (Operations/valueArray length initial))
                       (measured/expr (Operations/referenceArray length initial)))
          :read (measured/expr (Operations/arrayRead array 0))
          :store (let [batch (StoreBatch. flat? length stores-per-invocation)]
                   (reset/reset-measured
                    (measured/expr (.store batch))
                    (fn [_] (.reset batch))))))
      :field
      (let [source (if restricted?
                     (Operations/restrictedHolder initial)
                     (Operations/referenceHolder initial))
            payload (if (= :replace (:operation benchmark-case)) replacement initial)]
        (case (:operation benchmark-case)
          (:construct :replace)
          (if restricted?
            (measured/expr (Operations/restrictedHolder payload))
            (measured/expr (Operations/referenceHolder payload)))
          :read (if restricted?
                  (measured/expr (Operations/restrictedRead source))
                  (measured/expr (Operations/referenceRead source))))))))

(defn outcome
  "Retain the Criterium divisor and normalize store traversals to logical stores."
  [benchmark-case bench-data]
  (let [result (timing/outcome bench-data)
        store? (= :store (:operation benchmark-case))
        logical-count (if store? stores-per-invocation 1)]
    (assoc result
           :criterium-invocations (:operations result)
           :operations-per-invocation logical-count
           :operations (* (:operations result) logical-count))))

(defn collect!
  "Collect all experimental timing pairs once in a fresh JVM.
  Return raw batch estimates; per-traversal timer costs for stores are retained."
  [inventory fork-number limit-s]
  (let [plan (bench/options->bench-plan {:viewer :none :limit-time-s limit-s})]
    (into []
          (mapcat
           (fn [benchmark-case]
             (let [[left right :as representations]
                   (get-in inventory [:contracts (:contract benchmark-case) :representations])
                   run (fn [representation]
                         (let [expression (operation benchmark-case representation)]
                           (bench/bench-measured plan expression)
                           (merge (outcome benchmark-case (bench/last-bench))
                                  {:configuration :valhalla :fork fork-number
                                   :case-id (:id benchmark-case)
                                   :representation representation})))]
               (mapv (fn [representation result]
                       (assoc result :representation representation))
                     representations
                     (fork/in-order fork-number #(run left) #(run right))))))
          (filterv #(= :valhalla (:configuration %)) (:cases inventory)))))
