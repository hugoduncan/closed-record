(ns closed-record.api-docs-cli-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [closed-record.test-support.api-project :as api-project]
            [clojure.test :refer [deftest is]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- command [root & args]
  (apply shell/sh "bb" "api-docs" (concat args [:dir root])))

(deftest reference-file-lifecycle-test
  ;; Real task processes check bytes without writes and repair only on request.
  (api-project/with-project
    (fn [root]
      (let [output (io/file root "doc/API.md")
            source (io/file root "src/closed_record/core.cljc")
            documentation "Return x unchanged: café, λ, 日本語."
            _ (spit source
                    (str/replace (slurp source :encoding "UTF-8")
                                 "Return x if x is a closed record with no extra literal keys."
                                 documentation)
                    :encoding "UTF-8")
            source-bytes (vec (Files/readAllBytes (.toPath source)))
            missing (command root "--check")]
        (is (pos? (:exit missing)))
        (is (str/includes? (:err missing) "Run bb api-docs"))
        (is (not (.exists output)))
        (is (zero? (:exit (command root))))
        (let [generated (Files/readAllBytes (.toPath output))
              expected (vec (.getBytes documentation "UTF-8"))]
          (is (some #(= expected (vec %))
                    (partition (count expected) 1 generated))
              "Generated reference contains the exact UTF-8 docstring bytes")
          (is (zero? (:exit (command root "--check"))))
          (is (= source-bytes (vec (Files/readAllBytes (.toPath source)))))
          (is (= (vec generated) (vec (Files/readAllBytes (.toPath output)))))
          (is (zero? (:exit (command root))))
          (is (= (vec generated) (vec (Files/readAllBytes (.toPath output)))))
          (spit output "stale reference λ\n" :encoding "UTF-8")
          (let [stale (Files/readAllBytes (.toPath output))
                result (command root "--check")]
            (is (pos? (:exit result)))
            (is (str/includes? (:err result) "Run bb api-docs"))
            (is (= (vec stale) (vec (Files/readAllBytes (.toPath output)))))
            (doseq [args [["--unknown"] ["--check" "extra"]]]
              (let [invalid (apply command root args)]
                (is (pos? (:exit invalid)))
                (is (str/includes? (:err invalid) "Usage: bb api-docs"))
                (is (= (vec stale) (vec (Files/readAllBytes (.toPath output)))))))))
        (is (zero? (:exit (command root))))
        (is (zero? (:exit (command root "--check"))))))))

(deftest jvm-reader-branch-test
  ;; Run the owned generator on divergent reader branches through real Quickdoc.
  (api-project/with-project
    (fn [root]
      (spit (io/file root "src/closed_record/core.cljc")
            (str "(ns closed-record.core)\n"
                 "#?(:clj (defn check \"JVM branch contract.\" [x] x)\n"
                 "   :cljs (defn check \"JavaScript branch contract.\" [x y] y))\n"
                 "#?(:cljs (defn browser-only \"Browser API.\" [] nil))\n"))
      (let [result (command root)]
        (is (zero? (:exit result)) (pr-str result))
        (when (zero? (:exit result))
          (let [markdown (slurp (io/file root "doc/API.md"))]
            (is (str/includes? markdown "JVM branch contract."))
            (is (str/includes? markdown "(check x)"))
            (is (not (str/includes? markdown "JavaScript branch contract.")))
            (is (not (str/includes? markdown "browser-only")))))))))

(defn- git! [root environment & args]
  (let [result (apply shell/sh "git" (concat args [:dir root :env environment]))]
    (when-not (zero? (:exit result))
      (throw (ex-info "Fixture Git command failed" result)))
    (:out result)))

(defn- tracked-state [root environment]
  {:index (git! root environment "write-tree")
   :files (into {} (map (fn [path]
                          [path (vec (Files/readAllBytes (.toPath (io/file root path))))]))
                (.split (git! root environment "ls-files" "-z") "\u0000"))})

(deftest reference-line-ending-lifecycle-test
  ;; Freshness must reject CRLF without changing bytes or the staged reference.
  (api-project/with-project
    (fn [root]
      (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                     (System/getenv))
                               "GIT_CONFIG_NOSYSTEM" "1"
                               "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
            output (io/file root "doc/API.md")]
        (is (zero? (:exit (command root))))
        (let [original (vec (Files/readAllBytes (.toPath output)))
              markdown (slurp output :encoding "UTF-8")]
          (is (str/includes? markdown "\n"))
          (is (not (str/includes? markdown "\r")))
          (git! root environment "init")
          (git! root environment "config" "core.autocrlf" "false")
          (git! root environment "add" ".")
          (let [index (git! root environment "write-tree")]
            (spit output (str/replace markdown "\n" "\r\n") :encoding "UTF-8")
            (let [before (tracked-state root environment)
                  result (command root "--check")]
              (is (not= original (get-in before [:files "doc/API.md"])))
              (is (= 1 (:exit result)) (pr-str result))
              (is (str/includes? (:err result) "Run bb api-docs"))
              (is (= before (tracked-state root environment))))
            (is (zero? (:exit (command root))))
            (is (= original (vec (Files/readAllBytes (.toPath output)))))
            (let [before (tracked-state root environment)
                  result (command root "--check")]
              (is (zero? (:exit result)) (pr-str result))
              (is (= before (tracked-state root environment))))
            (is (= index (git! root environment "write-tree")))))))))

(deftest global-analysis-configuration-test
  ;; Real child processes must produce the same API under hostile user settings.
  (api-project/with-project
    (fn [root]
      (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                     (System/getenv))
                               "XDG_CONFIG_HOME" (str (io/file root "user-config"))
                               "GIT_CONFIG_NOSYSTEM" "1"
                               "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
            run-api (fn [& args]
                      (apply shell/sh "bb" "api-docs"
                             (concat args [:dir root :env environment])))
            output (io/file root "doc/API.md")
            global (io/file root "user-config/clj-kondo/config.edn")]
        (is (zero? (:exit (run-api))))
        (let [baseline (vec (Files/readAllBytes (.toPath output)))]
          (git! root environment "init")
          (git! root environment "config" "core.autocrlf" "false")
          (spit (io/file root "unrelated.md") "Original prose.\n")
          (with-open [stream (io/output-stream (io/file root "unrelated.bin"))]
            (.write stream (byte-array [0 -1 13 10])))
          (git! root environment "add" ".")
          (spit (io/file root "unrelated.md") "Unstaged prose.\r\n")
          (io/make-parents global)
          (doseq [config ["{}"
                          "{:lint-as {clojure.core/defn clojure.core/comment clojure.core/defmacro clojure.core/comment}}"
                          "not valid EDN {"]]
            (spit global config)
            (let [before (tracked-state root environment)
                  check (run-api "--check")]
              (is (zero? (:exit check)) (pr-str check))
              (is (= before (tracked-state root environment))))
            (is (zero? (:exit (run-api))))
            (is (= baseline (vec (Files/readAllBytes (.toPath output)))))
            (is (= ["check" "defclosedrecord"]
                   (mapv second (re-seq #"## <a name=\"closed-record.core/([^\"]+)\""
                                        (slurp output)))))
            (spit output "Stale API.\r\n")
            (let [before (tracked-state root environment)]
              (is (= 1 (:exit (run-api "--check"))))
              (is (= before (tracked-state root environment))))
            (is (zero? (:exit (run-api))))))))))

(deftest missing-source-test
  ;; Missing input must never replace a reference or create an empty one.
  (api-project/with-project
    (fn [root]
      (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                     (System/getenv))
                               "GIT_CONFIG_NOSYSTEM" "1"
                               "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
            output (io/file root "doc/API.md")]
        (is (zero? (:exit (command root))))
        (git! root environment "init")
        (git! root environment "add" ".")
        (git! root environment "rm" "-f" "src/closed_record/core.cljc")
        (doseq [existing? [true false]]
          (when-not existing?
            (git! root environment "rm" "-f" "doc/API.md"))
          (let [before (tracked-state root environment)]
            (doseq [args [[] ["--check"]]]
              (let [result (apply command root args)]
                (is (pos? (:exit result)) (pr-str result))
                (is (str/includes? (:err result) "src/closed_record/core.cljc"))
                (is (= before (tracked-state root environment)))
                (is (= existing? (.exists output)))))))))))

(deftest malformed-source-test
  ;; Analysis errors must stop both commands before any reference write.
  (api-project/with-project
    (fn [root]
      (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                     (System/getenv))
                               "GIT_CONFIG_NOSYSTEM" "1"
                               "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
            output (io/file root "doc/API.md")]
        (is (zero? (:exit (command root))))
        (git! root environment "init")
        (git! root environment "add" ".")
        (spit (io/file root "src/closed_record/core.cljc")
              "(ns closed-record.core)\n(defn check [x]\n")
        (doseq [existing? [true false]]
          (when-not existing?
            (git! root environment "rm" "-f" "doc/API.md"))
          (let [before (tracked-state root environment)]
            (doseq [args [[] ["--check"]]]
              (let [result (apply command root args)]
                (is (pos? (:exit result)) (pr-str result))
                (is (str/includes? (:err result) "API source analysis failed"))
                (is (str/includes? (:err result) "src/closed_record/core.cljc"))
                (is (str/includes? (:err result) ":syntax"))
                (is (= before (tracked-state root environment)))
                (is (= existing? (.exists output)))))))))))

(deftest missing-analysis-configuration-test
  ;; Missing owned reader policy must stop generation before any output write.
  (api-project/with-project
    (fn [root]
      (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                     (System/getenv))
                               "GIT_CONFIG_NOSYSTEM" "1"
                               "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
            output (io/file root "doc/API.md")]
        (is (zero? (:exit (command root))))
        (git! root environment "init")
        (git! root environment "add" ".")
        (git! root environment "rm" "-f" ".clj-kondo/config.edn")
        (doseq [existing? [true false]]
          (when-not existing?
            (git! root environment "rm" "-f" "doc/API.md"))
          (let [before (tracked-state root environment)]
            (doseq [args [[] ["--check"]]]
              (let [result (apply command root args)]
                (is (pos? (:exit result)) (pr-str result))
                (is (str/includes? (:err result) ".clj-kondo/config.edn"))
                (is (= before (tracked-state root environment)))
                (is (= existing? (.exists output)))))))))))

(deftest linked-output-test
  ;; Neither output-file nor parent links may expose external files to writes.
  (api-project/with-project
    (fn [external]
      (let [target (io/file external "API.md")]
        (spit target "Unrelated external prose.\r\n" :encoding "UTF-8")
        (doseq [parent? [false true]]
          (api-project/with-project
            (fn [root]
              (let [environment (assoc (into {} (remove (fn [[key _]] (.startsWith key "GIT_")))
                                             (System/getenv))
                                       "GIT_CONFIG_NOSYSTEM" "1"
                                       "GIT_CONFIG_GLOBAL" (str (io/file root "gitconfig")))
                    link (.toPath (io/file root (if parent? "doc" "doc/API.md")))
                    destination (.toPath (if parent? external target))]
                (when-not parent? (io/make-parents (.toFile link)))
                (Files/createSymbolicLink link destination (make-array FileAttribute 0))
                (try
                  (git! root environment "init")
                  (git! root environment "add" ".")
                  (let [index (git! root environment "write-tree")
                        bytes (vec (Files/readAllBytes (.toPath target)))]
                    (doseq [args [[] ["--check"]]]
                      (let [result (apply command root args)]
                        (is (pos? (:exit result)) (pr-str result))
                        (is (str/includes? (:err result) "doc/API.md"))
                        (is (str/includes? (:err result) "Symbolic links"))
                        (is (= bytes (vec (Files/readAllBytes (.toPath target)))))
                        (is (= destination (Files/readSymbolicLink link)))
                        (is (= index (git! root environment "write-tree"))))))
                  (finally (Files/delete link)))))))))))