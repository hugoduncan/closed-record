(ns closed-record.bench.matrix-budget
  "Pure inventory accounting, without measurement or acceptance claims."
  (:require [closed-record.bench.timing-budget :as timing-budget]))

(defn- case-counts
  [inventory benchmark-case]
  (let [{:keys [configuration contract]} benchmark-case
        spec (get-in inventory [:contracts contract])
        representations (or (:representations spec)
                            (get-in inventory [:configurations configuration :representations]))
        metrics (:metrics spec)
        roots (:graph-roots spec)]
    (when-not (and (seq representations)
                   (= (count representations) (count (set representations)))
                   (seq metrics)
                   (= (count metrics) (count (set metrics)))
                   (every? #{:time :allocation :graph} metrics)
                   (or (not (some #{:graph} metrics))
                       (and (seq roots) (= (count roots) (count (set roots))))))
      (throw (ex-info "Invalid matrix case contract" {:case benchmark-case})))
    {:cases 1
     :time (if (some #{:time} metrics) (count representations) 0)
     :allocation (if (some #{:allocation} metrics) (count representations) 0)
     :graph (if (some #{:graph} metrics) (* (count representations) (count roots)) 0)}))

(defn summarise
  "Count planned rows and timing sites by configuration and for the whole matrix.
  Seconds is an explicit per-site timing limit. Its sum excludes startup, warmup,
  analysis, allocation and graph work; it is not a wall-clock estimate or timeout.
  Counts include unavailable metric slots, not just successful measurements."
  [inventory seconds]
  (let [forks (get-in inventory [:execution :forks])
        cases (:cases inventory)
        limit (:limit-time-s (timing-budget/declaration seconds))]
    (when-not (and (integer? forks) (pos? forks) (seq cases)
                   (every? :id cases)
                   (= (count cases) (count (set (mapv :id cases))))
                   (every? #(contains? (:configurations inventory) (:configuration %)) cases))
      (throw (ex-info "Invalid matrix inventory" {})))
    (let [per-configuration
          (into (sorted-map)
                (map (fn [[configuration members]]
                       [configuration
                        (reduce #(merge-with + %1 (case-counts inventory %2))
                                {:cases 0 :time 0 :allocation 0 :graph 0} members)]))
                (group-by :configuration cases))
          totals (reduce #(merge-with + %1 %2) (vals per-configuration))
          expand (fn [counts]
                   (let [rows (into {} (map (fn [metric] [metric (* forks (metric counts))]))
                                    [:time :allocation :graph])]
                     {:cases (:cases counts)
                      :rows rows
                      :total-rows (reduce + (vals rows))
                      :timing-limit-sum-s (* limit (:time rows))}))]
      {:forks forks
       :per-site-limit-s limit
       :configurations (update-vals per-configuration expand)
       :total (expand totals)})))
