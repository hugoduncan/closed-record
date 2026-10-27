(ns closed-record.bench.allocation
  "Pure adaptation of thread allocation counter intervals to raw outcomes."
  (:require [closed-record.bench.metrics :as metrics]))

(defn outcome
  "Return unadjusted allocation bytes and the actual operation divisor.
  Inputs are before/after readings from the same validated thread counter.
  Reject unavailable, decreasing, or non-integral readings. Counter support,
  thread identity, setup exclusion, and result escape remain caller obligations.
  Zero-operation controls are evidence, not bytes/op outcomes."
  [before after operations]
  (when-not (and (integer? before) (integer? after)
                 (<= 0 before after Long/MAX_VALUE))
    (throw (ex-info "Invalid allocation counter interval"
                    {:before before :after after})))
  (let [result {:metric :allocation :unit :bytes/op :status :measured
                :raw (- after before) :operations operations
                :counter-before before :counter-after after}]
    (metrics/normalise result)
    result))
