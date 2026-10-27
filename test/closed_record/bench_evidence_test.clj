(ns closed-record.bench-evidence-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.evidence :as evidence])
  (:import (java.nio.file Files FileAlreadyExistsException)))

(defn- run-metadata
  [children]
  {:command ["test-benchmark"]
   :runtimes (into {} (map (fn [child]
                             [(:configuration child)
                              {:version "fixture-runtime" :java-home "/fixture/jdk"
                               :flags []}])) children)})

(deftest durable-manifest-test
  ;; A later failed attempt must not replace the original run association.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-evidence-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "run-019" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        metadata (run-metadata [child])]
    (try
      (testing "the manifest survives a duplicate run attempt"
        (let [manifest (evidence/create-run! directory inventory metadata [child])
              file (io/file directory "manifest.edn")
              original (slurp file)]
          (is (= manifest (edn/read-string original)))
          (is (= inventory (:inventory manifest)))
          (is (= metadata (:metadata manifest)))
          (is (thrown? FileAlreadyExistsException
                       (evidence/create-run! directory inventory metadata [child])))
          (is (= original (slurp file)))))
      (testing "invalid associations fail before creating a directory"
        (doseq [children [[] [child child] [(assoc child :fork 0)]
                          [child (assoc child :fork 2 :revision "other")]]]
          (let [invalid (io/file parent "invalid")]
            (is (thrown? clojure.lang.ExceptionInfo
                         (evidence/create-run! invalid inventory metadata children)))
            (is (not (.exists invalid))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest child-capture-test
  ;; Failed later children preserve earlier evidence and their own raw diagnostics.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-capture-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "capture" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        children (mapv #(assoc child :fork %) (range 1 6))
        valid (pr-str (assoc child :schema-version 1 :results []))]
    (try
      (evidence/create-run! directory inventory (run-metadata children) children)
      (testing "capture validates against the persisted association"
        (doseq [[index raw exit status]
                [[0 valid 0 :valid]
                 [1 "{broken" 3 :invalid]
                 [2 nil 0 :missing]
                 [3 valid 0 :invalid]
                 [4 (pr-str (assoc (nth children 4) :schema-version 1 :results []))
                  7 :valid]]]
          (let [outcome (evidence/capture-child!
                         directory index {:exit exit :out "output λ\n" :err "diagnostic\n"
                                          :raw-result raw})
                slot (io/file directory (str "child-" index))]
            (is (= status (get-in outcome [:validation :status])))
            (is (= exit (:exit outcome)))
            (is (= (nth children index) (:child outcome)))
            (is (= outcome (edn/read-string (slurp (io/file slot "outcome.edn")))))
            (is (= "output λ\n" (slurp (io/file slot "stdout.txt") :encoding "UTF-8")))
            (is (= "diagnostic\n" (slurp (io/file slot "stderr.txt"))))
            (is (= exit (:exit (edn/read-string (slurp (io/file slot "process.edn"))))))
            (if raw
              (is (= raw (slurp (io/file slot "result.edn"))))
              (is (not (.exists (io/file slot "result.edn"))))))))
      (testing "retries and invalid indices cannot replace saved evidence"
        (is (thrown? FileAlreadyExistsException
                     (evidence/capture-child! directory 0 {:exit 9 :out "" :err ""})))
        (doseq [index [-1 5 "0"]]
          (is (thrown? clojure.lang.ExceptionInfo
                       (evidence/capture-child! directory index {:exit 0 :out "" :err ""}))))
        (is (= valid (slurp (io/file directory "child-0/result.edn")))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest required-metadata-test
  ;; Metadata failures must leave no run directory or partial manifest.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-metadata-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        configuration (:configuration (first (:cases inventory)))
        child {:run-id "metadata" :revision "abc123" :configuration configuration :fork 1}
        metadata (run-metadata [child])
        runtime-path [:runtimes configuration]]
    (try
      (testing "command and every selected runtime are required"
        (doseq [invalid [nil {} (dissoc metadata :command)
                         (assoc metadata :command [])
                         (assoc metadata :command [" "])
                         (assoc metadata :command ["java" nil])
                         (assoc metadata :command '("java"))
                         (assoc metadata :runtimes {})
                         (assoc-in metadata [:runtimes :unknown] {})
                         (update-in metadata runtime-path dissoc :version)
                         (assoc-in metadata (conj runtime-path :version) " ")
                         (assoc-in metadata (conj runtime-path :java-home) nil)
                         (update-in metadata runtime-path dissoc :flags)
                         (assoc-in metadata (conj runtime-path :flags) [nil])]]
          (is (thrown? clojure.lang.ExceptionInfo
                       (evidence/create-run! directory inventory invalid [child])))
          (is (not (.exists directory)))))
      (testing "explicit empty flags and additional provenance survive storage"
        (let [complete (assoc metadata :machine {:cpu "fixture"})]
          (is (= complete (:metadata (evidence/create-run! directory inventory complete [child]))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest successful-child-test
  ;; Exit, transport and runtime checks are independent gates, not alternatives.
  (let [valid {:exit 0 :validation {:status :valid}
               :runtime-validation {:status :valid}}]
    (testing "only all successful gates permit subsequent coverage validation"
      (is (true? (evidence/successful-child? valid)))
      (doseq [outcome [nil {} (assoc valid :exit 2) (dissoc valid :exit)
                       (assoc valid :failure {:stage :launch})
                       (assoc valid :failure {:stage :result-read})
                       (dissoc valid :validation)
                       (dissoc valid :runtime-validation)]]
        (is (false? (evidence/successful-child? outcome)) (pr-str outcome))))
    (testing "every non-valid diagnostic blocks the process gate"
      (doseq [gate [:validation :runtime-validation]
              status [:missing :invalid :unavailable nil]]
        (is (false? (evidence/successful-child?
                     (assoc valid gate {:status status}))))))))

(deftest saved-manifest-schema-test
  ;; Unknown schemas must fail before capture reads, without changing evidence.
  (let [directory (.toFile (Files/createTempDirectory
                            "bench-schema-" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file directory "manifest.edn")]
    (try
      (testing "missing and unsupported versions are not interpreted as version one"
        (doseq [manifest [{} {:schema-version nil} {:schema-version 0}
                          {:schema-version 2} {:schema-version "1"}]]
          (let [text (pr-str (assoc manifest :children [{}]))]
            (spit file text)
            (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                  #"Unsupported saved manifest schema"
                                  (evidence/read-run directory)))
            (is (= text (slurp file)))
            (is (= ["manifest.edn"] (mapv #(.getName %) (.listFiles directory)))))))
      (finally
        (io/delete-file file true)
        (io/delete-file directory)))))

(deftest saved-manifest-contract-test
  ;; Saved declarations must meet creation rules before any capture is read.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-saved-contract-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "saved" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        metadata (run-metadata [child])]
    (try
      (let [manifest (evidence/create-run! directory inventory metadata [child])
            file (io/file directory "manifest.edn")]
        (testing "invalid version-one manifests fail without reading absent captures"
          (doseq [invalid [(dissoc manifest :children)
                           (assoc manifest :children [])
                           (assoc manifest :children [child child])
                           (assoc manifest :children [(assoc child :fork 0)])
                           (assoc manifest :children [(assoc child :configuration :unknown)])
                           (assoc manifest :children [child (assoc child :fork 2 :revision "other")])
                           (assoc manifest :children [child (assoc child :fork 2 :run-id "other")])
                           (assoc manifest :metadata {})
                           (update manifest :metadata dissoc :command)
                           (assoc-in manifest [:metadata :runtimes] {})
                           (update-in manifest [:metadata :runtimes (:configuration child)] dissoc :flags)]]
            (let [text (pr-str invalid)]
              (spit file text)
              (is (thrown? clojure.lang.ExceptionInfo (evidence/read-run directory)))
              (is (= text (slurp file)))
              (is (= ["manifest.edn"] (mapv #(.getName %) (.listFiles directory))))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest timing-declaration-manifest-test
  ;; Validate optional timing provenance on creation and on saved reads.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-tool-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "tool" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        identity {:library 'org.hugoduncan/criterium
                  :base-version "0.5.251-ALPHA" :distribution :upstream
                  :collector-sha256 "a73da4deaa664aa68ddbeb0989e7dcb22a5878864a720b662c00aa9ab1557fa2"}
        metadata (assoc (run-metadata [child]) :timing-tool identity)]
    (try
      (testing "invalid declarations leave no directory"
        (doseq [invalid [nil {} (assoc identity :distribution :local-correction)]]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid timing tool"
                                (evidence/create-run! directory inventory
                                                      (assoc metadata :timing-tool invalid)
                                                      [child])))
          (is (not (.exists directory)))))
      (let [manifest (evidence/create-run! directory inventory metadata [child])
            file (io/file directory "manifest.edn")]
        (is (= identity (get-in (edn/read-string (slurp file)) [:metadata :timing-tool])))
        (testing "saved declarations are checked before absent capture slots"
          (doseq [invalid [nil {} (assoc identity :base-version "other")]]
            (let [text (pr-str (assoc-in manifest [:metadata :timing-tool] invalid))]
              (spit file text)
              (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid timing tool"
                                    (evidence/read-run directory)))
              (is (= text (slurp file)))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest graph-declaration-manifest-test
  ;; Validate optional graph provenance on creation and on saved reads.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-graph-tool-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "tool" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        identity {:jar-sha256 (apply str (repeat 64 "a"))
                  :class-sha256 (apply str (repeat 64 "b"))}
        metadata (assoc (run-metadata [child]) :graph-tool identity)]
    (try
      (testing "invalid declarations leave no directory"
        (doseq [invalid [nil {} (dissoc identity :class-sha256)]]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid graph tool"
                                (evidence/create-run! directory inventory
                                                      (assoc metadata :graph-tool invalid)
                                                      [child])))
          (is (not (.exists directory)))))
      (let [manifest (evidence/create-run! directory inventory metadata [child])
            file (io/file directory "manifest.edn")]
        (is (= identity (get-in (edn/read-string (slurp file)) [:metadata :graph-tool])))
        (testing "saved declarations are checked before absent capture slots"
          (doseq [invalid [nil {} (assoc identity :jar-sha256 "other")]]
            (let [text (pr-str (assoc-in manifest [:metadata :graph-tool] invalid))]
              (spit file text)
              (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid graph tool"
                                    (evidence/read-run directory)))
              (is (= text (slurp file)))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
