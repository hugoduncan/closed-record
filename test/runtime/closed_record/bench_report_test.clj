(ns closed-record.bench-report-test
  "V-11: the pure layer of the benchmarks (`closed-record.bench.report`).
  The tests inject `last-bench`-shaped data, stats maps and child results. They run no
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
      (is (= "1.050" (nth (report/row "op" p r 5) 3)))
      (is (not (report/pass? p r))))))

(deftest row-test
  (testing "column order, 2 decimals for ns, 3 decimals for P / R, result text"
    (is (= ["(:a x)" "1.23 [1.20, 1.30]" "1.00 [0.99, 1.01]" "1.234" "fail" "5"]
           (report/row "(:a x)" (stats 1.234 1.2 1.3) (stats 1.0 0.99 1.01) 5)))
    (is (= ["(hash x)" "1.00 [0.90, 1.10]" "1.00 [0.90, 1.10]" "1.000" "pass" "5"]
           (report/row "(hash x)" (stats 1.0 0.9 1.1) (stats 1.0 0.9 1.1) 5)))))

(defn- cells
  "Return the trimmed, non-blank cells of a table line split on `|`."
  [line]
  (->> (str/split line #"\|") (map str/trim) (remove str/blank?) vec))

(deftest table-test
  (let [rows   [(report/row "(:a x)" (stats 1.0 0.9 1.1) (stats 1.0 0.9 1.1) 5)
                (report/row "(= x y)" (stats 2.0 1.9 2.1) (stats 1.0 0.9 1.1) 5)]
        parsed (map cells (str/split-lines (report/table rows)))]
    (testing "one data line for each operation, cells equal to its row, in input order"
      (is (= rows (filter (set rows) parsed))))
    (testing "the header ends with the Forks column"
      (is (= "Forks" (last (first parsed)))))))

(deftest exit-code-test
  (let [ok  ["(:a x)" (stats 1.0 0.9 1.1) (stats 1.0 0.9 1.1)]
        bad ["(= x y)" (stats 2.0 1.9 2.1) (stats 1.0 0.9 1.1)]]
    (testing "0 when all operations pass"
      (is (= 0 (report/exit-code [ok ok]))))
    (testing "1 when one operation fails"
      (is (= 1 (report/exit-code [ok bad ok]))))
    (testing "accepts [op p r n] entries"
      (is (= 0 (report/exit-code [(conj ok 5)])))
      (is (= 1 (report/exit-code [(conj ok 5) (conj bad 5)]))))))

(defn- child-result
  "Return a valid child result: P mean `pm` and R mean `rm` for each operation."
  [pm rm]
  (mapv (fn [op] [op (stats pm (- pm 0.1) (+ pm 0.1)) (stats rm (- rm 0.1) (+ rm 0.1))])
        report/ops))

(deftest encode-decode-test
  (testing "round trip"
    (let [r (child-result 1.5 2.5)]
      (is (= {:result r} (report/decode-result (report/encode-result r))))))
  (testing "invalid content gives an :error value, not an exception"
    (let [ok  (child-result 1.0 1.0)
          bad (fn [text] (:error (report/decode-result text)))]
      (is (bad ""))
      (is (str/starts-with? (bad "[1") "invalid EDN"))
      (is (bad (report/encode-result (pop ok))))
      (is (= "entry 1: operation is \"(.a x)\", expected \"(get x :a)\""
             (bad (report/encode-result (assoc ok 1 (nth ok 2) 2 (nth ok 1))))))
      (is (bad (report/encode-result (update-in ok [0 1] dissoc :low))))
      (is (bad (report/encode-result (assoc-in ok [0 2 :mean] "1.0"))))
      (is (bad (report/encode-result (assoc ok 0 ["(:a x)" (stats 1 1 1)])))))))

(defn- close? [a b] (< (Math/abs (- (double a) (double b))) 1e-9))

(deftest fork-stats-test
  (let [results (mapv #(child-result % 2.0) [1.0 2.0 3.0 4.0 5.0])
        out     (report/fork-stats results)
        ;; s = sqrt(10/4); h = 2.776 * s / sqrt(5) = 2.776 * sqrt(0.5)
        h       (* 2.776 (Math/sqrt 0.5))]
    (testing "one [op p r n] entry for each operation, in ops order"
      (is (= report/ops (mapv first out)))
      (is (every? #(= 5 (nth % 3)) out)))
    (testing "mean and t interval of the fork means"
      (let [[_ p r] (first out)]
        (is (close? 3.0 (:mean p)))
        (is (close? (- 3.0 h) (:low p)))
        (is (close? (+ 3.0 h) (:high p)))
        (is (= {:mean 2.0 :low 2.0 :high 2.0} r))))
    (testing "needs fork-count results"
      (is (thrown? AssertionError (report/fork-stats (pop results)))))))
