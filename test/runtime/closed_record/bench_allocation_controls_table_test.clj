(ns closed-record.bench-allocation-controls-table-test
  (:require [clojure.test :refer [deftest is]]
            [closed-record.bench.metric-table :as table]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.run-results :as results]
            [closed-record.bench.runtime :as runtime]
            [clojure.java.io :as io])
  (:import (java.nio.file Files)))

(defn- counted [bytes]
  {:metric :allocation :unit :bytes/op :status :measured
   :raw bytes :operations 10 :counter-before 100 :counter-after (+ 100 bytes)})

(defn- controlled [fork]
  (merge (counted 400)
         {:case-id :assoc :configuration :ordinary :representation :record :fork fork
          :allocation-controls
          {:operations 10 :warmup-batches 2
           :warmup [{:assoc (counted 420) :boundary (counted 8)}
                    {:assoc (counted 400) :boundary (counted 8)}]
           :boundary (counted 4)}}))

(deftest controls-table-test
  ;; Preserve both raw series and deterministic fork order, without convergence claims.
  (let [rows [(controlled 1) (controlled 2)]
        report (table/render-allocation-controls rows)]
    (is (= report (table/render-allocation-controls (vec (reverse rows)))))
    (is (.contains report "[420 400 400] | [8 8 4] | [2] | [3] | 1 | 0"))
    (is (.contains report "do not prove JIT convergence or acceptance"))
    (is (nil? (table/render-allocation-controls [])))
    (is (nil? (table/render-allocation-controls [(dissoc (first rows) :allocation-controls)])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (table/render-allocation-controls
                  [(assoc (first rows) :allocation-controls nil)])))))

(deftest saved-controls-test
  ;; Real saved captures expose controls only after the existing complete-run gates.
  (let [parent (.toFile (Files/createTempDirectory
                         "saved-controls" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory {:execution {:forks 2}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:assoc {:metrics [:allocation]}}
                   :cases [{:id :assoc :configuration :ordinary :contract :assoc}]}
        children (mapv (fn [fork] {:run-id "controls" :revision "test"
                                   :configuration :ordinary :fork fork}) [1 2])
        observation (runtime/observe)]
    (try
      (evidence/create-run! directory inventory
                            {:command ["test"] :runtimes {:ordinary observation}
                             :measurement {:operations 10 :warmup-batches 2}} children)
      (doseq [index [0 1]]
        (evidence/capture-child!
         directory index
         {:exit 0 :out "" :err ""
          :raw-result (pr-str (assoc (nth children index) :schema-version 1
                                     :runtime observation :results [(controlled (inc index))]))}))
      (let [snapshot (fn [] (into {} (map (fn [file] [(.getPath file) (slurp file)]))
                                  (filterv #(.isFile %) (vec (file-seq directory)))))
            before (snapshot)
            report (results/render-saved directory)]
        (is (.contains report "| 40.000 |"))
        (is (.contains report (table/render-allocation-controls [(controlled 1) (controlled 2)])))
        (is (= before (snapshot))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))
