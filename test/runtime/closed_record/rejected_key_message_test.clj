(ns closed-record.rejected-key-message-test
  "Ordinary-mode run of `closed-record.checks.rejected-key-message`."
  (:require
   [closed-record.checks.support :refer [define-checks]]))

(define-checks 'closed-record.checks.rejected-key-message
  {:record-opts [] :representation :identity})
