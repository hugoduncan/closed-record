(ns closed-record.impl.jvm
  "JVM emitter for defclosedrecord. Builds the deftype* form and the
  factories. Internal: the function name and argument shape can change.

  This namespace does not require closed-record.core. It emits the
  ClosedRecord protocol interface only as a symbol.")

(def ^:private protocol-interface
  'closed_record.core.ClosedRecord)

(def hidden-fields
  "The hidden fields of a closed record type, with their deftype* metadata,
  in field order after the declared fields."
  '[__meta __extmap
    ^int ^:unsynchronized-mutable __hash
    ^int ^:unsynchronized-mutable __hasheq])

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
            ^String (apply print-str "Unsupported option(s) -" bad)))))

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
       (remove '#{Object java.lang.Object})))

(defn- user-methods
  [impls]
  (for [[_ methods] impls
        [mname params & body] methods]
    (cons mname (maybe-destructured params body))))

(defn- closed-throw
  "A form that throws a closed-record ex-info. `msg-parts` are the arguments
  of `str` for the message."
  [msg-parts data]
  `(throw (ex-info (str ~@msg-parts) ~data)))

(defn- cached-hash
  "A form that returns the hash cache `field`. If the cache is 0, it
  calculates `expr`, sets the cache and returns the value."
  [field expr]
  `(let [c# ~field]
     (if (zero? c#)
       (let [h# ~expr]
         (set! ~field h#)
         h#)
       c#)))

(defn- key-case
  "A `case` form on `k`, with one clause for each declared keyword and field
  pair, and then `default`. `(clause-fn kw fld)` gives the clause expression."
  [k kws fields clause-fn default]
  `(case ~k
     ~@(mapcat (fn [kw fld] [kw (clause-fn kw fld)]) kws fields)
     ~default))

(defn- generated-methods
  "The methods of the generated type. The field names are locals in the
  method bodies, so every generated local is a gensym."
  [tname cname-str type-hash fields]
  (let [kws (mapv keyword fields)
        n (count fields)
        this (gensym "this__")
        k (gensym "k__")
        v (gensym "v__")
        nf (gensym "nf__")
        gtarget (gensym "gtarget__")
        gclass (gensym "gclass__")
        thunk (gensym "thunk__")
        hinted-target (with-meta gtarget {:tag tname})]
    [;; IHashEq / Object
     `(~'hasheq [~this]
                ~(cached-hash '__hasheq
                              `(int (bit-xor ~type-hash
                                             (clojure.lang.APersistentMap/mapHasheq ~this)))))
     `(~'hashCode [~this]
                  ~(cached-hash '__hash
                                `(clojure.lang.APersistentMap/mapHash ~this)))
     `(~'equals [~this o#] (clojure.lang.APersistentMap/mapEquals ~this o#))
     ;; IObj
     `(~'meta [~this] ~'__meta)
     `(~'withMeta [~this m#]
                  (new ~tname ~@fields m# ~'__extmap ~'__hash ~'__hasheq))
     ;; ILookup / IKeywordLookup
     `(~'valAt [~this ~k] (.valAt ~this ~k nil))
     `(~'valAt [~this ~k ~nf]
               ~(key-case k kws fields (fn [_ fld] fld) nf))
     `(~'getLookupThunk [~this ~k]
                        (let [~gclass (class ~this)]
                          ~(key-case
                            k kws fields
                            (fn [_ fld]
                              `(reify clojure.lang.ILookupThunk
                                 (~'get [~thunk ~gtarget]
                                   (if (identical? (class ~gtarget) ~gclass)
                                     (. ~hinted-target ~(symbol (str "-" fld)))
                                     ~thunk))))
                            nil)))
     ;; IPersistentMap
     `(~'count [~this] ~n)
     `(~'empty [~this]
               ~(closed-throw ["Cannot create empty closed record " cname-str]
                              `{:type :closed-record/empty
                                :record (class ~this)}))
     `(~'cons [~this e#]
              (cond
                (map-entry? e#)
                (assoc ~this (key e#) (val e#))

                (instance? clojure.lang.IPersistentVector e#)
                (assoc ~this (nth e# 0) (nth e# 1))

                :else
                (loop [r# ~this
                       es# (seq e#)]
                  (if es#
                    (let [pair# (first es#)]
                      (recur (assoc r# (key pair#) (val pair#)) (next es#)))
                    r#))))
     `(~'equiv [~this o#]
               (boolean
                (or (identical? ~this o#)
                    (when (identical? (class ~this) (class o#))
                      (let [~gtarget o#]
                        (and ~@(map (fn [fld]
                                      `(= ~fld (. ~hinted-target ~(symbol (str "-" fld)))))
                                    fields)))))))
     `(~'containsKey [~this ~k]
                     ~(if (seq kws) `(case ~k (~@kws) true false) false))
     `(~'entryAt [~this ~k]
                 ~(key-case k kws fields
                            (fn [kw fld] `(clojure.lang.MapEntry/create ~kw ~fld))
                            nil))
     `(~'seq [~this]
             (seq [~@(map (fn [kw fld] `(clojure.lang.MapEntry/create ~kw ~fld))
                          kws fields)]))
     `(~'iterator [~this]
                  (clojure.lang.RecordIterator. ~this ~kws (clojure.lang.RT/iter nil)))
     `(~'assoc [~this ~k ~v]
               ~(key-case k kws fields
                          (fn [_ fld] `(new ~tname ~@(replace {fld v} fields) ~'__meta ~'__extmap))
                          (closed-throw ["Undeclared key " k " for closed record " cname-str]
                                        `{:type :closed-record/undeclared-key
                                          :record (class ~this)
                                          :key ~k})))
     `(~'without [~this ~k]
                 ~(if (seq kws)
                    `(case ~k
                       (~@kws)
                       ~(closed-throw ["Cannot dissoc declared key " k
                                       " from closed record " cname-str]
                                      `{:type :closed-record/declared-key-dissoc
                                        :record (class ~this)
                                        :key ~k})
                       ~this)
                    this))
     ;; java.util.Map
     `(~'size [~this] (.count ~this))
     `(~'isEmpty [~this] (= 0 (.count ~this)))
     `(~'containsValue [~this ~v] (boolean (some #{~v} (vals ~this))))
     `(~'get [~this ~k] (.valAt ~this ~k))
     `(~'put [~this ~k ~v] (throw (UnsupportedOperationException.)))
     `(~'remove [~this ~k] (throw (UnsupportedOperationException.)))
     `(~'putAll [~this m#] (throw (UnsupportedOperationException.)))
     `(~'clear [~this] (throw (UnsupportedOperationException.)))
     `(~'keySet [~this] (set (keys ~this)))
     `(~'values [~this] (vals ~this))
     `(~'entrySet [~this] (set ~this))
     ;; ClosedRecord
     `(~'-extra-keys [~this] ~'__extmap)]))

(defn- positional-factory
  [fn-name classname cname-str fields]
  (let [[field-args over] (split-at 20 fields)
        over-count (count over)
        overage (gensym "overage__")]
    `(defn ~fn-name
       ~(str "Positional factory function for class " cname-str ".")
       [~@field-args ~@(when (seq over) ['& overage])]
       ~(if (seq over)
          `(if (= (count ~overage) ~over-count)
             (new ~classname
                  ~@field-args
                  ~@(for [i (range over-count)] `(nth ~overage ~i)))
             (throw (clojure.lang.ArityException.
                     (+ ~(count field-args) (count ~overage))
                     ~(name fn-name))))
          `(new ~classname ~@field-args)))))

(defn- map-factory
  [fn-name classname cname-str fields]
  (let [declared (set (map keyword fields))
        m (gensym "m__")
        extra (gensym "extra__")]
    `(defn ~fn-name
       ~(str "Factory function for class " cname-str
             ", taking a map of keywords to field values.")
       [~m]
       (let [~m (if (instance? clojure.lang.MapEquivalence ~m) ~m (into {} ~m))
             ~extra (into #{} (remove ~declared) (keys ~m))]
         (when (seq ~extra)
           ~(closed-throw ["Extra keys " extra " for closed record " cname-str]
                          `{:type :closed-record/extra-keys
                            :record ~classname
                            :keys ~extra}))
         (. ~classname ~'create ~m)))))

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
         ~(into (vec fields) hidden-fields)
         :implements ~(vec (distinct (concat base-interfaces
                                             [protocol-interface]
                                             (user-interfaces impls))))
         ~@(mapcat identity opts)
         ~@methods
         ~@(user-methods impls))
       (import ~classname)
       ~(positional-factory ->name classname cname-str plain-fields)
       ~(map-factory map->name classname cname-str plain-fields)
       ~classname)))
