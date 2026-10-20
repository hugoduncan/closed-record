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

(defn- thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable e e)))

(defn- data-of [f] (ex-data (thrown f)))

(defn- assert-extra-z
  "Assert that `check` of `x` throws `extra-keys` with `#{:z}`."
  [x]
  (is (= {:type :closed-record/extra-keys :record E :keys #{:z}}
         (data-of #(check x)))))

(defn- same
  "Read `x` from `lit`, assert that it has the hidden key `:z` (guard),
  then assert `(= (f plain) (f x))` for each `[label f]` pair."
  [pairs]
  (let [x (lit)]
    (assert-extra-z x)
    (doseq [[label f] pairs]
      (testing label
        (is (= (f plain) (f x)))))))

(deftest size-and-entries-test
  (same
   [["count" count]
    ["seq" seq]
    ["keys" keys]
    ["vals" vals]
    ["(into {} x)" #(into {} %)]
    ["iterator-seq of .iterator" #(iterator-seq (.iterator ^Iterable %))]
    ["reduce-kv" #(reduce-kv (fn [acc k v] (conj acc [k v])) [] %)]]))

(deftest lookup-test
  (same
   [["(get x :z :nf)" #(get % :z :nf)]
    ["(:z x)" :z]
    ["contains?" #(contains? % :z)]
    ["find" #(find % :z)]]))

(deftest java-util-map-test
  (same
   [[".size" #(.size ^java.util.Map %)]
    [".keySet" #(.keySet ^java.util.Map %)]
    [".values" #(.values ^java.util.Map %)]
    [".entrySet" #(.entrySet ^java.util.Map %)]
    [".containsKey :z" #(.containsKey ^java.util.Map % :z)]
    [".get :z" #(.get ^java.util.Map % :z)]
    [".containsValue 3" #(.containsValue ^java.util.Map % 3)]]))

(deftest equality-and-hash-test
  (same
   [["(= x plain)" #(= % plain)]
    ["hash" hash]
    [".hashCode" #(.hashCode ^Object %)]
    ["(.equals x {:a 1 :b 2})" #(.equals ^Object % {:a 1 :b 2})]
    ["(.equals {:a 1 :b 2 :z 3} x)" #(.equals ^Object {:a 1 :b 2 :z 3} %)]]))

(deftest print-test
  (same [["pr-str" pr-str]]))

(deftest retention-test
  ;; I-07: each case reaches a different emitter path.
  (let [x (lit)]
    (assert-extra-z x)
    (testing "assoc"
      (assert-extra-z (assoc x :a 5)))
    (testing "with-meta"
      (assert-extra-z (with-meta x {:m 1})))
    (testing "dissoc of an undeclared key returns the same value"
      (is (identical? x (dissoc x :z)))
      (assert-extra-z (dissoc x :z)))
    (testing "conj of a vector pair"
      (assert-extra-z (conj x [:a 5])))
    (testing "merge of a map"
      (assert-extra-z (merge x {:b 9})))))
