(ns closed-record.bench.assoc-graph-command
  "Command construction for isolated ordinary graph children."
  (:require [clojure.java.io :as io]
            [closed-record.bench.runtime :as runtime]
            [closed-record.bench.transport :as transport]))

(defn command
  "Build a graph child command with inherited Java home, flags and classpath.
  Validate association before launch. The caller must supply an instrumented
  JVM and verify agent provenance separately; this builder does not certify it."
  [inventory association result-path]
  (transport/validate-envelope inventory association
                               (assoc association :schema-version 1 :results []))
  (when-not (= :ordinary (:configuration association))
    (throw (ex-info "Assoc graphs require the ordinary configuration"
                    {:association association})))
  (let [{:keys [java-home flags]} (runtime/observe)]
    (into (into [(str (io/file java-home "bin" "java"))] flags)
          ["-cp" (System/getProperty "java.class.path")
           "clojure.main" "-m" "closed-record.bench.assoc-graph-child"
           (str result-path) (:run-id association) (:revision association)
           (str (:fork association))])))
