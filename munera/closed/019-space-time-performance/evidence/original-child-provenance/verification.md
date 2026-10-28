# Original child command provenance

First attempt on Temurin 21.0.10+7-LTS, Darwin arm64, Clojure 1.11.4.
Source revision is in revision.txt. No production source or dependency changed.

Preparation and the actual original child command both exit 0. The child uses
fork 0 and its existing five-second per-site budget. All six original operations
appear in child.edn, with the local correction source hash, patch hash, and
resolved resource URL. Streams are retained unchanged, including warnings.

Independent read-only validation uses legacy-transport/decode with a fresh
parent timing-tool/observe declaration. It also checks operation order, finite
positive statistics, and low <= mean <= high for both representations. Its
summary is validation.edn; exit 0. No retry or result filtering occurred.

## Reproduction

From the repository root:

```sh
export JAVA_HOME=/Users/duncan/.local/share/mise/installs/java/temurin-21
export PATH="$JAVA_HOME/bin:$PATH"
O=$(mktemp -d /tmp/019-original-child.XXXXXX)/overlay
sh dev/bench/timing/prepare.sh "$HOME/.m2/repository/org/hugoduncan/criterium/0.5.251-ALPHA/criterium-0.5.251-ALPHA.jar" "$O"
clojure -Sdeps "{:aliases {:candidate {:extra-paths [\"$O\"]}}}" \
  -M:bench:candidate 0 /tmp/original-child-result.edn
clojure -M:test -m scry.cli \
  --namespace closed-record.bench-legacy-transport-test \
  --namespace closed-record.bench-fork-test \
  --namespace closed-record.bench-report-test
```

Regression verification: 15 tests, 88 assertions pass, exit 0, no Scry failure
files. The command inherits the benchmark alias blackhole JVM flags. Default
aliases are unchanged. The temporary overlay path is retained as provenance,
not as a portable installation path; preparation recreates identical bytes.

This is a single-fork command/transport smoke check, not the five-fork S-03
acceptance run or evidence for full-matrix performance claims. No parity decision
was made. Default adoption, acceptance budgets, and memory gates remain open.
