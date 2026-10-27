(ns link-check
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]))

(defn- git-output [args]
  (:out (process/check
         (apply process/shell {:out :string} (into ["git"] args)))))

(defn check!
  "Check tracked Markdown with local fragments and online HTTP validation.
  Return a nonzero exit code if either lychee pass fails."
  []
  (let [root (str/trim (git-output ["rev-parse" "--show-toplevel"]))
        paths (into [] (remove str/blank?)
                    (str/split (git-output ["ls-files" "-z" "--" "*.md"]) #"\u0000"))]
    (when-not (= (fs/real-path root) (fs/real-path "."))
      (throw (ex-info "Run bb lychee:check from the repository root." {})))
    (when (empty? paths)
      (throw (ex-info "No tracked Markdown inputs found." {})))
    (doseq [path paths]
      (when-not (fs/regular-file? path)
        (throw (ex-info "Tracked Markdown input is missing or not a file."
                        {:path path}))))
    (let [base ["lychee" "--config" "lychee.toml" "--root-dir" root]
          ;; Do not short-circuit: report online failures even after local failures.
          exits (mapv (fn [options]
                        (:exit (apply process/shell {:continue true} (into (into base options) (into ["--"] paths)))))
                      [["--offline" "--include-fragments=anchor-only"]
                       ["--include-fragments=none"]])]
      (or (some #(when-not (zero? %) %) exits) 0))))
