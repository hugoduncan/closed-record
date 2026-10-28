(ns closed-record.bench-assoc-allocation-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-allocation :as assoc-allocation]
            [closed-record.bench.fixtures :as fixtures]))

(deftest escaping-assoc-batch-test
  ;; Exercise real inventory records and counters, not fixed allocation guesses.
  (let [{:keys [contracts cases]} (edn/read-string (slurp (io/resource "cases.edn")))]
    (doseq [benchmark-case (filterv #(= :assoc (:contract %)) cases)
            representation [:closed-record :defrecord]
            operations [1 17]]
      (testing (str (:id benchmark-case) " " representation " " operations)
        (let [fixture (fixtures/assoc-pair (:assoc contracts) benchmark-case)
              source (get fixture representation)
              original (into {} source)
              key (:key fixture)
              replacement (:replacement fixture)
              {:keys [results outcome thread-id]}
              (assoc-allocation/collect! source key replacement operations)]
          (is (= operations (alength ^objects results) (:operations outcome)))
          (is (= :allocation (:metric outcome)))
          (is (= :bytes/op (:unit outcome)))
          (is (= :measured (:status outcome)))
          (is (pos? (:raw outcome)))
          (is (= (:raw outcome) (- (:counter-after outcome) (:counter-before outcome))))
          (is (integer? thread-id))
          (is (= original (into {} source)))
          (doseq [index (range operations)]
            (let [result (aget ^objects results index)]
              (is (= (assoc original key replacement) (into {} result)))
              (is (= (class source) (class result)))
              (is (not (identical? source result)))
              (is (identical? replacement (get result key)))
              (when (pos? index)
                (is (not (identical? result (aget ^objects results (dec index)))))))))))))

(deftest invalid-assoc-batch-test
  ;; Reject undefined divisors and operations that do not replace a value.
  (doseq [operations [0 -1 1.5 nil (inc (long Integer/MAX_VALUE))]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"batch size"
                          (assoc-allocation/collect! {:a 1} :a 2 operations))))
  (doseq [[source key replacement] [[{:a 1} :b 2] [{:a 1} :a 1]]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"changed declared value"
                          (assoc-allocation/collect! source key replacement 1)))))

(deftest assoc-boundary-control-test
  ;; Keep actual boundary costs, without assuming JIT-dependent zero allocation.
  (let [{:keys [contracts cases]} (edn/read-string (slurp (io/resource "cases.edn")))]
    (doseq [benchmark-case (filterv #(= :assoc (:contract %)) cases)
            representation [:closed-record :defrecord]
            operations [1 17 1024]]
      (let [fixture (fixtures/assoc-pair (:assoc contracts) benchmark-case)
            source (get fixture representation)
            {:keys [control results thread-id outcome]}
            (assoc-allocation/collect-boundary! source operations)]
        (is (= :assoc-boundary control))
        (is (= operations (alength ^objects results) (:operations outcome)))
        (is (every? #(identical? source %) results))
        (is (integer? thread-id))
        (is (= :measured (:status outcome)))
        (is (= :bytes/op (:unit outcome)))
        (is (<= 0 (:raw outcome)))
        (is (= (:raw outcome) (- (:counter-after outcome) (:counter-before outcome)))))))
  (doseq [operations [0 -1 1.5 nil (inc (long Integer/MAX_VALUE))]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"batch size"
                          (assoc-allocation/collect-boundary! {} operations)))))

(deftest warmed-assoc-batches-test
  ;; Keep real warmup evidence separate from final counts and boundary controls.
  (let [{:keys [contracts cases]} (edn/read-string (slurp (io/resource "cases.edn")))]
    (doseq [benchmark-case (filterv #(= :assoc (:contract %)) cases)
            representation [:closed-record :defrecord]]
      (let [{:keys [key replacement] :as fixture}
            (fixtures/assoc-pair (:assoc contracts) benchmark-case)
            source (get fixture representation)
            original (into {} source)
            batch (assoc-allocation/collect-warmed! source key replacement 17 2)]
        (is (= 2 (:warmup-batches batch) (count (:warmup batch))))
        (is (= 17 (:operations batch)))
        (doseq [pair (conj (:warmup batch)
                           {:assoc (get-in batch [:assoc :outcome])
                            :boundary (get-in batch [:boundary :outcome])})
                outcome (vals pair)]
          (is (= 17 (:operations outcome)))
          (is (= :measured (:status outcome)))
          (is (= (:raw outcome)
                 (- (:counter-after outcome) (:counter-before outcome)))))
        (is (every? #(identical? source %) (get-in batch [:boundary :results])))
        (doseq [result (get-in batch [:assoc :results])]
          (is (= (assoc original key replacement) (into {} result)))
          (is (not (identical? source result))))
        (is (= original (into {} source)))))))

(deftest invalid-warmup-count-test
  ;; Reject absent warmup rather than label a cold batch warmed.
  (doseq [warmup [nil 0 -1 1.5 (inc (long Integer/MAX_VALUE))]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"warmup count"
                          (assoc-allocation/collect-warmed! {:a 1} :a 2 17 warmup)))))
