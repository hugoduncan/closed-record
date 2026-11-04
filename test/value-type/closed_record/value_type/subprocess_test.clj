(ns closed-record.value-type.subprocess-test
  "Checks that need their own JVM: VT-V1 (no option and `:value-type false`
  do not load value-type) and VT-V5 (the `error` fallback policy)."
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is]]
   [closed-record.subprocess :as subprocess]
   [closed-record.value-type.expectation :as expectation]))

(def ^:private expected (expectation/representation))

(deftest does-not-load-value-type-test
  ;; VT-V1: value-type is on the class path but not loaded.
  (let [script (pr-str
                '(do
                   (require 'closed-record.core)
                   (in-ns 'my-app.geo)
                   (clojure.core/refer-clojure)
                   (eval '(closed-record.core/defclosedrecord P [x]))
                   (eval '(closed-record.core/defclosedrecord F [x]
                            :value-type false))
                   (prn {:p (:x (eval '(->P 1)))
                         :f (:x (eval '(->F 2)))
                         :loaded (contains? (loaded-libs) 'value-type.core)})))
        {:keys [exit out err]} (subprocess/run ["-e" script])]
    (is (= 0 exit) err)
    (is (= {:p 1 :f 2 :loaded false} (edn/read-string out)))))

(when (= :identity expected)
  ;; VT-V5 applies to identity output only: with value output the policy
  ;; does not apply. scry fails a test with no assertion, so the test is
  ;; defined only for identity output.
  (deftest error-policy-test
    ;; VT-V5: on a JVM without value classes, the `error` policy makes the
    ;; definition throw value-type's `Error`.
    (let [script (pr-str
                  '(do
                     (require 'closed-record.core)
                     (in-ns 'my-app.geo)
                     (clojure.core/refer-clojure)
                     (prn (try
                            (eval '(closed-record.core/defclosedrecord P [x]
                                     :value-type true))
                            {:defined true}
                            (catch Throwable t
                              {:causes
                               (mapv #(.getName (class %))
                                     (take-while some?
                                                 (iterate ex-cause t)))})))))
          {:keys [exit out err]} (subprocess/run
                                  ["-e" script]
                                  :jvm-opts ["-Dvalue-type.fallback=error"])]
      (is (= 0 exit) err)
      (is (= "java.lang.Error" (peek (:causes (edn/read-string out))))
          (str "the error policy does not apply: " out)))))
