(ns closed-record.prose-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- put! [root path content]
  (let [file (io/file root path)]
    (io/make-parents file)
    (spit file content)))

(defn- with-project [f]
  (let [root (.toFile (Files/createTempDirectory "prose-" (make-array FileAttribute 0)))]
    (try
      (io/copy (io/file "bb.edn") (io/file root "bb.edn"))
      (doseq [path ["README.md" "README/nested/guide.md" "AGENTS.md" "doc/API.md"]]
        (put! root path "Clear prose.\n"))
      (put! root "styles/config/vocabularies/ClosedRecord/accept.txt" "ClosedRecord\n")
      (put! root ".vale.ini" "StylesPath = styles\nMinAlertLevel = suggestion\n[*.md]\nBasedOnStyles = Probe\n")
      (doseq [severity ["suggestion" "warning" "error"]]
        (put! root (str "styles/Probe/" severity ".yml")
              (str "extends: existence\nmessage: 'Found %s.'\nlevel: " severity
                   "\ntokens:\n  - " severity "token\n")))
      (f root)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

(defn- check!
  ([root args] (check! root args (into {} (System/getenv))))
  ([root args environment]
   (let [executable (.trim (:out (shell/sh "which" "bb")))]
     (shell/sh executable "-cp" (.getCanonicalPath (io/file "dev/quality"))
               "-e" (str "(require '[closed-record.prose :as prose])"
                         "(let [result (prose/check! " (pr-str (str root)) " " (pr-str args) ")]"
                         "(print (:out result)) (flush) (binding [*out* *err*] (print (:err result)) (flush))"
                         "(System/exit (:exit result)))")
               :env environment))))

(defn- snapshot [root]
  (into {} (comp (filter #(.isFile %))
                 (map (fn [file] [(str file) (slurp file)])))
        (file-seq root)))

(deftest real-vale-command-test
  ;; Real Vale rules test exit policy and recursive scope without network or mocks.
  (with-project
    (fn [root]
      (is (zero? (:exit (check! root []))))
      (doseq [[severity expected] [["suggestion" 0] ["warning" 1] ["error" 1]]]
        (testing severity
          (put! root "README/nested/guide.md" (str severity "token\n"))
          (let [before (snapshot root)
                result (check! root [])]
            (is (= expected (:exit result)) (pr-str result))
            (is (.contains (:out result) (str "Probe." severity)) (pr-str result))
            (is (= before (snapshot root)))))))))

(deftest invalid-prose-setup-test
  ;; Missing scope, invalid arguments, and real style failures cannot pass or write.
  (with-project
    (fn [root]
      (let [before (snapshot root)
            result (check! root ["--help"])]
        (is (pos? (:exit result)))
        (is (.contains (:err result) "Usage: bb vale"))
        (is (= before (snapshot root))))
      (doseq [path ["README.md" "doc/API.md" ".vale.ini"]]
        (let [file (io/file root path)
              content (slurp file)]
          (io/delete-file file)
          (let [before (snapshot root)
                result (check! root [])]
            (is (pos? (:exit result)))
            (is (.contains (:err result) path))
            (is (= before (snapshot root))))
          (spit file content)))
      (put! root ".vale.ini" "StylesPath = styles\n[*.md]\nBasedOnStyles = Absent\n")
      (let [before (snapshot root)
            result (check! root [])]
        (is (pos? (:exit result)) (pr-str result))
        (is (= before (snapshot root))))
      (put! root ".vale.ini" "StylesPath = styles\nPackages = /nonexistent/closed-record-style.zip\n[*.md]\nBasedOnStyles = Probe\n")
      (let [before (snapshot root)
            result (check! root [])]
        (is (pos? (:exit result)) (pr-str result))
        (is (= before (snapshot root)))))))

(deftest missing-vale-executable-test
  ;; An empty executable search path exercises a real launch failure, not a stub.
  (with-project
    (fn [root]
      (let [empty-bin (io/file root "empty-bin")
            _ (.mkdir empty-bin)
            before (snapshot root)
            environment (assoc (into {} (System/getenv)) "PATH" (str empty-bin))
            result (check! root [] environment)]
        (is (pos? (:exit result)) (pr-str result))
        (is (.contains (:err result) "vale") (pr-str result))
        (is (= before (snapshot root)))))))

(defn- git! [root environment & args]
  (let [result (apply shell/sh "git" (concat args [:dir (str root) :env environment]))]
    (when-not (zero? (:exit result))
      (throw (ex-info "Fixture Git command failed" result)))
    (:out result)))

(defn- tracked-state [root environment]
  (let [paths (.split (git! root environment "ls-files" "-z") "\u0000")]
    {:index (git! root environment "write-tree")
     :files (into {} (map (fn [path]
                            (let [file (io/file root path)]
                              [path (if (Files/isSymbolicLink (.toPath file))
                                      (str (Files/readSymbolicLink (.toPath file)))
                                      (when (.exists file)
                                        (vec (Files/readAllBytes (.toPath file)))))])))
                  paths)}))

(deftest tracked-prose-preservation-test
  ;; Real Git tracks binary and dirty files as well as the selected prose.
  (with-project
    (fn [root]
      (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                     (System/getenv))
                               "GIT_CONFIG_NOSYSTEM" "1"
                               "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
            original-config (slurp (io/file root ".vale.ini"))]
        (put! root "gitconfig" "")
        (git! root environment "init")
        (git! root environment "config" "core.autocrlf" "false")
        (with-open [output (io/output-stream (io/file root "unrelated.bin"))]
          (.write output (byte-array [0 1 -1 13 10])))
        (git! root environment "add" ".")
        (put! root "AGENTS.md" "Unstaged prose.\r\n")
        (doseq [[label content config expected]
                [["clean" "Clear prose.\n" original-config 0]
                 ["suggestion" "suggestiontoken\n" original-config 0]
                 ["warning" "warningtoken\n" original-config 1]
                 ["error" "errortoken\n" original-config 1]
                 ["missing style" "Clear prose.\n"
                  "StylesPath = styles\n[*.md]\nBasedOnStyles = Absent\n" 2]
                 ["sync failure" "Clear prose.\n"
                  "StylesPath = styles\nPackages = /nonexistent/closed-record-style.zip\n[*.md]\nBasedOnStyles = Probe\n" 2]]]
          (testing label
            (put! root "README/nested/guide.md" content)
            (put! root ".vale.ini" config)
            (let [before (tracked-state root environment)
                  result (check! root [] environment)]
              (is (= expected (:exit result)) (pr-str result))
              (is (= before (tracked-state root environment))))))
        (let [empty-bin (io/file root "empty-bin")]
          (put! root ".vale.ini" original-config)
          (.mkdir empty-bin)
          (doseq [[label args process-environment diagnostic]
                  [["invalid arguments" ["--help"] environment "Usage: bb vale"]
                   ["missing executable" [] (assoc environment "PATH" (str empty-bin)) "vale"]]]
            (testing label
              (let [before (tracked-state root environment)
                    result (check! root args process-environment)]
                (is (pos? (:exit result)) (pr-str result))
                (is (.contains (:err result) diagnostic) (pr-str result))
                (is (= before (tracked-state root environment)))))))
        (io/delete-file (io/file root "doc/API.md"))
        (let [before (tracked-state root environment)
              result (check! root [] environment)]
          (is (pos? (:exit result)) (pr-str result))
          (is (= before (tracked-state root environment))))))))

(deftest wrong-prose-path-types-test
  ;; Each wrong type must fail before style setup and preserve files and index.
  (doseq [path ["README.md" "AGENTS.md" "doc/API.md" ".vale.ini"
                "styles/config/vocabularies/ClosedRecord/accept.txt" "README"]]
    (testing path
      (with-project
        (fn [root]
          (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                         (System/getenv))
                                   "GIT_CONFIG_NOSYSTEM" "1"
                                   "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
                file (io/file root path)]
            (put! root "gitconfig" "")
            (git! root environment "init")
            (git! root environment "config" "core.autocrlf" "false")
            (doseq [entry (reverse (file-seq file))]
              (io/delete-file entry))
            (if (= path "README")
              (spit file "Not a directory.\n")
              (.mkdir file))
            (put! root "unrelated.txt" "Staged prose.\n")
            (git! root environment "add" ".")
            (put! root "unrelated.txt" "Unstaged prose.\r\n")
            (let [before (tracked-state root environment)
                  all-files (snapshot root)
                  result (check! root [] environment)]
              (is (pos? (:exit result)) (pr-str result))
              (is (.contains (:err result) "Invalid required quality path types:")
                  (pr-str result))
              (is (.contains (:err result) path) (pr-str result))
              (is (= before (tracked-state root environment)))
              (is (= all-files (snapshot root))))))))))

(defn- task! [root environment]
  (shell/sh "bb" "-cp" (.getCanonicalPath (io/file "dev/quality")) "vale"
            :dir (str root) :env environment))

(deftest prose-selection-test
  ;; The wrapper must check each maintained document, but not historical prose.
  (with-project
    (fn [root]
      (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                     (System/getenv))
                               "GIT_CONFIG_NOSYSTEM" "1"
                               "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
            excluded ["munera/closed/001-history/design.md" "ramora/history.md"]]
        (put! root "gitconfig" "")
        (doseq [path excluded]
          (put! root path "Clear prose.\n"))
        (put! root "unrelated.txt" "Staged prose.\n")
        (git! root environment "init")
        (git! root environment "config" "core.autocrlf" "false")
        (git! root environment "add" ".")
        (put! root "unrelated.txt" "Unstaged prose.\r\n")
        (testing "clean maintained documents pass"
          (let [before (tracked-state root environment)
                result (task! root environment)]
            (is (zero? (:exit result)) (pr-str result))
            (is (= before (tracked-state root environment)))))
        (doseq [path ["README.md" "AGENTS.md" "doc/API.md" "README/nested/guide.md"]]
          (testing (str "warning in " path " fails")
            (put! root path "warningtoken\n")
            (let [before (tracked-state root environment)
                  result (task! root environment)]
              (is (= 1 (:exit result)) (pr-str result))
              (is (.contains (:out result) path) (pr-str result))
              (is (.contains (:out result) "Probe.warning") (pr-str result))
              (is (= before (tracked-state root environment))))
            (put! root path "Clear prose.\n")))
        (testing "warnings in historical tasks and Ramora are excluded"
          (doseq [path excluded]
            (put! root path "warningtoken\n"))
          (let [before (tracked-state root environment)
                result (task! root environment)]
            (is (zero? (:exit result)) (pr-str result))
            (is (not (.contains (:out result) "Probe.warning")) (pr-str result))
            (is (= before (tracked-state root environment)))))))))

(defn- bounded-task! [root environment]
  (let [output (io/file root "command.out")
        builder (doto (ProcessBuilder.
                       ^java.util.List ["bb" "-cp" (.getCanonicalPath (io/file "dev/quality")) "vale"])
                  (.directory root)
                  (.redirectErrorStream true)
                  (.redirectOutput output))
        process-env (.environment builder)]
    (.clear process-env)
    (.putAll process-env environment)
    (let [process (.start builder)]
      (try
        (if (.waitFor process 30 java.util.concurrent.TimeUnit/SECONDS)
          {:exit (.exitValue process) :out (slurp output)}
          (throw (ex-info "Prose command exceeded 30 seconds" {})))
        (finally
          (.destroyForcibly process))))))

(deftest prose-symbolic-links-test
  ;; External links and cycles must be excluded without hiding ordinary prose.
  (with-project
    (fn [external]
      (put! external "external.md" "warningtoken\r\n")
      (with-project
        (fn [root]
          (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                         (System/getenv))
                                   "GIT_CONFIG_NOSYSTEM" "1"
                                   "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
                links {"README/nested/file.md" (io/file external "external.md")
                       "README/nested/directory" external
                       "README/nested/cycle" (io/file root "README")}
                external-before (vec (Files/readAllBytes (.toPath (io/file external "external.md"))))]
            (put! root "gitconfig" "")
            (try
              (doseq [[path target] links]
                (Files/createSymbolicLink (.toPath (io/file root path)) (.toPath target)
                                          (make-array FileAttribute 0)))
              (git! root environment "init")
              (git! root environment "config" "core.autocrlf" "false")
              (git! root environment "add" ".")
              (doseq [[content expected] [["Clear prose.\n" 0] ["warningtoken\n" 1]]]
                (put! root "README/nested/guide.md" content)
                (let [before (tracked-state root environment)
                      result (bounded-task! root environment)]
                  (is (= expected (:exit result)) (pr-str result))
                  (when (= 1 expected)
                    (is (.contains (:out result) "README/nested/guide.md") (pr-str result))
                    (is (.contains (:out result) "Probe.warning") (pr-str result)))
                  (is (= before (tracked-state root environment)))
                  (is (= external-before
                         (vec (Files/readAllBytes (.toPath (io/file external "external.md"))))))
                  (doseq [[path target] links]
                    (is (= (.toPath target)
                           (Files/readSymbolicLink (.toPath (io/file root path))))))))
              (finally
                ;; Remove links before the fixture's ordinary recursive cleanup.
                (doseq [path (keys links)]
                  (Files/deleteIfExists (.toPath (io/file root path))))))))))))
