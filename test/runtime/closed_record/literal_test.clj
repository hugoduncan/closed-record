(ns closed-record.literal-test
  "Ordinary-mode run of `closed-record.checks.literal`."
  (:require
   [closed-record.checks.support :refer [define-checks]]))

(define-checks 'closed-record.checks.literal
  {:record-opts [] :representation :identity})
