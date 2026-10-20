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

(defn- run-check
  "Pass if `(ok? (op))` is true. Any throw from `op` or `ok?` fails `id`."
  [id op ok?]
  (try
    (when-not (ok? (op)) (failure id nil))
    (catch Throwable e (failure id e))))

(defn- throws-check
  "Pass if `op` throws an `ex-info` with `ex-data` `=` to `data`."
  [id op data]
  (try
    (op)
    (failure id nil)
    (catch clojure.lang.ExceptionInfo e
      (when-not (= data (ex-data e)) (failure id e)))
    (catch Throwable e (failure id e))))

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
        (let [result (volatile! nil)
              model' (assoc model k v)]
          (or (run-check :assoc-type
                         #(vreset! result (assoc x k v))
                         #(instance? P %))
              (run-check :assoc-entries #(into {} @result) #(= model' %))
              [@result model']))
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
             (if (vector? r)
               (if-let [f (i01-checks (first r))] (reduced f) r)
               (reduced r))))
         [x {:a a :b b :c c}]
         steps))))

(def ^:private closed-key-set-property
  (prop/for-all [start gen-start
                 steps gen-steps]
                (let [r (run-steps start steps)]
                  (if (vector? r) true r))))

(deftest assoc-dissoc-sequence-property-test
  (let [result (tc/quick-check 200 closed-key-set-property)]
    (is (:pass? result)
        (pr-str {:seed (:seed result)
                 :smallest (get-in result [:shrunk :smallest])
                 :result-data (get-in result [:shrunk :result-data])}))))
