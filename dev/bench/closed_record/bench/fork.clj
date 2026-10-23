(ns closed-record.bench.fork
  "Fork layer of the benchmarks (V-11, S-03): the run order of a fork, the
  child command and the parent, which starts the forks, reads their result
  files and prints the table. No criterium dependency, so the tests can load
  it."
  (:require
   [clojure.java.io :as io]
   [closed-record.bench.report :as report])
  (:import
   (java.io File)
   (java.lang.management ManagementFactory)))

(defn in-order
  "Call the thunks `p-run` and `r-run` once each: `P` first in even forks,
  `R` first in odd forks. Return `[p r]`."
  [fork p-run r-run]
  (if (even? fork)
    (let [p (p-run) r (r-run)] [p r])
    (let [r (r-run) p (p-run)] [p r])))

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
