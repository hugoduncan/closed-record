#!/usr/bin/env bash
set -euo pipefail
out=munera/open/019-space-time-performance/evidence/experimental-clock-audit
export JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
# Use a new output directory for later attempts; preserve these raw streams.
clojure -Srepro -Sdeps '{:aliases {:probe {:extra-paths ["target/valhalla/classes" "target/019-timing-overlay"] :jvm-opts ["--enable-preview" "--add-exports=java.base/jdk.internal.value=ALL-UNNAMED" "--add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED"] :main-opts []}}}' -M:bench:probe "$out/audit.clj"
