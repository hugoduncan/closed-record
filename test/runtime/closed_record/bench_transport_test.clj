(ns closed-record.bench-transport-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.transport :as transport]))

(deftest raw-evidence-transport-test
  ;; Transport must preserve raw evidence and reject evidence from another child.
  (let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        case-spec (first (:cases inventory))
        configuration (:configuration case-spec)
        contract (get-in inventory [:contracts (:contract case-spec)])
        expected {:run-id "run-019" :revision "abc123"
                  :configuration configuration :fork 1}
        outcome {:case-id (:id case-spec) :configuration configuration
                 :representation (first (or (:representations contract)
                                            (get-in inventory [:configurations configuration :representations])))
                 :fork 1 :metric :time :unit :ns/op
                 :status :measured :raw 123 :operations 7}
        envelope (assoc expected :schema-version 1 :results [outcome])]
    (testing "raw counts survive without derived values"
      (is (= envelope (transport/decode inventory expected
                                        (transport/encode inventory expected envelope))))
      (is (= [] (:results (transport/decode inventory expected
                                            (pr-str (assoc envelope :results [])))))))
    (testing "failure evidence retains its reason"
      (let [failed (assoc envelope :results [(-> outcome
                                                 (dissoc :raw :operations)
                                                 (assoc :status :failed :reason "counter unavailable"))])]
        (is (= failed (transport/decode inventory expected (pr-str failed))))))
    (testing "wrong association, schema, duplicates and metric contracts fail"
      (doseq [invalid [(assoc envelope :run-id "other")
                       (assoc envelope :revision "other")
                       (assoc envelope :configuration :unknown)
                       (assoc envelope :fork 2)
                       (assoc envelope :schema-version 2)
                       (assoc envelope :results [outcome outcome])
                       (assoc-in envelope [:results 0 :fork] 2)
                       (assoc-in envelope [:results 0 :unit] :bytes)
                       (assoc-in envelope [:results 0 :case-id] :unknown)]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (transport/decode inventory expected (pr-str invalid))))))
    (testing "only one complete safe EDN form is accepted"
      (doseq [text ["" "[" "#=(System/exit 0)" "#unknown {}"
                    (str (pr-str envelope) " nil")]]
        (is (thrown? Exception (transport/decode inventory expected text)))))))
