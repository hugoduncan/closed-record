(ns closed-record.lint-dependencies-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [closed-record.lint-dependencies :as dependencies]
            [closed-record.test-support.code-project :as preservation])
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

(deftest independent-bases-import-test
  ;; Exported rules from additive and replacing aliases must both affect real lint.
  (preservation/with-project
    (fn [root environment]
      (doseq [kind ["additive" "replacing"]]
        (let [dependency (io/file root "local-deps" kind)
              config (io/file dependency "resources/clj-kondo.exports/fixture" kind "config.edn")]
          (io/make-parents config)
          (spit (io/file dependency "deps.edn") "{:paths [\"resources\"]}\n")
          (spit config (pr-str {:lint-as (hash-map (symbol (str "fixture." kind) "bind")
                                                   'clojure.core/let)}))))
      (spit (io/file root "deps.edn")
            (pr-str {:deps {}
                     :aliases {:test {:extra-deps {'fixture/additive {:local/root "local-deps/additive"}}}
                               :build {:replace-deps {'fixture/replacing {:local/root "local-deps/replacing"}}}}}))
      (spit (io/file root "src/probe.clj")
            (str "(ns probe (:require [fixture.additive :as a] [fixture.replacing :as r]))\n"
                 "(a/bind [left 1] left)\n(r/bind [right 2] right)\n"))
      (preservation/git! root environment "add" ".")
      (let [before (preservation/tracked-state root environment)
            lint #(shell/sh "clj-kondo" "--repro" "--config-dir" ".clj-kondo"
                            "--config" "{:output {:format :edn}}" "--lint" "src/probe.clj"
                            :dir (str root) :env environment)]
        (is (not (.exists (io/file root ".clj-kondo/imports"))))
        (is (pos? (:exit (lint))) "Without imports, binding symbols are unresolved")
        (shell/with-sh-env environment (dependencies/copy-config! root))
        (let [result (lint)
              summary (:summary (edn/read-string (:out result)))]
          (is (zero? (:exit result)) (pr-str result))
          (is (= 0 (:error summary)))
          (is (= 0 (:warning summary))))
        (is (= before (preservation/tracked-state root environment)))))))
