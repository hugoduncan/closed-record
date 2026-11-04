(ns closed-record.differential-test
  "Ordinary-mode run of `closed-record.checks.differential`."
  (:require
   [closed-record.checks.support :refer [define-checks]]))

(define-checks 'closed-record.checks.differential
  {:record-opts [] :representation :identity})
