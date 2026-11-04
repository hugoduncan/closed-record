(ns closed-record.equality-test
  "Ordinary-mode run of `closed-record.checks.equality`."
  (:require
   [closed-record.checks.equality :refer [define-checks]]))

(define-checks {:record-opts [] :representation :identity})
