(ns closed-record.value-type.non-nil-test
  "VT-V9: non-nil fields. value-type owns the field contract; closed-record
  owns the closed key set (VT-D11). Non-nil fields are read through the
  keyword lookup thunk at a call site, as that path loads the record class
  through the call-site code."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [check defclosedrecord]]
   [closed-record.probe-support :refer [causes data-of thrown]]
   [closed-record.value-type.expectation :as expectation])
  (:import
   (java.lang.reflect Field)))

(def ^:private expected (expectation/representation))

(defclosedrecord Inner [x] :value-type true)

(defclosedrecord NN [^Inner ^:non-nil i ^String ^:non-nil s u]
  :value-type true)

(def ^:private class-prefix
  (str "#" (.getName ^Class NN)))

(defn- read-literal
  "Read the literal text `s` at run time in this namespace."
  [s]
  (binding [*ns* (the-ns 'closed-record.value-type.non-nil-test)]
    (read-string s)))

(defn- map-literal
  "The map literal text of `NN` with the entries of `m`."
  [m]
  (str class-prefix (pr-str m)))

(defn- positional-literal
  "The positional literal text of `NN` with the values `vs`."
  [vs]
  (str class-prefix (pr-str (vec vs))))

(defn- cause-classes
  "The classes in the cause chain of the exception `e`."
  [e]
  (set (map class (causes e))))

(defn- error-type
  "The `:type` of the `ex-data` of the exception of `f`, or nil."
  [f]
  (:type (data-of f)))

(def ^:private inner (->Inner 1))

(def ^:private valid {:i inner :s "s" :u 1})

(def ^:private field-keys
  "The `NN` fields in declaration order."
  [:i :s :u])

(defn- positional-values
  "The values of `m` in `NN` field order."
  [m]
  (mapv m field-keys))

(def ^:private base
  "The valid base record, built from `valid`."
  (apply ->NN (positional-values valid)))

(defn- field-type
  [field]
  (.getType ^Field (.getDeclaredField ^Class NN field)))

(deftest storage-type-test
  (is (= Inner (field-type "i")))
  (is (= String (field-type "s")))
  (is (= Object (field-type "u"))))

(deftest representation-test
  (is (= expected (expectation/observed NN)))
  (when (= :value expected)
    (is (= :value (expectation/observed Inner)))))

(defn- check-fields
  [r]
  (is (= NN (class r)))
  (testing "keyword lookup at a call site"
    (is (= (:i valid) (:i r)))
    (is (= (:s valid) (:s r))))
  (testing "get"
    (is (= (:i valid) (get r :i)))
    (is (= (:s valid) (get r :s))))
  (testing "field access"
    (is (= (:i valid) (.-i ^NN r)))
    (is (= (:s valid) (.-s ^NN r)))))

(deftest valid-construction-test
  (doseq [[label v] (concat
                     [["->NN" base]
                      ["map->NN" (map->NN valid)]]
                     (for [k field-keys]
                       [(str "assoc " k) (assoc base k (valid k))])
                     [["map literal" (read-literal (map-literal valid))]
                      ["positional literal"
                       (read-literal
                        (positional-literal (positional-values valid)))]])]
    (testing label
      (check-fields v)
      (is (= base v))))
  (testing "with-meta keeps the fields"
    (let [m (with-meta base {:k 1})]
      (check-fields m)
      (is (= {:k 1} (meta m)))))
  (testing "check returns a valid record"
    (is (identical? base (check base)))))

(def ^:private bad
  "For each non-nil field: the wrong-type value."
  {:i "x" :s 1})

(def ^:private non-nil-keys
  "The non-nil fields of `NN`."
  (keys bad))

(defn- paths
  "For field `k` and value `v`: a vector of the paths, each a map with
  `:label`, a thunk `:f` and `:direct?`. A direct path throws the host
  exception with no wrapper; the reader wraps the literal paths in a
  `ReaderException` with `ex-data`."
  [k v]
  (let [m (assoc valid k v)]
    [{:label "->NN" :f #(apply ->NN (positional-values m)) :direct? true}
     {:label "map->NN" :f #(map->NN m) :direct? true}
     {:label "assoc" :f #(assoc base k v) :direct? true}
     {:label "map literal" :f #(read-literal (map-literal m)) :direct? false}
     {:label "positional literal"
      :f #(read-literal (positional-literal (positional-values m)))
      :direct? false}]))

(defn- missing-key-paths
  "For field `k`: a vector of the paths that build `NN` from `valid` without
  `k`, each a map with `:label`, a thunk `:f` and `:direct?`."
  [k]
  (let [m (dissoc valid k)]
    [{:label "map->NN" :f #(map->NN m) :direct? true}
     {:label "map literal" :f #(read-literal (map-literal m)) :direct? false}]))

(defn- check-host-exception
  "Check that `f` throws `cls` in the cause chain; when `direct?`, also
  check that the exception has no `ex-data` (closed-record does not wrap)."
  [{:keys [f direct?]} cls]
  (let [e (thrown f)]
    (is (contains? (cause-classes e) cls))
    (when direct?
      (is (nil? (ex-data e))))))

(deftest invalid-value-test
  (doseq [k non-nil-keys]
    (testing (str k " nil")
      (doseq [{:keys [label] :as path} (paths k nil)]
        (testing label
          (check-host-exception path NullPointerException))))
    (testing (str k " wrong type")
      (doseq [{:keys [label] :as path} (paths k (bad k))]
        (testing label
          (check-host-exception path ClassCastException))))
    (testing (str k " missing key")
      (doseq [{:keys [label] :as path} (missing-key-paths k)]
        (testing label
          (check-host-exception path NullPointerException))))))

(deftest untagged-nil-test
  (doseq [{:keys [label f]} (paths :u nil)]
    (testing label
      (is (nil? (:u (f)))))))

(deftest key-set-precedence-test
  (testing "assoc :z"
    (is (= :closed-record/undeclared-key (error-type #(assoc base :z 1)))))
  (doseq [k non-nil-keys]
    (testing (str k)
      (testing "dissoc"
        (is (= :closed-record/declared-key-dissoc
               (error-type #(dissoc base k)))))
      (testing "map->NN with an extra key"
        (is (= :closed-record/extra-keys
               (error-type #(map->NN (assoc (dissoc valid k) :z 1)))))))))
