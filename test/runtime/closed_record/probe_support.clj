(ns closed-record.probe-support
  "Helpers of the `:load-ns` probe tests: evaluate a form in a new namespace,
  and catch the exception of a call. The ordinary suite, the value-type suite
  and the shared check namespaces use them.")

(defn thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable e e)))

(defn data-of
  "Call `f`; return the `ex-data` of the exception that it throws, or nil."
  [f]
  (ex-data (thrown f)))

(defn eval-in-new-ns
  "Evaluate `form` in a new namespace from `create-ns` (not a loaded lib)."
  [form]
  (let [ns-sym (gensym "closed-record.load-ns-probe")]
    (try
      (binding [*ns* (create-ns ns-sym)]
        (refer-clojure)
        (eval form))
      (finally
        (remove-ns ns-sym)))))
