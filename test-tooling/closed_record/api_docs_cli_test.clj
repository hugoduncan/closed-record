(ns closed-record.api-docs-cli-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- with-project [f]
  (let [root (.toFile (Files/createTempDirectory "api-docs-" (make-array FileAttribute 0)))]
    (try
      (doseq [path ["bb.edn" "deps.edn" ".clj-kondo/config.edn" "src/closed_record/core.cljc"
                    "dev/quality/closed_record/api_docs.clj"
                    "dev/quality/closed_record/api_docs_cli.clj"]]
        (let [target (io/file root path)]
          (io/make-parents target)
          (io/copy (io/file path) target)))
      (f root)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

(defn- command [root & args]
  (apply shell/sh "bb" "api-docs" (concat args [:dir root])))

(deftest reference-file-lifecycle-test
  ;; Real task processes check bytes without writes and repair only on request.
  (with-project
    (fn [root]
      (let [output (io/file root "doc/API.md")
            missing (command root "--check")]
        (is (pos? (:exit missing)))
        (is (str/includes? (:err missing) "Run bb api-docs"))
        (is (not (.exists output)))
        (is (zero? (:exit (command root))))
        (let [generated (Files/readAllBytes (.toPath output))]
          (is (zero? (:exit (command root "--check"))))
          (is (= (vec generated) (vec (Files/readAllBytes (.toPath output)))))
          (is (zero? (:exit (command root))))
          (is (= (vec generated) (vec (Files/readAllBytes (.toPath output)))))
          (spit output "stale reference λ\n" :encoding "UTF-8")
          (let [stale (Files/readAllBytes (.toPath output))
                result (command root "--check")]
            (is (pos? (:exit result)))
            (is (str/includes? (:err result) "Run bb api-docs"))
            (is (= (vec stale) (vec (Files/readAllBytes (.toPath output)))))
            (doseq [args [["--unknown"] ["--check" "extra"]]]
              (let [invalid (apply command root args)]
                (is (pos? (:exit invalid)))
                (is (str/includes? (:err invalid) "Usage: bb api-docs"))
                (is (= (vec stale) (vec (Files/readAllBytes (.toPath output)))))))))
        (is (zero? (:exit (command root))))
        (is (zero? (:exit (command root "--check"))))))))

(deftest jvm-reader-branch-test
  ;; Run the owned generator on divergent reader branches through real Quickdoc.
  (with-project
    (fn [root]
      (spit (io/file root "src/closed_record/core.cljc")
            (str "(ns closed-record.core)\n"
                 "#?(:clj (defn check \"JVM branch contract.\" [x] x)\n"
                 "   :cljs (defn check \"JavaScript branch contract.\" [x y] y))\n"
                 "#?(:cljs (defn browser-only \"Browser API.\" [] nil))\n"))
      (let [result (command root)]
        (is (zero? (:exit result)) (pr-str result))
        (when (zero? (:exit result))
          (let [markdown (slurp (io/file root "doc/API.md"))]
            (is (str/includes? markdown "JVM branch contract."))
            (is (str/includes? markdown "(check x)"))
            (is (not (str/includes? markdown "JavaScript branch contract.")))
            (is (not (str/includes? markdown "browser-only")))))))))

(deftest fresh-reference-still-needs-content-tests
  ;; A real generator defect must fail unchanged content tests despite freshness.
  (with-project
    (fn [root]
      (let [generator (io/file root "dev/quality/closed_record/api_docs.clj")
            content-test (io/file root "test-tooling/closed_record/api_docs_test.clj")]
        (io/make-parents content-test)
        (io/copy (io/file "test-tooling/closed_record/api_docs_test.clj") content-test)
        (spit generator
              (str/replace (slurp generator)
                           "ClojureScript and Babashka are not supported."
                           "Support details omitted."))
        (is (zero? (:exit (command root))))
        (is (zero? (:exit (command root "--check"))))
        (let [result (shell/sh "clojure" "-Srepro" "-M:quickdoc:tooling-test"
                               "-m" "scry.cli" "--dir" "test-tooling" :dir root)
              failures (into [] (filter #(.isFile %))
                             (file-seq (io/file root ".scry-results")))]
          (is (= 1 (:exit result)) (pr-str result))
          (is (= 1 (count failures)))
          (when (= 1 (count failures))
            (let [failure (edn/read-string (slurp (first failures)))
                  values (into #{} (tree-seq coll? seq failure))]
              (is (contains? values "ClojureScript and Babashka are not supported."))
              (is (contains? values :fail)))))))))
