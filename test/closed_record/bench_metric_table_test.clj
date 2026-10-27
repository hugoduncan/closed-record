(ns closed-record.bench-metric-table-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.metric-table :as table]))

(defn- evidence [inventory]
  (vec (for [{:keys [id configuration contract]} (:cases inventory)
             :let [spec (get-in inventory [:contracts contract])]
             representation (or (:representations spec)
                                (get-in inventory [:configurations configuration :representations]))
             fork (range 1 6)
             metric (:metrics spec)
             root (if (= :graph metric) (:graph-roots spec) [nil])]
         (cond-> {:case-id id :configuration configuration :representation representation
                  :fork fork :metric metric :unit ({:time :ns/op :allocation :bytes/op :graph :bytes} metric)
                  :status :measured :raw 25}
           root (assoc :root root)
           (not root) (assoc :operations 2)))))

(deftest metric-table-test
  ;; Real inventory evidence exercises all topics without measurement dependencies.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        rows (evidence inventory)
        rendered (table/render inventory rows)]
    (testing "dimensions, external types, roots, units and variation are explicit"
      (doseq [text [":assoc/width-16" "width=16" "length=1024" ":external-java-point"
                    ":clojure-record" ":increment-relative-to-source" ":ns/op" ":bytes/op"
                    ":bytes" "12.500 | 12.500–12.500 | 0.000" "Sample SD"]]
        (is (str/includes? rendered text) text))
      (is (= rendered (table/render inventory (vec (reverse rows))))))
    (testing "locale does not change decimal formatting"
      (let [original (java.util.Locale/getDefault)]
        (try
          (java.util.Locale/setDefault java.util.Locale/GERMANY)
          (is (= rendered (table/render inventory rows)))
          (finally (java.util.Locale/setDefault original)))))
    (testing "failed and unavailable forks retain escaped reasons and no numbers"
      (let [missing (fn [entry status reason]
                      (-> entry (dissoc :raw :operations)
                          (assoc :status status :reason reason)))
            changed (-> rows
                        (update 0 missing :failed "bad|file\n<missing>")
                        (update 1 missing :unavailable "no tool"))
            output (table/render inventory changed)]
        (is (str/includes? output "| :unavailable | — | — | — |"))
        (is (str/includes? output "fork 1 failed: bad&#124;file &lt;missing>"))
        (is (str/includes? output "fork 2 unavailable: no tool"))
        (is (= (count (str/split-lines rendered)) (count (str/split-lines output))))))
    (testing "incomplete evidence cannot become a published table"
      (is (thrown? clojure.lang.ExceptionInfo (table/render inventory (pop rows)))))))

(deftest exploratory-parity-boundary-test
  ;; Large timing differences remain measurements, not S-03 decisions.
  (let [inventory (edn/read-string (slurp (io/resource "cases.edn")))
        rows (mapv (fn [entry]
                     (if (= :time (:metric entry))
                       (assoc entry :raw
                              (if (contains? #{:closed-record :value-array :restricted-holder}
                                             (:representation entry))
                                2000 2))
                       entry))
                   (evidence inventory))
        output (table/render inventory rows)
        cells (into #{} (comp (mapcat #(str/split % #"\|"))
                              (map str/trim))
                    (str/split-lines output))]
    (testing "all exploratory topics retain measured status despite a 1000-fold difference"
      (doseq [case-id [":assoc/width-3" ":array/read-4" ":field/read"]]
        (let [lines (filterv #(str/includes? % (str "| " case-id " |"))
                             (str/split-lines output))]
          (is (seq lines) case-id)
          (is (every? #(str/includes? % "| :measured |") lines) case-id)))
      (is (contains? cells "1000.000"))
      (is (contains? cells "1.000")))
    (testing "the exploratory report has neither parity labels nor ratio or interval columns"
      (doseq [label ["pass" "fail" "Result" "P / R" "P mean [low, high] ns"
                     "R mean [low, high] ns"]]
        (is (not (contains? cells label)) label)))))
