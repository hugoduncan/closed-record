(ns closed-record.quality-scope-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [closed-record.index-scope :as index]
            [closed-record.quality-scope :as scope])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- with-directory [f]
  (let [root (.toFile (Files/createTempDirectory "quality-scope-" (make-array FileAttribute 0)))]
    (try
      (f root)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

(deftest required-scope-test
  ;; Missing inputs must not silently reduce coverage.
  (with-directory
    (fn [root]
      (doseq [path scope/project-paths]
        (let [file (io/file root path)]
          (if (.contains path ".")
            (do (io/make-parents file) (spit file "{}"))
            (.mkdirs file))))
      (is (= scope/project-paths (scope/project-paths! root)))
      (doseq [path ["src" "deps.edn"]]
        (io/delete-file (io/file root path)))
      (testing "both file and directory failures identify the missing inputs"
        (is (= ["src" "deps.edn"]
               (try
                 (scope/project-paths! root)
                 (catch clojure.lang.ExceptionInfo error
                   (:missing-paths (ex-data error)))))))
      (testing "explicit paths do not depend on project completeness"
        (is (= ["test"] (scope/validate-paths! root ["test"])))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"src"
                              (scope/validate-paths! root ["src"])))))))

(deftest owned-source-test
  ;; Git path selection must not include downloaded or generated inputs.
  (testing "maintained code and configuration are selected"
    (doseq [path ["dev/probe.bb" "src/core.cljc" "dev/quality/a.clj"
                  "test/runtime/a.clj" "test/bench/a.clj" "test/build/a.clj"
                  "test/release/a.clj" "test/tooling/a.clj"
                  "build.clj" "bb.edn" "deps.edn" ".clj-kondo/config.edn"]]
      (is (scope/owned-source? path) path)))
  (testing "imports, caches, generated files, and adjacent paths are excluded"
    (doseq [path [".clj-kondo/imports/malli/config.edn" ".clj-kondo/.cache/a.edn"
                  "dev/.cache/a.edn" "target/a.clj" "src/../target/a.clj"
                  "src-other/a.clj" "README.md"]]
      (is (not (scope/owned-source? path)) path))))

(deftest maintained-scope-contract-test
  ;; Contract expectations stay independent of production scope and path order.
  (let [roots ["src" "test" "dev"]
        files ["build.clj" "bb.edn" "deps.edn" ".cljfmt.edn" ".clj-kondo/config.edn"]
        required (merge (zipmap roots (repeat :directory))
                        (zipmap files (repeat :file)))
        representative (fn [[path type]]
                         (if (= :directory type) (str path "/sample.clj") path))
        expected (into #{} (map representative) required)]
    (with-directory
      (fn [root]
        (let [env (into {"GIT_CONFIG_NOSYSTEM" "1"
                         "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig"))}
                        (remove (fn [[key _]] (str/starts-with? key "GIT_")))
                        (System/getenv))
              git! (fn [& args]
                     (let [result (apply shell/sh "git"
                                         (concat args [:dir (str root) :env env]))]
                       (when-not (zero? (:exit result))
                         (throw (ex-info "Fixture Git command failed" result)))))
              put! (fn [path]
                     (let [file (io/file root path)]
                       (io/make-parents file)
                       (spit file "{}")))
              missing (fn [select!]
                        (try
                          (select! root)
                          (catch clojure.lang.ExceptionInfo error
                            (set (:missing-paths (ex-data error))))))]
          ;; Populate added scope too, without deriving expectations from it.
          (doseq [entry (merge scope/required-types required)]
            (put! (representative entry)))
          (git! "init")
          (git! "add" ".")
          (shell/with-sh-env env
            (testing "project and index select every contract input"
              (doseq [select! [scope/project-source-paths! index/paths!]]
                (is (set/subset? expected (set (select! root))))))
            (doseq [[path type :as entry] required]
              (testing (str "missing required input " path)
                (let [sample (representative entry)]
                  (io/delete-file (io/file root sample))
                  (when (= :directory type)
                    (io/delete-file (io/file root path)))
                  (is (contains? (missing scope/project-source-paths!) path))
                  (put! sample)
                  (git! "rm" "--cached" "-r" "--" path)
                  (is (contains? (missing index/paths!) path))
                  (git! "add" "--" path))))))))))
