(ns closed-record.subprocess-test
  "The time limit of `closed-record.subprocess/run`."
  (:require
   [clojure.test :refer [deftest is]]
   [closed-record.subprocess :as subprocess]))

(deftest timeout-test
  ;; A child that does not exit is destroyed after the limit.
  (let [start (System/nanoTime)
        {:keys [exit timed-out err]}
        (subprocess/run ["-e" "(Thread/sleep 600000)"] :timeout-ms 500)
        elapsed-ms (/ (- (System/nanoTime) start) 1e6)]
    (is (nil? exit))
    (is (true? timed-out))
    (is (re-find #"timed out after 500 ms" err))
    (is (< elapsed-ms 60000))))
