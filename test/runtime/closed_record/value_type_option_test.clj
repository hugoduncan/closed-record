(ns closed-record.value-type-option-test
  "VT-V1 and VT-V10 in the ordinary suite: the `:value-type` option on a
  class path without value-type, and `:non-nil` field metadata with no
  option."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [closed-record.core :refer [defclosedrecord]]
   [closed-record.expansion-fixture :as fixture]
   [closed-record.form-support :as form]
   [closed-record.probe-support :refer [expansion-error]]
   [closed-record.subprocess :as subprocess])
  (:import
   (java.io FileNotFoundException)))

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
                (form/with-options form [:value-type false]))))))))

(defclosedrecord NonNilField [^String ^:non-nil s])

(deftest non-nil-ordinary-test
  ;; VT-V10: without `:value-type true`, `:non-nil` has no effect.
  (testing "->Name"
    (is (nil? (:s (->NonNilField nil)))))
  (testing "map->Name"
    (is (nil? (:s (map->NonNilField {:s nil})))))
  (testing "assoc"
    (is (nil? (:s (assoc (->NonNilField "a") :s nil)))))
  (testing "field storage type"
    (is (= Object (.getType (.getDeclaredField NonNilField "s"))))))

(deftest invalid-option-value-test
  (doseq [v [nil 1 :yes "true" 'true?]]
    (testing (pr-str v)
      (let [e (expansion-error
               'closed-record.value-type-option-test
               `(defclosedrecord ~'Bad [~'a] :value-type ~v))]
        (is (instance? IllegalArgumentException e))))))

(deftest value-type-unavailable-test
  (let [e (expansion-error
           'closed-record.value-type-option-test
           `(defclosedrecord ~'Point [~'x] :value-type true :no-print true))]
    (is (= {:type :closed-record/value-type-unavailable
            :class-name 'closed_record.value_type_option_test.Point}
           (ex-data e)))
    (is (instance? FileNotFoundException (ex-cause e)))))

(deftest absent-var-test
  ;; value-type.core loads but has no defvaluetype*: the error has no cause.
  (let [dir (subprocess/temp-dir "closed-record-stub")
        stub (io/file dir "value_type" "core.clj")
        script (pr-str
                '(do
                   (require 'closed-record.core 'closed-record.probe-support)
                   (create-ns 'my-app.geo)
                   (let [e (closed-record.probe-support/expansion-error
                            'my-app.geo
                            '(closed-record.core/defclosedrecord
                               Point [x] :value-type true))]
                     (prn {:data (ex-data e)
                           :cause (some-> (ex-cause e) class .getName)}))))]
    (try
      (io/make-parents stub)
      (spit stub "(ns value-type.core)")
      (let [{:keys [exit out err]} (subprocess/run ["-e" script]
                                                   :class-path [dir])]
        (is (= 0 exit) err)
        (is (= {:data {:type :closed-record/value-type-unavailable
                       :class-name 'my_app.geo.Point}
                :cause nil}
               (edn/read-string out))))
      (finally (subprocess/delete-tree dir)))))
