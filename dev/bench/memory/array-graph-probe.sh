#!/usr/bin/env bash
# Run from the repository root with an explicit benchmark JDK.
set -euo pipefail
: "${JAVA_HOME:?Select the benchmark JDK}"
mkdir -p target/bench-memory/classes
"$JAVA_HOME/bin/javac" -d target/bench-memory/classes dev/bench/memory/ArrayGraphProbe.java
printf 'Premain-Class: ArrayGraphProbe\n\n' > target/bench-memory/array-graph-agent.mf
"$JAVA_HOME/bin/jar" --create --file target/bench-memory/array-graph-agent.jar \
  --manifest target/bench-memory/array-graph-agent.mf \
  -C target/bench-memory/classes ArrayGraphProbe.class
"$JAVA_HOME/bin/java" -javaagent:target/bench-memory/array-graph-agent.jar \
  -cp target/bench-memory/classes ArrayGraphProbe
