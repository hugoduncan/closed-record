(ns closed-record.field-value-test
  "V-06 (keyword call sites), V-08 (nil values) and V-09 (type hints).
  V-08 and V-09 are differential: each case runs with the closed type
  and with the `defrecord`, and the outcomes must agree."
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
(def ^:private n-factories {:new ->N :map map->N})
(def ^:private dn-factories {:new ->DN :map map->DN})

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

(defn- same-outcome?
  "True if `f` of the closed factories and `f` of the `defrecord`
  factories give the same outcome (design Scope 2)."
  [closed record f]
  (let [a (outcome #(f closed))
        b (outcome #(f record))]
    (or (= a b)
        (do (println "closed:" (pr-str a) "defrecord:" (pr-str b)) false))))

(defn- c-diff [f] (same-outcome? c-factories dc-factories f))
(defn- n-diff [f] (same-outcome? n-factories dn-factories f))

(defn- c-new
  "A valid value from `(:new fs)`, with `kvs` assoc-ed onto the
  positional arguments by field key."
  [fs & {:as kvs}]
  (let [m (merge {:a 1 :n 2 :d 3.0 :i (int 4) :b true :s "s"} kvs)]
    ((:new fs) (:a m) (:n m) (:d m) (:i m) (:b m) (:s m))))

(defn- c-map
  "A `map->Name` input: all keys valid, `:i` an `Integer`."
  [& {:as kvs}]
  (merge {:a 1 :n 2 :d 3.0 :i (int 4) :b true :s "s"} kvs))

(deftest smoke-test
  (is (c-diff #(c-new %)))
  (is (= [:ok [5 Long]] (outcome (constantly 5)))))

(deftest positional-factory-hint-test
  ;; V-09 `->Name` rows.
  (testing "^long ← 2.7 casts" (is (c-diff #(c-new % :n 2.7))))
  (testing "^double ← 3 casts" (is (c-diff #(c-new % :d 3))))
  (testing "^long ← \"x\" throws" (is (c-diff #(c-new % :n "x"))))
  (testing "^long ← nil throws" (is (c-diff #(c-new % :n nil)))))

(deftest assoc-hint-test
  ;; V-09 `assoc` rows.
  (testing "^long ← 9.9 casts" (is (c-diff #(assoc (c-new %) :n 9.9))))
  (testing "^long ← \"x\" throws" (is (c-diff #(assoc (c-new %) :n "x"))))
  (testing "^long ← nil throws" (is (c-diff #(assoc (c-new %) :n nil))))
  (testing "^boolean ← nil throws" (is (c-diff #(assoc (c-new %) :b nil))))
  (testing "^int ← 5000000000 throws"
    (is (c-diff #(assoc (c-new %) :i 5000000000))))
  (testing "^String ← 5 is accepted" (is (c-diff #(assoc (c-new %) :s 5)))))

(deftest map-factory-hint-test
  ;; V-09 `map->Name` rows (`create` path).
  (testing "^long ← 2.5 throws" (is (c-diff #((:map %) (c-map :n 2.5)))))
  (testing "^long ← \"x\" throws" (is (c-diff #((:map %) (c-map :n "x")))))
  (testing "^boolean missing throws (D-19)"
    (is (c-diff #((:map %) (dissoc (c-map) :b)))))
  (testing "^int ← 1 (a Long) throws" (is (c-diff #((:map %) (c-map :i 1))))))

(deftest vals-hint-test
  ;; V-09 `vals` row. The Long inputs for ^double and ^int are cast.
  (is (c-diff #(mapv class (vals ((:new %) 1 2 3 4 true "s"))))))

(defn- get-n [x] (:n x))

(deftest keyword-call-hint-test
  ;; V-09 keyword call row.
  (is (c-diff #(get-n (c-new % :n 2.7)))))

(defn- get-a [x] (:a x))

(defn- nil-value [fs] ((:new fs) nil nil))

(deftest nil-lookup-test
  ;; V-08 lookup.
  (testing "get with not-found" (is (n-diff #(get (nil-value %) :a ::nf))))
  (testing "keyword call"
    (is (n-diff #(let [x (nil-value %)] [(get-a x) (get-a x)]))))
  (testing ".field"
    (is (n-diff
         #(if (= % n-factories)
            (let [^N x (nil-value %)] [(.-a x) (.-b x)])
            (let [^DN x (nil-value %)] [(.-a x) (.-b x)]))))))

(deftest nil-key-set-test
  ;; V-08 `contains?` and `count`.
  (testing "contains?" (is (n-diff #(contains? (nil-value %) :a))))
  (testing "count" (is (n-diff #(count (nil-value %))))))

(deftest nil-equality-test
  ;; V-08 equality and hashing. No cross-type `hash` (D-07).
  (testing "=" (is (n-diff #(= (nil-value %) (nil-value %)))))
  (testing "hash"
    (is (n-diff #(= (hash (nil-value %)) (hash (nil-value %))))))
  (testing ".hashCode"
    (is (n-diff #(= (.hashCode ^Object (nil-value %))
                    (.hashCode ^Object {:a nil :b nil})))))
  (testing ".equals"
    (is (n-diff #(.equals ^Object (nil-value %) {:a nil :b nil})))
    (is (n-diff #(.equals ^Object {:a nil :b nil} (nil-value %)))))
  (testing "nil ≠ false"
    (is (n-diff #(= (nil-value %) ((:new %) nil false))))))

(defn- in-test-ns
  "Call `f` with `*ns*` bound to this namespace."
  [f]
  (binding [*ns* (the-ns test-ns)] (f)))

(deftest nil-print-read-test
  ;; V-08 print and read.
  (is (n-diff #(let [x (nil-value %)]
                 (= x (in-test-ns (fn [] (read-string (pr-str x))))))))
  (is (true? (let [x (->N nil nil)]
               (= x (in-test-ns (fn [] (read-string (pr-str x)))))))))

(deftest call-site-boxed-value-test
  ;; V-06: a primitive field gives the boxed value.
  (let [v (get-n (->C 1 (int 2) 3.0 (int 4) true "s"))]
    (is (= 2 v))
    (is (= Long (class v)))))

(deftest call-site-type-change-test
  ;; V-06: one site sees A, then B, then A.
  (is (= [1 2 3] [(get-a (->N 1 nil)) (get-a (->N2 2 nil)) (get-a (->N 3 nil))])))
