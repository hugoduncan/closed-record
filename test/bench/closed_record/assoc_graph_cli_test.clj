(ns closed-record.assoc-graph-cli-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.assoc-graph-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.graph-build :as build])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- invoke!
  [parent arguments flags]
  (let [observed (runtime/observe)
        command (into (into [(str (io/file (:java-home observed) "bin" "java"))]
                            flags)
                      (into ["-cp" (System/getProperty "java.class.path")
                             "clojure.main" "-m" "closed-record.bench.assoc-graph-cli"]
                            arguments))
        output (io/file parent "stdout.txt")
        error (io/file parent "stderr.txt")
        process (.start (doto (ProcessBuilder. ^java.util.List command)
                          (.redirectOutput output)
                          (.redirectError error)))]
    {:exit (.waitFor process) :stderr (slurp error)}))

(deftest graph-cli-test
  ;; Real parent and child JVMs verify CLI provenance, rejection, and retry safety.
  (let [parent (.toFile (Files/createTempDirectory "graph-cli" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        arguments [(str directory) "cli-smoke" "test" (System/getProperty "graph.build.directory")]]
    (try
      (doseq [args [[] [(str directory) "run" "test" (str (io/file parent "missing"))]]]
        (is (= 2 (:exit (invoke! parent args (:flags (runtime/observe))))))
        (is (not (.exists directory))))
      (let [failed-directory (io/file parent "inactive-agent")
            outcome (invoke! parent [(str failed-directory) "failed" "test"
                                     (System/getProperty "graph.build.directory")]
                             (filterv #(not (.startsWith ^String % "-javaagent:"))
                                      (:flags (runtime/observe))))
            captures (:captures (evidence/read-run failed-directory))]
        (is (= 2 (:exit outcome)) (:stderr outcome))
        (is (= 5 (count captures)))
        (is (every? #(not (evidence/successful-child? (:outcome %))) captures)))
      (let [outcome (invoke! parent arguments (:flags (runtime/observe)))]
        (is (= 0 (:exit outcome)) (:stderr outcome))
        (when (zero? (:exit outcome))
          (let [rows (run/read-results directory)
                manifest (:manifest (evidence/read-run directory))
                before (into {} (map (fn [file] [(str file) (slurp file)]))
                             (filterv #(.isFile %) (vec (file-seq directory))))]
            (is (= 60 (count rows)))
            (is (= (build/read-declaration (System/getProperty "graph.build.directory"))
                   (get-in manifest [:metadata :graph-tool])))
            (is (= arguments (subvec (get-in manifest [:metadata :command])
                                     (- (count (get-in manifest [:metadata :command])) 4))))
            (is (string? (get-in manifest [:metadata :machine :os])))
            (is (= 2 (:exit (invoke! parent arguments (:flags (runtime/observe))))))
            (is (= before (into {} (map (fn [[path _]] [path (slurp path)])) before))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
