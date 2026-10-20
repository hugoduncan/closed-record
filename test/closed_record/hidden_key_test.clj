(ns closed-record.hidden-key-test
  "V-14: the extra keys of a map literal stay hidden (I-01, I-02, I-09)
  and values made from such a literal keep them (I-07, D-09).

  Each expected value comes from `plain`, the same value without extra
  keys: `__extmap` must have no effect except on `check`."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [check defclosedrecord]]))

(def ^:private test-ns 'closed-record.hidden-key-test)

(defclosedrecord E [a b])

(defn- in-test-ns
  "Call `f` with `*ns*` bound to this namespace."
  [f]
  (binding [*ns* (the-ns test-ns)] (f)))

(defn- read-literal [s] (in-test-ns #(read-string s)))

(defn- lit
  "A value with the hidden key `:z`. Read at run time: a literal in test
  source is a code constant and loses its extra keys."
  []
  (read-literal "#closed_record.hidden_key_test.E{:a 1 :b 2 :z 3}"))

(def ^:private plain (->E 1 2))

(defn- check-data
  "Return the `ex-data` of the exception that `check` of `x` throws, or nil."
  [x]
  (try (check x) nil (catch Throwable t (ex-data t))))

(defn- assert-extra-z
  "Assert that `check` of `x` throws `extra-keys` with `#{:z}`."
  [x]
  (let [d (check-data x)]
    (is (= :closed-record/extra-keys (:type d)))
    (is (= #{:z} (:keys d)))))

(defn- same
  "Assert `(= (f plain) (f v))` for each `label f` pair."
  [v & label-fs]
  (doseq [[label f] (partition 2 label-fs)]
    (testing label
      (is (= (f plain) (f v))))))

(deftest guard-test
  ;; Without this, every other test could pass with no hidden key.
  (assert-extra-z (lit)))

(deftest size-and-entries-test
  (let [v (lit)]
    (assert-extra-z v)
    (same v
          "(count x)" (fn [x] (count x))
          "(seq x)" (fn [x] (seq x))
          "(keys x)" (fn [x] (keys x))
          "(vals x)" (fn [x] (vals x))
          "(into {} x)" (fn [x] (into {} x))
          "(iterator-seq (.iterator ^Iterable x))" (fn [x] (iterator-seq (.iterator ^Iterable x)))
          "(reduce-kv (fn [acc k w] (conj acc [k w])) [] x)" (fn [x] (reduce-kv (fn [acc k w] (conj acc [k w])) [] x)))))

(deftest lookup-test
  (let [v (lit)]
    (assert-extra-z v)
    (same v
          "(get x :z :nf)" (fn [x] (get x :z :nf))
          "(:z x)" (fn [x] (:z x))
          "(contains? x :z)" (fn [x] (contains? x :z))
          "(find x :z)" (fn [x] (find x :z)))))

(deftest java-util-map-test
  (let [v (lit)]
    (assert-extra-z v)
    (same v
          "(.size ^java.util.Map x)" (fn [x] (.size ^java.util.Map x))
          "(.keySet ^java.util.Map x)" (fn [x] (.keySet ^java.util.Map x))
          "(.values ^java.util.Map x)" (fn [x] (.values ^java.util.Map x))
          "(.entrySet ^java.util.Map x)" (fn [x] (.entrySet ^java.util.Map x))
          "(.containsKey ^java.util.Map x :z)" (fn [x] (.containsKey ^java.util.Map x :z))
          "(.get ^java.util.Map x :z)" (fn [x] (.get ^java.util.Map x :z))
          "(.containsValue ^java.util.Map x 3)" (fn [x] (.containsValue ^java.util.Map x 3)))))

(deftest equality-and-hash-test
  (let [v (lit)]
    (assert-extra-z v)
    (same v
          "(= x plain)" (fn [x] (= x plain))
          "(hash x)" (fn [x] (hash x))
          "(.hashCode ^Object x)" (fn [x] (.hashCode ^Object x))
          "(.equals ^Object x {:a 1 :b 2})" (fn [x] (.equals ^Object x {:a 1 :b 2}))
          "(.equals ^Object {:a 1 :b 2 :z 3} x)" (fn [x] (.equals ^Object {:a 1 :b 2 :z 3} x)))))

(deftest print-test
  (let [v (lit)]
    (assert-extra-z v)
    (same v
          "(pr-str x)" (fn [x] (pr-str x)))))

(deftest retention-test
  ;; I-07: each case reaches a different emitter path.
  (let [v (lit)]
    (assert-extra-z v)
    (testing "assoc"
      (assert-extra-z (assoc v :a 5)))
    (testing "with-meta"
      (assert-extra-z (with-meta v {:m 1})))
    (testing "dissoc of an undeclared key returns the same value"
      (is (identical? v (dissoc v :z)))
      (assert-extra-z (dissoc v :z)))
    (testing "conj of a vector pair"
      (assert-extra-z (conj v [:a 5])))
    (testing "merge of a map"
      (assert-extra-z (merge v {:b 9})))))
