#!/bin/sh
# Default timing entry point. Keep each verified overlay for child JVMs.
# BENCH_ALIASES: extra aliases after :bench (for example :value-type-test).
# BENCH_JVM_OPTS: -J options for the run, separated by spaces.
set -eu
aliases=${BENCH_ALIASES:-}
jvm_opts=${BENCH_JVM_OPTS:-}
root=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
cd "$root"
classpath=$(clojure -Srepro -Spath -M:bench$aliases)
jar=$(printf '%s\n' "$classpath" | tr ':' '\n' | grep '/criterium-0.5.251-ALPHA.jar$')
if [ "$(printf '%s\n' "$jar" | wc -l | tr -d ' ')" != 1 ]; then
  echo 'Expected exactly one pinned Criterium JAR' >&2
  exit 2
fi
mkdir -p target
build=$(mktemp -d target/timing-run.XXXXXXXX)
sh dev/bench/timing/prepare.sh "$jar" "$build/overlay" >&2
if [ "$#" -eq 0 ]; then
  set -- -m closed-record.bench
fi
# The generated relative path contains no EDN quoting characters. The later
# alias puts corrected source before the dependency JAR and clears main opts.
# $jvm_opts is unquoted on purpose: one word per option.
# shellcheck disable=SC2086
exec clojure $jvm_opts -Srepro -Sdeps "{:aliases {:corrected-timing {:extra-paths [\"$build/overlay\"] :main-opts []}}}" \
  -M:bench$aliases:corrected-timing "$@"
