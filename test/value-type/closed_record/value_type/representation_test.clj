(ns closed-record.value-type.representation-test
  "VT-V2 (expansion inspection, the VT-I4 check on Temurin 21), the VT-V4
  representation checks, compared with the expected representation, and
  `:value-type true` together with `:no-print` and `:load-ns`."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]
   [closed-record.value-type.expectation :as expectation]))

(def ^:private expected (expectation/representation))

(defclosedrecord VP [a b] :value-type true)

(defclosedrecord VNP [a] :value-type true :no-print true)

(defclosedrecord VLN [a] :value-type true :load-ns true)

(defn- value-expansion
  []
  (binding [*ns* (the-ns 'closed-record.value-type.representation-test)]
    (macroexpand-1 '(closed-record.core/defclosedrecord Q [a b]
                      :value-type true))))

(defn- type-form
  "The `defvaluetype*` form of `expansion`."
  [expansion]
  (first (filter #(and (seq? %) (= 'value-type.core/defvaluetype* (first %)))
                 (tree-seq seq? seq expansion))))

(defn- method
  [tform mname]
  (first (filter #(and (seq? %) (= mname (first %))) tform)))

(defn- symbols
  [form]
  (set (filter symbol? (tree-seq coll? seq form))))

(deftest value-expansion-test
  ;; VT-V2: the expansion of a :value-type true definition.
  (let [expansion (value-expansion)
        tform (type-form expansion)
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
      (is (not-any? #{:value-type} (flatten (map #(if (coll? %) [] [%])
                                                 tform)))))
    (testing "hash forms compute on each call"
      (is (= 'clojure.lang.APersistentMap/mapHash
             (first (last (method specs 'hashCode)))))
      (is (contains? (symbols (method specs 'hasheq))
                     'clojure.lang.APersistentMap/mapHasheq)))
    (testing "withMeta calls the constructor with the fields, __meta and
              __extmap"
      (let [[_ [_ m] body] (method specs 'withMeta)]
        (is (= (list 'new 'Q 'a 'b m '__extmap) body))))))

(deftest representation-test
  ;; VT-V4: compare the observed representation with the expectation.
  (let [x (->VP 1 2)
        y (->VP 1 2)]
    (is (= expected (expectation/observed VP)))
    (if (= :value expected)
      (testing "value output"
        (is (identical? x y))
        (is (thrown? NoSuchFieldException (.getDeclaredField VP "__hash")))
        (is (thrown? NoSuchFieldException (.getDeclaredField VP "__hasheq"))))
      (testing "identity output"
        (is (not (identical? x y)))
        (is (some? (.getDeclaredField VP "__hash")))))
    (is (= x y))
    (is (= (hash x) (hash y)))))

(defn- thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable e e)))

(defn- eval-in-new-ns
  "Evaluate `form` in a new namespace from `create-ns` (not a loaded lib)."
  [form]
  (let [ns-sym (gensym "closed-record.value-type.load-ns-probe")]
    (try
      (binding [*ns* (create-ns ns-sym)]
        (refer-clojure)
        (eval form))
      (finally
        (remove-ns ns-sym)))))

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
