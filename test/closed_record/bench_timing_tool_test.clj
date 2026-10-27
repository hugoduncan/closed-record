(ns closed-record.bench-timing-tool-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.timing-tool :as tool]))

(def upstream
  {:library 'org.hugoduncan/criterium
   :base-version "0.5.251-ALPHA"
   :distribution :upstream
   :collector-sha256
   "a73da4deaa664aa68ddbeb0989e7dcb22a5878864a720b662c00aa9ab1557fa2"})

(def corrected
  (assoc
   upstream
   :distribution
   :local-correction
   :collector-sha256
   "5909a63300b38e72693261ffdd5accb2c76cfe809ac30a1d97ac38f068b74d11"
   :correction
   :positive-planning-duration-v1
   :patch-sha256
   "c8b1b7bb44877a6594a54f446d550810e0820bb1f78e4749e106f87507423f37"))

(deftest portable-identity-test
  ;; Resource locations are not portable tool identities.
  (doseq [identity [upstream corrected]]
    (let [parent
          (assoc identity :resource "file:/parent/collect.clj")
          child
          (assoc identity :resource "file:/child/collect.clj")]
      (is (= identity (tool/declaration parent)))
      (is (= child (tool/validate-observation parent child))))))

(deftest rejected-identity-test
  ;; Reject absent evidence and inconsistent correction claims.
  (testing "both sides must be internally valid"
    (doseq [invalid
            [nil
             {}
             (dissoc corrected :patch-sha256)
             (assoc corrected :patch-sha256 "changed")
             (assoc upstream :correction :positive-planning-duration-v1)
             (assoc upstream :base-version "other")
             (assoc upstream :library 'other/tool)
             (assoc upstream :collector-sha256 "unknown")]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Invalid timing tool"
                            (tool/validate-observation invalid corrected)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Invalid timing tool"
                            (tool/validate-observation corrected invalid)))))
  (testing "known but different collectors do not reconcile"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"identity mismatch"
                          (tool/validate-observation upstream corrected)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"identity mismatch"
                          (tool/validate-observation corrected upstream)))))
