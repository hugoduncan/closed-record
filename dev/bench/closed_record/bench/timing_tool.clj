(ns closed-record.bench.timing-tool
  "Identify the selected collector resource without loading measurement tools."
  (:require [clojure.java.io :as io])
  (:import (java.security MessageDigest)))

(def ^:private collectors
  {"a73da4deaa664aa68ddbeb0989e7dcb22a5878864a720b662c00aa9ab1557fa2"
   {:distribution :upstream}
   "5909a63300b38e72693261ffdd5accb2c76cfe809ac30a1d97ac38f068b74d11"
   {:distribution :local-correction
    :correction :positive-planning-duration-v1
    :patch-sha256 "c8b1b7bb44877a6594a54f446d550810e0820bb1f78e4749e106f87507423f37"}})

(defn identify
  "Identify known collector bytes; reject unknown sources before measurement.
  This identifies the collector, not every resource in the dependency graph."
  [^bytes content]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") content)
        sha (apply str (mapv #(format "%02x" (bit-and 255 %)) digest))
        identity (get collectors sha)]
    (when-not identity
      (throw (ex-info "Unknown Criterium collector source" {:sha256 sha})))
    (assoc identity :library 'org.hugoduncan/criterium
           :base-version "0.5.251-ALPHA" :collector-sha256 sha)))

(defn observe
  "Hash the selected classpath collector source and retain its resource URL.
  Does not prove that previously loaded or redefined vars match these bytes;
  use only in a fresh, unmodified measurement JVM."
  []
  (let [resource (io/resource "criterium/collect.clj")]
    (when-not resource
      (throw (ex-info "Missing Criterium collector source" {})))
    (with-open [stream (io/input-stream resource)]
      (assoc (identify (.readAllBytes stream)) :resource (str resource)))))

(defn declaration
  "Return a validated portable collector identity, without its resource URL.
  Reject missing, unknown, or inconsistent identity fields. Resource locations
  can differ between parent and child JVMs and are not part of the declaration."
  [observation]
  (let [sha (:collector-sha256 observation)
        known (get collectors sha)
        expected (assoc known :library 'org.hugoduncan/criterium
                        :base-version "0.5.251-ALPHA" :collector-sha256 sha)
        actual (select-keys observation
                            [:distribution :correction :patch-sha256 :library
                             :base-version :collector-sha256])]
    (when-not (and known (= expected actual))
      (throw (ex-info "Invalid timing tool identity" {:observation observation})))
    expected))

(defn validate-observation
  "Reconcile a portable parent declaration with a child collector observation.
  Validate both identities before comparison; preserve the full observation.
  This checks source identity, not runtime redefinitions or other dependencies."
  [declared observed]
  (let [expected (declaration declared)
        actual (declaration observed)]
    (when-not (= expected actual)
      (throw (ex-info "Timing tool identity mismatch"
                      {:expected expected :actual actual})))
    observed))
