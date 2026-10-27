(ns closed-record.hooks-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.code-preservation-test :as preservation]))

(defn- with-hooks [f]
  (#'preservation/with-project
   (fn [root environment]
     (doseq [source (filterv #(.isFile %) (vec (file-seq (io/file "dev/quality"))))]
       (let [target (io/file root (str source))]
         (io/make-parents target)
         (io/copy source target)))
     (doseq [path ["bb.edn" ".pre-commit-config.yaml"]]
       (io/copy (io/file path) (io/file root path)))
     (let [env (assoc environment "PRE_COMMIT_HOME" (str (io/file root ".pre-commit-cache")))
           run (fn [& args]
                 (apply shell/sh (concat args [:dir (str root) :env env])))
           git! (fn [& args] (apply #'preservation/git! root env args))]
       (git! "config" "user.name" "Hook Test")
       (git! "config" "user.email" "hook@example.invalid")
       (git! "config" "commit.gpgsign" "false")
       (git! "add" "bb.edn" "dev" ".pre-commit-config.yaml")
       (git! "commit" "-m" "Fixture baseline")
       (let [installed (run "pre-commit" "install")]
         (is (zero? (:exit installed)) (pr-str installed)))
       (f root env run git!)))))

(deftest installed-hooks-test
  ;; Actual Git commits exercise pre-commit isolation, repair detection and restore.
  (with-hooks
    (fn [root environment run git!]
      (let [probe (io/file root "src/probe.clj")
            clean "(ns probe)\n(def x 1)\n"
            state #(dissoc (#'preservation/tracked-state root environment) :index)
            tree #(git! "write-tree")
            commit #(run "git" "commit" "--allow-empty" "-m" "Hook probe")]
        (spit (io/file root "src/untracked.clj") "(missing-symbol)\n")
        (spit (io/file root "unrelated.md") "Unstaged prose.\r\n")
        (testing "clean indexed files pass despite unrelated and untracked defects"
          (let [before (state) index (tree) result (commit)]
            (is (zero? (:exit result)) (pr-str result))
            (is (= index (tree)))
            (is (= before (state)))))
        (doseq [[label staged] [["warning" "(ns probe)\n(let [unused 1] 2)\n"]
                                ["error" "(ns probe)\n(missing)\n"]]]
          (testing (str "staged " label " cannot be hidden by an unstaged correction")
            (spit probe staged)
            (git! "add" "src/probe.clj")
            (spit probe clean)
            (let [before (state) index (tree) result (commit)]
              (is (not (zero? (:exit result))) (pr-str result))
              (is (= index (tree)))
              (is (= before (state))))))
        (testing "repair stops the commit without staging; manual staging permits commit"
          (spit probe "(ns probe)\n(def x 1 )\n")
          (git! "add" "src/probe.clj")
          (let [index (tree) before (state) result (commit)]
            (is (not (zero? (:exit result))) (pr-str result))
            (is (= index (tree)))
            (is (= clean (slurp probe)))
            (is (= (dissoc (:files before) "src/probe.clj")
                   (dissoc (:files (state)) "src/probe.clj"))))
          (git! "add" "src/probe.clj")
          (let [index (tree) before (state) result (commit)]
            (is (zero? (:exit result)) (pr-str result))
            (is (= index (tree)))
            (is (= before (state)))))))))

(deftest partial-staging-test
  ;; Real commits must preserve unstaged edits, including formatter conflicts.
  (doseq [[label staged working expected]
          [["separate hunks"
            "(ns probe)\n(def x 1 )\n;; a\n;; b\n;; c\n;; d\n;; e\n;; f\n;; g\n;; h\n(def y 2)\n"
            "(ns probe)\n(def x 1 )\n;; a\n;; b\n;; c\n;; d\n;; e\n;; f\n;; g\n;; h\n(def y 3)\n"
            "(ns probe)\n(def x 1)\n;; a\n;; b\n;; c\n;; d\n;; e\n;; f\n;; g\n;; h\n(def y 3)\n"]
           ["conflicting hunk"
            "(ns probe)\n(def x 1 )\n"
            "(ns probe)\n(def x 2 )\n"
            "(ns probe)\n(def x 2 )\n"]]]
    (testing label
      (with-hooks
        (fn [root environment run git!]
          (let [probe (io/file root "src/probe.clj")]
            (spit probe staged)
            (git! "add" "src/probe.clj")
            (spit probe working)
            (spit (io/file root "unrelated.md") "Unstaged prose.\r\n")
            (let [before (#'preservation/tracked-state root environment)
                  head (git! "rev-parse" "HEAD")
                  result (run "git" "commit" "-m" "Partial staging probe")
                  after (#'preservation/tracked-state root environment)]
              (is (not (zero? (:exit result))) (pr-str result))
              (is (= head (git! "rev-parse" "HEAD")))
              (is (= (:index before) (:index after)))
              (is (= expected (slurp probe)))
              (is (= (dissoc (:files before) "src/probe.clj")
                     (dissoc (:files after) "src/probe.clj"))))))))))

(deftest staged-path-changes-test
  ;; Index mutations must retain full lint coverage without checking excluded files.
  (with-hooks
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
          (let [before (#'preservation/tracked-state root environment)
                head (git! "rev-parse" "HEAD")
                result (run "git" "commit" "-m" label)]
            (is (= expected-exit (:exit result)) (pr-str result))
            (is (= before (#'preservation/tracked-state root environment)))
            (when (pos? expected-exit)
              (is (= head (git! "rev-parse" "HEAD")))
              (is (.exists (io/file root ".cljfmt.edn"))))))))))

(deftest hook-setup-failures-test
  ;; Missing indexed scope and invalid tool configuration must fail without writes.
  (doseq [failure [:missing-directory :formatter-configuration]]
    (testing (name failure)
      (with-hooks
        (fn [root environment run git!]
          (case failure
            :missing-directory (git! "rm" "--cached" "test/placeholder.edn")
            :formatter-configuration
            (do
              (spit (io/file root ".cljfmt.edn") "{:indents {broken :invalid}}\n")
              (spit (io/file root "src/probe.clj") "(ns probe)\n(broken 1)\n")
              (git! "add" ".cljfmt.edn" "src/probe.clj")))
          (let [before (#'preservation/tracked-state root environment)
                head (git! "rev-parse" "HEAD")
                result (run "git" "commit" "-m" "Setup failure probe")]
            (is (not (zero? (:exit result))) (pr-str result))
            (is (= head (git! "rev-parse" "HEAD")))
            (is (= before (#'preservation/tracked-state root environment)))
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
  (with-hooks
    (fn [root environment run git!]
      (spit (io/file root "deps.edn")
            "{:deps {fixture/missing {:local/root \"missing-dependency\"}}}\n")
      (git! "add" "deps.edn")
      (let [before (#'preservation/tracked-state root environment)
            head (git! "rev-parse" "HEAD")
            result (run "git" "commit" "-m" "Lint setup failure probe")]
        (is (= 1 (:exit result)) (pr-str result))
        (is (re-find #"Lint indexed project" (:err result)) (pr-str result))
        (is (re-find #"Dependency lint setup failed" (:err result)) (pr-str result))
        (is (= head (git! "rev-parse" "HEAD")))
        (is (= before (#'preservation/tracked-state root environment)))))))

(deftest external-repository-preservation-test
  ;; Inherited Git overrides must not redirect fixture commits to another repository.
  (#'preservation/with-project
   (fn [root environment]
     (let [git! (fn [& args] (apply #'preservation/git! root environment args))]
       (git! "config" "user.name" "External Test")
       (git! "config" "user.email" "external@example.invalid")
       (git! "-c" "commit.gpgsign=false" "commit" "-m" "External baseline")
       (spit (io/file root "src/probe.clj") "(ns probe)\n(def x 2)\n")
       (git! "add" "src/probe.clj")
       (spit (io/file root "src/probe.clj") "(ns probe)\n(def x 3)\n")
       (let [before (#'preservation/tracked-state root environment)
             head (git! "rev-parse" "HEAD")
             index-bytes (vec (java.nio.file.Files/readAllBytes
                               (.toPath (io/file root ".git/index"))))
             result (shell/sh
                     (str (io/file (System/getProperty "java.home") "bin/java"))
                     "-cp" (System/getProperty "java.class.path")
                     "clojure.main" "-m" "scry.cli" "--var"
                     "closed-record.hooks-test/hook-lint-setup-failure-test"
                     :env (assoc environment
                                 "GIT_DIR" (str (io/file root ".git"))
                                 "GIT_WORK_TREE" (str root)
                                 "GIT_INDEX_FILE" (str (io/file root ".git/index"))))]
         (is (zero? (:exit result)) (pr-str result))
         (is (= index-bytes (vec (java.nio.file.Files/readAllBytes
                                  (.toPath (io/file root ".git/index"))))))
         (is (= head (git! "rev-parse" "HEAD")))
         (is (= before (#'preservation/tracked-state root environment))))))))
