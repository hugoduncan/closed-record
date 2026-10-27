(ns closed-record.bench-run-results-test
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.evidence :as evidence]
   [closed-record.bench.runtime :as runtime]
   [closed-record.bench-graph-tool-test :as graph-fixtures]
   [closed-record.bench-timing-tool-test :as tool-fixtures]
   [closed-record.bench.run-results :as run-results])
  (:import (java.nio.file Files)))

(deftest assemble-test
  ;; Process success and complete coverage are separate requirements.
  (let [inventory {:execution {:forks 2}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:time]}}
                   :cases [{:id :read :configuration :ordinary :contract :read}]}
        children (mapv (fn [fork] {:run-id "run" :revision "revision"
                                   :configuration :ordinary :fork fork}) [1 2])
        rows (mapv (fn [fork] {:case-id :read :configuration :ordinary
                               :representation :record :fork fork :metric :time
                               :unit :ns/op :status :unavailable :reason "Not measured"}) [1 2])
        captures (mapv (fn [child row]
                         {:outcome {:child child :exit 0
                                    :validation {:status :valid}
                                    :runtime-validation {:status :valid}}
                          :raw-result (pr-str (assoc child :schema-version 1 :results [row]))})
                       children rows)]
    (testing "complete unavailable evidence is retained without derived numbers"
      (is (= rows (run-results/assemble inventory children captures))))
    (testing "process failures cannot enter reporting"
      (is (thrown? clojure.lang.ExceptionInfo
                   (run-results/assemble inventory children
                                         (assoc-in captures [0 :outcome :exit] 1))))
      (is (thrown? clojure.lang.ExceptionInfo
                   (run-results/assemble inventory children (vec (reverse captures))))))
    (testing "partial results cannot enter reporting"
      (is (thrown? clojure.lang.ExceptionInfo
                   (run-results/assemble inventory children
                                         (assoc-in captures [0 :raw-result]
                                                   (pr-str (assoc (first children) :schema-version 1 :results [])))))))
    (testing "duplicate plans fail"
      (is (thrown? clojure.lang.ExceptionInfo
                   (run-results/assemble inventory [(first children) (first children)] captures))))))

(deftest saved-results-test
  ;; Saved artifacts must pass the same coverage and success gates as live data.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-read-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 1}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:time]}}
                   :cases [{:id :read :configuration :ordinary :contract :read}]}
        child {:run-id "saved" :revision "revision" :configuration :ordinary :fork 1}
        observation (runtime/observe)
        row {:case-id :read :configuration :ordinary :representation :record
             :fork 1 :metric :time :unit :ns/op :timing-budget {:limit-time-s 1.0} :status :measured
             :raw 20 :operations 2 :timing-tool tool-fixtures/corrected}
        raw (pr-str (assoc child :schema-version 1 :runtime observation :results [row]))]
    (try
      (doseq [[label exit result] [["complete" 0 raw] ["failed" 7 raw]
                                   ["absent-result" 0 nil]
                                   ["missing-runtime" 0
                                    (pr-str (assoc child :schema-version 1 :results [row]))]
                                   ["wrong-runtime" 0
                                    (pr-str (assoc child :schema-version 1 :results [row]
                                                   :runtime (assoc observation :java-home "/wrong-java")))]]]
        (let [directory (io/file parent label)]
          (evidence/create-run! directory inventory
                                {:command ["benchmark"] :measurement {:limit-time-s 1.0} :runtimes {:ordinary observation}
                                 :timing-tool tool-fixtures/corrected} [child])
          (testing "an unfinished capture cannot be reported"
            (is (thrown? java.io.FileNotFoundException (run-results/read-results directory))))
          (evidence/capture-child! directory 0 {:exit exit :out "output" :err "" :raw-result result})
          (let [before (into {} (map (fn [file] [(.getPath file) (slurp file)]))
                             (filterv #(.isFile %) (vec (file-seq directory))))]
            (if (= label "complete")
              (is (= [row] (run-results/read-results directory)))
              (do
                (is (thrown? clojure.lang.ExceptionInfo (run-results/read-results directory)))
                (is (thrown? clojure.lang.ExceptionInfo (run-results/render-saved directory)))))
            (is (= before (into {} (map (fn [file] [(.getPath file) (slurp file)]))
                                (filterv #(.isFile %) (vec (file-seq directory)))))))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))

(deftest saved-report-test
  ;; Report saved raw counts only after all evidence gates pass, without writes.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-report-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 2}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:time]}}
                   :cases [{:id :read :configuration :ordinary :contract :read
                            :operation :read :width 3}]}
        child {:run-id "report" :revision "revision" :configuration :ordinary :fork 1}
        children [child (assoc child :fork 2)]
        observation (runtime/observe)
        base {:case-id :read :configuration :ordinary :representation :record
              :fork 1 :metric :time :unit :ns/op :timing-budget {:limit-time-s 1.0} :timing-tool tool-fixtures/corrected}]
    (try
      (doseq [[label exit outcome]
              [["measured" 0 {:status :measured :raw 42 :operations 2}]
               ["unavailable" 0 {:status :unavailable :reason "Unsupported layout"}]
               ["failed" 3 {:status :measured :raw 42 :operations 2}]]]
        (let [directory (io/file parent label)
              raw (pr-str (assoc child :schema-version 1 :runtime observation
                                 :results [(merge base outcome)]))]
          (evidence/create-run! directory inventory
                                {:command ["benchmark"] :measurement {:limit-time-s 1.0} :runtimes {:ordinary observation}
                                 :timing-tool tool-fixtures/corrected} children)
          (evidence/capture-child! directory 0 {:exit exit :out "" :err "" :raw-result raw})
          (evidence/capture-child! directory 1
                                   {:exit 0 :out "" :err ""
                                    :raw-result (pr-str (assoc (second children) :schema-version 1
                                                               :runtime observation
                                                               :results [(assoc (merge base outcome) :fork 2)]))})
          (let [snapshot (fn [] (into {} (map (fn [file] [(.getPath file) (slurp file)]))
                                      (filterv #(.isFile %) (vec (file-seq directory)))))
                before (snapshot)]
            (testing label
              (let [command-result (shell/sh
                                    (str (System/getProperty "java.home") "/bin/java")
                                    "-cp" (System/getProperty "java.class.path")
                                    "clojure.main" "-m" "closed-record.bench.saved-report"
                                    (.getPath directory))]
                (is (= (if (zero? exit) 0 2) (:exit command-result)))
                (if (zero? exit)
                  (do (is (= "" (:err command-result)))
                      (is (= (str (run-results/render-saved directory) "\n")
                             (:out command-result))))
                  (do (is (= "" (:out command-result)))
                      (is (.contains (:err command-result) "Unsuccessful")))))
              (if (zero? exit)
                (let [report (run-results/render-saved directory)]
                  (is (.contains report "width=3"))
                  (is (.contains report ":ns/op"))
                  (if (= label "measured")
                    (is (.contains report "| 21.000 | 21.000–21.000 |"))
                    (do (is (.contains report "Unsupported layout"))
                        (is (.contains report "| :unavailable | — | — | — |")))))
                (is (thrown? clojure.lang.ExceptionInfo
                             (run-results/render-saved directory))))
              (is (= before (snapshot)))))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))

(deftest saved-report-command-rejection-test
  ;; Invalid invocation and missing evidence must not publish a partial table.
  (doseq [args [[] ["one" "two"] ["target/absent-saved-performance-run"]]]
    (let [result (apply shell/sh
                        (str (System/getProperty "java.home") "/bin/java")
                        "-cp" (System/getProperty "java.class.path")
                        "clojure.main" "-m" "closed-record.bench.saved-report" args)]
      (is (= 2 (:exit result)))
      (is (= "" (:out result)))
      (is (seq (:err result))))))

(deftest saved-runtime-revalidation-test
  ;; Previously valid capture diagnostics cannot certify changed raw provenance.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-runtime-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 1}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:time]}}
                   :cases [{:id :read :configuration :ordinary :contract :read}]}
        child {:run-id "runtime" :revision "revision" :configuration :ordinary :fork 1}
        observation (runtime/observe)
        row {:case-id :read :configuration :ordinary :representation :record
             :fork 1 :metric :time :unit :ns/op :status :unavailable :reason "Not measured"}
        envelope (assoc child :schema-version 1 :runtime observation :results [row])]
    (try
      (doseq [[index changed] (map-indexed vector
                                           [nil (assoc observation :version "other")
                                            (assoc observation :java-home "/other")
                                            (assoc observation :flags ["-Xmx64m"])])
              target [:manifest :result]]
        (let [directory (io/file parent (str index "-" (name target)))
              metadata {:command ["benchmark"] :measurement {:limit-time-s 1.0} :runtimes {:ordinary observation}
                        :timing-tool tool-fixtures/corrected}]
          (evidence/create-run! directory inventory metadata [child])
          (evidence/capture-child! directory 0
                                   {:exit 0 :out "" :err "" :raw-result (pr-str envelope)})
          (is (= [row] (run-results/read-results directory)))
          (if (= target :result)
            (spit (io/file directory "child-0/result.edn")
                  (pr-str (assoc envelope :runtime changed)))
            (spit (io/file directory "manifest.edn")
                  (pr-str (assoc-in (:manifest (evidence/read-run directory))
                                    [:metadata :runtimes :ordinary] changed))))
          (let [snapshot (fn [] (into {} (comp (filter #(.isFile %))
                                               (map (fn [file] [(.getPath file) (slurp file)])))
                                      (file-seq directory)))
                before (snapshot)]
            (doseq [reader [run-results/read-results run-results/render-saved]]
              (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                    (if (and (= target :manifest) (nil? changed))
                                      #"Invalid run command or runtime metadata"
                                      #"runtime differs")
                                    (reader directory))))
            (is (= before (snapshot))))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))

(deftest saved-timing-tool-test
  ;; Saved measured rows need portable provenance, not merely valid capture status.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-tool-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 1}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:time]}}
                   :cases [{:id :read :configuration :ordinary :contract :read}]}
        child {:run-id "tool" :revision "revision" :configuration :ordinary :fork 1}
        observation (runtime/observe)
        row {:case-id :read :configuration :ordinary :representation :record
             :fork 1 :metric :time :unit :ns/op :timing-budget {:limit-time-s 1.0} :status :measured :raw 20 :operations 2}]
    (try
      (doseq [[label declared observed valid?]
              [["matching" tool-fixtures/corrected
                (assoc tool-fixtures/corrected :resource "file:/child/collect.clj") true]
               ["missing-parent" nil tool-fixtures/corrected false]
               ["missing-child" tool-fixtures/corrected nil false]
               ["different" tool-fixtures/upstream tool-fixtures/corrected false]
               ["unknown" tool-fixtures/corrected
                (assoc tool-fixtures/corrected :collector-sha256 "unknown") false]]]
        (let [directory (io/file parent label)
              result (assoc row :timing-tool observed)
              metadata (cond-> {:command ["benchmark"] :measurement {:limit-time-s 1.0} :runtimes {:ordinary observation}}
                         declared (assoc :timing-tool declared))]
          (evidence/create-run! directory inventory metadata [child])
          (evidence/capture-child!
           directory 0 {:exit 0 :out "" :err ""
                        :raw-result (pr-str (assoc child :schema-version 1
                                                   :runtime observation :results [result]))})
          (let [snapshot (fn [] (into {} (comp (filter #(.isFile %))
                                               (map (fn [file] [(.getPath file) (slurp file)])))
                                      (file-seq directory)))
                before (snapshot)]
            (testing label
              (if valid?
                (is (= [result] (run-results/read-results directory)))
                (doseq [reader [run-results/read-results run-results/render-saved]]
                  (is (thrown? clojure.lang.ExceptionInfo (reader directory)))))
              (is (= before (snapshot)))))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))

(deftest saved-timing-budget-test
  ;; Raw budgets must still match after capture; rejection must not change files.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-budget-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 1}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:time]}}
                   :cases [{:id :read :configuration :ordinary :contract :read}]}
        child {:run-id "budget" :revision "revision" :configuration :ordinary :fork 1}
        observation (runtime/observe)
        budget {:limit-time-s 1.0}
        row {:case-id :read :configuration :ordinary :representation :record
             :fork 1 :metric :time :unit :ns/op :status :measured :raw 20 :operations 2
             :timing-tool tool-fixtures/corrected :timing-budget budget}
        envelope (assoc child :schema-version 1 :runtime observation :results [row])]
    (try
      (doseq [target [:manifest :result]
              [label changed] [["missing" nil] ["changed" {:limit-time-s 2.0}]
                               ["zero" {:limit-time-s 0.0}]]]
        (let [directory (io/file parent (str (name target) "-" label))]
          (evidence/create-run! directory inventory
                                {:command ["benchmark"] :runtimes {:ordinary observation}
                                 :timing-tool tool-fixtures/corrected :measurement budget}
                                [child])
          (evidence/capture-child! directory 0
                                   {:exit 0 :out "" :err "" :raw-result (pr-str envelope)})
          (is (= [row] (run-results/read-results directory)))
          (if (= target :manifest)
            (spit (io/file directory "manifest.edn")
                  (pr-str (assoc-in (:manifest (evidence/read-run directory))
                                    [:metadata :measurement] changed)))
            (spit (io/file directory "child-0/result.edn")
                  (pr-str (assoc-in envelope [:results 0 :timing-budget] changed))))
          (let [snapshot (fn [] (into {} (comp (filter #(.isFile %))
                                               (map (fn [file] [(.getPath file) (slurp file)])))
                                      (file-seq directory)))
                before (snapshot)]
            (doseq [reader [run-results/read-results run-results/render-saved]]
              (is (thrown? clojure.lang.ExceptionInfo (reader directory))))
            (is (= before (snapshot))))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))

(deftest saved-allocation-budget-test
  ;; Controls must match declared budgets, not just be internally consistent.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-allocation-budget-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 2}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:assoc {:metrics [:allocation]}}
                   :cases [{:id :assoc :configuration :ordinary :contract :assoc}]}
        child {:run-id "allocation" :revision "revision" :configuration :ordinary :fork 1}
        observation (runtime/observe)
        budget {:operations 2 :warmup-batches 1}
        outcome {:metric :allocation :unit :bytes/op :status :measured
                 :raw 40 :operations 2 :counter-before 100 :counter-after 140}
        controls (assoc budget :warmup [{:assoc outcome :boundary outcome}] :boundary outcome)
        row (merge outcome {:case-id :assoc :configuration :ordinary :representation :record
                            :fork 1 :allocation-controls controls})]
    (try
      (doseq [[label declared result valid?]
              [["matching" budget row true]
               ["missing" nil row false]
               ["operations" (assoc budget :operations 3) row false]
               ["warmup" (assoc budget :warmup-batches 2) row false]
               ["invalid" (assoc budget :operations 0) row false]
               ["malformed-controls" budget (assoc-in row [:allocation-controls :warmup] []) false]
               ["no-controls" nil (dissoc row :allocation-controls) true]
               ["unavailable" nil (merge (select-keys row [:case-id :configuration :representation :fork])
                                         {:metric :allocation :unit :bytes/op :status :unavailable
                                          :reason "Not measured"}) true]]]
        (let [directory (io/file parent label)]
          (evidence/create-run! directory inventory
                                {:command ["benchmark"] :runtimes {:ordinary observation}
                                 :measurement declared} [child (assoc child :fork 2)])
          (evidence/capture-child! directory 0
                                   {:exit 0 :out "" :err ""
                                    :raw-result (pr-str (assoc child :schema-version 1
                                                               :runtime observation :results [result]))})
          (evidence/capture-child! directory 1
                                   {:exit 0 :out "" :err ""
                                    :raw-result (pr-str (assoc child :fork 2 :schema-version 1
                                                               :runtime observation :results [(assoc result :fork 2)]))})
          (let [snapshot (fn [] (into {} (comp (filter #(.isFile %))
                                               (map (fn [file] [(.getPath file) (slurp file)])))
                                      (file-seq directory)))
                before (snapshot)]
            (testing label
              (if valid?
                (do (is (= [result (assoc result :fork 2)] (run-results/read-results directory)))
                    (is (string? (run-results/render-saved directory))))
                (doseq [reader [run-results/read-results run-results/render-saved]]
                  (is (thrown? clojure.lang.ExceptionInfo (reader directory)))))
              (is (= before (snapshot)))))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))

(deftest saved-graph-tool-test
  ;; Saved measured rows need portable provenance, not merely valid capture status.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-tool-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 2}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:graph] :graph-roots [:result]}}
                   :cases [{:id :read :configuration :ordinary :contract :read}]}
        child {:run-id "tool" :revision "revision" :configuration :ordinary :fork 1}
        observation (runtime/observe)
        row {:case-id :read :configuration :ordinary :representation :record
             :fork 1 :metric :graph :root :result :unit :bytes :status :measured :raw 20}]
    (try
      (doseq [[label declared observed valid?]
              [["unavailable" nil nil true]
               ["failed" nil nil true]
               ["matching" graph-fixtures/build-identity
                (assoc graph-fixtures/build-identity :location "file:/child/agent.jar") true]
               ["missing-parent" nil graph-fixtures/build-identity false]
               ["missing-child" graph-fixtures/build-identity nil false]
               ["different" (assoc graph-fixtures/build-identity :jar-sha256 (apply str (repeat 64 "c"))) graph-fixtures/build-identity false]
               ["different-class" graph-fixtures/build-identity
                (assoc graph-fixtures/build-identity :class-sha256 (apply str (repeat 64 "c"))) false]
               ["unknown" graph-fixtures/build-identity
                (assoc graph-fixtures/build-identity :class-sha256 "unknown") false]]]
        (let [directory (io/file parent label)
              result (if (#{"unavailable" "failed"} label)
                       (assoc (dissoc row :raw) :status (keyword label) :reason "Not measured")
                       (assoc row :graph-tool observed))
              metadata (cond-> {:command ["benchmark"] :measurement {:limit-time-s 1.0} :runtimes {:ordinary observation}}
                         declared (assoc :graph-tool declared))]
          (evidence/create-run! directory inventory metadata [child (assoc child :fork 2)])
          (evidence/capture-child!
           directory 0 {:exit 0 :out "" :err ""
                        :raw-result (pr-str (assoc child :schema-version 1
                                                   :runtime observation :results [result]))})
          (evidence/capture-child!
           directory 1 {:exit 0 :out "" :err ""
                        :raw-result (pr-str (assoc child :fork 2 :schema-version 1
                                                   :runtime observation
                                                   :results [(assoc result :fork 2)]))})
          (let [snapshot (fn [] (into {} (comp (filter #(.isFile %))
                                               (map (fn [file] [(.getPath file) (slurp file)])))
                                      (file-seq directory)))
                before (snapshot)]
            (testing label
              (if valid?
                (do (is (= [result (assoc result :fork 2)] (run-results/read-results directory)))
                    (is (string? (run-results/render-saved directory))))
                (doseq [reader [run-results/read-results run-results/render-saved]]
                  (is (thrown? clojure.lang.ExceptionInfo (reader directory)))))
              (is (= before (snapshot)))))))
      (finally
        (doseq [file (reverse (file-seq parent))] (io/delete-file file))))))
