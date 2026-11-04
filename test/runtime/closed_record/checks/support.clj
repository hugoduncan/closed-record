(ns closed-record.checks.support
  "Expansion of shared check bodies. A shared check namespace keeps its
  checks as a quoted body and a `define-checks` macro that calls `expand`.
  The ordinary suite and the value-type suite then define the same types and
  tests in their own namespaces, with their own record options.

  Body conventions:
  - A symbol that the source namespace refers or names through an alias is
    qualified, so the calling namespace needs no requires.
  - A symbol qualified with the source namespace (from syntax-quote) loses
    its namespace, so it names the var of the calling namespace.
  - `$ns` gives the quoted calling namespace symbol; `$ns/x` names `x` in
    the calling namespace.
  - `$P` in a string or a symbol name gives the munged calling namespace
    (the class name prefix).
  - `$representation` gives the representation form.
  - Each `defclosedrecord` form gets the record options after its field
    vector."
  (:require
   [clojure.string :as str]))

(defn- record-form?
  [form]
  (and (seq? form) (= 'closed-record.core/defclosedrecord (first form))))

(defn- with-record-opts
  [record-opts [head tname fields & more]]
  (apply list head tname fields (concat record-opts more)))

(defn- resolve-symbol
  [src-ns caller prefix sym]
  (let [sym-ns (namespace sym)
        sym-name (str/replace (name sym) "$P" prefix)]
    (cond
      (= "$ns" sym-ns) (symbol (str caller) sym-name)
      (= (str (ns-name src-ns)) sym-ns) (symbol sym-name)
      sym-ns (if-let [target (get (ns-aliases src-ns) (symbol sym-ns))]
               (symbol (str (ns-name target)) sym-name)
               (symbol sym-ns sym-name))
      :else (let [v (get (ns-refers src-ns) sym)]
              (if (and (var? v) (not= 'clojure.core (ns-name (.ns ^clojure.lang.Var v))))
                (symbol v)
                (symbol sym-name))))))

(defn- transform
  [src-ns {:keys [record-opts representation]} caller form]
  (let [prefix (namespace-munge caller)
        walk (fn walk [x]
               (let [y (cond
                         (= '$ns x) (list 'quote caller)
                         (= '$representation x) representation
                         (symbol? x) (resolve-symbol src-ns caller prefix x)
                         (string? x) (str/replace x "$P" prefix)
                         (seq? x) (let [l (apply list (map walk x))]
                                    (if (record-form? l) (with-record-opts record-opts l) l))
                         (map-entry? x) (clojure.lang.MapEntry. (walk (key x)) (walk (val x)))
                         (vector? x) (mapv walk x)
                         (map? x) (into (empty x) (map (fn [[k v]] [(walk k) (walk v)])) x)
                         (set? x) (into (empty x) (map walk) x)
                         :else x)]
                 (if (and (instance? clojure.lang.IObj y) (meta x) (not (identical? x y)))
                   (with-meta y (merge (meta y) (meta x)))
                   y)))]
    (walk form)))

(defn expand
  "The `do` form of `body` (a vector of quoted forms of `src-ns`) for the
  current `*ns*`, with `opts` `{:record-opts [k v …] :representation form}`."
  [src-ns opts body]
  (cons 'do (map #(transform src-ns opts (ns-name *ns*) %) body)))
