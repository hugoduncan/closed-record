(ns closed-record.value-type.aot-test
  "VT-V7: compile a namespace with a `:value-type true` record, then load
  its classes, without the source, in another JVM of the same kind."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [closed-record.subprocess :as subprocess]
   [closed-record.value-type.expectation :as expectation])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def ^:private expected (expectation/representation))

(def ^:private source
  "(ns aot-app.geo
     (:require [closed-record.core :refer [defclosedrecord]]))

   (defclosedrecord Point [x y] :value-type true)")

(def ^:private load-script
  (pr-str
   '(do
      (require 'aot-app.geo)
      (require 'closed-record.value-type.expectation)
      (let [p ((resolve 'aot-app.geo/->Point) 1 2)
            q ((resolve 'aot-app.geo/map->Point) {:x 1 :y 2})
            c (class p)]
        (prn {:source (some? (clojure.java.io/resource "aot_app/geo.clj"))
              := (= p q)
              :hash= (= (hash p) (hash q))
              :assoc (into {} (assoc p :x 3))
              :meta (meta (with-meta p {:m 1}))
              :representation
              ((resolve 'closed-record.value-type.expectation/observed) c)})))))

(deftest aot-test
  (let [tmp (.toFile (Files/createTempDirectory
                      "closed-record-aot" (make-array FileAttribute 0)))
        src (io/file tmp "src")
        classes (io/file tmp "classes")
        file (io/file src "aot_app" "geo.clj")]
    (io/make-parents file)
    (spit file source)
    (.mkdirs classes)
    (let [compiled (subprocess/run
                    ["-e" (pr-str `(binding [*compile-path* ~(str classes)]
                                     (compile '~'aot-app.geo)))]
                    :class-path [src classes])]
      (is (= 0 (:exit compiled)) (:err compiled))
      (let [{:keys [exit out err]} (subprocess/run ["-e" load-script]
                                                   :class-path [classes])]
        (is (= 0 exit) err)
        (is (= {:source false
                := true
                :hash= true
                :assoc {:x 3 :y 2}
                :meta {:m 1}
                :representation expected}
               (edn/read-string out)))))))
