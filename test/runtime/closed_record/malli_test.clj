(ns closed-record.malli-test
  "Ordinary-mode run of `closed-record.checks.malli`."
  (:require
   [closed-record.checks.support :refer [define-checks]]))

(define-checks 'closed-record.checks.malli
  {:record-opts [] :representation :identity})
