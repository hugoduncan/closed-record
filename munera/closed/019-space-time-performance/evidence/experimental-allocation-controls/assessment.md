# Allocation boundary controls

Pinned Valhalla checks pass, exit 0. Nine invalid sites reject before fixture
allocation, including null operation, non-positive count, invalid array length,
nonzero holder length, and operations for the wrong container.

For both array layouts, five real-counter comparisons after 20 warmup pairs
match the physical stride obtained from Unsafe: the 1,020-element length delta
allocates 4,080 additional bytes per operation. Raw totals retain all overhead;
no subtraction changes the reported samples. Small and large allocations remain
32 and 4,112 bytes per operation. Post-counter content/ownership checks still run.
This tests the allocation boundary against independent layout information, not
against a speed threshold. It does not establish retained graph sizes or holder
allocation controls. No mocks are used.

Run `bash munera/open/019-space-time-performance/evidence/experimental-allocation-controls/run.sh`
from the repository root after the existing fixture preparation. Environment and
source hashes, raw results, compiler output, and exit status are retained here.
This is one-JVM verification, not independent-fork acceptance evidence.
