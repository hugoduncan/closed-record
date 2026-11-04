(ns closed-record.value-type.errors-test
  "VT-V6: value-type errors pass through closed-record unchanged."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core]
   [closed-record.value-type.expectation :as expectation]))

(def ^:private expected (expectation/representation))

(defn- root-ex-data
  "The first `ex-data` with `:value-type/error` in the cause chain of `t`."
  [t]
  (some #(when (:value-type/error (ex-data %)) (ex-data %))
        (take-while some? (iterate ex-cause t))))

(defn- error-data
  "The value-type error data of the evaluation of the form that `text` gives,
  in this namespace. The form is text because a quoted constant loses the
  metadata of its symbols."
  [text]
  (binding [*ns* (the-ns 'closed-record.value-type.errors-test)]
    (try
      (eval (read-string text))
      nil
      (catch Throwable t (root-ex-data t)))))

(deftest non-nil-field-test
  (is (= {:value-type/error :invalid-field
          :reason :non-nil-unsupported}
         (select-keys
          (error-data "(closed-record.core/defclosedrecord NN [^:non-nil a]
                        :value-type true)")
          [:value-type/error :reason]))))

(deftest nil-restricted-field-test
  (is (= {:value-type/error :invalid-field
          :reason :removed-nil-restricted}
         (select-keys
          (error-data "(closed-record.core/defclosedrecord NR [^:nil-restricted a]
                        :value-type true)")
          [:value-type/error :reason]))))

(def ^:private access-form
  "(closed-record.core/defclosedrecord HA [a] :value-type true
     Object (toString [_] (str __hash)))")

(deftest removed-field-access-test
  ;; Value output only: the capture loader defines the class.
  (if (= :value expected)
    (is (= {:value-type/error :invalid-field
            :reason :removed-field-access}
           (select-keys
            (error-data access-form)
            [:value-type/error :reason])))
    (testing "identity output keeps the hash fields"
      (is (nil? (error-data access-form))))))
