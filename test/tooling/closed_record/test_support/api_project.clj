(ns closed-record.test-support.api-project
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn with-project
  "Call f with a temporary project; remove it on return or failure."
  [f]
  (let [root (.toFile (Files/createTempDirectory "api-docs-" (make-array FileAttribute 0)))]
    (try
      (doseq [path ["bb.edn" "deps.edn" ".clj-kondo/config.edn" "src/closed_record/core.cljc"
                    "dev/quality/closed_record/api_docs.clj"
                    "dev/quality/closed_record/quality_scope.clj"
                    "dev/quality/closed_record/api_docs_cli.clj"]]
        (let [target (io/file root path)]
          (io/make-parents target)
          (io/copy (io/file path) target)))
      (f root)
      (finally
        (doseq [file (reverse (file-seq root))]
          (io/delete-file file))))))

