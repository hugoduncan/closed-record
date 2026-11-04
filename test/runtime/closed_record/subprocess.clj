(ns closed-record.subprocess
  "Run `clojure.main` in a child JVM with the runtime of the test JVM, and
  manage its temporary files."
  (:require
   [clojure.string :as str])
  (:import
   (java.io File)
   (java.lang.management ManagementFactory)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)
   (java.util.concurrent TimeUnit)))

(defn- java-binary
  []
  (str (System/getProperty "java.home") File/separator "bin" File/separator
       "java"))

(defn- input-arguments
  []
  (vec (.getInputArguments (ManagementFactory/getRuntimeMXBean))))

(def default-timeout-ms
  "The default time limit of a child JVM: 5 minutes."
  300000)

(defn run
  "Run `clojure.main` with `args` in a child JVM. The child uses the `java`
  binary of `java.home`, the JVM input arguments of the test JVM, then
  `:jvm-opts`, and a class path of `:class-path` entries before the test JVM
  class path. Give `{:exit :out :err}`. A child that does not exit in
  `:timeout-ms` (default `default-timeout-ms`) is destroyed; then `:exit`
  is nil, `:timed-out` is true, and `:out` and `:err` hold the output so
  far."
  [args & {:keys [jvm-opts class-path timeout-ms]
           :or {timeout-ms default-timeout-ms}}]
  (let [cp (str/join File/pathSeparator
                     (concat (map str class-path)
                             [(System/getProperty "java.class.path")]))
        cmd (concat [(java-binary)]
                    (input-arguments)
                    jvm-opts
                    ["-cp" cp "clojure.main"]
                    args)
        proc (.start (ProcessBuilder. ^java.util.List (mapv str cmd)))
        out (future (slurp (.getInputStream proc)))
        err (future (slurp (.getErrorStream proc)))]
    (if (.waitFor proc timeout-ms TimeUnit/MILLISECONDS)
      {:exit (.exitValue proc) :out @out :err @err}
      (do (.waitFor (.destroyForcibly proc))
          {:exit nil
           :timed-out true
           :out @out
           :err (str @err "\nclosed-record.subprocess: child JVM timed out after "
                     timeout-ms " ms")}))))

(defn temp-dir
  "Create a temporary directory with name prefix `prefix`; give the `File`."
  ^File [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn delete-tree
  "Delete `file` and, if it is a directory, all of its contents. Do not
  follow symbolic links."
  [^File file]
  (when (and (.isDirectory file)
             (not (Files/isSymbolicLink (.toPath file))))
    (run! delete-tree (.listFiles file)))
  (.delete file))
