(ns closed-record.value-type.hidden-key-test
  "Value-type run of `closed-record.checks.hidden-key` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.hidden-key :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(expectation/representation)

(define-checks {:record-opts [:value-type true]
                :representation (expectation/representation)})
