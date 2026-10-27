(ns closed-record.quality-scope
  "Owned format and lint paths. Dependency imports are not owned inputs."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def project-paths
  "Required project inputs, relative to the repository root."
  ["src" "test" "dev" "test-build" "test-release" "test-tooling"
   "build.clj" "bb.edn" "deps.edn" ".cljfmt.edn" ".clj-kondo/config.edn"])

(defn validate-paths!
  "Return paths unchanged when all exist under root; otherwise throw with missing paths.
  Explicit inputs do not require the rest of the project scope."
  [root paths]
  (let [missing (filterv #(not (.exists (io/file root %))) paths)]
    (when (seq missing)
      (throw (ex-info (str "Missing required quality paths: " (str/join ", " missing))
                      {:missing-paths missing})))
    paths))

(defn project-paths!
  "Validate and return the maintained project scope under root."
  [root]
  (validate-paths! root project-paths))

(defn owned-source?
  "True for a repository-relative Clojure or EDN path in the maintained scope.
  Use slash-separated paths, as returned by Git."
  [path]
  (and (boolean (re-find #"\.(clj|cljc|cljs|edn)$" path))
       (not-any? #{".." ".cache" ".cpcache" "imports" "target"}
                 (str/split path #"/"))
       (boolean
        (some #(or (= path %) (str/starts-with? path (str % "/")))
              project-paths))))
