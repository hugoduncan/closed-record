(ns closed-record.value-type-option-test
  "VT-V1 in the ordinary suite: the `:value-type` option on a class path
  without value-type."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.expansion-fixture :as fixture]))

(deftest ordinary-expansion-test
  ;; VT-I2: the ordinary expansion equals the expansion fixture.
  ;; The `:value-type false` assertion comes with the option (slice 2).
  (let [recorded (fixture/fixture)]
    (is (= (set (keys fixture/forms)) (set (keys recorded))))
    (doseq [[k form] fixture/forms]
      (testing (str k " with no option")
        (is (= (recorded k) (fixture/expansion form)))))))
