# Experimental allocation boundary

Pinned Valhalla run exits zero. All 18 sites execute 20 warmup batches and five
saved batches of 1,024 operations in one JVM. These are boundary smoke results,
not five independent forks or acceptance evidence. No memory agent is loaded.

`Allocation.sample` uses the existing unchecked Operations methods and the real
thread allocation counter. Payloads, sources, result arrays, and primitive read
sinks are prepared before the counter. Each store has a distinct source array,
initially 17, and changes index 0 to 23 once. Construction retains every result;
reads retain primitive results without boxing. Post-counter checks verify all
array elements, result ownership, holder values, and unchanged holder sources.
No reset, validation, or setup allocation is subtracted from measured bytes.

Saved samples show 32 bytes per small array construction and 4,112 per large
array construction for both representations. Holder construction/replacement
uses 16 bytes per operation. Reads and stores report zero allocated bytes.
These values describe allocation, not retained graph size or speed. The loop
includes dispatch and result storage. Distinct sources increase the working set;
this boundary does not resolve the store timing issue.

Next connect this boundary to inventory rows and existing child persistence,
retain alternating five-fork results with allocation controls, and combine
supported graph outcomes. Invalid-input coverage and independent allocation
control comparison remain needed before approval. Timing quantisation and
flat-read variation remain open. The full experimental milestone is incomplete.

Reproduce from the repository root with `bash` and this directory's `run.sh`.
Source hashes and runtime are in `environment.txt`; the revision identifies the
base before this boundary was added. Raw totals and divisors are in `results.edn`.
