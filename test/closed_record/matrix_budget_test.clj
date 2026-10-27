(ns closed-record.matrix-budget-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.matrix-budget :as budget]
            [closed-record.bench.metrics :as metrics]))

(defn- inventory [] (edn/read-string (slurp "dev/bench/cases.edn")))

(deftest fixed-matrix-accounting
  ;; Cross-check planned row counts against the independent coverage validator.
  (let [spec (inventory)
        result (budget/summarise spec 5)
        missing (try (metrics/validate-complete-results spec [])
                     (catch clojure.lang.ExceptionInfo error (:missing (ex-data error))))]
    (testing "the fixed matrix counts each representation, fork, and graph root"
      (is (= {:cases 17 :rows {:time 170 :allocation 110 :graph 210}
              :total-rows 490 :timing-limit-sum-s 850.0}
             (:total result)))
      (is (= (count missing) (get-in result [:total :total-rows])))
      (is (= (frequencies (mapv :metric missing)) (get-in result [:total :rows])))
      (is (= 400.0 (get-in result [:configurations :ordinary :timing-limit-sum-s])))
      (is (= 450.0 (get-in result [:configurations :valhalla :timing-limit-sum-s]))))
    (testing "explicit budgets and fork counts scale without changing case counts"
      (is (= 85.0 (get-in (budget/summarise spec 0.5) [:total :timing-limit-sum-s])))
      (is (= 98 (get-in (budget/summarise (assoc-in spec [:execution :forks] 1) 5)
                        [:total :total-rows]))))))

(deftest invalid-matrix-inputs
  ;; Reject accounting inputs that could silently undercount the planned work.
  (doseq [spec [(assoc-in (inventory) [:execution :forks] 0)
                (assoc (inventory) :cases [])
                (update (inventory) :cases conj (first (:cases (inventory))))
                (assoc-in (inventory) [:cases 0 :configuration] :unknown)
                (assoc-in (inventory) [:contracts :assoc :metrics] [:time :unknown])
                (assoc-in (inventory) [:contracts :assoc :graph-roots] [])
                (assoc-in (inventory) [:configurations :ordinary :representations] [])]]
    (is (thrown? clojure.lang.ExceptionInfo (budget/summarise spec 5))))
  (doseq [seconds [0 -1 ##NaN ##Inf nil]]
    (is (thrown? clojure.lang.ExceptionInfo (budget/summarise (inventory) seconds)))))
