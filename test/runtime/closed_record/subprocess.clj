(ns closed-record.subprocess
  "Run `clojure.main` in a child JVM with the runtime of the test JVM."
  (:require
   [clojure.string :as str])
  (:import
   (java.io File)
   (java.lang.management ManagementFactory)))

(defn- java-binary
  []
  (str (System/getProperty "java.home") File/separator "bin" File/separator
       "java"))

(defn- input-arguments
  []
  (vec (.getInputArguments (ManagementFactory/getRuntimeMXBean))))

(defn run
  "Run `clojure.main` with `args` in a child JVM. The child uses the `java`
  binary of `java.home`, the JVM input arguments of the test JVM, then
  `:jvm-opts`, and a class path of `:class-path` entries before the test JVM
  class path. Give `{:exit :out :err}`."
  [args & {:keys [jvm-opts class-path]}]
  (let [cp (str/join File/pathSeparator
                     (concat (map str class-path)
                             [(System/getProperty "java.class.path")]))
        cmd (concat [(java-binary)]
                    (input-arguments)
                    jvm-opts
                    ["-cp" cp "clojure.main"]
                    args)
        proc (.start (ProcessBuilder. ^java.util.List (mapv str cmd)))
        err (future (slurp (.getErrorStream proc)))
        out (slurp (.getInputStream proc))]
    {:exit (.waitFor proc) :out out :err @err}))
