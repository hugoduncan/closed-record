(ns closed-record.assoc-timing-process-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.fork :as fork]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport]
            [closed-record.bench.timing-tool :as tool]))

(deftest timing-process-test
  ;; Real child JVMs preserve runtime flags and reject invalid measurement budgets.
  (let [parent (.toFile (java.nio.file.Files/createTempDirectory
                         "timing-process-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        observed (runtime/observe)
        prefix (into [(str (io/file (:java-home observed) "bin" "java"))]
                     (:flags observed))
        association {:run-id "timing-process" :revision "test"
                     :configuration :ordinary :fork 2}]
    (try
      (doseq [[index budget] (map-indexed vector ["1.0" "0" "NaN"])]
        (let [directory (io/file parent (str index))]
          (evidence/create-run! directory inventory
                                {:command prefix :runtimes {:ordinary observed}}
                                [association])
          (let [outcome (fork/capture-process!
                         directory 0
                         (fn [child path]
                           (into prefix
                                 ["-cp" (System/getProperty "java.class.path")
                                  "clojure.main" "-m" "closed-record.bench.assoc-timing-child"
                                  path (:run-id child) (:revision child)
                                  (str (:fork child)) budget])))]
            (if (zero? index)
              (let [capture (first (:captures (evidence/read-run directory)))
                    envelope (if (:raw-result capture)
                               (transport/decode inventory association (:raw-result capture))
                               (throw (ex-info "Child produced no result"
                                               {:outcome outcome
                                                :stderr (slurp (io/file directory "process-0/stderr.txt"))})))]
                (is (evidence/successful-child? outcome))
                (is (= observed (:runtime envelope)))
                (is (= 4 (count (:results envelope))))
                (is (every? #(= {:limit-time-s 1.0} (:timing-budget %))
                            (:results envelope)))
                (is (every? #(= (tool/observe) (:timing-tool %)) (:results envelope)))
                (is (every? #(and (= :time (:metric %))
                                  (= :measured (:status %))
                                  (pos? (:operations %)))
                            (:results envelope))))
              (do
                (is (= 2 (:exit outcome)))
                (is (not (evidence/successful-child? outcome)))
                (is (not (.exists (io/file directory "process-0/result.edn")))))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
