(ns closed-record.property-test
  "V-03: random `assoc` and `dissoc` sequences keep the key set closed
  (I-01, I-03, I-04).

  The property keeps a value `x` and a model map of the declared entries.
  It stops at the first failed check and returns a failure whose
  `result-data` names that check (and holds `:ex` if the check threw)."
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.results :as results]
   [closed-record.core :refer [check defclosedrecord]]))

(defclosedrecord P [a b c])

(def ^:private D #{:a :b :c})

(def ^:private candidate-keys [:a :b :c :x "a"])

;;; Generators

(def ^:private gen-value (gen/one-of [(gen/return nil) gen/small-integer]))

(def ^:private gen-start (gen/vector gen-value 3))

(def ^:private gen-key (gen/elements candidate-keys))

(def ^:private gen-step
  (gen/one-of [(gen/tuple (gen/return :assoc) gen-key gen-value)
               (gen/tuple (gen/return :dissoc) gen-key)]))

(def ^:private gen-steps (gen/vector gen-step 0 100))

;;; Checks: each returns nil on pass, or a failure Result.

(defn- failure [id ex]
  (let [data (cond-> {:check id} ex (assoc :ex ex))]
    (reify results/Result
      (pass? [_] false)
      (result-data [_] data))))

(defn- failure?
  "True if `r` is a failure Result. Other values are `[x model]` states."
  [r]
  (instance? clojure.test.check.results.Result r))

(defn- attempt
  "Run `op` once. Return `{:value v}` or `{:ex e}`."
  [op]
  (try {:value (op)} (catch Throwable e {:ex e})))

(defn- judge
  "Pass if `outcome` has no `:ex` and `(ok? value)` is true. An `:ex` in
  `outcome`, or a throw from `ok?`, fails `id`."
  [id {:keys [value ex]} ok?]
  (if ex
    (failure id ex)
    (try
      (when-not (ok? value) (failure id nil))
      (catch Throwable e (failure id e)))))

(defn- run-check [id op ok?]
  (judge id (attempt op) ok?))

(defn- throws-check
  "Pass if `op` throws an `ex-info` with `ex-data` `=` to `data`. `data`
  is not nil, so a throw that is not `ex-info` fails `id`."
  [id op data]
  (let [{:keys [ex]} (attempt op)]
    (cond
      (nil? ex) (failure id nil)
      (= data (ex-data ex)) nil
      :else (failure id ex))))

(defn- i01-checks [x]
  (or (run-check :keys #(set (keys x)) #(= D %))
      (run-check :key-set #(set (.keySet ^java.util.Map x)) #(= D %))
      (run-check :entry-set #(set (map key (.entrySet ^java.util.Map x))) #(= D %))
      (run-check :contains #(set (filter (fn [k] (contains? x k)) candidate-keys))
                 #(= D %))
      (run-check :count #(count x) #(= 3 %))
      (run-check :size #(.size ^java.util.Map x) #(= 3 %))
      (run-check :values #(count (.values ^java.util.Map x)) #(= 3 %))
      (run-check :check #(check x) #(identical? x %))))

(defn- run-step
  "Run the step checks of `step` on `[x model]`. Return the next
  `[x model]`, or a failure Result."
  [[x model] [op k v]]
  (let [declared? (contains? D k)]
    (case op
      :assoc
      (if declared?
        (let [outcome (attempt #(assoc x k v))
              model' (assoc model k v)]
          (or (judge :assoc-type outcome #(instance? P %))
              (judge :assoc-entries outcome #(= model' (into {} %)))
              [(:value outcome) model']))
        (or (throws-check :assoc-undeclared-throws
                          #(assoc x k v)
                          {:type :closed-record/undeclared-key :record P :key k})
            [x model]))
      :dissoc
      (if declared?
        (or (throws-check :dissoc-declared-throws
                          #(dissoc x k)
                          {:type :closed-record/declared-key-dissoc :record P :key k})
            [x model])
        (or (run-check :dissoc-undeclared-identical #(dissoc x k) #(identical? x %))
            [x model])))))

(defn- run-steps [[a b c] steps]
  (let [x (->P a b c)]
    (or (i01-checks x)
        (reduce
         (fn [state step]
           (let [r (run-step state step)]
             (if (failure? r)
               (reduced r)
               (if-let [f (i01-checks (first r))] (reduced f) r))))
         [x {:a a :b b :c c}]
         steps))))

(def ^:private closed-key-set-property
  (prop/for-all [start gen-start
                 steps gen-steps]
                (let [r (run-steps start steps)]
                  (if (failure? r) r true))))

(deftest assoc-dissoc-sequence-property-test
  (let [result (tc/quick-check 200 closed-key-set-property)]
    (is (:pass? result)
        (pr-str {:seed (:seed result)
                 :smallest (get-in result [:shrunk :smallest])
                 :result-data (get-in result [:shrunk :result-data])}))))
