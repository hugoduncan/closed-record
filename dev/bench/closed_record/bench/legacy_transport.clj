(ns closed-record.bench.legacy-transport
  "Tool-aware transport for original S-03 statistics, without measurement tools."
  (:require [clojure.edn :as edn]
            [closed-record.bench.report :as report]
            [closed-record.bench.timing-tool :as timing-tool]))

(defn encode
  "Encode original statistics with validated observed collector identity."
  [rows observation]
  (timing-tool/declaration observation)
  (when-let [error (:error (report/decode-result (report/encode-result rows)))]
    (throw (ex-info "Invalid original timing statistics" {:reason error})))
  (pr-str {:schema-version 1 :timing-tool observation :results rows}))

(defn decode
  "Decode original statistics and reconcile the declared collector identity.
  Return an error value for invalid evidence; never alter the S-03 statistics."
  [text declaration]
  (try
    (let [envelope (edn/read-string text)]
      (when-not (= 1 (:schema-version envelope))
        (throw (ex-info "Unsupported original timing schema" {})))
      (timing-tool/validate-observation declaration (:timing-tool envelope))
      (let [decoded (report/decode-result (pr-str (:results envelope)))]
        (if (:error decoded)
          decoded
          (assoc decoded :timing-tool (:timing-tool envelope)))))
    (catch Exception error
      {:error (ex-message error)})))
