(ns closed-record.assoc-timing-parent-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.assoc-timing-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.timing-tool :as tool])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest timing-parent-test
  ;; Five real fresh JVMs exercise command construction and saved validation.
  (let [parent (.toFile (Files/createTempDirectory "timing-parent" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        metadata {:command ["timing-parent-test"] :timing-tool (tool/observe)}]
    (try
      (let [outcome (run/collect! directory inventory metadata "smoke" "test" 1)
            manifest (:manifest (evidence/read-run directory))
            rows (run/read-results directory)]
        (is (= 0 (:exit outcome)))
        (is (= 5 (count (:outcomes outcome))))
        (is (every? evidence/successful-child? (:outcomes outcome)))
        (is (= 20 (count rows)))
        (is (= [1 2 3 4 5] (mapv :fork (:children manifest))))
        (is (= (tool/declaration (tool/observe)) (get-in manifest [:metadata :timing-tool])))
        (is (= {:scope :ordinary-assoc-timing :acceptance? false :limit-time-s 1.0}
               (get-in manifest [:metadata :measurement])))
        (is (thrown? java.nio.file.FileAlreadyExistsException
                     (run/collect! directory inventory metadata "smoke" "test" 1)))
        (is (= rows (run/read-results directory))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
