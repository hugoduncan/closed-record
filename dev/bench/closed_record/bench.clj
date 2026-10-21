(ns closed-record.bench
  "Benchmarks of a closed record against `defrecord` (V-11, S-03).

  Run with `clojure -M:bench`. Exits 0 when all operations pass, 1 if not."
  (:require
   [clojure.string :as str]
   [closed-record.core :refer [defclosedrecord]]
   [criterium.bench :as bench]))

(defclosedrecord P [a b c])
(defrecord R [a b c])

(def max-ratio
  "The closed record mean must not be more than 5% above the `defrecord` mean."
  1.05)

(defn per-call-stats
  "Return `{:mean :low :high}`, ns for one call, from `last-bench` data."
  [bench-data]
  (let [stats  (get-in bench-data [:data :bootstrap-stats])
        batch  (double (:batch-size stats))
        mean   (get-in stats [:bootstrap :elapsed-time :mean])
        alpha  (into {} (map (juxt :alpha :value)) (:estimate-quantiles mean))]
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

(defmacro bench-op
  "Bench the literal `expr` and return its per-call stats."
  [expr limit-s]
  `(do (bench/bench ~expr :viewer :none :limit-time-s ~limit-s)
       (per-call-stats (bench/last-bench))))

(defn- fmt-stats [{:keys [mean low high]}]
  (format "%.2f [%.2f, %.2f]" mean low high))

(defn row
  "Return the table row cells for operation `op` with stats `p` and `r`."
  [op p r]
  [op
   (fmt-stats p)
   (fmt-stats r)
   (format "%.3f" (ratio p r))
   (if (pass? p r) "pass" "fail")])

(def header ["Operation" "P mean [low, high] ns" "R mean [low, high] ns" "P / R" "Result"])

(defn table
  "Return the table text for `rows`, with padded columns."
  [rows]
  (let [all    (cons header rows)
        widths (apply map (fn [& cells] (apply max (map count cells))) all)
        line   (fn [cells]
                 (str "| "
                      (str/join " | " (map #(format (str "%-" %1 "s") %2) widths cells))
                      " |"))
        sep    (str "|" (str/join "|" (map #(apply str (repeat (+ 2 %) "-")) widths)) "|")]
    (str/join "\n" (concat [(line header) sep] (map line rows)))))

(defn run
  "Bench the five operations with `limit-s` seconds for each run.
  Return a vector of `[op p r]`."
  [limit-s]
  (let [px (->P 1 2 3)
        py (->P 1 2 3)
        rx (->R 1 2 3)
        ry (->R 1 2 3)]
    [["(:a x)"
      (let [x px] (bench-op (:a x) limit-s))
      (let [x rx] (bench-op (:a x) limit-s))]
     ["(.a x)"
      (let [x px] (bench-op (.a ^P x) limit-s))
      (let [x rx] (bench-op (.a ^R x) limit-s))]
     ["(assoc x :a 2)"
      (let [x px] (bench-op (assoc x :a 2) limit-s))
      (let [x rx] (bench-op (assoc x :a 2) limit-s))]
     ["(hash x)"
      (let [x px] (bench-op (hash x) limit-s))
      (let [x rx] (bench-op (hash x) limit-s))]
     ["(= x y)"
      (let [x px y py] (bench-op (= x y) limit-s))
      (let [x rx y ry] (bench-op (= x y) limit-s))]]))

(defn -main [& _]
  (let [results (run 10)]
    (println (table (map #(apply row %) results)))
    (flush)
    (System/exit (if (every? (fn [[_ p r]] (pass? p r)) results) 0 1))))
