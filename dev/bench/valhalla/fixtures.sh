#!/usr/bin/env bash
# Run from the repository root with the pinned experimental JAVA_HOME.
set -euo pipefail
: "${JAVA_HOME:?Select Valhalla 27-jep401ea3+1-1}"
bash dev/bench/valhalla/probe.sh >&2
"$JAVA_HOME/bin/javac" --enable-preview -source 27   --add-exports java.base/jdk.internal.value=ALL-UNNAMED   -cp target/valhalla/classes -d target/valhalla/classes   dev/bench/valhalla/FixtureProbe.java
classpath=$(clojure -Srepro -Spath)
"$JAVA_HOME/bin/java" --enable-preview \
  --add-exports java.base/jdk.internal.value=ALL-UNNAMED \
  --add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED \
  -cp "target/valhalla/classes:$classpath" \
  clojure.main dev/bench/valhalla/fixtures.clj
