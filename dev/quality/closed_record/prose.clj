(ns closed-record.prose
  "Vale command boundary. Style caches are the only permitted writes."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [closed-record.quality-scope :as scope]
            [closed-record.vale-results :as results]))

(defn- prose-paths! [root]
  (scope/validate-paths! root ["README.md" "README" "AGENTS.md" "doc/API.md"
                               ".vale.ini" "styles/config/vocabularies/ClosedRecord/accept.txt"])
  (when-not (.isDirectory (io/file root "README"))
    (throw (ex-info "Required prose directory: README" {})))
  (into ["README.md" "AGENTS.md" "doc/API.md"]
        (comp (filter #(.isFile %))
              (filter #(str/ends-with? (.getName %) ".md"))
              (map #(str (.relativize (.toPath (io/file root)) (.toPath %)))))
        (sort-by str (file-seq (io/file root "README")))))

(defn check!
  "Sync styles and check required prose with owned configuration. Return process
  output and exit status. Suggestions remain visible; setup failures never pass."
  [root args]
  (when (seq args)
    (throw (ex-info "Usage: bb vale" {:args args})))
  (let [paths (prose-paths! root)
        config ["--config=.vale.ini" "--no-global"]
        setup (apply shell/sh (into ["vale" "sync"] (concat config [:dir (str root)])))]
    (if-not (zero? (:exit setup))
      setup
      (let [{:keys [exit out] :as checked}
            (apply shell/sh
                   (into ["vale"]
                         (concat config ["--minAlertLevel=suggestion" "--output=JSON"]
                                 paths [:dir (str root)])))]
        ;; Preserve tool failures even if their output happens to be valid JSON.
        (if-not (zero? exit)
          checked
          (assoc checked :exit (:exit (results/outcome (json/parse-string out)))))))))
