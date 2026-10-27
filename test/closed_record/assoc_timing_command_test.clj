(ns closed-record.assoc-timing-command-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-timing-command :as command]
            [closed-record.bench.runtime :as runtime]))

(deftest timing-command-test
  ;; Command construction requires no measurement tools and preserves argument boundaries.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        association {:run-id "timing run" :revision "revision"
                     :configuration :ordinary :fork 2}
        observed (runtime/observe)
        prefix (into [(str (io/file (:java-home observed) "bin" "java"))]
                     (:flags observed))]
    (testing "valid numeric budgets use the child CLI decimal format"
      (doseq [budget [1 1.5 1/2]]
        (is (= (into prefix
                     ["-cp" (System/getProperty "java.class.path")
                      "clojure.main" "-m" "closed-record.bench.assoc-timing-child"
                      "result path.edn" "timing run" "revision" "2"
                      (str (double budget))])
               (command/command inventory association "result path.edn" budget)))))
    (testing "undefined and unrepresentable budgets fail before launch"
      (doseq [budget [nil "1" 0 -1 Double/NaN Double/POSITIVE_INFINITY
                      Double/NEGATIVE_INFINITY 1E-1000M]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (command/command inventory association "unused" budget)))))
    (testing "invalid associations fail before launch"
      (doseq [changed [(assoc association :fork 0)
                       (assoc association :configuration :unknown)
                       (dissoc association :revision)]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (command/command inventory changed "unused" 1)))))))
