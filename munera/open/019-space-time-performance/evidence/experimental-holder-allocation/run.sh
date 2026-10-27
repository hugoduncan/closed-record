#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home"
flags=(--enable-preview --add-exports java.base/jdk.internal.misc=ALL-UNNAMED --add-exports java.base/jdk.internal.value=ALL-UNNAMED --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED)
"$JAVA_HOME/bin/javac" "${flags[@]}" -source 27 -cp target/valhalla/classes -d target/valhalla/classes dev/bench/valhalla/CapabilityProbe.java dev/bench/valhalla/Operations.java dev/bench/valhalla/Allocation.java dev/bench/valhalla/HolderAllocationTest.java
printf 'Premain-Class: HolderAllocationTest\n\n' > target/valhalla/holder-allocation-agent.mf
"$JAVA_HOME/bin/jar" --create --file target/valhalla/holder-allocation-agent.jar --manifest target/valhalla/holder-allocation-agent.mf -C target/valhalla/classes HolderAllocationTest.class
"$JAVA_HOME/bin/java" "${flags[@]}" -javaagent:target/valhalla/holder-allocation-agent.jar -cp target/valhalla/classes HolderAllocationTest
