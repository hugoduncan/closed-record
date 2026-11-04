(ns closed-record.field-value-test
  "Ordinary-mode run of `closed-record.checks.field-value`, and the tests
  that are not shared."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.checks.support :refer [define-checks]]
   [closed-record.core :refer [defclosedrecord]]))

(define-checks 'closed-record.checks.field-value
  {:record-opts [] :representation :identity})

(defclosedrecord FL [a ^long n])

(defrecord DFL [a ^long n])

(defn- instance-fields
  "Return `[name type modifiers]` of the non-static declared fields of `c`,
  in declaration order."
  [^Class c]
  (->> (.getDeclaredFields c)
       (remove #(java.lang.reflect.Modifier/isStatic (.getModifiers ^java.lang.reflect.Field %)))
       (mapv (fn [^java.lang.reflect.Field f] [(.getName f) (.getType f) (.getModifiers f)]))))

(deftest field-declaration-test
  (testing "V-11: same instance fields as defrecord, in name, order, type and modifiers"
    (is (= (instance-fields DFL) (instance-fields FL)))
    (is (= ["a" "n"] (mapv first (take 2 (instance-fields FL)))))
    (is (= Long/TYPE (second (second (instance-fields FL)))))))
