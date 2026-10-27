(ns closed-record.index-scope
  "Index path selection for staged-content quality hooks. Does not stage files."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [closed-record.quality-scope :as scope]))

(defn select-paths!
  "Validate required scope against index paths and return owned source paths.
  A directory must contain an indexed file; working-tree files cannot satisfy it."
  [paths]
  (let [indexed (set paths)
        missing (filterv
                 (fn [required]
                   (not (or (contains? indexed required)
                            (some #(str/starts-with? % (str required "/")) paths))))
                 scope/project-paths)]
    (when (seq missing)
      (throw (ex-info (str "Missing indexed quality paths: " (str/join ", " missing))
                      {:missing-paths missing})))
    (into [] (comp (filter scope/owned-source?) (distinct)) paths)))

(defn paths!
  "Read and validate the Git index under root. Git failures throw.
  NUL delimiters preserve spaces and newlines in paths. Content isolation remains
  the responsibility of the pre-commit staged-files context."
  [root]
  (let [{:keys [exit out err]} (shell/sh "git" "ls-files" "--cached" "-z"
                                         :dir (str root))]
    (when-not (zero? exit)
      (throw (ex-info (str "Cannot read quality index: " err) {:exit exit})))
    (select-paths! (str/split out #"\u0000"))))
