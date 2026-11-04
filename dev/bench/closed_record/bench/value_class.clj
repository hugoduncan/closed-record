(ns closed-record.bench.value-class
  "Value class test of a class, through the early-access Valhalla API
  `Class.isValue`. No dependencies, so the `:test` and `:bench` aliases can
  both load it.")

(defn value-class?
  "True when `c` is a value class. False on a JVM without `Class.isValue`."
  [^Class c]
  (try
    (boolean (.invoke (.getMethod Class "isValue" (make-array Class 0))
                      c (object-array 0)))
    (catch NoSuchMethodException _ false)))
