(ns closed-record.value-type.representation-test
  "VT-V2 (expansion inspection, the VT-I4 check on Temurin 21, and the
  VT-I6 check of `:non-nil` fields, `non-nil-expansion-test`), the VT-V4
  representation checks, compared with the expected representation, and
  `:value-type true` together with `:no-print` and `:load-ns`."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]
   [closed-record.expansion-fixture :as fixture]
   [closed-record.probe-support :refer [eval-in-new-ns thrown]]
   [closed-record.value-type.expectation :as expectation]
   [closed-record.value-type.support :as support]))

(def ^:private expected (expectation/representation))

(defclosedrecord VP [a b] :value-type true)

(defclosedrecord VNP [a] :value-type true :no-print true)

(defclosedrecord VLN [a] :value-type true :load-ns true)

(defn- value-expansion
  []
  (binding [*ns* (the-ns 'closed-record.value-type.representation-test)]
    (macroexpand-1 '(closed-record.core/defclosedrecord Q [a b]
                      :value-type true))))

(defn- symbols
  [form]
  (set (filter symbol? (tree-seq coll? seq form))))

(defn- type-options
  "The option map of the type form `tform`: the keyword and value pairs
  after the field vector."
  [tform]
  (loop [xs (drop 4 tform)
         opts {}]
    (if (keyword? (first xs))
      (recur (drop 2 xs) (assoc opts (first xs) (second xs)))
      opts)))

(defn- factory-forms
  "The `->Q` and `map->Q` defn forms of `expansion`."
  [expansion]
  (filter #(and (seq? %)
                (= 'clojure.core/defn (first %))
                (contains? '#{->Q map->Q} (second %)))
          (tree-seq seq? seq expansion)))

(deftest value-expansion-test
  ;; VT-V2: the expansion of a :value-type true definition.
  (let [expansion (value-expansion)
        tform (support/type-form expansion)
        [_ _ _ fields & specs] tform]
    (testing "defvaluetype* replaces deftype*"
      (is (some? tform))
      (is (not-any? #{'deftype*} (symbols expansion))))
    (testing "hidden field layout"
      (is (= '[a b __meta __extmap __hash __hasheq] fields))
      (is (= ['int 'int] (map (comp :tag meta) (drop 4 fields))))
      (is (not-any? #(some (or (meta %) {}) [:unsynchronized-mutable
                                             :volatile-mutable])
                    fields)))
    (testing "the closed shape option (VT-D13)"
      (is (true? (:value-type/closed (type-options tform)))))
    (testing "no generated reference to a hidden field (VT-I4)"
      (is (not-any? #{'__meta '__extmap '__hash '__hasheq}
                    (symbols (concat specs (factory-forms expansion)))))
      (is (= 2 (count (factory-forms expansion)))))
    (testing "no IMeta or IObj (VT-I9)"
      (let [implements (:implements (type-options tform))]
        (is (vector? implements))
        (is (not-any? '#{clojure.lang.IMeta clojure.lang.IObj} implements))))
    (testing "the option is not passed to defvaluetype*"
      (is (not-any? #{:value-type} tform)))))

;; VT-I6: the expansion of a form with :non-nil fields, and of the same form
;; without :non-nil, as the expansion fixture expands (fixture namespace,
;; positions removed).

(def ^:private non-nil-form
  '(closed-record.core/defclosedrecord NN
     [^closed_record.value_type.representation_test.VP ^:non-nil p
      ^String ^:non-nil s
      u]
     :value-type true))

(defn- remove-non-nil
  [form]
  (fixture/strip-meta form [:non-nil]))

(def ^:private declared-non-nil-fields
  "The `:non-nil` field symbols of `non-nil-form`."
  (filter (comp :non-nil meta) (nth non-nil-form 2)))

(def ^:private plain-form
  "`non-nil-form` without `:non-nil`."
  (remove-non-nil non-nil-form))

(deftest non-nil-expansion-test
  ;; VT-V2, VT-I6: :non-nil reaches the defvaluetype* field vector unchanged
  ;; and the emitter adds nothing else for it.
  (let [expansion (fixture/expand non-nil-form)
        [_ _ _ fields] (support/type-form expansion)
        non-nil-fields (filter (comp :non-nil meta) fields)]
    (testing "(a) the non-nil field symbols carry :non-nil true"
      (is (= declared-non-nil-fields non-nil-fields))
      (is (every? #(true? (:non-nil (meta %))) non-nil-fields)))
    (testing "(a) no other place in the expansion carries :non-nil"
      (is (= (count declared-non-nil-fields)
             (count (re-seq #":non-nil" (fixture/printed expansion))))))
    (testing "(b) removal of :non-nil commutes with the expansion"
      (is (= (fixture/expansion plain-form)
             (fixture/printed (remove-non-nil expansion)))))))

(deftest representation-test
  ;; VT-V4: compare the observed representation with the expectation.
  (let [x (->VP 1 2)
        y (->VP 1 2)
        fields (support/instance-field-names VP)]
    (is (= expected (expectation/observed VP)))
    (if (= :value expected)
      (testing "value output"
        (is (identical? x y))
        (is (= #{"a" "b"} fields) "VT-I7: only the declared fields"))
      (testing "identity output"
        (is (not (identical? x y)))
        (is (contains? fields "__hash"))))
    (is (= x y))
    (is (= (hash x) (hash y)))))

(deftest options-test
  ;; Design, Public contract: the option works together with :no-print and
  ;; :load-ns.
  (testing ":value-type true :no-print true gives a working type"
    (is (= 1 (:a (->VNP 1))))
    (is (= (->VNP 1) (map->VNP {:a 1})))
    (is (= expected (expectation/observed VNP))))
  (testing ":value-type true :load-ns true gives a working type"
    (is (= 1 (:a (->VLN 1))))
    (is (= (->VLN 1) (map->VLN {:a 1})))
    (is (= expected (expectation/observed VLN))))
  (testing ":load-ns true makes class initialisation require the namespace"
    (is (= ExceptionInInitializerError
           (some-> (thrown
                    #(eval-in-new-ns
                      '(closed-record.core/defclosedrecord P [a]
                         :value-type true :load-ns true)))
                   ex-cause
                   class))))
  (testing "without :load-ns the class loads"
    (is (class?
         (eval-in-new-ns '(closed-record.core/defclosedrecord P [a]
                            :value-type true))))))
