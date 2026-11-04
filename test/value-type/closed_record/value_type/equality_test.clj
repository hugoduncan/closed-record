(ns closed-record.value-type.equality-test
  "Value-type run of `closed-record.checks.equality` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.support :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(expectation/representation)

(define-checks 'closed-record.checks.equality
  {:record-opts [:value-type true]
   :representation (expectation/representation)})
