(ns valhalla.operations-test
  (:require [clojure.test :refer [deftest is testing]])
  (:import Operations
           [jdk.internal.value ValueClass]))

(deftest array-operations-test
  ;; Check all elements and source identity without a timed diagnostic branch.
  (doseq [length [4 1024]
          flat? [true false]]
    (testing (str "array layout and operations " length " " flat?)
      (let [initial (Operations/point 17)
            replacement (Operations/point 23)
            array (if flat?
                    (Operations/valueArray length initial)
                    (Operations/referenceArray length initial))]
        (is (= flat? (ValueClass/isFlatArray array)))
        (is (= length (alength array)))
        (is (every? #(= 17 (Operations/arrayRead array %)) (range length)))
        (dotimes [_ 2]
          (is (identical? array (Operations/arrayStore array 0 replacement)))
          (is (= 23 (Operations/arrayRead array 0)))
          (is (every? #(= 17 (Operations/arrayRead array %)) (range 1 length)))
          (Operations/arrayStore array 0 initial)
          (is (= 17 (Operations/arrayRead array 0))))))))

(deftest field-operations-test
  ;; Replacement is a fresh holder, not mutation or a read of the source.
  (doseq [[construct read-value] [[#(Operations/restrictedHolder %) #(Operations/restrictedRead %)]
                                 [#(Operations/referenceHolder %) #(Operations/referenceRead %)]]]
    (let [initial (Operations/point 17)
          replacement (Operations/point 23)
          source (construct initial)
          result (construct replacement)]
      (is (not (identical? source result)))
      (is (= 17 (read-value source)))
      (is (= 23 (read-value result))))))
