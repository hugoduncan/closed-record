(ns closed-record.bench.assoc-allocation-command
  "Parent command construction for partial allocation evidence."
  (:require [clojure.java.io :as io]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport]))

(defn command
  "Build an allocation child command using this JVM's home, flags and classpath.
  Validate association and explicit positive int budgets before process launch.
  This command collects partial evidence, not the complete acceptance matrix."
  [inventory association result-path operations warmup-batches]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (when-not (and (= :ordinary (:configuration association))
                 (every? #(and (integer? %) (<= 1 % Integer/MAX_VALUE))
                         [operations warmup-batches]))
    (throw (ex-info "Invalid ordinary allocation command budgets or configuration"
                    {:association association :operations operations
                     :warmup-batches warmup-batches})))
  (let [{:keys [java-home flags]} (runtime/observe)]
    (into (into [(str (io/file java-home "bin" "java"))] flags)
          ["-cp" (System/getProperty "java.class.path")
           "clojure.main" "-m" "closed-record.bench.assoc-allocation-child"
           (str result-path) (:run-id association) (:revision association)
           (str (:fork association)) (str operations) (str warmup-batches)])))
