(ns closed-record.vale-policy-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- with-directory [f]
  (let [root (.toFile (Files/createTempDirectory "vale-policy-" (make-array FileAttribute 0)))]
    (try
      (f root)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

(defn- check-prose [file]
  (shell/sh "vale" (str "--config=" (.getCanonicalPath (io/file ".vale.ini")))
            "--minAlertLevel=warning" "--output=JSON" (.getPath file)))

(deftest owned-vale-exclusions-test
  ;; Real Markdown parsing must exclude instructions and code, not nearby prose.
  (with-directory
    (fn [root]
      (doseq [path ["AGENTS.md" "README/nested/guide.md"]]
        (let [file (io/file root path)
              protected-text (str "λ instructions. This is very unique.\n\n"
                                  "```clojure\nThis is very unique.\n```\n\n"
                                  "Use `very unique` as code.\n")]
          (io/make-parents file)
          (testing (str "code and lambda text in " path)
            (spit file protected-text)
            (let [result (check-prose file)]
              (is (zero? (:exit result)) (pr-str result)))
            (is (= protected-text (slurp file))))
          (testing (str "ordinary prose remains checked in " path)
            (let [content (str protected-text "\nThis is very unique.\n")]
              (spit file content)
              (let [result (check-prose file)]
                (is (pos? (:exit result)) (pr-str result))
                (is (.contains (:out result) "proselint") (pr-str result)))
              (is (= content (slurp file))))))))))

(deftest owned-vale-vocabulary-test
  ;; Project vocabulary must pass without disabling spelling for ordinary words.
  (with-directory
    (fn [root]
      (let [file (io/file root "API.md")]
        (spit file (str "Use ClosedRecord with Clojure on the JVM.\n"
                        "Use malli with destructuring.\n"
                        "Use Temurin and Clojars.\n"
                        "Check loopback connections and untracked worktree files.\n"))
        (let [result (check-prose file)]
          (is (zero? (:exit result)) (pr-str result)))
        (spit file "Use ClosedRecord with zzzmisspelledzzz.\n")
        (let [result (check-prose file)]
          (is (pos? (:exit result)) (pr-str result))
          (is (.contains (:out result) "Vale.Spelling") (pr-str result)))))))
