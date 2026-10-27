(ns closed-record.assoc-suite-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [closed-record.bench.allocation :as allocation]
            [closed-record.bench.graph :as graph]
            [closed-record.bench.evidence :as evidence]
            [closed-record.bench.runtime :as runtime]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-suite :as suite])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- manifests []
  (into {}
        (map (fn [[metric scope flags]]
               [metric {:inventory {:cases [:fixture]}
                        :children (mapv #(hash-map :fork % :configuration :ordinary
                                                   :revision "same" :run-id (name metric))
                                        (range 1 6))
                        :metadata {:machine {:architecture "aarch64" :name "host"}
                                   :runtimes {:ordinary {:version "21" :java-home "/jdk"
                                                         :flags flags}}
                                   :measurement {:scope scope :acceptance? false}}}]))
        [[:time :ordinary-assoc-timing ["-XX:CompileCommand=blackhole,fixture"]]
         [:allocation :ordinary-assoc-allocation []]
         [:graph :ordinary-assoc-graph ["-javaagent:/graph.jar"]]]))

(deftest suite-compatibility-test
  ;; Pure manifests establish compatibility only; no fixture claims measurement.
  (let [input (manifests)]
    (testing "separate run IDs and measurement flags remain intact"
      (is (= input (suite/validate-manifests input))))
    (testing "incompatible source, workload, machine, or runtime rejects"
      (doseq [path [[:graph :inventory]
                    [:allocation :children 0 :revision]
                    [:graph :metadata :machine]
                    [:time :metadata :runtimes :ordinary :version]
                    [:allocation :metadata :runtimes :ordinary :java-home]]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (suite/validate-manifests (assoc-in input path "different")))
            (str path))))
    (testing "missing members and invalid diagnostic scope reject"
      (doseq [invalid [(dissoc input :graph)
                       (assoc input :other {})
                       (assoc-in input [:graph :children] [])
                       (assoc-in input [:time :metadata :machine] {})
                       (assoc-in input [:graph :metadata :measurement :acceptance?] true)
                       (assoc-in input [:graph :metadata :measurement :scope] :other)]]
        (is (thrown? clojure.lang.ExceptionInfo (suite/validate-manifests invalid)))))
    (testing "instrumentation cannot silently enter timing"
      (doseq [flag ["-javaagent:/a.jar" "-agentlib:jdwp" "-agentpath:/a.so" "-Xrunhprof"]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (suite/validate-manifests
                      (assoc-in input [:time :metadata :runtimes :ordinary :flags] [flag]))))))))

(defn- save-suite! [parent]
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        timing-tool {:library 'org.hugoduncan/criterium
                     :base-version "0.5.251-ALPHA" :distribution :upstream
                     :collector-sha256 "a73da4deaa664aa68ddbeb0989e7dcb22a5878864a720b662c00aa9ab1557fa2"}
        graph-tool {:jar-sha256 (apply str (repeat 64 "a"))
                    :class-sha256 (apply str (repeat 64 "b"))}
        allocated (allocation/outcome 0 128 2)
        boundary (allocation/outcome 0 0 2)]
    (into {}
          (map
           (fn [[metric template]]
             (let [directory (io/file parent (name metric))
                   metadata (-> (:metadata template)
                                (assoc :command ["fixture"]
                                       :runtimes {:ordinary (runtime/observe)})
                                (cond-> (= metric :time)
                                  (assoc :timing-tool timing-tool)
                                  (= metric :graph) (assoc :graph-tool graph-tool))
                                (update :measurement merge
                                        (case metric
                                          :time {:limit-time-s 1.0}
                                          :allocation {:operations 2 :warmup-batches 1}
                                          :graph {})))]
               (evidence/create-run! directory inventory metadata (:children template))
               (doseq [[index association] (map-indexed vector (:children template))]
                 (let [outcomes (case metric
                                  :time [{:metric :time :status :measured :unit :ns/op
                                          :raw 100.0 :operations 2 :timing-tool timing-tool
                                          :timing-budget {:limit-time-s 1.0}}]
                                  :allocation [(assoc allocated :allocation-controls
                                                      {:operations 2 :warmup-batches 1
                                                       :warmup [{:assoc allocated :boundary boundary}]
                                                       :boundary boundary})]
                                  :graph (mapv #(assoc % :graph-tool graph-tool)
                                               (graph/outcomes 64 64 104)))
                       rows (vec (for [case-id [:assoc/width-3 :assoc/width-16]
                                       representation [:closed-record :defrecord]
                                       outcome outcomes]
                                   (merge outcome {:case-id case-id :representation representation
                                                   :configuration :ordinary :fork (:fork association)})))]
                   (evidence/capture-child!
                    directory index
                    {:exit 0 :out "" :err ""
                     :raw-result (pr-str (assoc association :schema-version 1
                                                :runtime (runtime/observe) :results rows))})))
               [metric directory])))
          (manifests))))

(defn- snapshot [directory]
  (into {} (comp (filter #(.isFile %))
                 (map (fn [file] [(str file) (slurp file)])))
        (file-seq directory)))

(deftest saved-suite-test
  ;; Numerical real-file fixtures exercise composition, not measurement or acceptance.
  (let [parent (.toFile (Files/createTempDirectory "assoc-suite" (make-array FileAttribute 0)))]
    (try
      (let [directories (save-suite! parent)
            before (snapshot parent)
            sources (suite/read-results directories)]
        (testing "source manifests and raw metric rows remain separate"
          (is (= {:time 20 :allocation 20 :graph 60}
                 (update-vals sources #(count (:results %)))))
          (doseq [[metric directory] directories]
            (is (= (:manifest (evidence/read-run directory))
                   (get-in sources [metric :manifest]))))
          (is (= sources (suite/read-results directories)))
          (is (= before (snapshot parent))))
        (testing "combined report keeps metrics, controls, and source provenance"
          (let [report (suite/render-saved directories)]
            (is (= report (suite/render-saved directories)))
            (doseq [text ["ns/op" "bytes/op" "Source manifests"
                          "not confidence bounds" "full-matrix acceptance"]]
              (is (str/includes? report text) text))
            (doseq [[_ source] sources]
              (is (str/includes? report (pr-str (:manifest source)))))
            (is (= before (snapshot parent)))))
        (testing "missing and extra sources reject"
          (is (thrown? clojure.lang.ExceptionInfo
                       (suite/read-results (dissoc directories :graph))))
          (is (thrown? clojure.lang.ExceptionInfo
                       (suite/read-results (assoc directories :other parent)))))
        (testing "each reader rejects invalid captures without writes"
          (doseq [[_ directory] directories]
            (let [file (io/file directory "child-0/outcome.edn")
                  original (slurp file)]
              (spit file (pr-str (assoc (edn/read-string original) :exit 2)))
              (let [changed (snapshot parent)]
                (is (thrown? clojure.lang.ExceptionInfo (suite/read-results directories)))
                (is (thrown? clojure.lang.ExceptionInfo (suite/render-saved directories)))
                (is (= changed (snapshot parent))))
              (spit file original))))
        (testing "individually valid but incompatible runs reject without writes"
          (let [file (io/file (:graph directories) "manifest.edn")
                original (slurp file)]
            (spit file (pr-str (assoc-in (edn/read-string original)
                                         [:metadata :machine :name] "other-host")))
            (let [changed (snapshot parent)]
              (is (thrown? clojure.lang.ExceptionInfo (suite/read-results directories)))
              (is (thrown? clojure.lang.ExceptionInfo (suite/render-saved directories)))
              (is (= changed (snapshot parent))))
            (spit file original)))
        (is (= before (snapshot parent))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest saved-suite-command-test
  ;; Real JVM exits and file snapshots verify the read-only command boundary.
  (let [parent (.toFile (Files/createTempDirectory "assoc suite report" (make-array FileAttribute 0)))
        command [(str (io/file (System/getProperty "java.home") "bin" "java"))
                 "-cp" (System/getProperty "java.class.path") "clojure.main"
                 "-m" "closed-record.bench.assoc-suite-report"]
        invoke (fn [args] (apply shell/sh (into command args)))]
    (try
      (let [directories (save-suite! parent)
            args (mapv #(str (get directories %)) [:time :allocation :graph])]
        (testing "success prints the validated combined report"
          (let [before (snapshot parent)
                result (invoke args)]
            (is (= 0 (:exit result)))
            (is (= "" (:err result)))
            (is (= (str (suite/render-saved directories) "\n") (:out result)))
            (is (= before (snapshot parent)))))
        (testing "bad arguments, missing sources, and swapped sources reject"
          (doseq [invalid [[] (pop args) (conj args "extra")
                           (assoc args 0 (str (io/file parent "absent")))
                           (assoc args 0 (args 2) 2 (args 0))]]
            (let [before (snapshot parent)
                  result (invoke invalid)]
              (is (= 2 (:exit result)))
              (is (= "" (:out result)))
              (is (seq (:err result)))
              (is (= before (snapshot parent))))))
        (testing "incompatible saved machine declarations reject before output"
          (let [file (io/file (:graph directories) "manifest.edn")]
            (spit file (pr-str (assoc-in (edn/read-string (slurp file))
                                         [:metadata :machine :name] "other-host")))
            (let [before (snapshot parent)
                  result (invoke args)]
              (is (= 2 (:exit result)))
              (is (= "" (:out result)))
              (is (seq (:err result)))
              (is (= before (snapshot parent)))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
