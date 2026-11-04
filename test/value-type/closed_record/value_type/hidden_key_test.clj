(ns closed-record.value-type.hidden-key-test
  "Value-type run of `closed-record.checks.hidden-key` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.support :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(define-checks 'closed-record.checks.hidden-key
  {:record-opts [:value-type true]
   :representation (expectation/representation)})
