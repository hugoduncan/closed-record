(ns closed-record.key-error-test
  "D-21: the private throw helper that the generated assoc and without call.
  The message and the data are the D-03 contract."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]))

(defclosedrecord K [a])

(def ^:private key-error #'closed-record.core/key-error)

(deftest key-error-test
  (testing "undeclared key"
    (let [e (key-error :closed-record/undeclared-key K :z)]
      (is (= "Undeclared key :z for closed record closed_record.key_error_test.K"
             (ex-message e)))
      (is (= {:type :closed-record/undeclared-key :record K :key :z}
             (ex-data e)))))
  (testing "declared key dissoc"
    (let [e (key-error :closed-record/declared-key-dissoc K :a)]
      (is (= "Cannot dissoc declared key :a from closed record closed_record.key_error_test.K"
             (ex-message e)))
      (is (= {:type :closed-record/declared-key-dissoc :record K :key :a}
             (ex-data e)))))
  (testing "the generated methods throw the helper result"
    (let [e (try (assoc (->K 1) :z 2) (catch Exception e e))]
      (is (= (ex-message (key-error :closed-record/undeclared-key K :z))
             (ex-message e))))
    (let [e (try (dissoc (->K 1) :a) (catch Exception e e))]
      (is (= (ex-message (key-error :closed-record/declared-key-dissoc K :a))
             (ex-message e))))))
