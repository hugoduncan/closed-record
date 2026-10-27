(ns closed-record.bench.assoc-allocation-result
  "EDN-safe ordinary assoc allocation evidence for child transport."
  (:require [closed-record.bench.allocation :as raw-allocation]
            [closed-record.bench.assoc-allocation :as allocation]
            [closed-record.bench.fixtures :as fixtures]
            [closed-record.bench.metrics :as metrics]))

(defn collect!
  "Collect one inventory representation with explicit warmup and batch budgets.
  Preserve raw warmup and boundary outcomes as supporting evidence on the final
  metric row. Do not serialize escaping object roots or subtract control costs.
  This partial row does not establish convergence or complete case coverage."
  [inventory benchmark-case representation fork operations warmup-batches]
  (let [identity {:case-id (:id benchmark-case)
                  :configuration (:configuration benchmark-case)
                  :representation representation :fork fork}
        ;; Reject invalid associations before doing measurement work.
        _ (metrics/validate-results
           inventory [(merge identity {:metric :allocation :unit :bytes/op
                                       :status :unavailable :reason "Preflight"})])
        {:keys [key replacement] :as fixture}
        (fixtures/assoc-pair (get-in inventory [:contracts :assoc]) benchmark-case)
        batch (allocation/collect-warmed! (get fixture representation)
                                          key replacement operations warmup-batches)]
    (merge identity (get-in batch [:assoc :outcome])
           {:allocation-controls
            {:warmup-batches (:warmup-batches batch)
             :operations (:operations batch)
             :warmup (:warmup batch)
             :boundary (get-in batch [:boundary :outcome])}})))

(defn- checked-count
  [outcome operations]
  (let [expected (raw-allocation/outcome (:counter-before outcome)
                                         (:counter-after outcome) operations)]
    (when-not (= expected (select-keys outcome (keys expected)))
      (throw (ex-info "Inconsistent allocation control" {:outcome outcome})))
    (:raw outcome)))

(defn stability
  "Summarize raw allocation counts without changing or subtracting evidence.
  Require complete, consistent fixed-budget controls. Report the number of
  trailing warmup batches equal to the final assoc count, and every transition
  (one-based batch index, including the final batch). Equal counts do not prove
  JIT convergence. Boundary counts remain separate and unadjusted, with their
  own changed batches and trailing warmup match count."
  [row]
  (let [{:keys [warmup warmup-batches operations boundary]} (:allocation-controls row)]
    (when-not (and (vector? warmup) (pos-int? warmup-batches)
                   (= warmup-batches (count warmup)))
      (throw (ex-info "Incomplete allocation warmup controls" {})))
    (let [assoc-counts (conj (mapv #(checked-count (:assoc %) operations) warmup)
                             (checked-count row operations))
          boundary-counts (conj (mapv #(checked-count (:boundary %) operations) warmup)
                                (checked-count boundary operations))
          final-count (peek assoc-counts)]
      {:operations operations
       :assoc-counts assoc-counts
       :boundary-counts boundary-counts
       :matching-trailing-warmup-batches
       (count (into [] (take-while #(= final-count %))
                    (rseq (pop assoc-counts))))
       :boundary-changed-batches
       (into [] (keep (fn [index]
                        (when (not= (nth boundary-counts (dec index))
                                    (nth boundary-counts index))
                          (inc index))))
             (range 1 (count boundary-counts)))
       :matching-trailing-boundary-warmup-batches
       (count (into [] (take-while #(= (peek boundary-counts) %))
                    (rseq (pop boundary-counts))))
       :changed-batches
       (into [] (keep (fn [index]
                        (when (not= (nth assoc-counts (dec index))
                                    (nth assoc-counts index))
                          (inc index))))
             (range 1 (count assoc-counts)))})))
