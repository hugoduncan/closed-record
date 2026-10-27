(ns closed-record.hooks
  "Hook boundaries. Invoke only inside pre-commit's staged-files context."
  (:require [closed-record.format :as format]
            [closed-record.index-scope :as index]
            [closed-record.lint :as lint]))

(defn format-staged!
  "Repair supplied indexed, owned paths. Never stage files.
  Pre-commit detects repairs and stops the commit for manual review."
  [root paths]
  (let [indexed (set (index/paths! root))
        selected (into [] (comp (filter indexed) (map #(str "./" %))) paths)]
    (if (seq selected)
      (format/format! root (into ["fix"] selected))
      0)))

(defn lint-staged!
  "Lint the index scope in pre-commit's staged-files context and return its exit."
  [root]
  (let [{:keys [exit out err]} (lint/lint-index! root)]
    (print out)
    (flush)
    (binding [*out* *err*] (print err) (flush))
    exit))
