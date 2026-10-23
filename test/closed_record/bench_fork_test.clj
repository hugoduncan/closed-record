(ns closed-record.bench-fork-test
  "V-11: the fork layer of the benchmarks (`closed-record.bench.fork`).
  `default-child-cmd` is tested against the state of the test JVM.
  The tests give `parent` short shell commands (`sh`, `rm`, `true`, `cp`
  of a fixture) in place of child JVMs, and give `in-order` thunks that record
  their calls. They run no benchmark and need no criterium. Run with the
  suite: `clojure -M:test -m scry.cli`."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [closed-record.bench.fork :as fork]
   [closed-record.bench.report :as report])
  (:import
   (java.io File StringWriter)
   (java.lang.management ManagementFactory)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- run-parent
  "Run `parent` with `child-cmd`. Return `{:exit :out :err}`."
  [child-cmd]
  (let [err  (StringWriter.)
        exit (atom nil)
        out  (with-out-str
               (binding [*err* err]
                 (reset! exit (fork/parent child-cmd))))]
    {:exit @exit :out out :err (str err)}))

(defn- temp-dir []
  (str (Files/createTempDirectory "closed-record-fork-test" (make-array FileAttribute 0))))

(defn- fixture
  "Write a valid child result (P mean `pm`, R mean `rm` for each operation)
  to a temp file. Return its path."
  [pm rm]
  (let [f (File/createTempFile "closed-record-fixture-" ".edn")
        s (fn [m] {:mean m :low m :high m})]
    (spit f (report/encode-result (mapv (fn [op] [op (s pm) (s rm)]) report/ops)))
    (str f)))

(defn- fork-failed? [{:keys [exit out err]} fork cause]
  (and (= 2 exit)
       (str/blank? out)
       (str/starts-with? err (str "fork " fork " failed: " cause))))

(deftest parent-fork-failure-test
  (testing "exit value not 0"
    (is (fork-failed? (run-parent (fn [_ _] ["sh" "-c" "exit 3"])) 0 "exit 3")))
  (testing "missing result file"
    (is (fork-failed? (run-parent (fn [_ f] ["rm" f])) 0 "missing result file")))
  (testing "decode error: empty file"
    (is (fork-failed? (run-parent (fn [_ _] ["true"])) 0 "not a vector of 6 entries")))
  (testing "decode error: invalid EDN"
    (is (fork-failed? (run-parent (fn [_ f] ["sh" "-c" (str "printf '[1' > " f)]))
                      0 "invalid EDN"))))

(deftest parent-stops-after-failure-test
  (testing "a failure at fork 1 starts no more forks"
    (let [dir (temp-dir)
          fix (fixture 1.0 1.0)
          cmd (fn [n f]
                (let [mark (str "touch " dir "/" n)]
                  ["sh" "-c" (if (= 1 n)
                               (str mark "; exit 3")
                               (str mark "; cp " fix " " f))]))]
      (is (fork-failed? (run-parent cmd) 1 "exit 3"))
      (is (= #{"0" "1"} (set (.list (io/file dir))))))))

(deftest parent-good-forks-test
  (doseq [[pm exit] [[1.0 0] [2.0 1]]]
    (testing (str "5 good forks: forks 0 to 4 in start order, table with Forks 5, exit " exit)
      (let [fix  (fixture pm 1.0)
            log  (str (temp-dir) "/forks")
            res  (run-parent (fn [n f]
                               ["sh" "-c" (str "echo " n " >> " log "; cp " fix " " f)]))
            rows (->> (str/split-lines (:out res))
                      (filter #(str/starts-with? % "| ("))
                      (map #(str/trim (last (str/split % #"\|")))))]
        (is (= exit (:exit res)))
        (is (= ["0" "1" "2" "3" "4"] (str/split-lines (slurp log))))
        (is (str/blank? (:err res)))
        (is (str/includes? (:out res) "Forks"))
        (is (= (repeat (count report/ops) "5") rows))))))

(deftest in-order-test
  (doseq [n (range report/fork-count)]
    (testing (str "fork " n)
      (let [calls (atom [])
            out   (fork/in-order n
                                 #(do (swap! calls conj :p) :p-result)
                                 #(do (swap! calls conj :r) :r-result))]
        (is (= (if (even? n) [:p :r] [:r :p]) @calls))
        (is (= [:p-result :r-result] out))))))

(deftest default-child-cmd-test
  (testing "the child has the java binary, JVM options and classpath of this JVM"
    (let [args (vec (.getInputArguments (ManagementFactory/getRuntimeMXBean)))
          n    (count args)
          cmd  (fork/default-child-cmd 3 "/tmp/r.edn")]
      (is (= (str (io/file (System/getProperty "java.home") "bin" "java")) (first cmd)))
      (is (= args (subvec cmd 1 (inc n))))
      (is (= ["-cp" (System/getProperty "java.class.path")] (subvec cmd (inc n) (+ n 3))))
      (is (= ["clojure.main" "-m" "closed-record.bench" "3" "/tmp/r.edn"] (subvec cmd (+ n 3))))
      (is (= (+ n 8) (count cmd))))))
