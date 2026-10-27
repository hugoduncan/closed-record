(ns closed-record.bench.assoc-graph-child-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.fork :as fork]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-graph-child :as child]
            [closed-record.bench.assoc-graph-command :as graph-command]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport])
  (:import RecordGraphProbe
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest saved-graph-rows
  ;; Exercise real instrumentation and exclusive file transport without mocks.
  (let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        directory (.toFile (Files/createTempDirectory
                            "assoc-graph-child" (make-array FileAttribute 0)))
        association {:run-id "graph-test" :revision "test-revision"
                     :configuration :ordinary :fork 1}]
    (try
      (doseq [fork [1 2]]
        (testing (str "saved fork " fork)
          (let [expected (assoc association :fork fork)
                path (io/file directory (str fork ".edn"))
                envelope (child/write-results! path inventory expected)
                text (slurp path)
                rows (:results envelope)]
            (is (= envelope (transport/decode inventory expected text)))
            (is (= (runtime/observe) (:runtime envelope)))
            (is (= 12 (count rows)))
            (is (every? #(= (into {} (map (fn [[k v]] [(keyword k) v]))
                                       (RecordGraphProbe/identity))
                            (:graph-tool %)) rows))
            (is (= (if (odd? fork) :closed-record :defrecord)
                   (:representation (first rows))))
            (is (every? #(and (= :graph (:metric %))
                              (= :measured (:status %))
                              (pos? (:raw %))) rows))
            (is (thrown? java.nio.file.FileAlreadyExistsException
                         (child/write-results! path inventory expected)))
            (is (= text (slurp path))))))
      (testing "invalid input creates no evidence"
        (let [path (io/file directory "invalid.edn")]
          (is (thrown? clojure.lang.ExceptionInfo
                       (child/write-results! path inventory (assoc association :fork 0))))
          (is (thrown? clojure.lang.ExceptionInfo
                       (child/write-results! path (assoc inventory :cases []) association)))
          (is (not (.exists path)))))
      (finally
        (run! io/delete-file (reverse (file-seq directory)))))))

(deftest independent-graph-command
  ;; Real instrumented children preserve raw graphs and reject invalid CLI input.
  (let [directory (.toFile (Files/createTempDirectory
                            "graph-command" (make-array FileAttribute 0)))
        run (io/file directory "run")
        inventory (edn/read-string (slurp (io/resource "cases.edn")))
        observed (runtime/observe)
        command (into [(str (io/file (:java-home observed) "bin" "java"))]
                      (concat (:flags observed)
                              ["-cp" (System/getProperty "java.class.path")
                               "clojure.main" "-m"
                               "closed-record.bench.assoc-graph-child"]))
        association {:run-id "graph-command" :revision "test"
                     :configuration :ordinary :fork 1}]
    (try
      (evidence/create-run! run inventory
                            {:command command :runtimes {:ordinary observed}}
                            [association])
      (let [outcome (fork/capture-process!
                     run 0 (fn [expected path]
                             (graph-command/command inventory expected path)))]
        (is (evidence/successful-child? outcome) (pr-str outcome)))
      (let [capture (first (:captures (evidence/read-run run)))
            envelope (transport/decode inventory association (:raw-result capture))]
        (is (= observed (:runtime envelope)))
        (is (every? #(= (into {} (map (fn [[k v]] [(keyword k) v]))
                                   (RecordGraphProbe/identity))
                        (:graph-tool %)) (:results envelope)))
        (is (= 12 (count (:results envelope))))
        (is (every? #(and (= :measured (:status %)) (pos? (:raw %)))
                    (:results envelope))))
      (doseq [args [[] ["0"] ["6"] ["2147483648"]]]
        (let [path (io/file directory "invalid.edn")
              arguments (if (empty? args) []
                            (into [(str path) "run" "test"] args))
              {:keys [exit out err]} (apply shell/sh (into command arguments))]
          (is (= 2 exit))
          (is (= "" out))
          (is (seq err))
          (is (not (.exists path)))))
      (finally
        (run! io/delete-file (reverse (file-seq directory)))))))
