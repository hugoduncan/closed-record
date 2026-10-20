(ns closed-record.differential-test
  "V-02: a closed record and an equivalent `defrecord` give the same
  results for each permitted operation (S-02, I-05).

  The property applies the same random sequence of permitted steps to a
  closed record `x` and to a `defrecord` `y`. After the start and after
  each step it compares `x` and `y` with named checks. It stops at the
  first failed step or check and returns a failure whose `result-data`
  names it in `:check`."
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.results :as results]
   [closed-record.core :refer [defclosedrecord]]))

(defclosedrecord P [a b c])

(defrecord R [a b c])

(def ^:private candidate-keys [:a :b :c :x "a"])

(def ^:private keyword-keys [:a :b :c :x])

;;; Generators

(def ^:private gen-value (gen/one-of [(gen/return nil) gen/small-integer]))

(def ^:private gen-start (gen/vector gen-value 3))

(def ^:private gen-declared-key (gen/elements [:a :b :c]))

(def ^:private gen-undeclared-key (gen/elements [:x "a"]))

(def ^:private gen-meta
  (gen/one-of [(gen/return nil) (gen/fmap (fn [v] {:n v}) gen-value)]))

(def ^:private gen-step
  (gen/one-of
   (conj (mapv (fn [op] (gen/tuple (gen/return op) gen-declared-key gen-value))
               [:assoc :conj :merge :into :update])
         (gen/tuple (gen/return :dissoc) gen-undeclared-key)
         (gen/tuple (gen/return :with-meta) gen-meta))))

(def ^:private gen-steps (gen/vector gen-step 0 100))

;;; Outcome model: each check returns nil on pass, or a failure Result.

(defn- failure [data]
  (reify results/Result
    (pass? [_] false)
    (result-data [_] data)))

(defn- failure?
  "True if `r` is a failure Result."
  [r]
  (instance? clojure.test.check.results.Result r))

(defn- attempt
  "Run `op` once. Return `{:value v}` or `{:ex e}`."
  [op]
  (try {:value (op)} (catch Throwable e {:ex e})))

(defn- thrown
  "Failure data for a throw in `xo` or `yo`, or nil. The `x` side wins."
  [xo yo]
  (cond
    (:ex xo) {:side :closed :ex (:ex xo)}
    (:ex yo) {:side :record :ex (:ex yo)}))

(defn- compare-outcomes
  "Fail `id` if `xo` or `yo` has `:ex`, or if their values differ. A throw
  from `=` (for example when it realizes a lazy seq) fails `id` with `:ex`."
  [id xo yo extra]
  (if-let [t (thrown xo yo)]
    (failure (merge {:check id} extra t))
    (let [{:keys [value ex]} (attempt #(= (:value xo) (:value yo)))]
      (cond
        ex (failure (merge {:check id} extra {:ex ex}))
        (not value) (failure (merge {:check id} extra
                                    {:closed (:value xo)
                                     :record (:value yo)}))))))

(defn- judge
  "Fail `id` if `op` throws on either side, or if its results differ."
  [id op x y]
  (compare-outcomes id (attempt #(op x)) (attempt #(op y)) nil))

(defn- judge-one
  "Fail `id` if `op` on `x` throws, or if `ok?` of its result is false. A
  throw from `ok?` fails `id` with `:ex`."
  [id op ok?]
  (let [xo (attempt op)]
    (if (:ex xo)
      (failure {:check id :side :closed :ex (:ex xo)})
      (let [{:keys [value ex]} (attempt #(ok? (:value xo)))]
        (cond
          ex (failure {:check id :ex ex})
          (not value) (failure {:check id :closed (:value xo)}))))))

(defn- judge-each
  "Compare `(f x k)` with `(f y k)` for each `k` in `ks`; fail `id` with
  `{ext-key k}`. Stop at the first failure."
  [id ext-key ks f x y]
  (some (fn [k]
          (compare-outcomes id (attempt #(f x k)) (attempt #(f y k)) {ext-key k}))
        ks))

(defn- checks
  "Compare `x` with `y`. `x0` and `y0` are the start values."
  [x y x0 y0]
  (or (judge-one :type #(class x) #(= P %))
      (judge :entries #(into {} %) x y)
      (judge :seq seq x y)
      (judge :count count x y)
      (judge-each :get :key candidate-keys #(get %1 %2 ::nf) x y)
      (judge-each :keyword :key keyword-keys #(%2 %1 ::nf) x y)
      (judge-each :contains :key candidate-keys contains? x y)
      (judge-each :find :key candidate-keys find x y)
      (judge :size #(.size ^java.util.Map %) x y)
      (judge :is-empty #(.isEmpty ^java.util.Map %) x y)
      (judge-each :map-get :key candidate-keys #(.get ^java.util.Map %1 %2) x y)
      (judge-each :contains-key :key candidate-keys
                  #(.containsKey ^java.util.Map %1 %2) x y)
      (judge :key-set #(.keySet ^java.util.Map %) x y)
      (judge :values #(.values ^java.util.Map %) x y)
      (judge :entry-set #(.entrySet ^java.util.Map %) x y)
      (judge-each :contains-value :value (conj (vec (vals y)) ::nf)
                  #(.containsValue ^java.util.Map %1 %2) x y)
      (judge :meta meta x y)
      (judge :hash-code #(.hashCode ^Object %) x y)
      (judge-one :equals #(.equals ^Object x y) true?)
      (compare-outcomes :start-equal
                        (attempt #(= x x0)) (attempt #(= y y0)) nil)
      (compare-outcomes :start-hash
                        (attempt #(= (hash x) (hash x0)))
                        (attempt #(= (hash y) (hash y0)))
                        nil)))

;;; Steps

(defn- apply-step
  "Apply `step` to `x`. A step is `[op k v]`, `[:dissoc k]` or
  `[:with-meta m]`."
  [x step]
  (if (= :with-meta (first step))
    (let [[_ m] step]
      (with-meta x m))
    (let [[op k v] step]
      (case op
        :assoc (assoc x k v)
        :conj (conj x [k v])
        :merge (merge x {k v})
        :into (into x {k v})
        :update (update x k (constantly v))
        :dissoc (dissoc x k)))))

(defn- run-steps [[a b c] steps]
  (let [x0 (->P a b c)
        y0 (->R a b c)]
    (or (checks x0 y0 x0 y0)
        (reduce
         (fn [[x y] step]
           (let [xo (attempt #(apply-step x step))
                 yo (attempt #(apply-step y step))]
             (if-let [t (thrown xo yo)]
               (reduced (failure (merge {:check :step :step step} t)))
               (let [x' (:value xo) y' (:value yo)]
                 (if-let [f (checks x' y' x0 y0)]
                   (reduced f)
                   [x' y'])))))
         [x0 y0]
         steps))))

(def ^:private differential-property
  (prop/for-all [start gen-start
                 steps gen-steps]
                (let [r (run-steps start steps)]
                  (if (failure? r) r true))))

(deftest defrecord-differential-property-test
  (let [result (tc/quick-check 200 differential-property)]
    (is (:pass? result)
        (pr-str {:seed (:seed result)
                 :smallest (get-in result [:shrunk :smallest])
                 :result-data (get-in result [:shrunk :result-data])}))))
