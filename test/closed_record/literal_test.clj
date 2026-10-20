(ns closed-record.literal-test
  "V-04 (print and read) and V-05 (code constants).
  These paths go through the Clojure printer, the reader and
  `Compiler.emitValue`, not through closed-record code."
  (:require
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [check defclosedrecord]]))

(def ^:private test-ns 'closed-record.literal-test)

(defclosedrecord T [a ^long n])

(defn- in-test-ns
  "Call `f` with `*ns*` bound to this namespace."
  [f]
  (binding [*ns* (the-ns test-ns)] (f)))

(defn- read-literal [s] (in-test-ns #(read-string s)))

(defn- thrown
  "Call `f`; return the exception that it throws, or nil."
  [f]
  (try (f) nil (catch Throwable t t)))

(defmacro ^:private constant
  "Read the literal string `s` at expansion time and return the record
  object. No `eval`: `eval` of a form that holds the object drops
  `__extmap` before the macro returns."
  [s]
  (read-string s))

(deftest round-trip-test
  ;; I-06. Each `r` has no metadata.
  (doseq [r [(->T 1 2) (->T (->T 3 4) 5)]]
    (testing (pr-str r)
      (let [v (read-literal (pr-str r))]
        (is (= r v))
        (is (= T (class v)))))))

(deftest print-dup-round-trip-test
  ;; Positional form: an observed printer fact, kept as a change signal.
  ;; The read-back verifies I-06 through the positional constructor (C-07).
  (doseq [r [(->T 1 2) (->T (->T 3 4) 5)]]
    (testing (pr-str r)
      (let [s (binding [*print-dup* true] (pr-str r))
            v (read-literal s)]
        (is (.startsWith ^String s (str "#" (.getName ^Class T) "[")))
        (is (= r v))
        (is (= T (class v)))))))

(deftest positional-literal-test
  (let [v (read-literal "#closed_record.literal_test.T[1 2]")]
    (is (= (->T 1 2) v))
    (is (= T (class v)))))

(deftest extra-key-literal-print-test
  ;; I-07: print drops __extmap.
  (let [lit (read-literal "#closed_record.literal_test.T{:a 1 :n 2 :x 3}")
        s   (pr-str lit)
        v   (read-literal s)]
    (is (= (pr-str (->T 1 2)) s))
    (is (= lit v))
    (is (identical? v (check v)))))

(deftest value-constant-test
  ;; V-05: a record value compiled as a constant in a function body.
  (let [v (constant "^{:m 1} #closed_record.literal_test.T{:a 1 :n 2}")]
    (is (= (->T 1 2) v))
    (is (= T (class v)))
    (is (= {:m 1} (meta v)))))

(deftest extra-key-constant-test
  ;; I-07, I-09: emitValue builds the constant from the Map view.
  (let [form '(closed-record.literal-test/constant
               "#closed_record.literal_test.T{:a 1 :n 2 :x 3}")
        e    (thrown #(check (in-test-ns (fn [] (macroexpand-1 form)))))]
    (is (= :closed-record/extra-keys (:type (ex-data e)))))
  (let [v (constant "#closed_record.literal_test.T{:a 1 :n 2 :x 3}")]
    (is (= (->T 1 2) v))
    (is (= T (class v)))
    (is (identical? v (check v)))))
