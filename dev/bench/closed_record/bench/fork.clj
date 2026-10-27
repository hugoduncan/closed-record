(ns closed-record.bench.fork
  "Fork layer of the benchmarks (V-11, S-03): the run order of a fork, the
  child command and the parent, which starts the forks, reads their result
  files and prints the table. No criterium dependency, so the tests can load
  it."
  (:require [clojure.edn :as edn] [clojure.java.io :as io] [clojure.string :as str] [closed-record.bench.evidence :as evidence] [closed-record.bench.report :as report] [closed-record.bench.legacy-transport :as legacy-transport] [closed-record.bench.timing-tool :as timing-tool])
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
  [child-cmd declared-tool fork]
  (let [file (File/createTempFile (str "closed-record-fork-" fork "-") ".edn")
        exit (-> (ProcessBuilder. ^java.util.List (child-cmd fork (str file)))
                 (.inheritIO)
                 (.start)
                 (.waitFor))]
    (cond
      (not= 0 exit)        {:error (str "exit " exit)}
      (not (.exists file)) {:error (str "missing result file " file)}
      :else                (legacy-transport/decode (slurp file) declared-tool))))

(defn parent
  "Run the forks with `child-cmd` (`[fork file]` → command vector), print the
  table or the fork error, and return the exit code 0, 1 or 2.
  Require a valid declared collector before launch and reconcile every child."
  [child-cmd declared-tool]
  (timing-tool/declaration declared-tool)
  (loop [fork 0 results []]
    (if (= fork report/fork-count)
      (let [stats (report/fork-stats results)]
        (println (report/table (map #(apply report/row %) stats)))
        (flush)
        (report/exit-code stats))
      (let [{:keys [result error]} (run-fork child-cmd declared-tool fork)]
        (if error
          (binding [*out* *err*]
            (println (str "fork " fork " failed: " error))
            (flush)
            2)
          (recur (inc fork) (conj results result)))))))

(defn- start-process
  [builder child workspace]
  (try
    {:process (.start ^ProcessBuilder builder)}
    (catch java.io.IOException error
      (let [outcome {:child child
                     :failure {:stage :launch
                               :reason (.getMessage error)
                               :exception-class (.getName (class error))}}]
        (spit (io/file workspace "outcome.edn") (pr-str outcome) :encoding "UTF-8")
        {:outcome outcome}))))

(defn- validate-command
  [command]
  (when-not (and (vector? command) (seq command)
                 (every? #(and (string? %) (not (str/includes? % "\u0000"))) command)
                 (not (str/blank? (first command))))
    (throw (ex-info "Invalid child command" {:command command})))
  command)

(defn capture-process!
  "Run a planned child and capture its result through the evidence boundary.
  child-cmd receives the saved association and an absolute, absent result path.
  An exclusive process directory retains command, streams and result bytes even
  if capture throws. Launch IO failures return and persist :failure with :stage
  :launch, without an exit or validation result. Result-read IO failures retain
  the exit with :stage :result-read and no validation result.
  Invalid command vectors throw before creating the process directory. Empty
  arguments are valid; blank executables and NUL characters are not.
  Completed nonzero exits return normal outcomes;
  callers must check exit, validation, and runtime-validation. This does not
  aggregate results."
  [directory child-index child-cmd]
  (let [manifest (edn/read-string (slurp (io/file directory "manifest.edn")))
        children (:children manifest)]
    (when-not (and (integer? child-index) (<= 0 child-index)
                   (< child-index (count children)))
      (throw (ex-info "Invalid child index" {:child-index child-index})))
    (let [workspace (io/file directory (str "process-" child-index))
          result-file (io/file workspace "result.edn")
          command (validate-command
                   (child-cmd (nth children child-index) (.getAbsolutePath result-file)))]
      (java.nio.file.Files/createDirectory
       (.toPath workspace) (make-array java.nio.file.attribute.FileAttribute 0))
      (let [stdout (io/file workspace "stdout.txt")
            stderr (io/file workspace "stderr.txt")]
        (spit (io/file workspace "command.edn") (pr-str command) :encoding "UTF-8")
        (let [{:keys [process outcome]}
              (start-process (-> (ProcessBuilder. ^java.util.List command)
                                 (.redirectOutput stdout)
                                 (.redirectError stderr))
                             (nth children child-index) workspace)]
          (if outcome
            outcome
            (let [exit (.waitFor ^Process process)]
              (spit (io/file workspace "exit.edn") (pr-str exit) :encoding "UTF-8")
              (let [result (try
                             {:raw-result (when (.exists result-file)
                                            (slurp result-file :encoding "UTF-8"))}
                             (catch java.io.IOException error
                               {:failure {:stage :result-read
                                          :reason (.getMessage error)
                                          :exception-class (.getName (class error))}}))]
                (if (:failure result)
                  (let [failed (assoc result :child (nth children child-index) :exit exit)]
                    (spit (io/file workspace "outcome.edn") (pr-str failed) :encoding "UTF-8")
                    failed)
                  (evidence/capture-child!
                   directory child-index
                   (assoc result :exit exit
                          :out (slurp stdout :encoding "UTF-8")
                          :err (slurp stderr :encoding "UTF-8"))))))))))))
