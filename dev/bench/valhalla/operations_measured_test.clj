(ns valhalla.operations-measured-test
  (:require [clojure.test :refer [deftest is]]
            [criterium.measured :as measured]
            [valhalla.reset-measured :as reset-measured])
  (:import Operations))

(defn- invoke [operation]
  (second (measured/invoke operation (measured/args operation) 2)))

(deftest measured-array-operations-test
  ;; Test the actual generated operation result, not a timing threshold.
  (doseq [length [4 1024]]
    (let [initial (Operations/point 17)
          replacement (Operations/point 23)
          value-array (invoke (measured/expr (Operations/valueArray length initial)))
          reference-array (invoke (measured/expr (Operations/referenceArray length initial)))]
      (doseq [array [value-array reference-array]]
        (is (= length (alength array)))
        (is (= 17 (invoke (measured/expr (Operations/arrayRead array 0)))))
        (let [operation (reset-measured/reset-measured
                         (measured/expr (Operations/arrayStore array 0 replacement))
                         (fn [_] (Operations/arrayStore array 0 initial)))]
          (is (identical? array (invoke operation)))
          (is (= 23 (Operations/arrayRead array 0)))
          (is (every? #(= 17 (Operations/arrayRead array %)) (range 1 length))))))))

(deftest measured-field-operations-test
  ;; Construction and replacement share the constructor, with distinct inputs.
  (let [initial (Operations/point 17)
        replacement (Operations/point 23)
        restricted (invoke (measured/expr (Operations/restrictedHolder initial)))
        reference (invoke (measured/expr (Operations/referenceHolder initial)))
        restricted-result (invoke (measured/expr (Operations/restrictedHolder replacement)))
        reference-result (invoke (measured/expr (Operations/referenceHolder replacement)))]
    (is (= 17 (invoke (measured/expr (Operations/restrictedRead restricted)))))
    (is (= 17 (invoke (measured/expr (Operations/referenceRead reference)))))
    (is (= 23 (Operations/restrictedRead restricted-result)))
    (is (= 23 (Operations/referenceRead reference-result)))
    (is (not (identical? restricted restricted-result)))
    (is (not (identical? reference reference-result)))))
