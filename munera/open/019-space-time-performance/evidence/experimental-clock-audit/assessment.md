# Experimental clock audit

Pinned Valhalla 27-jep401ea3+1-1, existing compiled fixtures, corrected timing
overlay, and no memory agent. `run.sh` reproduces the command without replacing
saved streams. `audit.clj` is a diagnostic, not an acceptance collector. The
process exited zero. Each distribution contains 100,000 observations. Each store
site first received 100,000 warmup invocations; the existing reset boundary was
used unchanged. Raw output includes the JVM blackhole directive before EDN.

Back-to-back nanoTime calls returned zero 69,131 times; the smallest positive
observations were 41 and 42 ns. Reset-aware single stores returned zero 70,423,
63,006, 54,024, and 69,090 times for flat-small, reference-small, flat-large, and
reference-large respectively. Nonzero samples predominantly have the same
41/42 ns granularity. These observations establish substantial quantisation in
this measurement boundary. They do not establish the cause of flat-read fork
variation or prove that sums of quantised samples are biased by a known amount.

Do not subtract timer costs, replace zero samples, or label stores unsupported.
Attempt 01 remains diagnostic and preserved. Before acceptance, use a boundary
that times enough changed stores together to exceed clock granularity while
keeping reset work outside timing (for example, distinct pre-reset locations).
Verify its ownership/cache semantics against the fixed case contract before
adoption. Alternatively establish error bounds for the existing boundary; this
probe does not supply them. Separate memory collection remains unfinished.

Real generated-expression/reset tests on the same runtime pass with no mocks:
Scry `valhalla.store-boundary-test`, 2 tests, 104 assertions, exit 0, empty
`.scry-results`. No timing threshold is asserted. No production code, dependency,
or acceptance result changed.
