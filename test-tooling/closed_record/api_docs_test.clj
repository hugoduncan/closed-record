(ns closed-record.api-docs-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [closed-record.api-docs :as docs]))

(deftest generated-api-test
  ;; Inspect real Quickdoc output, independently of any committed reference.
  (let [markdown (docs/generate)]
    (testing "only the two user API vars have entries"
      (is (= ["check" "defclosedrecord"]
             (mapv second (re-seq #"## <a name=\"closed-record.core/([^\"]+)\"" markdown))))
      (is (not (str/includes? markdown "ClosedRecord")))
      (is (not (str/includes? markdown "-extra-keys"))))
    (testing "the reference identifies its source and support boundary"
      (doseq [text ["Generated file. Do not edit."
                    "bb api-docs --check" "[README](../README.md)"
                    "Clojure 1.11+ on the JVM only"
                    "ClojureScript and Babashka are not supported."
                    "https://github.com/hugoduncan/closed-record/blob/master/src/closed_record/core.cljc#L"
                    "(check x)" "(defclosedrecord name [& fields] & opts+specs)"
                    ":no-print" ":load-ns" "->Name" "map->Name"
                    ":closed-record/undeclared-key" ":closed-record/declared-key-dissoc"
                    ":closed-record/empty" ":closed-record/extra-keys"
                    ":closed-record/not-closed-record" ":value-class"]]
        (is (str/includes? markdown text) text)))
    (testing "constructors, options, errors, and literal limits are documented"
      (doseq [text ["vector of field symbols" "protocol"
                    "interface names and method implementations"
                    "Options are keyword/value pairs before specs"
                    "passed to the JVM type compiler as in defrecord"
                    "one value per field in declaration order"
                    "map of keyword keys to field values"
                    "Missing declared keys in map->Name have nil values"
                    ":record (the record class)" "also include :key"
                    ":keys (the set of undeclared keys)"
                    "dissoc of an undeclared key returns the same record"
                    "assoc (also conj, merge, into, update)"
                    "empty throws" "not-found value"
                    "lookup, entries, equality, hash, or print"
                    "code constant and print/read round trips"
                    "check cannot find keys that have already been discarded"
                    "Return x" "including nil"]]
        (is (str/includes? (str/replace markdown #"\s+" " ") text) text)))
    (testing "repeated generation is deterministic"
      (is (= markdown (docs/generate))))))
