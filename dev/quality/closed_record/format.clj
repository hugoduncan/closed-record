(ns closed-record.format
  "Shared cljfmt command boundary."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [closed-record.quality-scope :as scope]))

(defn format!
  "Check or fix explicit paths, or the required project scope. Return the tool exit.
  Reject options as paths so callers cannot override the owned configuration."
  [root args]
  (let [[operation & paths] args]
    (when (or (not (#{"check" "fix"} operation))
              (some #(or (str/blank? %) (str/starts-with? % "-")) paths))
      (throw (ex-info "Usage: bb clj-fmt check|fix [PATHS...]" {:args args})))
    (let [inputs (if (seq paths)
                   (scope/validate-paths! root (vec paths))
                   (scope/project-source-paths! root))
          _ (scope/validate-paths! root [".cljfmt.edn"] scope/required-types)
          {:keys [exit out err]}
          (apply shell/sh
                 (into ["cljfmt" operation "--config" ".cljfmt.edn"
                        "--file-pattern" "\\.(?:clj[cs]?|bb|edn)$"]
                       (concat inputs [:dir (str root)])))]
      (print out)
      (flush)
      (binding [*out* *err*] (print err) (flush))
      exit)))
