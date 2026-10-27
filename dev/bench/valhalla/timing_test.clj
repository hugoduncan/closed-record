(ns valhalla.timing-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [criterium.measured :as measured]
            [valhalla.timing :as timing])
  (:import Operations))

(def inventory (edn/read-string (slurp "dev/bench/cases.edn")))

(deftest expression-results-test
  ;; Exercise the exact collector expressions, including repeated changed stores.
  (doseq [benchmark-case (:cases inventory)
          :when (= :valhalla (:configuration benchmark-case))
          representation (get-in inventory [:contracts (:contract benchmark-case) :representations])]
    (let [expression (timing/operation benchmark-case representation)
          state (measured/args expression)]
      (dotimes [_ 2]
        (let [[duration value] (measured/invoke expression state 3)
              expected (if (#{:store :replace} (:operation benchmark-case)) 23 17)]
          (is (<= 0 duration))
          (is (= expected
                 (if (= :read (:operation benchmark-case))
                   value
                   (if (= :array (:contract benchmark-case))
                     (Operations/arrayRead
                      (if (= :store (:operation benchmark-case))
                        (aget ^objects value 0) value) 0)
                     (if (= :restricted-holder representation)
                       (Operations/restrictedRead value)
                       (Operations/referenceRead value)))))))))))

(deftest collection-test
  ;; Run real estimation, warmup, batches and analysis for every inventory pair.
  (let [rows (timing/collect! inventory 0 1.0)]
    (is (= 18 (count rows)))
    (is (= 18 (count (set (map (juxt :case-id :representation) rows)))))
    (doseq [row rows]
      (is (= :measured (:status row)))
      (is (= :ns/op (:unit row)))
      (is (pos? (:raw row)))
      (is (pos-int? (:operations row))))))

(deftest store-divisor-test
  ;; Raw duration is unchanged; both levels of batching remain auditable.
  (let [data {:data {:bootstrap-stats
                     {:batch-size 7
                      :bootstrap {:elapsed-time {:mean {:point-estimate 7168.0}}}}}}]
    (is (= {:metric :time :unit :ns/op :status :measured
            :raw 7168.0 :operations 7168
            :criterium-invocations 7 :operations-per-invocation 1024}
           (timing/outcome {:operation :store} data)))
    (is (= 7 (:operations (timing/outcome {:operation :read} data))))))

(deftest store-batch-results-test
  ;; Generated normal and warmup states retain every changed array and tail.
  (doseq [benchmark-case (:cases inventory)
          :when (= :store (:operation benchmark-case))
          representation [:value-array :reference-array]]
    (let [expression (timing/operation benchmark-case representation)]
      (doseq [state [(measured/args expression) (measured/warmup-args expression)]
              invocation-count [1 3]]
        (let [[duration arrays] (measured/invoke expression state invocation-count)]
          (is (<= 0 duration))
          (is (= 1024 (alength ^objects arrays)))
          (is (every? (fn [array]
                        (and (= 23 (Operations/arrayRead array 0))
                             (= 17 (Operations/arrayRead array (dec (:length benchmark-case))))))
                      arrays)))))))
