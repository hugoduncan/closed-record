(ns closed-record.probe-support
  "Test probe helpers: evaluate a form in a new namespace, catch the
  exception of a call, and catch the exception of a macroexpansion. The
  ordinary suite, the value-type suites and the shared check namespaces use
  them.")

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

(defn expansion-error
  "The exception of the macroexpansion of `form` in the namespace `ns-sym`,
  without the `CompilerException` wrapper; nil when there is no exception."
  [ns-sym form]
  (binding [*ns* (the-ns ns-sym)]
    (try
      ;; Expand for the exception only.
      #_{:clj-kondo/ignore [:unused-value]}
      (macroexpand-1 form)
      nil
      (catch clojure.lang.Compiler$CompilerException t (ex-cause t)))))
