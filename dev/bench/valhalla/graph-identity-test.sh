#!/usr/bin/env bash
# Run from repository root with the pinned experimental JDK.
set -euo pipefail
: "${JAVA_HOME:?Select Valhalla 27-jep401ea3+1-1}"
bash dev/bench/valhalla/flat-graph.sh
flags=(--enable-preview --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
       --add-exports java.base/jdk.internal.value=ALL-UNNAMED
       --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED)
classes=target/valhalla/classes
agent=target/valhalla/flat-graph-agent.jar
"$JAVA_HOME/bin/javac" "${flags[@]}" -source 27 -cp "$classes" -d "$classes" \
  dev/bench/valhalla/GraphIdentityTest.java
"$JAVA_HOME/bin/java" "${flags[@]}" -javaagent:"$agent" -cp "$agent:$classes" GraphIdentityTest active "$agent"
"$JAVA_HOME/bin/java" "${flags[@]}" -cp "$agent:$classes" GraphIdentityTest inactive
"$JAVA_HOME/bin/java" "${flags[@]}" -javaagent:"$agent" -cp "$classes" GraphIdentityTest loose
