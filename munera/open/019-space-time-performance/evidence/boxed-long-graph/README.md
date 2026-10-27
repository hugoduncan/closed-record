# Boxed Long graph controls

Run from the repository root:

```sh
JAVA_HOME=/Users/duncan/.local/share/mise/installs/java/temurin-21 bash dev/bench/memory/record-graph-probe.sh
JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home" bash dev/bench/memory/record-graph-probe.sh
```

Both commands exit 0. Each EDN file contains two forms: existing primitive-array
controls and new inventory-driven Long controls. Metadata includes source hashes,
parent revision, dirty state, host, and runtime versions. These are memory-tool
controls, not timing or acceptance results.

The bounded walker now accepts exact Long leaves. Long has no instance reference
edges on these runtimes. Expected totals sum independent Instrumentation shallow
sizes for the inventory's distinct payloads and known shared source/result fields.
Both widths and both record types check source, result, union, incremental bytes,
and repeated roots, including a repeated cached Long root. Existing unknown-type
and metadata rejection checks still pass. No module-opening flags are needed.

Cached payloads count as reachable bytes even though another root also owns them.
Incremental reachable size is not new allocation or dominator-based retention.
This change does not establish physical identity for experimental value boxes.

Explicit formatting and focused lint pass. Shared lint reports an unrelated
existing duplicate clojure.edn require in dev/acceptance/api_content_defect.clj;
that file was not changed. No ordinary test dependencies changed.
