(ns closed-record.value-type-option-test
  "VT-V1 in the ordinary suite: the `:value-type` option on a class path
  without value-type."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]
   [closed-record.expansion-fixture :as fixture]
   [closed-record.subprocess :as subprocess])
  (:import
   (java.io FileNotFoundException)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(deftest ordinary-expansion-test
  ;; VT-I2: the ordinary expansion equals the expansion fixture.
  (let [recorded (fixture/fixture)]
    (is (= (set (keys fixture/forms)) (set (keys recorded))))
    (doseq [[k form] fixture/forms]
      (testing (str k " with no option")
        (is (= (recorded k) (fixture/expansion form))))
      (testing (str k " with :value-type false")
        (is (= (recorded k)
               (fixture/expansion
                (fixture/with-option form :value-type false))))))))

(defn- expansion-error
  "The exception of the macroexpansion of `form`, without the
  `CompilerException` wrapper. The expansion when there is no exception."
  [form]
  (try
    (macroexpand-1 form)
    (catch clojure.lang.Compiler$CompilerException t (ex-cause t))))

(deftest invalid-option-value-test
  (doseq [v [nil 1 :yes "true" 'true?]]
    (testing (pr-str v)
      (let [e (expansion-error
               `(defclosedrecord ~'Bad [~'a] :value-type ~v))]
        (is (instance? IllegalArgumentException e))))))

(deftest value-type-unavailable-test
  (let [e (binding [*ns* (the-ns 'closed-record.value-type-option-test)]
            (expansion-error
             `(defclosedrecord ~'Point [~'x] :value-type true :no-print true)))]
    (is (= {:type :closed-record/value-type-unavailable
            :class-name 'closed_record.value_type_option_test.Point}
           (ex-data e)))
    (is (symbol? (:class-name (ex-data e))))
    (is (instance? FileNotFoundException (ex-cause e)))))

(deftest absent-var-test
  ;; value-type.core loads but has no defvaluetype*: the error has no cause.
  (let [dir (.toFile (Files/createTempDirectory
                      "closed-record-stub" (make-array FileAttribute 0)))
        stub (io/file dir "value_type" "core.clj")
        script (pr-str
                '(do
                   (require 'closed-record.core)
                   (in-ns 'my-app.geo)
                   (clojure.core/refer-clojure)
                   (let [e (try
                             (macroexpand-1
                              '(closed-record.core/defclosedrecord
                                 Point [x] :value-type true))
                             nil
                             (catch Throwable t (ex-cause t)))]
                     (prn {:data (ex-data e)
                           :cause (some-> (ex-cause e) class .getName)}))))]
    (io/make-parents stub)
    (spit stub "(ns value-type.core)")
    (let [{:keys [exit out err]} (subprocess/run ["-e" script]
                                                 :class-path [dir])]
      (is (= 0 exit) err)
      (is (= {:data {:type :closed-record/value-type-unavailable
                     :class-name 'my_app.geo.Point}
              :cause nil}
             (edn/read-string out))))))
