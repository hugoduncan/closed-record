#!/usr/bin/env bash
# Run from the repository root with an explicit benchmark JDK.
set -euo pipefail
: "${JAVA_HOME:?Select the benchmark JDK}"
mkdir -p target/bench-memory/classes
"$JAVA_HOME/bin/javac" -d target/bench-memory/classes dev/bench/memory/RecordGraphProbe.java
printf 'Premain-Class: RecordGraphProbe\n\n' > target/bench-memory/record-graph-agent.mf
"$JAVA_HOME/bin/jar" --create --file target/bench-memory/record-graph-agent.jar \
  --manifest target/bench-memory/record-graph-agent.mf \
  -C target/bench-memory/classes RecordGraphProbe.class
classpath=$(clojure -Srepro -Spath)
"$JAVA_HOME/bin/java" -javaagent:target/bench-memory/record-graph-agent.jar \
  -cp "target/bench-memory/classes:dev/bench:$classpath" \
  clojure.main dev/bench/memory/record-graph.clj
