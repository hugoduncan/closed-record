(ns closed-record.index-scope
  "Index path selection for staged-content quality hooks. Does not stage files."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [closed-record.quality-scope :as scope]))

(defn select-paths!
  "Validate required scope against index paths and return owned source paths.
  A directory must contain an indexed file; working-tree files cannot satisfy it."
  ([paths] (select-paths! paths {}))
  ([paths modes]
   (let [indexed (set paths)
         missing (filterv
                  (fn [required]
                    (not (or (contains? indexed required)
                             (some #(str/starts-with? % (str required "/")) paths))))
                  scope/project-paths)
         invalid (filterv
                  (fn [required]
                    (case (get scope/required-types required)
                      :directory (contains? indexed required)
                      :file (or (some #(str/starts-with? % (str required "/")) paths)
                                (not (#{"100644" "100755"} (get modes required "100644"))))))
                  scope/project-paths)]
     (when (seq missing)
       (throw (ex-info (str "Missing indexed quality paths: " (str/join ", " missing))
                       {:missing-paths missing})))
     (when (seq invalid)
       (throw (ex-info (str "Invalid indexed quality path types: " (str/join ", " invalid))
                       {:invalid-paths invalid})))
     (into [] (comp (filter scope/owned-source?)
                    (filter #(#{"100644" "100755"} (get modes % "100644")))
                    (distinct)) paths))))

(defn paths!
  "Read and validate the Git index under root. Git failures throw.
  NUL delimiters preserve spaces and newlines in paths. Content isolation remains
  the responsibility of the pre-commit staged-files context."
  [root]
  (let [linked (filterv #(scope/linked-path? root %) scope/project-paths)]
    (when (seq linked)
      (throw (ex-info (str "Linked required quality paths: " (str/join ", " linked))
                      {:invalid-paths linked}))))
  (let [{:keys [exit out err]} (shell/sh "git" "ls-files" "--stage" "-z"
                                         :dir (str root))]
    (when-not (zero? exit)
      (throw (ex-info (str "Cannot read quality index: " err) {:exit exit})))
    (let [entries (into []
                        (map #(str/split % #"\t" 2))
                        (remove str/blank? (str/split out #"\u0000")))]
      (filterv #(not (scope/linked-path? root %))
               (select-paths! (mapv second entries)
                              (into {} (map (fn [[metadata path]]
                                              [path (first (str/split metadata #" "))])) entries))))))
