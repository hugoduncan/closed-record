(ns closed-record.form-support
  "Form helpers of the test support code: one walker that keeps metadata,
  and one function that inserts record options. The expansion fixture and the
  shared checks both use them.")

(defn walk-form
  "Rebuild `form` bottom-up and give the result of `f`. Walk the elements of
  each list, map entry, vector, map and set first; a rebuilt collection keeps
  the metadata of the input collection. Then apply `f` to each rebuilt
  element and to the rebuilt form. `f` sets the metadata of its result."
  [f form]
  (let [walk #(walk-form f %)
        rebuilt (cond
                  (seq? form) (apply list (map walk form))
                  (map-entry? form) (clojure.lang.MapEntry.
                                     (walk (key form)) (walk (val form)))
                  (vector? form) (mapv walk form)
                  (map? form) (into (empty form)
                                    (map (fn [[k v]] [(walk k) (walk v)]))
                                    form)
                  (set? form) (into (empty form) (map walk) form)
                  :else form)]
    (f (if (and (instance? clojure.lang.IObj rebuilt)
                (not (identical? form rebuilt)))
         (with-meta rebuilt (meta form))
         rebuilt))))

(defn with-options
  "Insert the option sequence `opts` (`[k v …]`) after the field vector of
  the `defclosedrecord` form `form`."
  [form opts]
  (let [[head tname fields & more] form]
    (apply list head tname fields (concat opts more))))
