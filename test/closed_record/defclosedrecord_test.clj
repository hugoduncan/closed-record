(ns closed-record.defclosedrecord-test
  "V-01, V-10, V-15, D-03 error data and expansion-time argument errors.
  The tests use only the public API and check behaviour, not forms."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [check defclosedrecord]]))

(def ^:private test-ns 'closed-record.defclosedrecord-test)

(defclosedrecord R [a b])

(defclosedrecord R2 [a b])

(defrecord DR [a b])

(defclosedrecord H [a ^long n])

(defclosedrecord Big
  [f0 f1 f2 f3 f4 f5 f6 f7 f8 f9 f10 f11 f12 f13 f14 f15 f16 f17 f18 f19 f20 f21])

(defclosedrecord S [this gtarget])

(defclosedrecord OV
  [overage f1 f2 f3 f4 f5 f6 f7 f8 f9 f10 f11 f12 f13 f14 f15 f16 f17 f18 f19 f20 f21])

(defprotocol Shape
  (area [x])
  (scaled [x factor])
  (sum-pair [x pair]))

(defclosedrecord U [a b]
  Shape
  (area [_] (* a b))
  (scaled [_ factor] (map->U {:a (* a factor) :b (* b factor)}))
  (sum-pair [_ [p q]] (+ a b p q))
  Comparable
  (compareTo [_ o] (compare a (:a o)))
  Object
  (toString [_] (str "U<" a "," b ">")))

(defclosedrecord JO [a]
  java.lang.Object
  (toString [_] (str "JO<" a ">")))

(defclosedrecord Z0 [])

(defclosedrecord NP [a] :no-print true)

(defclosedrecord LN [a] :load-ns true)

(defclosedrecord NPS [a b]
  :no-print true
  :load-ns false
  Shape
  (area [_] (* a b))
  (scaled [_ factor] (->NPS (* a factor) (* b factor)))
  (sum-pair [_ [p q]] (+ a b p q)))

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
    (is (= ::nf (:x r ::nf))))
  (testing "one call site with other types after the record"
    (let [f (fn [x] (:a x))]
      (is (= 1 (f r)))
      (is (= 7 (f (->R2 7 8))))
      (is (= 5 (f (->DR 5 6))))
      (is (= 3 (f {:a 3}))))))

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
  (testing "declared key gives hash caches 0 (no copy of cached hashes)"
    (let [h (->R 1 2)
          _ [(hash h) (.hashCode ^Object h)]
          v (assoc h :a 5)]
      (is (= (hash (->R 5 2)) (hash v)))
      (is (= (.hashCode ^Object (->R 5 2)) (.hashCode ^Object v)))))
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

;;; cons branches

(deftest cons-branches-test
  (testing "a vector of more than 2 elements uses elements 0 and 1"
    (is (= (->R 9 2) (conj r [:a 9 9]))))
  (testing "a vector of 1 element throws IndexOutOfBoundsException"
    (is (instance? IndexOutOfBoundsException (thrown #(conj r [:a])))))
  (testing "a value that is not seqable throws IllegalArgumentException"
    (is (instance? IllegalArgumentException (thrown #(conj r :x)))))
  (testing "a seq of values that are not map entries throws ClassCastException"
    (is (instance? ClassCastException (thrown #(conj r '([:a 3]))))))
  (testing "nil gives the record unchanged"
    (is (identical? r (conj r nil))))
  (testing "merge: :key is the first undeclared key in seq order"
    (let [m (array-map :y 1 :x 2)]
      (is (= (undeclared (key (first m))) (data-of #(merge r m)))))))

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
           (data-of #(map->R {:a 1 :x 2 :y 3})))))
  (testing "nil gives all fields nil"
    (is (= (->R nil nil) (map->R nil))))
  (testing "a java.util.HashMap gives its entries"
    (is (= (->R 1 2) (map->R (java.util.HashMap. {:a 1 :b 2})))))
  (testing "a vector of pairs gives its entries"
    (is (= (->R 1 2) (map->R [[:a 1] [:b 2]]))))
  (testing "a value that is not seqable throws IllegalArgumentException"
    (is (instance? IllegalArgumentException (thrown #(map->R :x)))))
  (testing "a closed record argument goes through (into {} m)"
    (is (= (->R 1 2) (map->R r))))
  (testing "a defrecord argument goes through (into {} m)"
    (is (= (->R 1 2) (map->R (->DR 1 2))))))

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

;;; V-01: count and seq

(deftest count-seq-test
  (is (= 2 (count r)))
  (is (= [[:a 1] [:b 2]] (seq r)))
  (is (= [:a :b] (keys r)))
  (is (= [1 2] (vals r))))

;;; V-01: equality and hash

(deftest equal-same-type-test
  (is (= r (->R 1 2)))
  (is (not= r (->R 1 3)))
  (is (not= r (->R 9 2))))

(deftest equal-other-type-test
  (is (not= r {:a 1 :b 2}))
  (testing "another closed record type with the same fields and values"
    (is (not= r (->R2 1 2))))
  (testing "a defrecord type with the same fields and values"
    (is (not= r (->DR 1 2)))))

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

;;; Metadata and extra-key retention

(deftest metadata-test
  (let [m {:m 1}]
    (testing "meta gives the metadata of with-meta"
      (is (nil? (meta r)))
      (is (= m (meta (with-meta r m)))))
    (testing "assoc of a declared key keeps the metadata"
      (is (= m (meta (assoc (with-meta r m) :a 5)))))
    (testing "with-meta copies the hash caches"
      (let [x (->R 3 4)
            h (hash x)
            hc (.hashCode ^Object x)
            y (with-meta x m)]
        (is (= h (hash y)))
        (is (= hc (.hashCode ^Object y)))))))

(deftest extra-key-retention-test
  (let [lit (read-literal "#closed_record.defclosedrecord_test.R{:a 1 :b 2 :z 3}")
        extra {:type :closed-record/extra-keys :record R :keys #{:z}}]
    (testing "assoc of a declared key keeps the extra keys"
      (is (= extra (data-of #(check (assoc lit :a 5))))))
    (testing "with-meta keeps the extra keys"
      (is (= extra (data-of #(check (with-meta lit {:m 1}))))))))

;;; java.io.Serializable (D-06)

(defn- round-trip
  "Write `x` with `ObjectOutputStream`; read it with `ObjectInputStream`."
  [x]
  (let [bytes (java.io.ByteArrayOutputStream.)]
    (with-open [out (java.io.ObjectOutputStream. bytes)]
      (.writeObject out x))
    (with-open [in (java.io.ObjectInputStream.
                    (java.io.ByteArrayInputStream. (.toByteArray bytes)))]
      (.readObject in))))

(deftest serializable-test
  (let [m (with-meta r {:m 1})
        s (round-trip m)]
    (is (= r s))
    (is (= R (class s)))
    (is (= {:m 1} (meta s)))
    (is (= (hash r) (hash s)))))

;;; Generated methods not in the V-01 tables (no extra keys)

(deftest is-empty-test
  (is (false? (.isEmpty ^java.util.Map r)))
  (is (true? (.isEmpty ^java.util.Map (map->Z0 {})))))

(deftest zero-fields-test
  (let [z (->Z0)]
    (is (false? (contains? z :a)))
    (is (identical? z (dissoc z :a)))
    (is (nil? (seq z)))
    (is (= z (map->Z0 {})))
    (is (identical? z (check z)))
    (is (= {:type :closed-record/undeclared-key :record Z0 :key :a}
           (data-of #(assoc z :a 1))))))

(deftest contains-value-test
  (let [nf (->R nil false)]
    (is (true? (.containsValue ^java.util.Map r 2)))
    (is (false? (.containsValue ^java.util.Map r 3)))
    (testing "nil and false give false, also if a field has that value"
      (is (false? (.containsValue ^java.util.Map nf nil)))
      (is (false? (.containsValue ^java.util.Map nf false))))))

(deftest entry-at-test
  (is (= [:a 1] (find r :a)))
  (is (map-entry? (find r :a)))
  (is (nil? (find r :x))))

(deftest iterator-test
  (is (= [[:a 1] [:b 2]] (iterator-seq (.iterator ^Iterable r)))))

(deftest java-map-views-test
  (let [jm ^java.util.Map r]
    (is (= 2 (.size jm)))
    (is (= 1 (.get jm :a)))
    (is (nil? (.get jm :x)))
    (is (true? (.containsKey jm :a)))
    (is (false? (.containsKey jm :x)))
    (is (= #{:a :b} (set (.keySet jm))))
    (is (= [1 2] (vec (.values jm))))
    (is (= #{[:a 1] [:b 2]} (set (.entrySet jm))))))

(deftest java-map-mutators-test
  (let [jm ^java.util.Map r]
    (is (instance? UnsupportedOperationException (thrown #(.put jm :a 5))))
    (is (instance? UnsupportedOperationException (thrown #(.remove jm :a))))
    (is (instance? UnsupportedOperationException (thrown #(.putAll jm {:a 5}))))
    (is (instance? UnsupportedOperationException (thrown #(.clear jm))))))

;;; User implementations (D-15)

(deftest user-implementations-test
  (let [u (->U 2 3)]
    (testing "protocol method"
      (is (= 6 (area u))))
    (testing "protocol method that calls map->Name"
      (is (= (->U 4 6) (scaled u 2))))
    (testing "method with a destructured argument"
      (is (= 10 (sum-pair u [2 3]))))
    (testing "Java interface method"
      (is (neg? (compare u (->U 5 0))))
      (is (= [(->U 1 0) (->U 2 3)] (sort [u (->U 1 0)]))))
    (testing "Object method"
      (is (= "U<2,3>" (str u)))))
  (testing "java.lang.Object method"
    (is (= "JO<1>" (str (->JO 1))))))

;;; Accepted options

(deftest accepted-options-test
  (testing ":no-print true gives a working type"
    (is (= 1 (:a (->NP 1))))
    (is (= (->NP 1) (map->NP {:a 1}))))
  (testing ":load-ns true gives a working type"
    (is (= 1 (:a (->LN 1))))
    (is (= (->LN 1) (map->LN {:a 1}))))
  (testing "two options, then a spec"
    (is (= 6 (area (->NPS 2 3))))
    (is (= (->NPS 2 3) (map->NPS {:a 2 :b 3})))))

(defn- eval-in-new-ns
  "Evaluate `form` in a new namespace from `create-ns` (not a loaded lib)."
  [form]
  (let [ns-sym (gensym "closed-record.load-ns-probe")]
    (try
      (binding [*ns* (create-ns ns-sym)]
        (refer-clojure)
        (eval form))
      (finally
        (remove-ns ns-sym)))))

(deftest load-ns-option-test
  (testing ":load-ns true makes class initialisation require the namespace"
    (is (= ExceptionInInitializerError
           (some-> (thrown
                    #(eval-in-new-ns
                      '(closed-record.core/defclosedrecord P [a] :load-ns true)))
                   ex-cause
                   class))))
  (testing "without :load-ns the class loads"
    (is (class?
         (eval-in-new-ns '(closed-record.core/defclosedrecord P [a]))))))

;;; Acceptance 4 basics

(deftest record-basics-test
  (is (record? r))
  (is (not (instance? clojure.lang.IType r)))
  (is (= "#closed_record.defclosedrecord_test.R{:a 1, :b 2}" (pr-str r))))

;;; Field names equal to generated local names

(deftest generated-local-names-test
  (let [s (->S 1 2)]
    (testing "get and keyword call give the field value"
      (is (= 1 (get s :this)))
      (is (= 2 (get s :gtarget)))
      (is (= 1 (:this s)))
      (is (= 2 (:gtarget s))))
    (testing "seq gives the field values"
      (is (= [[:this 1] [:gtarget 2]] (seq s))))
    (testing "assoc of the other field keeps the field value"
      (is (= {:this 1 :gtarget 3} (into {} (assoc s :gtarget 3))))
      (is (= {:this 3 :gtarget 2} (into {} (assoc s :this 3)))))
    (testing "= compares the field values"
      (is (= (->S 1 2) (->S 1 2)))
      (is (not= (->S 1 2) (->S 1 3)))))
  (testing "->Name with a field overage and >20 fields gives each field in order"
    (let [ks (cons :overage (map #(keyword (str "f" %)) (range 1 22)))]
      (is (= (range 22) (map #(get (apply ->OV (range 22)) %) ks))))))

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
           (data-of #(check 1))))
    (is (= {:type :closed-record/not-closed-record :value-class DR}
           (data-of #(check (->DR 1 2)))))))

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
