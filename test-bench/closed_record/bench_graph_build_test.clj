(ns closed-record.bench-graph-build-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.graph-build :as build]
            [closed-record.bench.graph-tool :as tool])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest prepared-build-test
  ;; Real compiler artifacts establish declarations; changed files fail read-only.
  (let [root (.toFile (Files/createTempDirectory
                       "graph-build-test" (make-array FileAttribute 0)))
        directory (io/file root "build")]
    (try
      (let [prepared (shell/sh "sh" "dev/bench/memory/prepare-record-graph.sh"
                               (str directory))]
        (is (zero? (:exit prepared)) (pr-str prepared))
        (when-not (zero? (:exit prepared))
          (throw (ex-info "Preparation failed" prepared))))
      (let [identity (build/read-declaration directory)]
        (is (= identity (tool/declaration identity)))
        (is (= identity (build/read-declaration directory)))
        (doseq [path ["RecordGraphProbe.java" "compiler.txt"
                      "classes/RecordGraphProbe.class" "record-graph-agent.jar"
                      "SHA256SUMS"]]
          (testing (str "Changed " path)
            (let [file (io/file directory path)
                  original (Files/readAllBytes (.toPath file))]
              (try
                (spit file "changed")
                (is (thrown? Exception (build/read-declaration directory)))
                (is (= "changed" (slurp file)))
                (finally
                  (with-open [out (io/output-stream file)]
                    (.write out original))))))))
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))
