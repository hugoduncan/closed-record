(ns link-check-test
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing]]
            [org.httpkit.server :as http]))

(def ^:private project-root (str (fs/absolutize ".")))
(def ^:private bb-bin (str (fs/which "bb")))

(defn- put! [root path content]
  (let [file (fs/path root path)]
    (fs/create-dirs (fs/parent file))
    (spit (str file) content)))

(defn- command [fixture & args]
  (apply process/shell
         {:dir (:root fixture) :env (:env fixture)
          :out :string :err :string :continue true}
         args))

(defn- git! [fixture & args]
  (process/check (apply command fixture "git" args)))

(defn- with-project [f]
  (let [workspace (fs/create-temp-dir {:prefix "link-contract-"})
        root (fs/path workspace "repo")
        env (into {} (remove (fn [[k _]] (str/starts-with? k "GIT_")))
                  (System/getenv))
        fixture {:root root
                 :env (assoc env "GIT_CONFIG_NOSYSTEM" "1"
                             "GIT_CONFIG_GLOBAL" (str (fs/path workspace "gitconfig")))}]
    (try
      (fs/create-dirs root)
      (put! workspace "gitconfig" "")
      (put! workspace "ignore" "ignored.md\n")
      (fs/create-dirs (fs/path workspace "hooks"))
      (git! fixture "init" "--quiet")
      (git! fixture "config" "core.hooksPath" (str (fs/path workspace "hooks")))
      (git! fixture "config" "core.excludesFile" (str (fs/path workspace "ignore")))
      (doseq [path ["bb.edn" "dev/link_check.clj" "lychee.toml"]]
        (put! root path (slurp (str (fs/path project-root path)))))
      (put! root "source.md" "# Source\n")
      (git! fixture "add" ".")
      (f fixture)
      (finally
        (fs/delete-tree workspace)))))

(defn- inventory [root]
  (into (sorted-map)
        (comp (filter fs/regular-file?)
              (map (fn [path]
                     [(str (fs/relativize root path))
                      (vec (fs/read-all-bytes path))])))
        (fs/glob root "**" {:hidden true})))

(defn- check! [fixture]
  (let [before (inventory (:root fixture))
        status (:out (git! fixture "status" "--porcelain"))
        child (process/process [bb-bin "lychee:check"]
                               {:dir (:root fixture) :env (:env fixture)
                                :out :string :err :string})
        result (try
                 (or (deref child 90000 nil)
                     (throw (ex-info "Link check exceeded 90 seconds" {})))
                 (finally (process/destroy-tree child)))]
    (is (= before (inventory (:root fixture))) "Check must not write files")
    (is (= status (:out (git! fixture "status" "--porcelain")))
        "Check must not change Git status")
    result))

(defn- failure! [result & diagnostics]
  (is (not (zero? (:exit result))) (pr-str result))
  (doseq [text diagnostics]
    (is (str/includes? (str (:out result) (:err result)) text)
        (str "Expected diagnostic: " text "\n" (pr-str result)))))

(deftest local-paths-test
  ;; Exercise source-relative resolution and GitHub heading rules together.
  (with-project
    (fn [{:keys [root] :as fixture}]
      (put! root "docs/source with spaces.md"
            (str "# Source\n[Self](#source)\n[File](../asset.txt)\n"
                 "[Directory](../assets/)\n[Root](/asset.txt)\n"
                 "[Heading](<target with spaces.md#heading>)\n"
                 "[Duplicate](<target with spaces.md#heading-1>)\n"))
      (put! root "docs/target with spaces.md" "# Heading\n# Heading\n")
      (put! root "asset.txt" "data")
      (fs/create-dirs (fs/path root "assets"))
      (git! fixture "add" ".")
      (is (zero? (:exit (check! fixture)))))))

(deftest tracked-selection-test
  ;; A broken link proves each tracked document is actually inspected.
  (doseq [path ["root.md" "doc/a.md" "ramora/a.md" "mementum/a.md"
                "munera/open/a.md" "munera/closed/a.md" ".hidden/a.md"
                "ignored.md"]]
    (testing path
      (with-project
        (fn [{:keys [root] :as fixture}]
          (put! root path "[Broken](/missing-target.txt)\n")
          (git! fixture "add" "--force" path)
          (failure! (check! fixture) path "missing-target.txt"))))))

(deftest untracked-selection-test
  ;; Untracked output and sibling documents are not input documents.
  (with-project
    (fn [{:keys [root] :as fixture}]
      (doseq [path ["untracked.md" "target/generated.md" "../outside.md"]]
        (put! root path "[Broken](missing-target.txt)\n"))
      (is (zero? (:exit (check! fixture)))))))

(deftest missing-local-targets-test
  ;; Each local failure retains source and target diagnostics.
  (doseq [target ["missing.txt" "missing-directory/" "#absent" "other.md#absent"]]
    (testing target
      (with-project
        (fn [{:keys [root] :as fixture}]
          (put! root "source.md" (str "# Source\n[Broken](" target ")\n"))
          (put! root "other.md" "# Present\n")
          (git! fixture "add" ".")
          (failure! (check! fixture) "source.md" (str/replace target #"/$" "")))))))

(defn- response [{:keys [uri]}]
  (case uri
    "/ok" {:status 200 :body "<h1>Present</h1>"}
    "/redirect" {:status 302 :headers {"Location" "/ok"} :body ""}
    "/auth" {:status 401 :body "Authentication required"}
    "/transient" {:status 503 :body "Unavailable"}
    {:status 404 :body "Missing"}))

(deftest http-test
  ;; Real loopback responses test redirects and failure policy without live sites.
  (let [server (http/run-server response {:ip "127.0.0.1" :port 0})
        base (str "http://127.0.0.1:" (:local-port (meta server)))]
    (try
      (doseq [[path success?] [["/ok" true] ["/redirect" true]
                               ["/ok#absent" true] ["/missing" false]
                               ["/auth" false] ["/transient" false]]]
        (testing path
          (with-project
            (fn [{:keys [root] :as fixture}]
              (put! root "source.md" (str "[Web](" base path ")\n"))
              (let [result (check! fixture)]
                (if success?
                  (is (zero? (:exit result)) (pr-str result))
                  (failure! result "source.md" (str base path))))))))
      (finally (server)))))

(deftest mixed-failure-test
  ;; A local fragment failure must not prevent online failure diagnostics.
  (let [server (http/run-server response {:ip "127.0.0.1" :port 0})
        url (str "http://127.0.0.1:" (:local-port (meta server)) "/missing")]
    (try
      (with-project
        (fn [{:keys [root] :as fixture}]
          (put! root "source.md" (str "# Source\n[Local](#absent)\n[Web](" url ")\n"))
          (failure! (check! fixture) "source.md" "#absent" url "404")))
      (finally (server)))))

(deftest network-policy-test
  ;; Unanswered requests and rate limits must fail within a bounded run.
  (let [channels (atom [])
        server (http/run-server
                (fn [{:keys [uri] :as request}]
                  (case uri
                    "/timeout" (http/as-channel request
                                                {:on-open #(swap! channels conj %)})
                    "/limited" {:status 429 :body "Rate limited"}))

                {:ip "127.0.0.1" :port 0})
        base (str "http://127.0.0.1:" (:local-port (meta server)))]
    (try
      (doseq [[path diagnostic] [["/timeout" "Timeout"]
                                 ["/limited" "429"]]]
        (testing path
          (with-project
            (fn [{:keys [root] :as fixture}]
              (put! root "source.md" (str "[Web](" base path ")\n"))
              (let [result (check! fixture)]
                (failure! result "source.md" (str base path) diagnostic))))))
      (finally
        (doseq [channel @channels] (http/close channel))
        (server)))))

(deftest invalid-input-test
  ;; Configuration errors and missing tracked inputs cannot become success.
  (with-project
    (fn [{:keys [root] :as fixture}]
      (put! root "lychee.toml" "invalid = [")
      (failure! (check! fixture))))
  (with-project
    (fn [{:keys [root] :as fixture}]
      (fs/delete (fs/path root "source.md"))
      (failure! (check! fixture) "source.md"))))

(deftest missing-tools-test
  ;; Restricted PATHs omit real tools; no replacement executables are used.
  (doseq [tool ["git" "lychee" "bb"]]
    (testing tool
      (with-project
        (fn [{:keys [root] :as fixture}]
          (let [bin (fs/path (fs/parent root) "bin")]
            (fs/create-dirs bin)
            (doseq [included (remove #{tool} ["git" "lychee" "bb"])]
              (fs/create-sym-link (fs/path bin included) (fs/which included)))
            (let [restricted (assoc-in fixture [:env "PATH"] (str bin))
                  before (inventory root)
                  result (if (= tool "bb")
                           (command restricted "/bin/sh" "-c" "bb lychee:check")
                           (command restricted bb-bin "lychee:check"))]
              (failure! result tool)
              (is (= before (inventory root)) "Missing tools must not write files"))))))))

(let [{:keys [fail error]} (run-tests 'link-check-test)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
