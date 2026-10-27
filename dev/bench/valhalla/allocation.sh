#!/usr/bin/env bash
# Run from the repository root. JAVA_HOME must select the pinned Valhalla JDK.
set -euo pipefail
: "${JAVA_HOME:?Select Valhalla 27-jep401ea3+1-1}"
flags=(--add-exports java.base/jdk.internal.value=ALL-UNNAMED
       --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
       --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED)
mkdir -p target/valhalla/classes
"$JAVA_HOME/bin/javac" --enable-preview -source 27 "${flags[@]}" \
  -d target/valhalla/classes dev/bench/valhalla/CapabilityProbe.java \
  dev/bench/valhalla/FlatAllocationProbe.java
"$JAVA_HOME/bin/java" --enable-preview "${flags[@]}" \
  -cp target/valhalla/classes FlatAllocationProbe
