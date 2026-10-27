# Isolated timing source candidate

This is a local candidate, not a released tool or an acceptance run. The project
still uses Criterium 0.5.251-ALPHA. No global Vars or methods were replaced.

`collector.patch` applies to `criterium/collect.clj` extracted from that pinned
JAR (JAR SHA-256 recorded in ../timing-tool-candidate/result.edn). It floors the
initial planning estimate at one ns, before either division in collect-plan.
The subsequent minimum estimate already had that floor; both now use one pure
helper. Raw samples remain unchanged. This floor is a scheduling approximation,
not a claim that an operation takes at least one ns. The warmed final estimate
already has its own one-ns floor in collect-plan.

The deterministic test exercises zero initial and subsequent estimates,
sub-nanosecond truncation, and positive values. It checks both planning divisors.
It does not force a real clock to return zero or establish whole-plan correctness.
Next run original six-case smoke measurements and the fresh child process test
against this isolated candidate before deciding on adoption. Keep all failures.

## Reproduce

From the project root, with OpenJDK 25.0.2+10-69 selected:

```sh
E=munera/open/019-space-time-performance/evidence/timing-source-correction
mkdir -p /tmp/019-timing-correction/criterium
unzip -p "$HOME/.m2/repository/org/hugoduncan/criterium/0.5.251-ALPHA/criterium-0.5.251-ALPHA.jar" criterium/collect.clj > /tmp/019-timing-correction/criterium/collect.clj
patch -d /tmp/019-timing-correction -p1 < "$E/collector.patch"
clojure -Sdeps '{:aliases {:candidate {:extra-paths ["/tmp/019-timing-correction" "munera/open/019-space-time-performance/evidence/timing-source-correction"] :main-opts []}}}' -M:test:bench:candidate -m scry.cli --namespace planning-estimate-test
```

Result: exit 0, 1 test, 19 assertions, no Scry failure artifacts. Clojure 1.11.4.
Full stdout and stderr are retained. The CLI warns about an external source path;
this temporary overlay is intentional and is not a project dependency change.
Patched source SHA-256:
`5909a63300b38e72693261ffdd5accb2c76cfe809ac30a1d97ac38f068b74d11`.

## Real smoke verification

On Temurin 21.0.10+7-LTS, macOS AArch64, Clojure 1.11.4, starting revision
`576d0d5727e83e34103832919d88a53cbd10af5b`, the same patched source hash above
passed both fresh-JVM checks on the first attempt. The only added test is
`test-bench/closed_record/original_timing_test.clj`. Dependencies are unchanged.

- Original six cases: one second per site, both representations, 1 test and
  26 assertions pass. All statistics are finite and positive, and intervals
  contain their means. No parity assertion applies to this smoke run.
- Independent timing-child capture: 1 test and 10 assertions pass, including
  inherited flags, actual runtime observations, four measured rows, and invalid
  budget rejection. This test deletes its temporary capture directories.
- Structured Scry results, stdout, stderr, and exits are retained with the
  `original-` and `process-` prefixes. The original result includes measured rows.
  These artifacts are smoke evidence, not durable acceptance captures.

Reproduce after creating the overlay as above:

```sh
export JAVA_HOME=/Users/duncan/.local/share/mise/installs/java/temurin-21
export PATH="$JAVA_HOME/bin:$PATH"
# Run each namespace in a separate fresh JVM.
for NS in closed-record.original-timing-test closed-record.assoc-timing-process-test; do
  clojure -Sdeps '{:aliases {:candidate {:extra-paths ["/tmp/019-timing-correction" "test-bench"] :main-opts []}}}' -M:test:bench:candidate -e "(require '$NS '[scry.core :as s]) (let [r (s/run {:namespaces ['$NS] :result-format {:namespace {:output? true}}})] (prn r) (System/exit (if (:pass? r) 0 1)))"
done
```

Both processes used the benchmark alias JVM flags. Focused formatting and lint
pass. Next select a reproducible benchmark-only packaging path for this local
correction and verify reporting regressions before adoption. Passing smoke runs
do not establish full-plan correctness, acceptance, or memory-tool support.
