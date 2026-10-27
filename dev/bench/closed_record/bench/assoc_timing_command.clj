(ns closed-record.bench.assoc-timing-command
  "Parent command construction for partial ordinary timing evidence."
  (:require [clojure.java.io :as io]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport]))

(defn command
  "Build a timing child command with inherited Java home, flags and classpath.
  Validate the association and positive finite seconds budget before launch.
  The budget is per site, not a process timeout or acceptance threshold."
  [inventory association result-path limit-s]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (when-not (and (= :ordinary (:configuration association))
                 (number? limit-s)
                 (Double/isFinite (double limit-s))
                 (pos? (double limit-s)))
    (throw (ex-info "Invalid ordinary timing command budget or configuration"
                    {:association association :limit-s limit-s})))
  (let [{:keys [java-home flags]} (runtime/observe)]
    (into (into [(str (io/file java-home "bin" "java"))] flags)
          ["-cp" (System/getProperty "java.class.path")
           "clojure.main" "-m" "closed-record.bench.assoc-timing-child"
           (str result-path) (:run-id association) (:revision association)
           (str (:fork association)) (str (double limit-s))])))
