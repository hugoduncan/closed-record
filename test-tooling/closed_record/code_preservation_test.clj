(ns closed-record.code-preservation-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [closed-record.quality-scope :as scope]
            [closed-record.test-support.code-project :as code-project]
            [closed-record.index-scope :as index]))

(deftest tracked-code-preservation-test
  ;; Real checks must preserve the whole tracked tree, not only the defective file.
  (code-project/with-project
    (fn [root environment]
      (doseq [[label source operation warnings errors]
              [["clean format" "(ns probe)\n(def x 1)\n" :format 0 0]
               ["format defect" "(ns probe)\n(def x 1 )\n" :format nil nil]
               ["clean lint" "(ns probe)\n(def x 1)\n" :lint 0 0]
               ["warning only" "(ns probe)\n(let [unused 1] 2)\n" :lint 1 0]
               ["lint error" "(ns probe)\n(missing)\n" :lint 0 1]]]
        (testing label
          (spit (io/file root "src/probe.clj") source)
          (let [before (code-project/tracked-state root environment)
                result (code-project/task! root environment (if (= :format operation) "clj-fmt:check" "clj-kondo:lint"))]
            (is (= (and (= 0 warnings) (= 0 errors)) (zero? (:exit result)))
                (pr-str result))
            (when (= "format defect" label)
              (is (.contains (str (:out result) (:err result)) "src/probe.clj")
                  (pr-str result)))
            (when (= :lint operation)
              (let [report (edn/read-string (:out result))
                    summary (:summary report)]
                (when (pos? (+ warnings errors))
                  (is (some #(and (.endsWith (:filename %) "src/probe.clj")
                                  (= (if (pos? warnings) :unused-binding :unresolved-symbol)
                                     (:type %)))
                            (:findings report)) (pr-str result)))
                (is (= warnings (:warning summary)) (pr-str result))
                (is (= errors (:error summary)) (pr-str result))))
            (is (= before (code-project/tracked-state root environment)))))))))

(deftest explicit-format-task-test
  ;; Public arguments select only the requested file, and repair stays explicit.
  (code-project/with-project
    (fn [root environment]
      (let [path "explicit path.clj"
            target (io/file root path)]
        (spit target "(def selected 1 )\n")
        (spit (io/file root "src/probe.clj") "(ns probe)\n(def x 1 )\n")
        (code-project/git! root environment "add" path)
        (let [before (code-project/tracked-state root environment)
              result (code-project/task! root environment "clj-fmt" "check" path)]
          (is (pos? (:exit result)) (pr-str result))
          (is (.contains (str (:out result) (:err result)) path) (pr-str result))
          (is (= before (code-project/tracked-state root environment)))
          (let [repair (code-project/task! root environment "clj-fmt" "fix" path)
                after (code-project/tracked-state root environment)]
            (is (zero? (:exit repair)) (pr-str repair))
            (is (= "(def selected 1)\n" (slurp target)))
            (is (= (:index before) (:index after)))
            (is (= (dissoc (:files before) path) (dissoc (:files after) path)))
            (let [clean (code-project/task! root environment "clj-fmt" "check" path)]
              (is (zero? (:exit clean)) (pr-str clean))
              (is (= after (code-project/tracked-state root environment))))))))))

(deftest shared-owned-file-scope-test
  ;; Project checks and hooks select the same extensions and exclude nested tool output.
  (code-project/with-project
    (fn [root environment]
      (let [excluded ["dev/.cache/generated.clj" "src/nested/.cpcache/generated.clj"
                      "test/imports/generated.clj" "dev/nested/target/generated.clj"]
            defect "(ns generated)\n(missing 1 )\n"]
        (doseq [path excluded]
          (io/make-parents (io/file root path))
          (spit (io/file root path) defect))
        (spit (io/file root "dev/probe.bb") "(ns probe)\n(def script 1)\n")
        (code-project/git! root environment "add" ".")
        (testing "project and index select the same owned files"
          (shell/with-sh-env environment
            (is (= (set (scope/project-source-paths! root)) (set (index/paths! root))))))
        (testing "excluded defects do not fail checks or change during project repair"
          (doseq [operation [:format :lint :fix]]
            (let [before (code-project/tracked-state root environment)
                  result (code-project/check! root environment operation)]
              (is (zero? (:exit result)) (pr-str result))
              (is (= before (code-project/tracked-state root environment))))))
        (testing "Babashka files are checked and repaired"
          (spit (io/file root "dev/probe.bb") "(ns probe)\n(missing 1 )\n")
          (doseq [operation [:format :lint]]
            (let [before (code-project/tracked-state root environment)
                  result (code-project/check! root environment operation)]
              (is (pos? (:exit result)) (pr-str result))
              (is (= before (code-project/tracked-state root environment)))))
          (is (zero? (:exit (code-project/check! root environment :fix))))
          (is (= "(ns probe)\n(missing 1)\n" (slurp (io/file root "dev/probe.bb"))))
          (doseq [path excluded]
            (is (= defect (slurp (io/file root path))) path)))))))

(deftest wrong-required-types-test
  ;; Wrong types fail before tool setup or repair, preserving all indexed bytes.
  (doseq [path scope/project-paths]
    (testing path
      (code-project/with-project
        (fn [root environment]
          (code-project/wrong-type! root path)
          (code-project/git! root environment "add" "-A")
          (doseq [operation [:format :fix :lint]]
            (let [before (code-project/tracked-state root environment)
                  result (code-project/check! root environment operation)]
              (is (pos? (:exit result)) (pr-str result))
              (is (.contains (:err result) "Invalid required quality path types:") (pr-str result))
              (is (.contains (:err result) path) (pr-str result))
              (is (= before (code-project/tracked-state root environment))))))))))

(deftest global-lint-configuration-test
  ;; A native control proves the fixture configuration can suppress both defects.
  (code-project/with-project
    (fn [root environment]
      (spit (io/file root "src/probe.clj")
            "(ns probe)\n(let [unused 1] 2)\n(missing)\n")
      (let [before (code-project/tracked-state root environment)
            control (shell/sh "clj-kondo" "--config-dir" ".clj-kondo"
                              "--config" "{:output {:format :edn}}"
                              "--lint" "src/probe.clj" :dir (str root) :env environment)
            result (code-project/check! root environment :lint)]
        (is (zero? (:exit control)) (pr-str control))
        (is (empty? (:findings (edn/read-string (:out control)))) (pr-str control))
        (is (pos? (:exit result)) (pr-str result))
        (is (= {:warning 1 :error 1}
               (select-keys (:summary (edn/read-string (:out result))) [:warning :error]))
            (pr-str result))
        (is (= before (code-project/tracked-state root environment)))))))

(deftest global-import-configuration-test
  ;; Dependency import must not even parse the user's unrelated configuration.
  (code-project/with-project
    (fn [root environment]
      (spit (io/file root "user-config/clj-kondo/config.edn") "{invalid")
      (let [before (code-project/tracked-state root environment)
            result (shell/sh
                    "bb" "-cp" (.getCanonicalPath (io/file "dev/quality"))
                    "-e" (str "(require '[closed-record.lint-dependencies :as deps])"
                              "(deps/copy-config! " (pr-str (str root)) ")")
                    :env environment)]
        (is (zero? (:exit result)) (pr-str result))
        (is (= before (code-project/tracked-state root environment)))))))