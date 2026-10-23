(ns closed-record.bench.report
  "Pure layer of the benchmarks (V-11, S-03): per-call stats from
  `criterium.bench/last-bench` data, the encode and decode of the child
  result, the statistics over forks, the pass rule, the table and the exit
  code. No criterium dependency, so the tests can load it."
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def max-ratio
  "The closed record mean must not be more than 5% above the `defrecord` mean."
  1.05)

(def ops
  "The operation texts, in the run and table order."
  ["(:a x)" "(get x :a)" "(.a x)" "(assoc x :a 2)" "(hash x)" "(= x y)"])

(def fork-count
  "The number of child JVMs of one bench run."
  5)

(def ^:private t-975-df4
  "The 0.975 quantile of the Student t distribution with 4 degrees of freedom
  (`fork-count` - 1)."
  2.776)

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

(defn encode-result
  "Return the EDN text of a child result, a vector of `[op p r]`."
  [result]
  (pr-str result))

(defn- stats-error
  "Return an error text when `s` is not a `{:mean :low :high}` map of numbers."
  [s]
  (cond
    (not (map? s))                        "stats is not a map"
    (not= #{:mean :low :high} (set (keys s))) "stats keys are not :mean :low :high"
    (not (every? number? (vals s)))       "stats value is not a number"))

(defn- entry-error [i entry]
  (let [op (nth ops i)]
    (when-let [msg (cond
                     (not (and (vector? entry) (= 3 (count entry))))
                     "not a vector of 3 elements"

                     (not= op (first entry))
                     (str "operation is " (pr-str (first entry))
                          ", expected " (pr-str op))

                     :else
                     (some stats-error (rest entry)))]
      (str "entry " i ": " msg))))

(defn decode-result
  "Return `{:result v}` when `text` is a valid child result, or `{:error msg}`
  if not. Does not throw."
  [text]
  (let [v (try {:value (edn/read-string text)}
               (catch Exception e {:error (str "invalid EDN: " (ex-message e))}))]
    (if-let [e (:error v)]
      {:error e}
      (let [v (:value v)]
        (if-not (and (vector? v) (= (count ops) (count v)))
          {:error (str "not a vector of " (count ops) " entries")}
          (if-let [e (some identity (map-indexed entry-error v))]
            {:error e}
            {:result v}))))))

(defn- mean-interval
  "Return `{:mean :low :high}`: the mean of `xs` and the Student t 95%
  interval of the mean."
  [xs]
  (let [n    (count xs)
        mean (/ (reduce + xs) n)
        s    (Math/sqrt (/ (reduce + (map #(let [d (- % mean)] (* d d)) xs))
                           (dec n)))
        h    (/ (* t-975-df4 s) (Math/sqrt n))]
    {:mean mean :low (- mean h) :high (+ mean h)}))

(defn fork-stats
  "Return a vector of `[op p r n]` from `fork-count` decoded child results.
  `p` and `r` are the mean and t interval of the fork means; `n` is the
  number of forks."
  [results]
  {:pre [(= fork-count (count results))]}
  (vec
   (for [i (range (count ops))]
     (let [entries (map #(nth % i) results)
           col     (fn [k] (mean-interval (map #(double (:mean (nth % k))) entries)))]
       [(nth ops i) (col 1) (col 2) (count results)]))))

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
  "Return 0 when all `[op p r ...]` results pass, 1 if not."
  [results]
  (if (every? (fn [[_ p r]] (pass? p r)) results) 0 1))

(defn- fmt
  "Format with `Locale/ROOT`, so the decimal mark is `.` in all locales."
  [pattern & args]
  (String/format java.util.Locale/ROOT pattern (to-array args)))

(defn- fmt-stats [{:keys [mean low high]}]
  (fmt "%.2f [%.2f, %.2f]" mean low high))

(defn row
  "Return the table row cells for operation `op` with stats `p` and `r`, over
  `n` forks."
  [op p r n]
  [op
   (fmt-stats p)
   (fmt-stats r)
   (fmt "%.3f" (ratio p r))
   (if (pass? p r) "pass" "fail")
   (str n)])

(def header
  ["Operation" "P mean [low, high] ns" "R mean [low, high] ns" "P / R" "Result" "Forks"])

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
