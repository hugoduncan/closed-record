(ns closed-record.value-type.non-nil-test
  "VT-V9: non-nil fields. value-type owns the field contract; closed-record
  owns the closed key set (VT-D11). Non-nil fields are read through the
  keyword lookup thunk at a call site, as that path loads the record class
  through the call-site code."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [check defclosedrecord]]
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
  "The classes in the cause chain of the exception of `f`, or nil."
  [f]
  (try (f) nil
       (catch Throwable t
         (set (map class (take-while some? (iterate ex-cause t)))))))

(defn- error-type
  "The `:type` of the `ex-data` of the exception of `f`, or nil."
  [f]
  (try (f) nil (catch Throwable t (:type (ex-data t)))))

(def ^:private inner (->Inner 1))

(def ^:private valid {:i inner :s "s" :u nil})

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
    (is (= inner (:i r)))
    (is (= "s" (:s r))))
  (testing "get"
    (is (= inner (get r :i)))
    (is (= "s" (get r :s))))
  (testing "field access"
    (is (= inner (.-i ^NN r)))
    (is (= "s" (.-s ^NN r)))))

(deftest valid-construction-test
  (let [r (->NN inner "s" nil)]
    (doseq [[label v] [["->NN" r]
                       ["map->NN" (map->NN valid)]
                       ["assoc :i" (assoc r :i (->Inner 1))]
                       ["assoc :s" (assoc r :s "s")]
                       ["assoc :u" (assoc r :u nil)]
                       ["map literal" (read-literal (map-literal valid))]
                       ["positional literal"
                        (read-literal (positional-literal [inner "s" nil]))]]]
      (testing label
        (check-fields v)
        (is (= r v))))
    (testing "with-meta keeps the fields"
      (let [m (with-meta r {:k 1})]
        (check-fields m)
        (is (= {:k 1} (meta m)))))
    (testing "check returns a valid record"
      (is (identical? r (check r))))))

(def ^:private bad
  "For each non-nil field: the wrong-type value."
  {:i "x" :s 1})

(defn- paths
  "For field `k` and value `v`: the label and a thunk of each path."
  [k v]
  (let [m (assoc valid k v)
        r (->NN inner "s" nil)]
    [["->NN" #(->NN (:i m) (:s m) (:u m))]
     ["map->NN" #(map->NN m)]
     ["assoc" #(assoc r k v)]
     ["map literal" #(read-literal (map-literal m))]
     ["positional literal"
      #(read-literal (positional-literal [(:i m) (:s m) (:u m)]))]]))

(deftest invalid-value-test
  (doseq [k [:i :s]]
    (testing (str k " nil")
      (doseq [[label f] (paths k nil)]
        (testing label
          (is (contains? (cause-classes f) NullPointerException)))))
    (testing (str k " wrong type")
      (doseq [[label f] (paths k (bad k))]
        (testing label
          (is (contains? (cause-classes f) ClassCastException)))))
    (testing (str k " missing key")
      (is (contains? (cause-classes #(map->NN (dissoc valid k)))
                     NullPointerException))
      (is (contains? (cause-classes
                      #(read-literal (map-literal (dissoc valid k))))
                     NullPointerException)))))

(deftest untagged-nil-test
  (doseq [[label f] (paths :u nil)]
    (testing label
      (is (nil? (:u (f)))))))

(deftest key-set-precedence-test
  (let [r (->NN inner "s" nil)]
    (is (= :closed-record/undeclared-key (error-type #(assoc r :z 1))))
    (doseq [k [:i :s]]
      (is (= :closed-record/declared-key-dissoc (error-type #(dissoc r k))))
      (is (= :closed-record/extra-keys
             (error-type #(map->NN (assoc (dissoc valid k) :z 1))))))))
