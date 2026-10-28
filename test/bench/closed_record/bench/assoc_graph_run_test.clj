(ns closed-record.bench.assoc-graph-run-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-graph-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.graph-build :as build])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest measured-parent-test
  ;; Use a real prepared agent and five independent JVMs, without mocks.
  (let [root (.toFile (Files/createTempDirectory
                       "graph-parent" (make-array FileAttribute 0)))
        directory (io/file root "run")
        build-directory (System/getProperty "graph.build.directory")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        metadata {:command ["graph-parent-test"]}]
    (try
      (testing "invalid preparation leaves no run directory"
        (is (thrown? java.io.FileNotFoundException
                     (run/collect! directory inventory metadata "test" "revision"
                                   (io/file root "missing"))))
        (is (not (.exists directory))))
      (let [outcome (run/collect! directory inventory metadata "test" "revision"
                                  build-directory)
            rows (run/read-results directory)
            manifest (:manifest (evidence/read-run directory))]
        (is (= 0 (:exit outcome)) (pr-str outcome))
        (is (= 5 (count (:outcomes outcome))))
        (is (every? evidence/successful-child? (:outcomes outcome)))
        (is (= 60 (count rows)))
        (is (= (build/read-declaration build-directory)
               (get-in manifest [:metadata :graph-tool])))
        (is (= {:scope :ordinary-assoc-graph :acceptance? false}
               (get-in manifest [:metadata :measurement])))
        (is (= [:closed-record :defrecord :closed-record :defrecord :closed-record]
               (mapv :representation (take-nth 12 rows))))
        (is (thrown? java.nio.file.FileAlreadyExistsException
                     (run/collect! directory inventory metadata "test" "revision"
                                   build-directory)))
        (is (= rows (run/read-results directory))))
      (finally
        (run! io/delete-file (reverse (file-seq root)))))))
