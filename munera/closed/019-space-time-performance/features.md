# Feature gate

## Current use

The capability gate is complete; see “Completed operation contract and gate”.
Use `plan.md` and `steps.md` for remaining work. Earlier discovery notes and
partial-command descriptions preserve evidence, not mandatory new milestones.
Existing separate metric commands may supply acceptance evidence once their
budgets and required coverage are verified. A universal matrix runner is not
required. Demonstrated measurement limits may remain explicitly unavailable
under the design; missing implementation and failed runs may not.

## Discovery: 2026-10-27

API selection is resolved below by design commit `adfa652`. No benchmark matrix
is fixed, and no capability or performance result is claimed. The discovery
findings come from upstream documentation and source, not executable probes.

## Distinct candidate APIs

1. [JEP 401: Value Objects](https://openjdk.org/jeps/401) is integrated for
   JDK 28 as a preview. It introduces `value class` and permits reference
   flattening, including nullable array elements and fields. Its documented
   compile command uses `javac --release 28 --enable-preview`; execution uses
   `java --enable-preview`. Flattening is a JVM choice, not a semantic guarantee.
   An array of value objects need not be null-restricted.
2. [Null-Restricted Value Class Types](https://openjdk.org/jeps/8316779)
   is a separate draft, updated 2026-05-28. It proposes `Point!` types and
   `Point![]` arrays. It explicitly excludes null-restricted types for identity
   classes and classes without a default value. A draft is not proof that a
   downloadable runtime implements the syntax.
3. The Valhalla `lworld` branch has internal experimental APIs. At revision
   `12e87d09246d366cc74cb409e4c914351256e98b`,
   [ValueClass.java](https://github.com/openjdk/valhalla/blob/12e87d09246d366cc74cb409e4c914351256e98b/src/java.base/share/classes/jdk/internal/value/ValueClass.java)
   exposes `newNullRestrictedAtomicArray`, `newNullRestrictedNonAtomicArray`,
   `isFlatArray`, `isNullRestrictedArray`, and `isNullRestrictedField`.
   [NullRestricted.java](https://github.com/openjdk/valhalla/blob/12e87d09246d366cc74cb409e4c914351256e98b/src/java.base/share/classes/jdk/internal/vm/annotation/NullRestricted.java)
   defines an internal field annotation. Its documentation says the annotation
   is ignored unless the field type is a concrete value type that can be
   preloaded. It is for internal experimental use, not specified Java SE
   behaviour. Atomic and non-atomic arrays are distinct comparison contracts.

The [Valhalla project page](https://openjdk.org/projects/valhalla/) lists value
objects, null-restricted storage, and array enhancements separately. The
[Valhalla download page](https://jdk.java.net/valhalla/) currently offers
`27-jep401ea3+1-1` (2026-03-11), including macOS AArch64. That build predates
both the current draft update and JDK 28 integration. Its availability does not
establish support for the current internal APIs or draft syntax. No experimental
runtime was installed or selected in this pass.

## Applicability and next probes

Closed records currently use the Clojure record layout, not Java `value class`
declarations. Do not label an external Java value class as a closed record.
Potential comparisons include an external value class array, or a closed-record
field that holds an external array/value object. Those are different claims.
A Clojure type hint does not request null restriction.

With API selection resolved, probes must establish construction, read, replacement,
null rejection, and actual layout independently. Ordinary baseline remains
Clojure 1.11.4 with Temurin 21, without preview flags. Exact runtime builds,
module exports, and layout flags for experimental probes remain to be verified.
No support limit has yet been demonstrated by execution.

## Selected path and remaining gate

Design commit `adfa652` permits internal Valhalla APIs with atomic arrays.
Use `ValueClass.newNullRestrictedAtomicArray` for null-restricted value arrays
and `jdk.internal.vm.annotation.NullRestricted` for fields on external value-type
fixtures where required. The references above identify the API candidates to
verify against the selected build; they do not prove that the listed EA binary
contains them. Do not require public APIs, non-atomic arrays, or draft `!` syntax.

Runtime selection, download, and isolated setup may proceed without another
user decision. Pin the actual JDK build, Clojure version, internal API signatures,
compile/run commands, module access flags, and layout settings. Keep this setup
in benchmark tooling and disclose its experimental status in the performance
report. Verify and record atomicity and memory-ordering guarantees for each
compared operation; atomicity alone does not prove flat storage.

The capability gate remains open. Run executable operation, null-rejection,
and layout checks before fixing the matrix. Establish closed-record applicability
separately from external comparison types. A missing runtime is an availability
limit, not proof that the feature is unsupported. If a selected build lacks the
APIs, record that result and seek a suitable build within this path; do not
silently substitute nullable arrays or draft syntax. Ask for a user decision
only if a remaining API interpretation would change the approved scope.

## Build probe: 2026-10-27

Downloaded and SHA-256 verified the macOS AArch64 EA archive, build
`27-jep401ea3+1-1`. The pinned URL and checksum are in
[evidence/runtime-gate/manifest.edn](evidence/runtime-gate/manifest.edn).
Extract it into ignored `target/valhalla/`, then run from the repository root:

```sh
JAVA_HOME=$PWD/target/valhalla/jdk-27.jdk/Contents/Home \
  bash dev/bench/valhalla/probe.sh
```

The script compiles with preview source 27 and runs with preview enabled.
It exports `jdk.internal.value`, `jdk.internal.misc`, and
`jdk.internal.vm.annotation` from `java.base`. No layout flags are changed.
`runtime.txt` retains default layout flags and the build's actual API signatures.
The archive's `lib/src.zip` is the build-specific API documentation; current
branch documentation above is not identical to this build.

The real Java probe asserts construction, read, element replacement, null
rejection, and atomic-array metadata. `ValueClass.isFlatArray` reports flat
storage; `Unsafe.isFlatField` reports a flat null-restricted field. Holder
replacement creates a new holder, while array replacement mutates one element.
The ordinary reference-array control is explicitly made with `newReferenceArray`
and is not flat. These are external Java types, not closed records.

The nullable field also reports flat storage. Thus null restriction is not the
cause of all flattening, and this fixture is not yet a nullable **reference**
field comparison. Add a verified non-flat field control before the matrix.
The atomic-array query states that its layout is designed for atomic accesses;
these plain stores do not establish volatile ordering or a happens-before edge.
Full operation-contract documentation remains open.

This build requires strict field initialization before `super()` for the
annotated field. The initial probe failed verification; the manifest preserves
that attempt and correction. Successful output is in `capability.edn`.
The executable assertions use the real VM, without mocks. Temurin 21.0.10+7-LTS
also loads the unchanged library with Clojure 1.11.4 and no preview tools.
Closed-record applicability, experimental Clojure loading, and the final
capability gate remain open. These results are not performance evidence.

## Non-flat field control: 2026-10-27

The probe now includes a final `Object` field containing the same `Point`
values as the restricted field. Runtime queries assert that this field is
neither null-restricted nor flat. Real operations assert null acceptance,
reads, and replacement by a distinct holder, with the source value unchanged.
The nullable `Point` field remains a separate control: it is nullable and flat.
Thus the comparison must label both declared type and observed layout; it must
not attribute all storage differences to null restriction alone. Reading the
Object field also requires a cast to Point when accessing its payload.

[evidence/reference-field/capability.edn](evidence/reference-field/capability.edn)
retains the successful probe, and its adjacent manifest records source hashes
and the command. The original runtime-gate evidence is unchanged. No timing or
memory result is implied. Experimental Clojure applicability and operation
ordering contracts still need verification before the gate can close.

## Clojure applicability: 2026-10-27

Run the real Clojure assertions with the same pinned JDK:

```sh
JAVA_HOME=$PWD/target/valhalla/jdk-27.jdk/Contents/Home \
  bash dev/bench/valhalla/applicability.sh
```

The launcher compiles and checks the Java fixtures, then loads Clojure 1.11.4
and the library with only the base classpath and compiled fixtures. No dependency
or alias changes are needed. Assertions fail the process on a contract violation.
[Structured results](evidence/clojure-applicability/result.edn) and the adjacent
manifest retain the command, source hashes, and the initial import failure.

Both generated record classes are identity classes, not value classes. An array
of these closed records is not flat. Closed records can hold the external Point
value and flat array as ordinary field values. Changed-value assoc preserves the
source; mutation through the stored array remains visible, as for any mutable
field value. A Point type hint still emits an Object field: runtime queries
confirm no null restriction and no flattening, and assoc accepts null. Thus
value arrays and restricted-field comparisons must use the external Java types,
not claim that closed records gain these representations. This probe establishes
applicability, not performance. Build-specific ordering documentation remains
open before the fixed matrix.

## Completed operation contract and gate: 2026-10-27

This section supersedes the open-gate statements in the chronological discovery
notes above. The executable gate passes on the pinned EA, not on arbitrary
Valhalla builds. The matrix and memory-tool gate remain separate work.

Build authority is `java.base/jdk/internal/value/ValueClass.java` and
`java.base/jdk/internal/vm/annotation/NullRestricted.java` inside the verified
archive's `lib/src.zip`. The former defines `isAtomicArray` as a query for a
layout designed for atomic accesses. The latter requires strict initialization
and rejects null stores; neither promises volatile ordering. These internal
contracts are experimental, not Java SE APIs. The source hash and repeat probe
outcomes are in [operation-contract evidence](evidence/operation-contract/manifest.edn).

| Operation | Atomicity and ordering | Update semantics |
| --- | --- | --- |
| Atomic value-array indexed read/store | Whole element uses the runtime's atomic layout; plain access, no volatile ordering | Store mutates one element; null rejected |
| Reference-array indexed read/store | Atomic reference access; plain access, no volatile ordering | Store mutates one reference; null accepted |
| Array construction | Private initialization, not publication to another thread | Allocate and initialize a new array |
| Restricted, nullable, or Object field read | Read an initialized final field in an immutable holder; no concurrent field writes | No field mutation |
| Holder construction/replacement | Final-field initialization, not a volatile operation | Construct a new holder; preserve the source |

The reference rules follow [JLS 17.7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.7);
final-field initialization follows [JLS 17.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.5).
Plain array access creates no happens-before edge between threads. Atomic here
means no torn element, not compare-and-set, read-modify-write, or synchronization.
No concurrent-throughput or publication claim is supported by these probes.
Final fields have no replacement store: replacing a holder is construction, not
persistent map assoc and not an array store. Keep all measurement fixtures
thread-confined. Do not infer mutable flat-field atomicity from this final-field
probe or infer flat storage from atomicity.

Supported experimental comparisons are external Point arrays (flat restricted
atomic versus non-flat nullable reference) and immutable holders (flat restricted
Point versus non-flat nullable Object with a Point payload and read cast). The
nullable Point holder is a separate flat control, not the reference baseline.
Probes now assert both array metadata contracts and the required flat layouts,
so an unexpected layout fails rather than silently changing the comparison.
Closed records remain identity classes with nullable Object fields. They can
hold these external objects but do not acquire their storage representation.
Ordinary closed-record/defrecord measurements remain on Temurin 21 without
preview flags. No support limit is caused by unavailable access, and no human
API decision remains. Memory measurements and timings are not yet validated.

## Fixed inventory and memory accounting

The [measurement contract](measurement-contract.md) defines the fixed dimensions,
units, payload ownership, graph roots, and initial run estimate for
`dev/bench/cases.edn`. Memory-tool validation and executable equivalence remain
open; no time or space result is claimed by the inventory.

## Saved evidence report command

Run `clojure -Sdeps '{:paths ["src" "dev/bench"]}' -M -m closed-record.bench.saved-report RUN-DIRECTORY`
from the repository root. This command needs no measurement tools. It reads an
existing manifest and captures, then prints Markdown to stdout without changing
evidence. Exit 0 means reporting succeeded, not performance acceptance. Explicit
unavailable metrics remain unavailable. Invalid arguments, rejected evidence,
and file IO failures print a diagnostic to stderr and exit 2. No table is printed
for these failures. This command does not run measurements or replace the original
S-03 command and its exit meanings.

## Partial allocation child command

From the repository root, with the ordinary JDK selected explicitly:

```sh
clojure -Sdeps '{:paths ["src" "dev/bench"]}' -M \
  -m closed-record.bench.assoc-allocation-child \
  target/assoc-smoke.edn smoke REVISION 1 17 2
```

Replace `REVISION` with the measured source revision. The output parent directory
must exist; the output file must not exist. Arguments after the revision are
fork, operations per batch, and warmup batches. The small counts above test the
command only; they are not acceptance budgets. This command measures allocation
for both ordinary widths and alternates representation order by fork. It writes
runtime observations and raw controls, without subtracting overhead. It does not
create a parent manifest or supply complete matrix coverage. Exit 0 means the
partial envelope was written. Invalid arguments and IO failures exit 2 with a
stderr diagnostic; other runtime failures remain nonzero JVM failures. Use parent
capture for durable stdout, stderr, command, and exit evidence.

## Manual partial allocation command

Select the ordinary Temurin 21 installation with `JAVA_HOME` before running.
From the repository root, use a new output directory for each attempt:

```sh
clojure -Sdeps '{:paths ["src" "dev/bench"]}' -M \
  -m closed-record.bench.assoc-allocation-cli \
  target/assoc-smoke assoc-smoke "$(git rev-parse HEAD)" 3 1
```

The last arguments are operations per batch and warmup batches. Both must be
positive decimal integers. The example is a process smoke check, not a selected
measurement budget. It runs five sequential JVMs and four allocation rows per
JVM. Allow about one minute for JVM startup; no timeout is enforced. Larger
budgets increase allocation and retained roots. Do not use maximum integer
budgets as a practical bound.

The new directory contains the manifest, process streams, raw EDN, and capture
checks. Existing directories are never replaced. Exit 0 means all five children
passed process, transport, and runtime checks, with every ordinary assoc allocation
row measured and its raw warmup controls validated. Missing rows or invalid controls
fail with exit 2. This does not mean complete matrix
coverage, stabilized measurements, or acceptance. Invalid arguments, IO errors,
and failed child checks exit 2. Other uncaught failures exit nonzero. Keep failed
attempt directories. The command records the effective Java invocation and basic
OS metadata; the supplied revision is a declaration, not a clean-worktree check.
No timing, graph measurement, Criterium, or experimental JVM flags are required.

The [ordinary allocation budget diagnostic](evidence/assoc-budget-diagnostic/README.md)
retains five pinned-runtime children and all warmup controls. The candidate budget
is 10,000 operations per batch and 20 warmup batches for ordinary assoc allocation
only. It is not a timing budget or complete acceptance evidence.

### Allocation controls in saved reports

The saved-report command appends per-fork allocation controls when present in a
complete run. It shows raw assoc and boundary batch bytes, changed batch indices,
and trailing warmup matches. It does not subtract boundary costs or infer JIT
convergence. Missing controls are not assessed; malformed supplied controls fail
reporting. Partial allocation diagnostic runs still fail full-matrix coverage.

### Read a partial allocation diagnostic

Use the ordinary test classpath to render saved allocation controls without
Criterium or another measurement run:

```sh
clojure -M:test -m closed-record.bench.assoc-allocation-report PATH-TO-SAVED-RUN
```

The reader requires all five successful ordinary allocation children, matching
runtime observations, complete allocation rows, and declared budgets. It does not
write evidence. Exit 0 means the diagnostic was reported, not acceptance. Exit 2
means invalid arguments, rejected evidence, or a file IO failure; diagnostics go
to stderr and no table is printed. The report
shows raw assoc and boundary counts, transitions, and trailing matches. It is a
partial diagnostic, not complete matrix or acceptance evidence.

## Ordinary assoc timing smoke test

The benchmark-only `closed-record.bench.assoc-timing/collect!` measures four
literal assoc expressions, once per JVM. It returns raw timing rows for widths
3 and 16. It does not persist a run or establish acceptance. Fixture setup is
outside timing; raw batch estimates retain their actual divisors.

Run the isolated real-Criterium test in a fresh JVM:

```sh
clojure -Sdeps '{:aliases {:smoke {:extra-paths ["test/bench"] :main-opts []}}}' \
  -M:test:bench:smoke -m scry.cli --namespace closed-record.assoc-timing-test
```

The final alias clears the benchmark's default main options. The 0.1-second
budget tests measurement plumbing only; expect warmup and sample-count warnings.
Do not publish these short timings as performance evidence. Ordinary tests do
not include this path or require Criterium.

`closed-record.bench.assoc-timing-child/write-results!` connects this collector to
an exclusive result file with observed JVM metadata. The same smoke test now
checks the saved EDN round trip. Supply an absent path and use a fresh JVM per
call. Independent child capture and full-matrix reporting remain separate work.

The timing child CLI accepts `RESULT RUN REVISION FORK SECONDS`:

```sh
clojure -Sdeps '{:aliases {:child {:main-opts []}}}' -M:bench:child \
  -m closed-record.bench.assoc-timing-child result.edn diagnostic REVISION 1 1.0
```

Use an absent result path and the actual source revision. Seconds is the
per-site budget, not a wall-clock timeout or an acceptance threshold. Exit 2
reports invalid input or IO failure. Exit zero means partial evidence was
written. Test independent process capture with the smoke command above, replacing
the namespace with `closed-record.assoc-timing-process-test`. This test uses a
one-second budget; its timings are not acceptance evidence.

The pure `closed-record.bench.assoc-timing-command/command` builder accepts the
inventory, child association, result path, and numeric seconds budget. It inherits
the current Java home, JVM flags, and classpath. It does not launch a process or
persist budget provenance. A one-second process check exposed a Criterium
`collect/batch-size` divide-by-zero; timing smoke reliability remains unresolved.

The [released candidate screen](evidence/timing-tool-candidate/README.md) rejects
Criterium 0.5.253-ALPHA as a fix: its collection sources are unchanged and the
zero-estimate diagnostic confirms the same exception. The dependency remains
0.5.251-ALPHA. One-shot collection is not a substitute for warmed batch statistics.

### Timing collector identity

Ordinary timing child rows include `:timing-tool`: library, base version,
collector SHA-256, selected resource URL, and distribution. The local overlay
also records `:positive-planning-duration-v1` and the patch SHA-256. Unknown
collector source bytes fail before collection. These fields identify the selected
source resource, not the entire dependency graph or runtime var changes. Use a
fresh, unmodified child JVM. Default aliases remain unchanged. Parent declaration
reconciliation and original S-03 provenance remain required before adoption.

### Manual partial timing parent

`assoc-timing-cli` saves five ordinary assoc timing forks with explicit tool,
runtime, machine, command, and per-site budget metadata. It validates saved
coverage and provenance before success. Use the verified opt-in overlay; default
aliases still select the unmodified dependency.

```sh
sh dev/bench/timing/prepare.sh \
  "$HOME/.m2/repository/org/hugoduncan/criterium/0.5.251-ALPHA/criterium-0.5.251-ALPHA.jar" \
  target/timing-overlay-manual
clojure -Sdeps '{:aliases {:timing {:extra-paths ["target/timing-overlay-manual"] :main-opts []}}}' \
  -M:bench:timing -m closed-record.bench.assoc-timing-cli \
  target/timing-diagnostic diagnostic "$(git rev-parse HEAD)" 1.0
```

Use a supported ordinary JDK (the smoke test uses Temurin 21). Both output paths
must be absent. One second is a plumbing smoke budget, not an acceptance budget
or a process timeout. Children run sequentially; no automatic retry occurs.
Exit 0 means validated partial diagnostic evidence, not full-matrix acceptance.
Exit 2 reports invalid arguments, rejected evidence, or IO failure. Other runtime
errors can exit nonzero. Failed-run files remain available; use a new directory
for a later attempt. Legacy S-03 tool provenance and default correction adoption
remain open.

### Default corrected timing launcher

Use `sh dev/bench/timing/run.sh` for the original S-03 command. To run the
partial timing diagnostic, use:

```sh
sh dev/bench/timing/run.sh -m closed-record.bench.assoc-timing-cli \
  target/timing-diagnostic diagnostic "$(git rev-parse HEAD)" 1.0
```

The launcher resolves the pinned benchmark classpath, verifies the JAR and
corrected source checksums through `prepare.sh`, and creates a fresh overlay
under `target/timing-run.*`. It retains the overlay for inherited child
classpaths and recorded resource URLs. No dependency cache is changed. Remove
these directories only after the run has ended. Preparation failures stop the
command; child exit codes are preserved. No retries occur.

This is now the default documented timing path. Earlier opt-in commands remain
valid. Direct `clojure -M:bench` remains an uncorrected diagnostic path; base and
ordinary test dependencies are unchanged. Parent and original S-03 provenance
gates are implemented and verified. One-second budgets remain smoke-only.
Acceptance budgets, full-matrix integration, and memory-tool approval remain
open. The local correction is not an upstream release.

### Saved partial timing report command

Report an existing diagnostic without loading timing tools or running measurements:

```sh
clojure -Sdeps '{:paths ["src" "dev/bench"]}' -M \
  -m closed-record.bench.assoc-timing-report target/timing-diagnostic
```

The command validates all five saved forks and their declared runtime, tool, and
budget before printing the table. It does not write evidence. Exit 0 means report
success, not acceptance or S-03 parity. Exit 2 reports invalid arguments, rejected
evidence, or file IO failures, with stderr diagnostics and no table on stdout.

### Ordinary assoc graph collector check

`closed-record.bench.assoc-graph/collect!` returns three raw graph rows for a
fixed ordinary assoc representation. It requires the isolated RecordGraphProbe
class and agent. It counts reachable physical fields and shared Long leaves,
not allocation or dominator retention. Unknown payload types fail explicitly.
Run the real-agent regression from the repository root with Temurin 21 selected:

```sh
bash dev/bench/memory/record-graph-probe.sh
clojure -J-javaagent:target/bench-memory/record-graph-agent.jar \
  -Sdeps '{:paths ["src" "target/bench-memory/classes" "test/bench"]}' \
  -M:test -m scry.cli --namespace closed-record.bench.assoc-graph-test
```

The agent-only `assoc-graph-child/write-results!` persists both widths through
the exclusive child envelope writer. Its regression namespace is
`closed-record.bench.assoc-graph-child-test`. For child tests, use the isolated
build below and put its JAR, not its classes directory, on the classpath.
This is collector verification, not an acceptance run. Independent child process
capture uses `assoc-graph-command/command` with inherited JVM flags and classpath.
Verified agent provenance remains open. Experimental reference-container
value-box graphs remain unvalidated.

The graph child command takes `RESULT RUN REVISION FORK`:

```sh
clojure -J-javaagent:target/record-graph-build/record-graph-agent.jar \
  -Sdeps '{:paths ["src" "target/record-graph-build/record-graph-agent.jar"]}' \
  -M:test -m closed-record.bench.assoc-graph-child result.edn diagnostic REVISION 1
```

Use an actual source revision in place of `REVISION`. The output path must not
exist. Exit 2 reports invalid arguments or file IO errors; exit 0 means partial
graph publication, not acceptance. The child test above also verifies independent
instrumented process capture. Verified agent provenance remains open.

### Isolated ordinary graph agent build

With the pinned Temurin 21 `JAVA_HOME`, run:

```sh
sh dev/bench/memory/prepare-record-graph.sh target/record-graph-build
sh dev/bench/memory/test-prepare-record-graph.sh
```

The output directory must not exist. The build checks the known source hash,
compiles for Java 21, and retains source, compiler version, class, agent JAR,
and `SHA256SUMS`. Fixed archive timestamps permit identical builds with the
same compiler. Failed build directories remain for diagnosis. Use the resulting
JAR with `-javaagent` and that same JAR on the graph-only classpath.
Each child row retains `:graph-tool` with the active agent JAR and class resource
SHA-256 and location. Class-directory loading is rejected. Resource hashes do
not detect runtime class transformations. Parent reconciliation with declared
build hashes remains required before graph acceptance measurements.

### Prepared graph build declarations

`closed-record.bench.graph-build/read-declaration` takes the output directory of
`dev/bench/memory/prepare-record-graph.sh`. It verifies the retained checksums,
pinned source, and packaged agent class without loading instrumentation or
changing files. The returned JAR/class hashes can populate `:graph-tool` parent
metadata. This checks artifact consistency, not compiler trust or runtime class
transformation. The graph parent reads this declaration before launch.

Run the real build-reader tests separately from ordinary tests:

```sh
clojure -Sdeps '{:aliases {:graph-build-test {:extra-paths ["test/bench"]}}}' \
  -M:test:graph-build-test -m scry.cli \
  --namespace closed-record.bench-graph-build-test
```

Set `JAVA_HOME` to the selected benchmark JDK (21 or later) before this command.
These tests compile the real agent but do not measure performance.

### Manual partial graph collection

Select the benchmark JDK and prepare the agent as above. Then run:

```sh
clojure -Sdeps '{:aliases {:graph-run {:extra-paths ["dev/bench" "target/record-graph-build/record-graph-agent.jar"] :jvm-opts ["-javaagent:target/record-graph-build/record-graph-agent.jar"]}}}' \
  -M:graph-run -m closed-record.bench.assoc-graph-cli \
  target/assoc-graph-diagnostic graph-diagnostic "$(git rev-parse HEAD)" \
  target/record-graph-build
```

The run directory must not exist. The parent retains its effective command,
machine metadata, verified build identity, and five independent child captures.
The agent JAR must be on both the classpath and the agent flags. Children inherit
these settings. Do not use this instrumented invocation for timing measurements.
Exit 0 means validated partial graph evidence, not full-matrix acceptance or
memory-tool approval. Invalid input, file errors, or failed collection exit 2.
Failed captures remain for diagnosis; the command does not retry or overwrite.

Render saved ordinary graph diagnostics without loading the agent:

```sh
clojure -Sdeps '{:paths ["src" "dev/bench"]}' -M \
  -m closed-record.bench.assoc-graph-report target/assoc-graph-diagnostic
```

The reader checks all five forks before rendering. It does not change saved files.
This graph-only report does not establish full-matrix acceptance or memory-tool
approval. Result, source/result union, and union minus source remain separate.

The report command exits 0 after successful rendering, not acceptance. It exits
2 for invalid arguments, rejected evidence, or file IO errors, without a table.

### Combined ordinary diagnostic report

Report three completed, compatible ordinary runs without measurement tools:

```sh
clojure -Sdeps '{:paths ["src" "dev/bench"]}' -M \
  -m closed-record.bench.assoc-suite-report \
  target/assoc-timing-diagnostic target/assoc-allocation-diagnostic \
  target/assoc-graph-diagnostic
```

Arguments are timing, allocation, then graph run directories. All saved readers
and cross-run compatibility checks must pass before output. The command preserves
source manifests and allocation controls and does not change evidence files.
Do not change the source files during reporting. Exit 0 means report success,
not acceptance. Invalid arguments, rejected evidence, and file IO errors exit 2
without a report. This report does not cover experimental cases or approve memory
tooling. It does not replace the complete matrix or the original S-03 decision.

## Matrix budget accounting

`closed-record.bench.matrix-budget/summarise` derives planned row counts from
`dev/bench/cases.edn`, without loading measurement tools:

```sh
clojure -M:test -e '(require (quote [clojure.edn :as edn]) (quote [closed-record.bench.matrix-budget :as budget])) (prn (budget/summarise (edn/read-string (slurp "dev/bench/cases.edn")) 5))'
```

The fixed inventory has 17 cases and five forks. It requires 490 outcome rows:
170 time, 110 allocation, and 210 graph rows. Graph roots are separate rows.
Failed and unavailable outcomes still occupy required slots. These counts do
not specify the number of child processes; metric instrumentation stays separate.

At an explicit five-second per-site timing limit, the sum is 850 seconds
(14 minutes 10 seconds): 400 seconds ordinary and 450 seconds experimental.
This is **not a wall-clock estimate or timeout**. It excludes JVM startup,
compilation, warmup, analysis, allocation, and graph collection. Acceptance
budgets and a measured manual-duration estimate remain open. The inventory's
five-second value is not newly approved by this calculation.

### Unchecked experimental operation checks

After the fixture command, compile the operations and run their real-expression
checks (not acceptance measurements):

```sh
export JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
bash dev/bench/valhalla/fixtures.sh
"$JAVA_HOME/bin/javac" --enable-preview -source 27 \
  --add-exports java.base/jdk.internal.value=ALL-UNNAMED \
  -cp target/valhalla/classes -d target/valhalla/classes \
  dev/bench/valhalla/Operations.java
clojure -Srepro -Sdeps '{:aliases {:probe {:extra-paths ["target/valhalla/classes"] :jvm-opts ["--enable-preview" "--add-exports=java.base/jdk.internal.value=ALL-UNNAMED" "--add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED"] :main-opts []}}}' \
  -M:bench:test:probe -m scry.cli \
  --namespace valhalla.operations-test \
  --namespace valhalla.operations-measured-test
```

Stores return the mutated array for consumption. Reset remains outside timing.
Holder replacement constructs a new holder from the prebuilt replacement value;
it does not mutate the source. Generated-expression tests do not establish
collection, allocation, graph support, or acceptance results.

### Experimental timing child

With the same compiled fixtures, pinned JVM, preview/access flags, and verified
collector overlay, invoke `valhalla.timing-child` instead of the test runner:

```sh
# Use the :probe alias from above, adding target/019-timing-overlay to extra-paths.
clojure -Srepro -Sdeps '{:aliases {:probe {:extra-paths ["target/valhalla/classes" "target/019-timing-overlay"] :jvm-opts ["--enable-preview" "--add-exports=java.base/jdk.internal.value=ALL-UNNAMED" "--add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED"] :main-opts []}}}' \
  -M:bench:probe -m valhalla.timing-child \
  target/experimental-timing-fork-1.edn experimental-smoke "$(git rev-parse HEAD)" 1 1
```

The result path must be absent. Arguments end with fork (1–5) and positive
seconds per site. The command saves 18 raw timing rows with observed collector
identity, budget, and JVM metadata. Do not load the memory agent. Exit zero
means one partial child was saved, not full coverage or acceptance. Run each
fork in a fresh JVM. Separate memory collection and combined experimental
reporting remain unfinished. Verify publication with `valhalla.timing-child-test`
in the same isolated test invocation used for the other experimental tests.
