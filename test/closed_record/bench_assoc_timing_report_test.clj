(ns closed-record.bench-assoc-timing-report-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-timing-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.runtime :as runtime])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- snapshot [directory]
  (into {} (comp (filter #(.isFile %))
                 (map (fn [file] [(str file) (vec (Files/readAllBytes (.toPath file)))])))
        (file-seq directory)))

(deftest saved-timing-command-test
  ;; Real JVM commands report validated rows or reject evidence without writes.
  (let [parent (.toFile (Files/createTempDirectory "timing-report" (make-array FileAttribute 0)))
        directory (io/file parent "saved run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        tool {:library 'org.hugoduncan/criterium
              :base-version "0.5.251-ALPHA" :distribution :upstream
              :collector-sha256 "a73da4deaa664aa68ddbeb0989e7dcb22a5878864a720b662c00aa9ab1557fa2"}
        children (mapv #(hash-map :run-id "test" :revision "revision"
                                  :configuration :ordinary :fork %) (range 1 6))
        metadata {:command ["fixture"] :runtimes {:ordinary (runtime/observe)}
                  :timing-tool tool
                  :measurement {:scope :ordinary-assoc-timing :acceptance? false
                                :limit-time-s 1.0}}
        command [(str (io/file (System/getProperty "java.home") "bin" "java"))
                 "-cp" (System/getProperty "java.class.path") "clojure.main"
                 "-m" "closed-record.bench.assoc-timing-report"]
        invoke (fn [args] (apply shell/sh (into command args)))]
    (try
      (evidence/create-run! directory inventory metadata children)
      (doseq [[index association] (map-indexed vector children)]
        (let [rows (vec (for [case-id [:assoc/width-3 :assoc/width-16]
                              representation [:closed-record :defrecord]]
                          {:case-id case-id :representation representation
                           :configuration :ordinary :fork (:fork association)
                           :metric :time :status :measured :unit :ns/op
                           :raw (* 100.0 (:fork association)) :operations 20 :timing-tool tool
                           :timing-budget {:limit-time-s 1.0}}))]
          (evidence/capture-child!
           directory index
           {:exit 0 :out "" :err ""
            :raw-result (pr-str (assoc association :schema-version 1
                                       :runtime (runtime/observe) :results rows))})))
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
          (spit file (pr-str (assoc-in manifest [:metadata :measurement :limit-time-s] 2.0)))
          (let [before (snapshot parent)
                result (invoke [(str directory)])]
            (is (= 2 (:exit result)))
            (is (= "" (:out result)))
            (is (seq (:err result)))
            (is (= before (snapshot parent))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
