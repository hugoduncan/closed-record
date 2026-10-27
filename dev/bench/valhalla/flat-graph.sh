#!/usr/bin/env bash
# Run from repository root with the pinned experimental JDK.
set -euo pipefail
: "${JAVA_HOME:?Select Valhalla 27-jep401ea3+1-1}"
flags=(--add-exports java.base/jdk.internal.value=ALL-UNNAMED
       --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
       --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED)
mkdir -p target/valhalla/classes
"$JAVA_HOME/bin/javac" --enable-preview -source 27 "${flags[@]}" \
  -d target/valhalla/classes dev/bench/valhalla/CapabilityProbe.java \
  dev/bench/valhalla/FlatGraphProbe.java
printf 'Premain-Class: FlatGraphProbe\n\n' > target/valhalla/flat-graph-agent.mf
"$JAVA_HOME/bin/jar" --create --file target/valhalla/flat-graph-agent.jar \
  --manifest target/valhalla/flat-graph-agent.mf \
  -C target/valhalla/classes FlatGraphProbe.class
"$JAVA_HOME/bin/java" --enable-preview "${flags[@]}" \
  -javaagent:target/valhalla/flat-graph-agent.jar \
  -cp target/valhalla/classes FlatGraphProbe
