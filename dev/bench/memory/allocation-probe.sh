#!/usr/bin/env bash
# Run from the repository root with an explicit benchmark JDK.
set -euo pipefail
: "${JAVA_HOME:?Select the benchmark JDK}"
mkdir -p target/bench-memory/classes
"$JAVA_HOME/bin/javac" -d target/bench-memory/classes dev/bench/memory/AllocationProbe.java
"$JAVA_HOME/bin/java" -cp target/bench-memory/classes AllocationProbe
