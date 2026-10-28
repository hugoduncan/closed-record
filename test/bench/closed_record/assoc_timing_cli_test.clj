(ns closed-record.assoc-timing-cli-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.assoc-timing-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.timing-tool :as tool])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- invoke!
  [parent arguments]
  (let [observed (runtime/observe)
        command (into (into [(str (io/file (:java-home observed) "bin" "java"))]
                            (:flags observed))
                      (into ["-cp" (System/getProperty "java.class.path")
                             "clojure.main" "-m" "closed-record.bench.assoc-timing-cli"]
                            arguments))
        output (io/file parent "stdout.txt")
        error (io/file parent "stderr.txt")
        process (.start (doto (ProcessBuilder. ^java.util.List command)
                          (.redirectOutput output)
                          (.redirectError error)))]
    {:exit (.waitFor process) :stderr (slurp error)}))

(deftest timing-cli-test
  ;; Real parent and child JVMs verify CLI provenance, rejection, and retry safety.
  (let [parent (.toFile (Files/createTempDirectory "timing-cli" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        arguments [(str directory) "cli-smoke" "test" "1.0"]]
    (try
      (doseq [args [[] [(str directory) "run" "test" "bad"]
                    [(str directory) "run" "test" "0"]
                    [(str directory) "run" "test" "NaN"]]]
        (is (= 2 (:exit (invoke! parent args))))
        (is (not (.exists directory))))
      (let [outcome (invoke! parent arguments)]
        (is (= 0 (:exit outcome)) (:stderr outcome))
        (when (zero? (:exit outcome))
          (let [rows (run/read-results directory)
                manifest (:manifest (evidence/read-run directory))
                before (into {} (map (fn [file] [(str file) (slurp file)]))
                             (filterv #(.isFile %) (vec (file-seq directory))))]
            (is (= 20 (count rows)))
            (is (= (tool/declaration (tool/observe))
                   (get-in manifest [:metadata :timing-tool])))
            (is (= arguments (subvec (get-in manifest [:metadata :command])
                                     (- (count (get-in manifest [:metadata :command])) 4))))
            (is (string? (get-in manifest [:metadata :machine :os])))
            (is (= 2 (:exit (invoke! parent arguments))))
            (is (= before (into {} (map (fn [[path _]] [path (slurp path)])) before))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
