(ns closed-record.bench-assoc-allocation-child-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.assoc-allocation-command :as allocation-command]
            [closed-record.bench.assoc-allocation-child :as child]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.fork :as fork]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest measured-child-capture-test
  ;; Real independent JVMs retain measured rows and controls through saved capture.
  (let [parent (.toFile (Files/createTempDirectory "assoc-child" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        declared (runtime/observe)
        java (str (io/file (:java-home declared) "bin" "java"))
        children (mapv (fn [n] {:run-id "assoc-smoke" :revision "test" :configuration :ordinary :fork n}) [1 2])]
    (try
      (evidence/create-run! directory inventory
                            {:command [java] :runtimes {:ordinary declared}} children)
      (doseq [index [0 1]]
        (is (evidence/successful-child?
             (fork/capture-process!
              directory index
              (fn [association path]
                (allocation-command/command inventory association path 17 2))))))
      (let [{:keys [captures]} (evidence/read-run directory)]
        (is (= 2 (count captures)))
        (doseq [[association capture] (mapv vector children captures)]
          (let [rows (:results (transport/decode inventory association (:raw-result capture)))
                order (if (odd? (:fork association))
                        [:closed-record :defrecord] [:defrecord :closed-record])]
            (is (= rows (child/validate-results inventory association rows)))
            (doseq [invalid [[] (pop rows) (conj rows (first rows))
                             (update rows 0 dissoc :allocation-controls)
                             (assoc-in rows [0 :allocation-controls :operations] 99)
                             (assoc rows 0 (-> (first rows)
                                               (dissoc :raw :operations :counter-before :counter-after)
                                               (assoc :status :unavailable :reason "Not measured")))]]
              (is (thrown? clojure.lang.ExceptionInfo
                           (child/validate-results inventory association invalid))))
            (is (= (into order order) (mapv :representation rows)))
            (is (= [:assoc/width-3 :assoc/width-3 :assoc/width-16 :assoc/width-16]
                   (mapv :case-id rows)))
            (doseq [row rows]
              (is (= :measured (:status row)))
              (is (pos? (:raw row)))
              (is (= 17 (:operations row)))
              (is (= 2 (count (get-in row [:allocation-controls :warmup]))))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest invalid-child-command-test
  ;; Invalid CLI budgets and associations must not publish a result file.
  (let [parent (.toFile (Files/createTempDirectory "assoc-cli" (make-array FileAttribute 0)))
        path (str (io/file parent "result.edn"))
        command [(str (io/file (System/getProperty "java.home") "bin" "java"))
                 "-cp" (System/getProperty "java.class.path") "clojure.main"
                 "-m" "closed-record.bench.assoc-allocation-child"]]
    (try
      (doseq [args [[]
                    [path "run" "revision" "1" "0" "2"]
                    [path "run" "revision" "1" "17" "-1"]
                    [path "run" "revision" "1" "2147483648" "2"]
                    [path "run" "revision" "6" "17" "2"]]]
        (let [{:keys [exit out err]} (apply shell/sh (into command args))]
          (is (= 2 exit))
          (is (= "" out))
          (is (seq err))
          (is (not (.exists (io/file path))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest allocation-command-budget-test
  ;; Reject invalid parent budgets without starting a process.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        association {:run-id "run" :revision "test" :configuration :ordinary :fork 1}]
    (doseq [budget [nil 0 -1 1.5 "17" (inc (long Integer/MAX_VALUE))]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (allocation-command/command inventory association "result" budget 2)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (allocation-command/command inventory association "result" 17 budget))))))

(deftest declared-budget-test
  ;; Internally valid controls must also match the parent workload declaration.
  (let [rows [{:case-id :assoc/width-3 :representation :closed-record
               :allocation-controls {:operations 17 :warmup-batches 2}}]
        budget {:operations 17 :warmup-batches 2 :acceptance? false}]
    (is (= rows (child/validate-budget rows budget)))
    (doseq [invalid [(assoc budget :operations 18)
                     (assoc budget :warmup-batches 3)
                     (dissoc budget :operations)
                     (assoc budget :operations 0)
                     (assoc budget :warmup-batches -1)]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (child/validate-budget rows invalid))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (child/validate-budget
                  (conj rows (assoc-in (first rows)
                                       [:allocation-controls :operations] 18))
                  budget)))))
