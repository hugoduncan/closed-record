(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as shell]
         '[clojure.string :as str]
         '[closed-record.test-support.api-project :as fixture])

;; Separate acceptance probe: freshness must not replace independent content checks.
(fixture/with-project
  (fn [root]
    (let [generator (io/file root "dev/quality/closed_record/api_docs.clj")
          content-test (io/file root "test-tooling/closed_record/api_docs_test.clj")]
      (io/make-parents content-test)
      (io/copy (io/file "test-tooling/closed_record/api_docs_test.clj") content-test)
      (spit generator
            (str/replace (slurp generator)
                         "ClojureScript and Babashka are not supported."
                         "Support details omitted."))
      (doseq [args [[] ["--check"]]]
        (let [result (apply shell/sh "bb" "api-docs" (concat args [:dir root]))]
          (when-not (zero? (:exit result))
            (throw (ex-info "Defective reference must pass generation and freshness" result)))))
      (let [result (shell/sh
                    "clojure" "-Srepro" "-M:quickdoc:tooling-test" "-e"
                    (pr-str '(do
                               (require '[scry.core :as scry])
                               (let [result (scry/run
                                             {:namespaces ['closed-record.api-docs-test]})]
                                 (spit "content-result.edn"
                                       (pr-str
                                        (update (select-keys result [:pass? :summary :results])
                                                :results
                                                (fn [entries]
                                                  (mapv (fn [entry]
                                                          {:var (:var entry)
                                                           :assertions
                                                           (mapv #(select-keys % [:type :message])
                                                                 (:assertions entry))})
                                                        entries)))))
                                 (System/exit (if (:pass? result) 0 1)))))
                    :dir root)]
        (when-not (= 1 (:exit result))
          (throw (ex-info "Content probe child did not report rejection" result)))
       ;; Read our explicit result artifact, not Scry's failure-file layout.
       ;; Missing, unreadable, or invalid EDN fails the probe rather than passing it.
        (let [outcome (edn/read-string (slurp (io/file root "content-result.edn")))
              failures (into []
                             (mapcat (fn [entry]
                                       (into []
                                             (comp
                                              (filter #(#{:fail :error} (:type %)))
                                              (map #(assoc % :var (:var entry))))
                                             (:assertions entry))))
                             (:results outcome))]
          (when-not (and (false? (:pass? outcome))
                         (= 1 (get-in outcome [:summary :fail]))
                         (= 0 (get-in outcome [:summary :error]))
                         (= [{:var 'closed-record.api-docs-test/generated-api-test
                              :type :fail
                              :message "ClojureScript and Babashka are not supported."}]
                            (mapv #(select-keys % [:var :type :message]) failures)))
            (throw (ex-info "Expected only the missing support notice assertion"
                            {:child result :outcome outcome})))
          (prn (select-keys outcome [:pass? :summary])))))))
