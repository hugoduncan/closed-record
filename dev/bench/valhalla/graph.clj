(ns valhalla.graph
  "Inventory adaptation for verified flat experimental graph counts."
  (:require [closed-record.bench.graph :as graph]
            [closed-record.bench.metrics :as metrics])
  (:import Graph))

(defn collect!
  "Return experimental graph rows in alternating pair order.
  Reference graphs are unavailable: value projection cannot establish physical
  box identity. Array roots count the result array; field roots also expose
  sharing. Agent provenance and persistence belong to the child boundary."
  [inventory fork]
  (when-not (and (integer? fork) (<= 1 fork 5))
    (throw (ex-info "Invalid experimental graph fork" {:fork fork})))
  (let [rows
        (into []
              (mapcat
               (fn [{:keys [id contract length operation]}]
                 (let [representations (get-in inventory [:contracts contract :representations])
                       ordered (if (odd? fork) representations (vec (reverse representations)))
                       roots (get-in inventory [:contracts contract :graph-roots])]
                   (into []
                         (mapcat
                          (fn [representation]
                            (let [identity {:case-id id :configuration :valhalla
                                            :representation representation :fork fork}
                                  flat? (contains? #{:value-array :restricted-holder} representation)
                                  outcomes
                                  (if flat?
                                    (let [counts (Graph/sample (= :array contract)
                                                               (name operation) (int (or length 0)))
                                          measured (graph/outcomes (.source counts) (.result counts)
                                                                   (.union counts))]
                                      (if (= :array contract)
                                        [(assoc (first measured) :root :array)]
                                        measured))
                                    (mapv (fn [root]
                                            {:metric :graph :unit :bytes :root root
                                             :status :unavailable
                                             :reason "Physical value-box identity is not observable through value projection; see task 019 value-identity probe evidence."})
                                          roots))]
                              (mapv #(merge identity %) outcomes))))
                         ordered))))
              (filterv #(= :valhalla (:configuration %)) (:cases inventory)))]
    (metrics/validate-results inventory rows)
    rows))
