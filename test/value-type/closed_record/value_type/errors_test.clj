(ns closed-record.value-type.errors-test
  "VT-V6: value-type errors pass through closed-record unchanged. The first
  cause that is not a `Compiler$CompilerException` must be the value-type
  `ex-info`: the same class, message and `ex-data` as the error of the
  `defvaluetype*` form alone."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core]
   [closed-record.value-type.expectation :as expectation]
   [closed-record.value-type.support :as support])
  (:import
   (clojure.lang Compiler$CompilerException ExceptionInfo)))

(def ^:private expected (expectation/representation))

(defn- first-cause
  "The first exception in the cause chain of `t` that is not a
  `Compiler$CompilerException`."
  [t]
  (first (remove #(instance? Compiler$CompilerException %)
                 (take-while some? (iterate ex-cause t)))))

(defn- eval-error
  "The `first-cause` of the evaluation of `form`, or nil."
  [form]
  (try (eval form) nil (catch Throwable t (first-cause t))))

(defn- errors
  "The `:closed-record` error of the form that `text` gives and the
  `:value-type` error of its `defvaluetype*` form alone, in this namespace.
  The form is text because a quoted constant loses the metadata of its
  symbols."
  [text]
  (binding [*ns* (the-ns 'closed-record.value-type.errors-test)]
    (let [form (read-string text)
          vt-form (support/type-form (macroexpand-1 form))]
      {:closed-record (eval-error form)
       :value-type-form vt-form
       :value-type (when vt-form (eval-error vt-form))})))

(defn- check-pass-through
  "Assert that the closed-record error is the value-type error with
  `reason`, unchanged."
  [text reason]
  (let [{:keys [closed-record value-type value-type-form]} (errors text)]
    (is (some? value-type-form) "the expansion has a defvaluetype* form")
    (is (instance? ExceptionInfo closed-record))
    (is (= {:value-type/error :invalid-field :reason reason}
           (select-keys (ex-data closed-record) [:value-type/error :reason])))
    (is (= (class value-type) (class closed-record)))
    (is (= (ex-message value-type) (ex-message closed-record)))
    (is (= (ex-data value-type) (ex-data closed-record)))))

(deftest non-nil-field-test
  (check-pass-through "(closed-record.core/defclosedrecord NN [^:non-nil a]
                        :value-type true)"
                      :missing-tag))

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
