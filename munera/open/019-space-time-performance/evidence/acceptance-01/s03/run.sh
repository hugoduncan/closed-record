#!/bin/sh
# Run from the repository root. Output directories must not exist.
set -eu
export JAVA_HOME="$HOME/.local/share/mise/installs/java/temurin-21.0.10+7.0.LTS"
export PATH="$JAVA_HOME/bin:$PATH"
e=munera/open/019-space-time-performance/evidence/acceptance-01/s03
test ! -e "$e/revision.txt"
revision=$(git rev-parse HEAD)
printf '%s\n' "$revision" > "$e/revision.txt"
"$JAVA_HOME/bin/java" -version > "$e/runtime.txt" 2>&1
uname -a > "$e/machine.txt"
sysctl -n machdep.cpu.brand_string >> "$e/machine.txt"
run() {
  name=$1; shift
  start=$(date +%s)
  set +e
  "$@" > "$e/$name.stdout" 2> "$e/$name.stderr"
  status=$?
  set -e
  end=$(date +%s)
  printf '{:exit %s :elapsed-seconds %s}\n' "$status" "$((end-start))" > "$e/$name.status.edn"
  test "$status" -eq 0
}
for fork in 0 1 2 3 4; do
  run "fork-$fork" sh dev/bench/timing/run.sh -m closed-record.bench "$fork" "$e/fork-$fork.edn"
done
run field-test clojure -Srepro -M:test -m scry.cli --var closed-record.field-value-test/field-declaration-test
mkdir "$e/scry-failures"
cp .scry-results/*.edn "$e/scry-failures/" 2>/dev/null || test -z "$(ls -A .scry-results)"
