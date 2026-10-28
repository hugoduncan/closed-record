(ns closed-record.test-support.hooks
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [closed-record.test-support.code-project :as preservation]))

(defn with-hooks
  "Call f with an isolated project and installed hooks; remove it afterward."
  [f]
  (preservation/with-project
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
            git! (fn [& args] (apply preservation/git! root env args))]
        (git! "config" "user.name" "Hook Test")
        (git! "config" "user.email" "hook@example.invalid")
        (git! "config" "commit.gpgsign" "false")
        (git! "add" "bb.edn" "dev" ".pre-commit-config.yaml")
        (git! "commit" "-m" "Fixture baseline")
        (let [installed (run "pre-commit" "install")]
          (when-not (zero? (:exit installed))
            (throw (ex-info "Hook installation failed" installed))))
        (f root env run git!)))))

