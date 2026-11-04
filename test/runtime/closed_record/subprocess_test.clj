(ns closed-record.subprocess-test
  "The time limit of `closed-record.subprocess/run` and `delete-tree`."
  (:require
   [clojure.java.io :as io]
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

(deftest delete-tree-test
  ;; A temporary directory and all of its contents are deleted.
  (let [dir (subprocess/temp-dir "closed-record-delete-tree")
        file (io/file dir "a" "b" "c.txt")]
    (io/make-parents file)
    (spit file "x")
    (is (.exists file))
    (subprocess/delete-tree dir)
    (is (not (.exists dir)))))
