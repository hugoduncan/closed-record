(ns closed-record.assoc-graph-run-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-graph-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.graph :as graph]
            [closed-record.bench.runtime :as runtime])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- snapshot [directory]
  (into {} (map (fn [file] [(str file) (slurp file)]))
        (filterv #(.isFile %) (vec (file-seq directory)))))

(deftest saved-graph-test
  ;; Real saved files exercise reader gates; numerical fixtures are not measurements.
  (let [parent (.toFile (Files/createTempDirectory "saved-graph" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        tool {:jar-sha256 (apply str (repeat 64 "a"))
              :class-sha256 (apply str (repeat 64 "b"))}
        children (mapv #(hash-map :run-id "test" :revision "revision"
                                  :configuration :ordinary :fork %) (range 1 6))
        metadata {:command ["fixture"] :runtimes {:ordinary (runtime/observe)}
                  :graph-tool tool
                  :measurement {:scope :ordinary-assoc-graph :acceptance? false}}]
    (try
      (evidence/create-run! directory inventory metadata children)
      (doseq [[index association] (map-indexed vector children)]
        (let [rows (vec (for [case-id [:assoc/width-3 :assoc/width-16]
                              representation [:closed-record :defrecord]
                              outcome (graph/outcomes 64 64 104)]
                          (merge outcome
                                 {:case-id case-id :representation representation
                                  :configuration :ordinary :fork (:fork association)
                                  :graph-tool (assoc tool :location "file:/child/agent.jar")})))]
          (evidence/capture-child!
           directory index
           {:exit 0 :out "" :err ""
            :raw-result (pr-str (assoc association :schema-version 1
                                       :runtime (runtime/observe) :results rows))})))
      (testing "complete partial rows retain raw values and fork order"
        (let [before (snapshot directory)
              rows (run/read-results directory)]
          (is (= 60 (count rows)))
          (is (= (vec (mapcat #(repeat 12 %) (range 1 6))) (mapv :fork rows)))
          (is (= #{64 104 40} (set (map :raw rows))))
          (let [report (run/render-saved directory)]
            (is (str/includes? report "not full-matrix acceptance or memory-tool approval"))
            (is (= 12 (count (filterv #(str/starts-with? % "| :assoc/")
                                      (str/split-lines report)))))
            (doseq [root [":result" ":source-and-result" ":increment-relative-to-source"]]
              (is (str/includes? report root)))
            (is (= report (run/render-saved directory))))
          (is (= before (snapshot directory)))))
      (let [manifest-file (io/file directory "manifest.edn")
            result-file (io/file directory "child-0/result.edn")
            outcome-file (io/file directory "child-0/outcome.edn")
            manifest (edn/read-string (slurp manifest-file))
            envelope (edn/read-string (slurp result-file))
            outcome (edn/read-string (slurp outcome-file))]
        (testing "changed declarations, raw evidence, and process gates reject without writes"
          (doseq [[file original variants]
                  [[manifest-file manifest
                    [(assoc-in manifest [:metadata :measurement :scope] :other)
                     (assoc-in manifest [:metadata :measurement :acceptance?] true)
                     (update-in manifest [:metadata] dissoc :graph-tool)
                     (update manifest :children pop)
                     (update manifest :children #(vec (reverse %)))]]
                   [result-file envelope
                    [(update envelope :results pop)
                     (assoc-in envelope [:results 0 :graph-tool] nil)
                     (assoc-in envelope [:results 0 :graph-tool :jar-sha256] (:class-sha256 tool))
                     (assoc-in envelope [:results 0 :source-bytes] 65)
                     (dissoc envelope :runtime)
                     (assoc-in envelope [:runtime :java-home] "different")
                     (assoc envelope :revision "different")]]
                   [outcome-file outcome
                    [(assoc outcome :exit 2)
                     (assoc-in outcome [:child :fork] 2)]]]]
            (doseq [variant variants]
              (spit file (pr-str variant))
              (let [before (snapshot directory)]
                (is (thrown? clojure.lang.ExceptionInfo (run/read-results directory)))
                (is (thrown? clojure.lang.ExceptionInfo (run/render-saved directory)))
                (is (= before (snapshot directory))))
              (spit file (pr-str original))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
