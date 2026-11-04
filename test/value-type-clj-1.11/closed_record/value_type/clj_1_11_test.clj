(ns closed-record.value-type.clj-1-11-test
  "VT-V1 on Clojure 1.11 with value-type on the class path: value-type needs
  Clojure 1.12, so each `:value-type true` expansion, not only the first,
  gives `:closed-record/value-type-unavailable` with a cause."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core]
   [closed-record.probe-support :refer [expansion-error]]))

(deftest value-type-unavailable-test
  (is (= {:major 1 :minor 11}
         (select-keys *clojure-version* [:major :minor])))
  (doseq [n ['A 'B 'C]]
    (testing (str n)
      (let [e (expansion-error
               'closed-record.value-type.clj-1-11-test
               (list 'closed-record.core/defclosedrecord n '[x]
                     :value-type true))]
        (is (= {:type :closed-record/value-type-unavailable
                :class-name (symbol (str "closed_record.value_type.clj_1_11_test."
                                         n))}
               (ex-data e)))
        (is (some? (ex-cause e)))))))
