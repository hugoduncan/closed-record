(ns closed-record.bench.assoc-allocation-child
  "Partial ordinary allocation collection in a dedicated child JVM."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [closed-record.bench.assoc-allocation-result :as result]
            [closed-record.bench.child :as child]
            [closed-record.bench.transport :as transport]))

(defn validate-results
  "Require all ordinary assoc allocation rows and valid raw warmup controls.
  Return rows unchanged. This checks partial diagnostic coverage, not acceptance."
  [inventory association rows]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results rows))
  (let [expected (into #{}
                       (mapcat (fn [benchmark-case]
                                 (mapv (fn [representation]
                                         [(:id benchmark-case) representation :allocation])
                                       (get-in inventory [:configurations :ordinary :representations]))))
                       (filterv #(and (= :assoc (:contract %))
                                      (= :ordinary (:configuration %)))
                                (:cases inventory)))
        actual (into #{} (map (juxt :case-id :representation :metric)) rows)]
    (when-not (and (= :ordinary (:configuration association))
                   (seq expected) (= expected actual)
                   (every? #(= :measured (:status %)) rows))
      (throw (ex-info "Incomplete ordinary assoc allocation results"
                      {:expected expected :actual actual})))
    (run! result/stability rows)
    rows))

(defn validate-budget
  "Require each row's controls to match the declared positive batch budgets.
  Return rows unchanged. Validate coverage and raw counts separately."
  [rows {:keys [operations warmup-batches] :as budget}]
  (when-not (and (pos-int? operations) (pos-int? warmup-batches))
    (throw (ex-info "Invalid allocation budget" {:budget budget})))
  (doseq [row rows]
    (let [actual (select-keys (:allocation-controls row)
                              [:operations :warmup-batches])
          expected {:operations operations :warmup-batches warmup-batches}]
      (when-not (= expected actual)
        (throw (ex-info "Allocation budget mismatch"
                        {:case-id (:case-id row) :representation (:representation row)
                         :expected expected :actual actual})))))
  rows)

(defn write-results!
  "Collect both ordinary assoc widths and persist raw allocation controls.
  Budgets are explicit, with no acceptance defaults. Alternate representation
  order by fork. This partial envelope cannot satisfy complete-run coverage."
  [path inventory association operations warmup-batches]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (when-not (= :ordinary (:configuration association))
    (throw (ex-info "Assoc allocation requires the ordinary configuration"
                    {:association association})))
  (let [representations (if (odd? (:fork association))
                          [:closed-record :defrecord]
                          [:defrecord :closed-record])
        cases (filterv #(and (= :assoc (:contract %))
                             (= :ordinary (:configuration %)))
                       (:cases inventory))
        rows (into []
                   (mapcat (fn [benchmark-case]
                             (mapv #(result/collect! inventory benchmark-case %
                                                     (:fork association)
                                                     operations warmup-batches)
                                   representations)))
                   cases)]
    (child/write-results! path inventory association
                          (validate-results inventory association rows))))

(defn -main
  "Collect partial allocation evidence with explicit decimal budgets.
  Arguments: result path, run ID, revision, fork, operations, warmup batches.
  Exit 2 for invalid arguments or IO failure; exit zero is not acceptance."
  [& args]
  (let [failure
        (try
          (when-not (and (= 6 (count args))
                         (every? #(boolean (re-matches #"[1-9][0-9]*" %))
                                 (subvec (vec args) 3)))
            (throw (ex-info "Usage: assoc-allocation-child RESULT RUN REVISION FORK OPERATIONS WARMUP" {})))
          (let [[path run-id revision fork-text operations-text warmup-text] args
                [fork operations warmup] (mapv #(Integer/parseInt %)
                                               [fork-text operations-text warmup-text])
                inventory (edn/read-string (slurp (io/resource "cases.edn")))]
            (write-results! path inventory
                            {:run-id run-id :revision revision
                             :configuration :ordinary :fork fork}
                            operations warmup))
          nil
          (catch clojure.lang.ExceptionInfo error (.getMessage error))
          (catch NumberFormatException error (.getMessage error))
          (catch java.io.IOException error (.getMessage error)))]
    (when failure
      (binding [*out* *err*] (println failure))
      (System/exit 2))))
