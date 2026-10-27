(ns closed-record.bench.runtime
  "Runtime observations and exact provenance checks for benchmark children."
  (:import (java.lang.management ManagementFactory)))

(defn observe
  "Read runtime identity inside the measured child, not its parent launcher."
  []
  {:version (System/getProperty "java.runtime.version")
   :java-home (System/getProperty "java.home")
   :flags (vec (.getInputArguments (ManagementFactory/getRuntimeMXBean)))})

(defn validate-observation
  "Require the child's observed runtime to match the declaration exactly.
  Preserve additional observed metadata. Flag order and Java home spelling are
  significant: do not silently normalise away a different launch environment."
  [declared observed]
  (let [keys-required [:version :java-home :flags]
        differences (into {}
                          (keep (fn [key]
                                  (when (or (not (and (map? observed) (contains? observed key)))
                                            (not= (get declared key) (get observed key)))
                                    [key {:declared (get declared key)
                                          :observed (get observed key)}])))
                          keys-required)]
    (when (or (not (map? observed)) (seq differences))
      (throw (ex-info "Child runtime differs from declaration"
                      {:differences differences :observed observed})))
    observed))
