(ns closed-record.value-type.closed-shape-test
  "VT-V11: the closed shape of a `:value-type true` record (VT-D13 to
  VT-D15, VT-I7 to VT-I9), on both representations."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [check defclosedrecord]]
   [closed-record.probe-support :refer [causes data-of thrown]]
   [closed-record.value-type.expectation :as expectation]
   [closed-record.value-type.support :as support])
  (:import
   (java.lang.reflect Constructor Field InvocationTargetException)))

(def ^:private expected (expectation/representation))

(defclosedrecord S [a b] :value-type true)

(defn- read-literal
  "Read the literal text `s` at run time in this namespace."
  [s]
  (binding [*ns* (the-ns 'closed-record.value-type.closed-shape-test)]
    (read-string s)))

(def ^:private class-prefix
  (str "#" (.getName ^Class S)))

(defn- records
  "A record from each construction path."
  []
  {:positional (->S 1 2)
   :map (map->S {:a 1 :b 2})
   :assoc (assoc (->S 0 2) :a 1)
   :map-literal (read-literal (str class-prefix "{:a 1 :b 2}"))
   :positional-literal (read-literal (str class-prefix "[1 2]"))})

(deftest metadata-test
  ;; (a) VT-D14, VT-I9: no metadata.
  (let [r (->S 1 2)]
    (is (not (instance? clojure.lang.IMeta r)))
    (is (not (instance? clojure.lang.IObj r)))
    (is (nil? (meta r)))
    (is (instance? ClassCastException (thrown #(with-meta r {:m 1}))))))

(deftest undeclared-key-test
  ;; (b) VT-D13: undeclared keys.
  (testing "a map literal with an undeclared key throws at read"
    (is (some #(instance? IllegalArgumentException %)
              (causes (thrown #(read-literal
                                (str class-prefix "{:a 1 :b 2 :x 3}")))))))
  (testing "map->S with an undeclared key throws :closed-record/extra-keys"
    (is (= :closed-record/extra-keys
           (:type (data-of #(map->S {:a 1 :b 2 :x 3})))))))

(def ^:private main-params
  "The main constructor: a b __meta __extmap __hash __hasheq."
  [Object Object Object Object Integer/TYPE Integer/TYPE])

(def ^:private meta-extmap-params
  "The constructor with a b __meta __extmap."
  [Object Object Object Object])

(defn- construct
  "Call the constructor of `S` that has the parameter types `params`.
  The declared fields get `1` and `2`; `m` and `ext` are the `__meta`
  and `__extmap` arguments; the hash arguments that `params` needs
  get `0`."
  [params m ext]
  (let [^Constructor c (.getConstructor ^Class S
                                        (into-array Class params))
        hashes (repeat (- (count params) 4) (int 0))]
    (.newInstance c (object-array (concat [1 2 m ext] hashes)))))

(deftest constructor-test
  ;; (c) VT-D13, VT-D15: the constructors reject non-nil __meta and
  ;; __extmap with IllegalArgumentException.
  (doseq [params [main-params meta-extmap-params]]
    (testing (str "constructor " (count params) " parameters")
      (is (instance? S (construct params nil nil)))
      (doseq [[m ext] [[{:m 1} nil] [nil {:x 1}] [nil {}]]]
        (let [e (thrown #(construct params m ext))]
          (is (instance? InvocationTargetException e) (pr-str [m ext]))
          (is (instance? IllegalArgumentException (ex-cause e))
              (pr-str [m ext])))))))

(deftest check-test
  ;; (d) check returns each record.
  (doseq [[path r] (records)]
    (is (identical? r (check r)) (name path))))

(defn- field-value
  [r field]
  (.get ^Field (.getDeclaredField ^Class S field) r))

(deftest hidden-field-test
  ;; (e) identity: __meta and __extmap hold nil; value: only the declared
  ;; fields (VT-I7).
  (if (= :value expected)
    (is (= #{"a" "b"} (support/instance-field-names S)))
    (doseq [[path r] (records)
            field ["__meta" "__extmap"]]
      (is (nil? (field-value r field)) (str (name path) " " field)))))
