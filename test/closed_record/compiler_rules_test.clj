(ns closed-record.compiler-rules-test
  "V-13: compiler rules K-06 to K-08 that closed-record depends on.
  These tests use only `defrecord`, `deftype`, `deftype*`, the reader and the
  compiler. Each `deftype*` form is quoted data given to `eval`."
  (:require
   [clojure.test :refer [deftest is testing]]))

(def ^:private test-ns 'closed-record.compiler-rules-test)

(def calls
  "Names of the type methods that the K-07 test type calls."
  (atom []))

(defn- in-test-ns
  "Call `f` with `*ns*` bound to this namespace."
  [f]
  (binding [*ns* (the-ns test-ns)] (f)))

(defn- eval-in-ns [form] (in-test-ns #(eval form)))

(defn- thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable t t)))

(defn- class-name [type-name]
  (str (namespace-munge test-ns) "." type-name))

(defn- load-class
  "Load the class of an `eval`ed `deftype*` form (K-06 class lookup)."
  [type-name]
  (with-bindings {clojure.lang.Compiler/LOADER (clojure.lang.DynamicClassLoader.)}
    (Class/forName (class-name type-name) true (clojure.lang.RT/baseLoader))))

(defn- deftype*-form
  "A `deftype*` form for `type-name` with `fields`, `interfaces` and `methods`."
  [type-name fields interfaces methods]
  (list* 'deftype*
         (symbol (str test-ns) (str type-name))
         (symbol (class-name type-name))
         fields
         :implements interfaces
         methods))

(def ^:private meta-field '__meta)
(def ^:private extmap-field '__extmap)
(def ^:private hash-field
  (with-meta '__hash {:tag 'int :unsynchronized-mutable true}))
(def ^:private hasheq-field
  (with-meta '__hasheq {:tag 'int :unsynchronized-mutable true}))

(def ^:private hidden-fields
  [meta-field extmap-field hash-field hasheq-field])

(defn- param-types [^Class c]
  (set (map #(vec (.getParameterTypes ^java.lang.reflect.Constructor %))
            (.getConstructors c))))

(defn- has-create? [^Class c]
  (boolean (some #(= "create" (.getName ^java.lang.reflect.Method %))
                 (.getMethods c))))

(defn- has-basis? [^Class c]
  (boolean (some #(= "getBasis" (.getName ^java.lang.reflect.Method %))
                 (.getMethods c))))

(defn- field
  "The value of the field `field-name` of `o`."
  [o field-name]
  (clojure.lang.Reflector/getInstanceField o field-name))

(deftype Q [a])

(deftest k-06-four-hidden-fields-test
  (testing "deftype* with the four hidden fields at the end"
    (eval-in-ns (deftype*-form 'K06Four (into '[a] hidden-fields)
                  '[clojure.lang.IRecord] []))
    (let [c (load-class 'K06Four)]
      (testing "has the static method create"
        (is (has-create? c)))
      (testing "has the declared+__meta+__extmap constructor"
        (is (contains? (param-types c) [Object Object Object])))
      (testing "has the declared-fields constructor"
        (is (contains? (param-types c) [Object]))))))

(deftest k-06-control-test
  (testing "deftype without hidden fields has getBasis and no create"
    (is (has-basis? Q))
    (is (not (has-create? Q)))))

(deftest k-06-three-hidden-fields-test
  (doseq [missing (range 4)]
    (let [type-name (symbol (str "K06Three" missing))
          fields (into '[a] (keep-indexed #(when (not= missing %1) %2))
                       hidden-fields)]
      (testing (str "deftype* with fields " fields)
        (eval-in-ns (deftype*-form type-name fields
                      '[clojure.lang.IRecord] []))
        (is (= VerifyError
               (class (thrown #(load-class type-name)))))))))

(deftest k-06-reserved-names-test
  (doseq [field-name '[__meta __extmap __hash __hasheq]]
    (testing (str "deftype rejects " field-name)
      (let [t (thrown
               (fn []
                 (in-test-ns
                  #(macroexpand-1 (list 'deftype 'T ['a field-name])))))]
        (is (= AssertionError (class (ex-cause t))))))))

(def ^:private k-07-methods
  (for [m '[assoc without cons]]
    (list m (if (= 'assoc m) '[this k v] '[this x])
          (list 'swap! 'calls 'conj (list 'quote m))
          'this)))

(deftest k-07-create-test
  (reset! calls [])
  (eval-in-ns (deftype*-form 'K07 (into '[a] hidden-fields)
                '[clojure.lang.IRecord clojure.lang.IPersistentMap]
                k-07-methods))
  (let [lit (fn [m] (in-test-ns #(read-string (str "#" (class-name 'K07) m))))]
    (testing "create puts extra literal keys into __extmap"
      (let [o (lit "{:a 1 :x 2}")]
        (is (= 1 (field o "a")))
        (is (= {:x 2} (field o "__extmap")))))
    (testing "create gives nil for a missing key"
      (is (nil? (field (lit "{:x 2}") "a"))))
    (testing "create does not call methods of the type"
      (is (= [] @calls)))))

(defrecord R [a])

(deftest k-08-defrecord-test
  (testing "defrecord does not implement IType"
    (is (not (instance? clojure.lang.IType (->R 1))))))

(def ^:private entry-set-method
  '(entrySet [this] #{(clojure.lang.MapEntry/create :a 99)}))

(defn- constant
  "Embed `v` as a constant in compiled code and return the rebuilt value."
  [v]
  (eval-in-ns (list (list 'fn [] v))))

(deftest k-08-irecord-constant-test
  (testing "a record constant without IType is built with create from the Map view"
    (eval-in-ns (deftype*-form
                  'K08Record (into '[a] hidden-fields)
                  '[clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]
                  [entry-set-method]))
    (let [v (eval-in-ns (list 'new (symbol (class-name 'K08Record)) 1 nil nil))
          c (constant v)]
      (is (= 1 (field v "a")))
      (is (= (class v) (class c)))
      (is (= 99 (field c "a"))))))

(deftest k-08-itype-before-irecord-test
  (testing "the IType path is used before the IRecord path"
    (eval-in-ns
     (deftype*-form
       'K08IType '[a]
       '[clojure.lang.IType clojure.lang.IRecord clojure.lang.IPersistentMap
         java.util.Map]
       [entry-set-method]))
    (let [v (eval-in-ns (list 'new (symbol (class-name 'K08IType)) 1))]
      (is (= 1 (field (constant v) "a")))))
  (testing "without IType, the IRecord path calls the missing create"
    (eval-in-ns
     (deftype*-form
       'K08NoIType '[a]
       '[clojure.lang.IRecord clojure.lang.IPersistentMap java.util.Map]
       [entry-set-method]))
    (let [v (eval-in-ns (list 'new (symbol (class-name 'K08NoIType)) 1))]
      (is (= NoSuchMethodError
             (class (thrown #(constant v))))))))
