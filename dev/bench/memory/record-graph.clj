(require '[closed-record.bench.fixtures :as fixtures]
         '[clojure.edn :as edn])

(import '[java.util Set]
        'RecordGraphProbe)

(defn verify-record
  "Check physical field traversal against a known byte-array graph."
  [constructor width]
  (let [shared (byte-array 64)
        old-value (byte-array 64)
        replacement (byte-array 64)
        source (apply constructor (assoc (vec (repeat width shared)) 1 old-value))
        result (assoc source :b replacement)
        types (Set/of (class source))
        graph (fn [& roots] (RecordGraphProbe/graphBytes types (object-array roots)))
        container (RecordGraphProbe/shallow source)
        payload (RecordGraphProbe/shallow shared)
        source-size (graph source)
        result-size (graph result)
        union-size (graph source result)]
    (assert (= (+ container (* 2 payload)) source-size result-size))
    (assert (= (+ (* 2 container) (* 3 payload)) union-size))
    (assert (= (+ container payload) (- union-size source-size)))
    (assert (= source-size (graph source source)))
    (assert (zero? (graph nil)))
    ;; A field walker must see non-map metadata, and reject unknown graphs.
    (doseq [unsupported [(with-meta source {:probe true})
                         (assoc source :a (Object.))]]
      (assert (try (graph unsupported) false
                   (catch IllegalArgumentException _ true))))
    {:class (.getName (class source)) :width width :status :passed :unit :bytes
     :source source-size :result result-size :union union-size
     :incremental (- union-size source-size)
     :container-shallow container :payload-shallow payload
     :duplicate-roots true :unsupported-rejected true :metadata-traversed true}))

(prn {:control :record-graph :scope :explicit-records-with-primitive-arrays
      :results (mapv (fn [[constructor width]] (verify-record constructor width))
                     [[fixtures/->Closed3 3] [fixtures/->Record3 3]
                      [fixtures/->Closed16 16] [fixtures/->Record16 16]])})

(defn verify-boxed-inventory
  "Check inventory Long leaves and source/result sharing against shallow sizes."
  [contract benchmark-case]
  (let [{:keys [closed-record defrecord key replacement]}
        (fixtures/assoc-pair contract benchmark-case)]
    (mapv
     (fn [source]
       (let [result (assoc source key replacement)
             width (:width benchmark-case)
             types (Set/of (class source))
             graph (fn [& roots] (RecordGraphProbe/graphBytes types (object-array roots)))
             payloads (mapv #(get source %) (subvec (:fields contract) 0 width))
             container (RecordGraphProbe/shallow source)
             payload-total (reduce + (mapv #(RecordGraphProbe/shallow %) payloads))
             old-size (RecordGraphProbe/shallow (get source key))
             replacement-size (RecordGraphProbe/shallow replacement)
             source-size (graph source)
             result-size (graph result)
             union-size (graph source result)]
         (assert (every? #(instance? Long %) (conj payloads replacement)))
         (assert (= (inc width) (count (set (conj payloads replacement)))))
         (assert (= (+ container payload-total) source-size))
         (assert (= (+ container (- payload-total old-size) replacement-size) result-size))
         (assert (= (+ (* 2 container) payload-total replacement-size) union-size))
         (assert (= (+ container replacement-size) (- union-size source-size)))
         (assert (= source-size (graph source source)))
         ;; A repeated cached box is one reachable node, not one node per field.
         (assert (= old-size (graph (get source key) (get source key))))
         {:class (.getName (class source)) :case-id (:id benchmark-case)
          :status :passed :unit :bytes :source source-size :result result-size
          :union union-size :incremental (- union-size source-size)
          :container-shallow container :payload-total payload-total
          :replacement-shallow replacement-size :duplicate-box-root true}))
     [closed-record defrecord])))

(let [inventory (edn/read-string (slurp "dev/bench/cases.edn"))]
  (prn {:control :boxed-long-record-graph
        :scope :inventory-assoc-records
        :results (into [] (mapcat #(verify-boxed-inventory
                                    (get-in inventory [:contracts :assoc]) %))
                       (filterv #(= :assoc (:contract %)) (:cases inventory)))}))
