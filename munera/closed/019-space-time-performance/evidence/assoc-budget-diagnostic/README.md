# Ordinary assoc allocation budget diagnostic

This is partial diagnostic evidence, not the first complete acceptance run.
The measured code was clean at revision
`15ee7adcaabab0267470e92d5283ce365c0f278c`. No code changed for this run.

From the repository root:

```sh
JAVA_HOME="$HOME/.local/share/mise/installs/java/temurin-21.0.10+7.0.LTS" \
clojure -Sdeps '{:paths ["src" "dev/bench"]}' -M \
  -m closed-record.bench.assoc-allocation-cli \
  munera/open/019-space-time-performance/evidence/assoc-budget-diagnostic/run \
  assoc-budget-diagnostic 15ee7adcaabab0267470e92d5283ce365c0f278c 10000 20
```

Use a new output directory to reproduce. Parent exit: 0. All five child exits,
transport checks, and runtime checks pass. The manifest records runtime, command,
OS, inventory, and budgets. Raw process and capture files are preserved unchanged.

## Observations and decision

Each of the 20 rows has 20 warmup batches and one final batch of 10,000
operations. Across both representations and all forks, width 3 final batches
allocate 400,000 bytes and width 16 batches allocate 960,000 bytes. Warmup batches
2–20 equal the final count for every row. The first batch has an extra 232 bytes
for the first row in each child and 176 bytes for each later row. The first
boundary batch in each child allocates 56 bytes; all other boundary batches,
including final boundaries, allocate zero bytes. These are observed counts, not
portable guarantees. No overhead is subtracted.

Use 10,000 operations and 20 warmup batches as the candidate ordinary assoc
allocation budget for subsequent integration. This gives 19 consecutive unchanged
warmup observations in every row. It does not prove JIT convergence, establish
timing budgets, or validate other runtimes and layouts. Keep raw warmup controls
in acceptance evidence and report any change rather than discard a run.

No code or dependencies changed, so no new unit tests were needed. The real
five-child command exercises collection, serialization, capture, and runtime
validation. A later ad hoc inspection expression failed to parse; it did not
launch measurements or change evidence. A corrected read-only inspection read
all 20 rows and their controls.
