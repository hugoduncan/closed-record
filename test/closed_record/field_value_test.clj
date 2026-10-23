(ns closed-record.field-value-test
  "V-06 (keyword call sites), V-08 (nil values) and V-09 (type hints).
  V-08 and V-09 are differential: each case runs with the closed type
  and with the `defrecord`, and the outcomes must agree.
  V-11 (field access, S-03): `field-declaration-test` checks that the
  closed type declares the same instance fields as the `defrecord` (D-01),
  so `(.a x)` compiles to the same `getfield`."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]))

(def ^:private test-ns 'closed-record.field-value-test)

(defclosedrecord C [a ^long n ^double d ^int i ^boolean b ^String s])

(defrecord DC [a ^long n ^double d ^int i ^boolean b ^String s])

(defclosedrecord N [a b])

(defrecord DN [a b])

(defclosedrecord N2 [a b])

(def ^:private c-factories {:new ->C :map map->C})
(def ^:private dc-factories {:new ->DC :map map->DC})
(def ^:private n-factories
  {:new ->N :fields (fn [^N x] [(.-a x) (.-b x)])})
(def ^:private dn-factories
  {:new ->DN :fields (fn [^DN x] [(.-a x) (.-b x)])})

(defn- outcome
  "Return `[:ok v]` or `[:ex class]` for a call of `f`. A record `v` is
  `(into {} v)` with each value paired with its class; another `v` is
  paired with its class."
  [f]
  (try
    (let [x (f)]
      [:ok (if (record? x)
             (update-vals (into {} x) (juxt identity class))
             [x (class x)])])
    (catch Throwable t [:ex (class t)])))

(defn- outcomes
  "The outcomes `[closed record]` of `f` of the closed factories and `f`
  of the `defrecord` factories (design Scope 2)."
  [closed record f]
  [(outcome #(f closed)) (outcome #(f record))])

(defmacro ^:private is-diff
  "Assert that `f` gives the same outcome with the `closed` and the
  `record` factories. A failure reports both outcomes."
  [closed record f]
  `(let [[c# r#] (outcomes ~closed ~record ~f)]
     (is (= c# r#))))

(defmacro ^:private is-c-diff [f] `(is-diff c-factories dc-factories ~f))
(defmacro ^:private is-n-diff [f] `(is-diff n-factories dn-factories ~f))

(defn- c-map
  "A `map->Name` input: all keys valid, `:i` an `Integer`, with `kvs`
  merged in."
  [& {:as kvs}]
  (merge {:a 1 :n 2 :d 3.0 :i (int 4) :b true :s "s"} kvs))

(defn- c-new
  "A value from `(:new fs)` with the `c-map` input as positional
  arguments."
  [fs & {:as kvs}]
  (apply (:new fs) (map (c-map kvs) [:a :n :d :i :b :s])))

(deftest positional-factory-hint-test
  ;; V-09 `->Name` rows.
  (testing "^long ← 2.7 casts" (is-c-diff #(c-new % :n 2.7)))
  (testing "^double ← 3 casts" (is-c-diff #(c-new % :d 3)))
  (testing "^long ← \"x\" throws" (is-c-diff #(c-new % :n "x")))
  (testing "^long ← nil throws" (is-c-diff #(c-new % :n nil))))

(deftest assoc-hint-test
  ;; V-09 `assoc` rows.
  (testing "^long ← 9.9 casts" (is-c-diff #(assoc (c-new %) :n 9.9)))
  (testing "^long ← \"x\" throws" (is-c-diff #(assoc (c-new %) :n "x")))
  (testing "^long ← nil throws" (is-c-diff #(assoc (c-new %) :n nil)))
  (testing "^boolean ← nil throws" (is-c-diff #(assoc (c-new %) :b nil)))
  (testing "^int ← 5000000000 throws"
    (is-c-diff #(assoc (c-new %) :i 5000000000)))
  (testing "^String ← 5 is accepted" (is-c-diff #(assoc (c-new %) :s 5))))

(deftest map-factory-hint-test
  ;; V-09 `map->Name` rows (`create` path).
  (testing "^long ← 2.5 throws" (is-c-diff #((:map %) (c-map :n 2.5))))
  (testing "^long ← \"x\" throws" (is-c-diff #((:map %) (c-map :n "x"))))
  (testing "^boolean missing throws (D-19)"
    (is-c-diff #((:map %) (dissoc (c-map) :b))))
  (testing "^int ← 1 (a Long) throws" (is-c-diff #((:map %) (c-map :i 1)))))

(deftest vals-hint-test
  ;; V-09 `vals` row. The Long inputs for ^double and ^int are cast.
  (is-c-diff #(mapv class (vals (c-new % :d 3 :i 4)))))

(deftest keyword-call-hint-test
  ;; V-09 keyword call row. The site is local to this test.
  (let [site (fn [x] (:n x))]
    (is-c-diff #(site (c-new % :n 2.7)))))

(defn- nil-value [fs] ((:new fs) nil nil))

(deftest nil-lookup-test
  ;; V-08 lookup.
  (testing "get with not-found" (is-n-diff #(get (nil-value %) :a ::nf)))
  (testing "keyword call"
    (let [site (fn [x] (:a x))]
      (is-n-diff #(let [x (nil-value %)] [(site x) (site x)]))))
  (testing ".field"
    (is-n-diff #((:fields %) (nil-value %)))))

(deftest nil-key-set-test
  ;; V-08 `contains?` and `count`.
  (testing "contains?" (is-n-diff #(contains? (nil-value %) :a)))
  (testing "count" (is-n-diff #(count (nil-value %)))))

(deftest nil-equality-test
  ;; V-08 equality and hashing. No cross-type `hash` (D-07).
  (testing "=" (is-n-diff #(= (nil-value %) (nil-value %))))
  (testing "hash"
    (is-n-diff #(= (hash (nil-value %)) (hash (nil-value %)))))
  (testing ".hashCode"
    (is-n-diff #(= (.hashCode ^Object (nil-value %))
                   (.hashCode ^Object {:a nil :b nil}))))
  (testing ".equals"
    (is-n-diff #(.equals ^Object (nil-value %) {:a nil :b nil}))
    (is-n-diff #(.equals ^Object {:a nil :b nil} (nil-value %))))
  (testing "nil ≠ false"
    (is-n-diff #(= (nil-value %) ((:new %) nil false)))))

(defn- in-test-ns
  "Call `f` with `*ns*` bound to this namespace."
  [f]
  (binding [*ns* (the-ns test-ns)] (f)))

(deftest nil-print-read-test
  ;; V-08 print and read.
  (is-n-diff #(let [x (nil-value %)]
                (= x (in-test-ns (fn [] (read-string (pr-str x))))))))

(deftest call-site-boxed-value-test
  ;; V-06: a primitive field gives the boxed value. The site is local to this test.
  (let [site (fn [x] (:n x))
        v    (site (c-new c-factories :n (int 2)))]
    (is (= 2 v))
    (is (= Long (class v)))))

(deftest call-site-type-change-test
  ;; V-06: one site sees A, then B, then A. The site is local to this test, so
  ;; A, B, A is the first sequence that it sees.
  (let [site (fn [x] (:a x))]
    (is (= [1 2 3] [(site (->N 1 nil)) (site (->N2 2 nil)) (site (->N 3 nil))]))))

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
