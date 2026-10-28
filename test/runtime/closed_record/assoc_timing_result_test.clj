(ns closed-record.assoc-timing-result-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-timing-result :as result]))

(deftest partial-timing-results-test
  ;; Partial timing success requires coverage and provenance, not just valid EDN.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        association {:run-id "test" :revision "revision"
                     :configuration :ordinary :fork 1}
        tool {:library 'org.hugoduncan/criterium
              :base-version "0.5.251-ALPHA" :distribution :upstream
              :collector-sha256 "a73da4deaa664aa68ddbeb0989e7dcb22a5878864a720b662c00aa9ab1557fa2"}
        budget {:limit-time-s 1.0}
        rows (vec (for [case-id [:assoc/width-3 :assoc/width-16]
                        representation [:closed-record :defrecord]]
                    {:case-id case-id :representation representation
                     :configuration :ordinary :fork 1 :metric :time
                     :status :measured :unit :ns/op :raw 100.0 :operations 20
                     :timing-tool (assoc tool :resource "file:/child/collect.clj")
                     :timing-budget budget}))
        validate #(result/validate-results inventory association tool budget %)]
    (testing "raw rows and child-local paths remain unchanged"
      (is (= rows (validate (edn/read-string (pr-str rows))))))
    (testing "coverage, status, counts, and per-row provenance are mandatory"
      (doseq [invalid [[] (pop rows) (conj rows (first rows))
                       (assoc-in rows [0 :operations] 0)
                       (assoc-in rows [0 :case-id] :unknown)
                       (assoc-in rows [0 :timing-tool] nil)
                       (assoc-in rows [0 :timing-budget] {:limit-time-s 2.0})
                       (assoc rows 0 (-> (first rows)
                                         (dissoc :raw :operations)
                                         (assoc :status :unavailable :reason "unsupported")))]]
        (is (thrown? clojure.lang.ExceptionInfo (validate invalid)))))
    (testing "missing declarations fail even without rows"
      (is (thrown? clojure.lang.ExceptionInfo
                   (result/validate-results inventory association nil budget [])))
      (is (thrown? clojure.lang.ExceptionInfo
                   (result/validate-results inventory association tool nil []))))))
