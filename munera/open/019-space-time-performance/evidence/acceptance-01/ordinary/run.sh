#!/bin/sh
# Run from the repository root. Output directories must not exist.
set -eu
export JAVA_HOME="$HOME/.local/share/mise/installs/java/temurin-21.0.10+7.0.LTS"
export PATH="$JAVA_HOME/bin:$PATH"
e=munera/open/019-space-time-performance/evidence/acceptance-01/ordinary
test ! -e "$e/revision.txt"
revision=$(git rev-parse HEAD)
printf '%s\n' "$revision" > "$e/revision.txt"
"$JAVA_HOME/bin/java" -version > "$e/runtime.txt" 2>&1
uname -a > "$e/machine.txt"
sysctl -n machdep.cpu.brand_string >> "$e/machine.txt"
run() {
  name=$1; shift
  start=$(date +%s)
  set +e
  "$@" > "$e/$name.stdout" 2> "$e/$name.stderr"
  status=$?
  set -e
  end=$(date +%s)
  printf '{:exit %s :elapsed-seconds %s}\n' "$status" "$((end-start))" > "$e/$name.status.edn"
  test "$status" -eq 0
}
run timing sh dev/bench/timing/run.sh -m closed-record.bench.assoc-timing-cli "$e/timing" acceptance-01-ordinary-timing "$revision" 5.0
run allocation clojure -Srepro -Sdeps '{:paths ["src" "dev/bench"]}' -M -m closed-record.bench.assoc-allocation-cli "$e/allocation" acceptance-01-ordinary-allocation "$revision" 10000 20
run prepare sh dev/bench/memory/prepare-record-graph.sh target/acceptance-01-ordinary-graph
run graph clojure -Srepro -Sdeps '{:aliases {:graph-run {:extra-paths ["dev/bench" "target/acceptance-01-ordinary-graph/record-graph-agent.jar"] :jvm-opts ["-javaagent:target/acceptance-01-ordinary-graph/record-graph-agent.jar"]}}}' -M:graph-run -m closed-record.bench.assoc-graph-cli "$e/graph" acceptance-01-ordinary-graph "$revision" target/acceptance-01-ordinary-graph
run report clojure -Srepro -Sdeps '{:paths ["src" "dev/bench"]}' -M -m closed-record.bench.assoc-suite-report "$e/timing" "$e/allocation" "$e/graph"
