(ns closed-record.bench.timing
  "Pure adaptation of Criterium batch estimates to raw timing outcomes."
  (:require [closed-record.bench.metrics :as metrics]))

(defn outcome
  "Return a raw time outcome from last-bench data, before per-call reduction.
  The raw value is the bootstrap mean batch estimate in nanoseconds, not a
  single observed sample. Retain its actual batch divisor without subtraction.
  Reject missing or invalid estimates; callers must report failures explicitly."
  [bench-data]
  (let [stats (get-in bench-data [:data :bootstrap-stats])
        result {:metric :time :unit :ns/op :status :measured
                :raw (get-in stats [:bootstrap :elapsed-time :mean :point-estimate])
                :operations (:batch-size stats)}]
    (metrics/normalise result)
    result))
