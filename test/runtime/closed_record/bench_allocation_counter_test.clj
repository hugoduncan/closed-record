(ns closed-record.bench-allocation-counter-test
  (:require [clojure.test :refer [deftest is testing]]
            [closed-record.bench.allocation :as allocation]
            [closed-record.bench.allocation-counter :as counter]))

(deftest real-thread-counter-test
  ;; Check actual escaping allocation without asserting unstable boundary cost.
  (let [bean (counter/prepare!)
        thread-id (counter/current-thread-id)]
    (testing "escaping payload bytes are included without overhead subtraction"
      (counter/read-bytes bean thread-id)
      (let [before (counter/read-bytes bean thread-id)
            payload (byte-array 65536)
            after (counter/read-bytes bean thread-id)
            result (allocation/outcome before after 1)]
        (is (= 65536 (alength payload)))
        (is (<= 65536 (:raw result)))
        (is (= before (:counter-before result)))
        (is (= after (:counter-after result)))))
    (testing "a different real thread cannot continue the interval"
      (let [result (promise)
            worker (Thread.
                    ^Runnable
                    (fn []
                      (deliver result
                               (try
                                 (counter/read-bytes bean thread-id)
                                 :unexpected-success
                                 (catch clojure.lang.ExceptionInfo exception
                                   (ex-data exception))))))]
        (.start worker)
        (.join worker)
        (is (= thread-id (:expected-thread-id @result)))
        (is (integer? (:actual-thread-id @result)))
        (is (not= thread-id (:actual-thread-id @result)))))))
