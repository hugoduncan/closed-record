(ns closed-record.property-test
  "Ordinary-mode run of `closed-record.checks.property`."
  (:require
   [closed-record.checks.property :refer [define-checks]]))

(define-checks {:record-opts [] :representation :identity})
