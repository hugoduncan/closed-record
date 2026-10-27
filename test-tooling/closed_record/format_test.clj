(ns closed-record.format-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.format :as format])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- with-project [f]
  (let [root (.toFile (Files/createTempDirectory "format-" (make-array FileAttribute 0)))]
    (try
      (spit (io/file root ".cljfmt.edn") (slurp ".cljfmt.edn"))
      (f root)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

(deftest explicit-format-test
  ;; Real cljfmt checks preserve bytes; only an explicit fix writes.
  (with-project
    (fn [root]
      (doseq [path ["example.clj" "example.edn"]]
        (let [file (io/file root path)
              original "{:a    [1   2] }\n"]
          (spit file original)
          (testing (str "check and repair " path)
            (is (pos? (format/format! root ["check" "."])))
            (is (= original (slurp file)))
            (is (zero? (format/format! root ["fix" path])))
            (is (not= original (slurp file)))
            (let [repaired (slurp file)]
              (is (zero? (format/format! root ["check" path])))
              (is (= repaired (slurp file))))))))))

(deftest invalid-input-test
  ;; Invalid commands and incomplete scope cannot repair any file.
  (with-project
    (fn [root]
      (let [file (io/file root "example.clj")
            original "(def x   1)\n"]
        (spit file original)
        (doseq [args [[] ["other"] ["check" "--config"] ["fix" ""]]]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Usage: bb clj-fmt"
                                (format/format! root args))))
        (doseq [operation ["check" "fix"]]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing required quality paths: src"
                                (format/format! root [operation])))
          (is (= original (slurp file))))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"absent.clj"
                              (format/format! root ["check" "absent.clj"])))
        (io/delete-file (io/file root ".cljfmt.edn"))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #".cljfmt.edn"
                              (format/format! root ["fix" "example.clj"])))
        (is (= original (slurp file)))))))

(deftest absolute-format-test
  ;; Explicit absolute files and directories can be outside the project root.
  (with-project
    (fn [root]
      (with-project
        (fn [external]
          (let [file (io/file external "nested/probe.clj")
                original "(def x 1 )\n"]
            (io/make-parents file)
            (doseq [path [(.getAbsolutePath file)
                          (.getAbsolutePath (.getParentFile file))]]
              (testing (str "absolute input " path)
                (spit file original)
                (is (pos? (format/format! root ["check" path])))
                (is (= original (slurp file)))
                (is (zero? (format/format! root ["fix" path])))
                (is (= "(def x 1)\n" (slurp file)))
                (is (zero? (format/format! root ["check" path])))
                (is (= "(def x 1)\n" (slurp file)))))
            (let [missing (.getAbsolutePath (io/file external "absent.clj"))]
              (doseq [operation ["check" "fix"]]
                (is (= [missing]
                       (try
                         (format/format! root [operation missing])
                         (catch clojure.lang.ExceptionInfo error
                           (:missing-paths (ex-data error)))))))))
          (testing "absolute inputs still require owned configuration"
            (io/delete-file (io/file root ".cljfmt.edn"))
            (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing required quality paths: .cljfmt.edn"
                                  (format/format! root ["fix" (.getAbsolutePath external)])))))))))
