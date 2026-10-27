(ns closed-record.bench-runtime-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.runtime :as runtime])
  (:import (java.lang.management ManagementFactory)))

(deftest runtime-observation-test
  ;; Real JVM observations must describe the process that collects them.
  (let [observed (runtime/observe)]
    (is (= (System/getProperty "java.runtime.version") (:version observed)))
    (is (= (System/getProperty "java.home") (:java-home observed)))
    (is (= (vec (.getInputArguments (ManagementFactory/getRuntimeMXBean)))
           (:flags observed)))
    (is (= observed (runtime/validate-observation observed observed)))))

(deftest runtime-reconciliation-test
  ;; Provenance differences must not be silently accepted or normalised.
  (let [declared {:version "21-build" :java-home "/jdk" :flags ["-Xms64m" "-Xmx128m"]}]
    (testing "additional observations survive"
      (let [observed (assoc declared :vendor "vendor")]
        (is (= observed (runtime/validate-observation declared observed)))))
    (testing "missing and changed fields fail with field-specific diagnostics"
      (doseq [key [:version :java-home :flags]
              observed [(dissoc declared key) (assoc declared key nil)]]
        (let [data (try (runtime/validate-observation declared observed)
                        (catch clojure.lang.ExceptionInfo error (ex-data error)))]
          (is (= #{key} (set (keys (:differences data))))))))
    (testing "malformed observations and reordered flags fail"
      (doseq [observed [nil [] "runtime" (assoc declared :flags [])
                        (update declared :flags #(vec (reverse %)))
                        (assoc declared :java-home "/jdk/")
                        (assoc declared :version "27-build")]]
        (is (thrown? clojure.lang.ExceptionInfo
                     (runtime/validate-observation declared observed)))))))
