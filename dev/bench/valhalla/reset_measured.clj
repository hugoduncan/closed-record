(ns valhalla.reset-measured
  "Reset mutable fixtures outside each separately timed operation."
  (:require [criterium.measured :as measured]))

(defn reset-measured
  "Wrap an expression with an untimed reset before each operation.

  reset! takes the generated expression state. Each operation retains the
  underlying timer and opaque-input costs; no overhead is subtracted. The
  returned value is the first result, as in Criterium's generated batches.
  This boundary is for time only, not allocation measurement."
  [operation reset!]
  (measured/measured
   #(measured/args operation)
   (fn [state evaluation-count]
     (when-not (pos-int? evaluation-count)
       (throw (ex-info "Expected a positive evaluation count"
                       {:evaluation-count evaluation-count})))
     (loop [remaining evaluation-count
            elapsed 0
            first-value nil]
       (reset! state)
       (let [[duration value] (measured/invoke operation state 1)
             result (if (= remaining evaluation-count) value first-value)
             total (+ elapsed duration)]
         (if (= remaining 1)
           [total result]
           (recur (dec remaining) total result)))))
   #(measured/symbolic operation)
   #(measured/warmup-args operation)))
