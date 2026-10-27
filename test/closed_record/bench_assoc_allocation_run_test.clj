(ns closed-record.bench-assoc-allocation-run-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.assoc-allocation-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.transport :as transport])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest allocation-run-test
  ;; Five real children persist partial evidence; invalid budgets consume no slot.
  (let [parent (.toFile (Files/createTempDirectory "allocation-run" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))]
    (try
      (is (thrown? clojure.lang.ExceptionInfo
                   (run/collect! directory inventory {:command ["test"]} "run" "test" 0 1)))
      (is (not (.exists directory)))
      (let [result (run/collect! directory inventory {:command ["test"]} "run" "test" 3 1)
            {:keys [manifest captures]} (evidence/read-run directory)]
        (is (= 0 (:exit result)))
        (is (= 5 (count (:outcomes result)) (count captures)))
        (is (= [1 2 3 4 5] (mapv :fork (:children manifest))))
        (is (= {:scope :ordinary-assoc-allocation :acceptance? false
                :operations 3 :warmup-batches 1}
               (get-in manifest [:metadata :measurement])))
        (doseq [[association capture] (mapv vector (:children manifest) captures)]
          (let [rows (:results (transport/decode inventory association (:raw-result capture)))]
            (is (= 4 (count rows)))
            (is (every? #(= 3 (:operations %)) rows))
            (is (every? #(= 1 (count (get-in % [:allocation-controls :warmup]))) rows))))
        (let [before (slurp (io/file directory "manifest.edn"))]
          (is (thrown? java.nio.file.FileAlreadyExistsException
                       (run/collect! directory inventory {:command ["retry"]} "run" "test" 3 1)))
          (is (= before (slurp (io/file directory "manifest.edn"))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest allocation-cli-test
  ;; Real parent and child JVMs preserve evidence and reject unsafe retries.
  (let [parent (.toFile (Files/createTempDirectory "allocation-cli" (make-array FileAttribute 0)))
        directory (io/file parent "run with spaces")
        command [(str (io/file (System/getProperty "java.home") "bin" "java"))
                 "-cp" (System/getProperty "java.class.path") "clojure.main"
                 "-m" "closed-record.bench.assoc-allocation-cli"]
        args [(str directory) "cli-run" "test" "3" "1"]]
    (try
      (doseq [invalid [[] (assoc args 3 "0") (assoc args 4 "2147483648")]]
        (let [result (apply shell/sh (into command invalid))]
          (is (= 2 (:exit result)))
          (is (seq (:err result)))
          (is (= "" (:out result)))
          (is (not (.exists directory)))))
      (let [result (apply shell/sh (into command args))
            {:keys [manifest captures]} (evidence/read-run directory)
            before (slurp (io/file directory "manifest.edn"))]
        (is (= 0 (:exit result)))
        (is (= "" (:out result)))
        (is (= 5 (count captures)))
        (is (every? (comp evidence/successful-child? :outcome) captures))
        (is (= args (subvec (get-in manifest [:metadata :command]) 6)))
        (is (seq (get-in manifest [:metadata :machine :architecture])))
        (is (= 2 (:exit (apply shell/sh (into command args)))))
        (is (= before (slurp (io/file directory "manifest.edn")))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest saved-allocation-validation-test
  ;; Saved evidence is reusable only while all manifest and capture gates hold.
  (let [parent (.toFile (Files/createTempDirectory "saved-allocation" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))]
    (try
      (run/collect! directory inventory {:command ["test"]} "saved" "test" 3 1)
      (is (= 20 (count (run/read-results directory))))
      (let [before (into {} (map (fn [file] [(str file) (slurp file)]))
                         (filterv #(.isFile %) (vec (file-seq directory))))
            report (run/render-saved directory)]
        (is (str/includes? report "not complete matrix or acceptance evidence"))
        (is (str/includes? report "Boundary changed batches"))
        (is (= 20 (count (filterv #(str/starts-with? % "| :assoc/")
                                  (str/split-lines report)))))
        (is (= report (run/render-saved directory)))
        (is (every? (fn [[path content]] (= content (slurp path))) before)))
      (let [manifest-file (io/file directory "manifest.edn")
            outcome-file (io/file directory "child-0" "outcome.edn")
            result-file (io/file directory "child-0" "result.edn")
            raw-result (edn/read-string (slurp result-file))
            manifest (edn/read-string (slurp manifest-file))
            outcome (edn/read-string (slurp outcome-file))]
        (doseq [[file original variants]
                [[manifest-file manifest
                  [(assoc-in manifest [:metadata :measurement :operations] 4)
                   (assoc-in manifest [:metadata :measurement :warmup-batches] 2)
                   (assoc-in manifest [:metadata :measurement :scope] :other)
                   (assoc-in manifest [:metadata :measurement :acceptance?] true)
                   (update manifest :children pop)
                   (assoc-in manifest [:children 1 :fork] 1)
                   (assoc-in manifest [:metadata :runtimes :ordinary :version] "other")
                   (assoc-in manifest [:metadata :runtimes :ordinary :java-home] "/other")
                   (assoc-in manifest [:metadata :runtimes :ordinary :flags] ["-Xint"])]]
                 [result-file raw-result
                  [(dissoc raw-result :runtime)
                   (assoc-in raw-result [:runtime :version] "other")
                   (assoc-in raw-result [:runtime :java-home] "/other")
                   (assoc-in raw-result [:runtime :flags] ["-Xint"])]]
                 [outcome-file outcome
                  [(assoc outcome :exit 2)
                   (assoc-in outcome [:child :run-id] "other")
                   (assoc-in outcome [:runtime-validation :status] :invalid)]]]]
          (doseq [variant variants]
            (spit file (pr-str variant))
            (let [before (slurp file)]
              (is (thrown? clojure.lang.ExceptionInfo (run/read-results directory)))
              (is (thrown? clojure.lang.ExceptionInfo (run/render-saved directory)))
              (is (= before (slurp file)))))
          (spit file (pr-str original)))
        (is (= 20 (count (run/read-results directory))))
        (io/delete-file (io/file directory "child-0" "result.edn"))
        (is (thrown? Exception (run/read-results directory))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
