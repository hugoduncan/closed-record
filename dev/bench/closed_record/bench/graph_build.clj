(ns closed-record.bench.graph-build
  "Read verified preparation artifacts without loading instrumentation."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [closed-record.bench.graph-tool :as tool])
  (:import [java.security MessageDigest]
           [java.util HexFormat]
           [java.util.jar JarFile]))

(def ^:private source-sha256
  "bff30ae8353f5a28811c55f1bf68ce4b37e42c2191f7cc68778cde28958a7d33")

(def ^:private artifacts
  ["RecordGraphProbe.java" "compiler.txt"
   "classes/RecordGraphProbe.class" "record-graph-agent.jar"])

(defn- digest
  [input]
  (with-open [stream (io/input-stream input)]
    (.formatHex (HexFormat/of)
                (.digest (MessageDigest/getInstance "SHA-256")
                         (.readAllBytes stream)))))

(defn read-declaration
  "Verify preparation checksums, pinned source, and the class inside the JAR.
  Return portable agent hashes without modifying files or loading the agent.
  This checks retained artifact consistency, not compiler trust or transformed
  runtime classes. Child observations must still match this declaration."
  [directory]
  (let [lines (str/split-lines (slurp (io/file directory "SHA256SUMS")))
        entries (mapv #(rest (re-matches #"([0-9a-f]{64})  (.+)" %)) lines)]
    (when-not (= artifacts (mapv second entries))
      (throw (ex-info "Invalid graph build checksum inventory" {:lines lines})))
    (let [hashes (into {} (map (fn [[hash path]] [path hash])) entries)]
      (doseq [path artifacts]
        (when-not (= (get hashes path) (digest (io/file directory path)))
          (throw (ex-info "Graph build checksum mismatch" {:artifact path}))))
      (when-not (= source-sha256 (get hashes "RecordGraphProbe.java"))
        (throw (ex-info "Unknown graph build source" {})))
      (with-open [jar (JarFile. (io/file directory "record-graph-agent.jar"))]
        (let [entry (.getJarEntry jar "RecordGraphProbe.class")]
          (when-not (and entry
                         (= "RecordGraphProbe"
                            (some-> (.getManifest jar) .getMainAttributes
                                    (.getValue "Premain-Class")))
                         (= (get hashes "classes/RecordGraphProbe.class")
                            (digest (.getInputStream jar entry))))
            (throw (ex-info "Graph build agent contents mismatch" {})))))
      (tool/declaration
       {:jar-sha256 (get hashes "record-graph-agent.jar")
        :class-sha256 (get hashes "classes/RecordGraphProbe.class")}))))
