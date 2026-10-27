(ns closed-record.lint-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.lint :as lint]
            [closed-record.quality-scope :as scope])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- with-project [f]
  (let [root (.toFile (Files/createTempDirectory "lint-" (make-array FileAttribute 0)))]
    (try
      (doseq [path scope/project-paths]
        (let [file (io/file root path)]
          (if (.contains path ".")
            (do (io/make-parents file) (spit file "{}\n"))
            (.mkdirs file))))
      (spit (io/file root ".clj-kondo/config.edn") (slurp ".clj-kondo/config.edn"))
      (f root)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

(def valid-source
  "(ns probe (:require [closed-record.core :refer [defclosedrecord]]))
(defclosedrecord Point [x]
  Object
  (toString [this] (str x (:x this))))
(->Point 1)
(map->Point {:x 1})\n")

(deftest lint-findings-test
  ;; Real lint proves the owned mapping and warning threshold without mocks.
  (with-project
    (fn [root]
      (let [file (io/file root "src/probe.clj")]
        (doseq [[label source warnings errors]
                [["valid bindings and constructors" valid-source 0 0]
                 ["warning without errors" (str valid-source "(let [unused 1] 2)\n") 1 0]
                 ["unresolved method body"
                  "(ns probe (:require [closed-record.core :refer [defclosedrecord]]))
(defclosedrecord Point [x] Object (toString [_] (missing x)))\n" 0 1]]]
          (testing label
            (spit file source)
            (let [{:keys [exit out]} (lint/lint! root)
                  summary (:summary (edn/read-string out))]
              (is (= warnings (:warning summary)) out)
              (is (= errors (:error summary)) out)
              (is (= (zero? (+ warnings errors)) (zero? exit)) out)
              (is (= source (slurp file))))))))))

(deftest lint-setup-failure-test
  ;; Missing scope and invalid dependency input must not reach a passing lint.
  (with-project
    (fn [root]
      (let [deps (io/file root "deps.edn")]
        (spit deps "not valid dependency edn")
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Dependency lint setup failed"
                              (lint/lint! root)))
        (is (= "not valid dependency edn" (slurp deps)))
        (io/delete-file (io/file root "build.clj"))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing required quality paths: build.clj"
                              (lint/lint! root)))
        (is (= "not valid dependency edn" (slurp deps)))))))
