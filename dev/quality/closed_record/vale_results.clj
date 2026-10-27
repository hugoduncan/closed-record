(ns closed-record.vale-results
  "Validation and failure policy for decoded Vale JSON results."
  (:require [clojure.string :as str]))

(defn- nonblank-string? [value]
  (and (string? value) (not (str/blank? value))))

(defn- finding? [value]
  (and (map? value)
       (contains? #{"suggestion" "warning" "error"} (get value "Severity"))
       (nonblank-string? (get value "Check"))
       (nonblank-string? (get value "Message"))
       (pos-int? (get value "Line"))
       (let [span (get value "Span")]
         (and (vector? span)
              (= 2 (count span))
              (every? pos-int? span)
              (<= (first span) (second span))))))

(defn outcome
  "Validate a decoded Vale JSON object with string keys. Return all findings
  and an exit status: suggestions pass, warnings and errors fail. Invalid
  structures or unknown severities throw instead of passing silently."
  [result]
  (when-not (and (map? result)
                 (every? (fn [[path findings]]
                           (and (nonblank-string? path)
                                (vector? findings)
                                (every? finding? findings)))
                         result))
    (throw (ex-info "Invalid Vale results" {:result result})))
  (let [findings (into []
                       (mapcat (fn [[path alerts]]
                                 (mapv #(assoc % "Path" path) alerts)))
                       (sort-by key result))]
    {:exit (if (some #(contains? #{"warning" "error"} (get % "Severity")) findings)
             1
             0)
     :findings findings}))
