(ns closed-record.bench-runtime-capture-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.evidence :as evidence]
   [closed-record.bench.fork :as fork]
   [closed-record.bench.runtime :as runtime])
  (:import (java.nio.file Files)))

(deftest child-runtime-capture-test
  ;; Real child observations must survive capture even when provenance differs.
  (let [parent (.toFile (Files/createTempDirectory "runtime-capture-" (make-array java.nio.file.attribute.FileAttribute 0)))
        inventory (edn/read-string (slurp "dev/bench/cases.edn"))
        child {:run-id "runtime-check" :revision "abc123"
               :configuration (:configuration (first (:cases inventory))) :fork 1}
        declared (assoc (runtime/observe) :flags [])
        java (str (io/file (:java-home declared) "bin" "java"))]
    (try
      (doseq [[label declaration include-runtime? expected]
              [["matching" declared true :valid]
               ["mismatch" (assoc declared :version "different-build") true :invalid]
               ["missing" declared false :invalid]]]
        (testing label
          (let [directory (io/file parent label)
                _ (evidence/create-run! directory inventory
                                        {:command [java] :runtimes {(:configuration child) declaration}}
                                        [child])
                outcome (fork/capture-process!
                         directory 0
                         (fn [association result-path]
                           [java "-cp" (System/getProperty "java.class.path") "clojure.main" "-e"
                            (if include-runtime?
                              (str "(require '[closed-record.bench.child :as child])"
                                   "(child/write-results! " (pr-str result-path) " "
                                   (pr-str inventory) " " (pr-str association) " [])")
                              (str "(spit " (pr-str result-path) " (pr-str "
                                   (pr-str (assoc association :schema-version 1 :results []))
                                   "))"))]))]
            (is (= 0 (:exit outcome)))
            (is (= :valid (get-in outcome [:validation :status])))
            (testing "runtime diagnostics preserve observations and exact differences"
              (is (= expected (get-in outcome [:runtime-validation :status])))
              (is (= (when include-runtime? declared)
                     (get-in outcome [:runtime-validation :observed])))
              (when (= label "mismatch")
                (is (= {:version {:declared "different-build" :observed (:version declared)}}
                       (get-in outcome [:runtime-validation :differences])))))
            (is (= outcome (edn/read-string (slurp (io/file directory "child-0" "outcome.edn"))))))))
      (finally
        (doseq [file (reverse (file-seq parent))]
          (io/delete-file file))))))
