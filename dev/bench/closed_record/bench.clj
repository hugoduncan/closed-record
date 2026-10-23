(ns closed-record.bench
  "Benchmarks of a closed record against `defrecord` (V-11, S-03).

  Run with `clojure -M:bench`. With no arguments, `-main` is the parent: it
  starts `report/fork-count` child JVMs, one after the other, reads their
  result files and prints the table. It exits 0 when all operations pass, 1
  if not, and 2 if a fork fails. With the arguments `fork result-file`,
  `-main` is a child: it measures and writes its result to the file."
  (:require
   [clojure.java.io :as io]
   [closed-record.bench.report :as report]
   [closed-record.core :refer [defclosedrecord]]
   [criterium.bench :as bench])
  (:import
   (java.io File)
   (java.lang.management ManagementFactory)))

(defclosedrecord P [a b c])
(defrecord R [a b c])

(defmacro bench-op
  "Bench the literal `expr` and return its per-call stats."
  [expr limit-s]
  `(do (bench/bench ~expr :viewer :none :limit-time-s ~limit-s)
       (report/per-call-stats (bench/last-bench))))

(defn- in-order
  "Call the thunks `p-run` and `r-run` once each: `P` first in even forks,
  `R` first in odd forks. Return `[p r]`."
  [fork p-run r-run]
  (if (even? fork)
    (let [p (p-run) r (r-run)] [p r])
    (let [r (r-run) p (p-run)] [p r])))

(defn run
  "Bench the six operations with `limit-s` seconds for each run, in the run
  order of fork `fork`. Each `bench` call site runs once. Return a vector of
  `[op p r]`, in `report/ops` order."
  [fork limit-s]
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
    (mapv (fn [op [p-run r-run]] (into [op] (in-order fork p-run r-run)))
          report/ops runs)))

(defn default-child-cmd
  "Return the command of child `fork` with result file `file`: the java binary,
  the input arguments and the classpath of this JVM."
  [fork file]
  (-> [(str (io/file (System/getProperty "java.home") "bin" "java"))]
      (into (.getInputArguments (ManagementFactory/getRuntimeMXBean)))
      (into ["-cp" (System/getProperty "java.class.path")
             "clojure.main" "-m" "closed-record.bench" (str fork) (str file)])))

(defn- run-fork
  "Run child `fork` with `child-cmd`. Return `{:result v}` or `{:error cause}`."
  [child-cmd fork]
  (let [file (File/createTempFile (str "closed-record-fork-" fork "-") ".edn")
        exit (-> (ProcessBuilder. ^java.util.List (child-cmd fork (str file)))
                 (.inheritIO)
                 (.start)
                 (.waitFor))]
    (cond
      (not= 0 exit)        {:error (str "exit " exit)}
      (not (.exists file)) {:error (str "missing result file " file)}
      :else                (report/decode-result (slurp file)))))

(defn parent
  "Run the forks with `child-cmd` (`[fork file]` → command vector), print the
  table or the fork error, and return the exit code 0, 1 or 2."
  [child-cmd]
  (loop [fork 0 results []]
    (if (= fork report/fork-count)
      (let [stats (report/fork-stats results)]
        (println (report/table (map #(apply report/row %) stats)))
        (flush)
        (report/exit-code stats))
      (let [{:keys [result error]} (run-fork child-cmd fork)]
        (if error
          (binding [*out* *err*]
            (println (str "fork " fork " failed: " error))
            (flush)
            2)
          (recur (inc fork) (conj results result)))))))

(defn- child [fork file]
  (spit file (report/encode-result (run (parse-long fork) 5)))
  0)

(defn -main [& args]
  (System/exit
   (case (count args)
     0 (parent default-child-cmd)
     2 (apply child args)
     (binding [*out* *err*]
       (println "usage: closed-record.bench [fork result-file]")
       64))))
