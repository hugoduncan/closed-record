(ns closed-record.bench.evidence
  "Filesystem storage for benchmark evidence; no measurement dependencies."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [closed-record.bench.graph-tool :as graph-tool]
   [closed-record.bench.transport :as transport]
   [closed-record.bench.runtime :as runtime]
   [closed-record.bench.timing-tool :as timing-tool])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file Files OpenOption StandardOpenOption)))

(defn- write-new!
  [file text]
  (Files/write (.toPath (io/file file))
               (.getBytes ^String text StandardCharsets/UTF_8)
               (into-array OpenOption [StandardOpenOption/CREATE_NEW
                                       StandardOpenOption/WRITE]))
  file)

(defn- nonblank-string?
  [value]
  (and (string? value) (not (str/blank? value))))

(defn- string-vector?
  [value]
  (and (vector? value) (every? nonblank-string? value)))

(defn- validate-metadata
  [metadata children]
  (let [configurations (set (mapv :configuration children))
        runtimes (:runtimes metadata)]
    (when-not
     (and (map? metadata)
          (string-vector? (:command metadata)) (seq (:command metadata))
          (map? runtimes) (= configurations (set (keys runtimes)))
          (every? (fn [runtime]
                    (and (map? runtime)
                         (nonblank-string? (:version runtime))
                         (nonblank-string? (:java-home runtime))
                         (string-vector? (:flags runtime))))
                  (vals runtimes)))
      (throw (ex-info "Invalid run command or runtime metadata"
                      {:metadata metadata :configurations configurations})))))

(defn- validate-run
  [inventory metadata children]
  (when-not (and (map? metadata) (vector? children) (seq children)
                 (= (count children) (count (distinct children))))
    (throw (ex-info "Invalid run manifest" {:metadata metadata :children children})))
  (doseq [child children]
    (transport/validate-envelope inventory child
                                 (assoc child :schema-version 1 :results [])))
  (when-not (and (apply = (mapv :run-id children))
                 (apply = (mapv :revision children)))
    (throw (ex-info "Children must belong to one run and revision" {:children children})))
  (validate-metadata metadata children)
  ;; Allocation-only runs need no timing dependency or declaration.
  (when (contains? metadata :timing-tool)
    (timing-tool/declaration (:timing-tool metadata)))
  ;; Timing and allocation runs need no graph agent declaration.
  (when (contains? metadata :graph-tool)
    (graph-tool/declaration (:graph-tool metadata))))

(defn create-run!
  "Persist the inventory, metadata and planned child associations before launch.
  The caller supplies runtime, command and machine metadata. The directory must
  not exist: retries require a new evidence directory. Return the manifest."
  [directory inventory metadata children]
  (validate-run inventory metadata children)
  (let [manifest {:schema-version 1 :metadata metadata
                  :inventory inventory :children children}]
    (Files/createDirectory (.toPath (io/file directory))
                           (make-array java.nio.file.attribute.FileAttribute 0))
    (write-new! (io/file directory "manifest.edn") (pr-str manifest))
    manifest))

(defn- result-diagnostic
  [inventory child raw-result]
  (if (nil? raw-result)
    {:status :missing :reason "Child result file is absent"}
    (try
      (transport/decode inventory child raw-result)
      {:status :valid}
      (catch Exception error
        {:status :invalid :reason (.getMessage error)
         :exception-class (.getName (class error))}))))

(defn- runtime-diagnostic
  [manifest child raw-result validation]
  (if (not= :valid (:status validation))
    {:status :unavailable :reason "No valid child envelope"}
    (let [observed (:runtime (transport/decode (:inventory manifest) child raw-result))
          declared (get-in manifest [:metadata :runtimes (:configuration child)])]
      (try
        (runtime/validate-observation declared observed)
        {:status :valid :observed observed}
        (catch clojure.lang.ExceptionInfo error
          {:status :invalid :reason (.getMessage error)
           :observed observed :differences (:differences (ex-data error))})))))

(defn capture-child!
  "Persist completed child output before validating its optional raw EDN text.
  Resolve the association from the saved manifest by zero-based child index.
  Missing or invalid results and nonzero exits remain separate evidence. Runtime
  observations in :runtime are checked separately against the manifest; absent
  observations cannot pass runtime validation. Return
  the saved outcome. A child slot cannot be overwritten; this does not launch
  processes or certify complete metric coverage. Text is stored as UTF-8."
  [directory child-index {:keys [exit out err raw-result]}]
  (let [manifest (edn/read-string (slurp (io/file directory "manifest.edn")))
        children (:children manifest)]
    (when-not (and (integer? child-index) (<= 0 child-index)
                   (< child-index (count children))
                   (integer? exit) (string? out) (string? err)
                   (or (nil? raw-result) (string? raw-result)))
      (throw (ex-info "Invalid child capture" {:child-index child-index :exit exit})))
    (let [child (nth children child-index)
          child-directory (io/file directory (str "child-" child-index))]
      (Files/createDirectory (.toPath child-directory)
                             (make-array java.nio.file.attribute.FileAttribute 0))
      (write-new! (io/file child-directory "stdout.txt") out)
      (write-new! (io/file child-directory "stderr.txt") err)
      (write-new! (io/file child-directory "process.edn")
                  (pr-str {:child child :exit exit}))
      (when (some? raw-result)
        (write-new! (io/file child-directory "result.edn") raw-result))
      (let [validation (result-diagnostic (:inventory manifest) child raw-result)
            outcome {:child child :exit exit :validation validation
                     :runtime-validation (runtime-diagnostic manifest child raw-result validation)}]
        (write-new! (io/file child-directory "outcome.edn") (pr-str outcome))
        outcome))))

(defn successful-child?
  "Return true only for a completed, validated child on the declared runtime.
  This is a process/transport gate, not metric coverage or measurement success.
  Partial results and explicit unavailable metrics still need coverage checks."
  [outcome]
  (and (nil? (:failure outcome))
       (= 0 (:exit outcome))
       (= :valid (get-in outcome [:validation :status]))
       (= :valid (get-in outcome [:runtime-validation :status]))))

(defn read-run
  "Read a saved manifest and its completed captures in planned child order.
  Missing or unreadable artifacts throw; reads never modify retained evidence.
  Raw result text is optional so failed captures retain their original outcome."
  [directory]
  (let [manifest (edn/read-string (slurp (io/file directory "manifest.edn")))
        _ (when-not (= 1 (:schema-version manifest))
            (throw (ex-info "Unsupported saved manifest schema"
                            {:schema-version (:schema-version manifest)})))
        _ (validate-run (:inventory manifest) (:metadata manifest) (:children manifest))
        captures (mapv (fn [index]
                         (let [slot (io/file directory (str "child-" index))
                               result (io/file slot "result.edn")]
                           {:outcome (edn/read-string (slurp (io/file slot "outcome.edn")))
                            :raw-result (when (.exists result) (slurp result :encoding "UTF-8"))}))
                       (range (count (:children manifest))))]
    {:manifest manifest :captures captures}))
