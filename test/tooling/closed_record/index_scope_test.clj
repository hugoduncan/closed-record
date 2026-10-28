(ns closed-record.index-scope-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [closed-record.index-scope :as index]
            [closed-record.lint :as lint]
            [closed-record.quality-scope :as scope])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest indexed-scope-test
  ;; Real index selection excludes untracked files and detects staged removal.
  (let [root (.toFile (Files/createTempDirectory "index-scope-" (make-array FileAttribute 0)))
        env (into {"GIT_CONFIG_NOSYSTEM" "1"
                   "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig"))}
                  (remove (fn [[key _]] (str/starts-with? key "GIT_")))
                  (System/getenv))
        run (fn [& args]
              (apply shell/sh (concat args [:dir (str root) :env env])))
        put! (fn [path]
               (let [file (io/file root path)]
                 (io/make-parents file)
                 (spit file "{}")))
        paths (mapv #(if (str/includes? % ".") % (str % "/sample.clj"))
                    scope/project-paths)]
    (try
      (is (zero? (:exit (run "git" "init"))))
      (doseq [path (conj paths "src/space and\nnewline.clj" "src/imports/ignored.edn")]
        (put! path))
      (is (zero? (:exit (run "git" "add" "."))))
      (put! "src/untracked.clj")
      (spit (io/file root "src/untracked.clj") "(missing-symbol)")
      (shell/with-sh-env env
        (testing "index paths retain unusual names and omit untracked/generated files"
          (let [before (:out (run "git" "write-tree"))]
            (is (= (set (conj paths "src/space and\nnewline.clj"))
                   (set (index/paths! root))))
            (is (= before (:out (run "git" "write-tree"))))))
        (testing "indexed lint excludes untracked defects but rejects indexed warnings"
          (let [before (:out (run "git" "write-tree"))
                clean (lint/lint-index! root)]
            (is (zero? (:exit clean)) (:out clean))
            (is (= before (:out (run "git" "write-tree")))))
          (spit (io/file root "src/sample.clj") "(ns sample) (let [unused 1] 2)")
          (is (zero? (:exit (run "git" "add" "src/sample.clj"))))
          (let [before (:out (run "git" "write-tree"))
                result (lint/lint-index! root)
                summary (:summary (edn/read-string (:out result)))]
            (is (not (zero? (:exit result))))
            (is (= 1 (:warning summary)))
            (is (zero? (:error summary)))
            (is (= before (:out (run "git" "write-tree"))))))
        (testing "a staged rename changes the selected path"
          (is (zero? (:exit (run "git" "mv" "src/sample.clj" "src/renamed.clj"))))
          (is (some #{"src/renamed.clj"} (index/paths! root)))
          (is (not-any? #{"src/sample.clj"} (index/paths! root))))
        (testing "an untracked configuration cannot replace indexed configuration"
          (is (zero? (:exit (run "git" "rm" "--cached" ".cljfmt.edn"))))
          (is (.exists (io/file root ".cljfmt.edn")))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing indexed.*cljfmt"
                                (lint/lint-index! root)))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing indexed.*cljfmt"
                                (index/paths! root)))))
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

(deftest required-index-types-test
  ;; Every requirement uses its declared type, including Git regular-file modes.
  (let [paths (mapv (fn [[path type]]
                      (if (= :directory type) (str path "/sample.clj") path))
                    scope/required-types)]
    (doseq [[path type] scope/required-types]
      (let [valid-path (if (= :directory type) (str path "/sample.clj") path)
            wrong-path (if (= :directory type) path (str path "/sample.clj"))]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid indexed quality path types"
                              (index/select-paths! (conj (filterv #(not= valid-path %) paths) wrong-path))))))
    (doseq [mode ["120000" "160000"]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid indexed quality path types"
                            (index/select-paths! paths {"build.clj" mode}))))))
