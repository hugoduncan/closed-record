(ns closed-record.value-type.representation-test
  "VT-V2 (expansion inspection, the VT-I4 check on Temurin 21), the VT-V4
  representation checks, compared with the expected representation, and
  `:value-type true` together with `:no-print` and `:load-ns`."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]
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
    (testing "no generated reference to __hash or __hasheq"
      (is (not-any? #{'__hash '__hasheq} (symbols specs))))
    (testing "the option is not passed to defvaluetype*"
      (is (not-any? #{:value-type} tform)))))

(defn- field-names
  [^Class c]
  (set (map #(.getName ^java.lang.reflect.Field %) (.getDeclaredFields c))))

(deftest representation-test
  ;; VT-V4: compare the observed representation with the expectation.
  (let [x (->VP 1 2)
        y (->VP 1 2)
        fields (field-names VP)]
    (is (= expected (expectation/observed VP)))
    (if (= :value expected)
      (testing "value output"
        (is (identical? x y))
        (is (not (contains? fields "__hash")))
        (is (not (contains? fields "__hasheq"))))
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
