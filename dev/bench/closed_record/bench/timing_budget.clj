(ns closed-record.bench.timing-budget
  "Pure per-site timing budget provenance. No measurement dependencies.")

(defn declaration
  "Return the positive finite double budget used by the timing CLI.
  This is a per-site measurement limit, not a timeout or convergence claim."
  [seconds]
  (when-not (and (number? seconds)
                 (Double/isFinite (double seconds))
                 (pos? (double seconds)))
    (throw (ex-info "Invalid timing budget" {:seconds seconds})))
  {:limit-time-s (double seconds)})

(defn validate-observation
  "Require a saved row budget to match the declared per-site limit.
  Return the unchanged row. Coverage and metric validation are separate checks."
  [declared row]
  (let [expected (declaration (:limit-time-s declared))
        observed (:timing-budget row)]
    (when-not (= expected observed)
      (throw (ex-info "Timing budget mismatch"
                      {:expected expected :observed observed})))
    row))
