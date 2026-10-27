(ns closed-record.bench.assoc-allocation
  "Escaping ordinary assoc allocation batches, separate from timing."
  (:require [closed-record.bench.allocation :as allocation]
            [closed-record.bench.allocation-counter :as counter]))

(defn collect!
  "Collect one changed-value assoc batch and retain every result.
  Source, key, and replacement come from validated fixtures. Allocate the root
  array and prepare the counter before sampling. The interval includes loop,
  assoc, array stores, and counter boundary costs; subtract no overhead.
  Warmup, case identity, and child transport remain caller obligations."
  [source key replacement operations]
  (when-not (and (integer? operations) (<= 1 operations Integer/MAX_VALUE))
    (throw (ex-info "Invalid assoc allocation batch size" {:operations operations})))
  (when-not (and (contains? source key) (not= (get source key) replacement))
    (throw (ex-info "Assoc allocation requires a changed declared value" {:key key})))
  (let [roots (object-array operations)
        bean (counter/prepare!)
        thread-id (counter/current-thread-id)
        before (counter/read-bytes bean thread-id)]
    (dotimes [index operations]
      (aset roots index (assoc source key replacement)))
    (let [after (counter/read-bytes bean thread-id)]
      {:results roots
       :thread-id thread-id
       :outcome (allocation/outcome before after operations)})))

(defn collect-boundary!
  "Collect the loop/store/counter boundary without assoc.
  Store the prebuilt source in every escaping root. Keep raw overhead evidence
  separate from assoc outcomes; do not subtract it or treat it as assoc cost.
  Root allocation and counter preparation are outside the sampled interval."
  [source operations]
  (when-not (and (integer? operations) (<= 1 operations Integer/MAX_VALUE))
    (throw (ex-info "Invalid allocation boundary batch size" {:operations operations})))
  (let [roots (object-array operations)
        bean (counter/prepare!)
        thread-id (counter/current-thread-id)
        before (counter/read-bytes bean thread-id)]
    (dotimes [index operations]
      (aset roots index source))
    (let [after (counter/read-bytes bean thread-id)]
      {:control :assoc-boundary
       :results roots
       :thread-id thread-id
       :outcome (allocation/outcome before after operations)})))

(defn collect-warmed!
  "Run explicit warmup batches, then collect assoc and boundary evidence.
  Keep warmup counter outcomes but release their result roots between batches.
  Return final escaping roots for caller validation. Warmup is a fixed workload,
  not proof of JIT convergence. Never subtract or merge boundary counts. The
  caller chooses budgets and persists evidence outside these intervals."
  [source key replacement operations warmup-batches]
  (when-not (and (integer? warmup-batches)
                 (<= 1 warmup-batches Integer/MAX_VALUE))
    (throw (ex-info "Invalid allocation warmup count"
                    {:warmup-batches warmup-batches})))
  (let [warmup (into []
                     (map (fn [_]
                            {:assoc (:outcome (collect! source key replacement operations))
                             :boundary (:outcome (collect-boundary! source operations))}))
                     (range warmup-batches))]
    {:warmup warmup
     :warmup-batches warmup-batches
     :operations operations
     :assoc (collect! source key replacement operations)
     :boundary (collect-boundary! source operations)}))
