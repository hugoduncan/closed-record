(ns closed-record.lint-dependencies
  "Resolve independent dependency bases before importing lint configuration."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [closed-record.quality-scope :as scope]))

(def dependency-bases
  "Separate bases prevent dependency-replacing aliases from hiding imports."
  [nil :test :build :build-test :bench :nrepl :deploy :quickdoc
   :tooling-test :clj-1.12 :clj-1.13])

(defn merge-classpaths
  "Combine classpaths in stable order without duplicate entries."
  [classpaths]
  (->> classpaths
       (into [] (comp (mapcat #(str/split (str/trim %) (re-pattern java.io.File/pathSeparator)))
                      (remove str/blank?)
                      (distinct)))
       (str/join java.io.File/pathSeparator)))

(defn- execute! [root command]
  (let [{:keys [exit out err] :as result}
        (apply shell/sh (into command [:dir (str root)]))]
    (when-not (zero? exit)
      (throw (ex-info (str "Dependency lint setup failed: " (str/join " " command)
                           "\n" err out)
                      (assoc result :command command))))
    result))

(defn copy-config!
  "Validate scope, resolve each basis, and copy dependency lint configurations.
  Failed resolution or import stops setup. Only ignored lint caches/imports change."
  [root]
  (scope/project-paths! root)
  (let [classpaths (mapv (fn [alias]
                           (:out (execute! root (cond-> ["clojure" "-Srepro"]
                                                  alias (conj (str "-A" alias))
                                                  true (conj "-Spath")))))
                         dependency-bases)
        result (execute! root ["clj-kondo" "--repro" "--config-dir" ".clj-kondo"
                               "--lint" (merge-classpaths classpaths)
                               "--dependencies" "--copy-configs" "--skip-lint"])]
    (print (:out result))
    (binding [*out* *err*] (print (:err result)))
    nil))
