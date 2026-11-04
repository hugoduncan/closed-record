(ns closed-record.property-test
  "Ordinary-mode run of `closed-record.checks.property`."
  (:require
   [closed-record.checks.support :refer [define-checks]]))

(define-checks 'closed-record.checks.property
  {:record-opts [] :representation :identity})
