#!/usr/bin/env bash
# Run once from the repository root. Preserve failed attempts; never overwrite.
set -euo pipefail
out=munera/open/019-space-time-performance/evidence/acceptance-01/experimental-timing
export JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
test ! -e "$out/revision.txt"
git rev-parse HEAD > "$out/revision.txt"
uname -a > "$out/machine.txt"
"$JAVA_HOME/bin/java" -version > "$out/java.txt" 2>&1
bash dev/bench/valhalla/fixtures.sh > "$out/fixtures.stdout" 2> "$out/fixtures.stderr"
"$JAVA_HOME/bin/javac" --enable-preview -source 27 --add-exports java.base/jdk.internal.value=ALL-UNNAMED -cp target/valhalla/classes -d target/valhalla/classes dev/bench/valhalla/Operations.java dev/bench/valhalla/StoreBatch.java
for fork in 1 2 3 4 5; do
  start=$SECONDS
  set +e
  clojure -Srepro -Sdeps '{:aliases {:probe {:extra-paths ["target/valhalla/classes" "target/019-acceptance-overlay"] :jvm-opts ["--enable-preview" "--add-exports=java.base/jdk.internal.value=ALL-UNNAMED" "--add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED"] :main-opts []}}}' -M:bench:probe -m valhalla.timing-child "$out/fork-$fork.edn" acceptance-01-experimental-timing "$(cat "$out/revision.txt")" "$fork" 1 > "$out/fork-$fork.stdout" 2> "$out/fork-$fork.stderr"
  status=$?
  set -e
  printf '%s\n' "$status" > "$out/fork-$fork.exit"
  printf '%s\n' "$((SECONDS-start))" > "$out/fork-$fork.seconds"
  test "$status" -eq 0 || exit "$status"
done
