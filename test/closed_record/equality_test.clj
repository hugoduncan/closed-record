(ns closed-record.equality-test
  "V-12: equality and hash agree with `defrecord` (I-05, D-07). Scope 3, 4
  and 5 cases are differential: each case runs with the closed factory and
  with the `defrecord` factory, and the result vectors must be `=`."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]))

(defclosedrecord E [a b])

(defrecord DE [a b])

(defmacro ^:private is-diff
  "Assert that `f` returns the same vector of booleans with `->E` and with
  `->DE`. A failure reports both vectors."
  [f]
  `(let [f# ~f]
     (is (= (f# ->E) (f# ->DE)))))

(defn- four
  "The four results of the first Context table for `x` and `y`. Both values
  must exist before this call, so that no hash is cached before `with-meta`."
  [x y]
  [(= x y)
   (= (hash x) (hash y))
   (.equals ^Object x y)
   (= (.hashCode ^Object x) (.hashCode ^Object y))])

(defn- pair
  "A function of a factory: `four` of `(f a1 2)` and `(f a2 2)`."
  [a1 a2]
  (fn [f] (four (f a1 2) (f a2 2))))

(deftest field-value-pairs-test
  ;; Scope 3: `=` uses `Util.equiv`, `.equals` uses `Util.equals`.
  (testing "1 and 1N"
    (is-diff (pair 1 1N)))
  (testing "(int 1) and 1"
    (is-diff (pair (int 1) 1)))
  (testing "0.0 and -0.0"
    (is-diff (pair 0.0 -0.0)))
  (testing "1 and 1.0"
    (is-diff (pair 1 1.0))))

(def ^:private m {:a 1 :b 2})

(defn- with-r
  "A function of a factory: `g` of `r = (f 1 2)`."
  [g]
  (fn [f] (g (f 1 2))))

(deftest plain-map-test
  ;; Scope 4. `r` is a record with `{:a 1 :b 2}`, `m` the plain map.
  (testing "="
    (is-diff (with-r (fn [r] [(= r m) (= m r)]))))
  (testing ".equals"
    (is-diff (with-r (fn [^Object r] [(.equals r m) (.equals ^Object m r)]))))
  (testing "java.util.HashMap .equals"
    (is-diff (with-r (fn [^Object r]
                       (let [h (java.util.HashMap. ^java.util.Map m)]
                         [(.equals r h) (.equals h r)])))))
  (testing ".hashCode"
    (is-diff (with-r (fn [^Object r]
                       [(= (.hashCode r) (.hashCode ^Object m))]))))
  (testing "hash"
    (is-diff (with-r (fn [r] [(= (hash r) (hash m))]))))
  (testing "contains? of a set"
    (is-diff (with-r (fn [r] [(contains? #{m} r)]))))
  (testing "java.util.HashSet .contains"
    (is-diff (with-r (fn [r]
                       [(.contains (java.util.HashSet. ^java.util.Collection [m])
                                   r)])))))

(deftest metadata-test
  ;; Scope 5. Both values exist before a hash call.
  (testing "r and (with-meta r {:m 1})"
    (is-diff (with-r (fn [r] (four r (with-meta r {:m 1}))))))
  (testing "different metadata, separate factory calls"
    (is-diff (fn [f] (four (with-meta (f 1 2) {:m 1})
                           (with-meta (f 1 2) {:m 2}))))))

(deftest cross-type-test
  ;; Scope 6. Closed value first only: the reverse calls run only
  ;; `defrecord` code.
  (let [e (->E 1 2)
        d (->DE 1 2)]
    (is (not (= e d)))
    (is (.equals ^Object e d))
    (is (= (.hashCode ^Object e) (.hashCode ^Object d)))))
