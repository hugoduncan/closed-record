(ns closed-record.defclosedrecord-test
  "V-01, V-10, V-15, D-03 error data and expansion-time argument errors.
  The tests use only the public API and check behaviour, not forms."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [check defclosedrecord]]))

(def ^:private test-ns 'closed-record.defclosedrecord-test)

(defclosedrecord R [a b])

(defclosedrecord H [a ^long n])

(defclosedrecord Big
  [f0 f1 f2 f3 f4 f5 f6 f7 f8 f9 f10 f11 f12 f13 f14 f15 f16 f17 f18 f19 f20 f21])

(def ^:private r (->R 1 2))

(defn- in-test-ns
  "Call `f` with `*ns*` bound to this namespace."
  [f]
  (binding [*ns* (the-ns test-ns)] (f)))

(defn- read-literal [s] (in-test-ns #(read-string s)))

(defn- thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable e e)))

(defn- data-of [f] (ex-data (thrown f)))

(defn- undeclared [k] {:type :closed-record/undeclared-key :record R :key k})

;;; V-01: get, keyword call, .field

(deftest get-test
  (testing "declared key gives the value"
    (is (= 1 (get r :a))))
  (testing "undeclared key gives the not-found value"
    (is (nil? (get r :x)))
    (is (= ::nf (get r :x ::nf)))))

(deftest keyword-call-test
  (testing "declared key gives the value"
    (is (= 2 (:b r))))
  (testing "undeclared key gives the not-found value"
    (is (nil? (:x r)))
    (is (= ::nf (:x r ::nf)))))

(deftest field-access-test
  (testing "declared field gives the value"
    (is (= 1 (.-a ^R r)))))

;;; V-01: contains?

(deftest contains-test
  (testing "declared key"
    (is (true? (contains? r :a))))
  (testing "undeclared key"
    (is (false? (contains? r :x)))))

;;; V-01: assoc, conj, merge, into, update

(deftest assoc-test
  (testing "declared key gives a new record of the same type"
    (let [v (assoc r :a 5)]
      (is (instance? R v))
      (is (= (->R 5 2) v))))
  (testing "undeclared key throws (assoc error data path)"
    (is (= (undeclared :x) (data-of #(assoc r :x 1))))))

(deftest conj-test
  (testing "declared key gives a new record of the same type"
    (let [v (conj r [:a 5])]
      (is (instance? R v))
      (is (= (->R 5 2) v))))
  (testing "undeclared key throws (cons error data path)"
    (is (= (undeclared :x) (data-of #(conj r [:x 1]))))))

(deftest merge-test
  (testing "declared key gives a new record of the same type"
    (let [v (merge r {:b 7})]
      (is (instance? R v))
      (is (= (->R 1 7) v))))
  (testing "undeclared key throws"
    (is (= (undeclared :x) (data-of #(merge r {:x 1}))))))

(deftest into-test
  (testing "declared key gives a new record of the same type"
    (let [v (into r {:b 7})]
      (is (instance? R v))
      (is (= (->R 1 7) v))))
  (testing "undeclared key throws"
    (is (= (undeclared :x) (data-of #(into r {:x 1}))))))

(deftest update-test
  (testing "declared key gives a new record of the same type"
    (let [v (update r :b inc)]
      (is (instance? R v))
      (is (= (->R 1 3) v))))
  (testing "undeclared key throws"
    (is (= (undeclared :x) (data-of #(update r :x identity))))))

;;; V-01: dissoc, empty

(deftest dissoc-test
  (testing "declared key throws (dissoc error data path)"
    (is (= {:type :closed-record/declared-key-dissoc :record R :key :a}
           (data-of #(dissoc r :a)))))
  (testing "undeclared key returns the record"
    (is (identical? r (dissoc r :x)))))

(deftest empty-test
  (is (= {:type :closed-record/empty :record R} (data-of #(empty r)))))

;;; V-01: map->Name

(deftest map-factory-test
  (testing "declared key: missing key gives nil"
    (let [v (map->H {:n 3})]
      (is (nil? (:a v)))
      (is (= 3 (:n v)))))
  (testing "declared key: missing ^long key throws NullPointerException itself"
    (let [e (thrown #(map->H {:a 1}))]
      (is (instance? NullPointerException e))
      (is (nil? (ex-cause e)))))
  (testing "undeclared key throws (map->Name error data path)"
    (is (= {:type :closed-record/extra-keys :record R :keys #{:x :y}}
           (data-of #(map->R {:a 1 :x 2 :y 3}))))))

;;; V-01: reader literal

(deftest reader-literal-declared-key-test
  (testing "missing key gives nil"
    (let [v (read-literal "#closed_record.defclosedrecord_test.H{:n 3}")]
      (is (instance? H v))
      (is (nil? (:a v)))))
  (testing "missing ^long key throws NullPointerException itself"
    (let [e (thrown #(read-literal "#closed_record.defclosedrecord_test.H{:a 1}"))]
      (is (instance? NullPointerException e))
      (is (nil? (ex-cause e))))))

(deftest reader-literal-undeclared-key-test
  (let [v (read-literal "#closed_record.defclosedrecord_test.R{:a 1 :b 2 :z 3}")]
    (is (instance? R v))
    (is (nil? (get v :z)))
    (is (nil? (:z v)))
    (is (false? (contains? v :z)))
    (is (= {:type :closed-record/extra-keys :record R :keys #{:z}}
           (data-of #(check v))))))

;;; V-01: equality and hash

(deftest equal-same-type-test
  (is (= r (->R 1 2)))
  (is (not= r (->R 1 3))))

(deftest equal-other-type-test
  (is (not= r {:a 1 :b 2}))
  (is (not= r (map->H {:a 1 :n 2}))))

(deftest equals-java-map-test
  (is (.equals ^Object r {:a 1 :b 2}))
  (is (not (.equals ^Object r {:a 1 :b 3}))))

(deftest hash-test
  (is (= (int (bit-xor (hash 'closed_record.defclosedrecord_test.R)
                       (clojure.lang.APersistentMap/mapHasheq r)))
         (hash r))))

(deftest hash-code-test
  (is (= (.hashCode ^Object {:a 1 :b 2}) (.hashCode ^Object r))))

(deftest equality-ignores-metadata-and-hidden-keys-test
  (let [m (with-meta r {:m 1})
        lit (read-literal "#closed_record.defclosedrecord_test.R{:a 1 :b 2 :z 3}")]
    (is (= r m lit))
    (is (= (hash r) (hash m) (hash lit)))
    (is (= (.hashCode ^Object r) (.hashCode ^Object m) (.hashCode ^Object lit)))))

;;; Acceptance 4 basics

(deftest record-basics-test
  (is (record? r))
  (is (not (instance? clojure.lang.IType r)))
  (is (= "#closed_record.defclosedrecord_test.R{:a 1, :b 2}" (pr-str r))))

;;; V-10: more than 20 fields

(deftest big-factories-test
  (let [ks (map #(keyword (str "f" %)) (range 22))]
    (testing "->Name gives each field in order"
      (is (= (range 22) (map #(get (apply ->Big (range 22)) %) ks))))
    (testing "map->Name gives each field in order"
      (is (= (range 22) (map #(get (map->Big (zipmap ks (range 22))) %) ks))))
    (testing "->Name with a wrong argument count throws ArityException"
      (is (instance? clojure.lang.ArityException (thrown #(apply ->Big (range 21)))))
      (is (instance? clojure.lang.ArityException (thrown #(apply ->Big (range 23))))))))

;;; V-15: check

(deftest check-test
  (testing "closed record with no extra keys gives the record"
    (is (identical? r (check r))))
  (testing "record from a literal with extra keys throws"
    (is (= {:type :closed-record/extra-keys :record R :keys #{:y :z}}
           (data-of #(check (read-literal
                             "#closed_record.defclosedrecord_test.R{:a 1 :y 2 :z 3}"))))))
  (testing "other values throw"
    (is (= {:type :closed-record/not-closed-record :value-class nil}
           (data-of #(check nil))))
    (is (= {:type :closed-record/not-closed-record
            :value-class clojure.lang.PersistentArrayMap}
           (data-of #(check {:a 1 :b 2}))))
    (is (= {:type :closed-record/not-closed-record :value-class Long}
           (data-of #(check 1))))))

;;; Expansion-time argument errors (acceptance 5)

(defn- expansion-cause
  "The class of the cause of the error of `macroexpand-1` of `form`."
  [form]
  (some-> (thrown #(in-test-ns (fn [] (macroexpand-1 form)))) ex-cause class))

(deftest argument-errors-test
  (testing "reserved field names"
    (doseq [n '[__meta __extmap __hash __hasheq]]
      (is (= AssertionError
             (expansion-cause (list 'defclosedrecord 'T ['a n]))))))
  (testing "duplicate field names"
    (is (= AssertionError (expansion-cause '(defclosedrecord T [a a])))))
  (testing "mutable field metadata"
    (is (= IllegalArgumentException
           (expansion-cause '(defclosedrecord T [^:volatile-mutable a]))))
    (is (= IllegalArgumentException
           (expansion-cause '(defclosedrecord T [^:unsynchronized-mutable a])))))
  (testing "unsupported option"
    (is (= IllegalArgumentException
           (expansion-cause '(defclosedrecord T [a] :foo 1))))))
