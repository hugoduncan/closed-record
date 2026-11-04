(ns closed-record.value-type.rejected-key-message-test
  "Value-type run of `closed-record.checks.rejected-key-message` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.rejected-key-message :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(expectation/representation)

(define-checks {:record-opts [:value-type true]
                :representation (expectation/representation)})
