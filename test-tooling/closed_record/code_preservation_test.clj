(ns closed-record.code-preservation-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.quality-scope :as scope])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- git! [root environment & args]
  (let [result (apply shell/sh "git" (concat args [:dir (str root) :env environment]))]
    (when-not (zero? (:exit result))
      (throw (ex-info "Fixture Git command failed" result)))
    (:out result)))

(defn- tracked-state [root environment]
  {:index (git! root environment "write-tree")
   :files (into {} (map (fn [path]
                          (let [file (io/file root path)]
                            [path (when (.exists file)
                                    (vec (Files/readAllBytes (.toPath file))))])))
                (.split (git! root environment "ls-files" "-z") "\u0000"))})

(defn- with-project [f]
  (let [root (.toFile (Files/createTempDirectory "code-preservation-" (make-array FileAttribute 0)))
        environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                 (System/getenv))
                           "GIT_CONFIG_NOSYSTEM" "1"
                           "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))]
    (try
      (doseq [path scope/project-paths]
        (let [file (io/file root path)]
          (if (.contains path ".")
            (do (io/make-parents file) (spit file "{}\n"))
            (do (.mkdirs file) (spit (io/file file "placeholder.edn") "{}\n")))))
      (doseq [path [".cljfmt.edn" ".clj-kondo/config.edn"]]
        (spit (io/file root path) (slurp path)))
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

(defn- check! [root environment operation]
  (shell/sh "bb" "-cp" (.getCanonicalPath (io/file "dev/quality"))
            "-e" (str "(require '[closed-record.format :as fmt] '[closed-record.lint :as lint])"
                      (if (= :format operation)
                        (str "(System/exit (fmt/format! " (pr-str (str root)) " [\"check\"]))")
                        (str "(let [result (lint/lint! " (pr-str (str root)) ")]"
                             "(print (:out result)) (flush) (System/exit (:exit result)))")))
            :env environment))

(deftest tracked-code-preservation-test
  ;; Real checks must preserve the whole tracked tree, not only the defective file.
  (with-project
    (fn [root environment]
      (doseq [[label source operation warnings errors]
              [["clean format" "(ns probe)\n(def x 1)\n" :format 0 0]
               ["format defect" "(ns probe)\n(def x 1 )\n" :format nil nil]
               ["clean lint" "(ns probe)\n(def x 1)\n" :lint 0 0]
               ["warning only" "(ns probe)\n(let [unused 1] 2)\n" :lint 1 0]
               ["lint error" "(ns probe)\n(missing)\n" :lint 0 1]]]
        (testing label
          (spit (io/file root "src/probe.clj") source)
          (let [before (tracked-state root environment)
                result (check! root environment operation)]
            (is (= (and (= 0 warnings) (= 0 errors)) (zero? (:exit result)))
                (pr-str result))
            (when (= :lint operation)
              (let [summary (:summary (edn/read-string (:out result)))]
                (is (= warnings (:warning summary)) (pr-str result))
                (is (= errors (:error summary)) (pr-str result))))
            (is (= before (tracked-state root environment)))))))))
