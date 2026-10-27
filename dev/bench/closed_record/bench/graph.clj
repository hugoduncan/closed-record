(ns closed-record.bench.graph
  "Pure adaptation of validated reachable-graph byte counts."
  (:require [closed-record.bench.metrics :as metrics]))

(defn outcomes
  "Return raw result, union, and incremental graph outcomes.
  Inputs count each physical object once within each named graph. Validate
  union bounds; incremental bytes are union minus source, not result minus
  source. Traversal support and physical identity remain collector obligations."
  [source result union]
  (when-not (and (every? #(and (integer? %) (<= 0 % Long/MAX_VALUE))
                         [source result union])
                 (<= (max source result) union (+' source result)))
    (throw (ex-info "Invalid reachable graph counts"
                    {:source source :result result :union union})))
  (mapv (fn [[root bytes]]
          (let [outcome {:metric :graph :unit :bytes :status :measured
                         :root root :raw bytes
                         :source-bytes source :result-bytes result
                         :union-bytes union}]
            (metrics/normalise outcome)
            outcome))
        [[:result result] [:source-and-result union]
         [:increment-relative-to-source (- union source)]]))
