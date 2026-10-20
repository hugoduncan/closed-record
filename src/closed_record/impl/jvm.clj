(ns closed-record.impl.jvm
  "JVM emitter for defclosedrecord. Builds the deftype* form and the
  factories. Internal: the function name and argument shape can change.

  This namespace does not require closed-record.core. It emits the
  ClosedRecord protocol interface only as a symbol.")

(def ^:private protocol-interface
  'closed_record.core.ClosedRecord)

(def ^:private base-interfaces
  '[clojure.lang.IRecord
    clojure.lang.IHashEq
    clojure.lang.IObj
    clojure.lang.ILookup
    clojure.lang.IKeywordLookup
    clojure.lang.IPersistentMap
    java.util.Map
    java.io.Serializable])

(defn- check-field-metadata
  [fields]
  (when (some #{:volatile-mutable :unsynchronized-mutable}
              (mapcat (comp keys meta) fields))
    (throw (IllegalArgumentException.
            (str ":volatile-mutable or :unsynchronized-mutable not supported"
                 " for closed record fields")))))

(defn- check-options
  [opts]
  (when-let [bad (seq (remove #{:no-print :load-ns} (keys opts)))]
    (throw (IllegalArgumentException.
            (apply print-str "Unsupported option(s) -" bad)))))

(defn- maybe-destructured
  "Replace each non-symbol method parameter with a gensym, and destructure
  it in a let around the body."
  [params body]
  (if (every? symbol? params)
    (cons params body)
    (loop [ps (seq params)
           new-params (with-meta [] (meta params))
           lets []]
      (if ps
        (if (symbol? (first ps))
          (recur (next ps) (conj new-params (first ps)) lets)
          (let [g (gensym "p__")]
            (recur (next ps) (conj new-params g) (conj lets (first ps) g))))
        (list new-params `(let ~lets ~@body))))))

(defn- spec-interface
  "A symbol that resolves to a var (a protocol) gives its interface."
  [sym]
  (let [r (resolve sym)]
    (if (var? r) (:on @r) sym)))

(defn- user-interfaces
  [impls]
  (->> (map (comp spec-interface first) impls)
       (remove '#{Object java.lang.Object})
       distinct
       vec))

(defn- user-methods
  [impls]
  (for [[_ methods] impls
        [mname params & body] methods]
    (cons mname (maybe-destructured params body))))

(defn- undeclared-key-throw
  [cname-str k]
  `(throw (ex-info (str "Undeclared key " ~k " for closed record " ~cname-str)
                   {:type :closed-record/undeclared-key
                    :record ~'(class this)
                    :key ~k})))

(defn- generated-methods
  "The methods of the generated type. Each method names its target
  `this`; the field names are locals in the method bodies."
  [tagname cname-str type-hash fields]
  (let [kws (mapv keyword fields)
        n (count fields)
        ctor-short (fn [replacements]
                     `(new ~tagname
                           ~@(replace replacements fields)
                           ~'__meta
                           ~'__extmap))
        hinted-target (with-meta 'gtarget {:tag tagname})]
    [;; IHashEq / Object
     `(~'hasheq [~'this]
                (let [hq# ~'__hasheq]
                  (if (zero? hq#)
                    (let [h# (int (bit-xor ~type-hash
                                           (clojure.lang.APersistentMap/mapHasheq ~'this)))]
                      (set! ~'__hasheq h#)
                      h#)
                    hq#)))
     `(~'hashCode [~'this]
                  (let [hc# ~'__hash]
                    (if (zero? hc#)
                      (let [h# (clojure.lang.APersistentMap/mapHash ~'this)]
                        (set! ~'__hash h#)
                        h#)
                      hc#)))
     `(~'equals [~'this o#] (clojure.lang.APersistentMap/mapEquals ~'this o#))
     ;; IObj
     `(~'meta [~'this] ~'__meta)
     `(~'withMeta [~'this m#]
                  (new ~tagname ~@fields m# ~'__extmap ~'__hash ~'__hasheq))
     ;; ILookup / IKeywordLookup
     `(~'valAt [~'this k#] (.valAt ~'this k# nil))
     `(~'valAt [~'this k# nf#]
               (case k# ~@(mapcat vector kws fields) nf#))
     `(~'getLookupThunk [~'this k#]
                        (let [~'gclass (class ~'this)]
                          (case k#
                            ~@(mapcat
                               (fn [fld kw]
                                 [kw
                                  `(reify clojure.lang.ILookupThunk
                                     (~'get [~'thunk ~'gtarget]
                                       (if (identical? (class ~'gtarget) ~'gclass)
                                         (. ~hinted-target ~(symbol (str "-" fld)))
                                         ~'thunk)))])
                               fields kws)
                            nil)))
     ;; IPersistentMap
     `(~'count [~'this] ~n)
     `(~'empty [~'this]
               (throw (ex-info (str "Cannot create empty closed record " ~cname-str)
                               {:type :closed-record/empty
                                :record (class ~'this)})))
     `(~'cons [~'this e#]
              (cond
                (map-entry? e#)
                (.assoc ~'this (key e#) (val e#))

                (instance? clojure.lang.IPersistentVector e#)
                (.assoc ~'this (nth e# 0) (nth e# 1))

                :else
                (loop [r# ~'this
                       o# e#]
                  (if (seq o#)
                    (let [^java.util.Map$Entry pair# (first o#)]
                      (recur (assoc r# (.getKey pair#) (.getValue pair#)) (rest o#)))
                    r#))))
     `(~'equiv [~'this o#]
               (boolean
                (or (identical? ~'this o#)
                    (when (identical? (class ~'this) (class o#))
                      (let [~hinted-target o#]
                        (and ~@(map (fn [fld]
                                      `(= ~fld (. ~'gtarget ~(symbol (str "-" fld)))))
                                    fields)))))))
     `(~'containsKey [~'this ~'k]
                     ~(if (seq kws) `(case ~'k (~@kws) true false) false))
     `(~'entryAt [~'this k#]
                 (case k#
                   ~@(mapcat (fn [kw fld] [kw `(clojure.lang.MapEntry/create ~kw ~fld)])
                             kws fields)
                   nil))
     `(~'seq [~'this]
             (seq [~@(map (fn [kw fld] `(clojure.lang.MapEntry/create ~kw ~fld))
                          kws fields)]))
     `(~'iterator [~'this]
                  (clojure.lang.RecordIterator. ~'this ~kws (clojure.lang.RT/iter nil)))
     (let [k (gensym "k__")
           v (gensym "v__")]
       `(~'assoc [~'this ~k ~v]
                 (case ~k
                   ~@(mapcat (fn [kw fld] [kw (ctor-short {fld v})]) kws fields)
                   ~(undeclared-key-throw cname-str k))))
     `(~'without [~'this ~'k]
                 (case ~'k
                   ~@(when (seq kws)
                       [(apply list kws)
                        `(throw (ex-info (str "Cannot dissoc declared key " ~'k
                                              " from closed record " ~cname-str)
                                         {:type :closed-record/declared-key-dissoc
                                          :record (class ~'this)
                                          :key ~'k}))])
                   ~'this))
     ;; java.util.Map
     `(~'size [~'this] (.count ~'this))
     `(~'isEmpty [~'this] (= 0 (.count ~'this)))
     `(~'containsValue [~'this v#] (boolean (some #{v#} (vals ~'this))))
     `(~'get [~'this k#] (.valAt ~'this k#))
     `(~'put [~'this k# v#] (throw (UnsupportedOperationException.)))
     `(~'remove [~'this k#] (throw (UnsupportedOperationException.)))
     `(~'putAll [~'this m#] (throw (UnsupportedOperationException.)))
     `(~'clear [~'this] (throw (UnsupportedOperationException.)))
     `(~'keySet [~'this] (set (keys ~'this)))
     `(~'values [~'this] (vals ~'this))
     `(~'entrySet [~'this] (set ~'this))
     ;; ClosedRecord
     `(~'-extra-keys [~'this] ~'__extmap)]))

(defn- positional-factory
  [fn-name classname fields]
  (let [[field-args over] (split-at 20 fields)
        over-count (count over)]
    `(defn ~fn-name
       ~(str "Positional factory function for class " classname ".")
       [~@field-args ~@(when (seq over) '[& overage])]
       ~(if (seq over)
          `(if (= (count ~'overage) ~over-count)
             (new ~classname
                  ~@field-args
                  ~@(for [i (range over-count)] `(nth ~'overage ~i)))
             (throw (clojure.lang.ArityException.
                     (+ ~(count field-args) (count ~'overage))
                     ~(name fn-name))))
          `(new ~classname ~@field-args)))))

(defn- map-factory
  [fn-name classname fields]
  (let [kws (set (map keyword fields))]
    `(defn ~fn-name
       ~(str "Factory function for class " classname
             ", taking a map of keywords to field values.")
       [m#]
       (let [m# (if (instance? clojure.lang.MapEquivalence m#) m# (into {} m#))
             extra# (into #{} (remove ~kws) (keys m#))]
         (when (seq extra#)
           (throw (ex-info (str "Extra keys " extra# " for closed record "
                                ~(str classname))
                           {:type :closed-record/extra-keys
                            :record ~classname
                            :keys extra#})))
         (~(symbol (str classname "/create")) m#)))))

(defn emit
  "Return the expansion form of a closed record type.

  `tname` is the type name symbol, `fields` the declared field vector (with
  metadata), `opts` the option map and `impls` a sequence of
  `[symbol method-forms]` pairs, in source order."
  [tname fields opts impls]
  (check-field-metadata fields)
  (check-options opts)
  (let [classname (with-meta (symbol (str (namespace-munge *ns*) "." tname))
                    (meta tname))
        cname-str (str classname)
        plain-fields (mapv #(with-meta % nil) fields)
        type-hash (hash classname)
        ->name (symbol (str "->" tname))
        map->name (symbol (str "map->" tname))
        methods (generated-methods tname cname-str type-hash plain-fields)]
    `(let []
       (declare ~->name)
       (declare ~map->name)
       (deftype* ~(symbol (name (ns-name *ns*)) (name tname)) ~classname
         ~(conj (vec fields) '__meta '__extmap
                '^int ^:unsynchronized-mutable __hash
                '^int ^:unsynchronized-mutable __hasheq)
         :implements ~(vec (distinct (concat base-interfaces
                                             [protocol-interface]
                                             (user-interfaces impls))))
         ~@(mapcat identity opts)
         ~@methods
         ~@(user-methods impls))
       (import ~classname)
       ~(positional-factory ->name classname plain-fields)
       ~(map-factory map->name classname plain-fields)
       ~classname)))
