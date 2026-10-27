(ns closed-record.bench-fixtures-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.fixtures :as fixtures]
   [closed-record.core :as closed]))

(deftest matched-assoc-test
  ;; Exercise actual generated types with the fixed inventory, without timing tools.
  (let [{:keys [contracts cases]} (edn/read-string (slurp (io/resource "cases.edn")))]
    (doseq [benchmark-case (filterv #(= :assoc (:contract %)) cases)]
      (testing (str (:id benchmark-case))
        (let [{:keys [closed-record defrecord key replacement]}
              (fixtures/assoc-pair (:assoc contracts) benchmark-case)
              width (:width benchmark-case)
              expected (zipmap (subvec (get-in contracts [:assoc :fields]) 0 width)
                               (subvec (get-in contracts [:assoc :values]) 0 width))
              closed-result (assoc closed-record key replacement)
              record-result (assoc defrecord key replacement)]
          (is (= expected (into {} closed-record) (into {} defrecord)))
          (is (= (assoc expected key replacement)
                 (into {} closed-result) (into {} record-result)))
          (is (= width (count closed-result) (count record-result)))
          (doseq [[source result] [[closed-record closed-result] [defrecord record-result]]]
            (is (= expected (into {} source)))
            (is (not (identical? source result)))
            (is (= (class source) (class result)))
            (is (not= (get source key) (get result key))))
          (doseq [field (remove #{key} (keys expected))]
            (is (identical? (get closed-record field) (get closed-result field)))
            (is (identical? (get defrecord field) (get record-result field)))
            (is (identical? (get closed-record field) (get defrecord field))))
          (is (identical? replacement (get closed-result key)))
          (is (identical? replacement (get record-result key)))
          (is (identical? closed-result (closed/check closed-result))))))))

(deftest unsupported-width-test
  ;; Do not silently use a nearby type for an unknown matrix dimension.
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unsupported assoc width"
                        (fixtures/assoc-pair {} {:width 4}))))
