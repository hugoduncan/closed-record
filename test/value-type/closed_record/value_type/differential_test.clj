(ns closed-record.value-type.differential-test
  "Value-type run of `closed-record.checks.differential` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.support :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(define-checks 'closed-record.checks.differential
  {:record-opts [:value-type true]
   :representation (expectation/representation)})
