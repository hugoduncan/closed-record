# Local timing correction packaging

The candidate correction is a checksum-verified source overlay for Criterium
0.5.251-ALPHA. It is not an upstream release. Base dependencies and the default
`:bench` alias remain unchanged. Explicit candidate commands must put the overlay
on the classpath before the dependency JAR. Child commands inherit that classpath.

From the repository root, resolve the pinned benchmark dependency, then prepare
an absent output directory (the parent directory must exist):

```sh
clojure -P -M:bench
mkdir -p target
sh dev/bench/timing/prepare.sh \
  "$HOME/.m2/repository/org/hugoduncan/criterium/0.5.251-ALPHA/criterium-0.5.251-ALPHA.jar" \
  target/019-timing-overlay
clojure -Sdeps '{:aliases {:candidate {:extra-paths ["target/019-timing-overlay" "munera/closed/019-space-time-performance/evidence/timing-source-correction"] :main-opts []}}}' -M:test:bench:candidate -m scry.cli --namespace planning-estimate-test
```

The script requires `shasum`, `unzip`, and `patch`. It verifies the full input
JAR and corrected source hashes, refuses an existing output directory, and never
writes to the dependency cache. A failed build can leave partial output; do not
use it. Select a new output directory for the next attempt. Do not retry a failed
measurement to replace evidence.

The patch floors planning estimates only; it does not change raw samples.
The task's `evidence/timing-source-correction` retains the original patch,
boundary tests, and original-six-case and fresh-child smoke results. The packaged
patch has identical bytes. Reporting regressions pass without Criterium on the
ordinary test classpath.

This packaging is opt-in pending default harness adoption. Before adoption,
verify real smoke runs through this packaged path and record the effective tool
identity in run provenance. Memory-tool approval and acceptance runs remain
separate gates.
