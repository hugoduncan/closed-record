#!/usr/bin/env bash
# Run once from the repository root. Never overwrite an attempt.
set -euo pipefail
out=munera/open/019-space-time-performance/evidence/acceptance-01/experimental-allocation
export JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
test ! -e "$out/revision.txt"
git rev-parse HEAD > "$out/revision.txt"
uname -a > "$out/machine.txt"
"$JAVA_HOME/bin/java" -version > "$out/java.txt" 2>&1
flags=(--enable-preview --add-exports java.base/jdk.internal.misc=ALL-UNNAMED --add-exports java.base/jdk.internal.value=ALL-UNNAMED --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED)
bash dev/bench/valhalla/fixtures.sh > "$out/fixtures.stdout" 2> "$out/fixtures.stderr"
"$JAVA_HOME/bin/javac" "${flags[@]}" -source 27 -cp target/valhalla/classes -d target/valhalla/classes dev/bench/valhalla/Operations.java dev/bench/valhalla/Allocation.java
classpath=$(clojure -Srepro -Spath)
for fork in 1 2 3 4 5; do
  start=$SECONDS
  set +e
  "$JAVA_HOME/bin/java" "${flags[@]}" -cp "target/valhalla/classes:dev/bench:$classpath" clojure.main -m valhalla.allocation-child "$out/fork-$fork.edn" acceptance-01-experimental-allocation "$(cat "$out/revision.txt")" "$fork" > "$out/fork-$fork.stdout" 2> "$out/fork-$fork.stderr"
  status=$?
  set -e
  printf '%s\n' "$status" > "$out/fork-$fork.exit"
  printf '%s\n' "$((SECONDS-start))" > "$out/fork-$fork.seconds"
  test "$status" -eq 0 || exit "$status"
done
"$JAVA_HOME/bin/java" -cp "dev/bench:$classpath" clojure.main "$out/report.clj" > "$out/report.md"
