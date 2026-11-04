(ns closed-record.value-type.literal-test
  "Value-type run of `closed-record.checks.literal` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.literal :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(expectation/representation)

(define-checks {:record-opts [:value-type true]
                :representation (expectation/representation)})
