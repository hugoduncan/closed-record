(ns closed-record.checks.support
  "Expansion of shared checks. A shared check namespace holds its checks as
  plain top-level forms, so clj-kondo lints them. No suite loads it:
  `define-checks` reads its source and defines the same types and tests in
  the calling namespace, with the record options of the caller. The ordinary
  suite and the value-type suite both use it.

  Source conventions:
  - A symbol that the source namespace refers or names through an alias is
    qualified, so the calling namespace needs no requires.
  - A symbol qualified with the source namespace (from syntax-quote) loses
    its namespace, so it names the var of the calling namespace.
  - `$P` in a string or a symbol name gives the munged calling namespace
    (the class name prefix).
  - `representation` of this namespace gives the representation value.
  - `metadata-and-hidden-keys?` of this namespace gives `false` when the
    record options have `:value-type true`, else `true`. A check guards its
    metadata and hidden-key parts with it (VT-V3).
  - Each `defclosedrecord` form gets the record options after its field
    vector."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [closed-record.form-support :as form])
  (:import
   (clojure.lang LineNumberingPushbackReader)))

(def representation
  "Placeholder in a shared check namespace. `define-checks` replaces it with
  the `:representation` value of the caller."
  :identity)

(def metadata-and-hidden-keys?
  "Placeholder in a shared check namespace. `define-checks` replaces it with
  `false` when the `:record-opts` of the caller have `:value-type true`, else
  with `true`. Value-mode records have no metadata and no hidden keys."
  true)

(defn- value-type-opts?
  [record-opts]
  (true? (get (apply hash-map record-opts) :value-type)))

(defn- record-form?
  [form]
  (and (seq? form) (= 'closed-record.core/defclosedrecord (first form))))

(defn- resolve-symbol
  [src-ns prefix sym]
  (let [sym-ns (namespace sym)
        sym-name (str/replace (name sym) "$P" prefix)]
    (cond
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
        metadata-and-hidden-keys? (not (value-type-opts? record-opts))
        keep-meta (fn [x y] (with-meta y (merge (meta y) (meta x))))]
    (form/walk-form
     (fn [x]
       (cond
         (symbol? x) (let [r (resolve-symbol src-ns prefix x)]
                       (case r
                         closed-record.checks.support/representation representation
                         closed-record.checks.support/metadata-and-hidden-keys? metadata-and-hidden-keys?
                         (keep-meta x r)))
         (string? x) (str/replace x "$P" prefix)
         (record-form? x) (keep-meta x (form/with-options x record-opts))
         :else x))
     form)))

(defn- source-path
  "The class path resource path of the source file of namespace `src-sym`."
  [src-sym]
  (str (-> (name src-sym) (str/replace "-" "_") (str/replace "." "/"))
       ".clj"))

(defn- read-source
  "The namespace and the forms after the `ns` form of the source file at
  `path`. Evaluate the `ns` form only (its requires, aliases and refers),
  then read the forms in that namespace, so syntax-quote and `::` keywords
  resolve as in the source."
  [src-sym path]
  (let [url (or (io/resource path)
                (throw (ex-info (str "No shared check source " path)
                                {:namespace src-sym})))]
    (with-open [r (LineNumberingPushbackReader. (io/reader url))]
      (let [ns-form (read r)
            src-ns (binding [*ns* *ns*] (eval ns-form) *ns*)
            forms (binding [*ns* src-ns]
                    (doall (take-while #(not= ::eof %)
                                       (repeatedly #(read r false ::eof)))))]
        [src-ns forms]))))

(defn define-checks
  "Define the checks of the shared check namespace `src-sym` in the current
  `*ns*`. `opts`: `{:record-opts [k v …] :representation r}`, `r` is
  `:identity` or `:value`. Each `representation` of this namespace in the
  check source becomes `r`. Each `metadata-and-hidden-keys?` of this
  namespace becomes `false` when `:record-opts` has `:value-type true`, else
  `true`. The compiler gets the check source file as
  source, so the file and the line of a failure both name the check source."
  [src-sym {:keys [representation] :as opts}]
  (when-not (#{:identity :value} representation)
    (throw (ex-info (str "representation must be :identity or :value, not "
                         (pr-str representation))
                    {:representation representation})))
  (let [path (source-path src-sym)
        [src-ns forms] (read-source src-sym path)
        caller (ns-name *ns*)]
    (with-bindings {Compiler/SOURCE_PATH path
                    Compiler/SOURCE (subs path (inc (str/last-index-of path "/")))}
      (doseq [form forms]
        (eval (transform src-ns opts caller form))))))
