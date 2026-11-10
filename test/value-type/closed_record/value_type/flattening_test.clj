(ns closed-record.value-type.flattening-test
  "VT-V12: arrays of small `:value-type true` records are flat on value
  output (VT-K8). Defined only when the expected representation is
  `:value`. It does not skip: without the `jdk.internal.value` export, or
  with `value-type.value-classes=off`, it fails."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]
   [closed-record.value-type.expectation :as expectation]
   [closed-record.value-type.support :as support]))

(defclosedrecord L [^long x] :value-type true)
(defclosedrecord II [^int x ^int y] :value-type true)

(when (= :value (expectation/representation))
  (deftest flattening-test
    (let [into-array (requiring-resolve 'value-type.array/into-array)
          array-info (requiring-resolve 'value-type.array/array-info)]
      (doseq [[c fields r] [[L #{"x"} (->L 1)]
                            [II #{"x" "y"} (->II 1 2)]]]
        (testing (.getSimpleName ^Class c)
          (is (= fields (support/instance-field-names c)))
          (let [info (array-info (into-array c [r]))]
            (is (true? (:flat? info)) info)))))))
