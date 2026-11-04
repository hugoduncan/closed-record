(ns closed-record.value-type.defclosedrecord-test
  "Value-type run of `closed-record.checks.defclosedrecord` (VT-V3, VT-V4)."
  (:require
   [closed-record.checks.defclosedrecord :refer [define-checks]]
   [closed-record.value-type.expectation :as expectation]))

(expectation/representation)

(define-checks {:record-opts [:value-type true]
                :representation (expectation/representation)})
