(ns closed-record.vale-results-test
  (:require [clojure.test :refer [deftest is testing]]
            [closed-record.vale-results :as results]))

(def suggestion
  {"Severity" "suggestion" "Check" "Style.Rule" "Message" "Use a simpler word."
   "Line" 1 "Span" [1 3]})

(deftest severity-policy-test
  ;; Suggestions remain visible without blocking; all stronger findings block.
  (is (= {:exit 0 :findings []} (results/outcome {})))
  (doseq [[severity exit] [["suggestion" 0] ["warning" 1] ["error" 1]]]
    (testing severity
      (let [alert (assoc suggestion "Severity" severity)
            outcome (results/outcome {"README.md" [alert]})]
        (is (= exit (:exit outcome)))
        (is (= [(assoc alert "Path" "README.md")] (:findings outcome))))))
  (is (= 1 (:exit (results/outcome
                   {"a.md" [suggestion]
                    "b.md" [(assoc suggestion "Severity" "warning")]})))))

(deftest invalid-results-test
  ;; Invalid output must never be mistaken for a clean prose check.
  (doseq [value [nil [] "{}" {"a.md" nil} {"a.md" {}} {"" []}
                 {"a.md" [nil]}
                 {"a.md" [(assoc suggestion "Severity" "notice")]}
                 {"a.md" [(dissoc suggestion "Severity")]}
                 {"a.md" [(assoc suggestion "Check" "")]}
                 {"a.md" [(assoc suggestion "Message" nil)]}
                 {"a.md" [(assoc suggestion "Line" 0)]}
                 {"a.md" [(assoc suggestion "Span" [3 1])]}
                 {"a.md" [(assoc suggestion "Span" [1])]}
                 {"a.md" [(assoc suggestion "Span" [0 1])]}]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid Vale results"
                          (results/outcome value))
        (pr-str value))))
