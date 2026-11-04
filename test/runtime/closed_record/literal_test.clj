(ns closed-record.literal-test
  "Ordinary-mode run of `closed-record.checks.literal`."
  (:require
   [closed-record.checks.literal :refer [define-checks]]))

(define-checks {:record-opts [] :representation :identity})
