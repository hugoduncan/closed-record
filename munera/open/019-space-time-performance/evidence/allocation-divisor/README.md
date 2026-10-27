# Allocation divisor controls

These are memory-tool controls, not performance acceptance results.
Both commands exit 0:

```sh
JAVA_HOME="$HOME/.local/share/mise/installs/java/temurin-21.0.10+7.0.LTS" bash dev/bench/memory/allocation-probe.sh
JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home" bash dev/bench/memory/allocation-probe.sh
```

The EDN files retain raw counts and operation counts. Each 64-byte array costs
80 allocated bytes at both batch sizes (257 and 4096). The empty batch has no
normalised result. No overhead is subtracted. Existing allocation, thread-scope,
and non-allocating controls run in the same process. Metadata records the parent
revision, dirty state, source hashes, runtimes, machine, timestamp, and exits.
No extra VM flags or external tools are used. Graph accounting is not tested.
