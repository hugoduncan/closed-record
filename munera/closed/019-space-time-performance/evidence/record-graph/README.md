# Record graph controls

Both commands exit 0 from the repository root:

```sh
JAVA_HOME=/Users/duncan/.local/share/mise/installs/java/temurin-21 bash dev/bench/memory/record-graph-probe.sh
JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home" bash dev/bench/memory/record-graph-probe.sh
```

`temurin21.edn` and `valhalla27.edn` retain all four control results.
Metadata records the parent revision, dirty state, source hashes, host, and JDKs.
These are tool controls, not timing or fixed-matrix acceptance results.

The walker accepts explicitly supplied record classes and primitive arrays only.
It traverses all instance reference fields, including metadata and extension
maps, but rejects unknown reachable objects instead of returning partial totals.
Static caches and primitive hash fields are not graph edges. Byte arrays make
payload sizes independently measurable without traversing JVM internals.
Expected totals use shallow Instrumentation sizes, not the walker itself.
Both widths check shared and distinct equal payloads, duplicate roots, null,
source/result union, and incremental bytes. Non-null metadata and unknown field
values must fail. This does not validate boxed Long inventory payloads or value
boxes; those need separate controls before tool approval.

Explicit formatting and shared lint pass. The initial lint run found a missing
Java class import; adding the import resolved it without rule suppression.
