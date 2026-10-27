(ns closed-record.bench.assoc-allocation-cli
  "Manual partial allocation run with explicit budgets."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [closed-record.bench.assoc-allocation-run :as run]
            [closed-record.bench.runtime :as runtime]))

(defn -main
  "Arguments: new directory, run ID, revision, operations, warmup batches.
  Exit 0 means partial evidence only; invalid input or failed collection exits 2."
  [& args]
  (let [exit
        (try
          (when-not (and (= 5 (count args))
                         (every? #(boolean (re-matches #"[1-9][0-9]*" %))
                                 (subvec (vec args) 3)))
            (throw (ex-info "Usage: assoc-allocation-cli DIRECTORY RUN REVISION OPERATIONS WARMUP" {})))
          (let [[directory run-id revision operations-text warmup-text] args
                operations (Integer/parseInt operations-text)
                warmup (Integer/parseInt warmup-text)
                observed (runtime/observe)
                command (into (into [(str (io/file (:java-home observed) "bin" "java"))]
                                    (:flags observed))
                              (into ["-cp" (System/getProperty "java.class.path")
                                     "clojure.main" "-m" "closed-record.bench.assoc-allocation-cli"]
                                    args))
                result (run/collect!
                        directory (edn/read-string (slurp (io/resource "cases.edn")))
                        {:command command
                         :machine {:os (System/getProperty "os.name")
                                   :architecture (System/getProperty "os.arch")
                                   :os-version (System/getProperty "os.version")}}
                        run-id revision operations warmup)]
            (binding [*out* *err*]
              (println "Partial allocation evidence:" directory "exit" (:exit result)))
            (:exit result))
          (catch clojure.lang.ExceptionInfo error
            (binding [*out* *err*] (println (.getMessage error))) 2)
          (catch NumberFormatException error
            (binding [*out* *err*] (println (.getMessage error))) 2)
          (catch java.io.IOException error
            (binding [*out* *err*] (println (.getMessage error))) 2))]
    (when-not (zero? exit) (System/exit exit))))
