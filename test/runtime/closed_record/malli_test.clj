(ns closed-record.malli-test
  "V-07: a closed record works with malli map schemas (S-04, D-11).

  Validation and explanation treat the record as a map. A transformer
  that adds an undeclared key or removes a declared key throws. Expected
  values are the observed results of malli 0.20.3."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]))

(defclosedrecord P [a b c])

(def ^:private p (->P 1 nil 3))

(def ^:private s [:map [:a :int] [:b [:maybe :int]] [:c :int]])

(def ^:private sc (into [:map {:closed true}] (rest s)))

(defn- thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable e e)))

(defn- data-of [f] (ex-data (thrown f)))

(deftest validation-test
  (testing "open and closed schemas of the declared keys validate"
    (is (true? (m/validate s p)))
    (is (true? (m/validate sc p))))
  (testing "a schema with an undeclared key does not validate"
    (is (false? (m/validate [:map [:a :int] [:d :int]] p))))
  (testing "a closed schema with fewer keys does not validate"
    (is (false? (m/validate [:map {:closed true} [:a :int]] p)))))

(deftest explanation-test
  (testing "a valid record has no explanation"
    (is (nil? (m/explain s p))))
  (testing "a field value that is not valid"
    (is (= {:a ["should be an integer"]}
           (me/humanize (m/explain s (->P "x" nil 3))))))
  (testing "a missing schema key"
    (is (= {:d ["missing required key"]}
           (me/humanize (m/explain [:map [:a :int] [:d :int]] p))))))

(deftest nil-field-test
  (testing "a nil field is valid for :maybe"
    (is (true? (m/validate [:map [:b [:maybe :int]]] p))))
  (testing "a nil field is not valid for :int"
    (is (false? (m/validate [:map [:b :int]] p)))))

(deftest permitted-transformer-test
  (testing "string-transformer decode gives a P"
    (is (= (->P 1 nil 3)
           (m/decode s (->P "1" nil "3") mt/string-transformer))))
  (testing "default-value-transformer sets a declared nil field"
    (is (= (->P 1 5 3)
           (m/decode [:map [:b {:default 5} :int]] p
                     mt/default-value-transformer))))
  (testing "json-transformer encode and coerce give the record"
    (is (= p (m/encode s p mt/json-transformer)))
    (is (= p (m/coerce s p)))))

(def ^:private throwing-cases
  "Each case: `[label schema transformer ex-data plain-result]`.
  The transformer throws on `p` with `ex-data`; on `(into {} p)` it
  gives `plain-result`."
  [["a default for an undeclared key"
    [:map [:a :int] [:d {:default 5} :int]]
    mt/default-value-transformer
    {:type :closed-record/undeclared-key :record P :key :d}
    {:a 1 :b nil :c 3 :d 5}]
   ["strip-extra-keys-transformer removes a declared key"
    [:map [:a :int]]
    mt/strip-extra-keys-transformer
    {:type :closed-record/declared-key-dissoc :record P :key :b}
    {:a 1}]
   ["key-transformer calls empty"
    s
    (mt/key-transformer {:decode name})
    {:type :closed-record/empty :record P}
    {"a" 1 "b" nil "c" 3}]])

(deftest throwing-transformer-test
  (doseq [[label schema transformer data _] throwing-cases]
    (testing label
      (is (= data (data-of #(m/decode schema p transformer)))))))

(deftest plain-map-fix-test
  (doseq [[label schema transformer _ plain] throwing-cases]
    (testing label
      (is (= plain (m/decode schema (into {} p) transformer))))))
