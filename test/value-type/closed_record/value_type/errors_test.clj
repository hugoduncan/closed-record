(ns closed-record.value-type.errors-test
  "VT-V6: value-type errors pass through closed-record unchanged. The first
  cause that is not a `Compiler$CompilerException` must be the value-type
  `ex-info`: the same class, message and `ex-data` as the error of the
  `defvaluetype*` form alone."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core]
   [closed-record.value-type.expectation :as expectation])
  (:import
   (clojure.lang Compiler$CompilerException ExceptionInfo)))

(def ^:private expected (expectation/representation))

(defn- first-cause
  "The first exception in the cause chain of `t` that is not a
  `Compiler$CompilerException`."
  [t]
  (first (remove #(instance? Compiler$CompilerException %)
                 (take-while some? (iterate ex-cause t)))))

(defn- thrown
  "The `first-cause` of the evaluation of `form`, or nil."
  [form]
  (try (eval form) nil (catch Throwable t (first-cause t))))

(defn- value-type-form
  "The `defvaluetype*` form of the expansion of `form`."
  [form]
  (some #(when (and (seq? %) (= 'value-type.core/defvaluetype* (first %))) %)
        (macroexpand-1 form)))

(defn- errors
  "The `:closed-record` error of the form that `text` gives and the
  `:value-type` error of its `defvaluetype*` form alone, in this namespace.
  The form is text because a quoted constant loses the metadata of its
  symbols."
  [text]
  (binding [*ns* (the-ns 'closed-record.value-type.errors-test)]
    (let [form (read-string text)
          closed (thrown form)]
      {:closed-record closed
       :value-type (thrown (value-type-form form))})))

(defn- check-pass-through
  "Assert that the closed-record error is the value-type error with
  `reason`, unchanged."
  [text reason]
  (let [{:keys [closed-record value-type]} (errors text)]
    (is (instance? ExceptionInfo closed-record))
    (is (= {:value-type/error :invalid-field :reason reason}
           (select-keys (ex-data closed-record) [:value-type/error :reason])))
    (is (= (class value-type) (class closed-record)))
    (is (= (ex-message value-type) (ex-message closed-record)))
    (is (= (ex-data value-type) (ex-data closed-record)))))

(deftest non-nil-field-test
  (check-pass-through "(closed-record.core/defclosedrecord NN [^:non-nil a]
                        :value-type true)"
                      :non-nil-unsupported))

(deftest nil-restricted-field-test
  (check-pass-through "(closed-record.core/defclosedrecord NR [^:nil-restricted a]
                        :value-type true)"
                      :removed-nil-restricted))

(def ^:private access-form
  "(closed-record.core/defclosedrecord HA [a] :value-type true
     Object (toString [_] (str __hash)))")

(deftest removed-field-access-test
  ;; Value output only: the capture loader defines the class.
  (if (= :value expected)
    (check-pass-through access-form :removed-field-access)
    (testing "identity output keeps the hash fields"
      (is (nil? (:closed-record (errors access-form)))))))
