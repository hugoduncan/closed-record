(require '[clojure.edn :as edn]
         '[criterium.measured :as measured]
         '[valhalla.timing :as timing])

(defn distribution
  "Retain counts of raw clock durations without subtracting overhead."
  [sample!]
  (frequencies (repeatedly 100000 sample!)))

(let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))
      cases (filterv #(and (= :array (:contract %))
                           (= :store (:operation %))) (:cases inventory))]
  (prn {:java (System/getProperty "java.runtime.version")
        :clock (distribution #(let [start (System/nanoTime)]
                                (- (System/nanoTime) start)))
        :stores
        (mapv (fn [[benchmark-case representation]]
                (let [expression (timing/operation benchmark-case representation)
                      state (measured/args expression)]
                  (dotimes [_ 100000] (measured/invoke expression state 1))
                  {:case-id (:id benchmark-case)
                   :representation representation
                   :durations-ns
                   (distribution #(first (measured/invoke expression state 1)))}))
              (for [benchmark-case cases
                    representation [:value-array :reference-array]]
                [benchmark-case representation]))}))
