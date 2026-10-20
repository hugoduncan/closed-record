(ns closed-record.equality-test
  "V-12: equality and hash agree with `defrecord` (I-05, D-07). The field
  value pair, plain map and metadata cases are differential: each case runs
  with the closed factory and with the `defrecord` factory, and the result
  maps must be `=`. Each map key names an operation, so a failure shows the
  operation that differs."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]))

(defclosedrecord E [a b])

(defrecord DE [a b])

(defmacro ^:private is-diff
  "Assert that `f` returns the same map of booleans with `->E` and with
  `->DE`. A failure reports both maps as `closed` and `record`."
  [f]
  `(let [f# ~f
         ~'closed (f# ->E)
         ~'record (f# ->DE)]
     (is (= ~'closed ~'record))))

(defn- equality-results
  "`=`, `hash` equality, `.equals` and `.hashCode` equality of `x` and `y`."
  [x y]
  {:= (= x y)
   :hash= (= (hash x) (hash y))
   :equals (.equals ^Object x y)
   :hashCode= (= (.hashCode ^Object x) (.hashCode ^Object y))})

(defn- pair
  "A function of a factory: `equality-results` of `(f a1 2)` and `(f a2 2)`."
  [a1 a2]
  (fn [f] (equality-results (f a1 2) (f a2 2))))

(deftest field-value-pairs-test
  ;; Field value pairs: `=` uses `Util.equiv`, `.equals` uses `Util.equals`.
  (testing "1 and 1N"
    (is-diff (pair 1 1N)))
  (testing "(int 1) and 1"
    (is-diff (pair (int 1) 1)))
  (testing "0.0 and -0.0"
    (is-diff (pair 0.0 -0.0)))
  (testing "1 and 1.0"
    (is-diff (pair 1 1.0))))

(def ^:private plain-map {:a 1 :b 2})

(defn- with-r
  "A function of a factory: `g` of `r = (f 1 2)`."
  [g]
  (fn [f] (g (f 1 2))))

(deftest plain-map-test
  ;; Plain map: `r` is a record with `{:a 1 :b 2}`, `plain-map` the plain map.
  (testing "="
    (is-diff (with-r (fn [r]
                       {:r-first (= r plain-map) :map-first (= plain-map r)}))))
  (testing ".equals"
    (is-diff (with-r (fn [^Object r]
                       {:r-first (.equals r plain-map)
                        :map-first (.equals ^Object plain-map r)}))))
  (testing "java.util.HashMap .equals"
    (is-diff (with-r (fn [^Object r]
                       (let [h (java.util.HashMap. ^java.util.Map plain-map)]
                         {:r-first (.equals r h) :map-first (.equals h r)})))))
  (testing ".hashCode"
    (is-diff (with-r (fn [^Object r]
                       {:hashCode= (= (.hashCode r) (.hashCode ^Object plain-map))}))))
  (testing "hash"
    (is-diff (with-r (fn [r] {:hash= (= (hash r) (hash plain-map))}))))
  (testing "contains? of a set"
    (is-diff (with-r (fn [r] {:contains? (contains? #{plain-map} r)}))))
  (testing "java.util.HashSet .contains"
    (is-diff (with-r (fn [r]
                       {:contains (.contains
                                   (java.util.HashSet. ^java.util.Collection [plain-map])
                                   r)})))))

(deftest metadata-test
  ;; Metadata: both values exist before a hash call.
  (testing "r and (with-meta r {:m 1})"
    (is-diff (with-r (fn [r] (equality-results r (with-meta r {:m 1}))))))
  (testing "different metadata, separate factory calls"
    (is-diff (fn [f] (equality-results (with-meta (f 1 2) {:m 1})
                                       (with-meta (f 1 2) {:m 2}))))))

(deftest cross-type-test
  ;; Cross type: closed value first only: the reverse calls run only
  ;; `defrecord` code.
  (let [e (->E 1 2)
        d (->DE 1 2)]
    (is (not (= e d)))
    (is (.equals ^Object e d))
    (is (= (.hashCode ^Object e) (.hashCode ^Object d)))))
