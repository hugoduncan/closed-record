#!/bin/sh
# Build an isolated agent and retain the exact inputs and binary checksums.
set -eu
if [ "$#" -ne 1 ]; then
  echo 'usage: sh dev/bench/memory/prepare-record-graph.sh NEW_OUTPUT_DIRECTORY' >&2
  exit 2
fi
: "${JAVA_HOME:?Select the benchmark JDK}"
output=$1
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
expected=bff30ae8353f5a28811c55f1bf68ce4b37e42c2191f7cc68778cde28958a7d33
actual=$(shasum -a 256 "$script_dir/RecordGraphProbe.java" | cut -d ' ' -f 1)
if [ "$actual" != "$expected" ]; then
  echo 'Record graph source checksum mismatch' >&2
  exit 2
fi
# Never replace prior evidence, including an incomplete build.
mkdir -- "$output"
cp "$script_dir/RecordGraphProbe.java" "$output/RecordGraphProbe.java"
mkdir -- "$output/classes"
"$JAVA_HOME/bin/javac" -version > "$output/compiler.txt" 2>&1
"$JAVA_HOME/bin/javac" --release 21 -d "$output/classes" "$output/RecordGraphProbe.java"
printf 'Premain-Class: RecordGraphProbe\n\n' > "$output/agent.mf"
# Fixed ZIP timestamps make repeated builds with the same compiler comparable.
"$JAVA_HOME/bin/jar" --create --date=2026-01-01T00:00:00Z \
  --file "$output/record-graph-agent.jar" --manifest "$output/agent.mf" \
  -C "$output/classes" RecordGraphProbe.class
(cd "$output" && shasum -a 256 RecordGraphProbe.java compiler.txt \
  classes/RecordGraphProbe.class record-graph-agent.jar > SHA256SUMS)
printf '%s\n' 'Built record graph agent; checksums identify this build, not runtime selection.'
