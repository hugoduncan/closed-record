(ns closed-record.bench-child-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.child :as child]
   [closed-record.bench.runtime :as runtime]
   [closed-record.bench.transport :as transport])
  (:import (java.nio.file Files FileAlreadyExistsException)))

(deftest write-results-test
  ;; A child records real provenance without replacing previous evidence.
  (let [directory (.toFile (Files/createTempDirectory "child-results-" (make-array java.nio.file.attribute.FileAttribute 0)))
        path (io/file directory "result.edn")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        association {:run-id "child-write" :revision "abc123"
                     :configuration (:configuration (first (:cases inventory))) :fork 1}]
    (try
      (testing "invalid results do not reserve the output path"
        (is (thrown? clojure.lang.ExceptionInfo
                     (child/write-results! path inventory association [{}])))
        (is (not (.exists path))))
      (testing "partial evidence includes the actual runtime"
        (let [envelope (child/write-results! path inventory association [])
              saved (slurp path)]
          (is (= (runtime/observe) (:runtime envelope)))
          (is (= envelope (transport/decode inventory association saved)))
          (is (= [] (:results envelope)))
          (is (thrown? FileAlreadyExistsException
                       (child/write-results! path inventory association [])))
          (is (= saved (slurp path)))))
      (finally
        (doseq [file (reverse (file-seq directory))]
          (io/delete-file file))))))
