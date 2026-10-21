(ns closed-record.bench
  "Benchmarks of a closed record against `defrecord` (V-11, S-03).

  Run with `clojure -M:bench`. Exits 0 when all operations pass, 1 if not."
  (:require
   [closed-record.bench.report :as report]
   [closed-record.core :refer [defclosedrecord]]
   [criterium.bench :as bench]))

(defclosedrecord P [a b c])
(defrecord R [a b c])

(defmacro bench-op
  "Bench the literal `expr` and return its per-call stats."
  [expr limit-s]
  `(do (bench/bench ~expr :viewer :none :limit-time-s ~limit-s)
       (report/per-call-stats (bench/last-bench))))

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
    (println (report/table (map #(apply report/row %) results)))
    (flush)
    (System/exit (report/exit-code results))))
