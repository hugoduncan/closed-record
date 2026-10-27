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
  dev/bench/valhalla/FieldAllocationProbe.java
printf 'Premain-Class: FieldAllocationProbe\n\n' > target/valhalla/size-agent.mf
"$JAVA_HOME/bin/jar" --create --file target/valhalla/size-agent.jar \
  --manifest target/valhalla/size-agent.mf \
  -C target/valhalla/classes FieldAllocationProbe.class
"$JAVA_HOME/bin/java" --enable-preview "${flags[@]}" \
  -javaagent:target/valhalla/size-agent.jar \
  -cp target/valhalla/classes FieldAllocationProbe
