(ns closed-record.api-docs
  "In-memory API reference generation. File operations belong at the CLI boundary."
  (:require [clj-kondo.core :as kondo]
            [closed-record.quality-scope :as scope]
            [quickdoc.impl :as quickdoc]))

(def ^:private source-path "src/closed_record/core.cljc")

(defn- reference-markdown []
  (scope/validate-paths! "." [source-path ".clj-kondo/config.edn"]
                         {source-path :file ".clj-kondo/config.edn" :file})
  ;; Quickdoc v0.2.6 does not expose :repro. Keep analysis owned and use its
  ;; pinned renderer without changing process globals or contributor settings.
  (let [result (kondo/run!
                {:lint [source-path]
                 :repro true
                 :config-dir ".clj-kondo"
                 :config {:skip-comments true
                          :output {:analysis
                                   {:arglists true
                                    :var-definitions {:meta [:no-doc :skip-wiki :arglists]}
                                    :namespace-definitions {:meta [:no-doc :skip-wiki]}}}}})
        errors (into [] (filter #(= :error (:level %))) (:findings result))
        _ (when (seq errors)
            (throw (ex-info (str "API source analysis failed: " source-path " "
                                 (pr-str errors))
                            {:source source-path :findings errors})))
        analysis (:analysis result)
        vars (into [] (remove #(#{'clojure.core/declare 'cljs.core/declare}
                                (:defined-by %)))
                   (:var-definitions analysis))
        namespaces (group-by :name (:namespace-definitions analysis))
        entries (group-by :ns vars)
        names (update-vals entries #(into #{} (map :name) %))
        options {:github/repo "https://github.com/hugoduncan/closed-record"
                 :git/branch "master"
                 :toc true
                 :var-links true
                 :var-regex #"`([^\s`]+)`"}
        overrides {}]
    (with-out-str
      (quickdoc/print-toc entries namespaces names options overrides)
      (doseq [[namespace definitions] (sort-by first entries)]
        (quickdoc/print-namespace namespaces names namespace definitions options overrides)))))

(defn generate
  "Return the API reference as Markdown without writing an output file."
  []
  (str "# API reference\n\n"
       "Generated file. Do not edit. Regenerate with `bb api-docs`; "
       "check with `bb api-docs --check`.\n\n"
       "See the [README](../README.md) for examples. "
       "This library supports Clojure 1.11+ on the JVM only. "
       "ClojureScript and Babashka are not supported.\n\n"
       (reference-markdown)))
