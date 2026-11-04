(ns closed-record.expansion-fixture
  "The VT-I2 expansion fixture: fixed `defclosedrecord` forms, their
  normalised printed expansion, and the command that records the fixture
  file. This namespace is also the fixed namespace of the expansions."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [closed-record.core]))

(def forms
  "Fixture form name to quoted `defclosedrecord` form."
  '{:plain
    (closed-record.core/defclosedrecord Plain [a b])

    :tagged
    (closed-record.core/defclosedrecord Tagged
      [^long n ^double d ^String s ^objects xs])

    :no-print
    (closed-record.core/defclosedrecord NoPrint [a] :no-print true)

    :load-ns
    (closed-record.core/defclosedrecord LoadNs [a] :load-ns true)

    :methods
    (closed-record.core/defclosedrecord Methods [a ^long b]
      :no-print true
      :load-ns true
      Object
      (toString [_] (str "M" a))
      java.lang.Comparable
      (compareTo [this ^Object other]
                 (compare (.length ^String (str a)) other))
      java.util.function.Supplier
      (get [_] a))

    :destructured
    (closed-record.core/defclosedrecord Destructured [a]
      java.util.function.BiFunction
      (apply [_ [x y] {:keys [z]}] (list a x y z)))})

(defn with-option
  "Insert the option pair `k v` after the field vector of `form`."
  [form k v]
  (let [[head tname fields & more] form]
    (apply list head tname fields k v more)))

(defn- strip-positions
  "Remove `:line`, `:column` and `:file` from the metadata of `form` and of
  every list, vector, map and set in it. Keep all other metadata."
  [form]
  (let [rebuilt (cond
                  (seq? form) (apply list (map strip-positions form))
                  (map-entry? form) (vec (map strip-positions form))
                  (vector? form) (vec (map strip-positions form))
                  (map? form) (into (empty form)
                                    (map (fn [[k v]]
                                           [(strip-positions k)
                                            (strip-positions v)]))
                                    form)
                  (set? form) (into (empty form) (map strip-positions) form)
                  :else form)]
    (if (instance? clojure.lang.IObj rebuilt)
      (with-meta rebuilt (not-empty (dissoc (meta form) :line :column :file)))
      rebuilt)))

(def ^:private gensym-pattern
  #"\b([A-Za-z_-]+__)(\d+)(__auto__)?\b")

(defn normalise
  "Replace each gensym name `prefix__N` or `prefix__N__auto__` in `s` with
  `prefix__G<i>` or `prefix__G<i>__auto__`, where `<i>` is the order of first
  appearance of the name."
  [s]
  (let [index (volatile! {})]
    (str/replace s gensym-pattern
                 (fn [[whole prefix _ auto]]
                   (let [i (or (@index whole)
                               (let [i (count @index)]
                                 (vswap! index assoc whole i)
                                 i))]
                     (str prefix "G" i auto))))))

(defn expansion
  "The normalised printed `macroexpand-1` of `form`, with `*ns*` bound to
  this namespace and `*print-meta*` true."
  [form]
  (binding [*ns* (the-ns 'closed-record.expansion-fixture)]
    (let [expanded (macroexpand-1 (strip-positions form))]
      (normalise (binding [*print-meta* true] (pr-str expanded))))))

(def fixture-path
  "The path of the fixture file, relative to the repository root."
  "test/runtime/closed_record/expansion_fixture.edn")

(defn fixture
  "The recorded fixture: form name to normalised printed expansion."
  []
  (read-string (slurp (io/resource "closed_record/expansion_fixture.edn"))))

(defn record!
  "Write the fixture file from the current emitter."
  []
  (spit fixture-path
        (binding [*print-meta* false]
          (str "{"
               (str/join "\n "
                         (for [[k f] (sort-by key forms)]
                           (str (pr-str k) "\n " (pr-str (expansion f)))))
               "}\n"))))
