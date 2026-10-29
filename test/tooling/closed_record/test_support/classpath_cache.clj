(ns closed-record.test-support.classpath-cache
  "Warm Clojure CLI classpath caches for lint fixtures.

  Lint setup resolves each dependency basis with `clojure -Srepro -Spath`. In a
  new fixture, each command starts a JVM. The CLI cache key uses config paths
  and aliases, not deps.edn content. The CLI uses a cached classpath unless a
  config file is newer than it. Thus a cache resolved from identical deps.edn
  content can be copied into a fixture. Lint still runs the real commands."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [closed-record.lint-dependencies :as dependencies])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute FileTime]))

(defn- delete-tree [file]
  (doseq [entry (reverse (file-seq file))]
    (io/delete-file entry true)))

(def ^:private templates
  "Resolved .cpcache directories by deps.edn content, removed at JVM exit."
  (memoize
   (fn [deps]
     (let [root (.toFile (Files/createTempDirectory "classpath-cache-"
                                                    (make-array FileAttribute 0)))]
       (.addShutdownHook (Runtime/getRuntime) (Thread. #(delete-tree root)))
       (spit (io/file root "deps.edn") deps)
       (doseq [alias dependencies/dependency-bases]
         (let [result (apply shell/sh (concat ["clojure" "-Srepro"]
                                              (when alias [(str "-A" alias)])
                                              ["-Spath" :dir (str root)]))]
           (when-not (zero? (:exit result))
             (throw (ex-info "Classpath cache setup failed" result)))))
       (io/file root ".cpcache")))))

(defn- set-age! [file seconds]
  (Files/setLastModifiedTime
   (.toPath file)
   (FileTime/fromMillis (- (System/currentTimeMillis) (* 1000 seconds)))))

(defn seed!
  "Copy the resolved classpath cache for root's deps.edn into root/.cpcache.
  In a Git fixture, exclude the cache in .git/info/exclude so later staging
  does not track it. The copied cache is older than now and newer than
  deps.edn. Some shells compare file times in whole seconds, so a later
  deps.edn write is still newer than the cache."
  [root]
  (let [deps-file (io/file root "deps.edn")
        source (templates (slurp deps-file))
        target (io/file root ".cpcache")
        git-dir (io/file root ".git")]
    (when (.isDirectory git-dir)
      (let [exclude (io/file git-dir "info/exclude")]
        (io/make-parents exclude)
        (spit exclude "/.cpcache/\n" :append true)))
    (.mkdirs target)
    (set-age! deps-file 10)
    (doseq [file (.listFiles source)]
      (let [copy (io/file target (.getName file))]
        (io/copy file copy)
        (set-age! copy 5)))))
