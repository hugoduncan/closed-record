(ns closed-record.bench.assoc-timing-report
  "Manual read-only reporting command for saved partial timing diagnostics."
  (:require [closed-record.bench.assoc-timing-run :as run-results]))

(defn -main
  "Print a saved timing table. Exit 2 for invalid arguments or rejected evidence.
  Exit zero means the evidence can be reported, not that acceptance passed."
  [& args]
  (let [outcome (try
                  (when-not (= 1 (count args))
                    (throw (ex-info "Usage: assoc-timing-report RUN-DIRECTORY" {})))
                  {:report (run-results/render-saved (first args))}
                  (catch clojure.lang.ExceptionInfo error
                    {:error (.getMessage error)})
                  (catch java.io.IOException error
                    {:error (.getMessage error)}))]
    (if-let [message (:error outcome)]
      (do (binding [*out* *err*] (println message))
          (System/exit 2))
      (println (:report outcome)))))
