(ns closed-record.bench.assoc-suite-report
  "Manual read-only reporting command for combined ordinary diagnostics."
  (:require [closed-record.bench.assoc-suite :as suite]))

(defn -main
  "Print validated saved diagnostics. Exit 2 for invalid arguments or evidence.
  Exit zero means report success, not performance acceptance."
  [& args]
  (let [outcome (try
                  (when-not (= 3 (count args))
                    (throw (ex-info "Usage: assoc-suite-report TIME-DIRECTORY ALLOCATION-DIRECTORY GRAPH-DIRECTORY" {})))
                  {:report (suite/render-saved (zipmap [:time :allocation :graph] args))}
                  (catch clojure.lang.ExceptionInfo error
                    {:error (.getMessage error)})
                  (catch java.io.IOException error
                    {:error (.getMessage error)}))]
    (if-let [message (:error outcome)]
      (do (binding [*out* *err*] (println message))
          (System/exit 2))
      (println (:report outcome)))))
