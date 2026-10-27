(ns closed-record.timing-tool-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.timing-tool :as tool]))

(deftest collector-provenance-test
  ;; Check actual classpath bytes, not a caller declaration of the tool version.
  (testing "the selected collector has a traceable identity"
    (let [observed (tool/observe)]
      (is (= 'org.hugoduncan/criterium (:library observed)))
      (is (= "0.5.251-ALPHA" (:base-version observed)))
      (is (= (str (io/resource "criterium/collect.clj")) (:resource observed)))
      (with-open [stream (io/input-stream (io/resource "criterium/collect.clj"))]
        (is (= (dissoc observed :resource)
               (tool/identify (.readAllBytes stream)))))
      (case (:distribution observed)
        :upstream (is (nil? (:correction observed)))
        :local-correction
        (do
          (is (= :positive-planning-duration-v1 (:correction observed)))
          (is (= "c8b1b7bb44877a6594a54f446d550810e0820bb1f78e4749e106f87507423f37"
                 (:patch-sha256 observed)))))))
  (testing "unknown collector bytes cannot claim the correction"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown Criterium"
                          (tool/identify (.getBytes "unknown" "UTF-8"))))))
