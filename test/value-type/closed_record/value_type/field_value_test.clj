(ns closed-record.value-type.field-value-test
  "Value-type run of `closed-record.checks.field-value` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.support :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(expectation/representation)

(define-checks 'closed-record.checks.field-value
  {:record-opts [:value-type true]
   :representation (expectation/representation)})
