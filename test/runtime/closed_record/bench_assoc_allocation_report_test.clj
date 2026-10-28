(ns closed-record.bench-assoc-allocation-report-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-allocation-run :as run])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- snapshot [directory]
  (into {} (comp (filter #(.isFile %))
                 (map (fn [file] [(str file) (vec (Files/readAllBytes (.toPath file)))])))
        (file-seq directory)))

(deftest saved-allocation-command-test
  ;; Real JVM commands report validated rows or reject evidence without writes.
  (let [parent (.toFile (Files/createTempDirectory "allocation-report" (make-array FileAttribute 0)))
        directory (io/file parent "saved run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        command [(str (io/file (System/getProperty "java.home") "bin" "java"))
                 "-cp" (System/getProperty "java.class.path") "clojure.main"
                 "-m" "closed-record.bench.assoc-allocation-report"]
        invoke (fn [args] (apply shell/sh (into command args)))]
    (try
      (run/collect! directory inventory {:command ["test"]} "report" "test" 3 1)
      (testing "success prints only the validated partial report"
        (let [before (snapshot parent)
              result (invoke [(str directory)])]
          (is (= 0 (:exit result)))
          (is (= "" (:err result)))
          (is (= (str (run/render-saved directory) "\n") (:out result)))
          (is (= before (snapshot parent)))))
      (testing "invalid arguments and absent runs fail without a report"
        (doseq [args [[] [(str directory) "extra"] [(str (io/file parent "absent"))]]]
          (let [before (snapshot parent)
                result (invoke args)]
            (is (= 2 (:exit result)))
            (is (= "" (:out result)))
            (is (seq (:err result)))
            (is (= before (snapshot parent))))))
      (testing "a changed declared budget rejects otherwise successful captures"
        (let [file (io/file directory "manifest.edn")
              manifest (edn/read-string (slurp file))]
          (spit file (pr-str (assoc-in manifest [:metadata :measurement :operations] 4)))
          (let [before (snapshot parent)
                result (invoke [(str directory)])]
            (is (= 2 (:exit result)))
            (is (= "" (:out result)))
            (is (seq (:err result)))
            (is (= before (snapshot parent))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
