(ns closed-record.bench.allocation-counter
  "JDK thread allocation counter access. No measurement dependency is needed."
  (:import (com.sun.management ThreadMXBean)
           (java.lang.management ManagementFactory)))

(defn prepare!
  "Enable allocation counting outside measurement and return the JDK bean.
  Unsupported counters throw; callers must report unavailable, never zero."
  []
  (let [bean (ManagementFactory/getThreadMXBean)]
    (when-not (and (instance? ThreadMXBean bean)
                   (.isThreadAllocatedMemorySupported ^ThreadMXBean bean))
      (throw (ex-info "Thread allocation counter is unsupported" {})))
    (when-not (.isThreadAllocatedMemoryEnabled ^ThreadMXBean bean)
      (.setThreadAllocatedMemoryEnabled ^ThreadMXBean bean true))
    bean))

(defn current-thread-id
  "Return the thread identity to retain with a counter interval."
  []
  (.getId (Thread/currentThread)))

(defn read-bytes
  "Read cumulative bytes for the current thread, requiring its expected ID.
  Return a primitive long to avoid a boxed reading at a hinted call site.
  Do not subtract reader or caller overhead. Setup, escaping results, and
  operation counts remain collector obligations. Disabled counters throw."
  ^long [^ThreadMXBean bean expected-thread-id]
  (when-not (= expected-thread-id (current-thread-id))
    (throw (ex-info "Allocation interval changed thread"
                    {:expected-thread-id expected-thread-id
                     :actual-thread-id (current-thread-id)})))
  (let [bytes (.getThreadAllocatedBytes bean (long expected-thread-id))]
    (when (neg? bytes)
      (throw (ex-info "Thread allocation reading is unavailable"
                      {:thread-id expected-thread-id :reading bytes})))
    bytes))
