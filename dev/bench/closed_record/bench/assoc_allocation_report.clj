(ns closed-record.bench.assoc-allocation-report
  "Manual read-only reporting command for saved partial allocation diagnostics."
  (:require [closed-record.bench.assoc-allocation-run :as run-results]))

(defn -main
  "Print a saved allocation control table. Exit 2 for invalid arguments or rejected evidence.
  Exit zero means the evidence can be reported, not that acceptance passed."
  [& args]
  (let [outcome (try
                  (when-not (= 1 (count args))
                    (throw (ex-info "Usage: assoc-allocation-report RUN-DIRECTORY" {})))
                  {:report (run-results/render-saved (first args))}
                  (catch clojure.lang.ExceptionInfo error
                    {:error (.getMessage error)})
                  (catch java.io.IOException error
                    {:error (.getMessage error)}))]
    (if-let [message (:error outcome)]
      (do (binding [*out* *err*] (println message))
          (System/exit 2))
      (println (:report outcome)))))
