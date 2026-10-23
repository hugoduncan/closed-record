(ns closed-record.rejected-key-message-test
  "The ex-info message of a rejected assoc or dissoc. D-03 makes the ex-data
  public, not the message; this test keeps the message text unchanged
  (task 012, Scope 2)."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]))

(defclosedrecord K [a])

(deftest rejected-key-message-test
  (testing "assoc of an undeclared key"
    (is (= "Undeclared key :z for closed record closed_record.rejected_key_message_test.K"
           (ex-message (try (assoc (->K 1) :z 2) (catch Exception e e))))))
  (testing "dissoc of a declared key"
    (is (= "Cannot dissoc declared key :a from closed record closed_record.rejected_key_message_test.K"
           (ex-message (try (dissoc (->K 1) :a) (catch Exception e e)))))))
