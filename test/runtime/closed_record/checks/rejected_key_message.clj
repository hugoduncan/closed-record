(ns closed-record.checks.rejected-key-message
  "The ex-info message of a rejected assoc or dissoc. D-03 makes the ex-data
  public, not the message; this test keeps the message text unchanged
  (task 012, Scope 2)."
  {:clj-kondo/config '{:linters {:unused-namespace {:level :off}
                                 :unused-referred-var {:level :off}}}}
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.checks.support :as support]
   [closed-record.core :refer [defclosedrecord]]))

(def ^:private source-ns *ns*)

(def ^:private body
  '[(defclosedrecord K [a])

    (deftest rejected-key-message-test
      (testing "assoc of an undeclared key"
        (is (= "Undeclared key :z for closed record $P.K"
               (ex-message (try (assoc (->K 1) :z 2) (catch Exception e e))))))
      (testing "dissoc of a declared key"
        (is (= "Cannot dissoc declared key :a from closed record $P.K"
               (ex-message (try (dissoc (->K 1) :a) (catch Exception e e)))))))])

(defmacro define-checks
  "Define the shared checks in the calling namespace. `opts`:
  `{:record-opts [k v …] :representation form}`."
  [opts]
  (support/expand source-ns opts body))
