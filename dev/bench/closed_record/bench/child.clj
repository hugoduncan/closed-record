(ns closed-record.bench.child
  "Child-side persistence of raw measurements and actual runtime identity."
  (:require
   [clojure.java.io :as io]
   [closed-record.bench.runtime :as runtime]
   [closed-record.bench.transport :as transport])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file Files OpenOption StandardOpenOption)))

(defn write-results!
  "Validate and write one child envelope to an absent result path.
  Observe this JVM rather than accept caller-supplied runtime claims. Preserve
  raw and partial results; this does not certify coverage or metric success.
  Invalid evidence creates no file. Existing evidence is never overwritten."
  [path inventory association results]
  (let [envelope (assoc association :schema-version 1 :results results
                        :runtime (runtime/observe))
        text (transport/encode inventory association envelope)]
    (Files/write (.toPath (io/file path))
                 (.getBytes ^String text StandardCharsets/UTF_8)
                 (into-array OpenOption [StandardOpenOption/CREATE_NEW
                                         StandardOpenOption/WRITE]))
    envelope))
