(ns closed-record.assoc-graph-result-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.assoc-graph-result :as result]
            [closed-record.bench.graph :as graph]))

(deftest partial-graph-results-test
  ;; Pure evidence checks need no agent and must not accept inconsistent roots.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        association {:run-id "test" :revision "revision"
                     :configuration :ordinary :fork 1}
        tool {:jar-sha256 (apply str (repeat 64 "a"))
              :class-sha256 (apply str (repeat 64 "b"))}
        rows (vec (for [case-id [:assoc/width-3 :assoc/width-16]
                        representation [:closed-record :defrecord]
                        outcome (graph/outcomes 64 64 104)]
                    (merge outcome {:case-id case-id :representation representation
                                    :configuration :ordinary :fork 1
                                    :graph-tool (assoc tool :location "file:/child/agent.jar")})))
        validate #(result/validate-results inventory association tool %)]
    (testing "complete raw evidence preserves order and process-local locations"
      (is (= rows (validate (edn/read-string (pr-str rows)))))
      (is (= (vec (reverse rows)) (validate (vec (reverse rows))))))
    (testing "coverage, status, identity, hashes, and counts are mandatory"
      (doseq [invalid [[] (pop rows) (conj rows (first rows))
                       (assoc-in rows [0 :case-id] :unknown)
                       (assoc-in rows [0 :fork] 2)
                       (assoc-in rows [0 :root] :unknown)
                       (assoc-in rows [0 :raw] -1)
                       (assoc-in rows [0 :raw] 65)
                       (assoc-in rows [0 :source-bytes] 65)
                       (assoc-in rows [0 :union-bytes] 200)
                       (update rows 0 dissoc :result-bytes)
                       (assoc-in rows [0 :graph-tool] nil)
                       (assoc-in rows [0 :graph-tool :jar-sha256] (:class-sha256 tool))
                       (assoc-in rows [0 :graph-tool :class-sha256] (:jar-sha256 tool))
                       (assoc rows 0 (-> (first rows) (dissoc :raw)
                                         (assoc :status :unavailable :reason "unsupported")))]]
        (is (thrown? clojure.lang.ExceptionInfo (validate invalid)) (pr-str invalid))))
    (testing "declarations and ordinary configuration are required"
      (is (thrown? clojure.lang.ExceptionInfo
                   (result/validate-results inventory association nil [])))
      (is (thrown? clojure.lang.ExceptionInfo
                   (result/validate-results inventory (assoc association :configuration :valhalla) tool rows))))))
