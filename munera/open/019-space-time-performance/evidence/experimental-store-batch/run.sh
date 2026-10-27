#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home"
flags=(--enable-preview --add-exports java.base/jdk.internal.misc=ALL-UNNAMED --add-exports java.base/jdk.internal.value=ALL-UNNAMED --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED)
"$JAVA_HOME/bin/javac" "${flags[@]}" -source 27 -cp target/valhalla/classes -d target/valhalla/classes dev/bench/valhalla/CapabilityProbe.java dev/bench/valhalla/Operations.java dev/bench/valhalla/StoreBatchProbe.java
"$JAVA_HOME/bin/java" "${flags[@]}" -cp target/valhalla/classes StoreBatchProbe
