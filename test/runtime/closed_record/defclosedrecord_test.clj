(ns closed-record.defclosedrecord-test
  "Ordinary-mode run of `closed-record.checks.defclosedrecord`, and the tests
  that are not shared."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.checks.support :refer [define-checks]]
   [closed-record.core :refer [defclosedrecord]]))

(defprotocol Measure
  (measure [x]))

(defn- thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable e e)))

(define-checks 'closed-record.checks.defclosedrecord
  {:record-opts [] :representation :identity})

(defclosedrecord NP [a] :no-print true)

(defclosedrecord LN [a] :load-ns true)

(defclosedrecord NPS [a b]
  :no-print true
  :load-ns false
  Measure
  (measure [_] (* a b)))

;;; Accepted options

(deftest accepted-options-test
  (testing ":no-print true gives a working type"
    (is (= 1 (:a (->NP 1))))
    (is (= (->NP 1) (map->NP {:a 1}))))
  (testing ":load-ns true gives a working type"
    (is (= 1 (:a (->LN 1))))
    (is (= (->LN 1) (map->LN {:a 1}))))
  (testing "two options, then a spec"
    (is (= 6 (measure (->NPS 2 3))))
    (is (= (->NPS 2 3) (map->NPS {:a 2 :b 3})))))

(defn- eval-in-new-ns
  "Evaluate `form` in a new namespace from `create-ns` (not a loaded lib)."
  [form]
  (let [ns-sym (gensym "closed-record.load-ns-probe")]
    (try
      (binding [*ns* (create-ns ns-sym)]
        (refer-clojure)
        (eval form))
      (finally
        (remove-ns ns-sym)))))

(deftest load-ns-option-test
  (testing ":load-ns true makes class initialisation require the namespace"
    (is (= ExceptionInInitializerError
           (some-> (thrown
                    #(eval-in-new-ns
                      '(closed-record.core/defclosedrecord P [a] :load-ns true)))
                   ex-cause
                   class))))
  (testing "without :load-ns the class loads"
    (is (class?
         (eval-in-new-ns '(closed-record.core/defclosedrecord P [a]))))))

;;; Expansion-time argument errors (acceptance 5)

(defn- expansion-cause
  "The class of the cause of the error of `macroexpand-1` of `form`."
  [form]
  (some-> (thrown #(binding [*ns* (the-ns 'closed-record.defclosedrecord-test)]
                     (macroexpand-1 form)))
          ex-cause
          class))

(deftest argument-errors-test
  (testing "reserved field names"
    (doseq [n '[__meta __extmap __hash __hasheq]]
      (is (= AssertionError
             (expansion-cause (list 'defclosedrecord 'T ['a n]))))))
  (testing "duplicate field names"
    (is (= AssertionError (expansion-cause '(defclosedrecord T [a a])))))
  (testing "mutable field metadata"
    (is (= IllegalArgumentException
           (expansion-cause '(defclosedrecord T [^:volatile-mutable a]))))
    (is (= IllegalArgumentException
           (expansion-cause '(defclosedrecord T [^:unsynchronized-mutable a])))))
  (testing "unsupported option"
    (is (= IllegalArgumentException
           (expansion-cause '(defclosedrecord T [a] :foo 1))))))
