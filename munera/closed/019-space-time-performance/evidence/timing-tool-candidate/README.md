# Timing tool candidate screen

Repository lookup on 2026-10-27 returned 0.5.253-ALPHA as the newest Criterium
version. It is not a fix for the zero initial duration. The two collection source
entries are byte-identical to 0.5.251-ALPHA. The existing deterministic diagnostic
passes because it expects an ArithmeticException for a zero estimate.

No dependency was changed. No timing result was collected. The original six
cases were not rerun: this candidate failed the prerequisite correction check.
This is tool diagnosis on OpenJDK 25, not acceptance on the pinned ordinary JDK.

## Reproduction

```sh
clojure -T:deps find-versions :lib org.hugoduncan/criterium
clojure -Sdeps '{:aliases {:candidate {:extra-paths ["test-bench"] :override-deps {org.hugoduncan/criterium {:mvn/version "0.5.253-ALPHA"}} :main-opts []}}}' \
  -M:test:bench:candidate -m scry.cli \
  --namespace closed-record.timing-estimator-test
```

Exit: 0. Scry failure directory: empty. See stdout.txt, stderr.txt, and result.edn.
For each version, extract `criterium/collect.clj` and
`criterium/collect_plan.clj` with `unzip -p` from the Maven JAR. Compare bytes
with `cmp` and compute SHA-256 with `shasum -a 256`; result.edn records the hashes.

The initial estimate still has no positive-duration floor. Both the batch-size
and estimation-sample divisions require a positive estimate. The supported
`:one-shot` collection mode avoids this estimation path, but takes one sample
with batch size one; it is not a replacement for the required warmed, batched
timing statistics. Next inspect a corrected upstream source path or a supported
collection extension. Do not use retries or a larger seconds budget as a fix.
