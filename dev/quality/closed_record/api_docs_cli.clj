(ns closed-record.api-docs-cli
  "File boundary for the generated API reference."
  (:require [clojure.java.io :as io] [closed-record.api-docs :as docs]
            [closed-record.quality-scope :as scope])
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file Files]
           [java.util Arrays]))

(defn execute!
  "Generate the reference, or check its exact UTF-8 bytes without writing."
  [args]
  (if-not (contains? #{[] ["--check"]} (vec args))
    (do (binding [*out* *err*]
          (println "Usage: bb api-docs [--check]"))
        2)
    (do
      (when (scope/linked-path? "." "doc/API.md")
        (throw (ex-info "Symbolic links are not allowed in API output path: doc/API.md"
                        {:path "doc/API.md"})))
      (let [output (io/file "doc/API.md")
            expected (.getBytes (docs/generate) StandardCharsets/UTF_8)]
        (if (= ["--check"] (vec args))
          (if (and (.isFile output)
                   (Arrays/equals expected (Files/readAllBytes (.toPath output))))
            0
            (do (binding [*out* *err*]
                  (println "Missing or stale doc/API.md. Run bb api-docs to regenerate."))
                1))
          (do (io/make-parents output)
              (with-open [stream (io/output-stream output)]
                (.write stream expected))
              0))))))

(defn -main
  "Run the API reference command."
  [& args]
  (System/exit (execute! args)))
