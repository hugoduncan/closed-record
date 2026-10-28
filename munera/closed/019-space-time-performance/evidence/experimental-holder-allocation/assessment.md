# Holder allocation boundary controls

Pinned Valhalla checks pass on the first run, exit 0. Both holder layouts allocate
16 bytes per construction and replacement with prebuilt payloads. Twenty recorded
batches of 1,024 operations match independent Instrumentation shallow sizes after
20 warmup batches per site. Allocation.sample also checks fresh results, expected
payloads, and unchanged sources after the counter boundary. No overhead is
subtracted. These are allocation controls, not retained graph totals or acceptance
results. In particular, reference payload box ownership remains a separate limit.

Run `bash munera/open/019-space-time-performance/evidence/experimental-holder-allocation/run.sh`
from the repository root. The script builds a test-only size agent and runs the
real allocation boundary. The agent is not used for timing. Environment, source
hashes, raw counts, compiler output, and exit status are retained here. No mocks
or speed thresholds are used. Five-fork inventory/persistence integration remains
unfinished; this single-JVM check does not complete the experimental milestone.
