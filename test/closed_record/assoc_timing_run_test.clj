(ns closed-record.assoc-timing-run-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-timing-run :as run]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.runtime :as runtime])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- snapshot [directory]
  (into {} (map (fn [file] [(str file) (slurp file)]))
        (filterv #(.isFile %) (vec (file-seq directory)))))

(deftest saved-timing-test
  ;; Real saved files exercise reader gates; numerical fixtures are not measurements.
  (let [parent (.toFile (Files/createTempDirectory "saved-timing" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        tool {:library 'org.hugoduncan/criterium
              :base-version "0.5.251-ALPHA" :distribution :upstream
              :collector-sha256 "a73da4deaa664aa68ddbeb0989e7dcb22a5878864a720b662c00aa9ab1557fa2"}
        children (mapv #(hash-map :run-id "test" :revision "revision"
                                  :configuration :ordinary :fork %) (range 1 6))
        metadata {:command ["fixture"] :runtimes {:ordinary (runtime/observe)}
                  :timing-tool tool
                  :measurement {:scope :ordinary-assoc-timing :acceptance? false
                                :limit-time-s 1.0}}]
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
      (testing "complete partial rows retain raw values and fork order"
        (let [before (snapshot directory)
              rows (run/read-results directory)]
          (is (= 20 (count rows)))
          (is (= (vec (mapcat #(repeat 4 %) (range 1 6))) (mapv :fork rows)))
          (is (every? #(= (* 100.0 (:fork %)) (:raw %)) rows))
          (let [report (run/render-saved directory)]
            (is (.contains report "Partial ordinary assoc timing diagnostic"))
            (is (.contains report "not full-matrix acceptance or S-03 parity"))
            (is (.contains report "width=16"))
            (is (.contains report "| 15.000 | 5.000–25.000 | 7.906 |"))
            (is (not (.contains report ":legacy/")))
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
                     (assoc-in manifest [:metadata :measurement :limit-time-s] 2.0)
                     (update-in manifest [:metadata] dissoc :timing-tool)
                     (update manifest :children pop)
                     (update manifest :children #(vec (reverse %)))]]
                   [result-file envelope
                    [(update envelope :results pop)
                     (assoc-in envelope [:results 0 :timing-tool] nil)
                     (assoc-in envelope [:results 0 :timing-budget :limit-time-s] 2.0)
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

(deftest prelaunch-declaration-test
  ;; Invalid declarations must not consume a run directory or launch children.
  (let [parent (.toFile (Files/createTempDirectory "timing-prelaunch" (make-array FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))]
    (try
      (doseq [[metadata seconds] [[{:command ["test"]} 1.0]
                                  [{:command ["test"] :timing-tool {}} 1.0]
                                  [{:command ["test"]} 0]
                                  [{:command ["test"]} Double/NaN]]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (run/collect! directory inventory metadata "test" "revision" seconds)))
        (is (not (.exists directory))))
      (finally
        (io/delete-file parent)))))
