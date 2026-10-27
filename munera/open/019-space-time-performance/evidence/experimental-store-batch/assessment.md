# Changed-store batch probe

This diagnostic uses the pinned Valhalla runtime, without a memory agent or
Criterium. It is not a replacement timing harness or acceptance evidence.
`run.sh` compiles and runs real Java operations. Exit 0 means all result checks
passed. The first compile failed because the capability class also needs the
internal misc export; its error and exit are preserved. The corrected command
was run once.

Each timed loop changes index 1 from 17 to 23 in distinct, preallocated arrays.
Reset fills all arrays before the timer. After each sample, checks inspect every
element and ensure that adjacent arrays are distinct. Both layouts and lengths
pass 1,000 samples for counts 1, 64, and 1,024 after 2,000 warmup batches.
No speed threshold is asserted and no overhead is subtracted.

At 1,024 stores, all four sites have positive durations. Minimum batch durations
are 250/625 ns for small flat/reference arrays and 11,208/1,166 ns for large
flat/reference arrays. The 64-store flat-small site still has 318 zero samples.
Thus a batch can reduce quantisation, but a small fixed batch is not sufficient
for every site. These data do not justify a per-store performance comparison.

This boundary changes the working set from one repeatedly reset array to many
arrays. Reset and full verification also touch all elements between batches.
The large-array asymmetry needs assessment; do not transplant these results into
the fixed inventory. Next choose and disclose a matched multi-array working set
and connect the real Criterium boundary, or establish bounds for single-store
measurement. Separate allocation/graph integration and flat-read variation remain
unfinished. The original timing attempt is preserved.

Environment and source checksum are in `environment.txt`; source revision is the
base revision before adding this probe. `results.edn` retains summary statistics,
not each duration. This bounded diagnostic does not claim full raw acceptance data.
