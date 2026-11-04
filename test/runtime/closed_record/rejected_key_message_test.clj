(ns closed-record.rejected-key-message-test
  "Ordinary-mode run of `closed-record.checks.rejected-key-message`."
  (:require
   [closed-record.checks.rejected-key-message :refer [define-checks]]))

(define-checks {:record-opts [] :representation :identity})
