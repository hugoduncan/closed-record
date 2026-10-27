(ns closed-record.bench-legacy-transport-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.legacy-transport :as transport]
            [closed-record.bench.report :as report]
            [closed-record.bench-timing-tool-test :refer [upstream corrected]]))

(deftest original-statistics-provenance-test
  ;; Pure evidence values verify provenance without loading Criterium.
  (let [stats {:mean 2.0 :low 1.0 :high 3.0}
        rows (mapv #(vector % stats stats) report/ops)]
    (doseq [identity [upstream corrected]]
      (let [observed (assoc identity :resource "file:/child/collect.clj")
            text (transport/encode rows observed)
            envelope (edn/read-string text)]
        (testing "statistics and child resource survive unchanged"
          (is (= {:result rows :timing-tool observed}
                 (transport/decode text (assoc identity :resource "file:/parent/collect.clj")))))
        (testing "missing or invalid evidence rejects rather than losing provenance"
          (doseq [invalid [(dissoc envelope :timing-tool)
                           (assoc envelope :schema-version 2)
                           (assoc envelope :results (pop rows))
                           (assoc envelope :timing-tool {})]]
            (is (string? (:error (transport/decode (pr-str invalid) identity)))))
          (is (:error (transport/decode text nil)))
          (is (:error (transport/decode text (if (= identity upstream) corrected upstream)))))))
    (is (:error (transport/decode "[" upstream)))
    (is (thrown? clojure.lang.ExceptionInfo (transport/encode rows nil)))))
