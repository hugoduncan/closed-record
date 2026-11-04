(ns closed-record.value-type.expectation
  "The expected representation of the value-type suite. The JVM system
  property `closed-record.test.representation` gives it. The property is
  test data only: it does not select the representation.")

(defn representation
  "`:value` or `:identity`, from the property
  `closed-record.test.representation`. Throw if the property is absent or has
  another value."
  []
  (let [p (System/getProperty "closed-record.test.representation")]
    (case p
      "value" :value
      "identity" :identity
      (throw (ex-info (str "closed-record.test.representation must be"
                           " value or identity, not " (pr-str p))
                      {:property "closed-record.test.representation"
                       :value p})))))

(defn value-class?
  "True when `c` is a value class. False on a JVM without `Class.isValue`."
  [^Class c]
  (try
    (boolean (.invoke (.getMethod Class "isValue" (make-array Class 0))
                      c (object-array 0)))
    (catch NoSuchMethodException _ false)))

(defn observed
  "The observed representation of class `c`: `:value` or `:identity`."
  [c]
  (if (value-class? c) :value :identity))
