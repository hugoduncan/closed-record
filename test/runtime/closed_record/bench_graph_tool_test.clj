(ns closed-record.bench-graph-tool-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [closed-record.bench.graph-tool :as tool]))

(def build-identity
  {:jar-sha256 (apply str (repeat 64 "a"))
   :class-sha256 (apply str (repeat 64 "b"))})

(deftest portable-identity-test
  ;; Build hashes are portable; resource locations must remain evidence only.
  (let [parent (assoc build-identity :location "file:/parent/agent.jar")
        child (assoc build-identity :location "file:/child/agent.jar")]
    (is (= build-identity (tool/declaration parent)))
    (is (= child (tool/validate-observation parent child)))
    (is (= child (tool/validate-observation
                  build-identity (edn/read-string (pr-str child))))))
  (testing "Each artifact must match independently"
    (doseq [field [:jar-sha256 :class-sha256]]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"identity mismatch"
           (tool/validate-observation
            build-identity
            (assoc build-identity field (apply str (repeat 64 "c")))))))))

(deftest invalid-identity-test
  ;; Neither side can bypass validation with matching malformed hashes.
  (doseq [identity (into [nil {}]
                         (mapcat (fn [field]
                                   (mapv #(assoc build-identity field %)
                                         [nil 42 "" "abc"
                                          (apply str (repeat 64 "G"))
                                          (apply str (repeat 63 "a"))
                                          (apply str (repeat 65 "a"))])))
                         [:jar-sha256 :class-sha256])]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid graph tool identity"
                          (tool/declaration identity)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (tool/validate-observation identity build-identity)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (tool/validate-observation build-identity identity)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (tool/validate-observation identity identity)))))
