(ns closed-record.timing-launcher-test
  (:require [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]))

(deftest corrected-launcher-test
  ;; Exercise dependency resolution, preparation, and a fresh JVM without timing.
  (testing "the standard launcher selects the correction before loading Criterium"
    (let [result (shell/sh
                  "sh" "dev/bench/timing/run.sh" "-e"
                  (str "(require '[closed-record.bench.timing-tool :as tool])"
                       "(assert (= :local-correction (:distribution (tool/observe))))"
                       "(require 'criterium.collect)"
                       "(println :corrected-launcher-ok)"))]
      (is (= 0 (:exit result)) (:err result))
      (is (.contains ^String (:out result) ":corrected-launcher-ok"))
      (is (.contains ^String (:err result) "Verified local Criterium source overlay"))))
  (testing "child failure remains a nonzero command exit"
    (let [result (shell/sh "sh" "dev/bench/timing/run.sh"
                           "-e" "(System/exit 7)")]
      (is (= 7 (:exit result))))))
