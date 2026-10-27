(ns closed-record.lint-dependencies-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [closed-record.lint-dependencies :as dependencies])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest classpath-union-test
  ;; A replacing basis must not remove entries from another basis.
  (let [separator java.io.File/pathSeparator
        join #(str/join separator %)]
    (testing "all distinct entries survive in first-seen order"
      (is (= (join ["src" "malli.jar" "build.jar"])
             (dependencies/merge-classpaths
              [(str (join ["src" "malli.jar"]) "\n")
               (join ["build.jar" "src"]) ""]))))))

(deftest missing-scope-test
  ;; Setup fails before any dependency process can create cache files.
  (let [root (.toFile (Files/createTempDirectory "lint-dependencies-"
                                                 (make-array FileAttribute 0)))]
    (try
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing required quality paths: src"
                            (dependencies/copy-config! root)))
      (is (empty? (.listFiles root)))
      (finally (io/delete-file root)))))
