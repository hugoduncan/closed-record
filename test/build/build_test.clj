(ns build-test
  (:require
   [build]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clojure.tools.build.api :as b]
   [clojure.xml :as xml])
  (:import
   [java.nio.file Files]
   [java.nio.file.attribute FileAttribute]
   [java.util Properties Arrays]
   [java.util.jar JarFile]))

(deftest version-result-test
  ;; Version validation must never invent a fallback version.
  (testing "a numeric Git result supplies the version"
    (is (= "0.1.42" (#'build/version-from-result {:exit 0 :out "42\n"}))))
  (testing "failed Git results retain error data"
    (try
      (#'build/version-from-result {:exit 128 :out "" :err "not a repository"})
      (is false "Expected version failure")
      (catch clojure.lang.ExceptionInfo error
        (is (str/includes? (ex-message error) "git rev-list"))
        (is (= {:exit 128 :err "not a repository"} (ex-data error))))))
  (testing "invalid counts retain the rejected output"
    (doseq [output ["" "\n" "abc" "42x" "-1" "1.5"]]
      (try
        (#'build/version-from-result {:exit 0 :out output})
        (is false (str "Expected rejection of " (pr-str output)))
        (catch clojure.lang.ExceptionInfo error
          (is (str/includes? (ex-message error) "invalid commit count"))
          (is (= output (:out (ex-data error)))))))))

(defn- prepare-config
  [config]
  (.mkdirs config)
  (spit (io/file config "deps.edn") "{}")
  (let [hooks (io/file config "empty-hooks")]
    (.mkdirs hooks)
    (spit (io/file config "gitconfig")
          (str "[core]\n\thooksPath = " (pr-str (str hooks)) "\n"
               "[user]\n\tname = Build Test\n"
               "\temail = build-test@example.invalid\n"
               "[commit]\n\tgpgsign = false\n"))))

(defn- fixture-env
  [directory config]
  ;; Git overrides can redirect writes outside the fixture, including from builds.
  (assoc (into {} (remove (fn [[key _]] (str/starts-with? key "GIT_")))
               (System/getenv))
         "CLJ_CONFIG" (str config)
         "GIT_CONFIG_NOSYSTEM" "1"
         "GIT_CONFIG_GLOBAL" (str (io/file config "gitconfig"))
         "GIT_CEILING_DIRECTORIES" (str directory)))

(defn- run-cli
  [directory config task]
  (shell/sh "clojure" "-T:build" task
            :dir (str directory)
            :env (fixture-env directory config)))

(deftest no-git-build-and-clean-test
  ;; Real CLI processes test failure before writes and clean without Git.
  (let [directory (.toFile (Files/createTempDirectory
                            "closed-record-build-" (make-array FileAttribute 0)))
        config (io/file directory "cli-config")
        source (io/file directory "src/probe.clj")]
    (try
      (prepare-config config)
      (doseq [path ["deps.edn" "build.clj" "LICENSE"]]
        (io/copy (io/file path) (io/file directory path)))
      (io/make-parents source)
      (spit source "(ns probe)\n")
      (testing "missing Git metadata fails before artifact writes"
        (let [result (run-cli directory config "jar")]
          (is (not= 0 (:exit result)) (pr-str result))
          (is (str/includes? (:err result) "Unable to compute version")
              (pr-str result))
          (is (not (.exists (io/file directory "target"))))))
      (testing "clean removes output, is repeatable, and preserves source"
        (let [sentinel (io/file directory "target/stale")]
          (io/make-parents sentinel)
          (spit sentinel "stale"))
        (dotimes [_ 2]
          (let [result (run-cli directory config "clean")]
            (is (= 0 (:exit result)) (pr-str result))
            (is (not (.exists (io/file directory "target")))))))
      (is (= "(ns probe)\n" (slurp source)))
      (finally
        (b/delete {:path (str directory)})))))

(defn- child-elements
  [element tag]
  (filterv #(and (map? %) (= tag (:tag %))) (:content element)))

(defn- element-text
  [element & path]
  (apply str (:content (reduce #(first (child-elements %1 %2)) element path))))

(defn- check-pom
  [directory pom version]
  (doseq [[tag expected] {:groupId "org.hugoduncan"
                          :artifactId "closed-record"
                          :version version
                          :name "closed-record"
                          :description "A Clojure record type with a fixed key set."
                          :url "https://github.com/hugoduncan/closed-record"}]
    (is (= expected (element-text pom tag)) (name tag)))
  (let [scm (first (child-elements pom :scm))
        license (first (child-elements (first (child-elements pom :licenses)) :license))
        dependencies (child-elements (first (child-elements pom :dependencies)) :dependency)]
    (is (= "https://github.com/hugoduncan/closed-record" (element-text scm :url)))
    (is (= "scm:git:https://github.com/hugoduncan/closed-record.git"
           (element-text scm :connection)))
    (is (= "scm:git:ssh://git@github.com/hugoduncan/closed-record.git"
           (element-text scm :developerConnection)))
    (is (empty? (child-elements scm :tag)))
    (is (= "Eclipse Public License 2.0" (element-text license :name)))
    (is (= "https://www.eclipse.org/legal/epl-2.0/" (element-text license :url)))
    (is (= "repo" (element-text license :distribution)))
    (is (= (:deps (edn/read-string (slurp (io/file directory "deps.edn"))))
           (into {} (map (fn [dependency]
                           [(symbol (element-text dependency :groupId)
                                    (element-text dependency :artifactId))
                            {:mvn/version (element-text dependency :version)}]))
                 dependencies)))))

(defn- entry-bytes
  [archive path]
  (with-open [stream (.getInputStream archive (.getJarEntry archive path))]
    (.readAllBytes stream)))

(defn- check-isolated-consumer
  [project artifact directory]
  (let [basis (b/create-basis {:project (str (io/file project "deps.edn")) :user nil :aliases []})
        runtime-jars (into [] (mapcat :paths) (vals (:libs basis)))
        artifact-path (.getCanonicalPath artifact)
        classpath (str/join java.io.File/pathSeparator
                            (into [artifact-path] runtime-jars))
        script (io/file directory "consumer.clj")]
    (is (seq runtime-jars))
    (is (every? #(and (.isFile (io/file %)) (str/ends-with? % ".jar"))
                runtime-jars))
    (spit script
          (pr-str
           '(do
              (require '[clojure.java.io :as io]
                       '[closed-record.core :refer [defclosedrecord]])
              (defclosedrecord ConsumerRecord [value])
              (let [record (->ConsumerRecord 7)
                    error-type (fn [operation]
                                 (try
                                   (operation)
                                   nil
                                   (catch clojure.lang.ExceptionInfo error
                                     (:type (ex-data error)))))]
                (prn {:resources (mapv #(str (io/resource %))
                                       ["closed_record/core.cljc"
                                        "closed_record/impl/jvm.clj"])
                      :value (:value record)
                      :updated (:value (assoc record :value 9))
                      :assoc-error (error-type #(assoc record :extra 1))
                      :dissoc-error (error-type #(dissoc record :value))})))))
    (let [result (shell/sh (str (io/file (System/getProperty "java.home") "bin/java"))
                           "-cp" classpath "clojure.main" (str script)
                           :dir (str directory))]
      (is (= 0 (:exit result)) (pr-str result))
      (when (zero? (:exit result))
        (is (= {:resources
                (mapv #(str "jar:" (.toURL (.toURI (io/file artifact-path))) "!/" %)
                      ["closed_record/core.cljc" "closed_record/impl/jvm.clj"])
                :value 7
                :updated 9
                :assoc-error :closed-record/undeclared-key
                :dissoc-error :closed-record/declared-key-dissoc}
               (edn/read-string (:out result))))))))

(defn- prepare-artifact-project
  [directory config]
  (prepare-config config)
  (b/copy-dir {:src-dirs ["src"] :target-dir (str (io/file directory "src"))})
  (doseq [path ["deps.edn" "build.clj" "LICENSE"]]
    (io/copy (io/file path) (io/file directory path)))
  (spit (io/file directory "src/packaging_probe.clj") "(ns packaging-probe)\n")
  (doseq [path ["test/excluded_test.clj"
                "dev/bench/excluded_bench.clj"
                "dev/release/excluded_release.clj"
                "test/release/excluded_test.clj"
                "munera/excluded-task.md"
                "mementum/excluded-memory.md"]]
    (let [probe (io/file directory path)]
      (io/make-parents probe)
      (spit probe "Excluded from the library JAR.\n")))
  (doseq [command [["git" "init"]
                   ["git" "add" "src" "deps.edn" "build.clj" "LICENSE"]
                   ["git" "commit" "-m" "Fixture"]]]
    (let [result (apply shell/sh (concat command [:dir (str directory)
                                                  :env (fixture-env directory config)]))]
      (when-not (zero? (:exit result))
        (throw (ex-info "Cannot create Git fixture" result))))))

(defn- source-inventory
  [directory]
  (let [source-root (.toPath (io/file directory "src"))]
    (into {}
          (comp (filter #(.isFile %))
                (map (fn [file]
                       [(str (.relativize source-root (.toPath file))) file])))
          (file-seq (io/file directory "src")))))

(defn- archive-entries
  [archive]
  (into #{} (comp (remove #(.isDirectory %)) (map #(.getName %)))
        (enumeration-seq (.entries archive))))

(defn- check-archive-content
  [directory archive sources entries pom-path properties-path]
  (let [required (into #{"LICENSE" pom-path properties-path} (keys sources))
        extra (set/difference entries required)]
    (is (set/subset? #{"closed_record/core.cljc" "closed_record/impl/jvm.clj"}
                     entries))
    (is (empty? (set/difference required entries)) "Required files are packaged")
    (is (every? #(re-matches #"META-INF/(MANIFEST\.MF|INDEX\.LIST|[^/]+\.(SF|RSA|DSA|EC))" %)
                extra)
        (str "Unexpected archive entries: " extra))
    (is (not-any? #(or (str/ends-with? % ".class")
                       (str/ends-with? % ".jar")) entries)
        "No compiled classes or bundled JARs")
    (is (not (contains? entries "stale.txt")))
    (doseq [[entry source] (assoc sources
                                  "LICENSE" (io/file directory "LICENSE")
                                  pom-path (io/file directory "target/classes" pom-path))]
      (is (Arrays/equals (Files/readAllBytes (.toPath source))
                         (entry-bytes archive entry)) entry))))

(defn- check-artifact
  [directory artifact version]
  (let [maven "META-INF/maven/org.hugoduncan/closed-record/"
        pom-path (str maven "pom.xml")
        properties-path (str maven "pom.properties")
        sources (source-inventory directory)]
    (with-open [archive (JarFile. artifact)]
      (testing "only runtime source, license, and standard metadata are packaged"
        (check-archive-content directory archive sources (archive-entries archive)
                               pom-path properties-path))
      (testing "generated and embedded Maven metadata agree"
        (check-pom directory (xml/parse (io/file directory "target/classes" pom-path)) version)
        (with-open [stream (.getInputStream archive (.getJarEntry archive pom-path))]
          (check-pom directory (xml/parse stream) version))
        (let [properties (Properties.)]
          (with-open [stream (.getInputStream archive (.getJarEntry archive properties-path))]
            (.load properties stream))
          (is (= {"groupId" "org.hugoduncan" "artifactId" "closed-record" "version" version}
                 (into {} properties))))))))

(deftest artifact-and-stale-output-test
  ;; Inspect required content and reject files outside the source-only contract.
  (let [directory (.toFile (Files/createTempDirectory
                            "closed-record-artifact-" (make-array FileAttribute 0)))
        config (io/file directory "cli-config")
        version "0.1.1"
        artifact (io/file directory (str "target/closed-record-" version ".jar"))
        sentinel (io/file directory "target/classes/stale.txt")
        obsolete (io/file directory "target/obsolete.jar")]
    (try
      (prepare-artifact-project directory config)
      (testing "two builds use the Git version and remove stale output"
        (let [result (shell/sh "clojure" "-M:build-test:clj-1.12" "-e"
                               "(require 'build) (prn (build/jar nil))"
                               :dir (str directory)
                               :env (fixture-env directory config))
              artifact-map (when (zero? (:exit result)) (edn/read-string (:out result)))]
          (is (= 0 (:exit result)) (pr-str result))
          (is (= {:version version
                  :jar-file (str "target/closed-record-" version ".jar")
                  :pom-file "target/classes/META-INF/maven/org.hugoduncan/closed-record/pom.xml"}
                 artifact-map))
          (doseq [key [:jar-file :pom-file]]
            (is (.isFile (io/file directory (get artifact-map key "missing"))))))
        (io/make-parents sentinel)
        (spit sentinel "stale staging")
        (spit obsolete "stale artifact")
        (let [result (run-cli directory config "jar")]
          (is (= 0 (:exit result)) (pr-str result)))
        (is (.isFile artifact))
        (is (not (.exists sentinel)))
        (is (not (.exists obsolete))))
      (check-artifact directory artifact version)
      (testing "a consumer uses only the artifact and runtime dependency JARs"
        (check-isolated-consumer directory artifact config))
      (finally
        (b/delete {:path (str directory)})))))

(deftest deployment-output-test
  ;; Step outputs append artifact data without replacing earlier outputs.
  (let [file (java.io.File/createTempFile "deploy-output-" ".txt")]
    (try
      (spit file "prior=value\n")
      (#'build/write-github-output! file
                                    {:version "0.1.9" :jar-file "target/library.jar" :pom-file "target/pom.xml"})
      (is (= "prior=value\nversion=0.1.9\njar-file=target/library.jar\npom-file=target/pom.xml\n"
             (slurp file)))
      (finally (.delete file)))))

(deftest deployment-credentials-test
  ;; Invalid credentials must fail before any build or dependency loading.
  (let [directory (.toFile (Files/createTempDirectory
                            "closed-record-deploy-" (make-array FileAttribute 0)))
        config (io/file directory "cli-config")
        sentinel (io/file directory "target/keep")
        secret "fixture-secret-never-print"]
    (try
      (prepare-config config)
      (doseq [path ["deps.edn" "build.clj"]]
        (io/copy (io/file path) (io/file directory path)))
      (io/make-parents sentinel)
      (spit sentinel "unchanged")
      (doseq [missing ["CLOJARS_USERNAME" "CLOJARS_PASSWORD"]
              value [nil "" " \t"]]
        (testing (str missing " rejects " (pr-str value))
          (let [environment (assoc (fixture-env directory config)
                                   "CLOJARS_USERNAME" secret "CLOJARS_PASSWORD" secret)
                result (shell/sh "clojure" "-T:build" "deploy"
                                 :dir (str directory)
                                 :env (if (nil? value)
                                        (dissoc environment missing)
                                        (assoc environment missing value)))
                diagnostic (str (:out result) (:err result))]
            (is (not= 0 (:exit result)))
            (is (str/includes? diagnostic missing))
            (is (not (str/includes? diagnostic secret)))
            (is (= "unchanged" (slurp sentinel))))))
      (finally (b/delete {:path (str directory)})))))

(deftest valid-deployment-credentials-test
  ;; Nonblank fixture credentials pass validation without building or deploying.
  (testing "both deployment credentials are present"
    (is (nil? (#'build/assert-deploy-credentials!
               {"CLOJARS_USERNAME" "fixture-user"
                "CLOJARS_PASSWORD" "fixture-password"})))))
