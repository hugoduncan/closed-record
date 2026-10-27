(ns closed-record.bench.transport
  "Pure EDN transport for raw metric evidence. No measurement dependencies."
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [closed-record.bench.metrics :as metrics])
  (:import (java.io PushbackReader StringReader)))

(defn validate-envelope
  "Validate raw evidence against the expected child association and inventory.
  Partial results are allowed for failure capture. Return unchanged raw evidence;
  derived values belong to reporting, not transport. Throws on invalid input."
  [inventory expected envelope]
  (let [association (select-keys envelope [:run-id :revision :configuration :fork])]
    (when-not (and (map? envelope)
                   (= 1 (:schema-version envelope))
                   (= expected association)
                   (every? #(and (string? %) (not (str/blank? %)))
                           [(:run-id envelope) (:revision envelope)])
                   (contains? (:configurations inventory) (:configuration envelope))
                   (integer? (:fork envelope))
                   (<= 1 (:fork envelope) (get-in inventory [:execution :forks])))
      (throw (ex-info "Invalid metric envelope association" {:expected expected
                                                             :envelope envelope})))
    (metrics/validate-results inventory (:results envelope))
    (when-not (every? #(= (select-keys association [:configuration :fork])
                          (select-keys % [:configuration :fork]))
                      (:results envelope))
      (throw (ex-info "Result does not belong to child" {:envelope envelope})))
    envelope))

(defn encode
  "Encode validated raw child evidence as EDN."
  [inventory expected envelope]
  (pr-str (validate-envelope inventory expected envelope)))

(defn decode
  "Read exactly one EDN envelope and validate its child association.
  Reject empty input, trailing forms, reader evaluation and unknown tags."
  [inventory expected text]
  (with-open [reader (PushbackReader. (StringReader. text))]
    (let [eof (Object.)
          envelope (edn/read {:eof eof} reader)]
      (when (identical? eof envelope)
        (throw (ex-info "Missing metric envelope" {})))
      (when-not (identical? eof (edn/read {:eof eof} reader))
        (throw (ex-info "Trailing EDN form" {})))
      (validate-envelope inventory expected envelope))))
