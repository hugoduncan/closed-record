(ns closed-record.bench.metric-table
  "Pure Markdown tables for the fixed metric inventory."
  (:require [clojure.string :as str]
            [closed-record.bench.metrics :as metrics]
            [closed-record.bench.assoc-allocation-result :as allocation-result]))

(defn- cell [value]
  (-> (str value)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace "|" "&#124;")
      (str/replace #"[\r\n]+" " ")))

(defn- number-cell [value]
  (String/format java.util.Locale/ROOT "%.3f" (object-array [(double value)])))

(defn- row [cells]
  (str "| " (str/join " | " (mapv cell cells)) " |\n"))

(defn- summary-row [cases configurations summary]
  (let [{:keys [case-id configuration representation metric root unit status forks]} summary
        {:keys [contract operation width length]} (cases case-id)
        measured? (= :measured status)
        reasons (into []
                      (comp (remove #(= :measured (:status %)))
                            (map #(str "fork " (:fork %) " " (name (:status %)) ": " (:reason %))))
                      forks)]
    (row [case-id contract (or (:fixture (configurations configuration)) :clojure-record)
          configuration representation operation
          (cond width (str "width=" width) length (str "length=" length) :else "—")
          metric (or root "—") unit status
          (if measured? (number-cell (:mean summary)) "—")
          (if measured? (str (number-cell (:min summary)) "–" (number-cell (:max summary))) "—")
          (if measured? (number-cell (:sample-standard-deviation summary)) "—")
          (str/join "; " reasons)])))

(defn render
  "Validate complete raw outcomes and render a deterministic Markdown table.
  Means weight each fork equally. Range and sample SD are not confidence bounds.
  Missing measurements have no numeric cells; each failed fork retains its reason.
  This exploratory table makes no S-03 parity decision."
  [inventory outcomes]
  (let [summaries (metrics/aggregate-results inventory outcomes)
        cases (into {} (map (juxt :id identity)) (:cases inventory))]
    (str (row ["Case" "Topic" "Comparison type" "Configuration" "Representation"
               "Operation" "Dimensions" "Metric" "Root" "Unit" "Status"
               "Mean" "Range" "Sample SD" "Reasons"])
         (row (vec (repeat 15 "---")))
         (apply str (mapv #(summary-row cases (:configurations inventory) %) summaries)))))

(defn render-allocation-controls
  "Render supplied allocation controls per fork, without averaging or subtraction.
  Call after inventory validation. Rows without controls do not imply stability.
  Malformed supplied controls throw rather than disappear from the report."
  [outcomes]
  (let [controlled (filterv #(contains? % :allocation-controls) outcomes)]
    (when (seq controlled)
      (str "\n## Allocation controls\n\n"
           "Raw batch bytes include boundary costs; no costs are subtracted. "
           "Changed batches are one-based and include the final sample. "
           "Trailing matches count warmup batches equal to the final sample. "
           "Equal counts do not prove JIT convergence or acceptance. "
           "Rows without controls are not assessed here.\n\n"
           (row ["Case" "Configuration" "Representation" "Fork" "Operations"
                 "Assoc bytes" "Boundary bytes" "Assoc changed batches"
                 "Boundary changed batches" "Assoc trailing matches"
                 "Boundary trailing matches"])
           (row (vec (repeat 11 "---")))
           (apply str
                  (mapv (fn [outcome]
                          (let [summary (allocation-result/stability outcome)]
                            (row [(:case-id outcome) (:configuration outcome)
                                  (:representation outcome) (:fork outcome)
                                  (:operations summary) (:assoc-counts summary)
                                  (:boundary-counts summary) (:changed-batches summary)
                                  (:boundary-changed-batches summary)
                                  (:matching-trailing-warmup-batches summary)
                                  (:matching-trailing-boundary-warmup-batches summary)])))
                        (sort-by (juxt :case-id :configuration :representation :fork)
                                 controlled)))))))
