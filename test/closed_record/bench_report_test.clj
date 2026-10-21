(ns closed-record.bench-report-test
  "V-11: the pure layer of the benchmarks (`closed-record.bench.report`).
  The tests inject `last-bench`-shaped data and stats maps. They run no
  benchmark and need no criterium. Run with the suite:
  `clojure -M:test -m scry.cli`."
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.report :as report]))

(defn- bench-data
  "Return `last-bench`-shaped data with the batch size, point estimate and
  quantiles (in the given order)."
  [batch point quantiles]
  {:data {:bootstrap-stats
          {:batch-size batch
           :bootstrap  {:elapsed-time
                        {:mean {:point-estimate     point
                                :estimate-quantiles quantiles}}}}}})

(defn- stats [mean low high]
  {:mean mean :low low :high high})

(deftest per-call-stats-test
  (testing "divides the mean and both quantiles by the batch size"
    (is (= {:mean 2.0 :low 1.5 :high 3.0}
           (report/per-call-stats
            (bench-data 100 200.0 [{:value 150.0 :alpha 0.025}
                                   {:value 300.0 :alpha 0.975}])))))
  (testing "low is alpha 0.025 and high is alpha 0.975, in any order"
    (is (= {:mean 2.0 :low 1.5 :high 3.0}
           (report/per-call-stats
            (bench-data 100 200.0 [{:value 300.0 :alpha 0.975}
                                   {:value 150.0 :alpha 0.025}]))))))

(deftest pass?-test
  (testing "ratio at most 1.05 passes"
    (is (report/pass? (stats 1.05 1.05 1.05) (stats 1.0 1.0 1.0))))
  (testing "ratio above 1.05 with no overlap fails"
    (is (not (report/pass? (stats 1.2 1.15 1.25) (stats 1.0 0.95 1.05)))))
  (testing "ratio above 1.05 with overlap passes"
    (is (report/pass? (stats 1.2 1.0 1.4) (stats 1.0 0.9 1.1))))
  (testing "inclusive overlap edge, P low = R high, passes"
    (is (report/pass? (stats 1.2 1.1 1.3) (stats 1.0 0.9 1.1))))
  (testing "faster P passes"
    (is (report/pass? (stats 0.5 0.45 0.55) (stats 1.0 0.95 1.05))))
  (testing "uses unrounded values: ratio prints 1.050 but is above 1.05"
    (let [p (stats 1.0504 1.0503 1.0505)
          r (stats 1.0 0.99 1.01)]
      (is (= "1.050" (nth (report/row "op" p r) 3)))
      (is (not (report/pass? p r))))))

(deftest row-test
  (testing "column order, 2 decimals for ns, 3 decimals for P / R, result text"
    (is (= ["(:a x)" "1.23 [1.20, 1.30]" "1.00 [0.99, 1.01]" "1.234" "fail"]
           (report/row "(:a x)" (stats 1.234 1.2 1.3) (stats 1.0 0.99 1.01))))
    (is (= ["(hash x)" "1.00 [0.90, 1.10]" "1.00 [0.90, 1.10]" "1.000" "pass"]
           (report/row "(hash x)" (stats 1.0 0.9 1.1) (stats 1.0 0.9 1.1))))))

(defn- cells
  "Return the trimmed, non-blank cells of a table line split on `|`."
  [line]
  (->> (str/split line #"\|") (map str/trim) (remove str/blank?) vec))

(deftest table-test
  (let [rows   [(report/row "(:a x)" (stats 1.0 0.9 1.1) (stats 1.0 0.9 1.1))
                (report/row "(= x y)" (stats 2.0 1.9 2.1) (stats 1.0 0.9 1.1))]
        parsed (map cells (str/split-lines (report/table rows)))]
    (testing "one data line for each operation, cells equal to its row, in input order"
      (is (= rows (filter (set rows) parsed))))))

(deftest exit-code-test
  (let [ok  ["(:a x)" (stats 1.0 0.9 1.1) (stats 1.0 0.9 1.1)]
        bad ["(= x y)" (stats 2.0 1.9 2.1) (stats 1.0 0.9 1.1)]]
    (testing "0 when all operations pass"
      (is (= 0 (report/exit-code [ok ok]))))
    (testing "1 when one operation fails"
      (is (= 1 (report/exit-code [ok bad ok]))))))
