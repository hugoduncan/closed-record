(ns closed-record.bench.report
  "Pure layer of the benchmarks (V-11, S-03): per-call stats from
  `criterium.bench/last-bench` data, the pass rule, the table and the exit
  code. No criterium dependency, so the tests can load it."
  (:require
   [clojure.string :as str]))

(def max-ratio
  "The closed record mean must not be more than 5% above the `defrecord` mean."
  1.05)

(defn per-call-stats
  "Return `{:mean :low :high}`, ns for one call, from `last-bench` data."
  [bench-data]
  (let [stats (get-in bench-data [:data :bootstrap-stats])
        batch (double (:batch-size stats))
        mean  (get-in stats [:bootstrap :elapsed-time :mean])
        alpha (into {} (map (juxt :alpha :value)) (:estimate-quantiles mean))]
    {:mean (/ (:point-estimate mean) batch)
     :low  (/ (get alpha 0.025) batch)
     :high (/ (get alpha 0.975) batch)}))

(defn ratio
  "Return `P mean / R mean`."
  [p r]
  (/ (:mean p) (:mean r)))

(defn pass?
  "True when `p` is not more than 5% above `r`, or the 95% intervals overlap."
  [p r]
  (or (<= (ratio p r) max-ratio)
      (and (<= (:low p) (:high r))
           (<= (:low r) (:high p)))))

(defn exit-code
  "Return 0 when all `[op p r]` results pass, 1 if not."
  [results]
  (if (every? (fn [[_ p r]] (pass? p r)) results) 0 1))

(defn- fmt
  "Format with `Locale/ROOT`, so the decimal mark is `.` in all locales."
  [pattern & args]
  (String/format java.util.Locale/ROOT pattern (to-array args)))

(defn- fmt-stats [{:keys [mean low high]}]
  (fmt "%.2f [%.2f, %.2f]" mean low high))

(defn row
  "Return the table row cells for operation `op` with stats `p` and `r`."
  [op p r]
  [op
   (fmt-stats p)
   (fmt-stats r)
   (fmt "%.3f" (ratio p r))
   (if (pass? p r) "pass" "fail")])

(def header ["Operation" "P mean [low, high] ns" "R mean [low, high] ns" "P / R" "Result"])

(defn table
  "Return the table text for `rows`, with padded columns."
  [rows]
  (let [all    (cons header rows)
        widths (apply map (fn [& cells] (apply max (map count cells))) all)
        line   (fn [cells]
                 (str "| "
                      (str/join " | " (map #(fmt (str "%-" %1 "s") %2) widths cells))
                      " |"))
        sep    (str "|" (str/join "|" (map #(apply str (repeat (+ 2 %) "-")) widths)) "|")]
    (str/join "\n" (concat [(line header) sep] (map line rows)))))
