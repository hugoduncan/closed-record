# Field allocation control
Tue Oct 27 22:11:03 UTC 2026
3c7ddafa3654c57672c4f29769970ab883ea9052
Working tree: probe added to revision above; source hashes below.
Command: JAVA_HOME="$PWD/target/valhalla/jdk-27.jdk/Contents/Home" bash dev/bench/valhalla/field-allocation.sh
Exit: 0. Control only, not acceptance evidence.
Attempt 1: exit 1; reference allocation exceeded holder shallow size. Final assertion accounts for the payload box. Failure stderr retained.
openjdk version "27-jep401ea3" 2026-09-15
OpenJDK Runtime Environment (build 27-jep401ea3+1-1)
OpenJDK 64-Bit Server VM (build 27-jep401ea3+1-1, mixed mode, sharing)
Darwin duncans-MacBook-Pro.local 27.0.0 Darwin Kernel Version 27.0.0: Tue Aug 11 21:06:40 PDT 2026; root:xnu-13432.1.9~1/RELEASE_ARM64_T6050 arm64
adb9b1fcddb3bb5fa33f4eccaf0b63d021fc26d07898896ac98e5b6586521bed  dev/bench/valhalla/FieldAllocationProbe.java
55775766aa4d1e81535fc6ce9bf8c11cdd7f2b6f2f93f078d3b3c23be67fa389  dev/bench/valhalla/CapabilityProbe.java
52a369441d4bb3a4d22e5611286fbcec52959155fcda8cbf8df973ea2198f538  dev/bench/valhalla/field-allocation.sh
