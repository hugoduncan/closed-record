(ns closed-record.lint
  "Shared clj-kondo command boundary."
  (:require [clojure.java.shell :as shell]
            [closed-record.index-scope :as index]
            [closed-record.lint-dependencies :as dependencies]
            [closed-record.quality-scope :as scope]))

(defn- lint-paths! [root paths]
  (dependencies/copy-config! root)
  (apply shell/sh
         (into ["clj-kondo" "--repro" "--config-dir" ".clj-kondo"
                "--fail-level" "warning" "--config" "{:output {:format :edn}}"
                "--lint"]
               (concat paths [:dir (str root)]))))

(defn lint!
  "Prepare dependency configuration and lint the required scope. Return tool output
  and exit status. Warnings and errors both fail; setup failures throw."
  [root]
  (lint-paths! root (scope/project-source-paths! root)))

(defn lint-index!
  "Lint owned indexed paths, excluding untracked files. Return tool output and exit.
  The caller must isolate staged content; this reads working-tree bytes."
  [root]
  (lint-paths! root (index/paths! root)))
