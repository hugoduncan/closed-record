(ns closed-record.api-docs
  "In-memory API reference generation. File operations belong at the CLI boundary."
  (:require [quickdoc.api :as quickdoc]))

(defn generate
  "Return the API reference as Markdown without writing an output file."
  []
  (str "# API reference\n\n"
       "Generated file. Do not edit. Regenerate with `bb api-docs`; "
       "check with `bb api-docs --check`.\n\n"
       "See the [README](../README.md) for examples. "
       "This library supports Clojure 1.11+ on the JVM only. "
       "ClojureScript and Babashka are not supported.\n\n"
       (:markdown
        (quickdoc/quickdoc
         {:outfile false
          :source-paths ["src/closed_record/core.cljc"]
          :github/repo "https://github.com/hugoduncan/closed-record"
          :git/branch "master"
          :overrides {'closed-record.core
                      {'ClosedRecord {:no-doc true}
                       '-extra-keys {:no-doc true}}}}))))
