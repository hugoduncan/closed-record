(ns closed-record.test-support.code-project
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [closed-record.quality-scope :as scope])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn check!
  "Run the shared format or lint helper in a real process."
  [root environment operation]
  (shell/sh "bb" "-cp" (.getCanonicalPath (io/file "dev/quality"))
            "-e" (str "(require '[closed-record.format :as fmt] '[closed-record.lint :as lint])"
                      (if (#{:format :fix} operation)
                        (str "(System/exit (fmt/format! " (pr-str (str root)) " " (pr-str [(if (= :fix operation) "fix" "check")]) "))")
                        (str "(let [result (lint/lint! " (pr-str (str root)) ")]"
                             "(print (:out result)) (flush) (System/exit (:exit result)))")))
            :env environment))

(defn git!
  "Run Git with the fixture environment and return stdout."
  [root environment & args]
  (let [result (apply shell/sh "git" (concat args [:dir (str root) :env environment]))]
    (when-not (zero? (:exit result))
      (throw (ex-info "Fixture Git command failed" result)))
    (:out result)))

(defn task!
  "Run a public Babashka task with the fixture environment."
  [root environment & args]
  (apply shell/sh "bb" "-cp" (.getCanonicalPath (io/file "dev/quality"))
         (concat args [:dir (str root) :env environment])))

(defn wrong-type!
  "Replace a fixture path with the opposite file/directory type."
  [root path]
  (let [file (io/file root path)]
    (if (.isDirectory file)
      (do
        (doseq [entry (reverse (file-seq file))] (io/delete-file entry))
        (spit file "(missing 1 )\n"))
      (do
        (io/delete-file file)
        (.mkdirs file)
        (spit (io/file file "placeholder.edn") "{}\n")))))

(defn tracked-state
  "Read the index tree and exact tracked file bytes."
  [root environment]
  {:index (git! root environment "write-tree")
   :files (into {} (map (fn [path]
                          (let [file (io/file root path)]
                            [path (when (.exists file)
                                    (vec (Files/readAllBytes (.toPath file))))])))
                (.split (git! root environment "ls-files" "-z") "\u0000"))})

(defn with-project
  "Call f with a temporary project; remove it on return or failure."
  [f]
  (let [root (.toFile (Files/createTempDirectory "code-preservation-" (make-array FileAttribute 0)))
        environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                 (System/getenv))
                           "XDG_CONFIG_HOME" (str (io/file root "user-config"))
                           "GIT_CONFIG_NOSYSTEM" "1"
                           "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))]
    (try
      (doseq [path scope/project-paths]
        (let [file (io/file root path)]
          (if (.contains path ".")
            (do (io/make-parents file) (spit file "{}\n"))
            (do (.mkdirs file) (spit (io/file file "placeholder.edn") "{}\n")))))
      (doseq [path ["bb.edn" ".cljfmt.edn" ".clj-kondo/config.edn"]]
        (spit (io/file root path) (slurp path)))
      (let [global-config (io/file root "user-config/clj-kondo/config.edn")]
        (io/make-parents global-config)
        (spit global-config
              "{:linters {:unused-binding {:level :off} :unresolved-symbol {:level :off}}}\n"))
      (spit (io/file root "gitconfig") "")
      (spit (io/file root "unrelated.md") "Original prose.\n")
      (spit (io/file root "src/probe.clj") "(ns probe)\n(def x 1)\n")
      (with-open [output (io/output-stream (io/file root "unrelated.bin"))]
        (.write output (byte-array [0 1 -1 13 10])))
      (git! root environment "init")
      (git! root environment "config" "core.autocrlf" "false")
      (git! root environment "add" ".")
      (spit (io/file root "unrelated.md") "Unstaged prose.\r\n")
      (f root environment)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

