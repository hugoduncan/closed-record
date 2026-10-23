(ns build-test
  (:require
   [build]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
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

(defn- run-cli
  [directory config task]
  (shell/sh "clojure" "-T:build" task
            :dir (str directory)
            :env (assoc (into {} (System/getenv))
                        "CLJ_CONFIG" (str config)
                        "GIT_CEILING_DIRECTORIES" (str directory))))

(deftest no-git-build-and-clean-test
  ;; Real CLI processes test failure before writes and clean without Git.
  (let [directory (.toFile (Files/createTempDirectory
                            "closed-record-build-" (make-array FileAttribute 0)))
        config (io/file directory "cli-config")
        source (io/file directory "src/probe.clj")]
    (try
      (.mkdirs config)
      (spit (io/file config "deps.edn") "{}")
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
  [pom version]
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
    (is (= (:deps (edn/read-string (slurp "deps.edn")))
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
  [artifact directory]
  (let [basis (b/create-basis {:project "deps.edn" :user nil :aliases []})
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

(deftest artifact-and-stale-output-test
  ;; Inspect real build output; an exact file inventory rejects bundled tooling.
  (let [config (.toFile (Files/createTempDirectory
                         "closed-record-cli-" (make-array FileAttribute 0)))
        version (str "0.1." (str/trim (:out (shell/sh "git" "rev-list" "--count" "HEAD"))))
        artifact (io/file (str "target/closed-record-" version ".jar"))
        maven "META-INF/maven/org.hugoduncan/closed-record/"
        pom-path (str maven "pom.xml")
        properties-path (str maven "pom.properties")
        sentinel (io/file "target/classes/stale.txt")
        obsolete (io/file "target/obsolete.jar")]
    (try
      (spit (io/file config "deps.edn") "{}")
      (testing "two builds use the Git version and remove stale output"
        (is (= 0 (:exit (run-cli "." config "jar"))))
        (io/make-parents sentinel)
        (spit sentinel "stale staging")
        (spit obsolete "stale artifact")
        (let [result (run-cli "." config "jar")]
          (is (= 0 (:exit result)) (pr-str result)))
        (is (.isFile artifact))
        (is (not (.exists sentinel)))
        (is (not (.exists obsolete))))
      (with-open [archive (JarFile. artifact)]
        (testing "only runtime source, license, and standard metadata are packaged"
          (is (= #{"META-INF/MANIFEST.MF" "LICENSE"
                   "closed_record/core.cljc" "closed_record/impl/jvm.clj"
                   pom-path properties-path}
                 (into #{} (comp (remove #(.isDirectory %)) (map #(.getName %)))
                       (enumeration-seq (.entries archive)))))
          (doseq [[entry source] {"LICENSE" "LICENSE"
                                  "closed_record/core.cljc" "src/closed_record/core.cljc"
                                  "closed_record/impl/jvm.clj" "src/closed_record/impl/jvm.clj"
                                  pom-path (str "target/classes/" pom-path)}]
            (is (Arrays/equals (Files/readAllBytes (.toPath (io/file source)))
                               (entry-bytes archive entry)) entry)))
        (testing "generated and embedded Maven metadata agree"
          (check-pom (xml/parse (io/file "target/classes" pom-path)) version)
          (with-open [stream (.getInputStream archive (.getJarEntry archive pom-path))]
            (check-pom (xml/parse stream) version))
          (let [properties (Properties.)]
            (with-open [stream (.getInputStream archive (.getJarEntry archive properties-path))]
              (.load properties stream))
            (is (= {"groupId" "org.hugoduncan" "artifactId" "closed-record" "version" version}
                   (into {} properties))))))
      (testing "a consumer uses only the artifact and runtime dependency JARs"
        (check-isolated-consumer artifact config))
      (finally
        (b/delete {:path (str config)})))))
