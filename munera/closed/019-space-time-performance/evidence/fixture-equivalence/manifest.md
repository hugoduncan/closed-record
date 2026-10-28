# Fixture-equivalence verification

Command: `JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home" bash dev/bench/valhalla/fixtures.sh`

Exit: 0. This is not a performance run.

Base revision: cfe79d712cecaf276824a6a07ce674d0837f14dd

Source working tree contains this slice. SHA-256:

```text
a883e81a01a73fdad774dad91bd4e1c7fbffa8ac7edd5d36248b41378bb95732  dev/bench/valhalla/FixtureProbe.java
748ea77515f35fdbef18ad0a5d9fe9e3e463d5b0cab60e640abe7e13a8022b85  dev/bench/valhalla/fixtures.clj
7c2cafa712960dc84892b371ff8031253884dad3623adf79f608857d1f874688  dev/bench/valhalla/fixtures.sh
55775766aa4d1e81535fc6ce9bf8c11cdd7f2b6f2f93f078d3b3c23be67fa389  dev/bench/valhalla/CapabilityProbe.java
97a387f5c0a9a460cab33480d3faabe73baf1c92de32e7f402f20216959fadee  dev/bench/valhalla/probe.sh
3d93df525e903df721546d4f6bc64c809ed64da26557517aadd1ff30d81e3056  dev/bench/cases.edn
```

Machine: Darwin arm64
