(ns closed-record.bench-process-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.evidence :as evidence]
   [closed-record.bench.fork :as fork])
  (:import (java.nio.file Files FileAlreadyExistsException)))

(defn- run-metadata
  [children]
  {:command ["test-benchmark"]
   :runtimes (into {} (map (fn [child]
                             [(:configuration child)
                              {:version "fixture-runtime" :java-home "/fixture/jdk"
                               :flags []}])) children)})

(deftest real-process-capture-test
  ;; Real shell children exercise file transport without measurement tools.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-process-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run with spaces")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "process" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        children (mapv #(assoc child :fork %) (range 1 6))]
    (try
      (evidence/create-run! directory inventory (run-metadata children) children)
      (testing "completed processes retain independent exit and validation results"
        (doseq [[index exit mode expected]
                [[0 0 :valid :valid] [1 7 :valid :valid]
                 [2 0 :missing :missing] [3 2 :malformed :invalid]
                 [4 0 :wrong-association :invalid]]]
          (let [raw (case mode
                      :malformed "{broken"
                      :wrong-association (pr-str (assoc child :schema-version 1 :results []))
                      (pr-str (assoc (nth children index) :schema-version 1 :results [])))
                command (fn [_ path]
                          ["sh" "-c"
                           (str "printf 'output λ\\n'; printf 'diagnostic\\n' >&2; "
                                (when-not (= :missing mode) "printf '%s' \"$2\" > \"$1\"; ")
                                "exit " exit)
                           "child" path raw])
                outcome (fork/capture-process! directory index command)]
            (is (= exit (:exit outcome)))
            (is (= expected (get-in outcome [:validation :status])))
            (is (= "output λ\n" (slurp (io/file directory (str "child-" index) "stdout.txt")
                                       :encoding "UTF-8")))
            (is (= "diagnostic\n" (slurp (io/file directory (str "child-" index) "stderr.txt"))))
            (is (= exit (edn/read-string (slurp (io/file directory (str "process-" index) "exit.edn"))))))))
      (testing "later failures and retries leave the first result intact"
        (let [file (io/file directory "child-0/result.edn")
              original (slurp file)]
          (is (= (assoc child :schema-version 1 :results []) (edn/read-string original)))
          (is (thrown? FileAlreadyExistsException
                       (fork/capture-process! directory 0 (fn [_ _] ["false"]))))
          (is (= original (slurp file)))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest launch-failure-capture-test
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-launch-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "launch" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        command [(.getAbsolutePath (io/file parent "absent-executable"))]]
    (try
      (evidence/create-run! directory inventory (run-metadata [child]) [child])
      (let [outcome (fork/capture-process! directory 0 (fn [_ _] command))
            saved (io/file directory "process-0/outcome.edn")
            original (slurp saved)]
        (is (= child (:child outcome)))
        (is (= :launch (get-in outcome [:failure :stage])))
        (is (seq (get-in outcome [:failure :reason])))
        (is (= "java.io.IOException" (get-in outcome [:failure :exception-class])))
        (is (not (contains? outcome :exit)))
        (is (not (contains? outcome :validation)))
        (is (= outcome (edn/read-string original)))
        (is (= command (edn/read-string (slurp (io/file directory "process-0/command.edn")))))
        (is (not (.exists (io/file directory "process-0/exit.edn"))))
        (is (thrown? FileAlreadyExistsException
                     (fork/capture-process! directory 0 (fn [_ _] ["true"]))))
        (is (= original (slurp saved))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest result-read-failure-capture-test
  ;; A real child leaves a directory where a result file is required.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-read-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "read" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}]
    (try
      (evidence/create-run! directory inventory (run-metadata [child]) [child])
      (let [outcome (fork/capture-process!
                     directory 0
                     (fn [_ path]
                       ["sh" "-c" "printf output; printf diagnostic >&2; mkdir \"$1\"; exit 7"
                        "child" path]))
            workspace (io/file directory "process-0")
            saved (io/file workspace "outcome.edn")
            original (slurp saved)]
        (is (= child (:child outcome)))
        (is (= 7 (:exit outcome)))
        (is (= :result-read (get-in outcome [:failure :stage])))
        (is (seq (get-in outcome [:failure :reason])))
        (is (seq (get-in outcome [:failure :exception-class])))
        (is (not (contains? outcome :validation)))
        (is (= outcome (edn/read-string original)))
        (is (= 7 (edn/read-string (slurp (io/file workspace "exit.edn")))))
        (is (= "output" (slurp (io/file workspace "stdout.txt"))))
        (is (= "diagnostic" (slurp (io/file workspace "stderr.txt"))))
        (is (.isDirectory (io/file workspace "result.edn")))
        (is (thrown? FileAlreadyExistsException
                     (fork/capture-process! directory 0 (fn [_ _] ["true"]))))
        (is (= original (slurp saved))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest invalid-command-test
  ;; Invalid commands leave the planned slot free for a corrected real launch.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-command-" (make-array java.nio.file.attribute.FileAttribute 0)))
        directory (io/file parent "run")
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "command" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}]
    (try
      (evidence/create-run! directory inventory (run-metadata [child]) [child])
      (testing "malformed commands fail before filesystem or process effects"
        (doseq [command [nil [] "sh" '("sh") [nil] [42] [""] ["  "]
                         ["sh" nil] ["sh" 1] ["sh\u0000"] ["sh" "a\u0000b"]]]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid child command"
                                (fork/capture-process! directory 0 (fn [_ _] command))))
          (is (not (.exists (io/file directory "process-0"))))))
      (testing "empty arguments are passed intact to a real process"
        (let [outcome (fork/capture-process!
                       directory 0
                       (fn [_ _] ["sh" "-c" "test \"$#\" -eq 1 && test -z \"$1\"" "child" ""]))]
          (is (= 0 (:exit outcome)))
          (is (= :missing (get-in outcome [:validation :status])))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))

(deftest capture-io-failure-preserves-artifacts-test
  ;; Real children create deterministic read and write conflicts, not mocks.
  (doseq [mode [:stdout-read :stderr-read :capture-write]]
    (let [parent (.toFile (Files/createTempDirectory
                           "bench-capture-io-" (make-array java.nio.file.attribute.FileAttribute 0)))
          directory (io/file parent "run")
          inventory (edn/read-string (slurp "dev/bench/cases.edn"))
          child {:run-id "capture-io" :revision "abc123"
                 :configuration (:configuration (first (:cases inventory))) :fork 1}
          workspace (io/file directory "process-0")
          raw (pr-str (assoc child :schema-version 1 :results []))
          stream (case mode :stdout-read "stdout.txt" :stderr-read "stderr.txt" nil)]
      (try
        (evidence/create-run! directory inventory (run-metadata [child]) [child])
        (let [manifest (slurp (io/file directory "manifest.edn"))
              command (fn [_ path]
                        ["sh" "-c"
                         (str "printf output; printf diagnostic >&2; printf '%s' \"$2\" > \"$1\"; "
                              (if stream
                                "mv \"$3/$4\" \"$3/$4.saved\"; mkdir \"$3/$4\""
                                "mkdir \"$3/../child-0\"; printf sentinel > \"$3/../child-0/marker\""))
                         "child" path raw (.getAbsolutePath workspace) (or stream "")])]
          (testing (str "IO failure propagates: " mode)
            (is (thrown? java.io.IOException
                         (fork/capture-process! directory 0 command)))
            (is (= 0 (edn/read-string (slurp (io/file workspace "exit.edn")))))
            (is (= raw (slurp (io/file workspace "result.edn"))))
            (is (vector? (edn/read-string (slurp (io/file workspace "command.edn")))))
            (is (= manifest (slurp (io/file directory "manifest.edn"))))
            (is (not (.exists (io/file workspace "outcome.edn"))))
            (is (not (.exists (io/file directory "child-0/outcome.edn")))))
          (is (= "output" (slurp (io/file workspace (if (= mode :stdout-read)
                                                      "stdout.txt.saved" "stdout.txt")))))
          (is (= "diagnostic" (slurp (io/file workspace (if (= mode :stderr-read)
                                                          "stderr.txt.saved" "stderr.txt")))))
          (when (= mode :capture-write)
            (is (= "sentinel" (slurp (io/file directory "child-0/marker"))))))
        (finally
          (doseq [file (reverse (file-seq parent))]
            (io/delete-file file)))))))

(deftest invalid-result-identity-process-test
  ;; Valid EDN from a successful process must still satisfy inventory identity.
  (let [parent (.toFile (Files/createTempDirectory
                         "bench-identity-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory {:execution {:forks 1}
                   :configurations {:ordinary {:representations [:record]}}
                   :contracts {:read {:metrics [:time]}}
                   :cases [{:id :read :configuration :ordinary :contract :read}]}
        child {:run-id "identity" :revision "abc123" :configuration :ordinary :fork 1}
        row {:case-id :read :configuration :ordinary :representation :record
             :fork 1 :metric :time :unit :ns/op :status :measured
             :raw 20 :operations 2}]
    (try
      (doseq [field [:case-id :configuration :representation]]
        (let [directory (io/file parent (name field))
              raw (pr-str (assoc child :schema-version 1
                                 :results [(assoc row field :unknown)]))]
          (evidence/create-run! directory inventory (run-metadata [child]) [child])
          (let [outcome (fork/capture-process!
                         directory 0
                         (fn [_ path]
                           ["sh" "-c" "printf '%s' \"$2\" > \"$1\"" "child" path raw]))]
            (testing (str "invalid " field " is retained but rejected")
              (is (= 0 (:exit outcome)))
              (is (= child (:child outcome)))
              (is (= :invalid (get-in outcome [:validation :status])))
              (is (seq (get-in outcome [:validation :reason])))
              (is (false? (evidence/successful-child? outcome)))
              (is (= raw (slurp (io/file directory "process-0/result.edn"))))
              (is (= raw (slurp (io/file directory "child-0/result.edn"))))
              (is (= outcome (edn/read-string
                              (slurp (io/file directory "child-0/outcome.edn")))))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
