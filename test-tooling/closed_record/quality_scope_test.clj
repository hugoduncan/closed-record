(ns closed-record.quality-scope-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
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
    (doseq [path ["src/core.cljc" "test/a.clj" "dev/quality/a.clj"
                  "test-tooling/a.clj" "build.clj" "bb.edn" "deps.edn"
                  ".clj-kondo/config.edn"]]
      (is (scope/owned-source? path) path)))
  (testing "imports, caches, generated files, and adjacent paths are excluded"
    (doseq [path [".clj-kondo/imports/malli/config.edn" ".clj-kondo/.cache/a.edn"
                  "dev/.cache/a.edn" "target/a.clj" "src/../target/a.clj"
                  "src-other/a.clj" "README.md"]]
      (is (not (scope/owned-source? path)) path))))
