#!/bin/sh
# Build an explicit source overlay; never modify the dependency cache.
set -eu
if [ "$#" -ne 2 ]; then
  echo 'usage: sh dev/bench/timing/prepare.sh CRITERIUM_JAR NEW_OUTPUT_DIRECTORY' >&2
  exit 2
fi
jar=$1
output=$2
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
expected=b2640cac514a15f1af9582345ed0c15bdee45bbeda93a2a7e4651aa96ed7e033
actual=$(shasum -a 256 "$jar" | cut -d ' ' -f 1)
if [ "$actual" != "$expected" ]; then
  echo 'Criterium JAR checksum mismatch' >&2
  exit 2
fi
# Exclusive creation prevents accidental reuse of an earlier build.
mkdir -- "$output"
mkdir -- "$output/criterium"
unzip -p "$jar" criterium/collect.clj > "$output/criterium/collect.clj"
patch -d "$output" -p1 < "$script_dir/collector.patch"
actual=$(shasum -a 256 "$output/criterium/collect.clj" | cut -d ' ' -f 1)
if [ "$actual" != 5909a63300b38e72693261ffdd5accb2c76cfe809ac30a1d97ac38f068b74d11 ]; then
  echo 'Corrected source checksum mismatch; do not use this output' >&2
  exit 2
fi
printf '%s\n' 'Verified local Criterium source overlay (not an upstream release).'
