(ns closed-record.bench.value-type
  "VT-V8: a manual, informational benchmark of `hash` and `=` for a record
  with value output (`V`) against an ordinary closed record (`P`), both in
  the same child JVM. No pass rule.

  Run on a Valhalla JDK, through `dev/bench/timing/run.sh` with
  `BENCH_ALIASES=:value-type-test:clj-1.12`,
  `BENCH_JVM_OPTS=-J--enable-preview` and the arguments
  `-m closed-record.bench.value-type`. With no arguments, `-main` is the
  parent: it starts `report/fork-count` child JVMs one after the other and
  prints the table. With `fork result-file`, `-main` is a child. Exit codes:
  0 (all forks valid), 2 (a fork is not valid), 64 (usage).

  This namespace loads value-type. No other namespace requires it."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [closed-record.bench :refer [bench-op]]
   [closed-record.bench.fork :as fork]
   [closed-record.bench.report :as report]
   [closed-record.bench.timing-tool :as timing-tool]
   [closed-record.core :refer [defclosedrecord]])
  (:import
   (java.io File)
   (java.lang.management ManagementFactory)))

(defclosedrecord V [a b c] :value-type true)
(defclosedrecord P [a b c])

(def ops
  "The measured operations, in result order."
  ["(hash x)" "(= x y)"])

(defn- value-class?
  "True when `c` is a value class. False on a JVM without `Class.isValue`."
  [^Class c]
  (try
    (boolean (.invoke (.getMethod Class "isValue" (make-array Class 0))
                      c (object-array 0)))
    (catch NoSuchMethodException _ false)))

(defn- run
  "Bench `ops` for `V` and `P` with `limit-s` seconds for each run, in the run
  order of fork `n`. Return a vector of `[op v p]`."
  [n limit-s]
  (let [vx (->V 1 2 3)
        vy (->V 1 2 3)
        px (->P 1 2 3)
        py (->P 1 2 3)
        runs [[#(let [x vx] (bench-op (hash x) limit-s))
               #(let [x px] (bench-op (hash x) limit-s))]
              [#(let [x vx y vy] (bench-op (= x y) limit-s))
               #(let [x px y py] (bench-op (= x y) limit-s))]]]
    (mapv (fn [op [v-run p-run]] (into [op] (fork/in-order n v-run p-run)))
          ops runs)))

(defn- child [n file]
  (when-not (value-class? V)
    (throw (ex-info "V is not a value class" {:class (.getName ^Class V)})))
  (let [observation (timing-tool/observe)]
    (spit file (pr-str {:schema-version 1
                        :timing-tool observation
                        :results (run n 5)})))
  0)

(defn- child-cmd
  "Return the command of child `fork` with result file `file`: the java binary,
  the input arguments and the class path of this JVM."
  [fork file]
  (-> [(str (io/file (System/getProperty "java.home") "bin" "java"))]
      (into (.getInputArguments (ManagementFactory/getRuntimeMXBean)))
      (into ["-cp" (System/getProperty "java.class.path")
             "clojure.main" "-m" "closed-record.bench.value-type"
             (str fork) (str file)])))

(defn- stats? [m]
  (and (map? m)
       (= #{:mean :low :high} (set (keys m)))
       (every? number? (vals m))))

(defn- result-error
  "Return an error text for the child result `r`, or nil when it is valid."
  [declared r]
  (cond
    (not (map? r)) "result is not a map"
    (not= 1 (:schema-version r)) "schema-version is not 1"
    :else
    (or (try (timing-tool/validate-observation declared (:timing-tool r)) nil
             (catch clojure.lang.ExceptionInfo e (ex-message e)))
        (let [results (:results r)]
          (when-not (and (vector? results)
                         (= (count ops) (count results))
                         (every? (fn [[e op]]
                                   (and (vector? e) (= 3 (count e))
                                        (= op (first e))
                                        (stats? (nth e 1)) (stats? (nth e 2))))
                                 (map vector results ops)))
            "results are not valid")))))

(defn- run-fork
  "Run child `n`. Return `{:result r}` or `{:error text}`."
  [declared n]
  (let [file (File/createTempFile (str "closed-record-vt-fork-" n "-") ".edn")
        _ (.delete file)]
    (try
      (let [exit (-> (ProcessBuilder. ^java.util.List (child-cmd n (str file)))
                     (.inheritIO)
                     (.start)
                     (.waitFor))]
        (cond
          (not= 0 exit) {:error (str "exit " exit)}
          (not (.exists file)) {:error (str "missing result file " file)}
          :else
          (let [r (try (edn/read-string (slurp file))
                       (catch Exception e {::read-error (ex-message e)}))]
            (if-let [e (or (::read-error r) (result-error declared r))]
              {:error e}
              {:result r}))))
      (finally (.delete file)))))

(defn- fmt [pattern & args]
  (String/format java.util.Locale/ROOT pattern (to-array args)))

(defn- summary
  "Return `{:mean :min :max}` of the fork `:mean` values."
  [stats]
  (let [means (map #(double (:mean %)) stats)]
    {:mean (/ (reduce + means) (count means))
     :min (apply min means)
     :max (apply max means)}))

(def ^:private header
  ["Operation" "V mean [min, max] ns" "P mean [min, max] ns" "V / P" "Forks"])

(defn- table [results]
  (let [rows (for [i (range (count ops))]
               (let [entries (map #(nth (:results %) i) results)
                     v (summary (map #(nth % 1) entries))
                     p (summary (map #(nth % 2) entries))
                     cell #(fmt "%.2f [%.2f, %.2f]" (:mean %) (:min %) (:max %))]
                 [(nth ops i) (cell v) (cell p)
                  (fmt "%.3f" (report/ratio v p)) (str (count results))]))
        all (cons header rows)
        widths (apply map (fn [& cells] (apply max (map count cells))) all)
        line (fn [cells]
               (str "| " (str/join " | " (map #(fmt (str "%-" %1 "s") %2) widths cells)) " |"))
        sep (str "|" (str/join "|" (map #(apply str (repeat (+ 2 %) "-")) widths)) "|")]
    (str/join "\n" (concat [(line header) sep] (map line rows)))))

(defn- parent []
  (let [declared (timing-tool/declaration (timing-tool/observe))]
    (loop [n 0 results []]
      (if (= n report/fork-count)
        (do (println (table results)) (flush) 0)
        (let [{:keys [result error]} (run-fork declared n)]
          (if error
            (binding [*out* *err*]
              (println (str "fork " n " failed: " error))
              (flush)
              2)
            (recur (inc n) (conj results result))))))))

(defn -main [& args]
  (System/exit
   (case (count args)
     0 (parent)
     2 (let [[n file] args] (child (parse-long n) file))
     (binding [*out* *err*]
       (println "usage: closed-record.bench.value-type [fork result-file]")
       64))))
