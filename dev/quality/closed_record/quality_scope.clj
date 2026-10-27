(ns closed-record.quality-scope
  "Owned format and lint paths. Dependency imports are not owned inputs."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.nio.file Files]))

(def required-types
  "Expected types of required quality inputs, relative to the repository root."
  (array-map "src" :directory "test" :directory "dev" :directory
             "test-build" :directory "test-release" :directory "test-tooling" :directory
             "build.clj" :file "bb.edn" :file "deps.edn" :file
             ".cljfmt.edn" :file ".clj-kondo/config.edn" :file))

(def project-paths
  "Required project inputs, relative to the repository root."
  (vec (keys required-types)))

(defn linked-path?
  "True when a relative path has a symbolic link component below root."
  [root path]
  (loop [file (io/file root path)]
    (cond
      (= file (io/file root)) false
      (nil? file) false
      (Files/isSymbolicLink (.toPath file)) true
      :else (recur (.getParentFile file)))))

(defn- resolve-path [root path]
  (let [file (io/file path)]
    (if (.isAbsolute file) file (io/file root path))))

(defn validate-paths!
  "Return existing paths unchanged. Optional expected types constrain required inputs.
  Explicit inputs accept relative or absolute files and directories."
  ([root paths] (validate-paths! root paths {}))
  ([root paths expected-types]
   (let [missing (filterv #(not (.exists (resolve-path root %))) paths)
         invalid (filterv (fn [path]
                            (let [file (resolve-path root path)]
                              (and (.exists file)
                                   (get expected-types path)
                                   (or (linked-path? root path)
                                       (case (get expected-types path)
                                         :directory (not (.isDirectory file))
                                         :file (not (.isFile file)))))))
                          paths)]
     (when (seq missing)
       (throw (ex-info (str "Missing required quality paths: " (str/join ", " missing))
                       {:missing-paths missing})))
     (when (seq invalid)
       (throw (ex-info (str "Invalid required quality path types: " (str/join ", " invalid))
                       {:invalid-paths invalid})))
     paths)))

(defn project-paths!
  "Validate required path types and return the maintained project scope under root."
  [root]
  (validate-paths! root project-paths required-types))

(defn owned-source?
  "True for a repository-relative Clojure or EDN path in the maintained scope.
  Use slash-separated paths, as returned by Git."
  [path]
  (and (boolean (re-find #"\.(clj|cljc|cljs|bb|edn)$" path))
       (not-any? #{".." ".cache" ".cpcache" "imports" "target"}
                 (str/split path #"/"))
       (boolean
        (some #(or (= path %) (str/starts-with? path (str % "/")))
              project-paths))))

(defn regular-files
  "Return files below path without following symbolic files or directories."
  [path]
  (into []
        (filter #(and (not (Files/isSymbolicLink (.toPath %))) (.isFile %)))
        (tree-seq
         (fn [file] (and (not (Files/isSymbolicLink (.toPath file)))
                         (.isDirectory file)))
         #(seq (.listFiles %))
         (io/file path))))

(defn project-source-paths!
  "Validate required paths and select owned working-tree files, including untracked files.
  Project commands and indexed hooks use the same ownership predicate."
  [root]
  (let [base (.toPath (io/file root))]
    (into []
          (comp (mapcat #(regular-files (io/file root %)))
                (map #(str (.relativize base (.toPath %))))
                (map #(str/replace % java.io.File/separator "/"))
                (filter owned-source?))
          (project-paths! root))))
