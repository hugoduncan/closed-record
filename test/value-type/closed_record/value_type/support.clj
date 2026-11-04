(ns closed-record.value-type.support
  "Support functions of the value-type suite tests.")

(defn type-form
  "The first `value-type.core/defvaluetype*` form at any depth of `form`, or
  nil."
  [form]
  (first (filter #(and (seq? %) (= 'value-type.core/defvaluetype* (first %)))
                 (tree-seq seq? seq form))))
