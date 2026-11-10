(ns closed-record.value-type.support
  "Support functions of the value-type suite tests.")

(defn type-form
  "The first `value-type.core/defvaluetype*` form at any depth of `form`, or
  nil."
  [form]
  (first (filter #(and (seq? %) (= 'value-type.core/defvaluetype* (first %)))
                 (tree-seq seq? seq form))))

(defn instance-field-names
  "The set of names of the non-static declared fields of class `c`."
  [^Class c]
  (->> (.getDeclaredFields c)
       (remove #(java.lang.reflect.Modifier/isStatic
                 (.getModifiers ^java.lang.reflect.Field %)))
       (map #(.getName ^java.lang.reflect.Field %))
       set))
