(ns closed-record.hidden-key-test
  "Ordinary-mode run of `closed-record.checks.hidden-key`."
  (:require
   [closed-record.checks.support :refer [define-checks]]))

(define-checks 'closed-record.checks.hidden-key
  {:record-opts [] :representation :identity})
