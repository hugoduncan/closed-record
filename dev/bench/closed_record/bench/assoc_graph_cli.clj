(ns closed-record.bench.assoc-graph-cli
  "Manual partial graph run with explicit prepared-build input."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [closed-record.bench.assoc-graph-run :as run]
            [closed-record.bench.runtime :as runtime]))

(defn -main
  "Arguments: new directory, run ID, revision, prepared build directory.
  Exit 0 means partial evidence only; invalid input or failed collection exits 2."
  [& args]
  (let [exit
        (try
          (when-not (= 4 (count args))
            (throw (ex-info "Usage: assoc-graph-cli DIRECTORY RUN REVISION BUILD_DIRECTORY" {})))
          (let [[directory run-id revision build-directory] args
                observed (runtime/observe)
                command (into (into [(str (io/file (:java-home observed) "bin" "java"))]
                                    (:flags observed))
                              (into ["-cp" (System/getProperty "java.class.path")
                                     "clojure.main" "-m" "closed-record.bench.assoc-graph-cli"]
                                    args))
                result (run/collect!
                        directory (edn/read-string (slurp (io/resource "cases.edn")))
                        {:command command
                         :machine {:os (System/getProperty "os.name")
                                   :architecture (System/getProperty "os.arch")
                                   :os-version (System/getProperty "os.version")}}
                        run-id revision build-directory)]
            (binding [*out* *err*]
              (println "Partial graph evidence:" directory "exit" (:exit result)))
            (:exit result))
          (catch clojure.lang.ExceptionInfo error
            (binding [*out* *err*] (println (.getMessage error))) 2)
          (catch java.io.IOException error
            (binding [*out* *err*] (println (.getMessage error))) 2))]
    (when-not (zero? exit) (System/exit exit))))
