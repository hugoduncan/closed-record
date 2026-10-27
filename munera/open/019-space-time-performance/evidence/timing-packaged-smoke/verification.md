# Packaged timing smoke verification

First attempts on Temurin 21.0.10+7-LTS, Clojure 1.11.4, Darwin arm64.
Starting source revision is in `revision.txt`. No source or dependency changed.

The real `dev/bench/timing/prepare.sh` built an exclusive temporary overlay from
the pinned Criterium JAR. Its input and output checksum checks passed (exit 0).
Both fresh parent JVMs resolved `criterium/collect.clj` from that overlay, as
recorded in the summaries. The timing child inherits the parent classpath and
JVM flags. No global Vars, clocks, or process functions were replaced.

- Original six-case smoke: exit 0; 1 test, 26 assertions, no failures or errors.
- Fresh timing-child capture: exit 0; 1 test, 10 assertions, no failures or errors.
- Each test ran once, with one-second site budgets. No retry occurred.

The full Scry results retain assertion details and output. Some assertion values
print as `#object`; decode those files with `{:default tagged-literal}` when
using `clojure.edn/read-string`. The separate summary files are plain EDN. The
initial strict EDN inspection rejected that tag; no test was rerun. Summaries
were extracted from the retained results, not from terminal text.

## Reproduction

From the project root:

```sh
export JAVA_HOME=/Users/duncan/.local/share/mise/installs/java/temurin-21
export PATH="$JAVA_HOME/bin:$PATH"
O=$(mktemp -d /tmp/019-packaged-smoke.XXXXXX)/overlay
sh dev/bench/timing/prepare.sh "$HOME/.m2/repository/org/hugoduncan/criterium/0.5.251-ALPHA/criterium-0.5.251-ALPHA.jar" "$O"
for NS in closed-record.original-timing-test closed-record.assoc-timing-process-test; do
  clojure -Sdeps "{:aliases {:candidate {:extra-paths [\"$O\" \"test-bench\"] :main-opts []}}}" -M:test:bench:candidate -e "(require '$NS '[scry.core :as s]) (let [r (s/run {:namespaces ['$NS] :result-format {:namespace {:output? true}}})] (prn r) (System/exit (if (:pass? r) 0 1)))"
done
```

The retained run writes each result with `spit`, adds the collector resource URL
and `java.runtime.version`, and captures stdout, stderr, and exit separately.
Preparation checks the JAR hash and corrected source hash specified in prepare.sh.

This verifies the packaged path, not full-matrix acceptance, parity, or memory
support. The process test deletes temporary captures. Default aliases remain
unchanged. Next add explicit correction provenance before default adoption.
