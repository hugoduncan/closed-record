(ns closed-record.assoc-graph-command-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-graph-command :as command]
            [closed-record.bench.runtime :as runtime]))

(deftest graph-command-test
  ;; Construction preserves argument boundaries without loading the graph agent.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        association {:run-id "graph run" :revision "revision"
                     :configuration :ordinary :fork 2}
        observed (runtime/observe)]
    (testing "the child inherits the complete runtime invocation"
      (is (= (into [(str (io/file (:java-home observed) "bin" "java"))]
                   (concat (:flags observed)
                           ["-cp" (System/getProperty "java.class.path")
                            "clojure.main" "-m" "closed-record.bench.assoc-graph-child"
                            "result path.edn" "graph run" "revision" "2"]))
             (command/command inventory association "result path.edn"))))
    (testing "invalid associations fail before launch"
      (doseq [changed [(assoc association :fork 0)
                       (assoc association :fork 6)
                       (assoc association :configuration :unknown)
                       (dissoc association :revision)]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (command/command inventory changed "unused")))))))
