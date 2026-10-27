(ns closed-record.bench.metrics
  "Pure metric validation and per-batch normalisation."
  (:require [clojure.string :as str]))

(def units
  "Units for the three independent measurement contracts."
  {:time :ns/op :allocation :bytes/op :graph :bytes})

(defn- nonnegative-finite? [value]
  (and (number? value)
       (Double/isFinite (double value))
       (not (neg? value))))

(defn normalise
  "Validate a metric outcome and retain its raw inputs.
  Measured time and allocation require a positive integer operation count.
  Graph totals have no divisor. Failed or unavailable outcomes need a reason
  and cannot contain numeric results. Throws ExceptionInfo for invalid input."
  [{:keys [metric unit status raw operations reason] :as outcome}]
  (let [measured? (= :measured status)
        graph? (= :graph metric)
        valid? (and (map? outcome)
                    (contains? units metric)
                    (= unit (units metric))
                    (contains? #{:measured :unavailable :failed} status)
                    (not (contains? outcome :value))
                    (if measured?
                      (and (nonnegative-finite? raw)
                           (not (contains? outcome :reason))
                           (if graph?
                             (and (integer? raw)
                                  (not (contains? outcome :operations)))
                             (and (integer? operations) (pos? operations)
                                  (or (= :time metric) (integer? raw)))))
                      (and (string? reason) (not (str/blank? reason))
                           (not-any? #(contains? outcome %) [:raw :operations]))))]
    (when-not valid?
      (throw (ex-info "Invalid metric outcome" {:outcome outcome})))
    (if measured?
      (assoc outcome :value (if graph? raw (/ raw operations)))
      outcome)))

(defn- validate-identity
  [inventory {:keys [case-id configuration representation fork metric root]
              :as outcome}]
  (let [matches (filterv #(= case-id (:id %)) (:cases inventory))
        case-spec (first matches)
        contract (get-in inventory [:contracts (:contract case-spec)])
        config (get-in inventory [:configurations configuration])
        representations (or (:representations contract) (:representations config))]
    (when-not (and (= 1 (count matches))
                   config
                   (= configuration (:configuration case-spec))
                   (some #{representation} representations)
                   (some #{metric} (:metrics contract))
                   (integer? fork) (<= 1 fork (get-in inventory [:execution :forks]))
                   (if (= :graph metric)
                     (some #{root} (:graph-roots contract))
                     (not (contains? outcome :root))))
      (throw (ex-info "Invalid result identity" {:outcome outcome})))
    outcome))

(defn validate-results
  "Validate and normalise one batch of per-fork metric outcomes.
  Identity includes case, configuration, representation, fork, metric and graph
  root. Reject duplicates, even when their statuses differ. Partial batches are
  permitted for durable failure evidence; this does not certify run coverage."
  [inventory outcomes]
  (when-not (vector? outcomes)
    (throw (ex-info "Results must be a vector" {:outcomes outcomes})))
  (:results
   (reduce
    (fn [{:keys [seen] :as state} outcome]
      (let [validated (normalise (validate-identity inventory outcome))
            identity (select-keys validated
                                  [:case-id :configuration :representation :fork :metric :root])]
        (when (contains? seen identity)
          (throw (ex-info "Duplicate result identity" {:identity identity})))
        (-> state
            (update :seen conj identity)
            (update :results conj validated))))
    {:seen #{} :results []}
    outcomes)))

(defn validate-complete-results
  "Validate complete inventory coverage and normalise all outcomes.
  Every configured fork, representation, metric and graph root must occur.
  Explicit failed and unavailable outcomes count as evidence, not measurements.
  Missing identities are returned in exception data; no results are invented."
  [inventory outcomes]
  (let [results (validate-results inventory outcomes)
        identity-keys [:case-id :configuration :representation :fork :metric :root]
        actual (into #{} (map #(select-keys % identity-keys)) results)
        expected
        (into #{}
              (for [{:keys [id configuration contract]} (:cases inventory)
                    :let [spec (get-in inventory [:contracts contract])]
                    representation (or (:representations spec)
                                       (get-in inventory [:configurations configuration :representations]))
                    fork (range 1 (inc (get-in inventory [:execution :forks])))
                    metric (:metrics spec)
                    root (if (= :graph metric) (:graph-roots spec) [nil])]
                (cond-> {:case-id id :configuration configuration
                         :representation representation :fork fork :metric metric}
                  root (assoc :root root))))
        missing (into #{} (remove actual) expected)]
    (when (seq missing)
      (throw (ex-info "Incomplete result coverage" {:missing missing})))
    results))

(defn- summarise-forks
  [rows]
  (let [ordered (vec (sort-by :fork rows))
        identity (select-keys (first ordered)
                              [:case-id :configuration :representation :metric :root :unit])
        complete? (every? #(= :measured (:status %)) ordered)]
    (cond-> (assoc identity :status (if complete? :measured :unavailable)
                   :forks ordered)
      (not complete?) (assoc :reason "Not all forks have measured outcomes")
      complete?
      (merge (let [values (mapv :value ordered)
                   n (count values)
                   mean (/ (reduce + values) n)]
               {:mean mean
                :min (reduce min values)
                :max (reduce max values)
                :sample-standard-deviation
                (Math/sqrt (double (/ (transduce (map #(let [delta (- % mean)]
                                                         (* delta delta)))
                                                 + values)
                                      (dec n))))})))))

(defn aggregate-results
  "Summarise complete evidence across independent forks with equal fork weight.
  Require at least two forks. Report mean, range and sample standard deviation,
  not a confidence interval or parity decision. Preserve sorted raw fork evidence.
  If any fork is failed or unavailable, omit all numeric summary fields."
  [inventory outcomes]
  (when-not (< 1 (get-in inventory [:execution :forks]))
    (throw (ex-info "Aggregation requires at least two forks" {})))
  (let [results (validate-complete-results inventory outcomes)
        groups (group-by #(select-keys % [:case-id :configuration :representation :metric :root])
                         results)]
    (->> (vals groups)
         (mapv summarise-forks)
         (sort-by (juxt :case-id :configuration :representation :metric :root))
         vec)))
