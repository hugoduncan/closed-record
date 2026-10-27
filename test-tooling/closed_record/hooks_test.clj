(ns closed-record.hooks-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.test-support.hooks :as hooks]
            [closed-record.test-support.code-project :as preservation]))

(deftest installed-hooks-test
  ;; Commits reject defects despite global rule suppression and unstaged corrections.
  (hooks/with-hooks
    (fn [root environment run git!]
      (let [probe (io/file root "src/probe.clj")
            clean "(ns probe)\n(def x 1)\n"
            untracked (doto (io/file root "src/untracked.clj")
                        (spit "(missing-symbol)\n"))
            untracked-bytes (vec (java.nio.file.Files/readAllBytes (.toPath untracked)))
            assert-untracked! (fn []
                                (is (.isFile untracked) "Untracked source must remain present")
                                (when (.isFile untracked)
                                  (is (= untracked-bytes
                                         (vec (java.nio.file.Files/readAllBytes (.toPath untracked))))
                                      "Untracked source bytes must not change"))
                                (is (= "" (git! "ls-files" "--" "src/untracked.clj"))
                                    "Untracked source must remain outside the index"))
            state #(dissoc (preservation/tracked-state root environment) :index)
            tree #(git! "write-tree")
            commit #(run "git" "commit" "--allow-empty" "-m" "Hook probe")]
        (spit (io/file root "unrelated.md") "Unstaged prose.\r\n")
        (testing "clean indexed files pass despite unrelated and untracked defects"
          (let [before (state) index (tree) result (commit)]
            (assert-untracked!)
            (is (zero? (:exit result)) (pr-str result))
            (is (= index (tree)))
            (is (= before (state)))))
        (doseq [[label staged diagnostic]
                [["warning" "(ns probe)\n(let [unused 1] 2)\n" ":unused-binding"]
                 ["error" "(ns probe)\n(missing)\n" ":unresolved-symbol"]]]
          (testing (str "staged " label " cannot be hidden by an unstaged correction")
            (spit probe staged)
            (git! "add" "src/probe.clj")
            (spit probe clean)
            (let [before (state)
                  index (tree)
                  head (git! "rev-parse" "HEAD")
                  result (commit)]
              (assert-untracked!)
              (is (not (zero? (:exit result))) (pr-str result))
              (is (.contains (:err result) "src/probe.clj") (pr-str result))
              (is (.contains (:err result) diagnostic) (pr-str result))
              (is (= head (git! "rev-parse" "HEAD")))
              (is (= index (tree)))
              (is (= before (state))))))
        (testing "repair stops the commit without staging; manual staging permits commit"
          (spit probe "(ns probe)\n(def x 1 )\n")
          (git! "add" "src/probe.clj")
          (let [index (tree)
                before (state)
                head (git! "rev-parse" "HEAD")
                result (commit)]
            (assert-untracked!)
            (is (not (zero? (:exit result))) (pr-str result))
            (is (.contains (:err result) "files were modified by this hook")
                (pr-str result))
            (is (not (.contains (:err result) "Lint indexed project"))
                (pr-str result))
            (is (= head (git! "rev-parse" "HEAD")))
            (is (= index (tree)))
            (is (= clean (slurp probe)))
            (is (= (dissoc (:files before) "src/probe.clj")
                   (dissoc (:files (state)) "src/probe.clj"))))
          (git! "add" "src/probe.clj")
          (let [index (tree) before (state) result (commit)]
            (assert-untracked!)
            (is (zero? (:exit result)) (pr-str result))
            (is (= index (tree)))
            (is (= before (state)))))))))

(deftest partial-staging-test
  ;; Real commits must preserve unstaged edits, including formatter conflicts.
  (doseq [[label staged working expected rollback?]
          [["separate hunks"
            "(ns probe)\n(def x 1 )\n;; a\n;; b\n;; c\n;; d\n;; e\n;; f\n;; g\n;; h\n(def y 2)\n"
            "(ns probe)\n(def x 1 )\n;; a\n;; b\n;; c\n;; d\n;; e\n;; f\n;; g\n;; h\n(def y 3)\n"
            "(ns probe)\n(def x 1)\n;; a\n;; b\n;; c\n;; d\n;; e\n;; f\n;; g\n;; h\n(def y 3)\n"]
           ["conflicting hunk"
            "(ns probe)\n(def x 1 )\n"
            "(ns probe)\n(def x 2 )\n"
            "(ns probe)\n(def x 2 )\n" true]]]
    (testing label
      (hooks/with-hooks
        (fn [root environment run git!]
          (let [probe (io/file root "src/probe.clj")]
            (spit probe staged)
            (git! "add" "src/probe.clj")
            (spit probe working)
            (spit (io/file root "unrelated.md") "Unstaged prose.\r\n")
            (let [before (preservation/tracked-state root environment)
                  head (git! "rev-parse" "HEAD")
                  result (run "git" "commit" "-m" "Partial staging probe")
                  after (preservation/tracked-state root environment)]
              (is (not (zero? (:exit result))) (pr-str result))
              (is (.contains (:err result) "files were modified by this hook")
                  (pr-str result))
              (when rollback?
                (is (.contains (:err result)
                               "Stashed changes conflicted with hook auto-fixes... Rolling back fixes...")
                    (pr-str result)))
              (is (= head (git! "rev-parse" "HEAD")))
              (is (= (:index before) (:index after)))
              (is (= expected (slurp probe)))
              (is (= (dissoc (:files before) "src/probe.clj")
                     (dissoc (:files after) "src/probe.clj"))))))))))

(deftest staged-path-changes-test
  ;; Index mutations must retain full lint coverage without checking excluded files.
  (hooks/with-hooks
    (fn [root environment run git!]
      (doseq [[label change expected-exit]
              [["addition"
                #(do (spit (io/file root "src/added.clj") "(ns added)\n(def x 1)\n")
                     (git! "add" "src/added.clj")) 0]
               ["rename" #(do (git! "mv" "src/added.clj" "src/renamed.clj")
                              (spit (io/file root "src/renamed.clj") "(ns renamed)\n(def x 1)\n")
                              (git! "add" "src/renamed.clj")) 0]
               ["deletion" #(git! "rm" "src/renamed.clj") 0]
               ["excluded source"
                #(do (.mkdirs (io/file root "generated"))
                     (spit (io/file root "generated/broken.clj") "(missing )\n")
                     (git! "add" "generated/broken.clj")) 0]
               ["missing indexed formatter configuration"
                #(git! "rm" "--cached" ".cljfmt.edn") 1]]]
        (testing label
          (change)
          (let [before (preservation/tracked-state root environment)
                head (git! "rev-parse" "HEAD")
                result (run "git" "commit" "-m" label)]
            (is (= expected-exit (:exit result)) (pr-str result))
            (is (= before (preservation/tracked-state root environment)))
            (when (pos? expected-exit)
              (is (= head (git! "rev-parse" "HEAD")))
              (is (.exists (io/file root ".cljfmt.edn"))))))))))

(deftest hook-setup-failures-test
  ;; Missing indexed scope and invalid tool configuration must fail without writes.
  (doseq [failure [:missing-directory :formatter-configuration]]
    (testing (name failure)
      (hooks/with-hooks
        (fn [root environment run git!]
          (case failure
            :missing-directory (git! "rm" "--cached" "test/placeholder.edn")
            :formatter-configuration
            (do
              (spit (io/file root ".cljfmt.edn") "{:indents {broken :invalid}}\n")
              (spit (io/file root "src/probe.clj") "(ns probe)\n(broken 1)\n")
              (git! "add" ".cljfmt.edn" "src/probe.clj")))
          (let [before (preservation/tracked-state root environment)
                head (git! "rev-parse" "HEAD")
                result (run "git" "commit" "-m" "Setup failure probe")]
            (is (not (zero? (:exit result))) (pr-str result))
            (is (= head (git! "rev-parse" "HEAD")))
            (is (= before (preservation/tracked-state root environment)))
            (case failure
              :missing-directory
              (do
                (is (.exists (io/file root "test/placeholder.edn")))
                (is (re-find #"Missing indexed quality paths: test" (:err result)) (pr-str result)))
              :formatter-configuration
              (do
                (is (re-find #"Failed to format file: src/probe.clj" (:err result)) (pr-str result))
                (is (not (.contains (:err result) "Lint indexed project")) (pr-str result))))))))))

(deftest hook-lint-setup-failure-test
  ;; Valid EDN reaches lint setup; a missing local dependency stops the commit.
  (hooks/with-hooks
    (fn [root environment run git!]
      (spit (io/file root "deps.edn")
            "{:deps {fixture/missing {:local/root \"missing-dependency\"}}}\n")
      (git! "add" "deps.edn")
      (let [before (preservation/tracked-state root environment)
            head (git! "rev-parse" "HEAD")
            result (run "git" "commit" "-m" "Lint setup failure probe")]
        (is (= 1 (:exit result)) (pr-str result))
        (is (re-find #"Lint indexed project" (:err result)) (pr-str result))
        (is (re-find #"Dependency lint setup failed" (:err result)) (pr-str result))
        (is (= head (git! "rev-parse" "HEAD")))
        (is (= before (preservation/tracked-state root environment)))))))

(deftest external-repository-preservation-test
  ;; Inherited Git overrides must not redirect fixture commits to another repository.
  (preservation/with-project
    (fn [root environment]
      (let [git! (fn [& args] (apply preservation/git! root environment args))]
        (git! "config" "user.name" "External Test")
        (git! "config" "user.email" "external@example.invalid")
        (git! "-c" "commit.gpgsign=false" "commit" "-m" "External baseline")
        (spit (io/file root "src/probe.clj") "(ns probe)\n(def x 2)\n")
        (git! "add" "src/probe.clj")
        (spit (io/file root "src/probe.clj") "(ns probe)\n(def x 3)\n")
        (let [before (preservation/tracked-state root environment)
              head (git! "rev-parse" "HEAD")
              index-bytes (vec (java.nio.file.Files/readAllBytes
                                (.toPath (io/file root ".git/index"))))
              result (shell/sh
                      (str (io/file (System/getProperty "java.home") "bin/java"))
                      "-cp" (System/getProperty "java.class.path")
                      "clojure.main" "-e"
                      (pr-str
                       '(do
                          (require 'closed-record.test-support.hooks)
                          (prn
                           (mapv
                            (fn [fail?]
                              (closed-record.test-support.hooks/with-hooks
                                (fn [child-root _ run child-git!]
                                  (when fail?
                                    (spit (clojure.java.io/file child-root "src/probe.clj")
                                          "(ns probe)\n(missing)\n")
                                    (child-git! "add" "src/probe.clj"))
                                  {:root (str child-root)
                                   :commit (run "git" "commit" "--allow-empty"
                                                "-m" "Isolation probe")})))
                            [false true]))
                          (System/exit 0)))
                      :env (assoc environment
                                  "GIT_DIR" (str (io/file root ".git"))
                                  "GIT_WORK_TREE" (str root)
                                  "GIT_INDEX_FILE" (str (io/file root ".git/index"))))]
          (is (zero? (:exit result)) (pr-str result))
          (let [outcomes (edn/read-string (:out result))]
            (is (= [0 1] (mapv #(get-in % [:commit :exit]) outcomes))
                (pr-str outcomes))
            (doseq [outcome outcomes]
              (is (not (.exists (io/file (:root outcome))))
                  (str "Child fixture must remove " (:root outcome)))))
          (is (= index-bytes (vec (java.nio.file.Files/readAllBytes
                                   (.toPath (io/file root ".git/index"))))))
          (is (= head (git! "rev-parse" "HEAD")))
          (is (= before (preservation/tracked-state root environment))))))))

(deftest wrong-indexed-types-test
  ;; Installed hooks reject wrong root/configuration types before any tool writes.
  (doseq [path ["src" ".cljfmt.edn" ".clj-kondo/config.edn" "build.clj"]]
    (testing path
      (hooks/with-hooks
        (fn [root environment run git!]
          (preservation/wrong-type! root path)
          (git! "add" "-A")
          (let [before (preservation/tracked-state root environment)
                head (git! "rev-parse" "HEAD")
                result (run "git" "commit" "-m" "Wrong type probe")]
            (is (pos? (:exit result)) (pr-str result))
            (is (.contains (:err result) "Invalid indexed quality path types:") (pr-str result))
            (is (.contains (:err result) path) (pr-str result))
            (is (= head (git! "rev-parse" "HEAD")))
            (is (= before (preservation/tracked-state root environment)))))))))

(deftest prose-only-commit-lints-whole-index-test
  ;; A prose-only commit must check unchanged indexed source, including warnings.
  (doseq [[label source diagnostic]
          [["clean" "(ns probe)\n(def x 1)\n" nil]
           ["warning" "(ns probe)\n(let [unused 1] 2)\n" ":unused-binding"]
           ["error" "(ns probe)\n(missing)\n" ":unresolved-symbol"]]]
    (testing label
      (hooks/with-hooks
        (fn [root environment run git!]
          (let [uninstalled (run "pre-commit" "uninstall")]
            (is (zero? (:exit uninstalled)) (pr-str uninstalled)))
          (spit (io/file root "src/probe.clj") source)
          (git! "add" "src/probe.clj")
          (git! "commit" "--allow-empty" "-m" "Source baseline before hook installation")
          (let [installed (run "pre-commit" "install")]
            (is (zero? (:exit installed)) (pr-str installed)))
          (spit (io/file root "unrelated.md") "Prose-only change.\n")
          (git! "add" "unrelated.md")
          (is (= "unrelated.md\n" (git! "diff" "--cached" "--name-only")))
          (let [before (preservation/tracked-state root environment)
                head (git! "rev-parse" "HEAD")
                result (run "git" "commit" "-m" "Prose-only probe")]
            (is (= (nil? diagnostic) (zero? (:exit result))) (pr-str result))
            (is (= before (preservation/tracked-state root environment)))
            (when diagnostic
              (is (.contains (:err result) diagnostic) (pr-str result))
              (is (.contains (:err result) "src/probe.clj") (pr-str result))
              (is (= head (git! "rev-parse" "HEAD"))))))))))

(deftest formatting-repairs-only-selected-staged-files-test
  ;; Unchanged indexed format defects must survive prose commits and selected repairs.
  (hooks/with-hooks
    (fn [root environment run git!]
      (let [uninstalled (run "pre-commit" "uninstall")]
        (is (zero? (:exit uninstalled)) (pr-str uninstalled)))
      (spit (io/file root "src/probe.clj") "(ns probe)\n(def x 1 )\n")
      (git! "add" "src/probe.clj")
      (git! "commit" "-m" "Format defect before hook installation")
      (let [installed (run "pre-commit" "install")]
        (is (zero? (:exit installed)) (pr-str installed)))
      (testing "a prose-only commit leaves unchanged source defects intact"
        (spit (io/file root "unrelated.md") "Prose-only change.\n")
        (git! "add" "unrelated.md")
        (is (= "unrelated.md\n" (git! "diff" "--cached" "--name-only")))
        (let [before (preservation/tracked-state root environment)
              head (git! "rev-parse" "HEAD")
              result (run "git" "commit" "-m" "Prose-only format probe")]
          (is (zero? (:exit result)) (pr-str result))
          (is (not= head (git! "rev-parse" "HEAD")))
          (is (= before (preservation/tracked-state root environment)))))
      (testing "a selected repair stops the commit and preserves every other file"
        (spit (io/file root "src/selected.clj") "(ns selected)\n(def x 2 )\n")
        (git! "add" "src/selected.clj")
        (spit (io/file root "unrelated.md") "Unstaged prose.\r\n")
        (is (= "src/selected.clj\n" (git! "diff" "--cached" "--name-only")))
        (let [before (preservation/tracked-state root environment)
              head (git! "rev-parse" "HEAD")
              result (run "git" "commit" "-m" "Selected format repair probe")
              after (preservation/tracked-state root environment)]
          (is (pos? (:exit result)) (pr-str result))
          (is (= head (git! "rev-parse" "HEAD")))
          (is (= (:index before) (:index after)))
          (is (= "(ns selected)\n(def x 2)\n"
                 (slurp (io/file root "src/selected.clj"))))
          (is (= (dissoc (:files before) "src/selected.clj")
                 (dissoc (:files after) "src/selected.clj"))))))))