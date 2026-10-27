(ns valhalla.graph-child-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [closed-record.bench.graph-tool :as tool]
            [closed-record.bench.transport :as transport]
            [valhalla.graph-child :as child])
  (:import FlatGraphProbe
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest saved-graphs
  ;; Check real agent publication, portable identity, and exclusive file ownership.
  (let [directory (.toFile (Files/createTempDirectory "flat-graph-child" (make-array FileAttribute 0)))
        path (io/file directory "result.edn")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        association {:run-id "graph-test" :revision "test" :configuration :valhalla :fork 1}
        observed (into {} (map (fn [[k v]] [(keyword k) v])) (FlatGraphProbe/identity))]
    (try
      (child/write-results! path inventory association)
      (let [text (slurp path)
            rows (:results (transport/decode inventory association text))]
        (is (= 30 (count rows)))
        (is (= #{:measured :unavailable} (set (map :status rows))))
        (doseq [row rows]
          (is (= observed (tool/validate-observation observed (:graph-tool row)))))
        (is (thrown? Exception (child/write-results! path inventory association)))
        (is (= text (slurp path))))
      (is (thrown? clojure.lang.ExceptionInfo
                   (child/write-results! (io/file directory "invalid.edn") inventory
                                         (assoc association :configuration :ordinary))))
      (is (not (.exists (io/file directory "invalid.edn"))))
      (finally
        (doseq [file (reverse (file-seq directory))] (io/delete-file file))))))
