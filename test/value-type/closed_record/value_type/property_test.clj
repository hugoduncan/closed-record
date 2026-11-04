(ns closed-record.value-type.property-test
  "Value-type run of `closed-record.checks.property` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.support :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(expectation/representation)

(define-checks 'closed-record.checks.property
  {:record-opts [:value-type true]
   :representation (expectation/representation)})
