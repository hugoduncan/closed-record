(ns closed-record.bench.graph-tool
  "Portable graph agent artifact reconciliation without loading the agent.")

(defn declaration
  "Return validated portable JAR and class hashes from a build or observation.
  Locations are process-local evidence. Hash syntax does not approve a build;
  callers must obtain declarations from the verified preparation output."
  [identity]
  (let [portable (select-keys identity [:jar-sha256 :class-sha256])]
    (when-not (and (= 2 (count portable))
                   (every? #(and (string? %) (re-matches #"[0-9a-f]{64}" %))
                           (vals portable)))
      (throw (ex-info "Invalid graph tool identity" {:identity identity})))
    portable))

(defn validate-observation
  "Compare declared build hashes with observed agent resource hashes.
  Preserve the full observation. This cannot detect runtime class transformations."
  [declared observed]
  (let [expected (declaration declared)
        actual (declaration observed)]
    (when-not (= expected actual)
      (throw (ex-info "Graph tool identity mismatch"
                      {:expected expected :actual actual})))
    observed))
