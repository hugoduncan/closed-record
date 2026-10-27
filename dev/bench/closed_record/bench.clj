(ns closed-record.bench
  "Benchmarks of a closed record against `defrecord` (V-11, S-03).

  Run with `sh dev/bench/timing/run.sh`. With no arguments, `-main` is the parent: it
  starts `report/fork-count` child JVMs, one after the other, reads their
  result files and prints the table. It exits 0 when all operations pass, 1
  if not, and 2 if a fork fails. With the arguments `fork result-file`,
  `-main` is a child: it measures and writes its result to the file. The
  parent and the run order are in `closed-record.bench.fork`."
  (:require
   [closed-record.bench.fork :as fork]
   [closed-record.bench.report :as report]
   [closed-record.bench.legacy-transport :as legacy-transport]
   [closed-record.bench.timing-tool :as timing-tool]
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
  "Bench the six operations with `limit-s` seconds for each run, in the run
  order of fork `n`. Each `bench` call site runs once. Return a vector of
  `[op p r]`, in `report/ops` order."
  [n limit-s]
  (let [px    (->P 1 2 3)
        py    (->P 1 2 3)
        rx    (->R 1 2 3)
        ry    (->R 1 2 3)
        runs  [[#(let [x px] (bench-op (:a x) limit-s))
                #(let [x rx] (bench-op (:a x) limit-s))]
               [#(let [x px] (bench-op (get x :a) limit-s))
                #(let [x rx] (bench-op (get x :a) limit-s))]
               [#(let [x px] (bench-op (.a ^P x) limit-s))
                #(let [x rx] (bench-op (.a ^R x) limit-s))]
               [#(let [x px] (bench-op (assoc x :a 2) limit-s))
                #(let [x rx] (bench-op (assoc x :a 2) limit-s))]
               [#(let [x px] (bench-op (hash x) limit-s))
                #(let [x rx] (bench-op (hash x) limit-s))]
               [#(let [x px y py] (bench-op (= x y) limit-s))
                #(let [x rx y ry] (bench-op (= x y) limit-s))]]]
    (mapv (fn [op [p-run r-run]] (into [op] (fork/in-order n p-run r-run)))
          report/ops runs)))

(defn- child [n file]
  (let [observation (timing-tool/observe)]
    (spit file (legacy-transport/encode (run n 5) observation)))
  0)

(defn -main [& args]
  (System/exit
   (case (count args)
     0 (fork/parent fork/default-child-cmd (timing-tool/observe))
     2 (let [[n file] args] (child (parse-long n) file))
     (binding [*out* *err*]
       (println "usage: closed-record.bench [fork result-file]")
       64))))
