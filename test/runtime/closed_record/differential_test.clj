(ns closed-record.differential-test
  "Ordinary-mode run of `closed-record.checks.differential`."
  (:require
   [closed-record.checks.differential :refer [define-checks]]))

(define-checks {:record-opts [] :representation :identity})
