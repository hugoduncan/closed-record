(ns closed-record.symlink-scope-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.test-support.code-project :as preservation]
            [closed-record.format :as format]
            [closed-record.test-support.hooks :as hooks]
            [closed-record.index-scope :as index]
            [closed-record.quality-scope :as scope])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- with-links [root f]
  (let [external (Files/createTempDirectory "external-source-" (make-array FileAttribute 0))
        source (.resolve external "defect.clj")
        links [(io/file root "src/external.clj") (io/file root "dev/external")]
        targets [source external]
        original "(ns defect)\n(missing 1 )\n"]
    (spit (.toFile source) original)
    (try
      (doseq [[link target] (map vector links targets)]
        (Files/createSymbolicLink (.toPath link) target (make-array FileAttribute 0)))
      (f)
      (is (= original (slurp (.toFile source))))
      (doseq [[link target] (map vector links targets)]
        (is (= target (Files/readSymbolicLink (.toPath link)))))
      (finally
        ;; Remove links before fixture cleanup, which uses file-seq.
        (doseq [link links] (Files/deleteIfExists (.toPath link)))
        (Files/delete source)
        (Files/delete external)))))

(deftest project-symlink-exclusion-test
  ;; Project checks and repair must not read or repair external linked sources.
  (preservation/with-project
    (fn [root environment]
      (with-links
        root
        (fn []
          (preservation/git! root environment "add" "src/external.clj" "dev/external")
          (shell/with-sh-env environment
            (let [project (set (scope/project-source-paths! root))]
              (is (= project (set (index/paths! root))))
              (is (not (contains? project "src/external.clj")))
              (is (not (contains? project "dev/external/defect.clj")))))
          (doseq [operation [:format :lint :fix]]
            (let [tree (preservation/git! root environment "write-tree")
                  result (preservation/check! root environment operation)]
              (is (zero? (:exit result)) (pr-str result))
              (is (= tree (preservation/git! root environment "write-tree")))))
          (testing "explicit formatting retains supplied-path semantics"
            (is (pos? (format/format! root ["check" "src/external.clj"])))))))))

(deftest installed-hook-symlink-exclusion-test
  ;; Actual installed hooks must skip both indexed file links and directory links.
  (hooks/with-hooks
    (fn [root _environment run git!]
      (with-links
        root
        (fn []
          (git! "add" "src/external.clj" "dev/external")
          (let [tree (git! "write-tree")
                result (run "git" "commit" "-m" "Linked source probe")]
            (is (zero? (:exit result)) (pr-str result))
            (is (= tree (git! "write-tree"))))
          (is (= 2 (count (filter #(= "120000" (first (.split % " ")))
                                  (.split (git! "ls-files" "--stage" "src/external.clj" "dev/external") "\n"))))))))))

(deftest required-path-symlink-test
  ;; Required roots, files, and configuration parents cannot redirect scope.
  (doseq [path ["src" ".cljfmt.edn" ".clj-kondo"]]
    (testing path
      (hooks/with-hooks
        (fn [root environment run git!]
          (let [original (.toPath (io/file root path))
                moved (.toPath (io/file root (str path "-original")))]
            (Files/move original moved (make-array java.nio.file.CopyOption 0))
            (try
              (Files/createSymbolicLink original moved (make-array FileAttribute 0))
              (git! "add" "-A")
              (let [tree (git! "write-tree")
                    head (git! "rev-parse" "HEAD")]
                (doseq [operation [:format :lint :fix]]
                  (let [result (preservation/check! root environment operation)]
                    (is (pos? (:exit result)) (pr-str result))
                    (is (.contains (:err result) "Invalid required quality path types") (pr-str result))))
                (let [result (run "git" "commit" "-m" "Linked required path probe")]
                  (is (pos? (:exit result)) (pr-str result))
                  (is (.contains (:err result) "Linked required quality paths") (pr-str result)))
                (is (= tree (git! "write-tree")))
                (is (= head (git! "rev-parse" "HEAD")))
                (is (= moved (Files/readSymbolicLink original))))
              (finally
                (Files/deleteIfExists original)
                (Files/move moved original (make-array java.nio.file.CopyOption 0))))))))))
