(ns closed-record.bench.assoc-timing-cli
  "Manual partial timing run with explicit budgets."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [closed-record.bench.assoc-timing-run :as run]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.timing-tool :as tool]))

(defn -main
  "Arguments: new directory, run ID, revision, seconds per site.
  Exit 0 means partial evidence only; invalid input or failed collection exits 2."
  [& args]
  (let [exit
        (try
          (when-not (= 4 (count args))
            (throw (ex-info "Usage: assoc-timing-cli DIRECTORY RUN REVISION SECONDS" {})))
          (let [[directory run-id revision limit-text] args
                limit-s (Double/parseDouble limit-text)
                observed (runtime/observe)
                command (into (into [(str (io/file (:java-home observed) "bin" "java"))]
                                    (:flags observed))
                              (into ["-cp" (System/getProperty "java.class.path")
                                     "clojure.main" "-m" "closed-record.bench.assoc-timing-cli"]
                                    args))
                result (run/collect!
                        directory (edn/read-string (slurp (io/resource "cases.edn")))
                        {:command command
                         :timing-tool (tool/observe)
                         :machine {:os (System/getProperty "os.name")
                                   :architecture (System/getProperty "os.arch")
                                   :os-version (System/getProperty "os.version")}}
                        run-id revision limit-s)]
            (binding [*out* *err*]
              (println "Partial timing evidence:" directory "exit" (:exit result)))
            (:exit result))
          (catch clojure.lang.ExceptionInfo error
            (binding [*out* *err*] (println (.getMessage error))) 2)
          (catch NumberFormatException error
            (binding [*out* *err*] (println (.getMessage error))) 2)
          (catch java.io.IOException error
            (binding [*out* *err*] (println (.getMessage error))) 2))]
    (when-not (zero? exit) (System/exit exit))))
