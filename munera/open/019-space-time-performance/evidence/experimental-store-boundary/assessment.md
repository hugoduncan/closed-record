# Experimental store boundary

This is a bounded real-tool diagnostic, not timing evidence or a support limit.
The pinned Valhalla fixture checks pass. Criterium's generated batch evaluates
one operation for its returned value, then consumes the remaining operations.
It does not reset a mutable array between operations. A two-operation batch
therefore stores 23 over 23 on its second operation, contrary to the fixed
changed-store contract. Both array layouts and both inventory lengths reproduce
this boundary. A single-operation invocation changes 17 to 23 correctly.

The diagnostic Java operation checks the previous value. It is not a proposed
acceptance expression. The first test wrongly expected Criterium to return the
last operation's value; it returns the first. The retained structured failure
shows this test error. The corrected test detects the invalid second store by
its state precondition, without mocks or timing assertions.

## Reproduction

Run from the repository root:

```sh
export JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
bash dev/bench/valhalla/fixtures.sh
"$JAVA_HOME/bin/javac" --enable-preview -source 27 \
  --add-exports java.base/jdk.internal.value=ALL-UNNAMED \
  -cp target/valhalla/classes -d target/valhalla/classes \
  dev/bench/valhalla/StoreBoundaryProbe.java
clojure -Srepro -Sdeps '{:aliases {:probe {:extra-paths ["target/valhalla/classes"] :jvm-opts ["--enable-preview" "--add-exports=java.base/jdk.internal.value=ALL-UNNAMED"] :main-opts []}}}' \
  -M:bench:test:probe -m scry.cli --namespace valhalla.store-boundary-test
```

Final Scry: one test, 16 assertions pass, exit 0, no failure files.
Focused formatting and clj-kondo pass. Clojure is 1.11.4; Criterium is the pinned
0.5.251-ALPHA. This test invokes generated measurements directly and does not
use the estimator, so the estimator overlay is not needed.

## Remaining integration

Do not wire a repeated plain store expression into the acceptance matrix.
Use Criterium's supported custom Measured boundary to reset outside each timed
operation, or another verified boundary that preserves the contract. Retain
and disclose timer/boundary costs; do not subtract them silently. Verify that
boundary before collection. Construction, scalar reads, holder replacement,
allocation, graph outcomes, five-fork capture, and experimental reporting still
need connection. The milestone is not complete and no human decision is needed.
