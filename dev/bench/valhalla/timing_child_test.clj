(ns valhalla.timing-child-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.timing-budget :as budget]
            [closed-record.bench.timing-tool :as tool]
            [closed-record.bench.transport :as transport]
            [valhalla.timing-child :as child])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest saved-collection-test
  ;; Real collection preserves all experimental rows and observed provenance.
  (let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        directory (Files/createTempDirectory "experimental-timing" (make-array FileAttribute 0))
        path (.resolve directory "child.edn")
        association {:run-id "experimental-smoke" :revision "test-revision"
                     :configuration :valhalla :fork 1}]
    (try
      (testing "invalid inputs fail before publication"
        (doseq [[identity seconds] [[(assoc association :fork 0) 1]
                                    [(assoc association :configuration :ordinary) 1]
                                    [association 0]]]
          (is (thrown? clojure.lang.ExceptionInfo
                       (child/write-results! (str path) inventory identity seconds)))
          (is (not (.exists (.toFile path))))))
      (let [envelope (child/write-results! (str path) inventory association 1)
            saved (transport/decode inventory association (slurp (str path)))
            declaration (tool/observe)]
        (is (= envelope saved))
        (is (= 18 (count (:results saved))))
        (is (map? (:runtime saved)))
        (doseq [row (:results saved)]
          (is (= row (budget/validate-observation {:limit-time-s 1.0} row)))
          (is (= declaration (:timing-tool row)))
          (is (pos? (:raw row)))
          (is (pos-int? (:operations row)))))
      (finally
        (Files/deleteIfExists path)
        (Files/delete directory)))))
