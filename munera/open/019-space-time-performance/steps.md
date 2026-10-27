# Implementation steps

Record decisions, commands, verification results, and limits in
`implementation.md` as each slice completes. Commit each completed slice.
The six slices below track implementation work. The final section tracks
feature discovery and plan-review follow-up.

## Slice 1 — Feature and runtime gate

- [ ] Complete the existing `features.md` with build-verified references for the selected value-array and null-restricted-field APIs, their status, and their applicability to closed records, field values, or external types. Source discovery and API selection are complete; build-specific verification remains open.
- [ ] Add isolated benchmark runtime setup for internal Valhalla atomic arrays and `@NullRestricted` fields with pinned Clojure/JDK versions, build-specific APIs, access flags, and layout settings; verify ordinary cases still load on Clojure 1.11.4 and Temurin 21 without experimental tools. API permission is resolved; setup may proceed.
- [ ] Add executable probes for array construction, read, and update; retain structured results that identify the actual representation and supported operations.
- [ ] Add executable probes for field construction, null rejection, read, replacement, and layout; retain separate findings for null restriction and flattening.
- [ ] Run the probes against closed-record applicability and any required external fixtures; save commands, metadata, failures, and support limits under `evidence/`.
- [ ] Resolve the executable capability and layout gate in `features.md` before fixing the matrix: identify each supported comparison or demonstrated limit, and distinguish runtime unavailability from unsupported features. API permission alone does not complete this step; ask for a user decision only if a remaining API interpretation would change the approved scope.

## Slice 2 — Inventory and memory contract

- [ ] Create `dev/bench/cases.edn` with stable IDs, the six original S-03 cases, exact small/larger record widths and array lengths, configurations, and topic-specific operations; check IDs are unique.
- [ ] Specify each case's matched values, changed replacement, mutation/copy semantics, and setup boundary in the inventory; include atomicity and memory-ordering guarantees for compared array operations and explain differences. Verify equivalent logical outputs without timing.
- [ ] Document ns/op, bytes/op, retained roots, payload ownership, identity deduplication, and source/result incremental accounting in `features.md`; define unavailable outcomes and memory-tool limitations.
- [ ] Select and pin memory tooling in benchmark-only dependencies; verify its compatibility with each selected runtime and layout without adding tools to base or ordinary test dependencies.
- [ ] Add allocation controls with known allocating and non-allocating operations; verify the counter scope, operation divisor, and measurement overhead treatment.
- [ ] Add retained-graph controls with shared and unshared values; verify result, source/result union, and newly retained accounting against independently established layouts.
- [ ] Record the complete matrix size, fork count, operation budgets, and estimated manual duration; confirm every supported topic has time and memory coverage before implementing full cases.

## Slice 3 — Reporting and evidence transport

- [ ] Add pure EDN result validation for case/configuration IDs, metric units, raw counts, normalisation inputs, and measured/unavailable/failed states; test malformed and duplicate entries.
- [ ] Add pure metric normalisation and aggregation tests, including unequal batch sizes, missing metrics, invalid divisors, and five-fork variation.
- [ ] Extend table rendering with topic, comparison type, dimensions, units, and unavailable reasons; test deterministic locale-independent output and no unavailable-to-zero conversion.
- [ ] Preserve the original S-03 ratio/interval rule and field-declaration requirement; test that exploratory comparisons do not receive parity decisions.
- [ ] Extend fork transport to persist run manifests and per-child artifacts before aggregation; test revision/configuration association and preservation after a later child fails.
- [ ] Add real short-process tests for nonzero exit, absent or malformed result files, and invalid case/configuration results; verify diagnostics and documented exit semantics.
- [ ] Run existing and new pure/report/fork tests without Criterium or memory tools on the ordinary test classpath; inspect Scry status and failure artifacts.

## Slice 4 — Measured cases and harness validity

- [ ] Add matched changed-value `assoc` cases for each selected record width; verify result values, unchanged sources, and distinct result objects before measurement.
- [ ] Add supported value-array/reference-array cases for construction, indexed read, and applicable update or replacement; verify equal logical data and fixture reset outside timed mutation.
- [ ] Add supported null-restricted/nullable-field cases for construction, read, and replacement; verify non-null logical equivalence and retain the representation checks.
- [ ] Add time, allocation, and retained-size collection for the fixed cases using the validated tools; test that unsupported cases and metrics retain explicit reasons.
- [ ] Check generated measurement forms and executable smoke probes for array-local compilation, reflection, opaque input handling, nested-form hoisting, and result consumption; record evidence that the intended operation remains measured.
- [ ] If correctness requires a Criterium upgrade, pin the verified version and rerun all six original case smoke checks and reporting regressions; otherwise record that the current version remains sufficient.
- [ ] Verify five independent child JVMs, alternating pair order, one execution per call site, inherited runtime flags, and separate memory instrumentation where needed.
- [ ] Run bounded smoke commands for each fixed case/configuration and check complete case identity, units, results, and explicit unavailable states; do not classify these partial or short runs as acceptance.
- [ ] Compare resolved base and ordinary test dependencies with the pre-change baseline; verify measurement tools and experimental fixtures remain isolated.

## Slice 5 — Acceptance evidence

- [ ] Record exact reproduction commands, capture paths, expected duration, failure semantics, and evidence bounds in `features.md`; commit the fixed harness and inventory before the first full run.
- [ ] Run the original S-03 command and field-declaration test on the ordinary runtime; retain structured results, full command output, exits, and the manifest even when parity fails.
- [ ] Run the complete new matrix on its specified configurations; retain the first complete run with per-fork time/memory results and capability outcomes, including unfavourable and unavailable results.
- [ ] Record all incomplete attempts and failures with revision, cause, and disposition; preserve prior evidence if a correctness fix requires a new harness revision.
- [ ] Audit the bounded `evidence/` set for complete case coverage, traceable metadata, units, uncertainty, and valid memory accounting; record any acceptance gap rather than treating it as zero or success.

## Slice 6 — Documentation and final verification

- [ ] Create `README/performance.md` with all three topics, definitions, configurations, commands, comparison semantics, measured tables, uncertainty, and support limits; link each numeric table to retained run/case evidence.
- [ ] Qualify the main README performance claim to match the evidence and add a direct adjacent link to the report.
- [ ] Update `README/development.md` to link to the report and its manual commands without duplicating measured tables.
- [ ] Run `clojure -M:test -m scry.cli` on the ordinary runtime, then the `:test:clj-1.12` and `:test:clj-1.13` variants; inspect exits and `.scry-results/` and record outcomes.
- [ ] Run `bb clj-fmt:check`, `bb clj-kondo:lint`, `bb vale`, and `bb api-docs --check`; correct defects and record final results without adding timing gates to CI.
- [ ] Stage the intended Markdown evidence and documentation, then run `bb lychee:check` so all new tracked links are checked; record local and online results.
- [ ] Audit every public claim against the retained evidence and all six design acceptance criteria; append final verification, support limits, and unresolved acceptance gaps to `implementation.md`.

## Feature discovery follow-up

- [x] Review authoritative feature documentation and pin internal API source references; record distinct candidates in `features.md`.
- [x] Resolve API selection from design commit `adfa652` in `features.md` and `plan.md`: internal Valhalla atomic arrays and `@NullRestricted` fields are the selected path. Runtime setup may proceed; no further API-selection decision is needed. Original slice 1 items remain open: build-specific verification and executable capability/layout evidence are still required before fixing the matrix.
