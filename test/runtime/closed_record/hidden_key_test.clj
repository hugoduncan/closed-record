(ns closed-record.hidden-key-test
  "Ordinary-mode run of `closed-record.checks.hidden-key`."
  (:require
   [closed-record.checks.hidden-key :refer [define-checks]]))

(define-checks {:record-opts [] :representation :identity})
