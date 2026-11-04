(ns closed-record.equality-test
  "Ordinary-mode run of `closed-record.checks.equality`."
  (:require
   [closed-record.checks.support :refer [define-checks]]))

(define-checks 'closed-record.checks.equality
  {:record-opts [] :representation :identity})
