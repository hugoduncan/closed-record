(ns closed-record.bench-assoc-allocation-result-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.assoc-allocation-result :as result]
            [closed-record.bench.child :as child]
            [closed-record.bench.transport :as transport])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest persisted-warmed-allocation-test
  ;; Real counters and child files retain raw controls without Java object roots.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        directory (Files/createTempDirectory "assoc-evidence" (make-array FileAttribute 0))
        path (.toFile (.resolve directory "result.edn"))
        association {:run-id "allocation-smoke" :revision "test-revision"
                     :configuration :ordinary :fork 1}]
    (try
      (let [rows (into []
                       (mapcat (fn [benchmark-case]
                                 (mapv #(result/collect! inventory benchmark-case % 1 17 2)
                                       [:closed-record :defrecord])))
                       (filterv #(= :assoc (:contract %)) (:cases inventory)))
            written (child/write-results! path inventory association rows)
            decoded (transport/decode inventory association (slurp path))]
        (is (= written decoded))
        (is (= 4 (count (:results decoded))))
        (doseq [row (:results decoded)]
          (is (= :measured (:status row)))
          (is (pos? (:raw row)))
          (is (= 17 (:operations row)))
          (let [controls (:allocation-controls row)]
            (is (= 2 (:warmup-batches controls) (count (:warmup controls))))
            (is (= 17 (:operations controls)))
            (doseq [outcome (into [row (:boundary controls)]
                                  (mapcat vals) (:warmup controls))]
              (is (= 17 (:operations outcome)))
              (is (= (:raw outcome)
                     (- (:counter-after outcome) (:counter-before outcome))))))))
      (finally
        (Files/deleteIfExists (.toPath path))
        (Files/delete directory)))))

(deftest invalid-allocation-association-test
  ;; Reject wrong inventory identities before collection.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        benchmark-case (first (filterv #(= :assoc (:contract %)) (:cases inventory)))]
    (doseq [[candidate representation fork]
            [[benchmark-case :unknown 1]
             [benchmark-case :closed-record 0]
             [(assoc benchmark-case :id :unknown) :closed-record 1]
             [(assoc benchmark-case :configuration :valhalla) :closed-record 1]]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (result/collect! inventory candidate representation fork 17 2))))))

(defn- counted
  [bytes]
  {:metric :allocation :unit :bytes/op :status :measured
   :raw bytes :operations 10 :counter-before 100 :counter-after (+ 100 bytes)})

(defn- controlled-row
  [counts final-count]
  (assoc (counted final-count) :allocation-controls
         {:operations 10 :warmup-batches (count counts)
          :warmup (mapv (fn [bytes] {:assoc (counted bytes) :boundary (counted 8)}) counts)
          :boundary (counted 4)}))

(deftest allocation-stability-test
  ;; Summaries retain overhead and expose late changes rather than hide them.
  (is (= {:operations 10 :assoc-counts [420 400 400 400]
          :boundary-counts [8 8 8 4] :matching-trailing-warmup-batches 2
          :changed-batches [2] :boundary-changed-batches [4]
          :matching-trailing-boundary-warmup-batches 0}
         (result/stability (controlled-row [420 400 400] 400))))
  (is (= [2 3 4] (:changed-batches
                  (result/stability (controlled-row [400 420 400] 430)))))
  (is (zero? (:matching-trailing-warmup-batches
              (result/stability (controlled-row [400 400] 430)))))
  (is (= 1 (:matching-trailing-warmup-batches
            (result/stability (controlled-row [400] 400)))))
  (doseq [row [(controlled-row [] 400)
               (dissoc (controlled-row [400] 400) :allocation-controls)
               (assoc (controlled-row [400] 400) :raw 399)
               (assoc-in (controlled-row [400] 400) [:allocation-controls :operations] 11)
               (assoc-in (controlled-row [400] 400) [:allocation-controls :warmup-batches] 2)
               (assoc-in (controlled-row [400] 400) [:allocation-controls :boundary :status] :failed)]]
    (is (thrown? clojure.lang.ExceptionInfo (result/stability row)))))

(deftest boundary-stability-test
  ;; Equal assoc counts must not hide changing loop/store/counter costs.
  (doseq [[counts final-count transitions trailing]
          [[[8] 8 [] 1]
           [[8 4 8] 4 [2 3 4] 0]
           [[8 4 4] 4 [2] 2]
           [[0 0] 0 [] 2]]]
    (let [row (-> (controlled-row (mapv (constantly 400) counts) 400)
                  (assoc-in [:allocation-controls :warmup]
                            (mapv (fn [bytes]
                                    {:assoc (counted 400) :boundary (counted bytes)})
                                  counts))
                  (assoc-in [:allocation-controls :boundary] (counted final-count)))
          summary (result/stability row)]
      (is (= [] (:changed-batches summary)))
      (is (= transitions (:boundary-changed-batches summary)))
      (is (= trailing (:matching-trailing-boundary-warmup-batches summary)))
      (is (= (conj counts final-count) (:boundary-counts summary)))
      (is (= (count counts) (:matching-trailing-warmup-batches summary))))))
